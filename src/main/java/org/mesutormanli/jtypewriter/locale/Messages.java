package org.mesutormanli.jtypewriter.locale;


import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {

    /**
     * Bundle lookup without the "fall back to the JVM default locale" rule of
     * {@link ResourceBundle#getBundle(String, Locale)}. English lives in the
     * base bundle ({@code messages.properties}, there is no
     * {@code messages_en.properties}), so that rule resolved English to the
     * <em>system</em> language first: on a Turkish, German, French, Italian or
     * Spanish machine the "English" UI showed the system language instead. The
     * plain candidate chain of the requested language is what we want here.
     */
    private static final ResourceBundle.Control NO_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private final LocaleManager localeManager;
    private ResourceBundle bundle;

    public Messages(LocaleManager localeManager) {
        this.localeManager = localeManager;
        reload();
        localeManager.addListener(lang -> reload());
    }

    private void reload() {
        try {
            bundle = bundleFor(localeManager.getCurrent());
        } catch (MissingResourceException e) {
            bundle = baseBundle();
        }
    }

    /** The bundle of a UI language, independent of the system locale (see {@link #NO_FALLBACK}). */
    static ResourceBundle bundleFor(Language language) {
        // Locale.ROOT matters: on a Turkish locale "IT".toLowerCase() is "ıt"
        // (the dotted/dotless i trap), and that dead tag fell back to the
        // English base bundle - the Italian UI never loaded on Turkish machines
        return ResourceBundle.getBundle("messages",
                Locale.forLanguageTag(language.name().toLowerCase(Locale.ROOT)), NO_FALLBACK);
    }

    /** The base bundle, which holds the English texts. */
    static ResourceBundle baseBundle() {
        return ResourceBundle.getBundle("messages", Locale.ROOT, NO_FALLBACK);
    }

    private Language l() {
        return localeManager.getCurrent();
    }

    /**
     * The locale of the selected UI language, deliberately not
     * {@link Locale#getDefault()}. {@code MessageFormat} picks its number,
     * date and quote rules from the locale it is given, so formatting through
     * the system locale showed "1,234" to a Turkish user on an English
     * machine. Uses {@link Locale#of} (not {@code forLanguageTag}) to avoid
     * the language-tag casing pitfalls; {@code Locale.ROOT} guards the
     * Turkish dotted/dotless i in the language code.
     */
    public Locale locale() {
        return Locale.of(l().code().toLowerCase(Locale.ROOT));
    }

    /** Formats a bundle pattern with the UI locale (see {@link #locale()}). */
    private String format(String key, Object... arguments) {
        return new MessageFormat(bundle.getString(key), locale()).format(arguments);
    }

    public String toolbarOpen() {
        return bundle.getString("toolbarOpen");
    }

    public String toolbarSave() {
        return bundle.getString("toolbarSave");
    }

    public String toolbarTheme() {
        return bundle.getString("toolbarTheme");
    }

    public String toolbarYolo() {
        return bundle.getString("toolbarYolo");
    }

    public String toolbarSound() {
        return bundle.getString("toolbarSound");
    }

    public String toolbarColor() {
        return bundle.getString("toolbarColor");
    }

    /** Caption of the font-size dropdown: {@code "Size: 13"}, tooltip {@code "Size (Ctrl++ / ...)"}. */
    public String toolbarFontSize() {
        return bundle.getString("toolbarFontSize");
    }

    /** Caption of the language dropdown: {@code "Language: Deutsch"}. */
    public String toolbarLanguage() {
        return bundle.getString("toolbarLanguage");
    }

    public String labelOn() {
        return bundle.getString("labelOn");
    }

    public String labelOff() {
        return bundle.getString("labelOff");
    }

    public String labelThemeLight() {
        return bundle.getString("labelThemeLight");
    }

    public String labelThemeDark() {
        return bundle.getString("labelThemeDark");
    }

    public String labelThemeSepia() {
        return bundle.getString("labelThemeSepia");
    }

    public String colorDefault() {
        return bundle.getString("colorDefault");
    }

    public String colorSage() {
        return bundle.getString("colorSage");
    }

    public String colorLavender() {
        return bundle.getString("colorLavender");
    }

    public String colorSand() {
        return bundle.getString("colorSand");
    }

    public String colorSteel() {
        return bundle.getString("colorSteel");
    }

    public String colorRose() {
        return bundle.getString("colorRose");
    }

    public String welcomeTitle() {
        return bundle.getString("welcomeTitle");
    }

    /** The one-line description of the app shown under the title in both helper windows. */
    public String appTagline() {
        return bundle.getString("appTagline");
    }

    public String welcomeWindowTitle() {
        return bundle.getString("welcomeWindowTitle");
    }

    public String welcomeButton() {
        return bundle.getString("welcomeButton");
    }

    public String welcomeShortcuts() {
        return bundle.getString("welcomeShortcuts");
    }

    public String fileOpenTitle() {
        return bundle.getString("fileOpenTitle");
    }

    public String fileSaveTitle() {
        return bundle.getString("fileSaveTitle");
    }

    public String fileTextFiles() {
        return bundle.getString("fileTextFiles");
    }

    public String fileAllFiles() {
        return bundle.getString("fileAllFiles");
    }

    public String fileTextFile() {
        return bundle.getString("fileTextFile");
    }

    public String unsavedTitle() {
        return bundle.getString("unsavedTitle");
    }

    public String unsavedHeader() {
        return bundle.getString("unsavedHeader");
    }

    public String unsavedContent() {
        return bundle.getString("unsavedContent");
    }

    public String unsavedSave() {
        return bundle.getString("unsavedSave");
    }

    public String unsavedDiscard() {
        return bundle.getString("unsavedDiscard");
    }

    public String unsavedCancel() {
        return bundle.getString("unsavedCancel");
    }

    public String overwriteTitle() {
        return bundle.getString("overwriteTitle");
    }

    public String overwriteHeader() {
        return bundle.getString("overwriteHeader");
    }

    public String overwriteContent(String fileName) {
        return format("overwriteContent", fileName);
    }

    public String overwriteConfirm() {
        return bundle.getString("overwriteConfirm");
    }

    public String overwriteCancel() {
        return bundle.getString("overwriteCancel");
    }

    public String errorTitle() {
        return bundle.getString("errorTitle");
    }

    public String errorOpen() {
        return bundle.getString("errorOpen");
    }

    public String errorSave() {
        return bundle.getString("errorSave");
    }

    public String appName() {
        return bundle.getString("appName");
    }

    /** Placeholder document name shown in the window title for a never-saved buffer. */
    public String windowUntitled() {
        return bundle.getString("windowUntitled");
    }

    public String menuCut() {
        return bundle.getString("menuCut");
    }

    public String menuCopy() {
        return bundle.getString("menuCopy");
    }

    public String menuPaste() {
        return bundle.getString("menuPaste");
    }

    public String menuSelectAll() {
        return bundle.getString("menuSelectAll");
    }

    public String toolbarUndo() {
        return bundle.getString("toolbarUndo");
    }

    public String toolbarRedo() {
        return bundle.getString("toolbarRedo");
    }

    public String toolbarAbout() {
        return bundle.getString("toolbarAbout");
    }

    public String aboutWindowTitle() {
        return bundle.getString("aboutWindowTitle");
    }

    public String aboutClose() {
        return bundle.getString("aboutClose");
    }

    /** The About line naming the running version, e.g. {@code "Version 1.0.0"}. */
    public String aboutVersion(String version) {
        return format("aboutVersion", version);
    }

    public String aboutCopyright() {
        return bundle.getString("aboutCopyright");
    }

    public String aboutLicense() {
        return bundle.getString("aboutLicense");
    }

    public String aboutWarranty() {
        return bundle.getString("aboutWarranty");
    }

    /**
     * The stats line. Numbers follow the UI language's conventions, not the
     * system locale's: a Turkish UI on an English machine shows "1.234".
     */
    public String statsFormat(int words, int chars, int charsNoSpace, int lines, long keystrokes, String duration, int wpm) {
        return format("statsFormat", words, chars, charsNoSpace, lines, keystrokes, duration, wpm);
    }

    public String recoveryTitle() {
        return bundle.getString("recoveryTitle");
    }

    public String recoveryHeader() {
        return bundle.getString("recoveryHeader");
    }

    public String recoveryContent(String savedAt) {
        return format("recoveryContent", savedAt);
    }

    public String recoveryYes() {
        return bundle.getString("recoveryYes");
    }

    public String recoveryNo() {
        return bundle.getString("recoveryNo");
    }

    public String recoveryLater() {
        return bundle.getString("recoveryLater");
    }

    public String encodingTitle() {
        return bundle.getString("encodingTitle");
    }

    public String encodingHeader() {
        return bundle.getString("encodingHeader");
    }

    public String encodingContent() {
        return bundle.getString("encodingContent");
    }

    public String encodingTurkish() {
        return bundle.getString("encodingTurkish");
    }

    public String encodingUtf8() {
        return bundle.getString("encodingUtf8");
    }

    public String encodingCancel() {
        return bundle.getString("encodingCancel");
    }

    public String encodingUpgradeTitle() {
        return bundle.getString("encodingUpgradeTitle");
    }

    public String encodingUpgradeHeader() {
        return bundle.getString("encodingUpgradeHeader");
    }

    public String encodingUpgradeContent(String charsetName) {
        return format("encodingUpgradeContent", charsetName);
    }

    public String encodingUpgradeSave() {
        return bundle.getString("encodingUpgradeSave");
    }

    public String encodingUpgradeCancel() {
        return bundle.getString("encodingUpgradeCancel");
    }

    public String encodingMalformedTitle() {
        return bundle.getString("encodingMalformedTitle");
    }

    public String encodingMalformedHeader() {
        return bundle.getString("encodingMalformedHeader");
    }

    public String encodingMalformedContent(String charsetName) {
        return format("encodingMalformedContent", charsetName);
    }

    public String encodingMalformedOpen() {
        return bundle.getString("encodingMalformedOpen");
    }

    public String encodingMalformedCancel() {
        return bundle.getString("encodingMalformedCancel");
    }

    public String errorLogged(String logDir) {
        return format("errorLogged", logDir);
    }

    public String singleInstanceTitle() {
        return bundle.getString("singleInstanceTitle");
    }

    public String singleInstanceHeader() {
        return bundle.getString("singleInstanceHeader");
    }

    public String singleInstanceContent() {
        return bundle.getString("singleInstanceContent");
    }

    /**
     * RW-19: raw {@code MessageFormat} pattern for the save status in the
     * stats bar, {@code {0}} being the localised save time. Returned as the
     * pattern (not formatted here) so composition stays in the pure, headless
     * {@code MainController.savedStatusFor}.
     */
    public String savedStatusTemplate() {
        return bundle.getString("savedStatus");
    }

    /**
     * V4/PW-27: the standing status-bar warning while the autosave safety net
     * cannot write a recovery copy. Deliberately phrased as an instruction, not
     * as a status: the only thing that helps the user right now is a normal
     * save.
     */
    public String autosaveWarning() {
        return bundle.getString("autosaveWarning");
    }

    /** V4/PW-27: header of the one-off dialog shown on the first failure. */
    public String autosaveFailedHeader() {
        return bundle.getString("autosaveFailedHeader");
    }

    /** V4/PW-27: body of the one-off dialog shown on the first failure. */
    public String autosaveFailedContent() {
        return bundle.getString("autosaveFailedContent");
    }

    /**
     * V17/PW-28: the standing status-bar warning shown when a draft that should
     * have been deleted could not be deleted, so a stale copy would be offered
     * for recovery again.
     */
    public String staleDraftWarning() {
        return bundle.getString("staleDraftWarning");
    }
}
