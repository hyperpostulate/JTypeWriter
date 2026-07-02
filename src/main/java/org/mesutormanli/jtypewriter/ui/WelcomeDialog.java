package org.mesutormanli.jtypewriter.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.springframework.stereotype.Component;

@Component
public class WelcomeDialog {

    private final Messages messages;
    private final LocaleManager localeManager;

    public WelcomeDialog(Messages messages, LocaleManager localeManager) {
        this.messages = messages;
        this.localeManager = localeManager;
    }

    public void show(Stage owner) {
        var titleLabel = new Label(messages.welcomeTitle());
        titleLabel.getStyleClass().add("welcome-title");

        var subtitleLabel = new Label(messages.welcomeSubtitle());
        subtitleLabel.getStyleClass().add("welcome-subtitle");

        var shortcutsLabel = new Label(messages.welcomeShortcuts());
        shortcutsLabel.getStyleClass().add("welcome-shortcuts");

        var startBtn = new Button(messages.welcomeButton());
        startBtn.getStyleClass().add("welcome-button");
        startBtn.setDefaultButton(true);

        var root = new VBox(12, titleLabel, subtitleLabel, shortcutsLabel, startBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32));
        root.getStyleClass().add("welcome-root");

        var scene = new Scene(root, 440, 380);
        scene.getStylesheets().addAll(owner.getScene().getStylesheets());

        var dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
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
