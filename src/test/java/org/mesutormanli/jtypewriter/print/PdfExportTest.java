/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.print;

import org.junit.jupiter.api.Test;
import org.mesutormanli.jtypewriter.ui.AppFonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PDF half must be a real, self-contained file and a deterministic one.
 * These tests read the bytes back with a hand-written parser rather than a PDF
 * library: the point is to prove the cross-reference table, stream lengths and
 * embedded font are correct, not that one reader happens to tolerate them.
 */
class PdfExportTest {

    private static final Pattern STREAM = Pattern.compile("<<([^>]*)>>\nstream\n");
    private static final Pattern LENGTH = Pattern.compile("/Length (\\d+)");

    @Test
    void writesAHeaderAndTrailerAroundTheDocument() {
        String pdf = latin(PdfExport.render("Hello"));

        assertTrue(pdf.startsWith("%PDF-1.7\n"));
        assertTrue(pdf.endsWith("%%EOF\n"));
        assertTrue(pdf.contains("/Type /Catalog"));
        assertTrue(pdf.contains("/Root 1 0 R"));
        assertTrue(pdf.contains("trailer\n"));
    }

    @Test
    void everyCrossReferenceOffsetPointsAtItsObject() {
        byte[] bytes = PdfExport.render("Hello");
        String pdf = latin(bytes);

        int startXrefMarker = pdf.lastIndexOf("startxref\n");
        int startXref = Integer.parseInt(
                pdf.substring(startXrefMarker + "startxref\n".length(),
                        pdf.indexOf('\n', startXrefMarker + "startxref\n".length())).trim());
        assertEquals("xref", pdf.substring(startXref, startXref + 4));

        int headerEnd = pdf.indexOf('\n', startXref + "xref\n".length());
        String[] header = pdf.substring(startXref + "xref\n".length(), headerEnd)
                .trim().split("\\s+");
        int size = Integer.parseInt(header[1]);
        int entryStart = headerEnd + 1;
        for (int object = 0; object < size; object++) {
            String entry = pdf.substring(entryStart + object * 20, entryStart + object * 20 + 20);
            assertEquals(20, entry.length());
            int offset = Integer.parseInt(entry.substring(0, 10));
            if (object == 0) {
                assertTrue(entry.endsWith("f \n"));
            } else {
                assertTrue(pdf.startsWith(object + " 0 obj", offset),
                        "xref entry " + object + " points at the wrong bytes");
            }
        }
    }

    @Test
    void everyStreamLengthMatchesTheBytesBeforeEndstream() {
        byte[] bytes = PdfExport.render("stream lengths\nsecond line");
        String pdf = latin(bytes);
        Matcher matcher = STREAM.matcher(pdf);
        int streams = 0;
        while (matcher.find()) {
            String dictionary = matcher.group(1);
            Matcher lengthMatcher = LENGTH.matcher(dictionary);
            assertTrue(lengthMatcher.find(), "stream has no /Length");
            int length = Integer.parseInt(lengthMatcher.group(1));
            int start = matcher.end();
            int end = start + length;
            assertTrue(end <= pdf.length(), "stream runs past the file");
            assertEquals("endstream", pdf.substring(end, end + 9));
            if (dictionary.contains("/Filter /FlateDecode")) {
                inflate(Arrays.copyOfRange(bytes, start, end));
            }
            streams++;
        }
        assertTrue(streams > 0, "the document has no streams");
    }

    @Test
    void theMediaBoxIsA4AndThePageCountMatchesTheLayout() {
        String text = String.join("\n", Collections.nCopies(100, "line"));
        byte[] bytes = PdfExport.render(text);
        String pdf = latin(bytes);

        assertTrue(pdf.contains("/MediaBox [0 0 595.276 841.890]"));
        int expected = DocumentLayout.paginate(text, PageGeometry.a4(),
                DocumentLayout.monoMetrics()).size();
        assertEquals(expected, count(pdf, "/Type /Page /Parent"));
    }

    @Test
    void theEmbeddedFontFileIsExactlyTheBundledFace() throws IOException {
        byte[] bytes = PdfExport.render("Hello");
        String pdf = latin(bytes);
        byte[] bundled;
        try (InputStream stream = PdfExportTest.class.getResourceAsStream(AppFonts.MONO_RESOURCE)) {
            bundled = stream.readAllBytes();
        }

        Matcher matcher = STREAM.matcher(pdf);
        boolean found = false;
        while (matcher.find()) {
            String dictionary = matcher.group(1);
            if (!dictionary.contains("/Length1")) {
                continue;
            }
            Matcher lengthMatcher = LENGTH.matcher(dictionary);
            assertTrue(lengthMatcher.find());
            int length = Integer.parseInt(lengthMatcher.group(1));
            byte[] embedded = inflate(Arrays.copyOfRange(bytes, matcher.end(),
                    matcher.end() + length));
            assertArrayEquals(bundled, embedded);
            found = true;
        }
        assertTrue(found, "the font was not embedded as /FontFile2");
    }

