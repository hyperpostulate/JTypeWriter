package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileServiceTest {

    private static final String TURKISH = "çöğü";

    private DialogService dialogService;
    private Messages messages;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        messages = mock(Messages.class);
        dialogService = mock(DialogService.class);
        fileService = new FileService(dialogService, messages);
    }

    @Test
    void noUnsavedChangesInitially() {
        assertFalse(fileService.hasUnsavedChanges());
    }

    @Test
    void detectsUnsavedChanges() {
        fileService.setCurrentContent("new content");
        assertTrue(fileService.hasUnsavedChanges());
    }

    @Test
    void revertingToTheSavedTextClearsTheDirtyFlag(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "saved text");
        assertTrue(fileService.loadFile(null, target).isPresent());

        fileService.setCurrentContent("edited text");
        assertTrue(fileService.hasUnsavedChanges());

        // undo back to the loaded text: the dirty marker must clear again
        fileService.setCurrentContent("saved text");
        assertFalse(fileService.hasUnsavedChanges());
    }

    @Test
    void sameLengthEditMarksTheDocumentDirty(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "abc");
        assertTrue(fileService.loadFile(null, target).isPresent());

        // Same length: only the comparison can tell, so this is the path that
        // has to fall through the length guard (analysis M4).
        fileService.setCurrentContent("abd");

        assertTrue(fileService.hasUnsavedChanges());
    }

    @Test
    void openIsAbortedWhenUnsavedChangesAreCancelled() {
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.CANCEL);

        assertTrue(fileService.openFile(null).isEmpty());

        verify(dialogService, never()).showOpenDialog(any());
        assertEquals("draft", fileService.getCurrentContent());
    }

    @Test
    void openDiscardsDraftOnlyOnExplicitConfirmation(@TempDir Path dir) throws IOException {
        var target = dir.resolve("opened.txt");
        Files.writeString(target, "from disk");
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.DISCARD);
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));

        var opened = fileService.openFile(null);

        assertTrue(opened.isPresent());
        assertEquals("from disk", fileService.getCurrentContent());
    }

    @Test
    void openIsAbortedWhenSavingIsCancelled() {
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.SAVE);
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.empty());

        assertTrue(fileService.openFile(null).isEmpty());

        verify(dialogService, never()).showOpenDialog(any());
        assertEquals("draft", fileService.getCurrentContent());
    }

    @Test
    void openSavesDraftFirstWhenRequested(@TempDir Path dir) throws IOException {
        var backup = dir.resolve("backup.txt");
        var target = dir.resolve("opened.txt");
        Files.writeString(target, "from disk");
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.SAVE);
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(backup.toFile()));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));

        assertTrue(fileService.openFile(null).isPresent());

        assertEquals("draft", Files.readString(backup));
        assertEquals("from disk", fileService.getCurrentContent());
        assertFalse(fileService.hasUnsavedChanges());
    }

    // --- data safety: atomic writes, backups, listener hooks ---------------

    @Test
    void overwriteKeepsPreviousVersionInBackup(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));

        fileService.saveFile(null, "v1");
        fileService.saveFile(null, "v2");

        assertEquals("v2", Files.readString(target));
        assertEquals("v1", Files.readString(BackupKeeper.backupPath(target, 0)));
    }

    @Test
    void interruptedSavesLeaveNoTempFilesBehind(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));

        fileService.saveFile(null, "v1");
        fileService.saveFile(null, "v2");

        try (Stream<Path> files = Files.list(dir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }

    // --- encoding stability -------------------------------------------------

    @Test
    void legacyEncodedFileSurvivesOpenAndSaveRoundTrip(@TempDir Path dir) throws IOException {
        var target = dir.resolve("ansi.txt");
        var ansi = TURKISH.getBytes(TextCodec.WINDOWS_1254);
        Files.write(target, ansi);
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);

        assertTrue(fileService.openFile(null).isPresent());
        assertEquals(TURKISH, fileService.getCurrentContent());

        fileService.saveFile(null, fileService.getCurrentContent());

        // byte-identical round trip: saving must never switch the file to UTF-8
        assertArrayEquals(ansi, Files.readAllBytes(target));
    }

    @Test
    void cancellingTheEncodingPromptAbortsTheOpen(@TempDir Path dir) throws IOException {
        var target = dir.resolve("ansi.txt");
        Files.write(target, TURKISH.getBytes(TextCodec.WINDOWS_1254));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.CANCEL);

        assertTrue(fileService.openFile(null).isEmpty());
    }

    @Test
    void validUtf8FileOpensWithoutAnyEncodingPrompt(@TempDir Path dir) throws IOException {
        var target = dir.resolve("utf8.txt");
        Files.write(target, "héllo wörld".getBytes(StandardCharsets.UTF_8));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));

        assertTrue(fileService.openFile(null).isPresent());

        assertEquals("héllo wörld", fileService.getCurrentContent());
        verify(dialogService, never()).showEncodingDialog(any(), any());
    }

    // --- lifecycle listeners and recovery attachment ------------------------

    @Test
    void loadFileRestoresADocumentWithoutAFilePicker(@TempDir Path dir) throws IOException {
        var target = dir.resolve("last-session.txt");
        Files.writeString(target, "where I left off");
        fileService.setCurrentContent("untitled scratch");

        var loaded = fileService.loadFile(null, target);

        assertTrue(loaded.isPresent());
        assertEquals("where I left off", fileService.getCurrentContent());
        assertFalse(fileService.hasUnsavedChanges());
        verify(dialogService, never()).showOpenDialog(any());
    }

    @Test
    void notifiesListenersAboutLoadsAndSaves(@TempDir Path dir) throws IOException {
        var events = new ArrayList<String>();
        fileService.addListener(new FileService.Listener() {
            @Override
            public void onLoaded(Path file) {
                events.add("loaded:" + file.getFileName());
            }

            @Override
            public void onSaved(Path file) {
                events.add("saved:" + file.getFileName());
            }
        });
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "from disk");
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));

        fileService.openFile(null);
        fileService.saveFile(null, "changed");

        assertEquals(List.of("loaded:doc.txt", "saved:doc.txt"), events);
    }

    @Test
    void listenerFailureDoesNotFailTheSave(@TempDir Path dir) throws IOException {
        var secondNotified = new ArrayList<String>();
        fileService.addListener(new FileService.Listener() {
            @Override
            public void onSaved(Path file) {
                throw new IllegalStateException("broken listener");
            }
        });
        fileService.addListener(new FileService.Listener() {
            @Override
            public void onSaved(Path file) {
                secondNotified.add("saved:" + file.getFileName());
            }
        });
        var target = dir.resolve("doc.txt");
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));

        var saved = fileService.saveFile(null, "durable text");

        // The bytes are on disk, so a listener bug must not report a failure:
        // the caller would otherwise refuse to close, open or start a document.
        assertEquals(Optional.of(target), saved);
        assertEquals("durable text", Files.readString(target));
        assertEquals(List.of("saved:doc.txt"), secondNotified);
        verify(dialogService, never()).showError(any(), any(), any());
    }

    @Test
    void restoredRecoveryDraftIsDirtyAndCanBeSavedThereAgain(@TempDir Path dir) throws IOException {
        var target = dir.resolve("novel.txt");
        Files.write(target, "published chapter".getBytes(TextCodec.WINDOWS_1254));

        fileService.restoreRecovered(target, "new chapter");

        assertEquals("new chapter", fileService.getCurrentContent());
        assertTrue(fileService.hasUnsavedChanges());

        fileService.saveFile(null, fileService.getCurrentContent());
        assertFalse(fileService.hasUnsavedChanges());
    }

    @Test
    void restoredLegacyDraftWithNonAsciiTextIsSavedByteIdentically(@TempDir Path dir) throws IOException {
        // A BOM-less windows-1254 original does not decode as UTF-8 (decode()
        // returns null), so this genuinely exercises the legacy fallback branch.
        var target = dir.resolve("novel.txt");
        Files.write(target, "Eski çöğü".getBytes(TextCodec.WINDOWS_1254));

        fileService.restoreRecovered(target, "Yeni çöğü");
        var saved = fileService.saveFile(null, "Yeni çöğü");

        assertTrue(saved.isPresent());
        // byte-identical windows-1254 round trip: the draft must not turn the
        // document into UTF-8 behind the user's back
        assertArrayEquals("Yeni çöğü".getBytes(TextCodec.WINDOWS_1254), Files.readAllBytes(target));
        assertFalse(fileService.hasUnsavedChanges());
        verify(dialogService, never()).showEncodingUpgradeDialog(any(), any());
    }

    @Test
    void restoredDraftWithoutAFileIsDirty(@TempDir Path dir) {
        fileService.restoreRecovered(null, "scratch text");

        assertTrue(fileService.hasUnsavedChanges());
        assertEquals(null, fileService.getCurrentFilePath());
    }

    @Test
    void restoredEmptyDraftStaysDirty() {
        // The draft is not on disk at all, so even empty content must not look
        // saved - otherwise autosave would clear the recovery file.
        fileService.restoreRecovered(null, "");

        assertTrue(fileService.hasUnsavedChanges());
    }

    @Test
    void restoredNonEmptyDraftWithoutAFileIsDirty() {
        fileService.restoreRecovered(null, "text");

        assertTrue(fileService.hasUnsavedChanges());
    }

    @Test
    void repeatedSavesOfUnchangedContentDoNotRotateBackups(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "v1");
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        assertTrue(fileService.openFile(null).isPresent());

        fileService.saveFile(null, "v2"); // changed content: the old version is kept
        assertEquals("v1", Files.readString(BackupKeeper.backupPath(target, 0)));

        fileService.saveFile(null, "v2"); // unchanged: must not age the backup history

        assertEquals("v1", Files.readString(BackupKeeper.backupPath(target, 0)));
        assertFalse(Files.exists(BackupKeeper.backupPath(target, 1)));
    }

    // --- backup failures must never cancel or reverse a document write ------

    @Test
    void backupStagingFailureStillSavesTheDocument(@TempDir Path dir) throws IOException {
        var keeper = mock(BackupKeeper.class);
        var service = new FileService(dialogService, messages, keeper);
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "v1");
        var newest = BackupKeeper.backupPath(target, 0);
        var older = BackupKeeper.backupPath(target, 1);
        Files.writeString(newest, "v0");
        Files.writeString(older, "v-1");
        when(keeper.prepare(target)).thenThrow(new IOException("disk full"));
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));

        var saved = service.saveFile(null, "v2");

        assertTrue(saved.isPresent());
        assertEquals("v2", Files.readString(target));
        assertFalse(service.hasUnsavedChanges());
        // a failed copy must not rotate or drop any generation
        assertEquals("v0", Files.readString(newest));
        assertEquals("v-1", Files.readString(older));
        assertFalse(Files.exists(BackupKeeper.backupPath(target, 2)));
        verify(dialogService, never()).showError(any(), any(), any());
    }

    @Test
    void backupInstallFailureStillSavesTheDocument(@TempDir Path dir) throws IOException {
        var keeper = mock(BackupKeeper.class);
        var service = new FileService(dialogService, messages, keeper);
        var target = dir.resolve("doc.txt");
        Files.writeString(target, "v1");
        var newest = BackupKeeper.backupPath(target, 0);
        var older = BackupKeeper.backupPath(target, 1);
        Files.writeString(newest, "v0");
        Files.writeString(older, "v-1");
        var staged = dir.resolve("doc.txt.bak.new");
        Files.writeString(staged, "v1");
        when(keeper.prepare(target))
                .thenReturn(Optional.of(new BackupKeeper.PreparedBackup(target, staged)));
        doThrow(new IOException("install failed")).when(keeper).install(any());
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));

        var saved = service.saveFile(null, "v2");

        assertTrue(saved.isPresent());
        assertEquals("v2", Files.readString(target));
        assertFalse(service.hasUnsavedChanges());
        assertEquals("v0", Files.readString(newest));
        assertEquals("v-1", Files.readString(older));
        assertFalse(Files.exists(BackupKeeper.backupPath(target, 2)));
        verify(dialogService, never()).showError(any(), any(), any());
    }

    // --- encoding upgrade: never silently turn characters into '?' ---------

    private static final String MODERN = "Merhaba \uD83D\uDE00 \u4F60\u597D";

    @Test
    void charactersThatDoNotFitTheEncodingAreSavedAsUtf8Instead(@TempDir Path dir) throws IOException {
        var target = dir.resolve("ansi.txt");
        Files.write(target, TURKISH.getBytes(TextCodec.WINDOWS_1254));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);
        assertTrue(fileService.openFile(null).isPresent());

        when(dialogService.showEncodingUpgradeDialog(any(), any())).thenReturn(true);
        fileService.setCurrentContent(MODERN);

        var saved = fileService.saveFile(null, fileService.getCurrentContent());

        assertTrue(saved.isPresent());
        verify(dialogService).showEncodingUpgradeDialog(any(), eq(TextCodec.WINDOWS_1254));
        // the text is intact and the document now lives in UTF-8
        assertEquals(MODERN, Files.readString(target, StandardCharsets.UTF_8));
        assertFalse(fileService.hasUnsavedChanges());
    }

    @Test
    void cancellingTheEncodingUpgradeLeavesTheFileUntouched(@TempDir Path dir) throws IOException {
        var target = dir.resolve("ansi.txt");
        var ansi = TURKISH.getBytes(TextCodec.WINDOWS_1254);
        Files.write(target, ansi);
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);
        assertTrue(fileService.openFile(null).isPresent());

        when(dialogService.showEncodingUpgradeDialog(any(), any())).thenReturn(false);
        fileService.setCurrentContent(MODERN);

        assertTrue(fileService.saveFile(null, fileService.getCurrentContent()).isEmpty());

        assertArrayEquals(ansi, Files.readAllBytes(target)); // nothing written, no '?' anywhere
        assertTrue(fileService.hasUnsavedChanges()); // the draft is still there to be saved
    }

    // --- new document (Ctrl+N) ----------------------------------------------

    // --- damaged BOMs: strict decode behind an explicit user prompt --------

    /** UTF-8 BOM followed by an invalid continuation byte. */
    private static final byte[] MALFORMED_UTF8_BOM = {
            (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, (byte) 0xC3, 0x28
    };

    @Test
    void damagedBomIsNeverDecodedWithoutAsking(@TempDir Path dir) throws IOException {
        var target = dir.resolve("damaged.txt");
        Files.write(target, MALFORMED_UTF8_BOM);
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showMalformedEncodingDialog(any(), any(), eq(StandardCharsets.UTF_8)))
                .thenReturn(true);

        assertTrue(fileService.openFile(null).isPresent());

        assertTrue(fileService.getCurrentContent().contains("\uFFFD"));
        verify(dialogService).showMalformedEncodingDialog(any(), any(), eq(StandardCharsets.UTF_8));
        verify(dialogService, never()).showEncodingDialog(any(), any());
    }

    @Test
    void acceptingADamagedBomLogsWarningsForDetectionAndReplacement(@TempDir Path dir) throws IOException {
        var target = dir.resolve("damaged.txt");
        Files.write(target, MALFORMED_UTF8_BOM);
        when(dialogService.showMalformedEncodingDialog(any(), any(), any())).thenReturn(true);

        var records = new ArrayList<LogRecord>();
        var logger = AppLogging.logger(FileService.class).getParent();
        var collector = collectInto(records);
        logger.addHandler(collector);
        try {
            assertTrue(fileService.loadFile(null, target).isPresent());
        } finally {
            logger.removeHandler(collector);
        }

        assertTrue(records.stream().anyMatch(r -> r.getLevel() == Level.WARNING
                        && r.getMessage().contains("byte-order mark")),
                "detection of the damaged BOM must be logged");
        assertTrue(records.stream().anyMatch(r -> r.getLevel() == Level.WARNING
                        && r.getMessage().contains("replacement characters")),
                "the lossy open the user accepted must be logged");
    }

    @Test
    void cancellingADamagedBomLeavesTheCurrentDocumentUntouched(@TempDir Path dir) throws IOException {
        var target = dir.resolve("damaged.txt");
        Files.write(target, MALFORMED_UTF8_BOM);
        fileService.setCurrentContent("my draft");
        when(dialogService.showMalformedEncodingDialog(any(), any(), any())).thenReturn(false);

        assertTrue(fileService.loadFile(null, target).isEmpty());

        assertEquals("my draft", fileService.getCurrentContent());
        assertEquals(null, fileService.getCurrentFilePath());
        assertTrue(fileService.hasUnsavedChanges());
        assertArrayEquals(MALFORMED_UTF8_BOM, Files.readAllBytes(target));
    }

    @Test
    void legacyFilesStillUseTheLegacyEncodingPrompt(@TempDir Path dir) throws IOException {
        var target = dir.resolve("ansi.txt");
        Files.write(target, TURKISH.getBytes(TextCodec.WINDOWS_1254));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(target.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);

        assertTrue(fileService.openFile(null).isPresent());

        assertEquals(TURKISH, fileService.getCurrentContent());
        verify(dialogService, never()).showMalformedEncodingDialog(any(), any(), any());
    }

    /** Marker handler so the test can remove exactly the one it installed. */
    private static final class Collector extends Handler {
        private final List<LogRecord> records;

        private Collector(List<LogRecord> records) {
            this.records = records;
        }

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static Handler collectInto(List<LogRecord> records) {
        return new Collector(records);
    }

    @Test
    void newDocumentOnACleanDocumentNeedsNoDialogs() {
        assertTrue(fileService.newDocument(null));

        verifyNoInteractions(dialogService);
        assertEquals(null, fileService.getCurrentFilePath());
        assertFalse(fileService.hasUnsavedChanges());
        assertEquals("", fileService.getCurrentContent());
    }

    @Test
    void newDocumentIsAbortedWhenUnsavedChangesAreCancelled() {
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.CANCEL);

        assertFalse(fileService.newDocument(null));

        assertEquals("draft", fileService.getCurrentContent());
        assertEquals(null, fileService.getCurrentFilePath());
        verify(dialogService, never()).showOpenDialog(any());
    }

    @Test
    void newDocumentDiscardsTheDraftOnlyOnExplicitConfirmation() {
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.DISCARD);

        assertTrue(fileService.newDocument(null));

        assertEquals("", fileService.getCurrentContent());
        assertFalse(fileService.hasUnsavedChanges());
        assertEquals(null, fileService.getCurrentFilePath());
    }

    @Test
    void newDocumentSavesTheDraftFirstWhenRequested(@TempDir Path dir) throws IOException {
        var backup = dir.resolve("backup.txt");
        fileService.setCurrentContent("draft");
        when(dialogService.showUnsavedChangesDialog(any())).thenReturn(DialogService.SaveChoice.SAVE);
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(backup.toFile()));

        assertTrue(fileService.newDocument(null));

        assertEquals("draft", Files.readString(backup));
        assertEquals("", fileService.getCurrentContent());
        assertEquals(null, fileService.getCurrentFilePath());
    }

    @Test
    void newDocumentForgetsTheEncodingOfThePreviousDocument(@TempDir Path dir) throws IOException {
        var legacy = dir.resolve("ansi.txt");
        Files.write(legacy, TURKISH.getBytes(TextCodec.WINDOWS_1254));
        when(dialogService.showOpenDialog(any())).thenReturn(Optional.of(legacy.toFile()));
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);
        assertTrue(fileService.openFile(null).isPresent());

        assertTrue(fileService.newDocument(null));

        var target = dir.resolve("fresh.txt");
        when(dialogService.showSaveDialog(any(), any())).thenReturn(Optional.of(target.toFile()));
        var text = "Yeni \u00e7\u00f6\u011f\u00fc \uD83D\uDE00";
        assertTrue(fileService.saveFile(null, text).isPresent());

        // The emoji is not representable in windows-1254: had the charset
        // survived the reset, this would have needed an upgrade prompt.
        assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
        verify(dialogService, never()).showEncodingUpgradeDialog(any(), any());
    }

    @Test
    void notifiesListenersAboutNewDocumentsInsteadOfLoads() {
        var events = new ArrayList<String>();
        fileService.addListener(new FileService.Listener() {
            @Override
            public void onLoaded(Path file) {
                events.add("loaded");
            }

            @Override
            public void onNewDocument() {
                events.add("new");
            }
        });

        assertTrue(fileService.newDocument(null));

        assertEquals(List.of("new"), events);
    }
}