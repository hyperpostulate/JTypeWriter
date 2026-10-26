/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the packaging contract introduced for PW-34 (finding B31): the fat jar
 * and the application image must carry the third-party notices and the license
 * texts they reference, at the exact paths tooling and users look for.
 *
 * <p>This is a test rather than a build-time check because the artifact-level
 * assertions (entries inside the jar, files inside the image) live in
 * {@code scripts/package.sh}, where the artifacts actually exist. What a
 * headless test can prove is the content contract: that the resources are on
 * the classpath at the expected paths, that the notice names every bundled
 * component with its real license id, and that the classpath copy is not stale
 * relative to the checked-in file.</p>
 *
 * <p>No JavaFX toolkit, no filesystem writes, no network: resources are read
 * from the classpath the same way {@link AppVersion} reads its Maven metadata,
 * and the source file is read from the project root Surefire runs in.</p>
 */
class PackagingNoticesTest {

    private static final String NOTICE_RESOURCE = "/META-INF/THIRD-PARTY-NOTICES";
    private static final String GPL_RESOURCE =
            "/META-INF/licenses/GPL-2.0-with-classpath-exception.txt";
    private static final String LGPL_RESOURCE = "/META-INF/licenses/LGPL-2.1.txt";

    @Test
    void thirdPartyNoticesAreOnTheClasspath() throws IOException {
        var text = read(NOTICE_RESOURCE);
        assertNotNull(text, NOTICE_RESOURCE + " is not on the classpath; the copy-license "
                + "execution in pom.xml is not packing THIRD-PARTY-NOTICES");
        assertTrue(!text.isBlank(), NOTICE_RESOURCE + " exists but is empty");
    }

    @Test
    void licenseTextsAreOnTheClasspath() throws IOException {
        var gpl = read(GPL_RESOURCE);
        assertNotNull(gpl, GPL_RESOURCE + " is not on the classpath");
        assertTrue(gpl.contains("\"CLASSPATH\" EXCEPTION TO THE GPL"),
                GPL_RESOURCE + " is not OpenJFX's LICENSE (no Classpath Exception section)");

        var lgpl = read(LGPL_RESOURCE);
        assertNotNull(lgpl, LGPL_RESOURCE + " is not on the classpath");
        assertTrue(lgpl.contains("GNU LESSER GENERAL PUBLIC LICENSE"),
                LGPL_RESOURCE + " does not contain the LGPL heading");
        assertTrue(lgpl.contains("Version 2.1"), LGPL_RESOURCE + " is not LGPL version 2.1");
    }

    @Test
    void everyBundledComponentIsNamed() throws IOException {
        var notice = read(NOTICE_RESOURCE);
        assertNotNull(notice, NOTICE_RESOURCE + " is not on the classpath");
        for (var component : List.of("JavaFX", "OpenJFX", "GStreamer", "KISS FFT",
                "Independent JPEG Group", "GCC")) {
            assertTrue(notice.contains(component),
                    NOTICE_RESOURCE + " does not name the bundled component: " + component);
        }
        for (var id : List.of("GPL-2.0-only WITH Classpath-exception-2.0",
                "LGPL-2.1-or-later", "GPL-3.0-or-later WITH GCC-exception-3.1")) {
            assertTrue(notice.contains(id),
                    NOTICE_RESOURCE + " does not state the license id: " + id);
        }
    }

    @Test
    void theNoticeSaysFfmpegIsNotBundled() throws IOException {
        var notice = read(NOTICE_RESOURCE);
        assertNotNull(notice, NOTICE_RESOURCE + " is not on the classpath");
        assertTrue(notice.contains("FFmpeg is not distributed"),
                NOTICE_RESOURCE + " must state plainly that FFmpeg is not distributed; "
                        + "the libavplugin*.so files only load it from the host");
    }

    @Test
    void zwspFreeAndLfOnly() throws IOException {
        var notice = read(NOTICE_RESOURCE);
        assertNotNull(notice, NOTICE_RESOURCE + " is not on the classpath");
        assertTrue(notice.indexOf('\u200B') < 0,
                NOTICE_RESOURCE + " contains a zero-width space at char "
                        + notice.indexOf('\u200B'));
        assertTrue(notice.indexOf('\r') < 0,
                NOTICE_RESOURCE + " contains a carriage return at char "
                        + notice.indexOf('\r'));

        var lines = notice.split("\n", -1);
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];
            assertTrue(line.length() <= 100,
                    NOTICE_RESOURCE + " line " + (i + 1) + " is " + line.length()
                            + " chars, over the 100-char limit: " + line);
            assertEquals(line.stripTrailing(), line,
                    NOTICE_RESOURCE + " line " + (i + 1) + " has trailing whitespace: " + line);
        }
    }

    @Test
    void theJarNoticeMatchesTheSourceFile() throws IOException {
        var fromClasspath = read(NOTICE_RESOURCE);
        assertNotNull(fromClasspath, NOTICE_RESOURCE + " is not on the classpath");
        var fromSource = Files.readString(Path.of("THIRD-PARTY-NOTICES"), StandardCharsets.UTF_8);
        assertEquals(fromSource, fromClasspath,
                "the classpath copy under target/classes/META-INF differs from the checked-in "
                        + "THIRD-PARTY-NOTICES; rebuild, and do not edit one without the other");
    }

    private static String read(String resource) throws IOException {
        try (InputStream stream = PackagingNoticesTest.class.getResourceAsStream(resource)) {
            if (stream == null) {
                return null;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
