/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.mesutormanli.jtypewriter.locale.Messages;

/**
 * The welcome card: the application mark, the tagline, the shortcut table and
 * the start button, floating over the editor in the window's top-right corner.
 *
 * <p>It was a modal, borderless window until the card form arrived (see
 * {@link OverlayCard}); everything visible is unchanged, including the
 * two-column shortcut grid from {@link ShortcutTable}, which is why the card still
 * sizes itself from the same content width. What changed is what the card no
 * longer needs: a second stage, a second scene, a scene fill sampled from the
 * theme, and the screen-size clamping those implied - the window is now the
 * card's only bound, and the card scrolls inside it.</p>
 */
public class WelcomeCard extends OverlayCard {

    /** Width of the drawn application mark; square artwork, so height matches. */
    private static final int ICON_SIZE = 80;

    /** The card's preferred width, kept from the old window. */
    private static final int CARD_WIDTH = 460;

    /** Content width of the card (card width minus the horizontal padding). */
    private static final int CONTENT_WIDTH = CARD_WIDTH - 2 * 32;

    private final Messages messages;

    public WelcomeCard(Messages messages) {
        super("welcome-card", messages::overlayClose);
        this.messages = messages;
        setCardWidth(CARD_WIDTH);
    }

    @Override
    protected Node buildContent() {
        var titleLabel = new Label(messages.welcomeTitle());
        titleLabel.getStyleClass().add("welcome-title");

        var taglineLabel = new Label(messages.appTagline());
        taglineLabel.getStyleClass().add("welcome-tagline");
        taglineLabel.setWrapText(true);
        taglineLabel.setMaxWidth(CONTENT_WIDTH);
        taglineLabel.setTextAlignment(TextAlignment.CENTER);
        taglineLabel.setAlignment(Pos.CENTER);

        // A real two-column grid (keys left, descriptions right-aligned on the
        // card edge) replaces the old single Label whose columns were faked
        // with space runs and rendered ragged. ShortcutTable sizes the card to
        // the full content box and keeps the wrapping the long German/French/
        // Italian/Spanish descriptions need.
        var shortcuts = ShortcutTable.build(
                ShortcutRows.fromActions(messages), CONTENT_WIDTH);

        var startButton = new Button(messages.welcomeButton());
        startButton.getStyleClass().add("welcome-button");
        startButton.setOnAction(event -> hide());

        var content = new VBox(14, titleLabel, taglineLabel, shortcuts, startButton);
        content.setAlignment(Pos.CENTER);
        // Only a small top inset: the card's close row already sits above
        // the scroll pane, so 36 px here would double the top gap.
        content.setPadding(new Insets(12, 32, 32, 32));

        var mark = markView(ICON_SIZE);
        if (mark != null) {
            content.getChildren().add(0, mark);
        }
        setInitialFocus(startButton);
        return content;
    }
}
