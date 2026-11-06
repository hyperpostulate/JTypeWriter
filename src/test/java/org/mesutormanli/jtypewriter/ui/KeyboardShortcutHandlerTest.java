/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the shortcut router: shift-sensitive combos must be
 * decoded in one place so they cannot leak into the plain-Ctrl branch.
 * (Ctrl+Shift+Y used to run Redo instead of toggling YOLO mode, and Ctrl+Shift+Z
 * used to run Undo instead of Redo.)
 *
 * <p>The decoding itself now lives in {@link EditorAction}, which also feeds
 * the shortcut card and the toolbar buttons; these tests pin the chord table
 * through the handler-facing entry point, and {@code EditorActionTest} pins
 * that each constant performs its own effect.</p>
 */
class KeyboardShortcutHandlerTest {

    @TempDir
    Path dir;

    private Optional<EditorAction> resolve(KeyCode code, boolean shift) {
        return EditorAction.resolve(code, shift);
    }

    @Test
    void ctrlShiftYMustToggleYoloNeverRedo() {
        assertEquals(Optional.of(EditorAction.TOGGLE_YOLO), resolve(KeyCode.Y, true));
        assertEquals(Optional.of(EditorAction.REDO), resolve(KeyCode.Y, false));
    }

    @Test
    void ctrlShiftZRedoesWhileCtrlZUndoes() {
        assertEquals(Optional.of(EditorAction.REDO), resolve(KeyCode.Z, true));
        assertEquals(Optional.of(EditorAction.UNDO), resolve(KeyCode.Z, false));
    }

    @Test
    void plainCtrlCIsNeverAShortcutButCtrlShiftCCyclesColor() {
        assertEquals(Optional.empty(), resolve(KeyCode.C, false)); // Copy must stay untouched
        assertEquals(Optional.of(EditorAction.CYCLE_TEXT_COLOR), resolve(KeyCode.C, true));
    }

    @Test
    void themeAndToolbarShareTWithShiftVariant() {
        assertEquals(Optional.of(EditorAction.CYCLE_THEME), resolve(KeyCode.T, true));
        assertEquals(Optional.of(EditorAction.TOGGLE_TOOLBAR), resolve(KeyCode.T, false));
    }

    @Test
    void saveAsAndSaveShareSWithShiftVariant() {
        assertEquals(Optional.of(EditorAction.SAVE_AS), resolve(KeyCode.S, true));
        assertEquals(Optional.of(EditorAction.SAVE), resolve(KeyCode.S, false));
    }

    @Test
    void openAndFontShortcutsResolve() {
        assertEquals(Optional.of(EditorAction.OPEN), resolve(KeyCode.O, false));
        assertEquals(Optional.of(EditorAction.FONT_RESET), resolve(KeyCode.DIGIT0, false));
        assertEquals(Optional.of(EditorAction.FONT_UP), resolve(KeyCode.EQUALS, false));
        assertEquals(Optional.of(EditorAction.FONT_UP), resolve(KeyCode.PLUS, false));
        assertEquals(Optional.of(EditorAction.FONT_DOWN), resolve(KeyCode.MINUS, false));
    }

    @Test
    void ctrlNCreatesANewDocumentWithOrWithoutShift() {
        assertEquals(Optional.of(EditorAction.NEW_DOCUMENT), resolve(KeyCode.N, false));
        assertEquals(Optional.of(EditorAction.NEW_DOCUMENT), resolve(KeyCode.N, true));
    }

    @Test
    void newDocumentShortcutDoesNotLeakIntoNeighbouringLetters() {
        assertEquals(Optional.of(EditorAction.OPEN), resolve(KeyCode.O, false));
        assertEquals(Optional.of(EditorAction.OPEN), resolve(KeyCode.O, true));
        assertEquals(Optional.empty(), resolve(KeyCode.M, false));
        assertEquals(Optional.empty(), resolve(KeyCode.M, true));
    }

