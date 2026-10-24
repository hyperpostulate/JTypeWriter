package org.mesutormanli.jtypewriter.locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.ui.theme.TextColor;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.nio.file.Path;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * i18n contract: the language bundles must stay key-complete, and display
 * names must come from the bundles. Theme and colour names used to be
 * hard-coded in code ({@code language == EN ? "Dark" : "Koyu"}), which left
 * German, French, Italian and Spanish users with Turkish theme names.
 */
class MessagesTest {

    @TempDir
    Path dir;

    @Test
    void everyBundleTranslatesTheSameKeys() {
        var reference = keysOf(Messages.bundleFor(Language.EN));

        for (var language : Language.values()) {
            assertEquals(reference, keysOf(Messages.bundleFor(language)),
                    "bundle of " + language + " is out of sync");
        }
    }

    /**
     * Regression: the English texts live in the base bundle (there is no
     * {@code messages_en.properties}), and the default-locale fallback of
     * {@link ResourceBundle#getBundle(String, Locale)} used to resolve
     * {@code en} to the <em>system</em> language - "English" showed Turkish on
     * a Turkish machine (and German on a German one, ...). Turkish also hides
     * the dotted/dotless i trap: {@code "IT".toLowerCase()} is {@code "ıt"}
     * there, a dead language tag that fell back to the English texts, so
     * Italian never loaded on Turkish machines. Every language must resolve to
     * its own texts no matter what the system locale is.
     */
    @Test
    void everyLanguageResolvesToItsOwnTextsRegardlessOfTheSystemLocale() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("Dark", messagesFor(Language.EN).labelThemeDark());
            assertEquals("Koyu", messagesFor(Language.TR).labelThemeDark());
            assertEquals("Dunkel", messagesFor(Language.DE).labelThemeDark());
            assertEquals("Sombre", messagesFor(Language.FR).labelThemeDark());
            assertEquals("Scuro", messagesFor(Language.IT).labelThemeDark());
            assertEquals("Oscuro", messagesFor(Language.ES).labelThemeDark());

            Locale.setDefault(Locale.forLanguageTag("de-DE"));
            assertEquals("Dark", messagesFor(Language.EN).labelThemeDark());
            assertEquals("Scuro", messagesFor(Language.IT).labelThemeDark());
            assertEquals("Koyu", messagesFor(Language.TR).labelThemeDark());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void themeNamesAreLocalisedForEveryLanguage() {
        assertEquals("Dark", Theme.DARK.getDisplayName(messagesFor(Language.EN)));
        assertEquals("Koyu", Theme.DARK.getDisplayName(messagesFor(Language.TR)));
        assertEquals("Dunkel", Theme.DARK.getDisplayName(messagesFor(Language.DE)));
        assertEquals("Sombre", Theme.DARK.getDisplayName(messagesFor(Language.FR)));
        assertEquals("Scuro", Theme.DARK.getDisplayName(messagesFor(Language.IT)));
        assertEquals("Oscuro", Theme.DARK.getDisplayName(messagesFor(Language.ES)));
    }

    @Test
    void colourNamesAreLocalisedForEveryLanguage() {
        assertEquals("Sage", TextColor.SAGE.getDisplayName(messagesFor(Language.EN)));
        assertEquals("Adaçayı", TextColor.SAGE.getDisplayName(messagesFor(Language.TR)));
        assertEquals("Salbei", TextColor.SAGE.getDisplayName(messagesFor(Language.DE)));
        assertEquals("Sauge", TextColor.SAGE.getDisplayName(messagesFor(Language.FR)));
        assertEquals("Salvia", TextColor.SAGE.getDisplayName(messagesFor(Language.IT)));
        assertEquals("Salvia", TextColor.SAGE.getDisplayName(messagesFor(Language.ES)));
    }

    @Test
    void switchingTheLanguageReloadsTheTexts() {
        var localeManager = new LocaleManager(new AppSettings(dir.resolve("settings.properties")));
        localeManager.setLanguage(Language.EN);
        var messages = new Messages(localeManager);

        localeManager.setLanguage(Language.TR);

        assertEquals("Açık", messages.labelThemeLight());
    }

    /**
     * Regression: these bundles shipped mistranslations that a key-equality
     * test cannot see. German "Redo" was "Wiederherstellen" ("restore") while
     * the shortcut list said "Wiederholen", and the recovery dialog offered
     * "Delete"/"Remove" as the way to <em>keep</em> a draft for later in
     * Turkish, French and Italian - a wording that invites data loss.
     */
    @Test
    void translationsThatCouldCostDataArePinned() {
        assertEquals("Wiederholen", messagesFor(Language.DE).toolbarRedo());
        assertEquals("Vazgeç", messagesFor(Language.TR).recoveryNo());
        assertEquals("Ignorer", messagesFor(Language.FR).recoveryNo());
        assertEquals("Ignora", messagesFor(Language.IT).recoveryNo());
    }

    @Test
    void theTurkishWelcomeTextNoLongerMixesInEnglish() {
        var shortcuts = messagesFor(Language.TR).welcomeShortcuts();

        assertTrue(shortcuts.contains("Araç çubuğu aç/kapa"), shortcuts);
        assertTrue(shortcuts.contains("Yazı tipi büyüt"), shortcuts);
        assertFalse(shortcuts.contains("Toolbar"), shortcuts);
        assertFalse(shortcuts.contains("Font"), shortcuts);
        assertFalse(shortcuts.contains("Dark"), shortcuts);
    }

    /** The list must follow {@link Theme#next()} (LIGHT, DARK, SEPIA), not a per-bundle guess. */
    @Test
    void theWelcomeTextListsTheThemesInCycleOrder() {
        assertEquals("Light/Dark/Sepia", themeOrderIn(messagesFor(Language.EN)));
        assertEquals("Açık/Koyu/Sepya", themeOrderIn(messagesFor(Language.TR)));
        assertEquals("Hell/Dunkel/Sepia", themeOrderIn(messagesFor(Language.DE)));
        assertEquals("Clair/Sombre/Sépia", themeOrderIn(messagesFor(Language.FR)));
        assertEquals("Chiaro/Scuro/Seppia", themeOrderIn(messagesFor(Language.IT)));
        assertEquals("Claro/Oscuro/Sepia", themeOrderIn(messagesFor(Language.ES)));
    }

    private String themeOrderIn(Messages messages) {
        var shortcuts = messages.welcomeShortcuts();
        var open = shortcuts.indexOf('(');
        var close = shortcuts.indexOf(')', open);
        assertTrue(open > 0 && close > open, "no theme list in: " + shortcuts);
        return shortcuts.substring(open + 1, close);
    }

    private Messages messagesFor(Language language) {
        var localeManager = new LocaleManager(
                new AppSettings(dir.resolve("settings-" + language.code() + ".properties")));
        localeManager.setLanguage(language);
        return new Messages(localeManager);
    }

    private static Set<String> keysOf(ResourceBundle bundle) {
        return bundle.keySet().stream().collect(Collectors.toCollection(TreeSet::new));
    }
}