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
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private final DialogService dialogService;
    private final Messages messages;
    private final BackupKeeper backupKeeper;
    private final IoExecutor ioExecutor;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private Path currentFilePath;
    private String savedContent = "";
    private String currentContent = "";
    private Charset currentCharset = StandardCharsets.UTF_8;
    private boolean currentBom;
    private boolean dirty;

    /**
     * Identifies the current document generation. A load, a new document or a
     * recovered draft bumps it; a save/load completion that captured an older
     * value is stale and must not mutate state.
     */
    private long operation;

    public FileService(DialogService dialogService, Messages messages) {
        this(dialogService, messages, new BackupKeeper(), IoExecutor.synchronous());
    }

    /** Visible for tests: lets a test inject a BackupKeeper whose phases fail. */
    FileService(DialogService dialogService, Messages messages, BackupKeeper backupKeeper) {
        this(dialogService, messages, backupKeeper, IoExecutor.synchronous());
    }

    /**
     * Production constructor: all blocking I/O is executed by {@code ioExecutor}
     * (shared with {@link AutoSaveService} so saves and snapshots serialise).
     */
    public FileService(DialogService dialogService, Messages messages,
                       BackupKeeper backupKeeper, IoExecutor ioExecutor) {
        this.dialogService = dialogService;
        this.messages = messages;
        this.backupKeeper = backupKeeper;
        this.ioExecutor = ioExecutor;
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
                            : loadFileAsync(stage, file.get().toPath()));
        });
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
            read = new ReadResult(null, new IOException(failure.getCause()));
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

    private static ReadResult read(Path path) {
        try {
            return new ReadResult(Files.readAllBytes(path), null);
        } catch (IOException failure) {
            return new ReadResult(null, failure);
        }
    }

    private Optional<Path> commitLoaded(javafx.stage.Stage stage, long operationId,
                                        Path path, ReadResult read) {
        if (operationId != operation) {
            return Optional.empty(); // a newer document operation won
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
        dirty = false;
        listeners.forEach(l -> l.onLoaded(path));
        return Optional.of(path);
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
     */
    public void restoreRecovered(Path file, String content) {
        operation++; // the recovered draft replaces whatever was current
        if (file != null && Files.isRegularFile(file)) {
            try {
                var onDisk = TextCodec.decode(Files.readAllBytes(file));
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
                currentFilePath = file;
            } catch (TextCodec.MalformedDocumentException malformed) {
                // The draft is what matters here; a damaged original must not
                // abort the restore, but it must not be silent either.
                LOG.log(Level.WARNING, "recovered file " + file
                        + " has malformed " + malformed.declaredCharset().name()
                        + " content under its byte-order mark; keeping the default encoding", malformed);
                currentFilePath = file;
            } catch (IOException e) {
                // The encoding is unknown, so the next save must ask for a path
                // instead of silently rewriting an unreadable original as UTF-8.
                LOG.log(Level.WARNING, "could not detect encoding of recovered file " + file, e);
            }
        }
        // Nothing on disk to compare against: the draft stays dirty even when it
        // is empty, so autosave never clears a recovered empty draft.
        savedContent = null;
        currentContent = content;
        dirty = true;
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

    /** Everything a save needs from the FX thread's current document, captured once. */
    private record SaveContext(long operationId, Path path, String content, Charset charset,
                               boolean bom, String expectedCurrent, boolean unchanged) {
    }

    private SaveContext contextFor(Path path, String content) {
        return new SaveContext(operation, path, content, currentCharset, currentBom,
                currentContent, path.equals(currentFilePath) && content.equals(savedContent));
    }

    private Optional<Path> writeBlocking(javafx.stage.Stage stage, Path path, String content) {
        var context = contextFor(path, content);
        notifySavingChanged(true);
        try {
            WriteResult result;
            try {
                result = ioExecutor.submit("save " + path,
                        () -> writeAttempt(context)).join();
            } catch (CompletionException failure) {
                result = new WriteFailed(messageOf(failure));
            }
            return finishBlocking(stage, context, result);
        } finally {
            notifySavingChanged(false);
        }
    }

    private Optional<Path> finishBlocking(javafx.stage.Stage stage, SaveContext context,
                                          WriteResult result) {
        if (result instanceof Written) {
            return commitSave(context);
        }
        if (result instanceof UpgradeRequired) {
            if (!dialogService.showEncodingUpgradeDialog(stage, context.charset())) {
                return Optional.empty(); // nothing written: the document stays dirty
            }
            var upgraded = new SaveContext(context.operationId(), context.path(), context.content(),
                    StandardCharsets.UTF_8, context.bom(), context.expectedCurrent(), false);
            WriteResult retry;
            try {
                retry = ioExecutor.submit("save " + context.path() + " as UTF-8",
                        () -> writeAttempt(upgraded)).join();
            } catch (CompletionException failure) {
                retry = new WriteFailed(messageOf(failure));
            }
            if (retry instanceof Written) {
                return commitSave(upgraded);
            }
            return reportFailure(stage, retry);
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
                .thenCompose(result -> switch (result) {
                    case Written written -> ioExecutor.completeOnFx(() -> commitSave(context));
                    case UpgradeRequired upgrade -> ioExecutor
                            .completeOnFx(() -> dialogService.showEncodingUpgradeDialog(
                                    stage, context.charset()))
                            .thenCompose(accepted -> accepted
                                    ? writeAttemptAsync(stage, new SaveContext(
                                            context.operationId(), context.path(), context.content(),
                                            StandardCharsets.UTF_8, context.bom(),
                                            context.expectedCurrent(), false))
                                    : CompletableFuture.completedFuture(Optional.<Path>empty()));
                    case WriteFailed failure -> ioExecutor
                            .completeOnFx(() -> reportFailure(stage, failure));
                });
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
            // Re-saving unchanged content would rotate the backup history with
            // copies of the same text; the encoding-upgrade flow always has
            // content != savedContent, so it is unaffected by this guard.
            // The document write is the primary operation; the backup protects
            // the previous version but must never be able to cancel the save.
            // The copy is therefore staged first, but failures only warn.
            BackupKeeper.PreparedBackup prepared = null;
            if (!context.unchanged()) {
                try {
                    prepared = backupKeeper.prepare(path).orElse(null);
                } catch (IOException | RuntimeException backupFailure) {
                    LOG.log(Level.WARNING, "could not stage a backup of " + path
                            + "; saving without it", backupFailure);
                }
            }
            try {
                AtomicFiles.writeMoving(path, bytes);
            } catch (IOException | RuntimeException writeFailure) {
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
            return Written.INSTANCE;
        } catch (IOException | RuntimeException e) {
            // An unexpected runtime failure must not take the app down mid-save:
            // log it and let the user retry from a dialog.
            LOG.log(Level.WARNING, "save failed for " + path, e);
            return new WriteFailed(e.getMessage());
        }
    }

    private Optional<Path> commitSave(SaveContext context) {
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
        return failure.getCause() == null ? failure.getMessage() : failure.getCause().getMessage();
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

    /** The shared I/O seam; package-visible so {@link AutoSaveService} can serialise with it. */
    IoExecutor ioExecutor() {
        return ioExecutor;
    }

    // --- results of the blocking parts, free of UI types --------------------

    private sealed interface WriteResult permits Written, UpgradeRequired, WriteFailed {
    }

    private record Written() implements WriteResult {
        static final Written INSTANCE = new Written();
    }

    private record UpgradeRequired() implements WriteResult {
        static final UpgradeRequired INSTANCE = new UpgradeRequired();
    }

    private record WriteFailed(String message) implements WriteResult {
    }

    private record ReadResult(byte[] bytes, IOException failure) {
    }
}
