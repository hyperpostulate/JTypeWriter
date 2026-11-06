/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.input.KeyCode;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The editor's command inventory: one constant per command, carrying its
 * label, its accelerator(s) and the effect it performs.
 *
 * <p>This exists because the same command used to be spelled out in three
 * places - a {@code switch} in the key handler, a hard-coded tooltip constant
 * per toolbar button, and a hand-padded {@code welcomeShortcuts} bundle text.
 * They drifted in both directions: the card promised {@code Ctrl+S  Save /
 * Save As} (no such shortcut) while {@code Ctrl+Shift+Z} - which the handler
 * did map - was missing, and nothing could detect either mistake. Now the
 * card rows are <em>generated</em> from this enum, the key filter resolves
 * through it, and the toolbar buttons run the very same constants, so a
 * mismatch has no place to hide.</p>
 *
 * <p>Pure: no JavaFX nodes, no window state. The effect is expressed against
 * {@link Context}, which {@link KeyboardShortcutHandler} implements; tests can
 * pass a recorder and assert each constant's meaning without a toolkit.</p>
 *
 * <p>Declaration order is the order of the shortcut card. {@code REDO} owns
 * two accelerators on purpose - {@code Ctrl+Shift+Z} and {@code Ctrl+Y} are
 * both commonly expected - and {@code EXIT_FULLSCREEN} is listed even though
 * it is performed by JavaFX's own {@code fullScreenExitKeyCombination}: the
 * card promises it, so the inventory has to know it.</p>
 */
public enum EditorAction {

    TOGGLE_TOOLBAR(List.of(ctrl("Ctrl+T", KeyCode.T)),
            Messages::shortcutToggleToolbar, Context::toggleToolbar),

    CYCLE_THEME(List.of(ctrlShift("Ctrl+Shift+T", KeyCode.T)),
            Messages::shortcutSwitchTheme, Context::cycleTheme),

    TOGGLE_YOLO(List.of(ctrlShift("Ctrl+Shift+Y", KeyCode.Y)),
            Messages::shortcutYoloMode, Context::toggleYolo),

    CYCLE_TEXT_COLOR(List.of(ctrlShift("Ctrl+Shift+C", KeyCode.C)),
            Messages::shortcutCycleTextColor, Context::cycleTextColor),

    FONT_UP(List.of(ctrl("Ctrl++", KeyCode.EQUALS, KeyCode.PLUS, KeyCode.ADD)),
            Messages::shortcutFontUp, Context::increaseFontSize),

    FONT_DOWN(List.of(ctrl("Ctrl+-", KeyCode.MINUS, KeyCode.SUBTRACT)),
            Messages::shortcutFontDown, Context::decreaseFontSize),

    FONT_RESET(List.of(ctrl("Ctrl+0", KeyCode.DIGIT0, KeyCode.NUMPAD0)),
            Messages::shortcutFontReset, Context::resetFontSize),

    UNDO(List.of(ctrl("Ctrl+Z", KeyCode.Z)),
            Messages::toolbarUndo, Context::undo),

    REDO(List.of(ctrlShift("Ctrl+Shift+Z", KeyCode.Z), ctrl("Ctrl+Y", KeyCode.Y)),
            Messages::toolbarRedo, Context::redo),

    NEW_DOCUMENT(List.of(ctrlAnyShift("Ctrl+N", KeyCode.N)),
            Messages::shortcutNewFile, Context::newDocument),

    OPEN(List.of(ctrlAnyShift("Ctrl+O", KeyCode.O)),
            Messages::toolbarOpen, Context::openDocument),

    SAVE(List.of(ctrl("Ctrl+S", KeyCode.S)),
            Messages::toolbarSave, Context::save),

    SAVE_AS(List.of(ctrlShift("Ctrl+Shift+S", KeyCode.S)),
            Messages::shortcutSaveAs, Context::saveAs),

    TOGGLE_FULLSCREEN(List.of(plain("F11", KeyCode.F11)),
            Messages::shortcutToggleFullscreen, Context::toggleFullScreen),

