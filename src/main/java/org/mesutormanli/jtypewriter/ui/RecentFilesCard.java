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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The recent-files card: the documents the welcome card used to list, in a card
 * of their own in the window's top-right corner.
 *
 * <p><strong>Why it is separate from the welcome card.</strong> The two surfaces
 * answer different questions and have different lifetimes. The greeting is read
 * once and then switched off; the recent list is a working surface that stays
 * useful for as long as the user keeps writing. Keeping the list on the
 * greeting forced every user who did not want to be greeted to lose the list
 * with it, and it made the greeting grow a file manager in its middle. As its
 * own card the list can be reopened at any time from the File menu, whether or
 * not the greeting is still on.</p>
 *
 * <p><strong>Why it keeps the overlay design.</strong> It is one of the cards
 * that share the corner with the find panel and About (see {@link OverlaySlot}),
 * so opening it hides whatever was there, it is bounded by the window rather
 * than the screen, and closing it hands the caret back to the editor. That is
 * the same contract the other two already follow; a fourth corner widget would
 * have needed a fourth rule.</p>
 *
 * <p>The rows come from {@link AppSettings#getRecentFiles()}, which has already
 * applied the ordering, de-duplication, cap and existence rules - so this class
 * only decides how a row looks and what clicking it means. It opens a document
 * through the port the controller installs, never by touching the editor
 * itself: the unsaved-changes gate lives on that path and must not be bypassed.</p>
 */
public class RecentFilesCard extends OverlayCard {

    /** The card's preferred width; narrower than About, wider than the find panel. */
    private static final int CARD_WIDTH = 380;

    private static final double SPACING = 10;

    private final AppSettings settings;

    private Consumer<Path> openRecent = path -> { };

    public RecentFilesCard(Messages messages, AppSettings settings) {
        super("recent-card", messages);
        this.settings = Objects.requireNonNull(settings, "settings");
        setCardWidth(CARD_WIDTH);
    }

    /**
     * Opens a document chosen from the list. Installed by the controller, which
     * owns the unsaved-changes gate: a card may name the next document, but it
     * must not become a second way to replace the one in the editor.
     */
    public void setOnOpenRecent(Consumer<Path> openRecent) {
        this.openRecent = Objects.requireNonNull(openRecent, "openRecent");
    }

    @Override
    protected Node buildContent() {
        var title = new Label(messages.recentTitle());
        title.getStyleClass().add("welcome-title");
        title.setMaxWidth(contentWidth());

        var column = new VBox(SPACING, title);
        column.setAlignment(Pos.CENTER);
        column.setPadding(new Insets(12, 32, 32, 32));

        var recent = settings.getRecentFiles();
        if (recent.isEmpty()) {
            // An empty card has to say so: a card with a heading and nothing
            // else reads as a failed load rather than as "you have not opened
            // anything yet".
            var empty = new Label(messages.recentEmpty());
            empty.getStyleClass().add("recent-empty");
            empty.setWrapText(true);
            empty.setMaxWidth(contentWidth());
            column.getChildren().add(empty);
        } else {
            for (var document : recent) {
                column.getChildren().add(row(document));
            }
        }
        return column;
    }

    /**
     * One document. The visible text is the file name, because a list of
     * absolute paths is unreadable and would put the user's home directory in
     * every screenshot; the full path stays on the row as its tooltip and its
     * accessible name, so two files with the same name are still
     * distinguishable.
     */
    private Node row(Path document) {
        var button = new Button(rowLabel(document));
        button.getStyleClass().add("recent-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setTooltip(new Tooltip(document.toString()));
        button.setAccessibleText(document.toString());
        button.setOnAction(event -> {
            openRecent.accept(document);
            hide();
        });
        return button;
    }

    /**
     * The text a row shows: the last path element, or the whole path when there
     * is none (the root of a file system). Pure so the rule is testable without
     * a toolkit.
     */
    static String rowLabel(Path document) {
        var name = Objects.requireNonNull(document, "document").getFileName();
        return name == null ? document.toString() : name.toString();
    }
}
