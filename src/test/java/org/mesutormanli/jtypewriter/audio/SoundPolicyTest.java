/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.audio;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W4-04: which keystrokes click.
 *
 * <p>The sound is the product's name, so it has to sound like a typewriter and
 * not like a keyboard: a typewriter clacks when it puts ink on paper. Arrows,
 * {@code Home}, {@code F11} and every shortcut move the caret or the window and
 * write nothing, so they stay silent - and so does any chord that the platform
 * reports as a shortcut rather than as text.</p>
 *
 * <p>The rule is deliberately expressed over the two events JavaFX actually
 * delivers, because that is where it is applied: a {@code KEY_TYPED} event
 * exists only when the keystroke produced a character, which already excludes
 * arrows, function keys, {@code Escape} and every {@code Ctrl}/{@code Alt}
 * chord; the two editing keys that delete text have no character of their own
 * and are named explicitly. No JavaFX node and no toolkit is needed to pin
 * that.</p>
 */
class SoundPolicyTest {

    /** Every key that writes a character into the document. */
    @Test
    void typingACharacterClicks() {
        for (var character : new String[]{"a", "Z", "7", " ", "\n", "\t", "ü", "\u00df", "."}) {
            assertTrue(SoundPolicy.playsOnTyped(character),
                    "typing \"" + character + "\" must click");
        }
    }

    /**
     * JavaFX delivers {@code KEY_TYPED} with {@link KeyEvent#CHAR_UNDEFINED}
     * for keys that have no character; those must not click even though an
     * event arrived.
     */
    @Test
    void anUndefinedCharacterIsSilent() {
        assertFalse(SoundPolicy.playsOnTyped(javafx.scene.input.KeyEvent.CHAR_UNDEFINED));
        assertFalse(SoundPolicy.playsOnTyped(null));
        assertFalse(SoundPolicy.playsOnTyped(""));
    }

    /**
     * Backspace and Delete rewrite the text and produce no {@code KEY_TYPED}
     * event, so they are the only two keys the press path may click for. A
     * typewriter's correction (a strike-through or a rub-out) is part of the
     * sound being imitated.
     */
    @Test
    void theDeletingKeysClickButNothingElseOnPressDoes() {
        assertTrue(SoundPolicy.playsOnPressed(KeyCode.BACK_SPACE));
        assertTrue(SoundPolicy.playsOnPressed(KeyCode.DELETE));

        for (var silent : new KeyCode[]{
                KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN,
                KeyCode.HOME, KeyCode.END, KeyCode.PAGE_UP, KeyCode.PAGE_DOWN,
                KeyCode.ESCAPE, KeyCode.F1, KeyCode.F11, KeyCode.CONTROL,
                KeyCode.ALT, KeyCode.SHIFT, KeyCode.META, KeyCode.CAPS,
                KeyCode.ENTER, KeyCode.TAB, KeyCode.SPACE, KeyCode.A}) {
            assertFalse(SoundPolicy.playsOnPressed(silent),
                    silent + " must not click on key press");
        }
    }

    /**
     * A shortcut must be silent even when its key would otherwise type: the
     * chord is a command, not ink. Checked separately from the typed rule
     * because {@code Ctrl+S} reaches the press handler with S as its code.
     */
    @Test
    void aShortcutChordIsSilentWhateverItsKeyIs() {
        for (var code : new KeyCode[]{KeyCode.S, KeyCode.Z, KeyCode.C, KeyCode.BACK_SPACE}) {
            assertFalse(SoundPolicy.playsFor(code, true),
                    code + " is a shortcut here, not a keystroke");
        }
        assertTrue(SoundPolicy.playsFor(KeyCode.BACK_SPACE, false));
    }
}
