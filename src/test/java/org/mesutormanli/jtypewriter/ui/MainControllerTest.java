/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The window title is the only place a user can see which document is open and
 * whether it is saved (the toolbar can be hidden), and the stats bar carries
 * the "Saved 12:04" confirmation. Both compositions are pure so they
 * can be pinned down here without a JavaFX toolkit.
 */
class MainControllerTest {

    @Test
    void titleShowsTheApplicationAndDocumentName() {
        assertEquals("JTypeWriter - novel.txt",
                MainController.titleFor("JTypeWriter", "novel.txt", false, false, " - "));
    }

    @Test
    void unsavedChangesAreMarkedWithAStar() {
        assertEquals("JTypeWriter - novel.txt *",
                MainController.titleFor("JTypeWriter", "novel.txt", true, false, " - "));
    }

    @Test
    void yoloBadgeIsAppendedAfterTheDirtyMarker() {
        assertEquals("JTypeWriter - novel.txt *   [YOLO]",
                MainController.titleFor("JTypeWriter", "novel.txt", true, true, " - "));
        assertEquals("JTypeWriter - novel.txt   [YOLO]",
                MainController.titleFor("JTypeWriter", "novel.txt", false, true, " - "));
    }

    /**
     * The text listener refreshes the title on every keystroke, so
     * an unchanged composition must never reach the native window again.
     */
    @Test
    void titleIsOnlyRewrittenWhenTheDirtyStateChanges() {
        var clean = MainController.titleFor("JTypeWriter", "novel.txt", false, false, " - ");
        var dirty = MainController.titleFor("JTypeWriter", "novel.txt", true, false, " - ");

        assertFalse(MainController.titleChanged(clean, clean));
        assertTrue(MainController.titleChanged(clean, dirty));
        assertTrue(MainController.titleChanged(null, clean), "the first paint has no previous title");
    }

    /**
     * The save status is composed from the bundle pattern and an
     * injected time (SessionStats' clock idiom), so it tests headlessly with
     * a fixed time. The time follows the UI language's short time pattern:
     * English is 12-hour with AM/PM while Turkish is 24-hour, so one
     * hard-coded pattern could only ever be right for one of them.
     */
    @Test
    void savedStatusUsesTheInjectedTimeInTheUiLanguagesFormat() {
        assertEquals("Saved 12:04\u202fPM",
                MainController.savedStatusFor("Saved {0}", LocalTime.of(12, 4), Locale.ENGLISH));
        assertEquals("Saved 9:07\u202fAM",
                MainController.savedStatusFor("Saved {0}", LocalTime.of(9, 7), Locale.ENGLISH),
                "English short time drops the leading zero of the hour");
        assertEquals("Kaydedildi 12:04",
                MainController.savedStatusFor("Kaydedildi {0}", LocalTime.of(12, 4), Locale.of("tr")),
                "Turkish stays 24-hour regardless of the system locale");
    }

    /**
     * The toolbar toggle is a focus boundary too: the control
     * that had focus may be gone after it, so hiding the toolbar has to hand
     * the caret back to the editor.
     */
    @Test
    void hidingTheToolbarHandsFocusBackToTheEditor() {
        assertTrue(MainController.handOffFocusToEditor(false));
        assertFalse(MainController.handOffFocusToEditor(true));
    }

    /** The shipped English, Turkish and German texts, through the real bundle lookup. */
    @Test
    void savedStatusIsLocalisedThroughTheBundles(@TempDir Path dir) {
        assertEquals("Saved 12:04\u202fPM", savedStatusFromBundle(Language.EN, LocalTime.of(12, 4), dir));
        assertEquals("Kaydedildi 12:04", savedStatusFromBundle(Language.TR, LocalTime.of(12, 4), dir));
        assertEquals("Gespeichert um 12:04", savedStatusFromBundle(Language.DE, LocalTime.of(12, 4), dir),
                "Turkish and German share the 24-hour clock but not the wording");
    }

    /**
     * The menu is generated from the same inventory as the key filter and the
     * toolbar, so every command the platform does not perform itself must
     * appear exactly once: as a leaf, or through the View submenu that selects
     * its value directly. No command may be missing and none may be repeated.
     */
    @Test
    void menuLayoutCoversEveryNonPlatformActionExactlyOnce() {
        var fromMenus = MainMenu.layout().stream()
                .flatMap(spec -> spec.entries().stream())
                .filter(MainMenu.ActionEntry.class::isInstance)
                .map(entry -> ((MainMenu.ActionEntry) entry).action())
                .toList();
        var expected = Arrays.stream(EditorAction.values())
                .filter(action -> !action.handledByPlatform())
                .filter(action -> !MainMenu.submenuBackedActions().contains(action))
                .toList();

        assertEquals(expected.size(), fromMenus.size(),
                "one menu entry per command, so no command is repeated");
        assertEquals(new HashSet<>(expected), new HashSet<>(fromMenus));
    }

