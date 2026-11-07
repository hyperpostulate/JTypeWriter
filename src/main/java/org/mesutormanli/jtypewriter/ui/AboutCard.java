/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.AppVersion;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The About card: title, running version, tagline, the shortcut table, the
 * licence and warranty lines and the author's e-mail link, floating over the
 * editor in the window's top-right corner.
 *
 * <p>Like the welcome card it used to be a modal, borderless window (see
 * {@link OverlayCard}). The reason it matters here is the e-mail link: opening
 * the mail client used to be reachable only while the window was up, and the
 * window took the whole screen. The card leaves the document visible, so a
 * user can copy a shortcut or a version string out of it while it is open.</p>
 */
public class AboutCard extends OverlayCard {

    /** Width of the drawn application mark; square artwork, so height matches. */
    private static final int ICON_SIZE = 80;

    /** The card's preferred width, kept from the old window. */
    private static final int CARD_WIDTH = 540;

    /** Content width of the card (card width minus the horizontal padding). */
    private static final int CONTENT_WIDTH = CARD_WIDTH - 2 * 32;

    private static final Logger LOG = AppLogging.logger(AboutCard.class);

    private final Messages messages;

    public AboutCard(Messages messages) {
        super("about-card", messages::overlayClose);
        this.messages = messages;
        setCardWidth(CARD_WIDTH);
    }

    @Override
    protected Node buildContent() {
        var titleLabel = new Label(messages.appName());
        titleLabel.getStyleClass().add("welcome-title");

        // Field reports need a version to name, so the running version
        // (from the Maven metadata, never hard-coded) sits right under the title.
        var versionLabel = new Label(messages.aboutVersion(AppVersion.get()));
        versionLabel.getStyleClass().add("about-version");

        var taglineLabel = new Label(messages.appTagline());
        taglineLabel.getStyleClass().add("welcome-tagline");
        taglineLabel.setWrapText(true);
        taglineLabel.setMaxWidth(CONTENT_WIDTH);
        taglineLabel.setTextAlignment(TextAlignment.CENTER);
        taglineLabel.setAlignment(Pos.CENTER);

        // Same two-column grid as the welcome card (see ShortcutTable): real
        // columns instead of the space-padded single Label, with the wrapping
        // the long German/French/Italian/Spanish descriptions need handled
        // inside the grid.
        var shortcuts = ShortcutTable.build(
                ShortcutRows.fromActions(messages), CONTENT_WIDTH);

        var emailLink = new Hyperlink("mesutormanli@gmail.com");
        emailLink.getStyleClass().add("about-email");
        emailLink.setOnAction(e -> openMailClient());

        // Licensing and the warranty disclaimer: the translated sentences are
        // longer than the card, so they wrap and centre like the tagline.
        var copyrightLabel = legalLabel(messages.aboutCopyright());
        var licenseLabel = legalLabel(messages.aboutLicense());
        var warrantyLabel = legalLabel(messages.aboutWarranty());

        var closeButton = new Button(messages.aboutClose());
        closeButton.getStyleClass().add("welcome-button");
        closeButton.setOnAction(event -> hide());

        var content = new VBox(12, titleLabel, versionLabel, taglineLabel, shortcuts,
                copyrightLabel, emailLink, licenseLabel, warrantyLabel, closeButton);
        content.setAlignment(Pos.CENTER);
        // Only a small top inset: the card's close row already sits above
        // the scroll pane, so 36 px here would double the top gap.
        content.setPadding(new Insets(12, 32, 32, 32));

        var mark = markView(ICON_SIZE);
        if (mark != null) {
            content.getChildren().add(0, mark);
        }
        setInitialFocus(closeButton);
        return content;
    }

    /**
     * Hands the address to the platform's mail client. Best effort by design: a
     * desktop without a mail handler must not turn a click on a link into an
     * error dialog, so the failure only reaches the log.
     */
    private static void openMailClient() {
        try {
            var desktop = java.awt.Desktop.getDesktop();
            if (java.awt.Desktop.isDesktopSupported()
                    && desktop.isSupported(java.awt.Desktop.Action.MAIL)) {
                desktop.mail(new java.net.URI("mailto:mesutormanli@gmail.com"));
            }
        } catch (Exception error) {
            LOG.log(Level.FINE, "could not open the mail client", error);
        }
    }

    /** A small, wrapped, centred legal line of the About card. */
    private static Label legalLabel(String text) {
        var label = new Label(text);
        label.getStyleClass().add("about-legal");
        label.setWrapText(true);
        label.setMaxWidth(CONTENT_WIDTH);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        return label;
    }
}
