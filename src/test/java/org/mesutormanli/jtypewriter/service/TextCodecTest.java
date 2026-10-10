package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void detectsUtf8BomAndStripsIt() {
        var bytes = TextCodec.encode("hello", StandardCharsets.UTF_8, true);

        var result = TextCodec.decode(bytes);

        assertEquals("hello", result.text());
        assertEquals(StandardCharsets.UTF_8, result.charset());
        assertEquals(true, result.bom());
    }

    @Test
    void detectsUtf16ByteOrderMarks() {
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
    void roundTripPreservesLegacyEncodingExactly() {
        var ansi = TURKISH.getBytes(TextCodec.WINDOWS_1254);
        var decoded = TextCodec.decodeWith(ansi, TextCodec.WINDOWS_1254);

        assertArrayEquals(ansi, TextCodec.encode(decoded.text(), decoded.charset(), decoded.bom()));
    }

    @Test
    void roundTripPreservesUtf8Bom() {
        var original = TextCodec.encode(TURKISH, StandardCharsets.UTF_8, true);
        var decoded = TextCodec.decode(original);

        assertArrayEquals(original, TextCodec.encode(decoded.text(), decoded.charset(), decoded.bom()));
    }

    @Test
    void emptyDocumentDecodes() {
        var result = TextCodec.decode(new byte[0]);

        assertEquals("", result.text());
    }
}