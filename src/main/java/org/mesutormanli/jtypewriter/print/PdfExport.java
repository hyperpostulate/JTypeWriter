/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.print;

import org.mesutormanli.jtypewriter.ui.AppFonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;

/**
 * Renders a document as a PDF file's bytes: the white sheet, the black 12 pt
 * text, the embedded monospace face and nothing from the window.
 *
 * <p>Deliberately dependency-free. A PDF writer is included rather than a PDF
 * library because the requirement is one font, one size, one colour and lines
 * of text - and because adding a library would put megabytes into the
 * application image and a second licence into {@code THIRD-PARTY-NOTICES} for
 * features nothing here uses.</p>
 */
public final class PdfExport {

    private static final String BASE_FONT = "DejaVuSansMono";
    private static final int DEFAULT_WIDTH = 600;

    private PdfExport() {
    }

    /**
     * The PDF document for {@code text} on {@link PageGeometry#a4()}.
     *
     * <p>Pure and headless: no JavaFX, no toolkit, no display, no console. The
     * font is the bundled monospace face, embedded whole, so the file is
     * self-contained and looks the same on a machine that has never seen
     * JTypeWriter.</p>
     */
    public static byte[] render(String text) {
        byte[] fontBytes = readMonoFont();
        TrueTypeFont font = new TrueTypeFont(fontBytes);
        PageGeometry geometry = PageGeometry.a4();
        List<DocumentLayout.Page> pages =
                DocumentLayout.paginate(text, geometry, DocumentLayout.monoMetrics());

        TreeMap<Integer, Integer> glyphToCodePoint = usedGlyphs(pages, font);
        int pageCount = pages.size();

        int firstPageObject = 3;
        int type0Object = firstPageObject + pageCount;
        int cidObject = type0Object + 1;
        int descriptorObject = cidObject + 1;
        int fontFileObject = descriptorObject + 1;
        int toUnicodeObject = fontFileObject + 1;
        int firstContentObject = toUnicodeObject + 1;

        PdfWriter writer = new PdfWriter();
        writer.addObject(ascii("<< /Type /Catalog /Pages 2 0 R >>\n"));

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(firstPageObject + i).append(" 0 R ");
        }
        writer.addObject(ascii("<< /Type /Pages /Kids [" + kids.toString().strip()
                + "] /Count " + pageCount + " >>\n"));

        for (int i = 0; i < pageCount; i++) {
            writer.addObject(ascii("<< /Type /Page /Parent 2 0 R"
                    + " /MediaBox [0 0 " + decimal(geometry.width())
                    + " " + decimal(geometry.height()) + "]"
                    + " /Resources << /Font << /F1 " + type0Object + " 0 R >> >>"
                    + " /Contents " + (firstContentObject + i) + " 0 R >>\n"));
        }

        writer.addObject(ascii("<< /Type /Font /Subtype /Type0 /BaseFont /" + BASE_FONT
                + " /Encoding /Identity-H /DescendantFonts [" + cidObject + " 0 R]"
                + " /ToUnicode " + toUnicodeObject + " 0 R >>\n"));

        writer.addObject(ascii("<< /Type /Font /Subtype /CIDFontType2 /BaseFont /" + BASE_FONT
                + " /CIDSystemInfo << /Registry (Adobe) /Ordering (Identity)"
                + " /Supplement 0 >>"
                + " /FontDescriptor " + descriptorObject + " 0 R"
                + " /DW " + DEFAULT_WIDTH
                + " /W [" + widths(glyphToCodePoint, font) + "]"
                + " /CIDToGIDMap /Identity >>\n"));

        writer.addObject(ascii(descriptor(font, fontFileObject)));

        byte[] compressedFont = deflate(fontBytes);
        writer.addObject(stream(compressedFont,
                "<< /Length " + compressedFont.length + " /Filter /FlateDecode"
                        + " /Length1 " + fontBytes.length + " >>"));

        byte[] toUnicode = toUnicode(glyphToCodePoint).getBytes(StandardCharsets.US_ASCII);
        byte[] compressedToUnicode = deflate(toUnicode);
        writer.addObject(stream(compressedToUnicode,
                "<< /Length " + compressedToUnicode.length
                        + " /Filter /FlateDecode >>"));

        for (DocumentLayout.Page page : pages) {
            byte[] content = content(page, geometry, font);
            writer.addObject(stream(content, "<< /Length " + content.length + " >>"));
        }

