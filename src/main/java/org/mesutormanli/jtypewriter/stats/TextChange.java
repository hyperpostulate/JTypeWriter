/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.stats;

/**
 * One text replacement as the editor saw it: {@code removedLength} characters
 * starting at {@code offset} were replaced by {@code insertedText}.
 *
 * <p>This is the delta {@link SessionStats} needs to update its counters in
 * O(edit size) instead of rescanning the document. It is a plain value with no
 * JavaFX type, so the statistics stay testable without a toolkit; the editor
 * builds it at its single enforcement point and hands it over through the text
 * listener.</p>
 *
 * <p>{@code insertedText} is normalised to {@code ""}: a {@code null}
 * replacement never describes a real edit, but a value object that can hold
 * one would push a null check into every consumer.</p>
 */
public record TextChange(int offset, int removedLength, String insertedText) {

    public TextChange {
        insertedText = insertedText == null ? "" : insertedText;
    }

    /** Length of the document after this change, given {@code previousLength}. */
    public int resultingLength(int previousLength) {
        return previousLength - removedLength + insertedText.length();
    }
}
