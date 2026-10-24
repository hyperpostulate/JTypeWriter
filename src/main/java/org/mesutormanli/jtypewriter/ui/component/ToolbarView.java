package org.mesutormanli.jtypewriter.ui.component;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.AboutDialog;
import org.mesutormanli.jtypewriter.ui.theme.FontSizeManager;
import org.mesutormanli.jtypewriter.ui.theme.TextColor;
import org.mesutormanli.jtypewriter.ui.theme.TextColorManager;
import org.mesutormanli.jtypewriter.ui.theme.Theme;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;

import java.util.List;
import java.util.function.Supplier;

/**
 * The top toolbar: file and undo actions, an About button and six dropdowns -
 * theme, YOLO mode, sound, text colour, font size and language.
 *
 * <p>Every dropdown mirrors state owned by a manager ({@link ThemeManager},
 * {@link ToolbarState}, {@link TextColorManager}, {@link FontSizeManager},
 * {@link LocaleManager}); none of them owns anything. Dropdown selections,
 * keyboard shortcuts and language switches all funnel through
 * {@link #refresh()}, so a control can never show a stale value - the two
 * paths used to be separate and the Ctrl+Shift+T shortcut left the theme
 * label behind (RW-12).</p>
 */
public class ToolbarView extends HBox {

    private final FileService fileService;
    private final ThemeManager themeManager;
    private final TextColorManager textColorManager;
    private final FontSizeManager fontSizeManager;
    private final ToolbarState toolbarState;
    private final Messages messages;
    private final LocaleManager localeManager;
    private final AboutDialog aboutDialog;

    private final Button openBtn;
    private final Button saveBtn;
    private final Button undoBtn;
    private final Button redoBtn;
    private final ComboBox<Theme> themeCombo;
    private final ComboBox<Boolean> yoloCombo;
    private final ComboBox<Boolean> soundCombo;
    private final ComboBox<TextColor> colorCombo;
    private final ComboBox<Integer> fontSizeCombo;
    private final ComboBox<Language> languageCombo;
    private final Button aboutBtn;

    /** Set while {@link #refresh()} re-pushes values, to suppress the change listeners. */
    private boolean refreshing;

    private EditorArea editorArea;
    private Runnable onStyleChange;
    private Runnable onYoloChange;
    private Stage stage;

    public ToolbarView(FileService fileService, ThemeManager themeManager,
                       TextColorManager textColorManager, FontSizeManager fontSizeManager,
                       ToolbarState toolbarState, Messages messages,
                       LocaleManager localeManager, AboutDialog aboutDialog) {
        this.fileService = fileService;
        this.themeManager = themeManager;
        this.textColorManager = textColorManager;
        this.fontSizeManager = fontSizeManager;
        this.toolbarState = toolbarState;
        this.messages = messages;
        this.localeManager = localeManager;
        this.aboutDialog = aboutDialog;
        this.editorArea = null;

        getStyleClass().add("toolbar");
        // Deliberately compact: with the longest translations (German) the
        // row's preferred width stays under the 900 px minimum window.
        setPadding(new Insets(8, 10, 8, 10));
        setSpacing(3);
        setAlignment(Pos.CENTER_LEFT);

        openBtn = createButton(messages.toolbarOpen());
        saveBtn = createButton(messages.toolbarSave());
        undoBtn = createButton(messages.toolbarUndo());
        redoBtn = createButton(messages.toolbarRedo());
        aboutBtn = createButton(messages.toolbarAbout());

        themeCombo = createCombo(ThemeManager.options(), themeName(), value -> {
            themeManager.setTheme(getScene(), value);
            refresh();
        });
        showAsCaptionedCombo(themeCombo, messages::toolbarTheme);

        yoloCombo = createCombo(ToolbarState.TOGGLE_OPTIONS, onOff(), value -> {
            if (editorArea == null) {
                refresh(); // nothing to switch yet: put the dropdown back in step
                return;
            }
            toolbarState.setYolo(value, editorArea);
            refresh();
            if (onYoloChange != null) {
                onYoloChange.run(); // keeps the [YOLO] window title badge honest
            }
        });
        showAsCaptionedCombo(yoloCombo, messages::toolbarYolo);

        soundCombo = createCombo(ToolbarState.TOGGLE_OPTIONS, onOff(), value -> {
            toolbarState.setSound(value);
            refresh();
        });
        showAsCaptionedCombo(soundCombo, messages::toolbarSound);

        colorCombo = createCombo(TextColorManager.options(), colourName(), value -> {
            textColorManager.set(value);
            refresh();
            notifyStyleChange();
        });
        showAsCaptionedCombo(colorCombo, messages::toolbarColor);

        fontSizeCombo = createCombo(
                FontSizeManager.optionsFor(fontSizeManager.getCurrentSize()), plainNumber(),
                value -> {
                    fontSizeManager.setSize(value);
                    refresh();
                    notifyStyleChange();
                });

        languageCombo = createCombo(List.of(Language.values()), nativeName(), value -> {
            // fires the LocaleManager listener, which re-enters refresh()
            localeManager.setLanguage(value);
            refresh();
        });
        languageCombo.getStyleClass().add("toolbar-lang-combo");

        openBtn.setOnAction(e -> {
            fileService.openFile(stage).ifPresent(path ->
                    editorArea.setDocument(fileService.getCurrentContent()));
        });
        saveBtn.setOnAction(e -> {
            if (editorArea != null) {
                fileService.saveFile(stage, editorArea.getText());
            }
        });
        undoBtn.setOnAction(e -> {
            if (editorArea != null) editorArea.requestUndo(); // no-op while YOLO mode is on
        });
        redoBtn.setOnAction(e -> {
            if (editorArea != null) editorArea.requestRedo();
        });
        aboutBtn.setOnAction(e -> {
            if (stage != null) aboutDialog.show(stage);
        });

        // Visual grouping replaces the old "Label("|")" separators: file
        // actions, edit actions, the settings dropdowns, and About + language.
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setMargin(themeCombo, new Insets(0, 0, 0, 4));

        getChildren().addAll(
                openBtn, saveBtn,
                undoBtn, redoBtn,
                themeCombo, yoloCombo, soundCombo, colorCombo, fontSizeCombo,
                spacer,
                aboutBtn,
                languageCombo
        );

        localeManager.addListener(lang -> refresh());
        refresh();
    }

