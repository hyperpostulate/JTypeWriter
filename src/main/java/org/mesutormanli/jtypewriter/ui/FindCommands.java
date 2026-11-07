/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

/**
 * The find/replace capabilities an {@link EditorAction.Context} exposes to the
 * key filter and the command menu.
 *
 * <p>The key handler routes {@code Ctrl+F}, {@code F3}, {@code Shift+F3} and
 * {@code Ctrl+H} to this port, exactly as it routes {@code Ctrl+S} to
 * {@code FileService}. Keeping the surface this small means the panel owns all
 * of its state, while the shortcut inventory never needs a reference to a
 * JavaFX node it could accidentally reach into.</p>
 *
 * <p>Implemented by the find panel. Every call may be made from the FX thread;
 * work that must not run there (scanning a large document) is scheduled by the
 * implementation.</p>
 */
public interface FindCommands {

    /** Opens the panel and focuses the find field. */
    void openFind();

    /** Opens the panel and focuses the replace field. */
    void openReplace();

    /** Selects and reveals the next match, wrapping at the end. */
    void findNext();

    /** Selects and reveals the previous match, wrapping at the start. */
    void findPrevious();
}