        return writer.finish(1);
    }

    /**
     * Maps each used glyph to a code point. A tree map fixes the order, so the
     * {@code /W} array and the {@code ToUnicode} pairs do not depend on hash
     * iteration. Glyph 0 is the missing-glyph bucket and deliberately gets no
     * reverse mapping: a code point the face cannot draw must not be extracted
     * as some other character.
     */
    private static TreeMap<Integer, Integer> usedGlyphs(List<DocumentLayout.Page> pages,
                                                        TrueTypeFont font) {
        TreeMap<Integer, Integer> used = new TreeMap<>();
        for (DocumentLayout.Page page : pages) {
            for (DocumentLayout.Line line : page.lines()) {
                for (int i = 0; i < line.text().length(); ) {
                    int codePoint = line.text().codePointAt(i);
                    int glyph = font.glyphId(codePoint);
                    if (glyph != 0) {
                        used.putIfAbsent(glyph, codePoint);
                    }
                    i += Character.charCount(codePoint);
                }
            }
        }
        return used;
    }

    private static String widths(Map<Integer, Integer> glyphs, TrueTypeFont font) {
        StringBuilder widths = new StringBuilder();
        for (int glyph : glyphs.keySet()) {
            int width = scale(font.advanceWidth(glyph), font.unitsPerEm());
            widths.append(glyph).append(" [").append(width).append("] ");
        }
        return widths.toString().strip();
    }

    private static String descriptor(TrueTypeFont font, int fontFileObject) {
        int[] bbox = font.fontBBox();
        StringBuilder descriptor = new StringBuilder();
        descriptor.append("<< /Type /FontDescriptor /FontName /").append(BASE_FONT)
                .append(" /FontBBox [")
                .append(scale(bbox[0], font.unitsPerEm())).append(' ')
                .append(scale(bbox[1], font.unitsPerEm())).append(' ')
                .append(scale(bbox[2], font.unitsPerEm())).append(' ')
                .append(scale(bbox[3], font.unitsPerEm())).append(']')
                .append(" /Flags 32 /ItalicAngle 0 /StemV 80")
                .append(" /Ascent ").append(scale(font.ascent(), font.unitsPerEm()))
                .append(" /Descent ").append(scale(font.descent(), font.unitsPerEm()));
        if (font.capHeight() != 0) {
            descriptor.append(" /CapHeight ")
                    .append(scale(font.capHeight(), font.unitsPerEm()));
        }
        descriptor.append(" /FontFile2 ").append(fontFileObject).append(" 0 R >>\n");
        return descriptor.toString();
    }

    private static String toUnicode(Map<Integer, Integer> glyphs) {
        StringBuilder cmap = new StringBuilder();
        cmap.append("/CIDInit /ProcSet findresource begin\n")
                .append("12 dict begin\n")
                .append("begincmap\n")
                .append("/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS)")
                .append(" /Supplement 0 >> def\n")
                .append("/CMapName /Adobe-Identity-UCS def\n")
                .append("/CMapType 2 def\n")
                .append("1 begincodespacerange\n")
                .append("<0000> <FFFF>\n")
                .append("endcodespacerange\n");

        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(glyphs.entrySet());
        for (int start = 0; start < entries.size(); start += 100) {
            int end = Math.min(start + 100, entries.size());
            cmap.append(end - start).append(" beginbfchar\n");
            for (int i = start; i < end; i++) {
                Map.Entry<Integer, Integer> entry = entries.get(i);
                cmap.append('<').append(hex4(entry.getKey())).append("> <")
                        .append(utf16Hex(entry.getValue())).append(">\n");
            }
            cmap.append("endbfchar\n");
        }

        cmap.append("endcmap\n")
                .append("CMapName currentdict /CMap defineresource pop\n")
                .append("end\n")
                .append("end\n");
        return cmap.toString();
    }

    /**
     * One page's content: paint the sheet white explicitly, then a single text
     * object. PDF's origin is bottom-left while layout baselines are measured
     * from the top, so each baseline is mirrored through the page height.
     */
    private static byte[] content(DocumentLayout.Page page, PageGeometry geometry,
                                  TrueTypeFont font) {
        StringBuilder content = new StringBuilder();
        content.append("q\n")
                .append("1 1 1 rg\n")
                .append("0 0 ").append(decimal(geometry.width())).append(' ')
                .append(decimal(geometry.height())).append(" re f\n")
                .append("Q\n")
                .append("BT\n")
                .append("/F1 ").append(size()).append(" Tf\n")
                .append("0 0 0 rg\n");
        for (DocumentLayout.Line line : page.lines()) {
            content.append("1 0 0 1 ").append(decimal(line.x())).append(' ')
                    .append(decimal(geometry.height() - line.baseline())).append(" Tm <")
                    .append(glyphHex(line.text(), font)).append("> Tj\n");
        }
        content.append("ET\n");
        return content.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private static String glyphHex(String text, TrueTypeFont font) {
        StringBuilder hex = new StringBuilder(text.length() * 4);
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            hex.append(hex4(font.glyphId(codePoint)));
            i += Character.charCount(codePoint);
        }
        return hex.toString();
    }

    private static String utf16Hex(int codePoint) {
        StringBuilder hex = new StringBuilder(8);
        for (char unit : Character.toChars(codePoint)) {
            hex.append(hex4(unit));
        }
        return hex.toString();
    }

    private static String hex4(int value) {
        String hex = Integer.toHexString(value & 0xFFFF).toUpperCase(Locale.ROOT);
        return "0".repeat(4 - hex.length()) + hex;
    }

    private static int scale(int fontUnits, int unitsPerEm) {
        return (int) Math.round(fontUnits * 1000.0 / unitsPerEm);
    }

    /** A PDF number with three decimals, formatted without the default locale. */
    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String size() {
        double size = DocumentLayout.FONT_SIZE;
        if (size == Math.rint(size)) {
            return Long.toString((long) size);
        }
        return decimal(size);
    }

    private static byte[] stream(byte[] data, String dictionary) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(ascii(dictionary + "\nstream\n"));
        body.writeBytes(data);
        body.writeBytes(ascii("endstream\n"));
        return body.toByteArray();
    }

    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        try {
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                int written = deflater.deflate(buffer);
                compressed.write(buffer, 0, written);
            }
            return compressed.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] readMonoFont() {
        try (InputStream stream = PdfExport.class.getResourceAsStream(AppFonts.MONO_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("bundled font " + AppFonts.MONO_RESOURCE
                        + " is not on the classpath");
            }
            return stream.readAllBytes();
        } catch (IOException error) {
            throw new IllegalStateException("bundled font " + AppFonts.MONO_RESOURCE
                    + " cannot be read", error);
        }
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }
}
