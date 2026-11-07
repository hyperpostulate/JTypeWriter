/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The welcome card: the application mark, the tagline and the shortcut table,
 * floating over the editor in the window's top-right corner. Its only button is
 * the header's {@code Close}; the card is something to read, not a step to
 * confirm, so the old "Start Writing" call-to-action is gone.
 *
 * <p>It was a modal, borderless window until the card form arrived (see
 * {@link OverlayCard}). What it shows is now exactly the shared column
 * ({@link OverlayCard#cardColumn(String, double, Node...)}) with the greeting's
 * own extras appended: the recent documents and the "don't show again"
 * opt-out. The About card is that column plus a version line and the legal
 * block - so neither class owns the shared part.</p>
 *
 * <p><strong>Why it still exists as its own view.</strong> It is the startup
 * greeting: a card that appears on every launch should not read like a licence
 * notice, and the About card is a reference a user asks for once. The recent
 * list and the opt-out belong on a greeting and not on About.</p>
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

    private Consumer<Path> openRecent = path -> { };

    public WelcomeCard(Messages messages, AppSettings settings) {
        super("welcome-card", messages);
        this.settings = Objects.requireNonNull(settings, "settings");
        setCardWidth(CARD_WIDTH);
    }

    /**
     * Opens a document chosen from the recent list. Installed by the
     * controller, which owns the unsaved-changes gate and the editor: the card
     * may name a document, but it must not become a second way to replace the
     * one in the editor.
     */
    public void setOnOpenRecent(Consumer<Path> openRecent) {
        this.openRecent = Objects.requireNonNull(openRecent, "openRecent");
    }

    @Override
    protected Node buildContent() {
        var column = cardColumn(messages.welcomeTitle(), SPACING);
        var recent = settings.getRecentFiles();
        if (!recent.isEmpty()) {
            column.getChildren().add(recentSection(recent));
        }
        column.getChildren().add(optOutCheckBox());
        return column;
    }

    /**
     * The recent-document rows. Every path in the list has already been checked
     * for existence by {@link AppSettings#getRecentFiles()}, so a row here is a
     * document that can actually be opened - the card never offers a link that
     * fails when clicked.
     */
    private Node recentSection(List<Path> recent) {
        var title = new Label(messages.welcomeRecentTitle());
        title.getStyleClass().add("welcome-recent-title");
        title.setMaxWidth(contentWidth());

        var rows = new VBox(6);
        rows.setFillWidth(true);
        for (var document : recent) {
            rows.getChildren().add(recentRow(document));
        }

        var section = new VBox(8, title, rows);
        section.setMaxWidth(contentWidth());
        return section;
    }

    private Node recentRow(Path document) {
        var name = document.getFileName() == null ? document.toString() : document.getFileName().toString();
        var row = new Button(name);
        row.getStyleClass().add("welcome-recent-item");
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setTooltip(new Tooltip(document.toString()));
        row.setAccessibleText(document.toString());
        row.setOnAction(event -> {
            openRecent.accept(document);
            hide();
        });
        return row;
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
