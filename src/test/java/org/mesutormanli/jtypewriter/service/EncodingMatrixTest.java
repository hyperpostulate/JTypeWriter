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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The PW-32 encoding matrix: every supported document encoding is opened,
 * saved unchanged, and byte-compared; then opened, edited in a way that stays
 * representable, saved, and byte-compared again. The UTF-16 rows are the
 * regression test for the silent-corruption half of analysis V6 ("a BOM-less
 * UTF-16 file in the ASCII range is valid UTF-8, so it opens without warning
 * full of NULs and is rewritten as UTF-8 on the next save"). They use a real
 * Turkish + ASCII payload, not an ASCII stand-in, because that is the text a
 * Turkish user actually has.
 */
class EncodingMatrixTest {

    private static final String TURKISH = "çöğüİıŞş\n\tsatır";
    /**
     * A real Turkish document, not an ASCII stand-in: it mixes Turkish and
     * ASCII in one multi-line payload, so a BOM-less UTF-16 round trip that
     * only works for the ASCII range cannot pass here. Turkish is also not
     * valid UTF-8 in UTF-16 ({@code ç} is {@code E7 00}), which is why the
     * detection rule works on byte parity instead of UTF-8 validity.
     */
    private static final String UTF16_TEXT = "Bölüm bir: çöğüİıŞş\n"
            + "ikinci satır ğüşiöç\n\tPublished chapter: ASCII 0123";

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
    void bomlessUtf16LeDocumentOpensAsUtf16(@TempDir Path dir) throws IOException {
        var original = UTF16_TEXT.getBytes(StandardCharsets.UTF_16LE);
        var target = dir.resolve("doc.txt");
        write(target, original);

        assertTrue(fileService.loadFile(null, target).isPresent());

        assertEquals(UTF16_TEXT, fileService.getCurrentContent());
        // The old code accepted these bytes as UTF-8 (or, for Turkish bytes,
        // prompted) and never showed the text: the string was NUL-riddled or
        // the file was rewritten as UTF-8 - exactly the silent defect.
        assertFalse(fileService.getCurrentContent().contains("\u0000"));
    }

    @Test
    void bomlessUtf16LeRoundTripsByteIdentically(@TempDir Path dir) throws IOException {
        var original = UTF16_TEXT.getBytes(StandardCharsets.UTF_16LE);
        var target = dir.resolve("doc.txt");
        write(target, original);
        assertTrue(fileService.loadFile(null, target).isPresent());

        assertTrue(fileService.saveFile(null, fileService.getCurrentContent()).isPresent());

        // No BOM may be added: the file is still BOM-less UTF-16LE.
        assertArrayEquals(original, Files.readAllBytes(target));
    }

    @Test
    void bomlessUtf16WithTurkishTextIsNotSentToTheLegacyPrompt(@TempDir Path dir) throws IOException {
        var original = UTF16_TEXT.getBytes(StandardCharsets.UTF_16LE);
        var target = dir.resolve("doc.txt");
        write(target, original);

        assertTrue(fileService.loadFile(null, target).isPresent());

        assertEquals(UTF16_TEXT, fileService.getCurrentContent());
        // The previous rule required the bytes to be valid UTF-8, and Turkish
        // UTF-16LE is not (ç = E7 00), so this file could only ever reach the
        // legacy prompt. Detection, not a question, must handle it now.
        verify(dialogService, never()).showEncodingDialog(any(), any());
        assertTrue(fileService.saveFile(null, fileService.getCurrentContent()).isPresent());
        assertArrayEquals(original, Files.readAllBytes(target));
    }

    @Test
    void bomlessUtf16BeRoundTripsByteIdentically(@TempDir Path dir) throws IOException {
        var original = UTF16_TEXT.getBytes(StandardCharsets.UTF_16BE);
        var target = dir.resolve("doc.txt");
        write(target, original);
        assertTrue(fileService.loadFile(null, target).isPresent());

        assertTrue(fileService.saveFile(null, fileService.getCurrentContent()).isPresent());

        assertArrayEquals(original, Files.readAllBytes(target));
    }

    @Test
    void editedBomlessUtf16KeepsItsEncoding(@TempDir Path dir) throws IOException {
        var original = UTF16_TEXT.getBytes(StandardCharsets.UTF_16LE);
        var target = dir.resolve("doc.txt");
        write(target, original);
        assertTrue(fileService.loadFile(null, target).isPresent());

        var edited = UTF16_TEXT + "\nyeni bir satır: çöğü İstanbul";
        assertTrue(fileService.saveFile(null, edited).isPresent());

        var written = Files.readAllBytes(target);
        assertArrayEquals(edited.getBytes(StandardCharsets.UTF_16LE), written);
        assertFalse(Arrays.equals(original, written), "the test must not be vacuous");
    }

    @Test
    void windows1254LegacyDocumentRoundTripsByteIdentically(@TempDir Path dir) throws IOException {
        var original = TURKISH.getBytes(TextCodec.WINDOWS_1254);
        var target = dir.resolve("ansi.txt");
        write(target, original);
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);

        assertTrue(fileService.loadFile(null, target).isPresent());
        assertEquals(TURKISH, fileService.getCurrentContent());
        assertTrue(fileService.saveFile(null, fileService.getCurrentContent()).isPresent());

        assertArrayEquals(original, Files.readAllBytes(target));
        verify(dialogService, never()).showMalformedEncodingDialog(any(), any(), any());
    }

    @Test
    void editedWindows1254DocumentStaysWindows1254(@TempDir Path dir) throws IOException {
        var original = TURKISH.getBytes(TextCodec.WINDOWS_1254);
        var target = dir.resolve("ansi.txt");
        write(target, original);
        when(dialogService.showEncodingDialog(any(), any()))
                .thenReturn(DialogService.EncodingChoice.WINDOWS1254);
        assertTrue(fileService.loadFile(null, target).isPresent());

        var edited = TURKISH + "\nyeni satır: çöğü";
        assertTrue(fileService.saveFile(null, edited).isPresent());

        assertArrayEquals(edited.getBytes(TextCodec.WINDOWS_1254), Files.readAllBytes(target));
        verify(dialogService, never()).showEncodingUpgradeDialog(any(), any());
    }

    @Test
    void mixedContentUtf8RoundTripsByteIdentically(@TempDir Path dir) throws IOException {
        var text = "ASCII line\r\nçöğüİıŞş\t\uD83D\uDE00\r\nson satır";
        var original = TextCodec.encode(text, StandardCharsets.UTF_8, false);
        var target = dir.resolve("mixed.txt");
        write(target, original);

        assertTrue(fileService.loadFile(null, target).isPresent());
        // A genuine UTF-8 document must not reach the encoding question at all.
        verify(dialogService, never()).showEncodingDialog(any(), any());
        assertEquals(text, fileService.getCurrentContent());

        assertTrue(fileService.saveFile(null, text).isPresent());
        assertArrayEquals(original, Files.readAllBytes(target));

        var edited = text + "\r\nyeni\tçöğü \uD83D\uDE00";
        assertTrue(fileService.saveFile(null, edited).isPresent());
        assertArrayEquals(edited.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target));
    }

    @Test
    void bomlessUtf16IsReportedInsteadOfAssumed() throws Exception {
        var bytes = UTF16_TEXT.getBytes(StandardCharsets.UTF_16LE);

        var records = new ArrayList<LogRecord>();
        var logger = AppLogging.logger(TextCodec.class).getParent();
        var collector = collectInto(records);
        logger.addHandler(collector);
        try {
            var decoded = TextCodec.decode(bytes);
            assertNotNull(decoded);
            assertEquals(StandardCharsets.UTF_16LE, decoded.charset());
        } finally {
            logger.removeHandler(collector);
        }

        assertTrue(records.stream().anyMatch(r -> r.getLevel() == Level.WARNING
                        && r.getMessage() != null
                        && r.getMessage().contains("without a byte-order mark")),
                "BOM-less UTF-16 detection must be logged");
        assertFalse(TextCodec.decode(bytes).text().contains("\u0000"));
    }

    @Test
    void plainUtf8WithNulsOnBothParitiesIsLeftAlone() throws Exception {
        var bytes = new byte[] {'a', 0, 0, 'b'};

        var records = new ArrayList<LogRecord>();
        var logger = AppLogging.logger(TextCodec.class).getParent();
        var collector = collectInto(records);
        logger.addHandler(collector);
        TextCodec.Result decoded;
        try {
            decoded = TextCodec.decode(bytes);
        } finally {
            logger.removeHandler(collector);
        }

        assertNotNull(decoded);
        assertEquals(StandardCharsets.UTF_8, decoded.charset());
        assertFalse(decoded.bom());
        assertEquals("a\u0000\u0000b", decoded.text());
        assertFalse(records.stream().anyMatch(r -> r.getMessage() != null
                        && r.getMessage().contains("without a byte-order mark")),
                "a valid-UTF-8 file with NULs on both parities fails the 80 %/10 %"
                        + " parity bounds, so it must stay UTF-8: this pins the new rule's boundary");
    }

    @Test
    void noEncodingPromptIsShownForBomlessUtf16(@TempDir Path dir) throws IOException {
        var target = dir.resolve("doc.txt");
        write(target, UTF16_TEXT.getBytes(StandardCharsets.UTF_16LE));

        assertTrue(fileService.loadFile(null, target).isPresent());

        // Detection, not a question, is what closed this hole.
        verify(dialogService, never()).showEncodingDialog(any(), any());
        verify(dialogService, never()).showMalformedEncodingDialog(any(), any(), any());
    }

    private static void write(Path target, byte[] bytes) throws IOException {
        Files.write(target, bytes);
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
}