    /**
     * The find/replace leaves are pinned as a block so a future Edit-menu edit
     * cannot silently drop one or reorder the flow Find -> Next -> Previous ->
     * Replace.
     */
    @Test
    void editMenuOffersTheFindAndReplaceCommandsInOrder() {
        var edit = MainMenu.layout().stream()
                .filter(spec -> spec.page() == MainMenu.Page.EDIT)
                .findFirst()
                .orElseThrow();
        var findReplace = java.util.EnumSet.of(
                EditorAction.FIND, EditorAction.FIND_NEXT,
                EditorAction.FIND_PREVIOUS, EditorAction.REPLACE);
        var actions = edit.entries().stream()
                .filter(MainMenu.ActionEntry.class::isInstance)
                .map(entry -> ((MainMenu.ActionEntry) entry).action())
                .filter(findReplace::contains)
                .toList();

        assertEquals(List.of(
                EditorAction.FIND,
                EditorAction.FIND_NEXT,
                EditorAction.FIND_PREVIOUS,
                EditorAction.REPLACE), actions);
    }

    /**
     * The submenu-backed set is the one thing the leaf test excludes, so it is
     * pinned here: adding a command to the menu by deleting it from this set
     * would otherwise be invisible.
     */
    @Test
    void theSubmenuBackedActionsAreExactlyTheSixSettingCommands() {
        assertEquals(new HashSet<>(List.of(
                        EditorAction.CYCLE_THEME,
                        EditorAction.TOGGLE_YOLO,
                        EditorAction.CYCLE_TEXT_COLOR,
                        EditorAction.FONT_UP,
                        EditorAction.FONT_DOWN,
                        EditorAction.FONT_RESET)),
                new HashSet<>(MainMenu.submenuBackedActions()));
    }

    /**
     * The View menu mirrors the toolbar's five settings as submenus, in the
     * same order the dropdowns sit in the bar. Sound has no {@code EditorAction}
     * at all, which is precisely why the submenu is generated from the shared
     * settings port rather than from a command.
     */
    @Test
    void viewMenuOffersTheFiveToolbarSettingsAsValueSubmenus() {
        var view = MainMenu.layout().stream()
                .filter(spec -> spec.page() == MainMenu.Page.VIEW)
                .findFirst()
                .orElseThrow();
        var kinds = view.entries().stream()
                .filter(MainMenu.ChoiceEntry.class::isInstance)
                .map(entry -> ((MainMenu.ChoiceEntry) entry).kind())
                .toList();

        assertEquals(List.of(
                MainMenu.ChoiceKind.THEME,
                MainMenu.ChoiceKind.YOLO,
                MainMenu.ChoiceKind.SOUND,
                MainMenu.ChoiceKind.TEXT_COLOR,
                MainMenu.ChoiceKind.FONT_SIZE), kinds);
    }

    /**
     * A resolved submenu checks exactly the live value and its rows select
     * their own value. This is the data half of "the menu is a second view of
     * one state", testable without a toolkit.
     */
    @Test
    void aResolvedChoiceChecksTheCurrentValueAndItsRowsSelectTheirOwn(@TempDir Path dir) {
        var messages = messagesFor(Language.EN, dir);
        var selected = new ArrayList<String>();
        MainMenu.Settings settings = kind -> new MainMenu.Choice<>(
                List.of("Light", "Dark", "Sepia"), "Dark", value -> value, selected::add);

        var resolved = MainMenu.resolve(MainMenu.ChoiceKind.THEME, messages, settings);

        assertEquals("Theme", resolved.title());
        assertEquals(List.of("Light", "Dark", "Sepia"),
                resolved.options().stream().map(MainMenu.ResolvedOption::label).toList());
        assertEquals(List.of(false, true, false),
                resolved.options().stream().map(MainMenu.ResolvedOption::selected).toList());
        resolved.options().get(2).select().run();
        assertEquals(List.of("Sepia"), selected);
    }

    /** A language switch relabels the menus through the bundles, never with blanks. */
    @Test
    void everyMenuTitleAndActionLabelIsNonBlankInEveryLanguage(@TempDir Path dir) {
        for (var language : Language.values()) {
            var messages = messagesFor(language, dir);
            for (var spec : MainMenu.layout()) {
                assertFalse(spec.page().title(messages).isBlank(),
                        () -> "blank menu title in " + language);
                for (var entry : spec.entries()) {
                    switch (entry) {
                        case MainMenu.ActionEntry(var action) -> assertFalse(
                                action.label(messages).isBlank(),
                                () -> "blank label for " + action + " in " + language);
                        case MainMenu.ChoiceEntry(var kind) -> assertFalse(
                                kind.title(messages).isBlank(),
                                () -> "blank submenu title for " + kind + " in " + language);
                    }
                }
            }
        }
    }

    /**
     * About is a dialog view, not a document command: giving it an
     * {@link EditorAction} would force a fake chord into the shortcut card, so
     * Help carries no entries and only its title comes from the bundle.
     */
    @Test
    void helpMenuCarriesNoEditorActionsButALocalisedTitle(@TempDir Path dir) {
        var help = MainMenu.layout().stream()
                .filter(spec -> spec.page() == MainMenu.Page.HELP)
                .findFirst()
                .orElseThrow();

        assertTrue(help.entries().isEmpty());
        assertEquals("Yard\u0131m", help.page().title(messagesFor(Language.TR, dir)));
        assertEquals("Help", help.page().title(messagesFor(Language.EN, dir)));
    }

    private static Messages messagesFor(Language language, Path dir) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("menu-settings-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        return new Messages(localeManager);
    }

    private static String savedStatusFromBundle(Language language, LocalTime savedAt, Path dir) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("settings-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        var messages = new Messages(localeManager);
        return MainController.savedStatusFor(messages.savedStatusTemplate(), savedAt, messages.locale());
    }
}
