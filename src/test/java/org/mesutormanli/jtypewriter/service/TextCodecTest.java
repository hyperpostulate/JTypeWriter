package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for the encoding contract: detection must recognize BOMs
 * and strict UTF-8, refuse to guess on foreign legacy encodings (so the caller
 * can ask the user), and never change a file's encoding on a round trip.
 */
class TextCodecTest {

    private static final String TURKISH = "çöğüÇĞÜİıŞş";

    @Test
    void detectsPlainUtf8() {
        var bytes = "Merhaba dünya".getBytes(StandardCharsets.UTF_8);

        var result = TextCodec.decode(bytes);

        assertEquals("Merhaba dünya", result.text());
        assertEquals(StandardCharsets.UTF_8, result.charset());
        assertEquals(false, result.bom());
    }

    @Test
    void detectsUtf8BomAndStripsIt() throws Exception {
        var bytes = TextCodec.encode("hello", StandardCharsets.UTF_8, true);

        var result = TextCodec.decode(bytes);

        assertEquals("hello", result.text());
        assertEquals(StandardCharsets.UTF_8, result.charset());
        assertEquals(true, result.bom());
    }

    @Test
    void detectsUtf16ByteOrderMarks() throws Exception {
        var le = TextCodec.decode(TextCodec.encode("hello", StandardCharsets.UTF_16LE, true));
        var be = TextCodec.decode(TextCodec.encode("hello", StandardCharsets.UTF_16BE, true));

        assertEquals("hello", le.text());
        assertEquals(StandardCharsets.UTF_16LE, le.charset());
        assertEquals("hello", be.text());
        assertEquals(StandardCharsets.UTF_16BE, be.charset());
    }

    @Test
    void refusesToGuessLegacyEncodings() {
        // Windows-1254 Turkish letters are not valid UTF-8: detection must ask
        var ansi = TURKISH.getBytes(TextCodec.WINDOWS_1254);

        assertNull(TextCodec.decode(ansi));
    }

    @Test
    void explicitLegacyChoiceDecodesTurkish() {
        var ansi = TURKISH.getBytes(TextCodec.WINDOWS_1254);

        var result = TextCodec.decodeWith(ansi, TextCodec.WINDOWS_1254);

        assertEquals(TURKISH, result.text());
        assertEquals(TextCodec.WINDOWS_1254, result.charset());
    }

    @Test
    void roundTripPreservesLegacyEncodingExactly() throws Exception {
        var ansi = TURKISH.getBytes(TextCodec.WINDOWS_1254);
        var decoded = TextCodec.decodeWith(ansi, TextCodec.WINDOWS_1254);

        assertArrayEquals(ansi, TextCodec.encode(decoded.text(), decoded.charset(), decoded.bom()));
    }

    @Test
    void roundTripPreservesUtf8Bom() throws Exception {
        var original = TextCodec.encode(TURKISH, StandardCharsets.UTF_8, true);
        var decoded = TextCodec.decode(original);

        assertArrayEquals(original, TextCodec.encode(decoded.text(), decoded.charset(), decoded.bom()));
    }

    @Test
    void emptyDocumentDecodes() {
        var result = TextCodec.decode(new byte[0]);

        assertEquals("", result.text());
    }

    // --- strict encoding: never silently drop characters -------------------

    @Test
    void unrepresentableCharactersFailInsteadOfTurningIntoQuestionMarks() {
        // an emoji and CJK characters do not exist in windows-1254
        assertThrows(CharacterCodingException.class,
                () -> TextCodec.encode("Merhaba \uD83D\uDE00 \u4F60\u597D", TextCodec.WINDOWS_1254, false));
    }

    @Test
    void unrepresentableCharactersAlwaysFitUtf8() throws Exception {
        var text = "Merhaba \uD83D\uDE00 \u4F60\u597D";

        var bytes = TextCodec.encode(text, StandardCharsets.UTF_8, false);

        assertEquals(text, new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    void representableLegacyTextStillEncodesExactly() throws Exception {
        var ansi = TURKISH.getBytes(TextCodec.WINDOWS_1254);

        assertArrayEquals(ansi, TextCodec.encode(TURKISH, TextCodec.WINDOWS_1254, false));
    }
}