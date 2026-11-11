/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.mesutormanli.jtypewriter.AppDirs;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.AtomicFiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Crash-recovery draft storage. Every {@code RecoveryStore} instance owns one
 * draft slot: a single UTF-8 file named {@code draft-<UUID>.jtw} inside the
 * store's directory. The UUID is generated in the constructor, so two running
 * application instances never overwrite each other's draft.
 *
 * <p>The file holds both the metadata and the document text, which makes the
 * pair a single atomic unit: a crash can no longer leave a timestamp without
 * its text or text without its timestamp. The layout is:</p>
 *
 * <pre>
 * JTypeWriter-Draft/3
 * saved.at=&lt;Instant.toString()&gt;
 * original.path.b64=&lt;Base64 of the absolute path, or empty&gt;
 * cursor=&lt;caret offset, -1 when unknown&gt;
 * charset=&lt;charset name, empty when unknown&gt;
 * bom=&lt;true or false&gt;
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
 * <p>A {@code /2} draft has no {@code cursor}/{@code charset}/{@code bom}
 * header keys. It is still read as a complete draft - text, path and time -
 * with the metadata reported as unknown; the reader must never mistake an
 * older file for a plain text blob. Line endings need no header field: the
 * payload is the verbatim document, so a CRLF document round-trips
 * byte-exactly.</p>
 *
 * <p>The original path is the target of the next save, so it is validated
 * before it is offered: it must be absolute, canonicalise and live under one
 * of the store's allowed roots, and it must not point into the application's
 * own data directory (see {@link RecoveryTargetPath}). A draft that fails the
 * check is still restored - without a path, which forces Save As - so a
 * foreign or tampered draft cannot redirect a save.</p>
 *
 * <p>Loading never discards recoverable text. A file without the magic
 * line, without the separator, or with an unparsable timestamp or path
 * degrades gracefully: the text is still offered, the broken metadata is
 * replaced by the file's last-modified time or by {@code null}, and a warning
 * is logged. Invalid UTF-8 is read with replacement characters instead of
 * being dropped. A single unreadable candidate never hides the others; the
 * newest surviving draft wins.</p>
 *
 * <p>Two exceptions to "never discards", both deliberate and both logged at
 * {@code WARNING}, because the alternatives are worse:</p>
 * <ul>
 *   <li><b>Symlinks are not drafts.</b> The scan filters with
 *       {@code NOFOLLOW_LINKS}, so a {@code draft-*.jtw} that is really a link
 *       to an arbitrary file (a key material file, a document the user never
 *       opened) is not read at all. Reading it would present someone else's
 *       content to the user as "your recovered draft" - the one place where a
 *       file name in a directory the user does not watch can decide what the
 *       product shows them.</li>
 *   <li><b>Oversized files are skipped.</b> A draft is a copy of the whole
 *       document, so a file above {@link #MAX_DRAFT_BYTES} was not written by
 *       this application and would be read into memory at startup for nothing.
 *       Offering a truncated prefix instead would be worse than skipping: a
 *       restored document that looks complete but is not, and can then be saved
 *       over the original, is a data-safety hazard disguised as a recovery.</li>
 * </ul>
 *
 * <p>Housekeeping: both {@code draft-*.jtw} and {@code pending-*.jtw} are
 * expired by age, count and total size on every scan
 * ({@link RecoveryRetention}). A live draft used to be exempt because it might
 * belong to a second instance, but that left every crashed session's draft on
 * disk forever. The safety rules are applied here, where the caller knows
 * which file is its own and when the directory was last written: this
 * session's own file is never a candidate, and neither is any file whose mtime
 * is within {@link #LIVE_DRAFT_GRACE} - another instance refreshes its draft
 * every 30 seconds, so a recently written file may still be someone's only
 * copy. Only after those filters does the pure retention policy see the
 * remaining entries.</p>
 *
 * <p>Housekeeping also sweeps abandoned {@code jtw-*.tmp} files. A hard kill
 * during an atomic write can leave one in the recovery directory forever;
 * anything older than {@link #ABANDONED_TEMP_AGE} is deleted, anything newer
 * is left alone because another instance may be mid-write.</p>
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

    /**
     * A recovered draft: its text, the document it belongs to (may be
     * {@code null}), its age, and the editor state that would otherwise be
     * lost.
     *
     * <p>{@code cursor} is a caret offset, {@code -1} when the draft predates
     * the metadata. {@code charset} is {@code null} when the draft did not
     * record one; callers treat that as "not recorded" and fall back to the
     * document on disk, exactly as a {@code /2} draft behaves. {@code bom}
     * means the text was written with a byte-order mark and is only meaningful
     * together with a recorded charset.</p>
     */
    public record Draft(String content, Path originalFile, Instant savedAt,
                        int cursor, Charset charset, boolean bom) {

        /**
         * A draft without editor metadata. Kept so callers and tests that only
         * care about text, path and time do not have to spell out the unknown
         * values.
         */
        public Draft(String content, Path originalFile, Instant savedAt) {
            this(content, originalFile, savedAt, -1, null, false);
        }
    }

    private static final Logger LOG = AppLogging.logger(RecoveryStore.class);

    private static final String MAGIC = "JTypeWriter-Draft/3";
    private static final String MAGIC_V2 = "JTypeWriter-Draft/2";
    private static final byte[] MAGIC_BYTES = MAGIC.getBytes(StandardCharsets.UTF_8);
    private static final byte[] MAGIC_V2_BYTES = MAGIC_V2.getBytes(StandardCharsets.UTF_8);
    private static final String FILE_PREFIX = "draft-";
    private static final String PARKED_PREFIX = "pending-";
    private static final String FILE_SUFFIX = ".jtw";
    private static final String TEMP_PREFIX = "jtw-";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final String KEY_SAVED_AT = "saved.at";
    private static final String KEY_ORIGINAL_PATH = "original.path.b64";
    private static final String KEY_CURSOR = "cursor";
    private static final String KEY_CHARSET = "charset";
    private static final String KEY_BOM = "bom";

    /**
     * How recently another instance may have written a draft. Live sessions
     * refresh every 30 seconds, so ten minutes covers several missed ticks plus
     * a stalled I/O worker; expiring inside that window could delete the only
     * copy of another process's text.
     */
    static final Duration LIVE_DRAFT_GRACE = Duration.ofMinutes(10);

    /**
     * A temp file this old cannot belong to a write still in progress; an hour
     * is far beyond the time an atomic draft write takes.
     */
    static final Duration ABANDONED_TEMP_AGE = Duration.ofHours(1);

    /**
     * Upper bound for a file this class is willing to read into memory. A draft
     * is a verbatim copy of a document that was open in an editor, so 64 MiB is
     * already far beyond a document a person types; anything larger is a stray
     * file that would otherwise be read at startup, on the UI thread, before
     * the window has finished appearing. See the class Javadoc for why skipping
     * beats offering a truncated prefix.
     */
    static final int MAX_DRAFT_BYTES = 64 * 1024 * 1024;

    private final Path dir;
    private final Path ownFile;
    private final Set<Path> allowedRoots;

    /**
     * Production store: a draft's original path is accepted only under the
     * user's home directory. Documents kept elsewhere (a mounted volume, a
     * removable disk) are still recovered, just without a path - saving them
     * asks for a location instead of trusting a header.
     */
    public RecoveryStore(Path dir) {
        this(dir, defaultAllowedRoots());
    }

    /**
     * Test seam for the root policy: the roots are passed explicitly so the
     * check can be exercised against a temporary directory instead of the real
     * home directory.
     */
    public RecoveryStore(Path dir, Collection<Path> allowedRoots) {
        this.dir = dir;
        this.ownFile = dir.resolve(FILE_PREFIX + UUID.randomUUID() + FILE_SUFFIX);
        this.allowedRoots = allowedRoots == null ? Set.of() : Set.copyOf(allowedRoots);
    }

    private static Set<Path> defaultAllowedRoots() {
        var home = System.getProperty("user.home");
        return home == null || home.isBlank() ? Set.of() : Set.of(Path.of(home));
    }

    /**
     * Stores (or refreshes) this session's single draft slot without editor
     * metadata.
     *
     * @see #saveDraft(String, Path, int, Charset, boolean)
     */
    public synchronized void saveDraft(String content, Path originalFile) throws IOException {
        saveDraft(content, originalFile, -1, null, false);
    }

    /**
     * Stores (or refreshes) this session's single draft slot.
     *
     * <p>The caret and encoding are captured by the caller on the FX thread
     * before the write is submitted, because by the time the I/O worker runs
     * the editor may already have moved on. {@code charset} may be
     * {@code null} to mean "not recorded".</p>
     */
    public synchronized void saveDraft(String content, Path originalFile, int cursor,
                                       Charset charset, boolean bom) throws IOException {
        Files.createDirectories(dir);
        var draft = new Draft(content,
                originalFile == null ? null : originalFile.toAbsolutePath(),
                Instant.now(), cursor, charset, bom);
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

        List<Path> entries;
        try (var listing = Files.list(dir)) {
            entries = listing.toList();
        } catch (IOException e) {
            LOG.log(Level.FINE, "cannot list recovery drafts in " + dir, e);
            return Optional.empty();
        }

        // Housekeeping runs before the scan so an expired draft is never even
        // read: expiring a file and then offering it would be the same as not
        // expiring it at all. Temp leftovers are swept first so a failed sweep
        // can never take the draft scan down with it.
        sweepAbandonedTemps(entries);
        var expired = expiredDrafts(entries);
        var candidates = new ArrayList<Candidate>();
        for (var file : entries) {
            var name = file.getFileName().toString();
            if (!isDraftFile(name) || expired.contains(file)) {
                continue;
            }
            // NOFOLLOW_LINKS: a link is not a draft. Reading through it would
            // offer an arbitrary file's content as recovered text.
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                LOG.log(Level.WARNING, "ignoring recovery entry that is not a regular file: " + file);
                continue;
            }
            var candidate = parse(file);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        var winner = newest(candidates);
        if (winner == null) {
            return Optional.empty();
        }
        adoptIfForeign(winner);
        return Optional.of(winner.draft());
    }

    /**
     * Deletes the recovery drafts {@link RecoveryRetention} no longer keeps and
     * returns their paths. Applies to both live {@code draft-*} and parked
     * {@code pending-*} files.
     *
     * <p>Two files are never handed to the policy, because the pure function
     * cannot know about them:</p>
     * <ul>
     *   <li>this session's own slot - deleting it would drop the text the
     *       current document just snapshotted;</li>
     *   <li>any file written within {@link #LIVE_DRAFT_GRACE} - another
     *       instance refreshes its live draft every 30 seconds, so a recent
     *       mtime means the file may still be that process's only copy.</li>
     * </ul>
     *
     * <p>Never fatal: housekeeping that fails only means the next start tries
     * again, and a directory it cannot delete from must not turn into a
     * startup failure.</p>
     */
    private Set<Path> expiredDrafts(List<Path> entries) {
        var now = Instant.now();
        var recentCutoff = now.minus(LIVE_DRAFT_GRACE);
        var candidates = new ArrayList<RecoveryRetention.Entry>();
        for (var file : entries) {
            if (!isDraftFile(file.getFileName().toString())
                    || file.equals(ownFile)
                    || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            var modified = lastModified(file);
            if (!modified.isBefore(recentCutoff)) {
                continue; // may be a live draft of another running instance
            }
            candidates.add(entryFor(file, modified));
        }
        var expired = new LinkedHashSet<Path>();
        for (var file : RecoveryRetention.expired(candidates, now)) {
            try {
                Files.deleteIfExists(file);
                expired.add(file); // known to be gone, whether or not it ever existed
                LOG.log(Level.INFO, "expired recovery draft " + file);
            } catch (IOException | SecurityException e) {
                LOG.log(Level.FINE, "could not expire recovery draft " + file, e);
            }
        }
        return expired;
    }

    /** A draft's age comes from its mtime: nothing has rewritten it since. */
    private static RecoveryRetention.Entry entryFor(Path file, Instant modified) {
        long size;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            size = 0; // unknown size: age and count still bound the set
        }
        return new RecoveryRetention.Entry(file, modified, size);
    }

    /**
     * Deletes abandoned atomic-write temp files. A hard kill between creating
     * {@code jtw-*-*.tmp} and moving it into place leaves the file behind, and
     * nothing else ever reclaims it. Only files older than
     * {@link #ABANDONED_TEMP_AGE} are touched: a newer one may belong to
     * another instance whose write is in flight.
     *
     * <p>Best-effort by design - a sweep failure is logged at {@code FINE} and
     * the scan continues, because a stray temp file is clutter, not recovered
     * text.</p>
     */
    private void sweepAbandonedTemps(List<Path> entries) {
        var cutoff = Instant.now().minus(ABANDONED_TEMP_AGE);
        for (var file : entries) {
            var name = file.getFileName().toString();
            if (!name.startsWith(TEMP_PREFIX) || !name.endsWith(TEMP_SUFFIX)) {
                continue;
            }
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            if (!lastModified(file).isBefore(cutoff)) {
                continue; // another instance may be mid-write
            }
            try {
                Files.deleteIfExists(file);
                LOG.log(Level.INFO, "deleted abandoned recovery temp file " + file);
            } catch (IOException | SecurityException e) {
                LOG.log(Level.FINE, "could not delete abandoned recovery temp file " + file, e);
            }
        }
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
     * would be offered for recovery again and could overwrite a newer document,
     * so it is reported at {@code WARNING} and returned to the caller instead
     * of being logged at {@code FINE}.
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
        return isDraftFile(file.getFileName().toString());
    }

    private static boolean isDraftFile(String name) {
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
     *         or is larger than {@link #MAX_DRAFT_BYTES}
     */
    private Candidate parse(Path file) {
        byte[] bytes;
        try {
            bytes = readBounded(file);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "skipping unreadable recovery draft " + file, e);
            return null;
        }
        if (bytes == null) {
            return null; // oversized; readBounded has already warned
        }
        var modified = lastModified(file);

        var headerStart = magicLength(bytes);
        if (headerStart < 0) {
            LOG.log(Level.WARNING, "recovery draft without a valid header, treating it as text: " + file);
            return new Candidate(new Draft(decode(bytes), null, modified), file, modified);
        }
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
        var cursor = -1;
        Charset charset = null;
        var bom = false;
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
            } else if (KEY_CURSOR.equals(key)) {
                cursor = parseCursor(value);
            } else if (KEY_CHARSET.equals(key)) {
                charset = parseCharset(file, value);
            } else if (KEY_BOM.equals(key)) {
                bom = "true".equals(value.trim());
            }
        }
        return new Candidate(new Draft(decode(contentBytes), original, savedAt, cursor, charset, bom),
                file, modified);
    }

    /**
     * A caret is a hint, never a reason to hide the text: an unreadable value
     * becomes {@code -1}, which the caller treats as "no caret recorded".
     */
    private static int parseCursor(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * An unknown charset name only loses the encoding hint. Every
     * {@link Charset#forName} failure - an unknown name, an illegal one - is
     * caught here so a bad metadata line can never abort the draft parse.
     */
    private static Charset parseCharset(Path file, String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return Charset.forName(value.trim());
        } catch (IllegalArgumentException e) { // includes UnsupportedCharsetException
            LOG.log(Level.FINE, "unknown charset in recovery draft, dropping the hint: " + file, e);
            return null;
        }
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
    private Path parseOriginalPath(Path file, String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            var decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            if (decoded.isBlank()) {
                return null;
            }
            // The decoded value becomes the next save's target; a draft from
            // another machine must not be able to name an arbitrary file.
            return RecoveryTargetPath.accept(Path.of(decoded), allowedRoots,
                    List.of(AppDirs.dataDir()));
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

    /**
     * Reads a draft, refusing anything above {@link #MAX_DRAFT_BYTES}.
     *
     * <p>The size check comes first, so an enormous file is never opened at
     * all; the bounded read after it covers a file that grows between the check
     * and the read, and keeps a lying or racing sparse file from turning into an
     * {@code OutOfMemoryError} on the startup thread.</p>
     *
     * @return the bytes, or {@code null} when the file is too large
     */
    private static byte[] readBounded(Path file) throws IOException {
        var size = Files.size(file);
        if (size > MAX_DRAFT_BYTES) {
            LOG.log(Level.WARNING, "skipping recovery draft larger than " + MAX_DRAFT_BYTES
                    + " bytes (" + size + "): " + file);
            return null;
        }
        try (var in = Files.newInputStream(file)) {
            var bytes = in.readNBytes(MAX_DRAFT_BYTES + 1);
            if (bytes.length > MAX_DRAFT_BYTES) {
                LOG.log(Level.WARNING, "skipping recovery draft larger than " + MAX_DRAFT_BYTES
                        + " bytes, read from: " + file);
                return null;
            }
            return bytes;
        }
    }

    /**
     * @return the length of the recognised magic prefix, or {@code -1} when the
     *         file has none. The length matters because the header starts right
     *         after the magic: a {@code /2} file's shorter magic must not shift
     *         its header by the {@code /3} length.
     */
    private static int magicLength(byte[] bytes) {
        if (bytes.length >= MAGIC_BYTES.length
                && Arrays.equals(bytes, 0, MAGIC_BYTES.length, MAGIC_BYTES, 0, MAGIC_BYTES.length)) {
            return MAGIC_BYTES.length;
        }
        if (bytes.length >= MAGIC_V2_BYTES.length
                && Arrays.equals(bytes, 0, MAGIC_V2_BYTES.length, MAGIC_V2_BYTES, 0, MAGIC_V2_BYTES.length)) {
            return MAGIC_V2_BYTES.length;
        }
        return -1;
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
     * still more useful than a deleted one.
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
                + KEY_ORIGINAL_PATH + "=" + encodedPath + "\n"
                + KEY_CURSOR + "=" + draft.cursor() + "\n"
                + KEY_CHARSET + "=" + (draft.charset() == null ? "" : draft.charset().name()) + "\n"
                + KEY_BOM + "=" + draft.bom() + "\n\n";
        var out = new ByteArrayOutputStream();
        out.writeBytes(header.getBytes(StandardCharsets.UTF_8));
        out.writeBytes(draft.content().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
