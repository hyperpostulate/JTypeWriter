/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the bundled icon assets without starting the JavaFX toolkit or
 * constructing a {@link javafx.stage.Stage}. The PNGs are inspected by parsing
 * their bytes directly, so this stays a pure classpath/format check.
 */
class AppIconsTest {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Test
    void windowIconResourcesAreListed() {
        var resources = AppIcons.windowIconResources();

        assertFalse(resources.isEmpty(), "at least one window icon size must be listed");
        for (var name : resources) {
            assertNotNull(AppIcons.class.getResource(name), "missing on classpath: " + name);
        }
    }

    @Test
    void windowIconResourcesAreUnmodifiable() {
        var resources = AppIcons.windowIconResources();

        assertThrows(UnsupportedOperationException.class, () -> resources.add("/icons/extra.png"));
    }

    @Test
    void windowIconsArePngsWithTheAdvertisedSize() throws IOException {
        for (var name : AppIcons.windowIconResources()) {
            var bytes = read(name);

            assertArrayEquals(PNG_SIGNATURE, Arrays.copyOf(bytes, PNG_SIGNATURE.length),
                    "not a PNG: " + name);
            assertEquals("IHDR", new String(bytes, 12, 4, StandardCharsets.US_ASCII),
                    "first PNG chunk is not IHDR: " + name);

            var width = readInt(bytes, 16);
            var height = readInt(bytes, 20);
            var expected = sizeFromName(name);
            assertEquals(expected, width, "width in " + name);
            assertEquals(expected, height, "height in " + name);
        }
    }

    /**
     * The welcome and about windows draw {@link AppIcons#dialogIconResource()}
     * as an {@code ImageView} inside the scene, so the dialog artwork must be
     * the single high-resolution PNG the code advertises.
     */
    @Test
    void dialogIconIsTheBundled512Png() throws IOException {
        var name = AppIcons.dialogIconResource();
        var bytes = read(name);

        assertArrayEquals(PNG_SIGNATURE, Arrays.copyOf(bytes, PNG_SIGNATURE.length),
                "not a PNG: " + name);
        assertEquals(512, readInt(bytes, 16), "width in " + name);
        assertEquals(512, readInt(bytes, 20), "height in " + name);
    }

    @Test
    void packagingAssetsArePresentWithTheirMagicBytes() throws IOException {
        assertArrayEquals(new byte[]{0, 0, 1, 0}, Arrays.copyOf(read("/icons/jtypewriter.ico"), 4),
                "not an ICO: /icons/jtypewriter.ico");
        assertArrayEquals("icns".getBytes(StandardCharsets.US_ASCII),
                Arrays.copyOf(read("/icons/jtypewriter.icns"), 4),
                "not an ICNS: /icons/jtypewriter.icns");
    }

    private static byte[] read(String name) throws IOException {
        try (InputStream in = AppIcons.class.getResourceAsStream(name)) {
            assertNotNull(in, "missing on classpath: " + name);
            return in.readAllBytes();
        }
    }

    /** Big-endian unsigned 32-bit value at the given offset. */
    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private static int sizeFromName(String name) {
        var base = name.substring(name.lastIndexOf('-') + 1, name.lastIndexOf('.'));
        return Integer.parseInt(base);
    }

    @Test
    void listOrderIsAscending() {
        List<Integer> sizes = AppIcons.windowIconResources().stream()
                .map(AppIconsTest::sizeFromName)
                .toList();

        for (var i = 1; i < sizes.size(); i++) {
            assertTrue(sizes.get(i - 1) < sizes.get(i), "icon sizes must ascend: " + sizes);
        }
    }
}