    // --- dropdown option lists (pure; unit tested headlessly) -------------

    /** Theme dropdown order: Light, Dark, Sepia (the {@link Theme} declaration order). */
    public static List<Theme> themeOptions() {
        return ThemeManager.options();
    }

    /** YOLO and Sound dropdown order: On, then Off. */
    public static List<Boolean> toggleOptions() {
        return ToolbarState.TOGGLE_OPTIONS;
    }

    /** Text colour dropdown order: Default, Sage, Lavender, Sand, Steel, Rose. */
    public static List<TextColor> textColorOptions() {
        return TextColorManager.options();
    }

    /**
     * Font size dropdown for the given persisted size: the fixed ladder from
     * {@link FontSizeManager#optionsFor(int)}, with an off-ladder size inserted
     * in sorted position so the real value stays visible.
     */
    public static List<Integer> fontSizeOptions(int currentSize) {
        return FontSizeManager.optionsFor(currentSize);
    }

    /** Language dropdown order: the {@link Language} declaration order. */
    public static List<Language> languageOptions() {
        return List.of(Language.values());
    }

    // --- the single sync point ---------------------------------------------

    /**
     * Re-reads every manager and re-renders the whole toolbar: button texts,
     * dropdown items (so a language switch re-localises them) and selections
     * (so a shortcut cannot leave a dropdown stale). Dropdown handlers,
     * keyboard shortcuts and the locale listener all end here - there is no
     * second path that could drift out of step.
     */
    public void refresh() {
        refreshing = true;
        try {
            openBtn.setText(messages.toolbarOpen());
            saveBtn.setText(messages.toolbarSave());
            undoBtn.setText(messages.toolbarUndo());
            redoBtn.setText(messages.toolbarRedo());
            aboutBtn.setText(messages.toolbarAbout());

            resync(themeCombo, themeOptions(), themeManager.getCurrentTheme());
            resync(yoloCombo, toggleOptions(), toolbarState.isYoloMode());
            resync(soundCombo, toggleOptions(), toolbarState.isSoundEnabled());
            resync(colorCombo, textColorOptions(), textColorManager.getCurrent());
            var size = fontSizeManager.getCurrentSize();
            resync(fontSizeCombo, fontSizeOptions(size), size);
            resync(languageCombo, languageOptions(), localeManager.getCurrent());
        } finally {
            refreshing = false;
        }
    }

    // --- keyboard shortcut entry points -------------------------------------

    /**
     * Ctrl+Shift+T: advances the theme through {@link ThemeManager#setTheme}
     * and refreshes, so the dropdown always shows the applied theme.
     */
    public void cycleTheme() {
        var scene = getScene();
        if (scene != null) {
            themeManager.cycleTheme(scene);
            refresh();
        }
    }

    /**
     * Ctrl+Shift+Y: toggles YOLO mode through {@link ToolbarState#setYolo} -
     * the same single path the dropdown uses - and keeps the {@code [YOLO]}
     * window title badge (via {@code onYoloChange}) in step.
     */
    public void toggleYolo() {
        if (editorArea == null) {
            return;
        }
        toolbarState.setYolo(!toolbarState.isYoloMode(), editorArea);
        refresh();
        if (onYoloChange != null) {
            onYoloChange.run();
        }
    }

