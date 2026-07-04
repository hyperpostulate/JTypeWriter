package org.mesutormanli.jtypewriter.locale;

import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Component
public class Messages {

    private final LocaleManager localeManager;
    private ResourceBundle bundle;

    public Messages(LocaleManager localeManager) {
        this.localeManager = localeManager;
        reload();
        localeManager.addListener(lang -> reload());
    }

    private void reload() {
        try {
            bundle = ResourceBundle.getBundle("messages",
                    Locale.forLanguageTag(localeManager.getCurrent().name().toLowerCase()));
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
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
}