    @Test
    void numpadKeysWorkAsEquivalentsOfTheMainKeyboard() {
        assertEquals(Optional.of(EditorAction.FONT_RESET), resolve(KeyCode.NUMPAD0, false));
        assertEquals(Optional.of(EditorAction.FONT_UP), resolve(KeyCode.ADD, false));
        assertEquals(Optional.of(EditorAction.FONT_DOWN), resolve(KeyCode.SUBTRACT, false));
    }

    /** F11 is modifier-free, and Shift is not part of its identity either. */
    @Test
    void f11TogglesFullscreenWithoutModifiers() {
        assertEquals(Optional.of(EditorAction.TOGGLE_FULLSCREEN), resolve(KeyCode.F11, false));
        assertEquals(Optional.of(EditorAction.TOGGLE_FULLSCREEN), resolve(KeyCode.F11, true));
    }

    /** Escape belongs to the stage's fullscreen exit combination, not to the key filter. */
    @Test
    void escapeIsLeftToTheStage() {
        assertEquals(Optional.empty(), resolve(KeyCode.ESCAPE, false));
    }

    @Test
    void unrelatedKeysAreNotShortcuts() {
        assertEquals(Optional.empty(), resolve(KeyCode.A, true));
        assertEquals(Optional.empty(), resolve(KeyCode.F1, true));
    }

    /**
     * The structural half of U13: one chord cannot mean two commands. The
     * inventory is a list, not a switch, so a duplicated accelerator would
     * silently make one of the two unreachable - and the card would still list
     * both. Only {@code EXIT_FULLSCREEN} is exempt because it is not resolved
     * here at all.
     */
    @Test
    void noTwoCommandsClaimTheSameChord() {
        Map<String, EditorAction> owners = new LinkedHashMap<>();
        for (var action : EditorAction.values()) {
            if (action.handledByPlatform()) {
                continue;
            }
            for (var accelerator : action.accelerators()) {
                var key = accelerator.chord() + "/shift=" + accelerator.shift();
                var previous = owners.put(key, action);
                assertNull(previous,
                        key + " is claimed by both " + previous + " and " + action);
            }
        }
        assertTrue(owners.size() >= 15, "the inventory lost chords: " + owners.keySet());
    }

    /** Every non-platform constant must be reachable through its own first chord. */
    @Test
    void everyCommandIsReachableByItsPrimaryChord() {
        for (var action : EditorAction.values()) {
            if (action.handledByPlatform()) {
                continue;
            }
            var accelerator = action.accelerators().get(0);
            assertEquals(Optional.of(action),
                    resolve(accelerator.codes().get(0), Boolean.TRUE.equals(accelerator.shift())),
                    action + " cannot be reached by " + accelerator.chord());
        }
    }

    /** The card lists the same chords in the same order as the inventory. */
    @Test
    void theCardIsGeneratedFromTheInventory() {
        var cardChords = ShortcutRows.fromActions(messages()).stream()
                .map(ShortcutRows.Row::keys).toList();
        var inventoryChords = Arrays.stream(EditorAction.values())
                .flatMap(action -> action.accelerators().stream())
                .map(EditorAction.Accelerator::chord)
                .toList();

        assertEquals(inventoryChords, cardChords);
        assertEquals(List.of("Ctrl+T", "Ctrl+Shift+T", "Ctrl+Shift+Y", "Ctrl+Shift+C",
                        "Ctrl++", "Ctrl+-", "Ctrl+0", "Ctrl+Z", "Ctrl+Shift+Z", "Ctrl+Y",
                        "Ctrl+N", "Ctrl+O", "Ctrl+S", "Ctrl+Shift+S", "F11", "Escape"),
                cardChords, "the published card order is part of the contract");
    }

    private Messages messages() {
        var settings = new AppSettings(dir.resolve("settings.properties"));
        var localeManager = new LocaleManager(settings);
        localeManager.setLanguage(Language.EN);
        return new Messages(localeManager);
    }
}
