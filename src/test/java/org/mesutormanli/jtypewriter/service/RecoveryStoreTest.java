package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Crash-recovery draft lifecycle: survive, be found, be dropped. */
class RecoveryStoreTest {

    @Test
    void roundTripsDraftWithMetadata(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        var original = dir.resolve("roman.txt");

        store.saveDraft("yarım kalmış bölüm", original);

        var draft = store.loadDraft().orElseThrow();
        assertEquals("yarım kalmış bölüm", draft.content());
        assertEquals(original.toAbsolutePath(), draft.originalFile());
        assertTrue(draft.savedAt().isBefore(Instant.now().plusSeconds(5)));
    }

    @Test
    void draftWithoutOriginalFileIsSupported(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);

        store.saveDraft("never saved anywhere", null);

        var draft = store.loadDraft().orElseThrow();
        assertEquals("never saved anywhere", draft.content());
        assertEquals(null, draft.originalFile());
    }

    @Test
    void latestDraftWins(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);

        store.saveDraft("first", null);
        store.saveDraft("second", null);

        assertEquals("second", store.loadDraft().orElseThrow().content());
        // the temp file of the write pipeline must never linger
        assertFalse(Files.exists(dir.resolve("draft.txt.tmp")));
        assertFalse(Files.exists(dir.resolve("draft.properties.tmp")));
    }

    @Test
    void clearRemovesTheDraft(@TempDir Path dir) throws IOException {
        var store = new RecoveryStore(dir);
        store.saveDraft("something", null);

        store.clear();

        assertTrue(store.loadDraft().isEmpty());
    }

    @Test
    void emptyStoreHasNothingToOffer(@TempDir Path dir) {
        var store = new RecoveryStore(dir);

        assertTrue(store.loadDraft().isEmpty());
        store.clear(); // clearing an empty store must not blow up
    }
}