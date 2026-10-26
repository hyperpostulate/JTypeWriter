package org.mesutormanli.jtypewriter.service;

import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.AtomicFiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Crash-recovery draft storage. Every {@code RecoveryStore} instance owns one
 * draft slot: a single UTF-8 file named {@code draft-<UUID>.jtw} inside the
 * store's directory. The UUID is generated in the constructor, so two running
 * application instances never overwrite each other's draft (RW-04).
 *
 * <p>The file holds both the metadata and the document text, which makes the
 * pair a single atomic unit: a crash can no longer leave a timestamp without
 * its text or text without its timestamp (RW-03). The layout is:</p>
 *
 * <pre>
 * JTypeWriter-Draft/2
 * saved.at=&lt;Instant.toString()&gt;
 * original.path.b64=&lt;Base64 of the absolute path, or empty&gt;
 *
 * &lt;document text, verbatim&gt;
 * </pre>
 *
 * <p>The header is separated from the text by exactly one blank line. The
 * original path is Base64-encoded so separators, newlines, NUL characters,
 * non-ASCII names and paths written on another platform cannot corrupt the
 * header. The text bytes after the separator are stored untouched. Everything
 * is written through {@link AtomicFiles#writeMoving}, so a crash or power cut
 * cannot leave a truncated draft behind.</p>
 *
 * <p>Loading never discards recoverable text (RW-02). A file without the magic
 * line, without the separator, or with an unparsable timestamp or path
 * degrades gracefully: the text is still offered, the broken metadata is
 * replaced by the file's last-modified time or by {@code null}, and a warning
 * is logged. Invalid UTF-8 is read with replacement characters instead of
 * being dropped. A single unreadable candidate never hides the others; the
 * newest surviving draft wins.</p>
 *
 * <p>Adoption: the store has one slot, so when the newest draft belongs to
 * another session's file, loading copies it into this session's file and
 * deletes the old one. Later {@link #clear()} calls and snapshots then target
 * this session's file only, without any change in {@code AutoSaveService}.</p>
 *
 * <p>Parking: a draft the user postponed ("ask later") is moved out of the
 * active slot into {@code pending-<UUID>.jtw} by {@link #park()}. Parked files
 * are ordinary drafts for reading - {@link #loadDraft()} considers them - but
 * nothing ever writes them, so the session can keep snapping the document the
 * user is actually editing instead of going silent. Without parking the single
 * slot forced a choice between overwriting the postponed text and not writing
 * a recovery copy at all; parking removes that choice. {@link #clear()} is
 * scoped to this session's slot, so a parked draft survives a clean exit.</p>
 *
 * <p>Lifecycle: the draft is refreshed periodically while a document is dirty,
 * cleared on every successful save and on clean exit. If the application dies
 * in between, the draft survives and is offered for recovery on next startup.
 * A clear that fails is not silent: the slot still holds the draft afterwards,
 * {@link #clear()} reports that, and the stale copy is offered again on the
 * next start unless the caller acts on the failure. Free of UI types so it can
 * be unit tested headlessly.</p>
 */
public class RecoveryStore {

    /** A recovered draft: its text, the document it belongs to (may be {@code null}) and age. */
    public record Draft(String content, Path originalFile, Instant savedAt) {
    }

    private static final Logger LOG = AppLogging.logger(RecoveryStore.class);

    private static final String MAGIC = "JTypeWriter-Draft/2";
    private static final byte[] MAGIC_BYTES = MAGIC.getBytes(StandardCharsets.UTF_8);
    private static final String FILE_PREFIX = "draft-";
    private static final String PARKED_PREFIX = "pending-";
    private static final String FILE_SUFFIX = ".jtw";
    private static final String KEY_SAVED_AT = "saved.at";
    private static final String KEY_ORIGINAL_PATH = "original.path.b64";

    private final Path dir;
    private final Path ownFile;

    public RecoveryStore(Path dir) {
        this.dir = dir;
        this.ownFile = dir.resolve(FILE_PREFIX + UUID.randomUUID() + FILE_SUFFIX);
    }

    /** Stores (or refreshes) this session's single draft slot. */
    public synchronized void saveDraft(String content, Path originalFile) throws IOException {
        Files.createDirectories(dir);
        var draft = new Draft(content, originalFile == null ? null : originalFile.toAbsolutePath(), Instant.now());
        AtomicFiles.writeMoving(ownFile, serialize(draft));
    }

    /**
     * @return the newest surviving draft, if any, including drafts a previous
     *         session parked. When it comes from another file, it is adopted
     *         into this session's slot first, so later {@link #clear()} calls
     *         only affect this session.
     */
    public synchronized Optional<Draft> loadDraft() {
        if (!Files.isDirectory(dir)) {
            LOG.log(Level.FINE, "recovery directory does not exist: " + dir);
            return Optional.empty();
        }

        var candidates = new ArrayList<Candidate>();
        try (var entries = Files.list(dir)) {
            entries.filter(Files::isRegularFile)
                    .filter(RecoveryStore::isDraftFile)
                    .forEach(file -> {
                        var candidate = parse(file);
                        if (candidate != null) {
                            candidates.add(candidate);
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.FINE, "cannot list recovery drafts in " + dir, e);
            return Optional.empty();
        }

        var winner = newest(candidates);
        if (winner == null) {
            return Optional.empty();
        }
        adoptIfForeign(winner);
        return Optional.of(winner.draft());
    }

    /**
     * Moves this session's draft out of the active slot so that later snapshots
     * cannot overwrite it. Used for a draft the user postponed: the text stays
     * readable by {@link #loadDraft()} (in this session and the next one) while
     * the slot is free for the document the user is editing now.
     *
     * @return {@code true} when the slot holds no draft afterwards (there was
     *         nothing to park, or the move succeeded); {@code false} only when
     *         the move failed, in which case the caller must keep the slot
     *         frozen - overwriting the only copy of the text is worse than not
     *         snapping
     */
    public synchronized boolean park() {
        if (!Files.isRegularFile(ownFile)) {
            return true;
        }
        var parked = dir.resolve(PARKED_PREFIX + UUID.randomUUID() + FILE_SUFFIX);
        try {
            move(ownFile, parked);
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "could not park recovery draft " + ownFile, e);
            return false;
        }
    }

    /** Rename inside the store directory; the source must survive a failed move. */
    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Drops this session's draft (successful save, new document, or an answered
     * exit). A failing delete is a real condition, not noise: the stale draft
     * would be offered for recovery again and could overwrite a newer document
     * (V17/PW-28), so it is reported at {@code WARNING} and returned to the
     * caller instead of being logged at {@code FINE}.
     *
     * @return {@code true} when the slot holds no draft afterwards (there was
     *         nothing to delete, or the delete succeeded); {@code false} when
     *         the delete failed and the draft is still on disk
     */
    public synchronized boolean clear() {
        try {
            Files.deleteIfExists(ownFile);
            return true;
        } catch (IOException | SecurityException e) {
            LOG.log(Level.WARNING, "could not delete recovery draft " + ownFile
                    + "; the stale draft will be offered again on the next start and must not"
                    + " overwrite a newer document", e);
            return false;
        }
    }

    /**
     * Moves a draft that belongs to another session into this session's slot.
     * The store has a single slot and adoption keeps later cleanup scoped to
     * this instance. A failed copy is logged and the foreign file is left in
     * place: the parsed text is returned either way.
     */
    private void adoptIfForeign(Candidate winner) {
        if (winner.file().equals(ownFile)) {
            return;
        }
        try {
            AtomicFiles.writeMoving(ownFile, serialize(winner.draft()));
            Files.deleteIfExists(winner.file());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "could not adopt recovery draft " + winner.file(), e);
        }
    }

    private static boolean isDraftFile(Path file) {
        var name = file.getFileName().toString();
        return (name.startsWith(FILE_PREFIX) || name.startsWith(PARKED_PREFIX))
                && name.endsWith(FILE_SUFFIX);
    }

    /** A parsed candidate; {@code draft.savedAt()} and {@code modified} drive ordering. */
    private record Candidate(Draft draft, Path file, Instant modified) {
    }

    private static Candidate newest(List<Candidate> candidates) {
        Candidate best = null;
        for (var candidate : candidates) {
            if (best == null || isNewer(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isNewer(Candidate candidate, Candidate current) {
        var bySavedAt = candidate.draft().savedAt().compareTo(current.draft().savedAt());
        if (bySavedAt != 0) {
            return bySavedAt > 0;
        }
        var byModified = candidate.modified().compareTo(current.modified());
        if (byModified != 0) {
            return byModified > 0;
        }
        return candidate.file().getFileName().toString()
                .compareTo(current.file().getFileName().toString()) > 0;
    }

    /**
     * Parses one draft file in isolation. A broken header never hides the text:
     * metadata falls back to the file's last-modified time or {@code null}.
     *
     * @return the parsed candidate, or {@code null} when the file cannot be read
     */
    private Candidate parse(Path file) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "skipping unreadable recovery draft " + file, e);
            return null;
        }
        var modified = lastModified(file);

        if (!startsWithMagic(bytes)) {
            LOG.log(Level.WARNING, "recovery draft without a valid header, treating it as text: " + file);
            return new Candidate(new Draft(decode(bytes), null, modified), file, modified);
        }

        var headerStart = MAGIC_BYTES.length;
        if (bytes.length <= headerStart || bytes[headerStart] != '\n') {
            LOG.log(Level.WARNING, "recovery draft header is not terminated, treating it as text: " + file);
            return new Candidate(new Draft(decode(bytes), null, modified), file, modified);
        }
        var separator = indexOfBlankLine(bytes, headerStart);
        if (separator < 0) {
            LOG.log(Level.WARNING, "recovery draft has no header separator, treating it as text: " + file);
            return new Candidate(new Draft(decode(bytes), null, modified), file, modified);
        }

        var headerBytes = Arrays.copyOfRange(bytes, headerStart + 1, separator);
        var contentBytes = Arrays.copyOfRange(bytes, separator + 2, bytes.length);
        var savedAt = modified;
        Path original = null;
        for (var line : new String(headerBytes, StandardCharsets.UTF_8).split("\n", -1)) {
            var trimmed = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
            var equals = trimmed.indexOf('=');
            if (equals < 0) {
                continue;
            }
            var key = trimmed.substring(0, equals);
            var value = trimmed.substring(equals + 1);
            if (KEY_SAVED_AT.equals(key)) {
                savedAt = parseSavedAt(file, value, modified);
            } else if (KEY_ORIGINAL_PATH.equals(key)) {
                original = parseOriginalPath(file, value);
            }
        }
        return new Candidate(new Draft(decode(contentBytes), original, savedAt), file, modified);
    }

    private static Instant parseSavedAt(Path file, String value, Instant fallback) {
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            LOG.log(Level.WARNING, "malformed saved.at in recovery draft, using its mtime: " + file, e);
            return fallback;
        }
    }

    /**
     * The original path is optional information. A blank value means "not
     * associated with a file"; otherwise it is Base64-decoded, because the raw
     * path may contain separators, newlines or NUL characters. An unusable
     * value only loses the link to the file, never the draft text.
     */
    private static Path parseOriginalPath(Path file, String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            var decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            return decoded.isBlank() ? null : Path.of(decoded);
        } catch (IllegalArgumentException e) { // bad Base64, or InvalidPathException
            LOG.log(Level.WARNING, "unusable original path in recovery draft, dropping it: " + file, e);
            return null;
        }
    }

    /** @return the index of the blank-line separator, or {@code -1} when absent */
    private static int indexOfBlankLine(byte[] bytes, int from) {
        for (var i = from; i + 1 < bytes.length; i++) {
            if (bytes[i] == '\n' && bytes[i + 1] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private static boolean startsWithMagic(byte[] bytes) {
        return bytes.length >= MAGIC_BYTES.length
                && Arrays.equals(bytes, 0, MAGIC_BYTES.length, MAGIC_BYTES, 0, MAGIC_BYTES.length);
    }

    private static Instant lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "cannot read mtime of recovery draft " + file, e);
            return Instant.EPOCH;
        }
    }

    /**
     * Strict UTF-8 decode with a lossy fallback: a draft with invalid bytes is
     * still more useful than a deleted one (RW-02).
     */
    private static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            LOG.log(Level.WARNING, "recovery draft is not valid UTF-8, replacing broken characters", e);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static byte[] serialize(Draft draft) {
        var encodedPath = draft.originalFile() == null
                ? ""
                : Base64.getEncoder().encodeToString(
                        draft.originalFile().toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8));
        var header = MAGIC + "\n"
                + KEY_SAVED_AT + "=" + draft.savedAt() + "\n"
                + KEY_ORIGINAL_PATH + "=" + encodedPath + "\n\n";
        var out = new ByteArrayOutputStream();
        out.writeBytes(header.getBytes(StandardCharsets.UTF_8));
        out.writeBytes(draft.content().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
