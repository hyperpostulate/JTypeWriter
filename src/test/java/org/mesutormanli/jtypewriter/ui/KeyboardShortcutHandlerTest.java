package org.mesutormanli.jtypewriter.ui;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;
import org.mesutormanli.jtypewriter.ui.KeyboardShortcutHandler.ShortcutAction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for the shortcut router: shift-sensitive combos must be
 * decoded in one place so they cannot leak into the plain-Ctrl branch.
 * (Ctrl+Shift+Y used to run Redo instead of toggling YOLO mode, and Ctrl+Shift+Z
 * used to run Undo instead of Redo.)
 */
class KeyboardShortcutHandlerTest {

    private ShortcutAction resolve(KeyCode code, boolean shift) {
        return KeyboardShortcutHandler.resolve(code, shift);
    }

    @Test
    void ctrlShiftYMustToggleYoloNeverRedo() {
        assertEquals(ShortcutAction.TOGGLE_YOLO, resolve(KeyCode.Y, true));
        assertEquals(ShortcutAction.REDO, resolve(KeyCode.Y, false));
    }

    @Test
    void ctrlShiftZRedoesWhileCtrlZUndoes() {
        assertEquals(ShortcutAction.REDO, resolve(KeyCode.Z, true));
        assertEquals(ShortcutAction.UNDO, resolve(KeyCode.Z, false));
    }

    @Test
    void plainCtrlCIsNeverAShortcutButCtrlShiftCCyclesColor() {
        assertEquals(ShortcutAction.NONE, resolve(KeyCode.C, false)); // Copy must stay untouched
        assertEquals(ShortcutAction.CYCLE_TEXT_COLOR, resolve(KeyCode.C, true));
    }

    @Test
    void themeAndToolbarShareTWithShiftVariant() {
        assertEquals(ShortcutAction.CYCLE_THEME, resolve(KeyCode.T, true));
        assertEquals(ShortcutAction.TOGGLE_TOOLBAR, resolve(KeyCode.T, false));
    }

    @Test
    void saveAsAndSaveShareSWithShiftVariant() {
        assertEquals(ShortcutAction.SAVE_AS, resolve(KeyCode.S, true));
        assertEquals(ShortcutAction.SAVE, resolve(KeyCode.S, false));
    }

    @Test
    void openAndFontShortcutsResolve() {
        assertEquals(ShortcutAction.OPEN, resolve(KeyCode.O, false));
        assertEquals(ShortcutAction.FONT_RESET, resolve(KeyCode.DIGIT0, false));
        assertEquals(ShortcutAction.FONT_UP, resolve(KeyCode.EQUALS, false));
        assertEquals(ShortcutAction.FONT_UP, resolve(KeyCode.PLUS, false));
        assertEquals(ShortcutAction.FONT_DOWN, resolve(KeyCode.MINUS, false));
    }

    @Test
    void unrelatedKeysAreNotShortcuts() {
        assertEquals(ShortcutAction.NONE, resolve(KeyCode.A, true));
        assertEquals(ShortcutAction.NONE, resolve(KeyCode.N, false));
        assertEquals(ShortcutAction.NONE, resolve(KeyCode.F1, true));
    }
}