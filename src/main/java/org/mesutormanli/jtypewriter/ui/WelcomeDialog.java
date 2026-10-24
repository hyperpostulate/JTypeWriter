package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;

public class WelcomeDialog {

    /** Margin keeping the card's border and drop shadow clear of the window edge. */
    private static final int SHELL_MARGIN = 14;

    /** Width of the drawn application mark; square artwork, so height matches. */
    private static final int ICON_SIZE = 80;

    /** Content width of the card (scene width minus the horizontal padding). */
    private static final int CONTENT_WIDTH = 460 - 2 * 32;

    private final Messages messages;
    private final ThemeManager themeManager;

    public WelcomeDialog(Messages messages, ThemeManager themeManager) {
        this.messages = messages;
        this.themeManager = themeManager;
    }

    public void show(Stage owner) {
        var titleLabel = new Label(messages.welcomeTitle());
        titleLabel.getStyleClass().add("welcome-title");

        var taglineLabel = new Label(messages.appTagline());
        taglineLabel.getStyleClass().add("welcome-tagline");
        taglineLabel.setWrapText(true);
        taglineLabel.setMaxWidth(CONTENT_WIDTH);
        taglineLabel.setTextAlignment(TextAlignment.CENTER);
        taglineLabel.setAlignment(Pos.CENTER);

        var shortcutsLabel = new Label(messages.welcomeShortcuts());
        shortcutsLabel.getStyleClass().add("welcome-shortcuts");

        var startBtn = new Button(messages.welcomeButton());
        startBtn.getStyleClass().add("welcome-button");
        startBtn.setDefaultButton(true);

        var root = new VBox(14, titleLabel, taglineLabel, shortcutsLabel, startBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(36, 32, 32, 32));
        root.getStyleClass().addAll("root", "welcome-root");

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
            root.getChildren().add(0, icon);
        }

        // Opaque borderless window whose scene fill equals the card surface
        // colour: the anti-aliased rounded corners blend into that identical
        // colour, and the margin keeps the border and drop shadow inside the
        // window - no alpha compositing anywhere, so no white fringe or flash
        // can form at the edges.
        var shell = new StackPane(root);
        shell.setPadding(new Insets(SHELL_MARGIN));
        shell.setStyle("-fx-background-color: transparent;");

        var surface = themeManager.getCurrentTheme().getDialogFill();
        // The card fits the tallest locale the static font-metric model found
        // (German: the shortcuts block wraps twice, the tagline once more).
        var scene = new Scene(shell, 460 + 2 * SHELL_MARGIN, 750 + 2 * SHELL_MARGIN);
        scene.setFill(Color.web(surface));
        scene.getStylesheets().addAll(owner.getScene().getStylesheets());

        var dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        // Undecorated: no visible title bar, but the OS task switcher / window
        // list still shows this title - do not remove it as "dead code".
        dialog.setTitle(messages.welcomeWindowTitle());
        dialog.setScene(scene);
        dialog.setResizable(false);

        startBtn.setOnAction(e -> dialog.close());

        scene.setOnKeyPressed(e -> {
            var code = e.getCode().toString();
            if (code.equals("ESCAPE") || code.equals("ENTER")) {
                dialog.close();
            }
        });

        dialog.showAndWait();
    }
}
