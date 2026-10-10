package org.mesutormanli.jtypewriter.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

/**
 * Crash-recovery draft storage. A draft is a pair of files in a fixed
 * directory: {@code draft.txt} (the unsaved text, UTF-8) and
 * {@code draft.properties} (metadata: original file and save time). Both are
 * written to a temp file and moved into place so a crash can never leave a
 * half-written draft that hides the previous one. Free of UI types so it can be
 * unit tested headlessly.
 *
 * <p>Lifecycle: the draft is refreshed periodically while a document is dirty,
 * cleared on every successful save and on clean exit. If the application dies
 * in between, the draft survives and is offered for recovery on next startup.</p>
 */
public class RecoveryStore {

    /** A recovered draft: its text, the document it belongs to (may be {@code null}) and age. */
    public record Draft(String content, Path originalFile, Instant savedAt) {
    }

    private static final String DRAFT_FILE = "draft.txt";
    private static final String META_FILE = "draft.properties";
    private static final String KEY_SAVED_AT = "saved.at";
    private static final String KEY_ORIGINAL_PATH = "original.path";

    private final Path dir;

    public RecoveryStore(Path dir) {
        this.dir = dir;
    }

    /** Stores (or refreshes) the single draft slot. */
    public synchronized void saveDraft(String content, Path originalFile) throws IOException {
        Files.createDirectories(dir);
        writeMoving(dir.resolve(DRAFT_FILE), writer -> Files.writeString(writer, content, StandardCharsets.UTF_8));

        var meta = new Properties();
        meta.setProperty(KEY_SAVED_AT, Instant.now().toString());
        if (originalFile != null) {
            meta.setProperty(KEY_ORIGINAL_PATH, originalFile.toAbsolutePath().toString());
        }
        writeMoving(dir.resolve(META_FILE), target -> {
            try (OutputStream out = Files.newOutputStream(target)) {
                meta.store(out, "JTypeWriter recovery draft");
            }
        });
    }

    /** @return the surviving draft, if any */
    public synchronized Optional<Draft> loadDraft() {
        var draftFile = dir.resolve(DRAFT_FILE);
        if (!Files.isRegularFile(draftFile)) {
            return Optional.empty();
        }
        try {
            var content = Files.readString(draftFile, StandardCharsets.UTF_8);
            Path original = null;
            var savedAt = Files.getLastModifiedTime(draftFile).toInstant();
            var metaFile = dir.resolve(META_FILE);
            if (Files.isRegularFile(metaFile)) {
                var meta = new Properties();
                try (InputStream in = Files.newInputStream(metaFile)) {
                    meta.load(in);
                }
                var path = meta.getProperty(KEY_ORIGINAL_PATH);
                if (path != null && !path.isBlank()) {
                    original = Path.of(path);
                }
                var stamp = meta.getProperty(KEY_SAVED_AT);
                if (stamp != null && !stamp.isBlank()) {
                    savedAt = Instant.parse(stamp);
                }
            }
            return Optional.of(new Draft(content, original, savedAt));
        } catch (IOException | java.time.format.DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /** Drops the draft (successful save or clean exit). */
    public synchronized void clear() {
        try {
            Files.deleteIfExists(dir.resolve(DRAFT_FILE));
            Files.deleteIfExists(dir.resolve(META_FILE));
        } catch (IOException ignored) {
            // a leftover draft is retried on the next autosave; never fatal
        }
    }

    private static void writeMoving(Path target, Writer writer) throws IOException {
        var temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            writer.write(temp);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    @FunctionalInterface
    private interface Writer {
        void write(Path target) throws IOException;
    }
}