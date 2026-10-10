package org.mesutormanli.jtypewriter.service;

import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Document I/O with data-safety guarantees:
 * <ul>
 *   <li><b>Atomic writes:</b> content is written to a temp file and moved into
 *       place, so a crash mid-save can never leave a truncated document;</li>
 *   <li><b>Rotating backups:</b> every overwrite keeps the previous version as
 *       {@code <file>.bak} (plus {@code .bak.1/.bak.2} generations);</li>
 *   <li><b>Encoding stability:</b> a document is read with encoding detection
 *       (BOM / strict UTF-8, otherwise the user is asked - see
 *       {@link TextCodec}) and always written back in that same encoding.</li>
 * </ul>
 */
public class FileService {

    /** Observer hooks for the document lifecycle (autosave, session stats). */
    public interface Listener {
        default void onLoaded(Path file) {
        }

        default void onSaved(Path file) {
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
        if (hasUnsavedChanges()) {
            switch (dialogService.showUnsavedChangesDialog(stage)) {
                case SAVE -> {
                    if (saveFile(stage, currentContent).isEmpty()) {
                        return Optional.empty(); // save cancelled or failed: keep the draft
                    }
                }
                case DISCARD -> {
                    // the user explicitly accepted losing the draft
                }
                case CANCEL -> {
                    return Optional.empty();
                }
            }
        }

        var file = dialogService.showOpenDialog(stage);
        if (file.isEmpty()) return Optional.empty();

        var path = file.get().toPath();
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
            dialogService.showError(messages.errorOpen(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Re-attaches a recovered draft as a dirty document. The encoding of the
     * on-disk original (when still present) is reused so a subsequent save
     * preserves it.
     */
    public void restoreRecovered(Path file, String content) {
        if (file != null && Files.isRegularFile(file)) {
            try {
                var onDisk = TextCodec.decode(Files.readAllBytes(file));
                if (onDisk != null) {
                    currentCharset = onDisk.charset();
                    currentBom = onDisk.bom();
                }
                currentFilePath = file;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "could not detect encoding of recovered file " + file, e);
            }
        }
        savedContent = ""; // force the dirty flag: the draft is not on disk yet
        currentContent = content;
    }

    public Optional<Path> saveFile(javafx.stage.Stage stage, String content) {
        if (currentFilePath == null) {
            return saveFileAs(stage, content);
        }
        return writeFile(currentFilePath, content);
    }

    public Optional<Path> saveFileAs(javafx.stage.Stage stage, String content) {
        var file = dialogService.showSaveDialog(stage);
        if (file.isEmpty()) return Optional.empty();

        return writeFile(file.get().toPath(), content);
    }

    private Optional<Path> writeFile(Path path, String content) {
        try {
            backupKeeper.backup(path);
            writeAtomically(path, TextCodec.encode(content, currentCharset, currentBom));
            currentFilePath = path;
            savedContent = content;
            currentContent = content;
            listeners.forEach(l -> l.onSaved(path));
            return Optional.of(path);
        } catch (IOException e) {
            dialogService.showError(messages.errorSave(), e.getMessage());
            return Optional.empty();
        }
    }

    /** Temp-file-then-move so an interrupted save leaves the old document intact. */
    private static void writeAtomically(Path path, byte[] bytes) throws IOException {
        var absolute = path.toAbsolutePath();
        var temp = Files.createTempFile(absolute.getParent(), absolute.getFileName().toString(), ".tmp");
        var moved = false;
        try {
            Files.write(temp, bytes);
            try {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temp);
            }
        }
    }

    /** @return the decoded document, or {@code null} when the user cancelled the encoding prompt */
    private TextCodec.Result decode(byte[] bytes, javafx.stage.Stage stage, Path path) {
        var decoded = TextCodec.decode(bytes);
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