    @Test
    void toUnicodeMapsTheSampleLineBackToItsCodePoints() {
        String text = "Hello, ş ğ ı İ ç ö ü";
        String decoded = decodeText(PdfExport.render(text));

        assertEquals(text, decoded);
    }

    @Test
    void emptyTextStillProducesOnePage() {
        String pdf = latin(PdfExport.render(""));

        assertEquals(1, count(pdf, "/Type /Page /Parent"));
        assertTrue(pdf.contains("BT\n/F1 12 Tf\n0 0 0 rg\n"));
    }

    @Test
    void twoRendersOfTheSameTextAreByteIdentical() {
        String text = "ş ğ ı İ ç ö ü\nsecond\tline\n\n" + "x".repeat(300);

        assertArrayEquals(PdfExport.render(text), PdfExport.render(text));
    }

    @Test
    void theContentStreamPaintsWhitePaperThenBlackTextWithoutCss() {
        String content = firstPageContent(PdfExport.render("Hello"));

        assertTrue(content.contains("1 1 1 rg"));
        assertTrue(content.contains("0 0 0 rg"));
        assertTrue(content.indexOf("1 1 1 rg") < content.indexOf("BT"));
        assertTrue(content.contains("/F1 12 Tf"));
    }

    private static String decodeText(byte[] bytes) {
        Map<Integer, String> glyphToCodePoint = parseToUnicode(bytes);
        String content = firstPageContent(bytes);

        StringBuilder decoded = new StringBuilder();
        Matcher matcher = Pattern.compile("Tm <([0-9A-F]*)> Tj").matcher(content);
        boolean first = true;
        while (matcher.find()) {
            if (!first) {
                decoded.append('\n');
            }
            first = false;
            String glyphs = matcher.group(1);
            for (int i = 0; i < glyphs.length(); i += 4) {
                int glyph = Integer.parseInt(glyphs.substring(i, i + 4), 16);
                decoded.append(glyphToCodePoint.getOrDefault(glyph, "\uFFFD"));
            }
        }
        return decoded.toString();
    }

    private static Map<Integer, String> parseToUnicode(byte[] bytes) {
        String pdf = latin(bytes);
        Matcher matcher = STREAM.matcher(pdf);
        while (matcher.find()) {
            String dictionary = matcher.group(1);
            if (dictionary.contains("/Length1") || !dictionary.contains("/FlateDecode")) {
                continue;
            }
            Matcher lengthMatcher = LENGTH.matcher(dictionary);
            assertTrue(lengthMatcher.find());
            int length = Integer.parseInt(lengthMatcher.group(1));
            String cmap = new String(inflate(Arrays.copyOfRange(bytes,
                    matcher.end(), matcher.end() + length)), StandardCharsets.ISO_8859_1);
            if (!cmap.contains("begincmap")) {
                continue;
            }
            Map<Integer, String> mapping = new HashMap<>();
            Matcher pair = Pattern.compile("<([0-9A-F]{4})> <([0-9A-F]{4,8})>")
                    .matcher(cmap);
            while (pair.find()) {
                int glyph = Integer.parseInt(pair.group(1), 16);
                mapping.put(glyph, utf16(pair.group(2)));
            }
            return mapping;
        }
        throw new AssertionError("the document has no /ToUnicode stream");
    }

    private static String firstPageContent(byte[] bytes) {
        String pdf = latin(bytes);
        Matcher matcher = STREAM.matcher(pdf);
        while (matcher.find()) {
            String dictionary = matcher.group(1);
            if (dictionary.contains("/Filter")) {
                continue;
            }
            Matcher lengthMatcher = LENGTH.matcher(dictionary);
            assertTrue(lengthMatcher.find());
            int length = Integer.parseInt(lengthMatcher.group(1));
            String content = pdf.substring(matcher.end(), matcher.end() + length);
            if (content.contains("BT\n")) {
                return content;
            }
        }
        throw new AssertionError("the document has no content stream");
    }

    private static String utf16(String hex) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 4) {
            decoded.append((char) Integer.parseInt(hex.substring(i, i + 4), 16));
        }
        return decoded.toString();
    }

    private static byte[] inflate(byte[] compressed) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int read = inflater.inflate(buffer);
                if (read == 0 && inflater.needsInput()) {
                    break;
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (DataFormatException error) {
            throw new AssertionError("stream is not valid Flate data", error);
        } finally {
            inflater.end();
        }
    }

    private static String latin(byte[] bytes) {
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int index = text.indexOf(needle); index >= 0;
             index = text.indexOf(needle, index + needle.length())) {
            count++;
        }
        return count;
    }
}
