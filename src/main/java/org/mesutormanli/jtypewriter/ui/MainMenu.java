/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The command menu - File / Edit / View / Help - as one toolbar button.
 *
 * <p>It is a {@link MenuButton}, not a {@code MenuBar}: a permanent menu strip
 * above the toolbar costs a whole row of chrome that the text cannot reclaim,
 * and this application's promise is that {@code Ctrl+T} leaves nothing but
 * text. The menu therefore lives <em>inside</em> the toolbar row as a single
 * {@value #GLYPH} control, so hiding the toolbar hides the menu with it and no
 * separate permanent row exists. The discoverability the menu exists for is
 * kept: {@code New} and {@code Save As} have a visible home again.</p>
 *
 * <p>The entries are not hand-written: they are generated from the same
 * {@link EditorAction} inventory that drives the key filter, the toolbar
 * buttons and the shortcut card, so a command cannot be visible in one place
 * and missing from another. Before this class {@code New} and
 * {@code Save As} had shortcuts and card rows but no visible entry at all;
 * generating the menus from the inventory is what keeps that from recurring.</p>
 *
 * <p>Deliberately <em>not</em> a {@link MenuButton} subclass. Extending it
 * would initialise {@code javafx.scene.control.Control}, whose static
 * initialiser needs a JavaFX toolkit; headless tests that only call the pure
 * {@link #layout()} would then fail with "Toolkit not initialized".
 * Composition keeps the pure layout testable while {@link #node()} hands the
 * real button to {@link org.mesutormanli.jtypewriter.ui.component.ToolbarView}.</p>
 *
 * <p>The accelerators shown in the popup are a <em>description</em>, not a
 * second implementation: {@link KeyboardShortcutHandler}'s scene-level key
 * filter consumes every chord in the capturing phase, before any menu item
 * accelerator could run. They are still read from
 * {@link EditorAction#primaryChord()}, so a renamed chord cannot leave the
 * popup lying.</p>
 *
 * <p>{@code About} is the one entry that is <em>not</em> an
 * {@link EditorAction}: it opens a dialog view, not a document command.
 * Giving it an {@code EditorAction} would force a fake chord into the shortcut
 * card (where it has no meaning) and into the key filter (where it must not
 * fire). It is appended to Help here, using the same bundle label as the
 * toolbar's About button.</p>
 */
public final class MainMenu {

    /**
     * The closed button's label. A glyph rather than a word keeps the toolbar
     * narrow and language-neutral; the tooltip and the accessible text carry
     * the localized name. {@code U+2630} is covered by the DejaVu and Segoe
     * families, i.e. on both supported platforms.
     */
    public static final String GLYPH = "\u2630";

    /** The four top-level menus, with their bundle-backed titles. */
    public enum Page {
        FILE(Messages::menuFile),
        EDIT(Messages::menuEdit),
        VIEW(Messages::menuView),
        HELP(Messages::menuHelp);

        private final Function<Messages, String> title;

        Page(Function<Messages, String> title) {
            this.title = title;
        }

        public String title(Messages messages) {
            return title.apply(messages);
        }
    }

    /**
     * One menu and its generated commands. Defensive copy: callers cannot
     * mutate the shared layout after it is handed out.
     */
    public record MenuSpec(Page page, List<EditorAction> actions) {

        public MenuSpec {
            actions = List.copyOf(actions);
        }
    }

    /**
     * The menu structure, independent of JavaFX nodes and of the current
     * language. Every action that the platform does not perform itself appears
     * exactly once: {@code EXIT_FULLSCREEN} is owned by JavaFX's
     * {@code fullScreenExitKeyCombination}, so it gets no menu entry.
     */
    public static List<MenuSpec> layout() {
        return List.of(
                new MenuSpec(Page.FILE, List.of(
                        EditorAction.NEW_DOCUMENT,
                        EditorAction.OPEN,
                        EditorAction.SAVE,
                        EditorAction.SAVE_AS)),
                new MenuSpec(Page.EDIT, List.of(
                        EditorAction.UNDO,
                        EditorAction.REDO)),
                new MenuSpec(Page.VIEW, List.of(
                        EditorAction.TOGGLE_TOOLBAR,
                        EditorAction.CYCLE_THEME,
                        EditorAction.TOGGLE_YOLO,
                        EditorAction.CYCLE_TEXT_COLOR,
                        EditorAction.FONT_UP,
                        EditorAction.FONT_DOWN,
                        EditorAction.FONT_RESET,
                        EditorAction.TOGGLE_FULLSCREEN)),
                new MenuSpec(Page.HELP, List.of()));
    }

    private final Messages messages;
    private final AboutDialog aboutDialog;
    private final MenuButton menuButton = new MenuButton(GLYPH);

    private Consumer<EditorAction> actionRunner = action -> { };
    private Stage stage;

    public MainMenu(Messages messages, LocaleManager localeManager, AboutDialog aboutDialog) {
        this.messages = messages;
        this.aboutDialog = aboutDialog;
        // Both style classes matter: .toolbar-button gives the flat, themed
        // look and the focus ring; .toolbar-menu-button removes the dropdown
        // arrow, which would read as a second, competing affordance next to
        // the button labels it sits among.
        menuButton.getStyleClass().addAll("toolbar-button", "toolbar-menu-button");
        localeManager.addListener(language -> refresh());
        refresh();
    }

    /** The real JavaFX node, for the toolbar row to host. */
    public MenuButton node() {
        return menuButton;
    }

    /** Shared with the toolbar buttons and the key filter; defaults to a no-op. */
    public void setActionRunner(Consumer<EditorAction> actionRunner) {
        this.actionRunner = actionRunner;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Rebuilds the whole popup. A language switch has to relabel menu titles,
     * item labels and the tooltip; rebuilding is simpler and safer than
     * mutating an existing tree in place.
     */
    public void refresh() {
        menuButton.getItems().clear();
        for (var spec : layout()) {
            var menu = new Menu(spec.page().title(messages));
            for (var action : spec.actions()) {
                var item = new MenuItem(action.label(messages));
                item.setAccelerator(acceleratorFor(action));
                item.setOnAction(e -> actionRunner.accept(action));
                menu.getItems().add(item);
            }
            if (spec.page() == Page.HELP) {
                menu.getItems().add(aboutItem());
            }
            menuButton.getItems().add(menu);
        }
        // The tooltip and accessible name are owned by ToolbarView (it hosts
        // the node and re-reads them on every refresh, like every other
        // control's), not here: two owners would only fight over the same
        // property.
    }

    private MenuItem aboutItem() {
        var item = new MenuItem(messages.toolbarAbout());
        item.setOnAction(e -> {
            if (stage != null) {
                aboutDialog.show(stage);
            }
        });
        return item;
    }

    /**
     * Parses an action's primary chord into a JavaFX accelerator.
     *
     * <p>One special case: JavaFX's parser rejects {@code "Ctrl++"} because the
     * trailing {@code +} reads as another separator, so the final {@code +} is
     * written as {@code +Plus} ({@code "Ctrl++"} becomes {@code "Ctrl+Plus"}),
     * which JavaFX displays as {@code Ctrl++} again. Every other chord is
     * parsed verbatim from {@link EditorAction#primaryChord()}.</p>
     */
    static KeyCombination acceleratorFor(EditorAction action) {
        var chord = action.primaryChord();
        if (chord.endsWith("++")) {
            chord = chord.substring(0, chord.length() - 1) + "Plus";
        }
        return KeyCombination.keyCombination(chord);
    }
}
