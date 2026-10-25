package org.mesutormanli.jtypewriter.locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.ui.theme.TextColor;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
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

    /**
     * The list must follow {@link Theme#next()} from the persisted default
     * (DARK -> SEPIA -> LIGHT), not the declaration order of the enum.
     */
    @Test
    void theWelcomeTextListsTheThemesInCycleOrder() {
        assertEquals("Dark/Sepia/Light", themeOrderIn(messagesFor(Language.EN)));
        assertEquals("Koyu/Sepya/Açık", themeOrderIn(messagesFor(Language.TR)));
        assertEquals("Dunkel/Sepia/Hell", themeOrderIn(messagesFor(Language.DE)));
        assertEquals("Sombre/Sépia/Clair", themeOrderIn(messagesFor(Language.FR)));
        assertEquals("Scuro/Seppia/Chiaro", themeOrderIn(messagesFor(Language.IT)));
        assertEquals("Oscuro/Sepia/Claro", themeOrderIn(messagesFor(Language.ES)));
    }

    /**
     * Regression (PW-04): {@code MessageFormat} treats a lone apostrophe as
     * its quote character, so an unescaped apostrophe in a bundle pattern
     * ("This file's ...") swallowed the rest of the sentence and printed
     * {@code {0}} literally - the charset argument never reached the user in
     * English, French and Italian. The argument-bearing keys are derived from
     * the bundles themselves (every pattern containing {@code {0}}), so a new
     * text with arguments is covered without touching this test. The default
     * locale is Turkish so the dotted/dotless i trap cannot hide a failure.
     */
    @Test
    void everyArgumentPatternFormatsWithoutLeftoverPlaceholdersInEveryLanguage() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            var keys = argumentKeys();
            assertFalse(keys.isEmpty(), "no argument-bearing key found in the bundles");
            assertTrue(keys.contains("encodingMalformedContent"),
                    "the derivation missed the malformed-content text: " + keys);

            var leftover = Pattern.compile("\\{[0-9]+\\}");
            for (var language : Language.values()) {
                var bundle = Messages.bundleFor(language);
                var messages = messagesFor(language);
                for (var key : keys) {
                    var formatted = new MessageFormat(bundle.getString(key), messages.locale())
                            .format(new Object[]{"a", "b", "c", "d", "e", "f", "g"});
                    assertFalse(leftover.matcher(formatted).find(),
                            language + " " + key + " left a placeholder: " + formatted);
                    assertFalse(formatted.isBlank(),
                            language + " " + key + " formatted blank");
                }

                var malformed = messages.encodingMalformedContent("UTF-8");
                assertTrue(malformed.contains("UTF-8"),
                        language + " lost the charset argument: " + malformed);
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    /**
     * Regression (PW-05): {@code MessageFormat.format} used to follow the JVM
     * default locale, so a Turkish UI on an English machine grouped numbers as
     * "1,234". The UI language decides the separators, not the system locale.
     */
    @Test
    void statsFormatUsesTheUiLanguageNumberFormatNotTheSystemLocale() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);

            assertTrue(statsLine(messagesFor(Language.TR)).contains("1.234"),
                    "Turkish groups with a dot: " + statsLine(messagesFor(Language.TR)));
            assertTrue(statsLine(messagesFor(Language.EN)).contains("1,234"),
                    "English groups with a comma: " + statsLine(messagesFor(Language.EN)));
            assertTrue(statsLine(messagesFor(Language.DE)).contains("1.234"),
                    "German groups with a dot: " + statsLine(messagesFor(Language.DE)));

            // French uses a (narrow) no-break space, whose code point varies by
            // JDK; assert only that it is not comma-grouped like the system locale.
            var french = statsLine(messagesFor(Language.FR));
            assertFalse(french.contains("1,234"), "French followed the system locale: " + french);
        } finally {
            Locale.setDefault(previous);
        }
    }

    /**
     * Guard against dead bundle keys (M12/I5).
     *
     * <p>The parity test above only compares the six bundles with each other:
     * a key that no getter ever reads survives in six files forever, and it
     * cannot see the opposite mistake either - a getter added without a bundle
     * key compiles fine and only fails at runtime with a
     * {@link java.util.MissingResourceException}. Comparing the keys
     * {@code Messages} actually references against the English base bundle
     * makes both directions fail the build.</p>
     */
    @Test
    void everyKeyMessagesReferencesExistsInTheBundlesAndNoBundleKeyIsUnreferenced() {
        var referenced = referencedKeys();
        var bundled = keysOf(Messages.bundleFor(Language.EN));

        var missingFromBundle = new TreeSet<>(referenced);
        missingFromBundle.removeAll(bundled);
        var unreferencedInCode = new TreeSet<>(bundled);
        unreferencedInCode.removeAll(referenced);

        assertEquals(referenced, bundled,
                "Messages.java and messages.properties disagree;"
                        + " referenced but missing from the bundle: " + missingFromBundle
                        + "; present in the bundle but referenced nowhere (dead key): " + unreferencedInCode);
    }

    /**
     * Every bundle key {@code Messages.java} references, via the two call
     * shapes the class uses: {@code bundle.getString("<key>")} and
     * {@code format("<key>", ...)}. The source is read the same way
     * {@code DialogSizingTest} reads its dialog sources, relative to the
     * project directory.
     */
    private static Set<String> referencedKeys() {
        var path = Path.of("src", "main", "java", "org", "mesutormanli", "jtypewriter", "locale", "Messages.java");
        String source;
        try {
            source = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path.toAbsolutePath(), e);
        }

        var keys = new TreeSet<String>();
        for (var pattern : new Pattern[]{
                Pattern.compile("bundle\\.getString\\(\\s*\"([^\"]+)\""),
                Pattern.compile("\\bformat\\(\\s*\"([^\"]+)\"")}) {
            var matcher = pattern.matcher(source);
            while (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }
        return keys;
    }

    private static String statsLine(Messages messages) {
        return messages.statsFormat(1234, 5678, 4321, 90, 12345L, "2m 5s", 210);
    }

    /** Every bundle key whose raw value is a {@code MessageFormat} pattern with an argument. */
    private static Set<String> argumentKeys() {
        var keys = new TreeSet<String>();
        for (var language : Language.values()) {
            var bundle = Messages.bundleFor(language);
            for (var key : bundle.keySet()) {
                if (bundle.getString(key).contains("{0}")) {
                    keys.add(key);
                }
            }
        }
        return keys;
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