    EXIT_FULLSCREEN(List.of(plain("Escape", KeyCode.ESCAPE)),
            Messages::shortcutExitFullscreen, Context::exitFullScreen);

    /** What a command does, expressed as capabilities of the live window. */
    public interface Context {

        void toggleToolbar();

        void cycleTheme();

        void undo();

        void redo();

        void toggleYolo();

        void cycleTextColor();

        void openDocument();

        void newDocument();

        void save();

        void saveAs();

        void increaseFontSize();

        void decreaseFontSize();

        void resetFontSize();

        void toggleFullScreen();

        void exitFullScreen();
    }

    /**
     * One chord the user can press: how it is written on the card, which key
     * codes it accepts, whether Ctrl is required and whether Shift is part of
     * the identity.
     *
     * <p>{@code codes} is a list because the numpad keys must reach the same
     * command as their main-keyboard neighbours ({@code Ctrl+Num0} resets the
     * font just like {@code Ctrl+0}). {@code shift == null} means "either
     * state": the chords that never involved Shift in the old filter
     * ({@code Ctrl+N}, {@code Ctrl+O}, F11) still fire with it held, so this
     * refactor changes no existing shortcut. Ctrl is declared explicitly
     * because F11 and Escape are meaningful without it.</p>
     */
    public record Accelerator(String chord, boolean requiresCtrl, Boolean shift,
                              List<KeyCode> codes) {

        public Accelerator {
            codes = List.copyOf(codes);
        }

        boolean matches(KeyCode code, boolean shiftDown) {
            return codes.contains(code) && (shift == null || shift == shiftDown);
        }
    }

    private final List<Accelerator> accelerators;
    private final Function<Messages, String> label;
    private final Consumer<Context> effect;

    EditorAction(List<Accelerator> accelerators, Function<Messages, String> label,
                 Consumer<Context> effect) {
        this.accelerators = List.copyOf(accelerators);
        this.label = label;
        this.effect = effect;
    }

    /** The chord(s) this command answers to, in card order. */
    public List<Accelerator> accelerators() {
        return accelerators;
    }

    /** The chord shown next to a toolbar button. */
    public String primaryChord() {
        return accelerators.get(0).chord();
    }

    /** The localized label in the current UI language. */
    public String label(Messages messages) {
        return label.apply(messages);
    }

    public void run(Context context) {
        effect.accept(context);
    }

    /**
     * Whether JavaFX performs this command itself, before the key filter sees
     * it - true only for {@code EXIT_FULLSCREEN}, whose chord is the stage's
     * {@code fullScreenExitKeyCombination}.
     */
    public boolean handledByPlatform() {
        return this == EXIT_FULLSCREEN;
    }

    /**
     * The command a key press means, or empty when the chord belongs to no
     * editor action (a plain {@code Ctrl+C} copy, for instance, must never be
     * consumed by this layer).
     */
    public static Optional<EditorAction> resolve(KeyCode code, boolean shift) {
        for (var action : values()) {
            if (action.handledByPlatform()) {
                continue;
            }
            for (var accelerator : action.accelerators) {
                if (accelerator.matches(code, shift)) {
                    return Optional.of(action);
                }
            }
        }
        return Optional.empty();
    }

    private static Accelerator ctrl(String chord, KeyCode... codes) {
        return new Accelerator(chord, true, false, List.of(codes));
    }

    private static Accelerator ctrlShift(String chord, KeyCode... codes) {
        return new Accelerator(chord, true, true, List.of(codes));
    }

    /** Ctrl plus any Shift state, matching the old filter for Ctrl+N and Ctrl+O. */
    private static Accelerator ctrlAnyShift(String chord, KeyCode... codes) {
        return new Accelerator(chord, true, null, List.of(codes));
    }

    private static Accelerator plain(String chord, KeyCode... codes) {
        return new Accelerator(chord, false, null, List.of(codes));
    }
}
