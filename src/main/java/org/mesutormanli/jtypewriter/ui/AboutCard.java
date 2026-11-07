/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.text.TextAlignment;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.AppVersion;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The About card: the shared column (mark, title, tagline, shortcut table) with
 * the running version under the title, then the licence and warranty lines and
 * the author's e-mail link, floating over the editor in the window's top-right
 * corner.
 *
 * <p>Like the welcome card it used to be a modal, borderless window (see
 * {@link OverlayCard}). The reason it matters here is the e-mail link: opening
 * the mail client used to be reachable only while the window was up, and the
 * window took the whole screen. The card leaves the document visible, so a
 * user can copy a shortcut or a version string out of it while it is open.</p>
 *
 * <p>Everything it shares with the welcome card is built by
 * {@link OverlayCard#cardColumn(String, double, Node...)}; this class only adds
 * what makes it the About card, so the two can never drift.</p>
 */
public class AboutCard extends OverlayCard {

    /** The card's preferred width, kept from the old window. */
    private static final int CARD_WIDTH = 540;

    /** Vertical gap of this card's column; the welcome card's is looser. */
    private static final double SPACING = 12;

    private static final Logger LOG = AppLogging.logger(AboutCard.class);

    public AboutCard(Messages messages) {
        super("about-card", messages);
        setCardWidth(CARD_WIDTH);
    }

    @Override
    protected Node buildContent() {
        // Field reports need a version to name, so the running version
        // (from the Maven metadata, never hard-coded) sits right under the title.
        var versionLabel = new Label(messages.aboutVersion(AppVersion.get()));
        versionLabel.getStyleClass().add("about-version");

        var column = cardColumn(messages.appName(), SPACING, versionLabel);

        var emailLink = new Hyperlink("mesutormanli@gmail.com");
        emailLink.getStyleClass().add("about-email");
        emailLink.setOnAction(e -> openMailClient());

        // Licensing and the warranty disclaimer: the translated sentences are
        // longer than the card, so they wrap and centre like the tagline.
        var copyrightLabel = legalLabel(messages.aboutCopyright());
        var licenseLabel = legalLabel(messages.aboutLicense());
        var warrantyLabel = legalLabel(messages.aboutWarranty());

        column.getChildren().addAll(copyrightLabel, emailLink, licenseLabel, warrantyLabel);
        return column;
    }

    /**
     * Hands the address to the platform's mail client. Best effort by design: a
     * desktop without a mail handler must not turn a click on a link into an
     * error dialog, so the failure only reaches the log.
     */
    private void openMailClient() {
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
    private Label legalLabel(String text) {
        var label = new Label(text);
        label.getStyleClass().add("about-legal");
        label.setWrapText(true);
        label.setMaxWidth(contentWidth());
        label.setTextAlignment(TextAlignment.CENTER);
        label.setAlignment(Pos.CENTER);
        return label;
    }
}
