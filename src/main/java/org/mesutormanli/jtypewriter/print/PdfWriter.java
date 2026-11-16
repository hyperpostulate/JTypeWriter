/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.print;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes the object table, cross-reference table and trailer of a PDF file.
 *
 * <p>Objects are added in their final numerical order: object {@code n} is the
 * {@code n}-th call. The cross-reference table is built from the byte offset of
 * each object as it is written, exactly 20 bytes per entry as the format
 * requires, and the trailer carries no identifier or timestamp. That is what
 * makes two renders of the same text byte-for-byte identical: nothing in this
 * class depends on the clock, the host or hash iteration order.</p>
 */
final class PdfWriter {

    private static final Charset ASCII = StandardCharsets.US_ASCII;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final List<Integer> offsets = new ArrayList<>();

    PdfWriter() {
        write("%PDF-1.7\n");
        // A comment with bytes above 127 tells tools the file is binary.
        out.writeBytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});
    }

    /** Appends one complete object body; its number is the call count. */
    void addObject(byte[] body) {
        offsets.add(out.size());
        write((offsets.size()) + " 0 obj\n");
        out.writeBytes(body);
        if (body.length > 0 && body[body.length - 1] != '\n') {
            out.write('\n');
        }
        write("endobj\n");
    }

    /** Appends the cross-reference table, trailer and end marker. */
    byte[] finish(int rootObject) {
        int startXref = out.size();
        write("xref\n");
        write("0 " + (offsets.size() + 1) + "\n");
        write("0000000000 65535 f \n");
        for (int offset : offsets) {
            write(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        write("trailer\n");
        write("<< /Size " + (offsets.size() + 1) + " /Root " + rootObject + " 0 R >>\n");
        write("startxref\n");
        write(startXref + "\n");
        write("%%EOF\n");
        return out.toByteArray();
    }

    private void write(String text) {
        out.writeBytes(text.getBytes(ASCII));
    }
}
