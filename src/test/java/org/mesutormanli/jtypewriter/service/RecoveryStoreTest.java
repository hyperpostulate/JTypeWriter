package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Crash-recovery draft lifecycle: survive crashes, be found, survive corrupt
 * metadata, stay isolated per session and be dropped cleanly.
 */
class RecoveryStoreTest {

    private static final String MAGIC = "JTypeWriter-Draft/2";

    @Test
    void roundTripsDraftWithMetadata(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        var original = dir.resolve("roman.txt");

        store.saveDraft("yarım kalmış bölüm", original);

        var draft = store.loadDraft().orElseThrow();
        assertEquals("yarım kalmış bölüm", draft.content());
        assertEquals(original.toAbsolutePath(), draft.originalFile());
        assertNotNull(draft.savedAt());
        assertTrue(draft.savedAt().isBefore(Instant.now().plusSeconds(5)));
        // loading must be repeatable (startup may ask twice)
        assertEquals("yarım kalmış bölüm", store.loadDraft().orElseThrow().content());
    }

    @Test
    void draftWithoutOriginalFileIsSupported(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);

        store.saveDraft("never saved anywhere", null);

        var draft = store.loadDraft().orElseThrow();
        assertEquals("never saved anywhere", draft.content());
        assertNull(draft.originalFile());
    }

    @Test
    void latestDraftWinsWithinOneSession(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);

        store.saveDraft("first", null);
        store.saveDraft("second", null);

        assertEquals("second", store.loadDraft().orElseThrow().content());
        assertEquals(1, draftFiles(dir).size());
    }

    @Test
    void savingLeavesNoTempFilesBehind(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        store.saveDraft("some text", null);

        try (var files = Files.walk(dir)) {
            assertTrue(files.filter(Files::isRegularFile)
                    .noneMatch(file -> file.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void clearRemovesTheDraft(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        store.saveDraft("something", null);

        store.clear();

        assertTrue(store.loadDraft().isEmpty());
        assertTrue(draftFiles(dir).isEmpty());
    }

    @Test
    void emptyStoreHasNothingToOffer(@TempDir Path dir) {
        var store = new RecoveryStore(dir);

        assertTrue(store.loadDraft().isEmpty());
        store.clear(); // clearing an empty store must not blow up
    }

    @Test
    void twoStoresOnTheSameDirectoryKeepSeparateSlots(@TempDir Path dir) throws IOException {
        var storeA = new RecoveryStore(dir);
        var storeB = new RecoveryStore(dir);

        storeA.saveDraft("A içeriği", null);
        storeB.saveDraft("B içeriği", null);

        assertEquals(2, draftFiles(dir).size());

        storeA.clear(); // only A's own slot may disappear

        assertEquals(1, draftFiles(dir).size());
        assertEquals("B içeriği", storeB.loadDraft().orElseThrow().content());
    }

    @Test
    void aNewSessionAdoptsTheSurvivingDraft(@TempDir Path dir) throws IOException {
        var sessionA = new RecoveryStore(dir);
        sessionA.saveDraft("eski taslak", null);
        assertEquals(1, draftFiles(dir).size());

        var sessionB = new RecoveryStore(dir);
        assertEquals("eski taslak", sessionB.loadDraft().orElseThrow().content());

        // the winner was moved into B's slot, so exactly one file remains
        assertEquals(1, draftFiles(dir).size());
        assertEquals("eski taslak", sessionB.loadDraft().orElseThrow().content());

        sessionB.clear();
        assertTrue(draftFiles(dir).isEmpty());
    }

    @Test
    void parkingFreesTheSlotWithoutLosingTheDraft(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        store.saveDraft("ertelenen metin", null);

        assertTrue(store.park());

        assertTrue(draftFiles(dir).isEmpty()); // the active slot is free again
        assertEquals(1, parkedFiles(dir).size());
        assertEquals("ertelenen metin", store.loadDraft().orElseThrow().content());
    }

    @Test
    void snapshotsAfterParkingDoNotOverwriteTheParkedDraft(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        store.saveDraft("ertelenen metin", null);
        assertTrue(store.park());

        store.saveDraft("yeni belgede yazılanlar", null);

        // Two independent drafts: the postponed one and the live one. The old
        // single-slot design could only keep one of them.
        assertEquals(1, parkedFiles(dir).size());
        assertEquals(1, draftFiles(dir).size());
        var parked = Files.readString(parkedFiles(dir).get(0));
        var active = Files.readString(draftFiles(dir).get(0));
        assertTrue(parked.contains("ertelenen metin"));
        assertTrue(active.contains("yeni belgede yazılanlar"));
    }

    @Test
    void clearKeepsParkedDrafts(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        store.saveDraft("ertelenen metin", null);
        assertTrue(store.park());
        store.saveDraft("canlı taslak", null);

        store.clear(); // a successful save or a clean exit

        assertEquals(1, parkedFiles(dir).size());
        assertTrue(draftFiles(dir).isEmpty());
        assertEquals("ertelenen metin", store.loadDraft().orElseThrow().content());
    }

    @Test
    void parkingWithoutADraftIsANoOp(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);

        assertTrue(store.park()); // nothing to move, slot already free

        assertEquals(0, jtwFiles(dir, "draft-").size());
        assertEquals(0, jtwFiles(dir, "pending-").size());
    }

    @Test
    void aParkedDraftIsAdoptedByTheNextSession(@TempDir Path dir) throws IOException {
        var first = new RecoveryStore(dir);
        first.saveDraft("önceki oturumun ertelenen metni", null);
        assertTrue(first.park());

        var second = new RecoveryStore(dir);
        assertEquals("önceki oturumun ertelenen metni", second.loadDraft().orElseThrow().content());
        assertEquals(1, draftFiles(dir).size()); // adopted into the new session's slot
        assertTrue(parkedFiles(dir).isEmpty());

        assertTrue(second.park()); // answering "later" again parks it again
        assertEquals(1, parkedFiles(dir).size());
    }

    @Test
    void badTimestampKeepsTheTextAndFallsBackToTheFileMtime(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        var original = dir.resolve("roman.txt");
        store.saveDraft("kurtarılacak metin", original);
        var file = draftFiles(dir).get(0);

        writeRaw(file, rawDraft("saved.at=not-an-instant\noriginal.path.b64=" + base64(original.toAbsolutePath().toString()),
                "kurtarılacak metin"));

        var draft = store.loadDraft().orElseThrow();

        assertEquals("kurtarılacak metin", draft.content());
        assertEquals(original.toAbsolutePath(), draft.originalFile());
        assertNotNull(draft.savedAt());
        assertEquals(Files.getLastModifiedTime(file).toInstant(), draft.savedAt());
    }

    @Test
    void unusableOriginalPathNeverHidesTheText(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        store.saveDraft("yarım kalmış bölüm", null);
        var file = draftFiles(dir).get(0);

        var unusable = "bad" + (char) 0 + "name";
        writeRaw(file, rawDraft("saved.at=" + Instant.now() + "\noriginal.path.b64=" + base64(unusable),
                "yarım kalmış bölüm"));

        var draft = store.loadDraft().orElseThrow();

        assertEquals("yarım kalmış bölüm", draft.content()); // the text survives
        assertNull(draft.originalFile()); // only the link to the file is lost
    }

    @Test
    void truncatedFileWithoutSeparatorIsOfferedAsText(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        var raw = MAGIC + "\nsaved.at=2024-01-01T00:00:00Z\nno separator here";
        writeRaw(dir.resolve("draft-" + java.util.UUID.randomUUID() + ".jtw"), bytes(raw));

        var draft = store.loadDraft().orElseThrow();

        assertEquals(raw, draft.content()); // nothing dropped, no exception
        assertNull(draft.originalFile());
    }

    @Test
    void invalidUtf8StillProducesADraft(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        var out = new ByteArrayOutputStream();
        out.writeBytes(bytes(MAGIC + "\nsaved.at=2024-01-01T00:00:00Z\noriginal.path.b64=\n\n"));
        out.writeBytes(new byte[] {'o', 'k', (byte) 0xC3, (byte) 0x28}); // broken UTF-8 sequence
        writeRaw(dir.resolve("draft-" + java.util.UUID.randomUUID() + ".jtw"), out.toByteArray());

        var draft = store.loadDraft().orElseThrow();

        assertFalse(draft.content().isEmpty());
        assertTrue(draft.content().contains("ok"));
    }

    @Test
    void draftIsOneFileHoldingTextAndMetadata(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        var original = dir.resolve("roman.txt");

        store.saveDraft("tek dosya", original);

        var files = draftFiles(dir);
        assertEquals(1, files.size());
        assertFalse(Files.exists(dir.resolve("draft.properties")));
        assertFalse(Files.exists(dir.resolve("draft.txt")));

        var stored = Files.readString(files.get(0), StandardCharsets.UTF_8);
        assertTrue(stored.startsWith(MAGIC));
        assertTrue(stored.contains("tek dosya"));
        assertTrue(stored.contains(base64(original.toAbsolutePath().toString())));
    }

    /** Files that make up the on-disk draft set. */
    private static List<Path> draftFiles(Path dir) throws IOException {
        return jtwFiles(dir, "draft-");
    }

    /** Drafts a session parked for a later decision. */
    private static List<Path> parkedFiles(Path dir) throws IOException {
        return jtwFiles(dir, "pending-");
    }

    private static List<Path> jtwFiles(Path dir, String prefix) throws IOException {
        try (var files = Files.list(dir)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().startsWith(prefix))
                    .filter(file -> file.getFileName().toString().endsWith(".jtw"))
                    .sorted()
                    .toList();
        }
    }

    private static void writeRaw(Path file, byte[] bytes) throws IOException {
        Files.write(file, bytes);
    }

    /** A complete draft file: magic, header lines and text after one blank line. */
    private static byte[] rawDraft(String headerLines, String text) {
        return bytes(MAGIC + "\n" + headerLines + "\n\n" + text);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(bytes(value));
    }
}
