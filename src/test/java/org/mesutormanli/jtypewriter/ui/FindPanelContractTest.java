/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Pins the one rule the find card cannot state behaviourally without a
 * toolkit: a found match must arrive <em>selected</em>, not scrolled past.
 *
 * <p>The defect this test exists for: {@code reveal} called
 * {@code selectRange(start, end)} and then {@code positionCaret(end)} to make
 * the skin follow the caret. JavaFX implements {@code positionCaret(pos)} as
 * {@code selectRange(pos, pos)}, so the second call collapsed the selection to
 * an empty range - the match was scrolled into view but nothing was
 * highlighted, and pressing Replace would then have acted on a range the user
 * could not see.</p>
 *
 * <p>Headless by necessity, and by the project rule that no test may start a
 * JavaFX toolkit: the behavioural version would need a {@code TextArea}, so the
 * source scan is the honest alternative, the same way
 * {@code EditorUndoContractTest} pins the undo contract.</p>
 */
class FindPanelContractTest {

    private static final Path PANEL = Path.of("src", "main", "java", "org", "mesutormanli",
            "jtypewriter", "ui", "FindPanel.java");

    @Test
    void revealingAMatchSelectsItWithoutCollapsingTheSelection() throws IOException {
        var body = methodBody(codeOnly(read(PANEL)), "private void reveal(");

        assertTrue(body.contains("selectRange("),
                "reveal must select the match range so the found text is highlighted;"
                        + " body was: " + body);
        assertFalse(body.contains("positionCaret("),
                "positionCaret(pos) is selectRange(pos, pos) in JavaFX: calling it after"
                        + " selectRange collapses the match to an empty selection. Scrolling is"
                        + " SHOW_TEXT_RANGE's job; body was: " + body);
    }

    /** The source text between the braces of the first method whose signature matches. */
    private static String methodBody(String source, String signature) {
        var start = source.indexOf(signature);
        assertTrue(start >= 0, "signature not found: " + signature);
        var open = source.indexOf('{', start);
        var depth = 0;
        for (var i = open; i < source.length(); i++) {
            if (source.charAt(i) == '{') {
                depth++;
            } else if (source.charAt(i) == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new AssertionError("unbalanced braces after " + signature);
    }

    /**
     * The source with comments removed, so the Javadoc that explains why
     * {@code positionCaret} must not be added does not itself count as a call.
     */
    private static String codeOnly(String source) {
        var out = new StringBuilder(source.length());
        var i = 0;
        while (i < source.length()) {
            var c = source.charAt(i);
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                while (i < source.length() && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                var end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
                out.append(' '); // keep tokens either side of a comment apart
            } else if (c == '"' || c == '\'') {
                out.append(c);
                i++;
                while (i < source.length() && source.charAt(i) != c) {
                    if (source.charAt(i) == '\\' && i + 1 < source.length()) {
                        out.append(source.charAt(i++));
                    }
                    out.append(source.charAt(i++));
                }
                if (i < source.length()) {
                    out.append(source.charAt(i++)); // the closing quote
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
