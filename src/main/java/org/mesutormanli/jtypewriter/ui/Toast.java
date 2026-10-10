package org.mesutormanli.jtypewriter.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Minimal transient notification: a small auto-closing overlay near the top of
 * its owner, used for mode changes that would otherwise be easy to miss (such
 * as entering/leaving YOLO mode).
 */
public final class Toast {

    private static final Duration VISIBLE_FOR = Duration.seconds(2);

    private Toast() {
    }

    public static void show(Stage owner, String message) {
        if (owner == null) {
            return;
        }
        var toast = new Stage();
        toast.initOwner(owner);
        toast.initStyle(StageStyle.TRANSPARENT);
        toast.setAlwaysOnTop(true);

        var label = new Label(message);
        label.setStyle("-fx-background-color: #c0883c; -fx-text-fill: white;"
                + " -fx-padding: 10 18 10 18; -fx-background-radius: 6;");
        var root = new StackPane(label);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: transparent;");

        var scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toast.setScene(scene);

        toast.show();
        // top-center of the owner window, below the title area
        toast.setX(owner.getX() + (owner.getWidth() - toast.getWidth()) / 2);
        toast.setY(owner.getY() + 64);

        var pause = new PauseTransition(VISIBLE_FOR);
        pause.setOnFinished(e -> toast.close());
        pause.play();
    }
}