/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.mesutormanli.jtypewriter.locale.Messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the {@link EditorAction} inventory into key/description rows for the
 * welcome and About shortcut cards.
 *
 * <p>The rows used to be a hand-padded {@code welcomeShortcuts} bundle blob
 * that had to be kept in step with the key handler by hand, and the two did
 * drift apart. The chords are no longer written anywhere but
 * {@link EditorAction}; only the descriptions are translated, one bundle key
 * per action.</p>
 *
 * <p>Deliberately free of JavaFX nodes, like {@link DialogSizing}: the unit
 * tests build every row of every shipped language without starting a toolkit.
 * The JavaFX side of the card lives in {@link ShortcutTable}.</p>
 */
public final class ShortcutRows {

    /** One line of the shortcut list: the chord(s) and what they do. */
    public record Row(String keys, String description) {
    }

    private ShortcutRows() {
    }

    /**
     * The card, in {@link EditorAction} declaration order. An action with two
     * accelerators ({@code Redo}) yields two rows, exactly as the card always
     * listed it; the label is read in the current UI language on every call,
     * so a language switch re-renders the card.
     */
    public static List<Row> fromActions(Messages messages) {
        var rows = new ArrayList<Row>();
        for (var action : EditorAction.values()) {
            var description = action.label(messages);
            for (var accelerator : action.accelerators()) {
                rows.add(new Row(accelerator.chord(), description));
            }
        }
        return List.copyOf(rows);
    }
}
