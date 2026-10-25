package org.mesutormanli.jtypewriter.service;

import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.AtomicFiles;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
 *       {@code <file>.bak} (plus {@code .bak.1/.bak.2} generations);</li>
 *   <li><b>Encoding stability:</b> a document is read with encoding detection
 *       (BOM / strict UTF-8, otherwise the user is asked - see
 *       {@link TextCodec}) and always written back in that same encoding.
 *       Only when the text no longer fits that encoding does the user get to
 *       switch the document to UTF-8 - unmappable characters are never
 *       silently turned into {@code ?}.</li>
 * </ul>
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
    }

    private static final Logger LOG = AppLogging.logger(FileService.class);

    private final DialogService dialogService;
    private final Messages messages;
    private final BackupKeeper backupKeeper = new BackupKeeper();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private Path currentFilePath;
    private String savedContent = "";
    private String currentContent = "";
    private Charset currentCharset = StandardCharsets.UTF_8;
    private boolean currentBom;

    public FileService(DialogService dialogService, Messages messages) {
        this.dialogService = dialogService;
        this.messages = messages;
    }

    public String getCurrentContent() {
        return currentContent;
    }

    public void setCurrentContent(String content) {
        this.currentContent = content;
    }

    public boolean hasUnsavedChanges() {
        // savedContent is null when the draft was never on disk (restoreRecovered):
        // equals(null) is false, so even an empty draft stays dirty
        return !currentContent.equals(savedContent);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Opens a file only after the current document is safe. Unsaved changes
     * trigger the same Save / Don't Save / Cancel prompt used on exit; choosing
     * Cancel - or abandoning the save - aborts the open so no text is ever lost.
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
        currentFilePath = null;
        currentCharset = StandardCharsets.UTF_8;
        currentBom = false;
        savedContent = "";
        currentContent = "";
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

    /**
     * Loads a document that is already known - the file picker has been
     * answered, or the path comes from session restore. The encoding prompt
     * still applies, so a legacy document is never silently mangled.
     */
    public Optional<Path> loadFile(javafx.stage.Stage stage, Path path) {
        try {
            var decoded = decode(Files.readAllBytes(path), stage, path);
            if (decoded == null) {
                return Optional.empty(); // unsupported encoding: user cancelled
            }
            currentFilePath = path;
            currentCharset = decoded.charset();
            currentBom = decoded.bom();
            savedContent = decoded.text();
            currentContent = decoded.text();
            listeners.forEach(l -> l.onLoaded(path));
            return Optional.of(path);
        } catch (IOException e) {
            dialogService.showError(stage, messages.errorOpen(), e.getMessage());
            return Optional.empty();
        }
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
    }

    public Optional<Path> saveFile(javafx.stage.Stage stage, String content) {
        if (currentFilePath == null) {
            return saveFileAs(stage, content);
        }
        return writeFile(stage, currentFilePath, content);
    }

    public Optional<Path> saveFileAs(javafx.stage.Stage stage, String content) {
        var file = dialogService.showSaveDialog(stage, currentFilePath);
        if (file.isEmpty()) return Optional.empty();

        return writeFile(stage, file.get().toPath(), content);
    }

    /**
     * Writes the document in its own encoding. When the text no longer fits
     * that encoding (e.g. a pasted emoji in a windows-1254 document), the user
     * is asked to switch the document to UTF-8 - the unmappable characters are
     * never silently replaced with {@code ?}.
     */
    private Optional<Path> writeFile(javafx.stage.Stage stage, Path path, String content) {
        byte[] bytes;
        try {
            bytes = TextCodec.encode(content, currentCharset, currentBom);
        } catch (CharacterCodingException notRepresentable) {
            if (!dialogService.showEncodingUpgradeDialog(stage, currentCharset)) {
                return Optional.empty(); // nothing written: the document stays dirty
            }
            currentCharset = StandardCharsets.UTF_8; // the file is rewritten in UTF-8
            try {
                bytes = TextCodec.encode(content, currentCharset, currentBom);
            } catch (CharacterCodingException impossible) {
                // UTF-8 represents every character: unreachable in practice
                dialogService.showError(stage, messages.errorSave(), impossible.getMessage());
                return Optional.empty();
            }
        }
        try {
            // Re-saving unchanged content would rotate the backup history with
            // copies of the same text; the encoding-upgrade flow always has
            // content != savedContent, so it is unaffected by this guard.
            var unchangedSave = path.equals(currentFilePath) && content.equals(savedContent);
            if (!unchangedSave) {
                backupKeeper.backup(path);
            }
            AtomicFiles.writeMoving(path, bytes);
            currentFilePath = path;
            savedContent = content;
            currentContent = content;
            listeners.forEach(l -> l.onSaved(path));
            return Optional.of(path);
        } catch (IOException | RuntimeException e) {
            // An unexpected runtime failure must not take the app down mid-save:
            // log it and let the user retry from a dialog.
            LOG.log(Level.WARNING, "save failed for " + path, e);
            dialogService.showError(stage, messages.errorSave(), e.getMessage());
            return Optional.empty();
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

    public void promptSaveOnExit(javafx.stage.Stage stage) {
        var choice = dialogService.showUnsavedChangesDialog(stage);
        if (choice == DialogService.SaveChoice.SAVE) {
            saveFile(stage, currentContent).ifPresent(p -> stage.close());
        } else if (choice == DialogService.SaveChoice.DISCARD) {
            stage.close();
        }
    }

    public Path getCurrentFilePath() {
        return currentFilePath;
    }
}