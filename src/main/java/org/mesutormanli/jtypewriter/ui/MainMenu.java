/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCombination;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
 * <p>The entries are not hand-written: the leaves are generated from the same
 * {@link EditorAction} inventory that drives the key filter, the toolbar
 * buttons and the shortcut card. The five View settings are generated from the
 * same option lists as the toolbar dropdowns, through the {@link Settings}
 * port the toolbar implements, so a value list cannot be visible in the bar and
 * different in the menu. Before this class {@code New} and {@code Save As} had
 * shortcuts and card rows but no visible entry at all; generating the menus
 * from the inventory is what keeps that from recurring.</p>
 *
 * <p><strong>View is a set of value submenus, not cycling commands.</strong>
 * {@code Theme}, {@code YOLO}, {@code Sound}, {@code Text Color} and
 * {@code Font Size} open one level deeper and list the same values the toolbar
 * dropdowns do, with the current one checked. That is deliberate: the toolbar
 * already offers direct choice, so {@code View > Theme > Dark} has to mean
 * {@code Dark}, not "the next theme" as the {@code CYCLE_THEME} shortcut does.
 * The cycling actions keep their chords (and their shortcut-card rows); they
 * are simply no longer the menu's way of expressing the setting.</p>
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
     * The five settings the View menu mirrors from the toolbar, in toolbar
     * order. Each one is a submenu of concrete values rather than a cycling
     * action; the titles reuse the toolbar's bundle keys, so a language switch
     * relabels both entry points at once.
     */
    public enum ChoiceKind {
        THEME(Messages::toolbarTheme),
        YOLO(Messages::toolbarYolo),
        SOUND(Messages::toolbarSound),
        TEXT_COLOR(Messages::toolbarColor),
        FONT_SIZE(Messages::toolbarFontSize);

        private final Function<Messages, String> title;

        ChoiceKind(Function<Messages, String> title) {
            this.title = title;
        }

        public String title(Messages messages) {
            return title.apply(messages);
        }
    }

    /** One entry of a top-level menu: either a command leaf or a value submenu. */
    public sealed interface Entry permits ActionEntry, ChoiceEntry {
    }

    /** A leaf that runs one {@link EditorAction}. */
    public record ActionEntry(EditorAction action) implements Entry {
    }

    /** A submenu whose radio items select one value of a settings family. */
    public record ChoiceEntry(ChoiceKind kind) implements Entry {
    }

    /**
     * One menu and its generated entries. Defensive copy: callers cannot
     * mutate the shared layout after it is handed out.
     */
    public record MenuSpec(Page page, List<Entry> entries) {

        public MenuSpec {
            entries = List.copyOf(entries);
        }
    }

    /**
     * One settings family as the menu needs it: the values in display order,
     * the current one, how a value is named, and what selecting it does.
     * Built by the toolbar on every rebuild, so the popup cannot keep a second
     * copy of the state - a change made from either entry point is visible in
     * the other.
     */
    public record Choice<T>(List<T> options, T current,
                            Function<T, String> label, Consumer<T> select) {

        public Choice {
            options = List.copyOf(options);
        }
    }

    /**
     * Supplies the live choice lists. The toolbar implements it because it
     * already owns the managers and the apply paths the dropdowns use; the
     * menu must not duplicate those writes.
     */
    public interface Settings {

        Choice<?> choice(ChoiceKind kind);
    }

    /** One resolved radio row: its label, whether it is the current value, and its effect. */
    public record ResolvedOption(String label, boolean selected, Runnable select) {
    }

    /** A resolved value submenu, free of JavaFX nodes so it can be tested headlessly. */
    public record ResolvedChoice(String title, List<ResolvedOption> options) {

        public ResolvedChoice {
            options = List.copyOf(options);
        }
    }

    /**
     * The menu structure, independent of JavaFX nodes and of the current
     * language. Every command leaf is an {@link EditorAction}, and every
     * action the platform does not perform itself appears either as a leaf or
     * through a value submenu - never twice. {@code EXIT_FULLSCREEN} is owned
     * by JavaFX's {@code fullScreenExitKeyCombination}, so it gets no entry;
     * the six setting commands are represented by the submenus that select
     * their value directly (see the class Javadoc).
     */
    public static List<MenuSpec> layout() {
        return List.of(
                new MenuSpec(Page.FILE, List.of(
                        command(EditorAction.NEW_DOCUMENT),
                        command(EditorAction.OPEN),
                        command(EditorAction.SAVE),
                        command(EditorAction.SAVE_AS))),
                new MenuSpec(Page.EDIT, List.of(
                        command(EditorAction.UNDO),
                        command(EditorAction.REDO),
                        command(EditorAction.FIND),
                        command(EditorAction.FIND_NEXT),
                        command(EditorAction.FIND_PREVIOUS),
                        command(EditorAction.REPLACE))),
                new MenuSpec(Page.VIEW, List.of(
                        command(EditorAction.TOGGLE_TOOLBAR),
                        new ChoiceEntry(ChoiceKind.THEME),
                        new ChoiceEntry(ChoiceKind.YOLO),
                        new ChoiceEntry(ChoiceKind.SOUND),
                        new ChoiceEntry(ChoiceKind.TEXT_COLOR),
                        new ChoiceEntry(ChoiceKind.FONT_SIZE),
                        command(EditorAction.TOGGLE_FULLSCREEN))),
                new MenuSpec(Page.HELP, List.of()));
    }

    /**
     * The commands whose setting the View menu expresses as a value submenu
     * instead of a leaf: the theme, YOLO and colour cycles plus the font-step
     * commands. They keep their chords and their shortcut-card rows; pinning
     * the set here means the menu cannot silently drop one, while a new
     * settings command has to make the same choice explicitly.
     */
    public static Set<EditorAction> submenuBackedActions() {
        return Set.of(
                EditorAction.CYCLE_THEME,
                EditorAction.TOGGLE_YOLO,
                EditorAction.CYCLE_TEXT_COLOR,
                EditorAction.FONT_UP,
                EditorAction.FONT_DOWN,
                EditorAction.FONT_RESET);
    }

    private static ActionEntry command(EditorAction action) {
        return new ActionEntry(action);
    }

    private final Messages messages;
    private final AboutCard aboutCard;
    private final MenuButton menuButton = new MenuButton(GLYPH);

    private Consumer<EditorAction> actionRunner = action -> { };
    private Runnable welcomeRunner = () -> { };
    private Runnable recentRunner = () -> { };
    private Settings settings;

    public MainMenu(Messages messages, LocaleManager localeManager, AboutCard aboutCard) {
        this.messages = messages;
        this.aboutCard = aboutCard;
        // Both style classes matter: .toolbar-button gives the flat, themed
        // look and the focus ring; .toolbar-menu-button removes the dropdown
        // arrow, which would read as a second, competing affordance next to
        // the button labels it sits among.
        menuButton.getStyleClass().addAll("toolbar-button", "toolbar-menu-button");
        // Rebuild right before the popup opens: the checked row and the option
        // lists follow the live managers, so a theme changed by the toolbar, a
        // shortcut or a language switch cannot leave the menu stale. ON_SHOWING
        // fires before the skin calls popup.show(), so replacing the items here
        // is safe and the popup never renders a half-old tree.
        menuButton.setOnShowing(event -> refresh());
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

    /**
     * The File menu's "Show welcome card" command. It is a view action, not a
     * document command, so it has no {@link EditorAction} - and therefore no
     * chord in the key filter or row on the shortcut card, exactly like About.
     * Installed by the controller, which owns the card and the preference.
     */
    public void setWelcomeRunner(Runnable welcomeRunner) {
        this.welcomeRunner = Objects.requireNonNull(welcomeRunner, "welcomeRunner");
    }

    /**
     * The File menu's "Recent Files" command, the other view action on this
     * menu. It sits beside "Show Welcome Card" because the two used to be one
     * surface: the list is a working view, so it keeps its own entry no matter
     * whether the greeting is switched off.
     */
    public void setRecentRunner(Runnable recentRunner) {
        this.recentRunner = Objects.requireNonNull(recentRunner, "recentRunner");
    }

    /** Installed by the toolbar, which owns the managers and the apply paths. */
    public void setSettings(Settings settings) {
        this.settings = settings;
        refresh();
    }

    /**
     * Rebuilds the whole popup. A language switch has to relabel menu titles,
     * item labels and the tooltip; a settings change has to move the checked
     * radio row. Rebuilding is simpler and safer than mutating an existing
     * tree in place.
     */
    public void refresh() {
        menuButton.getItems().clear();
        for (var spec : layout()) {
            var menu = new Menu(spec.page().title(messages));
            for (var entry : spec.entries()) {
                switch (entry) {
                    case ActionEntry(var action) -> menu.getItems().add(commandItem(action));
                    case ChoiceEntry(var kind) -> menu.getItems().add(choiceMenu(kind));
                }
            }
            if (spec.page() == Page.FILE) {
                // A separator, because the two view commands below it are not
                // document commands.
                menu.getItems().addAll(new SeparatorMenuItem(), recentItem(), welcomeItem());
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

    private MenuItem commandItem(EditorAction action) {
        var item = new MenuItem(action.label(messages));
        item.setAccelerator(acceleratorFor(action));
        item.setOnAction(event -> actionRunner.accept(action));
        return item;
    }

    /**
     * Builds one value submenu. Radio items in an explicit {@link ToggleGroup}
     * make the current value visible and keep exactly one selected; without a
     * settings provider (only possible before the toolbar installs itself) the
     * submenu still exists but is empty, which the tests never see in the live
     * graph.
     */
    private Menu choiceMenu(ChoiceKind kind) {
        if (settings == null) {
            return new Menu(kind.title(messages));
        }
        var resolved = resolve(kind, messages, settings);
        var menu = new Menu(resolved.title());
        var group = new ToggleGroup();
        for (var option : resolved.options()) {
            var item = new RadioMenuItem(option.label());
            item.setToggleGroup(group);
            item.setSelected(option.selected());
            item.setOnAction(event -> option.select().run());
            menu.getItems().add(item);
        }
        return menu;
    }

    /**
     * Resolves a submenu to plain data: title, option labels, the checked one
     * and the effect. Pure - no JavaFX node is created - so the radio-row
     * decision can be tested headlessly with a fake {@link Settings}.
     */
    public static ResolvedChoice resolve(ChoiceKind kind, Messages messages, Settings settings) {
        return resolveChoice(kind, messages, settings.choice(kind));
    }

    private static <T> ResolvedChoice resolveChoice(ChoiceKind kind, Messages messages, Choice<T> choice) {
        var options = new ArrayList<ResolvedOption>();
        for (var value : choice.options()) {
            options.add(new ResolvedOption(
                    choice.label().apply(value),
                    Objects.equals(value, choice.current()),
                    () -> choice.select().accept(value)));
        }
        return new ResolvedChoice(kind.title(messages), options);
    }

    private MenuItem aboutItem() {
        var item = new MenuItem(messages.toolbarAbout());
        item.setOnAction(event -> aboutCard.show());
        return item;
    }

    private MenuItem recentItem() {
        var item = new MenuItem(messages.menuRecentFiles());
        item.setOnAction(event -> recentRunner.run());
        return item;
    }

    private MenuItem welcomeItem() {
        var item = new MenuItem(messages.menuShowWelcome());
        item.setOnAction(event -> welcomeRunner.run());
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
