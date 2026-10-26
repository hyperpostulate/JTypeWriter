/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the licence-provenance contract introduced for PW-37 (findings B34 and
 * B35): every Java source under {@code src/} carries an SPDX licence header as
 * the first thing in the file, and the canonical icon artwork declares its
 * author and licence too.
 *
 * <p>This is a test rather than a build-time check because this project has no
 * checkstyle and no spotless by design - a plugin would be a new build
 * dependency and a new formatting standard to maintain. A missing licence
 * header is a legal defect, not a style preference, so it should fail the suite
 * the way a broken invariant does, instead of surviving as a review note that
 * the next large diff quietly ignores.</p>
 *
 * <p>Headless and offline: the sources are read from the project root Surefire
 * runs in, no JavaFX toolkit is started, nothing is written, and no file count
 * is hardcoded - a new source file is checked automatically, and the failures
 * are reported together so one bad file cannot hide the others.</p>
 */
class LicenseHeaderTest {

    private static final String SPDX = "SPDX-License-Identifier: Apache-2.0";
    private static final String COPYRIGHT_HOLDER = "Mesut ORMANLI";

    private static final Path SOURCE_ROOT = Path.of("src");
    private static final Path ICON =
            Path.of("src", "main", "resources", "icons", "jtypewriter.svg");

    @Test
    void everyJavaSourceHasTheSpdxHeader() throws IOException {
        var sources = javaSources();
        assertTrue(!sources.isEmpty(),
                "found no .java files under " + SOURCE_ROOT.toAbsolutePath()
                        + "; Surefire is not running from the project root");

        var missing = new ArrayList<String>();
        for (var source : sources) {
            var block = firstCommentBlock(read(source));
            if (block.isEmpty()) {
                missing.add(source + " (no /* comment at all)");
            } else if (!block.contains(SPDX)) {
                missing.add(source + " (first /* comment has no " + SPDX + ")");
            }
        }

        assertTrue(missing.isEmpty(),
                missing.size() + " of " + sources.size() + " Java sources under " + SOURCE_ROOT
                        + " lack the licence header; run the insert script again:\n  "
                        + String.join("\n  ", missing));
    }

    @Test
    void theHeaderIsTheFirstThingInTheFile() throws IOException {
        var failures = new ArrayList<String>();
        for (var source : javaSources()) {
            var text = read(source);
            if (!text.startsWith("/*")) {
                failures.add(source + " does not begin with the licence comment");
                continue;
            }
            var blockEnd = text.indexOf("*/");
            if (blockEnd < 0) {
                failures.add(source + " opens a comment that is never closed");
                continue;
            }
            var block = text.substring(0, blockEnd);
            if (!block.contains(SPDX)) {
                failures.add(source + " starts with a comment that is not the licence block");
            }
            var packageAt = text.indexOf("package ");
            if (packageAt < 0) {
                failures.add(source + " has no package declaration");
            } else if (packageAt < blockEnd) {
                failures.add(source + " declares its package before the licence block");
            }
        }

        assertTrue(failures.isEmpty(),
                failures.size() + " Java sources do not open with the licence block at byte 0:\n  "
                        + String.join("\n  ", failures));
    }

    @Test
    void theIconDeclaresItsLicence() throws IOException {
        assertTrue(Files.isRegularFile(ICON), ICON + " is missing; it is the canonical artwork");
        var svg = Files.readString(ICON, StandardCharsets.UTF_8);

        assertTrue(svg.startsWith("<?xml"),
                ICON + " does not start with an XML declaration");
        assertTrue(svg.contains(SPDX),
                ICON + " does not declare " + SPDX
                        + "; the icon inherits the project licence and must say so");
        assertTrue(svg.contains(COPYRIGHT_HOLDER),
                ICON + " does not name the copyright holder " + COPYRIGHT_HOLDER);
    }

    private static List<Path> javaSources() throws IOException {
        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    /** The text of the first block comment in the file, or "" if there is none. */
    private static String firstCommentBlock(String text) {
        var start = text.indexOf("/*");
        if (start < 0) {
            return "";
        }
        var end = text.indexOf("*/", start + 2);
        return end < 0 ? text.substring(start) : text.substring(start, end + 2);
    }

    private static String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
