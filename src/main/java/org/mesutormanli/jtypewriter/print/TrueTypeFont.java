/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.print;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The small part of a TrueType file that PDF export needs: units per em, glyph
 * ids for code points, advances and the vertical metrics.
 *
 * <p>Deliberately a parser rather than a dependency. The export embeds the
 * complete bundled file and only has to answer three questions about it - which
 * glyph does this code point use, how wide is that glyph, and where are the
 * font's boxes - so a full font library would be a large amount of code and
 * another licence for nothing. The file is read as a byte array and every
 * offset is bounds-checked: a truncated or non-font file fails with
 * {@link IllegalStateException} instead of writing a broken PDF.</p>
 *
 * <p>Instances are immutable after construction. Table order is not part of the
 * result, so a hash map is safe here; nothing iterates it when producing
 * output.</p>
 */
final class TrueTypeFont {

    private final byte[] data;
    private final int unitsPerEm;
    private final int numGlyphs;
    private final int numberOfHMetrics;
    private final int hmtxOffset;
    private final int hmtxLength;
    private final int ascent;
    private final int descent;
    private final int xMin;
    private final int yMin;
    private final int xMax;
    private final int yMax;
    private final int capHeight;
    private final Cmap cmap;

    TrueTypeFont(byte[] data) {
        if (data == null || data.length < 12) {
            throw new IllegalStateException("not a TrueType font: file is too short");
        }
        this.data = data.clone();
        int tableCount = u16(4);
        if (tableCount <= 0 || 12L + 16L * tableCount > this.data.length) {
            throw new IllegalStateException("not a TrueType font: bad table directory");
        }
        Map<String, int[]> tables = new HashMap<>();
        for (int i = 0; i < tableCount; i++) {
            int record = 12 + i * 16;
            String tag = new String(this.data, record, 4, StandardCharsets.ISO_8859_1);
            int offset = (int) u32(record + 8);
            int length = (int) u32(record + 12);
            if (offset < 0 || length < 0 || (long) offset + length > this.data.length) {
                throw new IllegalStateException("not a TrueType font: table " + tag
                        + " lies outside the file");
            }
            tables.put(tag, new int[]{offset, length});
        }

        int[] head = require(tables, "head", 54);
        int[] hhea = require(tables, "hhea", 36);
        int[] maxp = require(tables, "maxp", 6);
        int[] hmtx = require(tables, "hmtx", 4);
        int[] cmapTable = require(tables, "cmap", 4);

        unitsPerEm = u16(head[0] + 18);
        if (unitsPerEm <= 0) {
            throw new IllegalStateException("not a TrueType font: unitsPerEm is zero");
        }
        xMin = i16(head[0] + 36);
        yMin = i16(head[0] + 38);
        xMax = i16(head[0] + 40);
        yMax = i16(head[0] + 42);

        ascent = i16(hhea[0] + 4);
        descent = i16(hhea[0] + 6);
        numberOfHMetrics = u16(hhea[0] + 34);
        if (numberOfHMetrics <= 0 || numberOfHMetrics * 4L > hmtx[1]) {
            throw new IllegalStateException("not a TrueType font: bad horizontal metrics");
        }
        hmtxOffset = hmtx[0];
        hmtxLength = hmtx[1];

        numGlyphs = u16(maxp[0] + 4);
        if (numGlyphs <= 0) {
            throw new IllegalStateException("not a TrueType font: no glyphs");
        }

        int[] os2 = tables.get("OS/2");
        capHeight = capHeightFrom(os2);

        cmap = Cmap.parse(this.data, cmapTable[0], numGlyphs);
    }

    private int capHeightFrom(int[] os2) {
        if (os2 == null || os2[1] < 90) {
            return 0;
        }
        int version = u16(os2[0]);
        if (version < 2) {
            return 0;
        }
        return i16(os2[0] + 88);
    }

    private int[] require(Map<String, int[]> tables, String tag, int minimumLength) {
        int[] table = tables.get(tag);
        if (table == null) {
            throw new IllegalStateException("not a TrueType font: missing " + tag + " table");
        }
        if (table[1] < minimumLength) {
            throw new IllegalStateException("not a TrueType font: short " + tag + " table");
        }
        return table;
    }

    /** The design grid the font's coordinates are expressed in. */
    int unitsPerEm() {
        return unitsPerEm;
    }

    /** Number of glyphs in the font's {@code glyf}/{@code loca} tables. */
    int numGlyphs() {
        return numGlyphs;
    }

    /** The glyph id for {@code codePoint}, or 0 when the face has no glyph for it. */
    int glyphId(int codePoint) {
        return cmap.glyphId(codePoint);
    }

    /** The advance of {@code glyphId} in font units. */
    int advanceWidth(int glyphId) {
        if (glyphId < 0 || glyphId >= numGlyphs) {
            return 0;
        }
        int index = Math.min(glyphId, numberOfHMetrics - 1);
        int offset = hmtxOffset + index * 4;
        if (offset + 2 > hmtxOffset + hmtxLength) {
            throw new IllegalStateException("not a TrueType font: glyph outside hmtx");
        }
        return u16(offset);
    }

    int ascent() {
        return ascent;
    }

    int descent() {
        return descent;
    }

    int[] fontBBox() {
        return new int[]{xMin, yMin, xMax, yMax};
    }

    /** The OS/2 cap height in font units, or 0 when the table does not carry it. */
    int capHeight() {
        return capHeight;
    }

