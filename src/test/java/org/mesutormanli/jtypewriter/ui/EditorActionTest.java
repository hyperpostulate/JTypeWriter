/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The inventory contract: every constant performs its own effect, and no other
 * source file redeclares a chord.
 *
 * <p>The card used to be a padded bundle blob and the toolbar hard-coded its
 * own {@code Ctrl+O}/{@code Ctrl+S} strings, so the same chord was written in
 * three places. This test is the mechanical half of "no action is defined
 * twice": the effect of each constant is observed through a recording
 * {@link EditorAction.Context} (no toolkit), and the source scan fails if a
 * chord literal reappears outside {@link EditorAction}.</p>
 */
class EditorActionTest {

    @TempDir
    Path dir;

    private static final Path UI_DIR =
            Path.of("src", "main", "java", "org", "mesutormanli", "jtypewriter", "ui");

    /** Every way two actions could differ; a constant must use exactly one. */
    private enum Effect {
        TOGGLE_TOOLBAR, CYCLE_THEME, UNDO, REDO, TOGGLE_YOLO, CYCLE_TEXT_COLOR,
        OPEN_DOCUMENT, NEW_DOCUMENT, SAVE, SAVE_AS, INCREASE_FONT, DECREASE_FONT,
        RESET_FONT, TOGGLE_FULLSCREEN, EXIT_FULLSCREEN
    }

    @Test
    void everyConstantPerformsItsOwnEffect() {
        for (var action : EditorAction.values()) {
            var recorder = new Recorder();

            action.run(recorder);

            assertEquals(List.of(expected(action)), recorder.effects,
                    action + " does not perform exactly one distinct effect");
        }
    }

    /** Coverage: the mapping is total in both directions - no chord without a command. */
    @Test
    void theEffectMappingCoversEveryConstantExactlyOnce() {
        Set<Effect> seen = new LinkedHashSet<>();
        for (var action : EditorAction.values()) {
            assertTrue(seen.add(expected(action)), action + " duplicates another command's effect");
        }
        assertEquals(Set.of(Effect.values()).size(), seen.size());
    }

    @Test
    void everyConstantCarriesALabelAndAtLeastOneChord() {
        var messages = englishMessages();
        for (var action : EditorAction.values()) {
            assertFalse(action.accelerators().isEmpty(), action + " has no chord");
            assertFalse(action.label(messages).isBlank(), action + " has no label");
            assertFalse(action.primaryChord().isBlank(), action + " has no chord text");
        }
    }

    /** The platform performs exactly one command; everything else is ours. */
    @Test
    void onlyExitFullscreenIsHandledByThePlatform() {
        for (var action : EditorAction.values()) {
            assertEquals(action == EditorAction.EXIT_FULLSCREEN, action.handledByPlatform(),
                    action.toString());
        }
    }

