package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AboutDialog {

    /** Margin keeping the card's border and drop shadow clear of the window edge. */
    private static final int SHELL_MARGIN = 14;

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

        var subtitleLabel = new Label(messages.welcomeSubtitle());
        subtitleLabel.getStyleClass().add("welcome-subtitle");

        var descLabel = new Label(messages.aboutDescription());
        descLabel.getStyleClass().add("about-description");
        descLabel.setWrapText(true);

        var shortcutsLabel = new Label(messages.welcomeShortcuts());
        shortcutsLabel.getStyleClass().add("welcome-shortcuts");

        var authorLabel = new Label(messages.aboutAuthor());
        authorLabel.getStyleClass().add("about-author");

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

        var closeBtn = new Button(messages.aboutClose());
        closeBtn.getStyleClass().add("welcome-button");
        closeBtn.setDefaultButton(true);

        var contentBox = new VBox(12, titleLabel, subtitleLabel, descLabel, shortcutsLabel, authorLabel, emailLink, closeBtn);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(36, 32, 32, 32));
        contentBox.getStyleClass().addAll("root", "welcome-root");

        var scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("about-scroll");

        // Opaque borderless window whose scene fill equals the card surface
        // colour: the anti-aliased rounded corners and drop shadow blend into
        // that identical colour and stay inside the window margin - no alpha
        // compositing anywhere, so no white fringe or flash can form at the
        // edges.
        var shell = new StackPane(scrollPane);
        shell.setPadding(new Insets(SHELL_MARGIN));
        shell.setStyle("-fx-background-color: transparent;");

        var surface = themeManager.getCurrentTheme().getDialogFill();
        var scene = new Scene(shell, 540 + 2 * SHELL_MARGIN, 640 + 2 * SHELL_MARGIN);
        scene.setFill(Color.web(surface));
        scene.getStylesheets().addAll(owner.getScene().getStylesheets());

        var dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setScene(scene);
        dialog.setResizable(false);

        closeBtn.setOnAction(e -> dialog.close());

        scene.setOnKeyPressed(e -> {
            var code = e.getCode().toString();
            if (code.equals("ESCAPE") || code.equals("ENTER")) {
                dialog.close();
            }
        });

        dialog.showAndWait();
    }
}
