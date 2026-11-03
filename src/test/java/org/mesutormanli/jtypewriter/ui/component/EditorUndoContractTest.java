/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the undo contract at a document boundary, in the only way it can be
 * guarded without a display: by reading the sources.
 *
 * <p>The defect this test exists for: loading a document used to "help"
 * JavaFX by reflecting into {@code TextInputControl} and setting two private
 * fields - the undo cursor {@code undoChange} and its sentinel
 * {@code undoChangeHead} - to {@code null}. They are not a record but a
 * position in a linked list: {@code updateContent} calls
 * {@code undoChange.add(...)} on every edit and {@code resetUndoRedoState}
 * dereferences {@code undoChange.next}, neither of which null-checks. So the
 * <em>first keystroke</em> after opening any saved file threw a
 * {@code NullPointerException} inside JavaFX, and the next programmatic
 * {@code setText} threw another one while resetting what was already null.</p>
 *
 * <p>The framework already does that reset itself:
 * {@code TextInputControl.TextProperty.doSet} assumes a direct text set means
 * the control is being reused and calls {@code resetUndoRedoState()}, which
 * restores the cursor to the sentinel instead of nulling it. There is therefore
 * nothing for this project to implement, and the invariant to pin is the
 * absence of the workaround - not a particular call order.</p>
 *
 * <p>Headless by necessity: the behavioural version of this test would need a
 * toolkit, and no test in this project may start one. A source scan is the
 * honest alternative here, the same way {@code LicenseHeaderTest} and
 * {@code CiGatesTest} pin rules that no plugin enforces.</p>
 */
class EditorUndoContractTest {

    private static final Path SOURCES = Path.of("src", "main", "java");

    /**
     * The names of the private {@code TextInputControl} fields. Reaching them
     * at all is the defect: no production code may name, resolve or assign
     * them, whether through one helper class or inline.
     *
     * <p>Comments are stripped before the scan: the class Javadoc that explains
     * why the workaround is gone has to be able to name the fields it talks
     * about, and prose cannot break the editor. Only live code can.</p>
     */
    @Test
    void noProductionSourceNamesTheJavaFxUndoManagerFields() {
        var offenders = new ArrayList<String>();
        for (var file : javaSources()) {
            var source = codeOnly(read(file));
            for (var field : List.of("undoChange", "undoChangeHead", "createNewUndoRecord",
                    "undoable", "redoable")) {
                if (source.contains(field)) {
                    offenders.add(SOURCES.relativize(file) + " mentions " + field);
                }
            }
        }

        assertTrue(offenders.isEmpty(),
                "JavaFX's undo manager is a linked list whose cursor must never be nulled -"
                        + " the next keystroke dereferences it and the editor is dead until restart."
                        + " A document load resets it through setText(), which doSet() already"
                        + " routes through resetUndoRedoState(). Offenders: " + offenders);
    }

    /**
     * A document load must still go through {@code setText}, because that is
     * the call {@code doSet} performs the reset for, and because it bypasses
     * the YOLO-mode funnel so that opening a file is never blocked while
     * drafting. If someone replaces it with character-by-character
     * {@code replaceText} calls to "keep undo history", the history would then
     * span two documents - the opposite failure of the one above.
     */
    @Test
    void loadingADocumentStillReplacesTheTextInOneProgrammaticCall() throws IOException {
        var source = read(SOURCES.resolve(Path.of("org", "mesutormanli", "jtypewriter",
                "ui", "component", "EditorArea.java")));
        var body = methodBody(source, "public void setDocument(String text)");

        assertTrue(body.contains("setText(text)"),
                "setDocument must set the text in one call, so JavaFX resets the undo history"
                        + " and YOLO mode cannot block a load; body was: " + body);
        assertFalse(body.contains("replaceText("),
                "building the document character by character would run it through the YOLO"
                        + " funnel and keep an undo history that spans documents; body was: " + body);
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
     * The source with comments removed, so a name that appears only in prose
     * does not count as a use. String and character literals are kept as-is:
     * a field name inside a literal is a reflective lookup, which is exactly
     * what this test is looking for.
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

    private static List<Path> javaSources() {
        try (Stream<Path> files = Files.walk(SOURCES)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
