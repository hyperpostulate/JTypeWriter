package org.mesutormanli.jtypewriter.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
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
 *
 * <p>The window is deliberately opaque ({@code UNDECORATED}) and its scene fill
 * equals the surface colour underneath (the editor background). Transparent
 * windows rely on per-pixel alpha, which several compositors fringe with white
 * along every anti-aliased edge; opaque matching fills blend into themselves,
 * so neither a fringe nor a startup flash can appear. Placement is derived from
 * the owner's geometry with finite-value guards (the owner size is not always
 * available immediately) and clamped inside the owner so the toast can never
 * fly off to a screen edge.</p>
 */
public final class Toast {

    private static final Duration VISIBLE_FOR = Duration.seconds(2);
    /** Margin letting the pill float clear of the window edges. */
    private static final double SHELL_MARGIN = 8;
    private static final double TOP_OFFSET = 64;

    private Toast() {
    }

    /** Shows {@code message} on a pill over an opaque chip coloured like the canvas. */
    public static void show(Stage owner, String message, String surfaceFill) {
        if (owner == null) {
            return;
        }
        var toast = new Stage();
        toast.initOwner(owner);
        toast.initStyle(StageStyle.UNDECORATED);
        toast.setAlwaysOnTop(true);

        var label = new Label(message);
        label.setStyle("-fx-background-color: #c0883c; -fx-text-fill: white;"
                + " -fx-padding: 10 18 10 18; -fx-background-radius: 6;");

        var root = new StackPane(label);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(SHELL_MARGIN));
        root.setStyle("-fx-background-color: transparent;");

        var scene = new Scene(root);
        scene.setFill(Color.web(surfaceFill));
        toast.setScene(scene);
        toast.sizeToScene();

        toast.show();
        placeInside(toast, owner);

        var pause = new PauseTransition(VISIBLE_FOR);
        pause.setOnFinished(e -> toast.close());
        pause.play();
    }

    /** Top-centered inside the owner, never outside it, with NaN-safe math. */
    private static void placeInside(Stage toast, Stage owner) {
        double width = finite(toast.getWidth(), 0);
        double height = finite(toast.getHeight(), 0);
        double ownerX = finite(owner.getX(), 0);
        double ownerY = finite(owner.getY(), 0);
        double ownerWidth = finite(owner.getWidth(), width);
        double ownerHeight = finite(owner.getHeight(), TOP_OFFSET + height);

        var x = ownerX + Math.max(0, (ownerWidth - width) / 2);
        var y = clamp(ownerY + TOP_OFFSET, ownerY, Math.max(ownerY, ownerY + ownerHeight - height));
        toast.setX(x);
        toast.setY(y);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }
}