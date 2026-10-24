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

    public String langCode() {
        return l().code();
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

    public String toolbarFontUp() {
        return bundle.getString("toolbarFontUp");
    }

    public String toolbarFontDown() {
        return bundle.getString("toolbarFontDown");
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

    public String yoloModeOn() {
        return bundle.getString("yoloModeOn");
    }

    public String yoloModeOff() {
        return bundle.getString("yoloModeOff");
    }

    public String soundOn() {
        return bundle.getString("soundOn");
    }

    public String soundOff() {
        return bundle.getString("soundOff");
    }

    public String welcomeTitle() {
        return bundle.getString("welcomeTitle");
    }

    public String welcomeWindowTitle() {
        return bundle.getString("welcomeWindowTitle");
    }

    public String welcomeSubtitle() {
        return bundle.getString("welcomeSubtitle");
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
        return MessageFormat.format(bundle.getString("overwriteContent"), fileName);
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

    public String emptyFile() {
        return bundle.getString("emptyFile");
    }

    public String toolbarAbout() {
        return bundle.getString("toolbarAbout");
    }

    public String aboutWindowTitle() {
        return bundle.getString("aboutWindowTitle");
    }

    public String aboutDescription() {
        return bundle.getString("aboutDescription");
    }

    public String aboutAuthor() {
        return bundle.getString("aboutAuthor");
    }

    public String aboutClose() {
        return bundle.getString("aboutClose");
    }

    public String statsFormat(int words, int chars, int charsNoSpace, int lines, long keystrokes, String duration, int wpm) {
        return MessageFormat.format(bundle.getString("statsFormat"),
                words, chars, charsNoSpace, lines, keystrokes, duration, wpm);
    }

    public String recoveryTitle() {
        return bundle.getString("recoveryTitle");
    }

    public String recoveryHeader() {
        return bundle.getString("recoveryHeader");
    }

    public String recoveryContent(String savedAt) {
        return MessageFormat.format(bundle.getString("recoveryContent"), savedAt);
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
        return MessageFormat.format(bundle.getString("encodingUpgradeContent"), charsetName);
    }

    public String encodingUpgradeSave() {
        return bundle.getString("encodingUpgradeSave");
    }

    public String encodingUpgradeCancel() {
        return bundle.getString("encodingUpgradeCancel");
    }

    public String errorLogged(String logDir) {
        return MessageFormat.format(bundle.getString("errorLogged"), logDir);
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
}
