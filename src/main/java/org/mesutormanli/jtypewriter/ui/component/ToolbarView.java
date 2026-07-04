package org.mesutormanli.jtypewriter.ui.component;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.AboutDialog;
import org.mesutormanli.jtypewriter.ui.theme.FontSizeManager;
import org.mesutormanli.jtypewriter.ui.theme.TextColorManager;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;
import org.springframework.stereotype.Component;

@Component
public class ToolbarView extends HBox {

    private final FileService fileService;
    private final ThemeManager themeManager;
    private final TextColorManager textColorManager;
    private final FontSizeManager fontSizeManager;
    private final TypewriterSound typewriterSound;
    private final Messages messages;
    private final LocaleManager localeManager;
    private final AboutDialog aboutDialog;
    private final Button openBtn;
    private final Button saveBtn;
    private final Button undoBtn;
    private final Button redoBtn;
    private final Button themeBtn;
    private final Button yoloBtn;
    private final Button soundBtn;
    private final Button langBtn;
    private final Button aboutBtn;
    private final Label themeLabel;
    private final Label yoloLabel;
    private final Label soundLabel;
    private final Button colorBtn;
    private final Label colorLabel;
    private final Label fontSizeLabel;
    private EditorArea editorArea;
    private Runnable onStyleChange;
    private Stage stage;

    private boolean yoloState;
    private boolean soundState = true;

    public ToolbarView(FileService fileService, ThemeManager themeManager,
                       TextColorManager textColorManager, FontSizeManager fontSizeManager,
                       TypewriterSound typewriterSound, Messages messages,
                       LocaleManager localeManager, AboutDialog aboutDialog) {
        this.fileService = fileService;
        this.themeManager = themeManager;
        this.textColorManager = textColorManager;
        this.fontSizeManager = fontSizeManager;
        this.typewriterSound = typewriterSound;
        this.messages = messages;
        this.localeManager = localeManager;
        this.aboutDialog = aboutDialog;
        this.editorArea = null;

        getStyleClass().add("toolbar");
        setPadding(new Insets(8, 12, 8, 12));
        setSpacing(8);

        openBtn = createButton(messages.toolbarOpen());
        saveBtn = createButton(messages.toolbarSave());
        undoBtn = createButton(messages.toolbarUndo());
        redoBtn = createButton(messages.toolbarRedo());
        themeBtn = createButton(messages.toolbarTheme());
        yoloBtn = createButton(messages.toolbarYolo());
        soundBtn = createButton(messages.toolbarSound());
        colorBtn = createButton(messages.toolbarColor());
        colorLabel = new Label(textColorManager.getCurrentDisplayName());
        colorLabel.getStyleClass().add("toolbar-label");
        langBtn = new Button(messages.langCode());
        langBtn.getStyleClass().add("toolbar-lang");
        langBtn.setFocusTraversable(false);

        aboutBtn = createButton(messages.toolbarAbout());

        themeLabel = new Label(themeManager.getCurrentTheme().getDisplayName(localeManager.getCurrent()));
        themeLabel.getStyleClass().add("toolbar-label");
        yoloLabel = new Label(messages.yoloModeOff());
        yoloLabel.getStyleClass().add("toolbar-label");
        soundLabel = new Label(messages.soundOn());
        soundLabel.getStyleClass().add("toolbar-label");

        var fontSizeUpBtn = createButton(messages.toolbarFontUp());
        var fontSizeDownBtn = createButton(messages.toolbarFontDown());
        fontSizeLabel = new Label(String.valueOf(fontSizeManager.getCurrentSize()));
        fontSizeLabel.getStyleClass().add("toolbar-label");

        openBtn.setOnAction(e -> {
            fileService.openFile(stage).ifPresent(path -> {
                editorArea.setText(fileService.getCurrentContent());
            });
        });
        saveBtn.setOnAction(e -> {
            if (editorArea != null) {
                fileService.saveFile(stage, editorArea.getText());
            }
        });

        undoBtn.setOnAction(e -> {
            if (editorArea != null) editorArea.undo();
        });
        redoBtn.setOnAction(e -> {
            if (editorArea != null) editorArea.redo();
        });

        themeBtn.setOnAction(e -> cycleTheme());
        yoloBtn.setOnAction(e -> toggleYolo());
        soundBtn.setOnAction(e -> toggleSound());
        colorBtn.setOnAction(e -> cycleTextColor());
        aboutBtn.setOnAction(e -> {
            if (stage != null) aboutDialog.show(stage);
        });
        langBtn.setOnAction(e -> toggleLanguage());
        fontSizeUpBtn.setOnAction(e -> changeFontSize(1));
        fontSizeDownBtn.setOnAction(e -> changeFontSize(-1));

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                openBtn, saveBtn,
                sep(),
                undoBtn, redoBtn,
                sep(),
                themeBtn, themeLabel,
                sep(),
                yoloBtn, yoloLabel,
                sep(),
                soundBtn, soundLabel,
                sep(),
                colorBtn, colorLabel,
                sep(),
                fontSizeDownBtn, fontSizeLabel, fontSizeUpBtn,
                spacer,
                aboutBtn,
                langBtn
        );

        localeManager.addListener(lang -> refreshTexts());
    }

    public void setEditorArea(EditorArea editorArea) {
        this.editorArea = editorArea;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnStyleChange(Runnable callback) {
        this.onStyleChange = callback;
    }

    public int getCurrentFontSize() {
        return Integer.parseInt(fontSizeLabel.getText());
    }

    private void refreshTexts() {
        var lang = localeManager.getCurrent();
        openBtn.setText(messages.toolbarOpen());
        saveBtn.setText(messages.toolbarSave());
        undoBtn.setText(messages.toolbarUndo());
        redoBtn.setText(messages.toolbarRedo());
        themeBtn.setText(messages.toolbarTheme());
        yoloBtn.setText(messages.toolbarYolo());
        soundBtn.setText(messages.toolbarSound());
        colorBtn.setText(messages.toolbarColor());
        colorLabel.setText(textColorManager.getCurrentDisplayName());
        aboutBtn.setText(messages.toolbarAbout());
        langBtn.setText(messages.langCode());
        themeLabel.setText(themeManager.getCurrentTheme().getDisplayName(lang));
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

    private void updateYoloLabel() {
        yoloLabel.setText(yoloState ? messages.yoloModeOn() : messages.yoloModeOff());
    }

    private void updateSoundLabel() {
        soundLabel.setText(soundState ? messages.soundOn() : messages.soundOff());
    }

    public void changeFontSize(int delta) {
        fontSizeManager.changeSize(delta);
        fontSizeLabel.setText(String.valueOf(fontSizeManager.getCurrentSize()));
        notifyStyleChange();
    }

    public String getCurrentTextCssColor() {
        return textColorManager.getCurrentCssColor();
    }

    public void cycleTextColor() {
        textColorManager.cycle();
        colorLabel.setText(textColorManager.getCurrentDisplayName());
        notifyStyleChange();
    }

    private void notifyStyleChange() {
        if (onStyleChange != null) {
            onStyleChange.run();
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
