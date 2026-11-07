/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A card that floats over the editor in the window's top-right corner, hosted
 * in the scene instead of in a window of its own.
 *
 * <p><strong>Why the welcome and About views are cards now.</strong> Both used
 * to be borderless, modal, undecorated {@code Stage}s. That made them the only
 * two surfaces in the product with a second window, a second scene and a second
 * stylesheet list, and it made them block the editor: the user could not copy
 * a shortcut out of the card while reading it, and closing it did not return
 * focus to the text. The find panel had already solved the same problem with a
 * card in the corner (see {@link FindPanel}), so these follow it: one window,
 * one scene, one theme lookup, and the editor stays usable behind the card.</p>
 *
 * <p><strong>Why the card is not modal.</strong> Modality was there to stop
 * typing into a document the user could not see; a card that leaves the text
 * visible has nothing to protect. What it does keep from the old dialogs is the
 * keyboard: {@code Escape} and {@code Enter} dismiss it while focus is inside,
 * and focus starts on the close button, so either key works the moment the card
 * appears.</p>
 *
 * <p><strong>Why the content is rebuilt on every show.</strong> The bundles can
 * change language between two openings, and the shortcut table is generated
 * from the same {@link EditorAction} inventory the key filter uses. Rebuilding
 * from the live bundle on {@link #show()} is one line of policy that cannot go
 * stale, where a {@code relocalise()} per label would be a second list of every
 * string in the card.</p>
 *
 * <p><strong>Why the scroll pane can shrink.</strong> A {@link ScrollPane}'s
 * minimum size is normally its content's, which is exactly what a card bounded
 * by a short window must not inherit: the card would refuse to shrink and would
 * be clipped by the scene instead of scrolling. {@link #buildContent()} is
 * therefore hosted in a pane whose minimum is zero, and the window's height
 * caps the card (see {@code MainController}).</p>
 */
public abstract class OverlayCard extends VBox {

    /** Inset from the window edge; the same 12 px the find panel keeps. */
    public static final int EDGE_MARGIN = 12;

    private final ScrollPane scrollPane = new ScrollPane();
    private final Button closeButton = new Button();
    private final Tooltip closeTooltip = new Tooltip();
    private final Supplier<String> closeLabel;

    /**
     * The application mark, decoded once. The content is rebuilt on every show,
     * so decoding the 512 px asset there would re-read it per opening.
     */
    private final Image mark = AppIcons.dialogIcon();

    private OverlaySlot slot;

    /** Run after the card hides, so the caret can go back to the text. */
    private Runnable onClosed = () -> { };

    /**
     * @param styleClass a card-specific style class next to the shared
     *                   {@code welcome-root} surface (background, border,
     *                   radius and shadow all come from the theme tokens)
     * @param closeLabel the corner button's label in the current UI language
     */
    protected OverlayCard(String styleClass, Supplier<String> closeLabel) {
        this.closeLabel = Objects.requireNonNull(closeLabel, "closeLabel");
        getStyleClass().addAll("welcome-root", styleClass);
        setVisible(false);
        setManaged(false);

        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        // A ScrollPane's minimum is its content's; zero is what lets the card
        // shrink to the window and scroll instead of being clipped.
        scrollPane.setMinSize(0, 0);
        scrollPane.getStyleClass().add("dialog-scroll");

        closeButton.getStyleClass().add("overlay-card-close");
        closeButton.setTooltip(closeTooltip);
        closeButton.setOnAction(event -> hide());

        // The close button gets a row of its own above the scroll pane: at the
        // card's top-right corner it would sit on top of the vertical
        // scrollbar, swallowing its first clicks exactly when the content is
        // long enough to need it. It carries the localised "Close" label
        // rather than a glyph, the same way the find panel does - one word is
        // plainer than a multiplication sign, and it is the card's only
        // button now that the call-to-action at the bottom is gone.
        var header = new HBox(closeButton);
        header.setAlignment(Pos.CENTER_RIGHT);
        header.setPadding(new Insets(4, 6, 0, 6));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, scrollPane);
        // Escape and Enter are the two keys the old dialogs handled on their
        // own scene; as a filter on the card they dismiss it only while focus
        // is inside, so Enter typed in the editor still inserts a newline.
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (dismisses(event.getCode())) {
                hide();
                event.consume();
            }
        });
    }

    /**
     * Whether a key that reached the card dismisses it. Pure so the rule can be
     * tested without a toolkit.
     */
    static boolean dismisses(KeyCode code) {
        return code == KeyCode.ESCAPE || code == KeyCode.ENTER;
    }

    /** Builds the card's content column; called on every {@link #show()}. */
    protected abstract Node buildContent();

    /** Fixes the card's width; the height follows the content up to the window. */
    protected final void setCardWidth(double width) {
        setPrefWidth(width);
        setMaxWidth(width);
    }

    /**
     * The application mark drawn above the title, README-header style, or
     * {@code null} when the asset is missing - the cards skip it entirely then
     * rather than leaving a blank gap.
     */
    protected final ImageView markView(double size) {
        if (mark == null) {
            return null;
        }
        var view = new ImageView(mark);
        view.getStyleClass().add("dialog-icon");
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setCache(true);
        view.setFitWidth(size);
        return view;
    }

    /**
     * Makes the card the visible one in the corner and gives it focus.
     *
     * <p>Content is rebuilt here, not in the constructor, so the labels always
     * come from the bundle that is current at the moment the user asks.</p>
     */
    public final void show() {
        if (slot != null) {
            slot.aboutToShow(this);
        }
        rebuild();
        setVisible(true);
        setManaged(true);
        // One pulse later, not here: a card opened from a menu item is shown
        // while the popup is still closing, and a popup hands focus back to its
        // owner button as it hides - a synchronous request would be undone and
        // Escape/Enter would never reach the card.
        Platform.runLater(() -> {
            if (isVisible()) {
                closeButton.requestFocus();
            }
        });
    }

    /**
     * Rebuilds the content from the live bundle. Called on every show, and on a
     * language switch while the card is up, so a card can never be the one
     * surface still speaking the previous language.
     */
    public final void relocalise() {
        if (isVisible()) {
            rebuild();
        }
    }

    private void rebuild() {
        var label = closeLabel.get();
        closeButton.setText(label);
        closeTooltip.setText(label);
        closeButton.setAccessibleText(label);
        scrollPane.setContent(buildContent());
    }

    /**
     * Hides the card and hands the caret back. Hiding an already hidden card
     * does nothing: {@link OverlaySlot} hides every other member on each show,
     * and a card that was not on screen must not steal focus from the one that
     * is about to open.
     */
    public final void hide() {
        if (!isVisible()) {
            return;
        }
        setVisible(false);
        setManaged(false);
        onClosed.run();
    }

    /** Whether the card is currently in the corner. */
    public final boolean isShown() {
        return isVisible();
    }

    /** Runs after {@link #hide()}; the controller uses it to refocus the editor. */
    public final void setOnClosed(Runnable onClosed) {
        this.onClosed = Objects.requireNonNull(onClosed, "onClosed");
    }

    /** Joins the shared top-right slot, so opening this card closes the others. */
    public final void setOverlaySlot(OverlaySlot slot) {
        this.slot = slot;
        if (slot != null) {
            slot.register(this, this::hide);
        }
    }
}
