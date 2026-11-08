/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.audio;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Which keystrokes click, expressed as pure predicates.
 *
 * <p>The sound is the product's name, so it has to sound like a typewriter and
 * not like a keyboard: a typewriter clacks when it puts ink on paper. An
 * arrow, {@code Home}, {@code F11} or a shortcut moves the caret or the window
 * and writes nothing, so it stays silent. The old rule only excluded modifier
 * keys, which made every arrow and function key click.</p>
 *
 * <p><strong>Two events, one decision.</strong> JavaFX delivers a
 * {@code KEY_TYPED} event only when the keystroke produced a character, which
 * already excludes arrows, function keys, {@code Escape} and every
 * {@code Ctrl}/{@code Alt} chord. The two editing keys that rewrite text have
 * no character of their own, so they are named explicitly on the press path.
 * A shortcut is checked separately because {@code Ctrl+S} reaches the press
 * handler with {@code S} as its code - the chord is a command, not ink.</p>
 *
 * <p>Deliberately free of JavaFX nodes: the constants are enough, and this
 * class can be pinned headlessly like {@link org.mesutormanli.jtypewriter.ui.component.ToolbarFit}.</p>
 */
public final class SoundPolicy {

    private SoundPolicy() {
    }

    /**
     * Whether a typed character clicks. {@code null}, the empty string and
     * {@link KeyEvent#CHAR_UNDEFINED} are not characters, even though an event
     * arrived for them.
     */
    public static boolean playsOnTyped(String character) {
        return character != null && !character.isEmpty()
                && !KeyEvent.CHAR_UNDEFINED.equals(character);
    }

    /**
     * Whether pressing a key with no typed character clicks. Only the two keys
     * that delete text qualify: a typewriter's correction stroke is part of
     * the sound being imitated, while caret movement is not.
     */
    public static boolean playsOnPressed(KeyCode code) {
        return code == KeyCode.BACK_SPACE || code == KeyCode.DELETE;
    }

    /**
     * The full press-path decision: a deleting key clicks unless the keystroke
     * is a shortcut. A shortcut that happens to use {@code Backspace} is a
     * command and stays silent.
     */
    public static boolean playsFor(KeyCode code, boolean isShortcut) {
        return !isShortcut && playsOnPressed(code);
    }
}
