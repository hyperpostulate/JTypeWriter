package org.mesutormanli.jtypewriter.ui.component;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;
import org.springframework.stereotype.Component;

@Component
public class ToolbarView extends HBox {

    private final FileService fileService;
    private final ThemeManager themeManager;
    private final TypewriterSound typewriterSound;
    private final Messages messages;
    private final LocaleManager localeManager;
    private EditorArea editorArea;

    private final Button openBtn;
    private final Button saveBtn;
    private final Button themeBtn;
    private final Button focusBtn;
    private final Button yoloBtn;
    private final Button soundBtn;
    private final Button langBtn;
    private final Label themeLabel;
    private final Label focusLabel;
    private final Label yoloLabel;
    private final Label soundLabel;
    private final Label fontSizeLabel;

    private Runnable onFontSizeChange;

    private int focusState; // 0=off, 1=line, 2=paragraph
    private boolean yoloState;
    private boolean soundState = true;

    public ToolbarView(FileService fileService, ThemeManager themeManager,
                       TypewriterSound typewriterSound, Messages messages,
                       LocaleManager localeManager) {
        this.fileService = fileService;
        this.themeManager = themeManager;
        this.typewriterSound = typewriterSound;
        this.messages = messages;
        this.localeManager = localeManager;
        this.editorArea = null;

        getStyleClass().add("toolbar");
        setPadding(new Insets(8, 12, 8, 12));
        setSpacing(8);

        openBtn = createButton(messages.toolbarOpen());
        saveBtn = createButton(messages.toolbarSave());
        themeBtn = createButton(messages.toolbarTheme());
        focusBtn = createButton(messages.toolbarFocus());
        yoloBtn = createButton(messages.toolbarYolo());
        soundBtn = createButton(messages.toolbarSound());
        langBtn = new Button(messages.langCode());
        langBtn.getStyleClass().add("toolbar-lang");
        langBtn.setFocusTraversable(false);

        themeLabel = new Label(themeManager.getCurrentTheme().getDisplayName(localeManager.getCurrent()));
        themeLabel.getStyleClass().add("toolbar-label");
        focusLabel = new Label(messages.labelFocusOff());
        focusLabel.getStyleClass().add("toolbar-label");
        yoloLabel = new Label(messages.yoloModeOff());
        yoloLabel.getStyleClass().add("toolbar-label");
        soundLabel = new Label(messages.soundOn());
        soundLabel.getStyleClass().add("toolbar-label");

        var fontSizeUpBtn = createButton(messages.toolbarFontUp());
        var fontSizeDownBtn = createButton(messages.toolbarFontDown());
        fontSizeLabel = new Label("16");
        fontSizeLabel.getStyleClass().add("toolbar-label");

        openBtn.setOnAction(e -> fileService.openFile(null));
        saveBtn.setOnAction(e -> fileService.saveFile(null, editorArea != null ? editorArea.getText() : ""));

        themeBtn.setOnAction(e -> cycleTheme());
        focusBtn.setOnAction(e -> cycleFocus());
        yoloBtn.setOnAction(e -> toggleYolo());
        soundBtn.setOnAction(e -> toggleSound());
        langBtn.setOnAction(e -> toggleLanguage());
        fontSizeUpBtn.setOnAction(e -> changeFontSize(1));
        fontSizeDownBtn.setOnAction(e -> changeFontSize(-1));

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                openBtn, saveBtn,
                sep(),
                themeBtn, themeLabel,
                sep(),
                focusBtn, focusLabel,
                sep(),
                yoloBtn, yoloLabel,
                sep(),
                soundBtn, soundLabel,
                sep(),
                fontSizeDownBtn, fontSizeLabel, fontSizeUpBtn,
                spacer,
                langBtn
        );

        localeManager.addListener(lang -> refreshTexts());
    }

    public void setEditorArea(EditorArea editorArea) {
        this.editorArea = editorArea;
    }

    public void setOnFontSizeChange(Runnable callback) {
        this.onFontSizeChange = callback;
    }

    public int getCurrentFontSize() {
        return Integer.parseInt(fontSizeLabel.getText());
    }

    private void refreshTexts() {
        var lang = localeManager.getCurrent();
        openBtn.setText(messages.toolbarOpen());
        saveBtn.setText(messages.toolbarSave());
        themeBtn.setText(messages.toolbarTheme());
        focusBtn.setText(messages.toolbarFocus());
        yoloBtn.setText(messages.toolbarYolo());
        soundBtn.setText(messages.toolbarSound());
        langBtn.setText(messages.langCode());
        themeLabel.setText(themeManager.getCurrentTheme().getDisplayName(lang));
        updateFocusLabel();
        updateYoloLabel();
        updateSoundLabel();
    }

    private void cycleTheme() {
        var scene = getScene();
        if (scene != null) {
            var theme = themeManager.cycleTheme(scene);
            themeLabel.setText(theme.getDisplayName(localeManager.getCurrent()));
        }
    }

    private void cycleFocus() {
        if (editorArea == null) return;
        focusState = (focusState + 1) % 3;
        switch (focusState) {
            case 0 -> {
                editorArea.setLineFocusMode(false);
                editorArea.setParagraphFocusMode(false);
            }
            case 1 -> {
                editorArea.setLineFocusMode(true);
                editorArea.setParagraphFocusMode(false);
            }
            case 2 -> {
                editorArea.setLineFocusMode(false);
                editorArea.setParagraphFocusMode(true);
            }
        }
        updateFocusLabel();
    }

    private void toggleYolo() {
        if (editorArea == null) return;
        yoloState = !yoloState;
        editorArea.setYoloMode(yoloState);
        updateYoloLabel();
    }

    private void toggleSound() {
        soundState = !soundState;
        typewriterSound.setEnabled(soundState);
        updateSoundLabel();
    }

    private void toggleLanguage() {
        localeManager.toggle();
    }

    private void updateFocusLabel() {
        focusLabel.setText(switch ((int) focusState) {
            case 0 -> messages.labelFocusOff();
            case 1 -> messages.labelFocusLine();
            case 2 -> messages.labelFocusParagraph();
            default -> "";
        });
    }

    private void updateYoloLabel() {
        yoloLabel.setText(yoloState ? messages.yoloModeOn() : messages.yoloModeOff());
    }

    private void updateSoundLabel() {
        soundLabel.setText(soundState ? messages.soundOn() : messages.soundOff());
    }

    public void changeFontSize(int delta) {
        var current = getCurrentFontSize();
        var newSize = Math.max(10, Math.min(48, current + delta));
        fontSizeLabel.setText(String.valueOf(newSize));
        if (onFontSizeChange != null) {
            onFontSizeChange.run();
        }
    }

    private Button createButton(String text) {
        var btn = new Button(text);
        btn.getStyleClass().add("toolbar-button");
        btn.setFocusTraversable(false);
        return btn;
    }

    private Label sep() {
        var s = new Label("│");
        s.getStyleClass().add("toolbar-separator");
        return s;
    }
}