    /** Ctrl+Shift+C: advances the text colour through {@link TextColorManager#set}. */
    public void cycleTextColor() {
        textColorManager.cycle();
        refresh();
        notifyStyleChange();
    }

    /**
     * Ctrl++ / Ctrl+-: one ladder step ({@link FontSizeManager#stepUp(int)} /
     * {@link FontSizeManager#stepDown(int)}) through {@link FontSizeManager#setSize},
     * so the shortcut and the dropdown always agree on the available sizes.
     */
    public void changeFontSize(int delta) {
        var current = fontSizeManager.getCurrentSize();
        fontSizeManager.setSize(delta >= 0
                ? FontSizeManager.stepUp(current)
                : FontSizeManager.stepDown(current));
        refresh();
        notifyStyleChange();
    }

    /** Ctrl+0: restores the default editor font size (single source: {@link FontSizeManager#DEFAULT_SIZE}). */
    public void resetFontSize() {
        fontSizeManager.reset();
        refresh();
        notifyStyleChange();
    }

    // --- wiring to the rest of the window ------------------------------------

    public void setEditorArea(EditorArea editorArea) {
        this.editorArea = editorArea;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnStyleChange(Runnable callback) {
        this.onStyleChange = callback;
    }

    public void setOnYoloChange(Runnable callback) {
        this.onYoloChange = callback;
    }

    public int getCurrentFontSize() {
        return fontSizeManager.getCurrentSize();
    }

    public String getCurrentTextCssColor() {
        return textColorManager.getCurrentCssColor();
    }

    // --- plumbing -------------------------------------------------------------

    /**
     * Builds one flat toolbar dropdown: non-editable, keyboard-transparent
     * (the shortcuts cover everything), styled by {@code .toolbar-combo}.
     * {@code onSelect} never runs for values pushed in by {@link #refresh()}.
     */
    private <T> ComboBox<T> createCombo(List<T> options, StringConverter<T> converter,
                                        java.util.function.Consumer<T> onSelect) {
        var combo = new ComboBox<>(FXCollections.observableArrayList(options));
        combo.setConverter(converter);
        combo.setEditable(false);
        combo.setFocusTraversable(false);
        combo.getStyleClass().add("toolbar-combo");
        combo.valueProperty().addListener((obs, oldV, newV) -> {
            if (refreshing || newV == null) {
                return;
            }
            onSelect.accept(newV);
        });
        return combo;
    }

    /**
     * The closed dropdown reads {@code "Theme: Dark"}-style (caption from the
     * bundle, value from the converter), while the open popup lists the plain
     * localised values. The caption is read through the supplier on every
     * render, so a language switch re-localises button and items together.
     */
    private <T> void showAsCaptionedCombo(ComboBox<T> combo, Supplier<String> caption) {
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(caption.get() + ": " + combo.getConverter().toString(item));
                }
            }
        });
    }

    /**
     * Replaces the items (fresh list, so the localized converter text re-renders)
     * and re-pushes the value. The brief null-reset makes even an unchanged
     * selection re-run the cell update, which is what re-localises the button
     * text after a language switch.
     */
    private static <T> void resync(ComboBox<T> combo, List<T> items, T value) {
        combo.setItems(FXCollections.observableArrayList(items));
        combo.setValue(null);
        combo.setValue(value);
    }

    private StringConverter<Theme> themeName() {
        return new StringConverter<>() {
            @Override
            public String toString(Theme theme) {
                return theme == null ? "" : theme.getDisplayName(messages);
            }

            @Override
            public Theme fromString(String text) {
                return null; // the dropdown is never editable
            }
        };
    }

    private StringConverter<TextColor> colourName() {
        return new StringConverter<>() {
            @Override
            public String toString(TextColor color) {
                return color == null ? "" : color.getDisplayName(messages);
            }

            @Override
            public TextColor fromString(String text) {
                return null;
            }
        };
    }

    private StringConverter<Boolean> onOff() {
        return new StringConverter<>() {
            @Override
            public String toString(Boolean on) {
                return Boolean.TRUE.equals(on) ? messages.labelOn() : messages.labelOff();
            }

            @Override
            public Boolean fromString(String text) {
                return null;
            }
        };
    }

    private StringConverter<Integer> plainNumber() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer size) {
                return size == null ? "" : String.valueOf(size);
            }

            @Override
            public Integer fromString(String text) {
                return null;
            }
        };
    }

    /** Native language names - identical in every UI language, see {@link Language#nativeName()}. */
    private StringConverter<Language> nativeName() {
        return new StringConverter<>() {
            @Override
            public String toString(Language language) {
                return language == null ? "" : language.nativeName();
            }

            @Override
            public Language fromString(String text) {
                return null;
            }
        };
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
}
