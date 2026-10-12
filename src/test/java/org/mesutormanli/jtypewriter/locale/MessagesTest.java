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
        var reference = keysOf(ResourceBundle.getBundle("messages", Locale.ENGLISH));

        for (var language : Language.values()) {
            var bundle = ResourceBundle.getBundle("messages",
                    Locale.forLanguageTag(language.code().toLowerCase()));
            assertEquals(reference, keysOf(bundle), "bundle of " + language + " is out of sync");
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