    /**
     * The grep half of the card's definition of done: a chord is written in
     * one file only. Comments are stripped first, so prose that mentions a
     * chord does not count.
     */
    @Test
    void noOtherSourceFileDeclaresAChord() {
        var offenders = new ArrayList<String>();
        var chordLiteral = Pattern.compile("\"(Ctrl|F11|Escape)");

        try (var files = Files.walk(UI_DIR)) {
            for (var file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("EditorAction.java")) {
                    continue;
                }
                var code = codeOnly(Files.readString(file, StandardCharsets.UTF_8));
                if (chordLiteral.matcher(code).find()) {
                    offenders.add(file.toString());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        assertTrue(offenders.isEmpty(),
                "a chord is still declared outside EditorAction: " + offenders);
    }

    /**
     * The toolbar must delegate, not reimplement: a button that calls
     * {@code fileService} or {@code requestUndo} directly would be a second
     * definition of a command the shortcuts already own.
     */
    @Test
    void theToolbarButtonsRunTheSharedActions() throws IOException {
        var source = codeOnly(Files.readString(UI_DIR.resolve("component").resolve("ToolbarView.java"),
                StandardCharsets.UTF_8));

        for (var action : List.of("OPEN", "SAVE", "UNDO", "REDO")) {
            assertTrue(source.contains("actionRunner.accept(EditorAction." + action + ")"),
                    "the toolbar does not route " + action + " through the shared action");
        }
        for (var forbidden : List.of("openFileAsync(", "saveFileAsync(", "requestUndo(", "requestRedo(")) {
            assertFalse(source.contains(forbidden),
                    "the toolbar still implements " + forbidden + " itself");
        }
    }

    private Messages englishMessages() {
        var localeManager = new LocaleManager(new AppSettings(dir.resolve("settings.properties")));
        localeManager.setLanguage(Language.EN);
        return new Messages(localeManager);
    }

    private static Effect expected(EditorAction action) {
        return switch (action) {
            case TOGGLE_TOOLBAR -> Effect.TOGGLE_TOOLBAR;
            case CYCLE_THEME -> Effect.CYCLE_THEME;
            case TOGGLE_YOLO -> Effect.TOGGLE_YOLO;
            case CYCLE_TEXT_COLOR -> Effect.CYCLE_TEXT_COLOR;
            case FONT_UP -> Effect.INCREASE_FONT;
            case FONT_DOWN -> Effect.DECREASE_FONT;
            case FONT_RESET -> Effect.RESET_FONT;
            case UNDO -> Effect.UNDO;
            case REDO -> Effect.REDO;
            case NEW_DOCUMENT -> Effect.NEW_DOCUMENT;
            case OPEN -> Effect.OPEN_DOCUMENT;
            case SAVE -> Effect.SAVE;
            case SAVE_AS -> Effect.SAVE_AS;
            case TOGGLE_FULLSCREEN -> Effect.TOGGLE_FULLSCREEN;
            case EXIT_FULLSCREEN -> Effect.EXIT_FULLSCREEN;
        };
    }

    /** Records which single capability an action exercised. */
    private static final class Recorder implements EditorAction.Context {

        private final List<Effect> effects = new ArrayList<>();

        private void record(Effect effect) {
            effects.add(effect);
        }

        @Override
        public void toggleToolbar() {
            record(Effect.TOGGLE_TOOLBAR);
        }

        @Override
        public void cycleTheme() {
            record(Effect.CYCLE_THEME);
        }

        @Override
        public void undo() {
            record(Effect.UNDO);
        }

        @Override
        public void redo() {
            record(Effect.REDO);
        }

        @Override
        public void toggleYolo() {
            record(Effect.TOGGLE_YOLO);
        }

        @Override
        public void cycleTextColor() {
            record(Effect.CYCLE_TEXT_COLOR);
        }

        @Override
        public void openDocument() {
            record(Effect.OPEN_DOCUMENT);
        }

        @Override
        public void newDocument() {
            record(Effect.NEW_DOCUMENT);
        }

        @Override
        public void save() {
            record(Effect.SAVE);
        }

        @Override
        public void saveAs() {
            record(Effect.SAVE_AS);
        }

        @Override
        public void increaseFontSize() {
            record(Effect.INCREASE_FONT);
        }

        @Override
        public void decreaseFontSize() {
            record(Effect.DECREASE_FONT);
        }

        @Override
        public void resetFontSize() {
            record(Effect.RESET_FONT);
        }

        @Override
        public void toggleFullScreen() {
            record(Effect.TOGGLE_FULLSCREEN);
        }

        @Override
        public void exitFullScreen() {
            record(Effect.EXIT_FULLSCREEN);
        }
    }

    /** Strips comments so only code is searched; string literals stay. */
    private static String codeOnly(String source) {
        var out = new StringBuilder(source.length());
        var inLine = false;
        var inBlock = false;
        var inString = false;
        for (var i = 0; i < source.length(); i++) {
            var c = source.charAt(i);
            var next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (inLine) {
                if (c == '\n') {
                    inLine = false;
                    out.append(c);
                }
            } else if (inBlock) {
                if (c == '*' && next == '/') {
                    inBlock = false;
                    i++;
                }
            } else if (inString) {
                out.append(c);
                if (c == '\\' && next != '\0') {
                    out.append(next);
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '/' && next == '/') {
                inLine = true;
                i++;
            } else if (c == '/' && next == '*') {
                inBlock = true;
                i++;
            } else {
                if (c == '"') {
                    inString = true;
                }
                out.append(c);
            }
        }
        return out.toString();
    }
}