    private int u16(int offset) {
        if (offset < 0 || offset + 2 > data.length) {
            throw new IllegalStateException("not a TrueType font: read outside the file");
        }
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private int i16(int offset) {
        return (short) u16(offset);
    }

    private long u32(int offset) {
        if (offset < 0 || offset + 4 > data.length) {
            throw new IllegalStateException("not a TrueType font: read outside the file");
        }
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    /**
     * The character-to-glyph mapping, holding either a format 4 BMP segment
     * table or a format 12 group table. Format 12 is preferred when both are
     * present because it also covers code points above the BMP; DejaVu ships
     * both, and either answers the same for Turkish.
     */
    private static final class Cmap {

        private final byte[] data;
        private final int format;
        private final int offset;
        private final int segmentCount;
        private final int endCodes;
        private final int startCodes;
        private final int idDeltas;
        private final int idRangeOffsets;
        private final int groupCount;
        private final int glyphCount;

        private Cmap(byte[] data, int format, int offset, int segmentCount, int groupCount,
                     int glyphCount) {
            this.data = data;
            this.format = format;
            this.offset = offset;
            this.segmentCount = segmentCount;
            this.groupCount = groupCount;
            this.glyphCount = glyphCount;
            this.endCodes = offset + 14;
            this.startCodes = endCodes + segmentCount * 2 + 2;
            this.idDeltas = startCodes + segmentCount * 2;
            this.idRangeOffsets = idDeltas + segmentCount * 2;
        }

        static Cmap parse(byte[] data, int cmapOffset, int glyphCount) {
            int count = u16(data, cmapOffset + 2);
            int bestOffset = -1;
            int bestFormat = 0;
            int bestRank = -1;
            for (int i = 0; i < count; i++) {
                int record = cmapOffset + 4 + i * 8;
                int platform = u16(data, record);
                int encoding = u16(data, record + 2);
                int subtable = cmapOffset + (int) u32(data, record + 4);
                if (subtable <= cmapOffset || subtable + 2 > data.length) {
                    continue;
                }
                int format = u16(data, subtable);
                int rank = -1;
                if (platform == 3 && encoding == 10 && format == 12) {
                    rank = 5;
                } else if (platform == 0 && format == 12) {
                    rank = 4;
                } else if (platform == 3 && encoding == 1 && format == 4) {
                    rank = 3;
                } else if (platform == 0 && format == 4) {
                    rank = 2;
                }
                if (rank > bestRank) {
                    bestRank = rank;
                    bestOffset = subtable;
                    bestFormat = format;
                }
            }
            if (bestOffset < 0) {
                throw new IllegalStateException("not a TrueType font: no Unicode cmap subtable");
            }
            if (bestFormat == 12) {
                int groups = (int) u32(data, bestOffset + 12);
                if (bestOffset + 16L + (long) groups * 12 > data.length) {
                    throw new IllegalStateException("not a TrueType font: short format 12 cmap");
                }
                return new Cmap(data, 12, bestOffset, 0, groups, glyphCount);
            }
            int segments = u16(data, bestOffset + 6) / 2;
            if (segments <= 0
                    || bestOffset + 16L + (long) segments * 8 > data.length) {
                throw new IllegalStateException("not a TrueType font: short format 4 cmap");
            }
            return new Cmap(data, 4, bestOffset, segments, 0, glyphCount);
        }

        int glyphId(int codePoint) {
            if (codePoint < 0 || codePoint > 0x10FFFF) {
                return 0;
            }
            return format == 12 ? glyph12(codePoint) : glyph4(codePoint);
        }

        private int glyph12(int codePoint) {
            int low = 0;
            int high = groupCount - 1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                int group = offset + 16 + middle * 12;
                long start = u32(data, group);
                long end = u32(data, group + 4);
                if (codePoint < start) {
                    high = middle - 1;
                } else if (codePoint > end) {
                    low = middle + 1;
                } else {
                    long glyph = u32(data, group + 8) + (codePoint - start);
                    return glyph >= glyphCount ? 0 : (int) glyph;
                }
            }
            return 0;
        }

        private int glyph4(int codePoint) {
            if (codePoint > 0xFFFF) {
                return 0;
            }
            for (int i = 0; i < segmentCount; i++) {
                int end = u16(data, endCodes + i * 2);
                if (codePoint <= end) {
                    int start = u16(data, startCodes + i * 2);
                    if (codePoint < start) {
                        return 0;
                    }
                    int delta = ((data[idDeltas + i * 2] & 0xFF) << 8)
                            | (data[idDeltas + i * 2 + 1] & 0xFF);
                    int rangeOffset = ((data[idRangeOffsets + i * 2] & 0xFF) << 8)
                            | (data[idRangeOffsets + i * 2 + 1] & 0xFF);
                    if (rangeOffset == 0) {
                        return (codePoint + delta) & 0xFFFF;
                    }
                    int glyphAddress = idRangeOffsets + i * 2 + rangeOffset
                            + (codePoint - start) * 2;
                    int glyph = u16(data, glyphAddress);
                    return glyph == 0 ? 0 : (glyph + delta) & 0xFFFF;
                }
            }
            return 0;
        }

        private static int u16(byte[] data, int offset) {
            if (offset < 0 || offset + 2 > data.length) {
                throw new IllegalStateException("not a TrueType font: read outside the file");
            }
            return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        }

        private static long u32(byte[] data, int offset) {
            if (offset < 0 || offset + 4 > data.length) {
                throw new IllegalStateException("not a TrueType font: read outside the file");
            }
            return ((long) (data[offset] & 0xFF) << 24)
                    | ((long) (data[offset + 1] & 0xFF) << 16)
                    | ((long) (data[offset + 2] & 0xFF) << 8)
                    | (data[offset + 3] & 0xFF);
        }
    }
}
