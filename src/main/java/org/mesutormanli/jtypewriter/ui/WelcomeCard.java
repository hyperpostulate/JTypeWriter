/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.Node;
import org.mesutormanli.jtypewriter.locale.Messages;

/**
 * The welcome card: the application mark, the tagline and the shortcut table,
 * floating over the editor in the window's top-right corner. Its only button is
 * the header's {@code Close}; the card is something to read, not a step to
 * confirm, so the old "Start Writing" call-to-action is gone.
 *
 * <p>It was a modal, borderless window until the card form arrived (see
 * {@link OverlayCard}). What it shows is now exactly the shared column
 * ({@link OverlayCard#cardColumn(String, double, Node...)}) with nothing added,
 * while the About card is that column plus a version line and the legal block -
 * so this class is only the card's width and rhythm.
 *
 * <p><strong>Why it still exists as its own view.</strong> It is the startup
 * greeting: a card that appears on every launch should not read like a licence
 * notice, and the About card is a reference a user asks for once. W4-02 gives
 * this surface recent files and a "don't show again" opt-out of its own, which
 * belong on a greeting and not on About.</p>
 */
public class WelcomeCard extends OverlayCard {

    /** The card's preferred width, kept from the old window. */
    private static final int CARD_WIDTH = 460;

    /** Vertical gap of this card's column; the About card's is tighter. */
    private static final double SPACING = 14;

    public WelcomeCard(Messages messages) {
        super("welcome-card", messages);
        setCardWidth(CARD_WIDTH);
    }

    @Override
    protected Node buildContent() {
        return cardColumn(messages.welcomeTitle(), SPACING);
    }
}
