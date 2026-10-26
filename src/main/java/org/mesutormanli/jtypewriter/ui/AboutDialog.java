/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.AppVersion;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AboutDialog {

    /** Margin keeping the card's border and drop shadow clear of the window edge. */
    private static final int SHELL_MARGIN = 14;

    /** Width of the drawn application mark; square artwork, so height matches. */
    private static final int ICON_SIZE = 80;

    /** Content width of the card (scene width minus the horizontal padding). */
    private static final int CONTENT_WIDTH = 540 - 2 * 32;

    private static final Logger LOG = AppLogging.logger(AboutDialog.class);

    private final Messages messages;
    private final ThemeManager themeManager;

    public AboutDialog(Messages messages, ThemeManager themeManager) {
        this.messages = messages;
        this.themeManager = themeManager;
    }

    public void show(Stage owner) {
        var titleLabel = new Label("JTypeWriter");
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

        // Same two-column grid as the welcome dialog (see ShortcutTable):
        // real columns instead of the space-padded single Label, with the
        // wrapping the long German/French/Italian/Spanish descriptions need
        // is handled inside the grid.
        var shortcuts = ShortcutTable.build(
                ShortcutRows.parse(messages.welcomeShortcuts()), CONTENT_WIDTH);

        var emailLink = new Hyperlink("mesutormanli@gmail.com");
        emailLink.getStyleClass().add("about-email");
        emailLink.setOnAction(e -> {
            try {
                var desktop = java.awt.Desktop.getDesktop();
                if (java.awt.Desktop.isDesktopSupported() && desktop.isSupported(java.awt.Desktop.Action.MAIL)) {
                    desktop.mail(new java.net.URI("mailto:mesutormanli@gmail.com"));
                }
            } catch (Exception error) {
                LOG.log(Level.FINE, "could not open the mail client", error);
            }
        });

        // Licensing and the warranty disclaimer: the translated sentences are
        // longer than the card, so they wrap and centre like the tagline.
        var copyrightLabel = legalLabel(messages.aboutCopyright());
        var licenseLabel = legalLabel(messages.aboutLicense());
        var warrantyLabel = legalLabel(messages.aboutWarranty());

        var closeBtn = new Button(messages.aboutClose());
        closeBtn.getStyleClass().add("welcome-button");
        closeBtn.setDefaultButton(true);

        var contentBox = new VBox(12, titleLabel, versionLabel, taglineLabel, shortcuts,
                copyrightLabel, emailLink, licenseLabel, warrantyLabel, closeBtn);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(36, 32, 32, 32));
        contentBox.getStyleClass().addAll("root", "welcome-root");

        // Application mark above the title, README-header style; skipped
        // entirely when the asset is missing so no blank gap appears.
        var iconImage = AppIcons.dialogIcon();
        if (iconImage != null) {
            var icon = new ImageView(iconImage);
            icon.getStyleClass().add("dialog-icon");
            icon.setPreserveRatio(true);
            icon.setSmooth(true);
            icon.setCache(true);
            icon.setFitWidth(ICON_SIZE);
            contentBox.getChildren().add(0, icon);
        }

        var scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("dialog-scroll");

        // Opaque borderless window whose scene fill equals the card surface
        // colour: the anti-aliased rounded corners and drop shadow blend into
        // that identical colour and stay inside the window margin - no alpha
        // compositing anywhere, so no white fringe or flash can form at the
        // edges.
        var shell = new StackPane(scrollPane);
        shell.setPadding(new Insets(SHELL_MARGIN));
        shell.setStyle("-fx-background-color: transparent;");

        var surface = themeManager.getCurrentTheme().getDialogFill();
        // Preferred size stays the historical card size, so the tallest locales
        // still fit without scrolling; clamping then keeps the window inside
        // the visual bounds on a 768 px-tall display.
        var visual = Screen.getPrimary().getVisualBounds();
        var scene = new Scene(shell,
                DialogSizing.clampWidth(540 + 2 * SHELL_MARGIN, visual.getWidth()),
                DialogSizing.clampHeight(820 + 2 * SHELL_MARGIN, visual.getHeight()));
        scene.setFill(Color.web(surface));
        scene.getStylesheets().addAll(owner.getScene().getStylesheets());

        var dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        // Undecorated: no visible title bar, but the OS task switcher / window
        // list still shows this title - do not remove it as "dead code".
        dialog.setTitle(messages.aboutWindowTitle());
        dialog.setScene(scene);
        dialog.setResizable(true);

        closeBtn.setOnAction(e -> dialog.close());

        scene.setOnKeyPressed(e -> {
            var code = e.getCode().toString();
            if (code.equals("ESCAPE") || code.equals("ENTER")) {
                dialog.close();
            }
        });

        dialog.showAndWait();
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
