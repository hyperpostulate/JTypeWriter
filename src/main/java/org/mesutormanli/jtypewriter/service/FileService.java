/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.AtomicFiles;
import org.mesutormanli.jtypewriter.IoExecutor;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Document I/O with data-safety guarantees:
 * <ul>
 *   <li><b>Atomic, durable writes:</b> content is fsynced to a temp file and
 *       renamed into place (see {@link AtomicFiles}), so neither a crash
 *       mid-save nor a power cut can leave a truncated document;</li>
 *   <li><b>Rotating backups:</b> every overwrite keeps the previous version as
 *       {@code <file>.bak} (plus {@code .bak.1/.bak.2} generations). The backup
 *       is staged before the write and promoted afterwards, and a failing
 *       backup is logged without ever cancelling the document write;</li>
 *   <li><b>Encoding stability:</b> a document is read with encoding detection
 *       (BOM / strict UTF-8, otherwise the user is asked - see
 *       {@link TextCodec}) and always written back in that same encoding.
 *       Only when the text no longer fits that encoding does the user get to
 *       switch the document to UTF-8 - unmappable characters are never
 *       silently turned into {@code ?}.</li>
 *   <li><b>External-change detection:</b> the content hash of the file is
 *       remembered on load and after every save; a save whose target no longer
 *       matches it asks before overwriting, so a {@code git checkout}, a sync
 *       client or a second editor cannot lose someone else's text silently.
 *       The digest, not the timestamp, decides: two edits inside the same
 *       second look identical to a modification time.</li>
 * </ul>
 *
 * <p><strong>Threading:</strong> every call that touches a file goes through
 * {@link IoExecutor}; the FX thread only ever runs state commits, dialogs and
 * listener notifications. The {@code *Async} methods are the production entry
 * points: they return immediately and complete on the FX thread after the
 * write/read is done. The synchronous methods remain for startup restore and
 * for headless tests; they block the caller on the worker but still never
 * perform the I/O on that caller. Their default executor is
 * {@link IoExecutor#synchronous()} - the application wires a shared
 * {@link IoExecutor#background()} in {@code AppGraph}.</p>
 *
 * <p><strong>Stale completions:</strong> because a save or load can finish
 * after the user already opened another document, every operation captures a
 * document token. A completion whose token is no longer current is dropped:
 * the file itself was still written, but it never overwrites the state of a
 * newer document.</p>
 */
public class FileService {

    /** Observer hooks for the document lifecycle (autosave, session stats). */
    public interface Listener {
        default void onLoaded(Path file) {
        }

        default void onSaved(Path file) {
        }

        /**
         * Called after a brand-new, untitled document replaced the previous one.
         * Unlike {@link #onLoaded(Path)} there is no file to report: the new
         * document has no path yet (it is only assigned on the first save), so
         * listeners must not try to remember or re-read a file here.
         */
        default void onNewDocument() {
        }

        /**
         * Reports that a save started or finished, so the UI can show a
         * "saving" state instead of looking frozen. Always called on the FX
         * thread; a finished save fires after {@link #onSaved(Path)}.
         */
        default void onSavingChanged(boolean saving) {
        }
    }

    private static final Logger LOG = AppLogging.logger(FileService.class);

    /**
     * Upper bound for a file this service is willing to read into the editor.
     *
     * <p>{@link Files#readAllBytes} has no size concept: a stray multi-gigabyte
     * file (a log, a disk image, a runaway export) would be pulled into memory
     * and could take the application down with an {@code OutOfMemoryError}
     * before a single character is shown. The check is deliberately generous
     * (64 MiB is far beyond a document a person types) and has the same value
     * as {@link RecoveryStore#MAX_DRAFT_BYTES}, because a recovery draft is a
     * copy of such a document.</p>
     */
    public static final int MAX_DOCUMENT_BYTES = 64 * 1024 * 1024;

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    /** The atomic writer; an interface so a test can simulate a failed save. */
    @FunctionalInterface
    interface Writer {
        void write(Path target, byte[] bytes) throws IOException;
    }

    /**
     * Signals "measure the target's file store" from the {@link #usableSpace}
     * seam. Only tests inject a concrete value; production must always go
     * through the real file store so the check follows the volume the document
     * is actually written to.
     */
    private static final long MEASURE_USABLE_SPACE = Long.MIN_VALUE;

    private static final LongSupplier MEASURE = () -> MEASURE_USABLE_SPACE;

    private final DialogService dialogService;
    private final Messages messages;
    private final BackupKeeper backupKeeper;
    private final IoExecutor ioExecutor;
    private final Writer writer;
    private final LongSupplier usableSpace;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private Path currentFilePath;
    private String savedContent = "";
    private String currentContent = "";
    private Charset currentCharset = StandardCharsets.UTF_8;
    private boolean currentBom;
    private boolean dirty;

    /**
     * What the file at {@link #currentFilePath} contained when this service last
     * read or wrote it, or {@code null} when there is nothing to compare
     * (a never-saved document, or an original that could not be read). A save
     * whose target no longer matches it must ask before replacing the bytes.
     */
    private DiskFingerprint savedFingerprint;

    /**
     * Identifies the current document generation. A load, a new document or a
     * recovered draft bumps it; a save/load completion that captured an older
     * value is stale and must not mutate state.
     */
    private long operation;

    public FileService(DialogService dialogService, Messages messages) {
        this(dialogService, messages, new BackupKeeper(), IoExecutor.synchronous(),
                AtomicFiles::writeMoving);
    }

    /** Visible for tests: lets a test inject a BackupKeeper whose phases fail. */
    FileService(DialogService dialogService, Messages messages, BackupKeeper backupKeeper) {
        this(dialogService, messages, backupKeeper, IoExecutor.synchronous(),
                AtomicFiles::writeMoving);
    }

    /**
     * Production constructor: all blocking I/O is executed by {@code ioExecutor}
     * (shared with {@link AutoSaveService} so saves and snapshots serialise).
     */
    public FileService(DialogService dialogService, Messages messages,
                       BackupKeeper backupKeeper, IoExecutor ioExecutor) {
        this(dialogService, messages, backupKeeper, ioExecutor, AtomicFiles::writeMoving);
    }

    /**
     * Visible for tests: lets a test replace the atomic writer, e.g. to prove
     * that a write rejected by a full disk is reported as a failure and leaves
     * the document dirty instead of pretending the save succeeded.
     */
    FileService(DialogService dialogService, Messages messages, BackupKeeper backupKeeper,
                IoExecutor ioExecutor, Writer writer) {
        this(dialogService, messages, backupKeeper, ioExecutor, writer, MEASURE);
    }

    /**
     * Visible for tests: additionally lets a test inject the free-space seam,
     * so the pre-flight check for a backup staging copy can be exercised
     * without filling a real disk. Production keeps the real file store
     * ({@link #fileStoreUsableSpace(Path)}) through the {@link #MEASURE}
     * supplier.
     */
    FileService(DialogService dialogService, Messages messages, BackupKeeper backupKeeper,
                IoExecutor ioExecutor, Writer writer, LongSupplier usableSpace) {
        this.dialogService = dialogService;
        this.messages = messages;
        this.backupKeeper = backupKeeper;
        this.ioExecutor = ioExecutor;
        this.writer = writer;
        this.usableSpace = usableSpace;
    }

    public String getCurrentContent() {
        return currentContent;
    }

    public void setCurrentContent(String content) {
        this.currentContent = content;
        this.dirty = differsFromSaved(content);
    }

    public boolean hasUnsavedChanges() {
        return dirty;
    }

    /**
     * Decides the dirty marker in O(1) for the common case: a keystroke changes
     * the length, so the per-key path never walks the document.
     * Only a same-length replacement pays for the comparison, and that is also
     * what lets "undone back to the saved text" clear the marker again.
     */
    private boolean differsFromSaved(String content) {
        if (savedContent == null) return true;          // recovered draft: never on disk
        if (content == savedContent) return false;      // same instance
        if (content.length() != savedContent.length()) return true;  // a keystroke: length decided it
        return !content.equals(savedContent);           // same length: paste/undo, compare once
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Opens a file only after the current document is safe. Unsaved changes
     * trigger the same Save / Don't Save / Cancel prompt used on exit; choosing
     * Cancel - or abandoning the save - aborts the open so no text is ever lost.
     *
     * <p>Blocking variant; production UI uses {@link #openFileAsync}.</p>
     */
    public Optional<Path> openFile(javafx.stage.Stage stage) {
        if (!confirmDiscardOrSave(stage)) {
            return Optional.empty();
        }

        var file = dialogService.showOpenDialog(stage);
        if (file.isEmpty()) return Optional.empty();

        return loadFile(stage, file.get().toPath());
    }

    /**
     * Async open: the disk read runs on the I/O worker and the returned future
     * completes on the FX thread, so the caller can hand the decoded text to
     * the editor without freezing the UI.
     */
    public CompletableFuture<Optional<Path>> openFileAsync(javafx.stage.Stage stage) {
        return confirmDiscardOrSaveAsync(stage).thenCompose(proceed -> {
            if (!proceed) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return ioExecutor.completeOnFx(() -> dialogService.showOpenDialog(stage))
                    .thenCompose(file -> file.isEmpty()
                            ? CompletableFuture.completedFuture(Optional.<Path>empty())
                            : openPathAsync(stage, file.get().toPath()));
        });
    }

    /**
     * Opens a document whose path is already known - a drop, or any other
     * gesture that names the file itself instead of asking the file chooser.
     *
     * <p>The unsaved-changes gate is the one used by
     * {@link #openFileAsync(javafx.stage.Stage)} and {@link #newDocumentAsync}:
     * this method does not ask a second question and does not invent a second
     * save path. A drop is just another way to name the next document, so
     * Cancel, a failing save and the encoding prompt all behave exactly as they
     * do on Open.</p>
     */
    public CompletableFuture<Optional<Path>> openPathAsync(javafx.stage.Stage stage, Path path) {
        return confirmDiscardOrSaveAsync(stage).thenCompose(proceed ->
                proceed
                        ? loadFileAsync(stage, path)
                        : CompletableFuture.completedFuture(Optional.empty()));
    }

    /**
     * Blocking variant of {@link #openPathAsync(javafx.stage.Stage, Path)};
     * production UI uses the async one, tests use this.
     */
    public Optional<Path> openPath(javafx.stage.Stage stage, Path path) {
        if (!confirmDiscardOrSave(stage)) {
            return Optional.empty();
        }
        return loadFile(stage, path);
    }

    /**
     * Starts a new, empty document. The unsaved-changes prompt is exactly the
     * one used by {@link #openFile(javafx.stage.Stage)}: choosing Cancel - or
     * abandoning the save - aborts the switch so no text is ever lost.
     *
     * <p>The resulting document is clean (no dirty marker) and does not inherit
     * anything from the previous file: it has no path and uses plain UTF-8
     * without a BOM, exactly as if the application had just started.</p>
     *
     * @return {@code true} when the new document was created, {@code false}
     *         when the user cancelled or the save of the old draft failed
     */
    public boolean newDocument(javafx.stage.Stage stage) {
        if (!confirmDiscardOrSave(stage)) {
            return false;
        }
        return commitNewDocument(++operation);
    }

    /** Async variant of {@link #newDocument(javafx.stage.Stage)}. */
    public CompletableFuture<Boolean> newDocumentAsync(javafx.stage.Stage stage) {
        return confirmDiscardOrSaveAsync(stage).thenCompose(proceed ->
                proceed
                        ? ioExecutor.completeOnFx(() -> commitNewDocument(++operation))
                        : CompletableFuture.completedFuture(false));
    }

    private boolean commitNewDocument(long operationId) {
        if (operationId != operation) {
            return false; // a newer document replaced this one while the prompt was open
        }
        currentFilePath = null;
        currentCharset = StandardCharsets.UTF_8;
        currentBom = false;
        savedContent = "";
        currentContent = "";
        savedFingerprint = null;
        dirty = false;
        listeners.forEach(Listener::onNewDocument);
        return true;
    }

    /**
     * Shared guard for every document switch: returns {@code true} when it is
     * safe to leave the current document, {@code false} when the user
     * cancelled or the draft could not be saved.
     */
    private boolean confirmDiscardOrSave(javafx.stage.Stage stage) {
        if (!hasUnsavedChanges()) {
            return true;
        }
        switch (dialogService.showUnsavedChangesDialog(stage)) {
            case SAVE -> {
                if (saveFile(stage, currentContent).isEmpty()) {
                    return false; // save cancelled or failed: keep the draft
                }
            }
            case DISCARD -> {
                // the user explicitly accepted losing the draft
            }
            case CANCEL -> {
                return false;
            }
        }
        return true;
    }

    private CompletableFuture<Boolean> confirmDiscardOrSaveAsync(javafx.stage.Stage stage) {
        if (!hasUnsavedChanges()) {
            return CompletableFuture.completedFuture(true);
        }
        return switch (dialogService.showUnsavedChangesDialog(stage)) {
            case SAVE -> saveFileAsync(stage, currentContent).thenApply(Optional::isPresent);
            case DISCARD -> CompletableFuture.completedFuture(true);
            case CANCEL -> CompletableFuture.completedFuture(false);
        };
    }

    /**
     * Loads a document that is already known - the file picker has been
     * answered, or the path comes from session restore. The encoding prompt
     * still applies, so a legacy document is never silently mangled.
     *
     * <p>Blocking variant; production UI uses {@link #loadFileAsync}.</p>
     */
    public Optional<Path> loadFile(javafx.stage.Stage stage, Path path) {
        long operationId = ++operation;
        ReadResult read;
        try {
            read = ioExecutor.submit("open " + path, () -> read(path)).join();
        } catch (CompletionException failure) {
            read = ReadResult.failed(new IOException(failure.getCause()));
        }
        return commitLoaded(stage, operationId, path, read);
    }

    /** Async load: the read runs on the I/O worker, the commit on the FX thread. */
    public CompletableFuture<Optional<Path>> loadFileAsync(javafx.stage.Stage stage, Path path) {
        long operationId = ++operation;
        return ioExecutor.submit("open " + path, () -> read(path))
                .thenCompose(read -> ioExecutor.completeOnFx(
                        () -> commitLoaded(stage, operationId, path, read)));
    }

    /**
     * Reads a document with a hard size ceiling. The size is checked before the
     * file is opened, and the bounded read after it covers a file that grows
     * between the check and the read (or a sparse/lying size). An oversized file
     * is never truncated: the caller refuses the open and tells the user why.
     */
    private static ReadResult read(Path path) {
        try {
            var size = Files.size(path);
            if (size > MAX_DOCUMENT_BYTES) {
                return ReadResult.tooLarge(size);
            }
            try (var in = Files.newInputStream(path)) {
                var bytes = in.readNBytes(MAX_DOCUMENT_BYTES + 1);
                if (bytes.length > MAX_DOCUMENT_BYTES) {
                    return ReadResult.tooLarge(bytes.length);
                }
                return ReadResult.ok(bytes, DiskFingerprint.of(path, bytes));
            }
        } catch (IOException failure) {
            return ReadResult.failed(failure);
        }
    }

    private Optional<Path> commitLoaded(javafx.stage.Stage stage, long operationId,
                                        Path path, ReadResult read) {
        if (operationId != operation) {
            return Optional.empty(); // a newer document operation won
        }
        if (read.tooLarge()) {
            // No silent truncation and no partial document: an editor that opens
            // the first 64 MiB of a file invites the user to save that prefix
            // over the original. Refusing is the only safe answer.
            dialogService.showError(stage, messages.errorOpenTooLargeHeader(),
                    messages.errorOpenTooLargeContent(path.getFileName().toString(),
                            megabytes(read.tooLargeBytes()), megabytes(MAX_DOCUMENT_BYTES)));
            return Optional.empty();
        }
        if (read.failure() != null) {
            dialogService.showError(stage, messages.errorOpen(), read.failure().getMessage());
            return Optional.empty();
        }
        var decoded = decode(read.bytes(), stage, path);
        if (decoded == null || operationId != operation) {
            return Optional.empty(); // unsupported encoding cancelled, or a newer load won
        }
        currentFilePath = path;
        currentCharset = decoded.charset();
        currentBom = decoded.bom();
        savedContent = decoded.text();
        currentContent = decoded.text();
        savedFingerprint = read.fingerprint();
        dirty = false;
        listeners.forEach(l -> l.onLoaded(path));
        return Optional.of(path);
    }

    /**
     * Re-attaches a recovered draft as a dirty document, using this build's
     * default encoding for the recovered text.
     *
     * @see #restoreRecovered(Path, String, Charset, boolean)
     */
    public void restoreRecovered(Path file, String content) {
        restoreRecovered(file, content, null, false);
    }

    /**
     * Re-attaches a recovered draft as a dirty document. The encoding of the
     * on-disk original (when still present) is reused so a subsequent save
     * preserves it.
     *
     * <p>When the original does not decode on its own it is a legacy,
     * BOM-less, non-UTF-8 document. The only legacy encoding this application
     * offers is windows-1254, so that is assumed here as well: a windows-1254
     * document restored from a draft must stay byte-identical when it is
     * saved, instead of being silently rewritten as UTF-8.</p>
     *
     * <p>{@code draftCharset}/{@code draftBom} are what the draft recorded when
     * it was written; they are used only when the original can no longer be
     * read, because the file itself is the better evidence. A draft can outlive
     * its document (a deleted file, an unreadable volume), and without the
     * hint a windows-1254 document would be restored and then saved as UTF-8 -
     * an encoding change the user never asked for.</p>
     *
     * <p>The original is read through {@link IoExecutor} and bounded by
     * {@link #MAX_DOCUMENT_BYTES}: this method blocks its caller on the worker,
     * but it never performs the disk read on the FX thread, and an oversized
     * original is treated as unverifiable instead of being pulled into memory.
     * When the original's byte-order mark declares an encoding its bytes do not
     * satisfy, the document keeps that <em>declared</em> encoding: the damaged
     * file is not silently converted to UTF-8, and a character the encoding
     * cannot represent still reaches the existing "save as UTF-8" prompt.</p>
     *
     * @param draftCharset the charset recorded in the draft, or {@code null}
     *        when it predates the metadata
     * @param draftBom whether the draft recorded a byte-order mark
     */
    public void restoreRecovered(Path file, String content,
                                 Charset draftCharset, boolean draftBom) {
        operation++; // the recovered draft replaces whatever was current
        savedFingerprint = null;
        // Start from "no document on disk": when the original is gone, the
        // recovered text must not inherit the previous document's path or
        // encoding and be saved over it. Every failure branch below leaves this
        // null rather than pointing at an unrelated document.
        currentFilePath = null;
        currentCharset = draftCharset == null ? StandardCharsets.UTF_8 : draftCharset;
        currentBom = draftCharset != null && draftBom;
        if (file != null && Files.isRegularFile(file)) {
            try {
                var read = ioExecutor.submit("restore " + file,
                        () -> readRecoveredOriginal(file)).join();
                if (read.tooLarge()) {
                    // Without the bytes the encoding cannot be verified. Treat it
                    // as an unverifiable original: no path (so a save must ask
                    // where to go) and the draft hint survives, instead of
                    // reattaching the draft to a file this build refuses to read.
                    LOG.log(Level.WARNING, "recovered file " + file + " is "
                            + read.tooLargeBytes() + " bytes, above the "
                            + MAX_DOCUMENT_BYTES + "-byte read ceiling; its encoding cannot be"
                            + " verified, so the draft encoding is kept and the file is not reattached");
                } else {
                    var original = read.bytes();
                    // Remember what was on disk at recovery time, so the first save
                    // of the draft asks before replacing a version that changed in
                    // the meantime (the draft itself is newer than this content).
                    savedFingerprint = DiskFingerprint.of(file, original);
                    applyDetectedEncoding(file, original);
                    currentFilePath = file;
                }
            } catch (CompletionException failure) {
                // The worker wraps the real cause; an IOException here means the
                // original could not be read, so its encoding is unknown.
                var cause = failure.getCause() == null ? failure : failure.getCause();
                // The encoding is unknown, so the next save must ask for a path
                // instead of silently rewriting an unreadable original as UTF-8.
                LOG.log(Level.WARNING, "could not detect encoding of recovered file " + file, cause);
            }
        }
        // Nothing on disk to compare against: the draft stays dirty even when it
        // is empty, so autosave never clears a recovered empty draft.
        savedContent = null;
        currentContent = content;
        dirty = true;
    }

    /**
     * Applies the encoding the original's own bytes declare. A malformed BOM
     * keeps the declared charset and BOM flag: that is the encoding the file is
     * written in, so the recovered draft is not silently re-encoded as UTF-8 on
     * its next save. The malformed content itself is not needed - the draft is
     * the recovered text - but the declaration must not be dropped.
     */
    private void applyDetectedEncoding(Path file, byte[] original) {
        try {
            var onDisk = TextCodec.decode(original);
            if (onDisk != null) {
                currentCharset = onDisk.charset();
                currentBom = onDisk.bom();
            } else {
                // Legacy, BOM-less content that is not valid UTF-8: keep the
                // app's fallback encoding so the next save does not convert
                // the file to UTF-8 behind the user's back.
                currentCharset = TextCodec.WINDOWS_1254;
                currentBom = false;
            }
        } catch (TextCodec.MalformedDocumentException malformed) {
            // The draft is what matters here; a damaged original must not abort
            // the restore. Its BOM still declares the encoding the file is in,
            // so carry that declaration instead of falling back to UTF-8.
            currentCharset = malformed.declaredCharset();
            currentBom = malformed.declaredByBom();
            LOG.log(Level.WARNING, "recovered file " + file
                    + " has malformed " + malformed.declaredCharset().name()
                    + " content under its byte-order mark; restoring it with that declared encoding",
                    malformed);
        }
    }

    /**
     * Reads the original of a recovered draft with the same size policy as
     * {@link #read(Path)}. Runs on the I/O worker only.
     */
    private static RecoveredRead readRecoveredOriginal(Path file) throws IOException {
        var size = Files.size(file);
        if (size > MAX_DOCUMENT_BYTES) {
            return RecoveredRead.tooLarge(size);
        }
        try (var in = Files.newInputStream(file)) {
            var bytes = in.readNBytes(MAX_DOCUMENT_BYTES + 1);
            if (bytes.length > MAX_DOCUMENT_BYTES) {
                return RecoveredRead.tooLarge(bytes.length);
            }
            return RecoveredRead.ok(bytes);
        }
    }

    /** Blocking save; production UI uses {@link #saveFileAsync}. */
    public Optional<Path> saveFile(javafx.stage.Stage stage, String content) {
        if (currentFilePath == null) {
            return saveFileAs(stage, content);
        }
        return writeBlocking(stage, currentFilePath, content);
    }

    /** Blocking Save As; production UI uses {@link #saveFileAsAsync}. */
    public Optional<Path> saveFileAs(javafx.stage.Stage stage, String content) {
        var file = dialogService.showSaveDialog(stage, currentFilePath);
        if (file.isEmpty()) return Optional.empty();

        return writeBlocking(stage, file.get().toPath(), content);
    }

    /** Async save: writes on the I/O worker, commits and reports on the FX thread. */
    public CompletableFuture<Optional<Path>> saveFileAsync(javafx.stage.Stage stage, String content) {
        if (currentFilePath == null) {
            return saveFileAsAsync(stage, content);
        }
        return writeAsync(stage, currentFilePath, content);
    }

    /** Async Save As. */
    public CompletableFuture<Optional<Path>> saveFileAsAsync(javafx.stage.Stage stage, String content) {
        var file = dialogService.showSaveDialog(stage, currentFilePath);
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return writeAsync(stage, file.get().toPath(), content);
    }

    /**
     * Everything a save needs from the FX thread's current document, captured
     * once. {@code expectedFingerprint} is what the target file held when this
     * service last saw it, or {@code null} when the target is a new file and
     * there is nothing that could have been overwritten.
     */
    private record SaveContext(long operationId, Path path, String content, Charset charset,
                               boolean bom, String expectedCurrent, boolean unchanged,
                               DiskFingerprint expectedFingerprint) {

        /** The same save re-encoded as UTF-8, still guarding the same disk state. */
        SaveContext asUtf8() {
            return new SaveContext(operationId, path, content, StandardCharsets.UTF_8,
                    bom, expectedCurrent, false, expectedFingerprint);
        }

        /**
         * The same save after the user chose to replace a changed file. The
         * fingerprint guard is dropped (the user has just seen the conflict) and
         * {@code unchanged} is forced off, so the externally changed version is
         * preserved as the backup copy instead of being skipped.
         */
        SaveContext overwriting() {
            return new SaveContext(operationId, path, content, charset, bom,
                    expectedCurrent, false, null);
        }
    }

    private SaveContext contextFor(Path path, String content) {
        var sameFile = path.equals(currentFilePath);
        return new SaveContext(operation, path, content, currentCharset, currentBom,
                currentContent, sameFile && content.equals(savedContent),
                sameFile ? savedFingerprint : null);
    }

    private Optional<Path> writeBlocking(javafx.stage.Stage stage, Path path, String content) {
        var context = contextFor(path, content);
        notifySavingChanged(true);
        try {
            return handleResult(stage, context, writeOnce(context));
        } finally {
            notifySavingChanged(false);
        }
    }

    /** Runs one encoding+write attempt on the I/O worker and waits for its verdict. */
    private WriteResult writeOnce(SaveContext context) {
        try {
            return ioExecutor.submit("save " + context.path(), () -> writeAttempt(context)).join();
        } catch (CompletionException failure) {
            return new WriteFailed(messageOf(failure));
        }
    }

    private Optional<Path> handleResult(javafx.stage.Stage stage, SaveContext context,
                                        WriteResult result) {
        if (result instanceof Written written) {
            return commitSave(context, written.fingerprint());
        }
        if (result instanceof UpgradeRequired) {
            if (!dialogService.showEncodingUpgradeDialog(stage, context.charset())) {
                return Optional.empty(); // nothing written: the document stays dirty
            }
            var upgraded = context.asUtf8();
            return handleResult(stage, upgraded, writeOnce(upgraded));
        }
        if (result instanceof Conflict) {
            return switch (dialogService.showExternalChangeDialog(stage, context.path())) {
                case OVERWRITE -> {
                    var override = context.overwriting();
                    yield handleResult(stage, override, writeOnce(override));
                }
                // Save As keeps both versions and returns the usual save result,
                // so an exit/open waiting on this save still sees a cancelled
                // file picker as "not safe to leave".
                case SAVE_AS -> saveFileAs(stage, context.content());
                case CANCEL -> Optional.empty();
            };
        }
        return reportFailure(stage, result);
    }

    private CompletableFuture<Optional<Path>> writeAsync(javafx.stage.Stage stage,
                                                         Path path, String content) {
        var context = contextFor(path, content);
        notifySavingChanged(true);
        return writeAttemptAsync(stage, context)
                .whenComplete((ignored, failure) -> notifySavingChanged(false));
    }

    private CompletableFuture<Optional<Path>> writeAttemptAsync(javafx.stage.Stage stage,
                                                                SaveContext context) {
        return ioExecutor.submit("save " + context.path(), () -> writeAttempt(context))
                .thenCompose(result -> handleResultAsync(stage, context, result));
    }

    private CompletableFuture<Optional<Path>> handleResultAsync(javafx.stage.Stage stage,
                                                                SaveContext context,
                                                                WriteResult result) {
        return switch (result) {
            case Written written -> ioExecutor.completeOnFx(
                    () -> commitSave(context, written.fingerprint()));
            case UpgradeRequired upgrade -> ioExecutor
                    .completeOnFx(() -> dialogService.showEncodingUpgradeDialog(
                            stage, context.charset()))
                    .thenCompose(accepted -> accepted
                            ? writeAttemptAsync(stage, context.asUtf8())
                            : CompletableFuture.completedFuture(Optional.<Path>empty()));
            case Conflict conflict -> ioExecutor
                    .completeOnFx(() -> dialogService.showExternalChangeDialog(
                            stage, context.path()))
                    .thenCompose(choice -> switch (choice) {
                        case OVERWRITE -> writeAttemptAsync(stage, context.overwriting());
                        case SAVE_AS -> saveFileAsAsync(stage, context.content());
                        case CANCEL -> CompletableFuture.completedFuture(Optional.<Path>empty());
                    });
            case WriteFailed failure -> ioExecutor
                    .completeOnFx(() -> reportFailure(stage, failure));
        };
    }

    /**
     * Encodes and writes one attempt. Runs on the I/O worker only: it never
     * shows a dialog, it reports what happened so the caller can decide.
     */
    private WriteResult writeAttempt(SaveContext context) {
        byte[] bytes;
        try {
            bytes = TextCodec.encode(context.content(), context.charset(), context.bom());
        } catch (CharacterCodingException notRepresentable) {
            if (StandardCharsets.UTF_8.equals(context.charset())) {
                // UTF-8 represents every character: unreachable in practice
                return new WriteFailed(notRepresentable.getMessage());
            }
            return UpgradeRequired.INSTANCE;
        }
        var path = context.path();
        try {
            if (changedOnDisk(context)) {
                // Stop before the backup too: the conflict dialog may end in
                // "Save As" or "Cancel", and neither should leave a staged copy.
                return Conflict.INSTANCE;
            }
            // Re-saving unchanged content would rotate the backup history with
            // copies of the same text; the encoding-upgrade flow always has
            // content != savedContent, so it is unaffected by this guard.
            // The document write is the primary operation; the backup protects
            // the previous version but must never be able to cancel the save.
            // The copy is therefore staged first, but failures only warn.
            BackupKeeper.PreparedBackup prepared = null;
            if (!context.unchanged()) {
                var stagingCopy = Files.isRegularFile(path);
                if (stagingCopy) {
                    // The backup staging copy occupies the same volume before the
                    // document temp file is created. Checking only the document
                    // bytes would let the copy consume the space the document
                    // then needs, or silently degrade to "no backup" on a nearly
                    // full disk. Unknown free space must never block a save.
                    long required = bytes.length + Files.size(path);
                    long usable = usableSpace(path);
                    if (usable >= 0 && required > usable) {
                        return new WriteFailed("not enough free disk space: " + required
                                + " bytes needed for the document and its backup, "
                                + usable + " available");
                    }
                }
                try {
                    prepared = backupKeeper.prepare(path).orElse(null);
                } catch (IOException | RuntimeException backupFailure) {
                    LOG.log(Level.WARNING, "could not stage a backup of " + path
                            + "; saving without it", backupFailure);
                }
            }
            try {
                writer.write(path, bytes);
            } catch (IOException | RuntimeException | Error writeFailure) {
                if (prepared != null) {
                    backupKeeper.discard(prepared);
                }
                throw writeFailure; // the outer catch reports the failed save
            }
            if (prepared != null) {
                // The new content is already on disk: a failing install must
                // not turn a successful save into an error.
                try {
                    backupKeeper.install(prepared);
                } catch (IOException | RuntimeException backupFailure) {
                    LOG.log(Level.WARNING, "could not install the backup of " + path
                            + "; the document itself was saved", backupFailure);
                    backupKeeper.discard(prepared);
                }
            }
            // The bytes just written are the new truth about this file: a later
            // save must compare against them, not against what was loaded.
            return new Written(DiskFingerprint.of(path, bytes));
        } catch (IOException | RuntimeException | Error e) {
            // An unexpected runtime failure or an Error (notably an
            // OutOfMemoryError while encoding a huge paste) must not take the
            // app down mid-save and must not look like a successful save: it
            // reaches the same failure path as a disk error, so the document
            // stays dirty and the recovery draft is kept.
            LOG.log(Level.WARNING, "save failed for " + path, e);
            return WriteFailed.of(e);
        }
    }

    /**
     * Tells whether the target changed on disk since this service last read or
     * wrote it. Only the document's own file is guarded: a Save As writes a new
     * target whose previous content is not this document's concern.
     *
     * <p>The comparison uses the content digest, never the modification time or
     * the size alone - a file system may report a one-second timestamp, and a
     * same-length edit leaves the size untouched. A missing file counts as a
     * change: overwriting would silently recreate a file someone deleted or
     * moved.</p>
     */
    private static boolean changedOnDisk(SaveContext context) throws IOException {
        var expected = context.expectedFingerprint();
        if (expected == null) {
            return false; // new target: nothing to replace
        }
        try {
            var actual = DiskFingerprint.read(context.path());
            if (expected.sameContentAs(actual)) {
                return false;
            }
            LOG.log(Level.WARNING, "external change detected for " + context.path()
                    + ": expected " + expected.describe() + ", found " + actual.describe());
            return true;
        } catch (NoSuchFileException deleted) {
            LOG.log(Level.WARNING, "the target of " + context.path()
                    + " was deleted while the document was open");
            return true;
        }
    }

    private Optional<Path> commitSave(SaveContext context, DiskFingerprint fingerprint) {
        if (context.operationId() != operation) {
            // The file was written, but the user has already moved to another
            // document: never let the old save overwrite the new state.
            LOG.log(Level.FINE, "dropping the state commit of a stale save for " + context.path());
            return Optional.empty();
        }
        currentFilePath = context.path();
        currentCharset = context.charset();
        currentBom = context.bom();
        savedContent = context.content();
        savedFingerprint = fingerprint;
        if (Objects.equals(currentContent, context.expectedCurrent())) {
            // Nothing typed while the save was in flight: the document is the
            // text that just reached the disk.
            currentContent = context.content();
        }
        dirty = differsFromSaved(currentContent);
        // The file is written and fsynced at this point, so a failing listener
        // must never turn a successful save into a reported failure: each one is
        // isolated and only warns, and the rest still run.
        for (var listener : listeners) {
            try {
                listener.onSaved(context.path());
            } catch (RuntimeException failure) {
                LOG.log(Level.WARNING, "a save listener failed for " + context.path()
                        + "; the document itself was saved", failure);
            }
        }
        return Optional.of(context.path());
    }

    private Optional<Path> reportFailure(javafx.stage.Stage stage, WriteResult result) {
        if (result instanceof WriteFailed failure) {
            dialogService.showError(stage, messages.errorSave(), failure.message());
        }
        return Optional.empty();
    }

    private static String messageOf(CompletionException failure) {
        return WriteFailed.describe(failure.getCause() == null ? failure : failure.getCause());
    }

    /**
     * Usable bytes on the volume the save target lives on. The injected seam
     * wins when a test supplies one; production measures the target's file
     * store. An unmeasurable store returns {@code -1} ("unknown") so the save
     * itself decides - a network share or an exotic provider must never be
     * turned into a false "disk full".
     */
    private long usableSpace(Path target) {
        var supplied = usableSpace.getAsLong();
        return supplied == MEASURE_USABLE_SPACE ? fileStoreUsableSpace(target) : supplied;
    }

    /**
     * Mirrors {@code AtomicFiles.usableSpace}: a total size of zero means the
     * provider does not know the volume, while a zero <em>usable</em> size on a
     * known volume is a genuinely full disk and must stay distinguishable from
     * "unknown".
     */
    private static long fileStoreUsableSpace(Path target) {
        try {
            var dir = target.toAbsolutePath().getParent();
            if (dir == null) {
                return -1;
            }
            var store = Files.getFileStore(dir);
            return store.getTotalSpace() > 0 ? store.getUsableSpace() : -1;
        } catch (IOException unreadable) {
            LOG.log(Level.FINE, "cannot read free space for " + target, unreadable);
            return -1;
        }
    }

    /** Whole megabytes, rounded up so a limit is never understated in the dialog. */
    private static long megabytes(long bytes) {
        return Math.max(1, (bytes + BYTES_PER_MEGABYTE - 1) / BYTES_PER_MEGABYTE);
    }

    /** The "saving" state is advisory UI; a broken listener must never abort a save. */
    private void notifySavingChanged(boolean saving) {
        for (var listener : listeners) {
            try {
                listener.onSavingChanged(saving);
            } catch (RuntimeException failure) {
                LOG.log(Level.WARNING, "a saving-state listener failed", failure);
            }
        }
    }

    /** @return the decoded document, or {@code null} when the user cancelled the encoding prompt */
    private TextCodec.Result decode(byte[] bytes, javafx.stage.Stage stage, Path path) {
        TextCodec.Result decoded;
        try {
            decoded = TextCodec.decode(bytes);
        } catch (TextCodec.MalformedDocumentException malformed) {
            // The BOM promised an encoding the bytes do not honour: never show
            // replacement characters without the user's explicit consent.
            LOG.log(Level.WARNING, "malformed " + malformed.declaredCharset().name()
                    + " content under the byte-order mark of " + path, malformed);
            if (!dialogService.showMalformedEncodingDialog(stage, path, malformed.declaredCharset())) {
                return null; // open aborted: the current document stays untouched
            }
            LOG.log(Level.WARNING, "opening " + path + " as " + malformed.declaredCharset().name()
                    + " with replacement characters, confirmed by the user");
            return TextCodec.decodeBomWithReplacement(bytes, malformed.declaredCharset());
        }
        if (decoded != null) {
            return decoded;
        }
        return switch (dialogService.showEncodingDialog(stage, path)) {
            case WINDOWS1254 -> TextCodec.decodeWith(bytes, TextCodec.WINDOWS_1254);
            case UTF8 -> TextCodec.decodeWith(bytes, StandardCharsets.UTF_8);
            case CANCEL -> null;
        };
    }

    /** The verdict of the exit prompt; the caller owns closing the window. */
    public enum ExitAnswer {
        /** The document is safe (saved, never dirty, or explicitly discarded). */
        CLOSE,
        /** The user cancelled, or the save was abandoned: the window must stay. */
        STAY
    }

    /**
     * Asks the unsaved-changes question and reports the verdict, without
     * closing the window. The caller closes it, because only the caller can
     * tell the autosave service that this exit was a sanctioned one before the
     * window hides - the difference between deleting a recovery draft the user
     * has just abandoned and deleting one that was never asked about.
     *
     * <p>Blocking variant; the close handler uses {@link #requestExitAsync} so
     * a save during exit does not freeze the window.</p>
     */
    public ExitAnswer requestExit(javafx.stage.Stage stage) {
        return confirmDiscardOrSave(stage) ? ExitAnswer.CLOSE : ExitAnswer.STAY;
    }

    /** Async exit verdict: completes on the FX thread after any save finishes. */
    public CompletableFuture<ExitAnswer> requestExitAsync(javafx.stage.Stage stage) {
        return confirmDiscardOrSaveAsync(stage)
                .thenApply(safe -> safe ? ExitAnswer.CLOSE : ExitAnswer.STAY);
    }

    public Path getCurrentFilePath() {
        return currentFilePath;
    }

    /**
     * The charset the current document is written with. Read by
     * {@link AutoSaveService} so a recovery draft records the encoding its
     * document needs, and by the UI to describe the document.
     */
    public Charset getCurrentCharset() {
        return currentCharset;
    }

    /** Whether a save of the current document reproduces a byte-order mark. */
    public boolean isCurrentBom() {
        return currentBom;
    }

    /** The shared I/O seam; package-visible so {@link AutoSaveService} can serialise with it. */
    IoExecutor ioExecutor() {
        return ioExecutor;
    }

    // --- results of the blocking parts, free of UI types --------------------

    private sealed interface WriteResult permits Written, UpgradeRequired, WriteFailed, Conflict {
    }

    /** The bytes are on disk; the fingerprint describes the file they landed in. */
    private record Written(DiskFingerprint fingerprint) implements WriteResult {
    }

    private record UpgradeRequired() implements WriteResult {
        static final UpgradeRequired INSTANCE = new UpgradeRequired();
    }

    /**
     * The target changed on disk since the document was loaded or last saved.
     * Nothing has been written: the caller asks the user what to do.
     */
    private record Conflict() implements WriteResult {
        static final Conflict INSTANCE = new Conflict();
    }

    private record WriteFailed(String message) implements WriteResult {
        static WriteFailed of(Throwable failure) {
            return new WriteFailed(describe(failure));
        }

        /** An {@code Error} often has no message; its type is still worth showing. */
        static String describe(Throwable failure) {
            var message = failure.getMessage();
            return message == null || message.isBlank()
                    ? failure.getClass().getSimpleName()
                    : message;
        }
    }

    /**
     * The original of a recovered draft: either its bounded bytes, or the size
     * that made it too large to verify. A truncated prefix is never returned as
     * the "original", because decoding and fingerprinting it would describe a
     * file the editor refuses to open.
     */
    private record RecoveredRead(byte[] bytes, long tooLargeBytes) {
        static RecoveredRead ok(byte[] bytes) {
            return new RecoveredRead(bytes, -1);
        }

        static RecoveredRead tooLarge(long bytes) {
            return new RecoveredRead(null, bytes);
        }

        boolean tooLarge() {
            return tooLargeBytes >= 0;
        }
    }

    /**
     * A read outcome. {@code tooLargeBytes} is negative unless the file exceeded
     * {@link #MAX_DOCUMENT_BYTES}, in which case {@code bytes} is always empty:
     * a truncated prefix is never handed to the decoder.
     */
    private record ReadResult(byte[] bytes, IOException failure, long tooLargeBytes,
                              DiskFingerprint fingerprint) {
        static ReadResult ok(byte[] bytes, DiskFingerprint fingerprint) {
            return new ReadResult(bytes, null, -1, fingerprint);
        }

        static ReadResult failed(IOException failure) {
            return new ReadResult(null, failure, -1, null);
        }

        static ReadResult tooLarge(long bytes) {
            return new ReadResult(null, null, bytes, null);
        }

        boolean tooLarge() {
            return tooLargeBytes >= 0;
        }
    }

    /**
     * The identity of a file's content as this service last saw it.
     *
     * <p>The SHA-256 digest, not the timestamp, decides whether the file
     * changed: file systems with one-second timestamp granularity cannot
     * distinguish two edits inside the same second, and a same-length edit
     * leaves the size untouched. The modification time and size are recorded
     * for the log line and for a human reading a support report.</p>
     */
    private record DiskFingerprint(long size, long lastModifiedMillis, byte[] sha256) {

        /** A fingerprint of bytes this service has just read or is about to write. */
        static DiskFingerprint of(Path path, byte[] bytes) {
            return new DiskFingerprint(bytes.length, lastModified(path),
                    newDigest().digest(bytes));
        }

        /** A fingerprint of the file as it is on disk right now. */
        static DiskFingerprint read(Path path) throws IOException {
            var digest = newDigest();
            long size = 0;
            try (var in = Files.newInputStream(path)) {
                // Streamed: an external file that grew past the read ceiling
                // must not be loaded whole just to be compared.
                var buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                    size += read;
                }
            }
            return new DiskFingerprint(size, lastModified(path), digest.digest());
        }

        boolean sameContentAs(DiskFingerprint other) {
            return other != null && Arrays.equals(sha256, other.sha256);
        }

        /** The non-content half, for the log line that explains a conflict. */
        String describe() {
            var modified = lastModifiedMillis < 0
                    ? "unknown"
                    : Instant.ofEpochMilli(lastModifiedMillis).toString();
            return size + " bytes, modified at " + modified;
        }

        private static long lastModified(Path path) {
            try {
                return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException unreadable) {
                // Only diagnostic: the digest decides, so a missing timestamp
                // must not turn a comparison or a save into a failure.
                LOG.log(Level.FINE, "cannot read the modification time of " + path, unreadable);
                return -1;
            }
        }

        private static MessageDigest newDigest() {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                // Every conforming Java platform provides SHA-256.
                throw new IllegalStateException("SHA-256 is unavailable", impossible);
            }
        }
    }
}
