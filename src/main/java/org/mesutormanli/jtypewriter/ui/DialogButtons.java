package org.mesutormanli.jtypewriter.ui;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Factory for the custom buttons of the application's {@link javafx.scene.control.Alert}s.
 *
 * <p>A bare {@code new ButtonType(text)} carries {@link ButtonBar.ButtonData#OTHER},
 * so the dialog ends up with no default and no cancel button: Enter does nothing
 * and Escape does not close the prompt. Every custom button therefore declares
 * its intent here, so the keyboard contract is decided in one place instead of
 * being repeated - and accidentally forgotten - at each alert (analysis U11).
 *
 * <p>Pure JavaFX control classes, no toolkit: safe to unit-test headlessly.
 */
public final class DialogButtons {

    private DialogButtons() {
    }

    /**
     * The affirmative action of a dialog, such as "Save" or "Overwrite".
     * {@link ButtonBar.ButtonData#OK_DONE} marks it as the default button, so
     * Enter triggers it wherever focus happens to be.
     */
    public static ButtonType primary(String text) {
        return new ButtonType(text, ButtonBar.ButtonData.OK_DONE);
    }

    /**
     * The "do nothing" action, such as "Cancel" or "Later".
     * {@link ButtonBar.ButtonData#CANCEL_CLOSE} marks it as the cancel button, so
     * Escape activates it and the window close button maps to the same choice -
     * which is exactly what an aborted operation must mean.
     */
    public static ButtonType cancel(String text) {
        return new ButtonType(text, ButtonBar.ButtonData.CANCEL_CLOSE);
    }

    /**
     * A real alternative that is neither the default nor the cancel action, such
     * as "Discard" or the lossy encoding choice. It must not be
     * {@link ButtonBar.ButtonData#OK_DONE}, because Enter has to keep meaning the
     * safe/expected action, and it must not be
     * {@link ButtonBar.ButtonData#CANCEL_CLOSE}, because Escape would then run a
     * destructive branch. {@link ButtonBar.ButtonData#OTHER} leaves the button
     * clickable while claiming neither accelerator.
     */
    public static ButtonType alternative(String text) {
        return new ButtonType(text, ButtonBar.ButtonData.OTHER);
    }
}
