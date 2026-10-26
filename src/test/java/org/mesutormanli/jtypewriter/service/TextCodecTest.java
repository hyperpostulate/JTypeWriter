/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the encoding contract: detection must recognize BOMs
 * and strict UTF-8, refuse to guess on foreign legacy encodings (so the caller
 * can ask the user), and never change a file's encoding on a round trip.
 */
class TextCodecTest {

    private static final String TURKISH = "çöğüÇĞÜİıŞş";

    @Test
    void detectsPlainUtf8() throws Exception {
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
    void refusesToGuessLegacyEncodings() throws Exception {
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
    void emptyDocumentDecodes() throws Exception {
        var result = TextCodec.decode(new byte[0]);

        assertEquals("", result.text());
    }

    // --- damaged byte-order marks: strict, never silent U+FFFD -------------

    @Test
    void malformedUtf8BomIsRejectedInsteadOfReplaced() {
        var bytes = new byte[]{
                (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, // UTF-8 BOM
                (byte) 0xC3, 0x28                       // invalid continuation byte
        };

        var failure = assertThrows(TextCodec.MalformedDocumentException.class,
                () -> TextCodec.decode(bytes));

        assertEquals(StandardCharsets.UTF_8, failure.declaredCharset());
        assertTrue(failure.declaredByBom());
    }

    @Test
    void truncatedUtf16LeBomIsRejected() {
        // "A" plus a dangling low byte: an odd-length UTF-16LE payload
        var bytes = new byte[]{(byte) 0xFF, (byte) 0xFE, 0x41, 0x00, 0x42};

        var failure = assertThrows(TextCodec.MalformedDocumentException.class,
                () -> TextCodec.decode(bytes));

        assertEquals(StandardCharsets.UTF_16LE, failure.declaredCharset());
        assertTrue(failure.declaredByBom());
    }

    @Test
    void truncatedUtf16BeBomIsRejected() {
        var bytes = new byte[]{(byte) 0xFE, (byte) 0xFF, 0x00, 0x41, 0x00};

        var failure = assertThrows(TextCodec.MalformedDocumentException.class,
                () -> TextCodec.decode(bytes));

        assertEquals(StandardCharsets.UTF_16BE, failure.declaredCharset());
        assertTrue(failure.declaredByBom());
    }

    @Test
    void loneSurrogateInUtf16BomIsRejected() {
        // U+D800 high surrogate with no low surrogate after it
        var bytes = new byte[]{(byte) 0xFF, (byte) 0xFE, 0x00, (byte) 0xD8};

        var failure = assertThrows(TextCodec.MalformedDocumentException.class,
                () -> TextCodec.decode(bytes));

        assertEquals(StandardCharsets.UTF_16LE, failure.declaredCharset());
    }

    @Test
    void replacementCharactersOnlyAppearWhenExplicitlyRequested() {
        var bytes = new byte[]{
                (byte) 0xEF, (byte) 0xBB, (byte) 0xBF,
                (byte) 0xC3, 0x28
        };

        var lossy = TextCodec.decodeBomWithReplacement(bytes, StandardCharsets.UTF_8);

        assertTrue(lossy.text().contains("\uFFFD"));
        assertEquals(StandardCharsets.UTF_8, lossy.charset());
        assertTrue(lossy.bom());
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