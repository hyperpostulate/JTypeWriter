/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.util.Objects;

/**
 * The welcome card: the application mark, the tagline and the shortcut table,
 * floating over the editor in the window's top-right corner. Its only button is
 * the header's {@code Close}; the card is something to read, not a step to
 * confirm, so the old "Start Writing" call-to-action is gone.
 *
 * <p>It was a modal, borderless window until the card form arrived (see
 * {@link OverlayCard}). What it shows is now exactly the shared column
 * ({@link OverlayCard#cardColumn(String, double, Node...)}) plus the greeting's
 * own opt-out, while the About card is that column plus a version line and the
 * legal block - so neither class owns the shared part.</p>
 *
 * <p><strong>Why it still exists as its own view.</strong> It is the startup
 * greeting: a card that appears on every launch should not read like a licence
 * notice, and the About card is a reference a user asks for once. The opt-out
 * belongs on a greeting; the recent-document list does not, and lives in
 * {@link RecentFilesCard} instead - a user who switches the greeting off still
 * wants their files.</p>
 *
 * <p><strong>Why the opt-out is a checkbox, not a button.</strong> A button
 * would have to guess whether "Close" and "never again" are the same gesture.
 * The checkbox states the preference itself - "don't show this again" - and
 * writes it immediately, so closing the card afterwards is an ordinary Close
 * and the user never has to decide what the card will do next time.</p>
 */
public class WelcomeCard extends OverlayCard {

    /** The card's preferred width, kept from the old window. */
    private static final int CARD_WIDTH = 460;

    /** Vertical gap of this card's column; the About card's is tighter. */
    private static final double SPACING = 14;

    private final AppSettings settings;

    public WelcomeCard(Messages messages, AppSettings settings) {
        super("welcome-card", messages);
        this.settings = Objects.requireNonNull(settings, "settings");
        setCardWidth(CARD_WIDTH);
    }

    @Override
    protected Node buildContent() {
        var column = cardColumn(messages.welcomeTitle(), SPACING);
        column.getChildren().add(optOutCheckBox());
        return column;
    }

    /**
     * The opt-out. It reflects the stored preference rather than a local flag,
     * so the File menu's "Show welcome" and this checkbox cannot disagree: the
     * menu clears the preference and the next rebuild checks this box again.
     */
    private Node optOutCheckBox() {
        var optOut = new CheckBox(messages.welcomeOptOut());
        optOut.getStyleClass().add("welcome-option");
        optOut.setSelected(!settings.isWelcomeCardEnabled());
        optOut.selectedProperty().addListener((observable, wasSelected, isSelected) ->
                settings.setWelcomeCardEnabled(!isSelected));
        return optOut;
    }
}
