package org.mesutormanli.jtypewriter.ui.component;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
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

import java.util.ArrayList;
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
 *
 * <p>At narrow widths the row never wraps and never clips: {@link ToolbarFit}
 * measures the labels (RW-15) and the dropdowns collapse from
 * {@code "Theme: Dark"} to {@code "Dark"} so the five buttons and all six
 * dropdowns always stay visible. Every control carries a tooltip from the
 * bundle and is Tab-reachable in visual order (RW-16).</p>
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

    /**
     * Language-neutral key notation, matching the shortcut list in the
     * localized {@code welcomeShortcuts} text; only the label part of a
     * tooltip comes from the bundle.
     */
    private static final String SHORTCUT_OPEN = "Ctrl+O";
    private static final String SHORTCUT_SAVE = "Ctrl+S";
    private static final String SHORTCUT_UNDO = "Ctrl+Z";
    private static final String SHORTCUT_REDO = "Ctrl+Y";
    private static final String SHORTCUT_THEME = "Ctrl+Shift+T";
    private static final String SHORTCUT_YOLO = "Ctrl+Shift+Y";
    private static final String SHORTCUT_COLOR = "Ctrl+Shift+C";
    private static final String SHORTCUT_FONT = "Ctrl++ / Ctrl+- / Ctrl+0";

    /** One tooltip and the bundle-derived text behind it, re-read on every {@link #refresh()}. */
    private record TooltipBinding(Tooltip tooltip, Supplier<String> text) {
    }

    private final List<TooltipBinding> tooltipBindings = new ArrayList<>();

    /** The current caption style; changed only by {@link #applyFit(double)} (RW-15). */
    private ToolbarFit.LayoutMode layoutMode = ToolbarFit.LayoutMode.FULL;

    /** Preferred width of the fully-captioned row; recomputed in every {@link #refresh()}. */
    private double requiredFullWidthPx;

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
        // Deliberately compact; when even that is not enough at a narrow
        // window, applyFit() collapses the dropdown captions (RW-15).
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
        showAsCaptionedCombo(fontSizeCombo, messages::toolbarFontSize);

        languageCombo = createCombo(List.of(Language.values()), nativeName(), value -> {
            // fires the LocaleManager listener, which re-enters refresh()
            localeManager.setLanguage(value);
            refresh();
        });
        showAsCaptionedCombo(languageCombo, messages::toolbarLanguage);
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

        // Tooltips: the localized label plus the language-neutral key hint,
        // re-read from the bundle in every refresh() so a language switch
        // cannot leave a stale-language tooltip behind (RW-16).
        attachTooltip(openBtn, () -> withShortcut(messages.toolbarOpen(), SHORTCUT_OPEN));
        attachTooltip(saveBtn, () -> withShortcut(messages.toolbarSave(), SHORTCUT_SAVE));
        attachTooltip(undoBtn, () -> withShortcut(messages.toolbarUndo(), SHORTCUT_UNDO));
        attachTooltip(redoBtn, () -> withShortcut(messages.toolbarRedo(), SHORTCUT_REDO));
        attachTooltip(themeCombo, () -> withShortcut(messages.toolbarTheme(), SHORTCUT_THEME));
        attachTooltip(yoloCombo, () -> withShortcut(messages.toolbarYolo(), SHORTCUT_YOLO));
        attachTooltip(soundCombo, messages::toolbarSound);
        attachTooltip(colorCombo, () -> withShortcut(messages.toolbarColor(), SHORTCUT_COLOR));
        attachTooltip(fontSizeCombo, () -> withShortcut(messages.toolbarFontSize(), SHORTCUT_FONT));
        attachTooltip(languageCombo, messages::toolbarLanguage);
        attachTooltip(aboutBtn, messages::toolbarAbout);

        // Tab order = visual order, left to right, buttons first and the
        // dropdowns after them. JavaFX 26 has no nextFocus API, so the order
        // is pinned deliberately through viewOrder, which both the indexed
        // and the geometrical traversal methods honour (RW-16). It follows
        // the child order, so the paint order cannot change.
        var traversalOrder = new Node[]{
                openBtn, saveBtn, undoBtn, redoBtn,
                themeCombo, yoloCombo, soundCombo, colorCombo, fontSizeCombo,
                aboutBtn, languageCombo
        };
        for (var index = 0; index < traversalOrder.length; index++) {
            traversalOrder[index].setViewOrder(10 * (index + 1));
        }

        localeManager.addListener(lang -> refresh());
        refresh();

        // RW-15: keep the single row at every width by collapsing the
        // dropdown captions when the fully-captioned row would not fit.
        widthProperty().addListener((obs, oldWidth, width) -> {
            if (applyFit(width.doubleValue())) {
                refresh();
            }
        });
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

    // --- overflow fit (RW-15) -------------------------------------------------

    /**
     * The toolbar row as pure measurement data, in child order: every label
     * the buttons render and, per dropdown, the popup values plus their
     * captioned closed form (or the bare value when {@code compact}). Shared
     * by the live fit decision and the headless DE/FR acceptance measurements
     * in {@code ToolbarFitTest} - there is no second copy that could drift.
     */
    public static List<ToolbarFit.Control> fitControls(Messages messages, boolean compact) {
        var themeTexts = themeOptions().stream().map(theme -> theme.getDisplayName(messages)).toList();
        var onOffTexts = List.of(messages.labelOn(), messages.labelOff());
        var colorTexts = textColorOptions().stream().map(color -> color.getDisplayName(messages)).toList();
        var sizeTexts = fontSizeOptions(FontSizeManager.DEFAULT_SIZE).stream()
                .map(String::valueOf).toList();
        var languageTexts = languageOptions().stream().map(Language::nativeName).toList();
        // A null caption is the COMPACT form: the closed dropdown shows the bare value.
        return List.of(
                ToolbarFit.Control.button(messages.toolbarOpen()),
                ToolbarFit.Control.button(messages.toolbarSave()),
                ToolbarFit.Control.button(messages.toolbarUndo()),
                ToolbarFit.Control.button(messages.toolbarRedo()),
                ToolbarFit.Control.combo(compact ? null : messages.toolbarTheme(), themeTexts),
                ToolbarFit.Control.combo(compact ? null : messages.toolbarYolo(), onOffTexts),
                ToolbarFit.Control.combo(compact ? null : messages.toolbarSound(), onOffTexts),
                ToolbarFit.Control.combo(compact ? null : messages.toolbarColor(), colorTexts),
                ToolbarFit.Control.combo(compact ? null : messages.toolbarFontSize(), sizeTexts),
                ToolbarFit.Control.button(messages.toolbarAbout()),
                ToolbarFit.Control.combo(compact ? null : messages.toolbarLanguage(), languageTexts),
                ToolbarFit.Control.spacer()
        );
    }

    /**
     * Applies the overflow decision for the current toolbar width.
     *
     * <p>Returns whether the mode changed; the caller re-renders through
     * {@link #refresh()} - the one sync path. It must return the verdict
     * instead of refreshing itself: {@link #refresh()} calls back into this
     * method once the captions (and therefore the required width) have been
     * re-measured, and a direct recursive refresh would render twice.</p>
     */
    private boolean applyFit(double availableWidth) {
        var next = ToolbarFit.decide(availableWidth, requiredFullWidthPx, layoutMode);
        if (next == layoutMode) {
            return false;
        }
        layoutMode = next;
        return true;
    }

    /** Re-measures the fully-captioned row; see {@link #fitControls}. */
    private void recomputeFitRequirement() {
        requiredFullWidthPx = ToolbarFit.requiredWidth(
                fitControls(messages, false),
                ToolbarFit.TOOLBAR_FONT_SIZE_PX,
                ToolbarFit::awtTextWidth);
    }

    // --- the single sync point ---------------------------------------------

    /**
     * Re-reads every manager and re-renders the whole toolbar: button texts,
     * dropdown items (so a language switch re-localises them), selections (so
     * a shortcut cannot leave a dropdown stale), tooltip and accessible texts,
     * and the measured width of the fully-captioned row. Dropdown handlers,
     * keyboard shortcuts and the locale listener all end here - there is no
     * second path that could drift out of step.
     */
    public void refresh() {
        // A language switch re-measures the row (German and French labels are
        // far wider), so one pass can flip FULL -> COMPACT. The second pass
        // then renders the collapsed captions. Three passes is a hard stop:
        // the policy is idempotent, so this loop exits after at most two.
        for (var pass = 0; pass < 3; pass++) {
            if (!renderOnce()) {
                return;
            }
        }
    }

    /**
     * One render pass of {@link #refresh()}; returns whether the fit mode
     * changed and another pass is needed.
     */
    private boolean renderOnce() {
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

            themeCombo.setAccessibleText(messages.toolbarTheme());
            yoloCombo.setAccessibleText(messages.toolbarYolo());
            soundCombo.setAccessibleText(messages.toolbarSound());
            colorCombo.setAccessibleText(messages.toolbarColor());
            fontSizeCombo.setAccessibleText(messages.toolbarFontSize());
            languageCombo.setAccessibleText(messages.toolbarLanguage());
            for (var binding : tooltipBindings) {
                binding.tooltip().setText(binding.text().get());
            }

            // Captions are bundle texts, so the fit measurement re-runs here
            // too - a switch from English to German can change the mode. The
            // window width is unchanged in that case, so nothing else would
            // re-evaluate the decision.
            recomputeFitRequirement();
            return getWidth() > 0 && applyFit(getWidth());
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
     * Builds one flat toolbar dropdown: non-editable, Tab-reachable like the
     * rest of the toolbar (RW-16; the popup list is keyboard-driven by
     * JavaFX itself), styled by {@code .toolbar-combo}. {@code onSelect}
     * never runs for values pushed in by {@link #refresh()}.
     */
    private <T> ComboBox<T> createCombo(List<T> options, StringConverter<T> converter,
                                        java.util.function.Consumer<T> onSelect) {
        var combo = new ComboBox<>(FXCollections.observableArrayList(options));
        combo.setConverter(converter);
        combo.setEditable(false);
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
     * render, so a language switch re-localises button and items together. In
     * {@link ToolbarFit.LayoutMode#COMPACT} the caption is left out and the
     * closed dropdown shows the bare value - that is the RW-15 degradation.
     */
    private <T> void showAsCaptionedCombo(ComboBox<T> combo, Supplier<String> caption) {
        combo.setAccessibleRole(AccessibleRole.COMBO_BOX);
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (layoutMode == ToolbarFit.LayoutMode.COMPACT) {
                    setText(combo.getConverter().toString(item));
                } else {
                    setText(caption.get() + ToolbarFit.CAPTION_SEPARATOR
                            + combo.getConverter().toString(item));
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
        return btn;
    }

    /** Gives a control its tooltip; the text supplier is re-read in every {@link #refresh()}. */
    private void attachTooltip(Control control, Supplier<String> text) {
        var tooltip = new Tooltip(text.get());
        control.setTooltip(tooltip);
        tooltipBindings.add(new TooltipBinding(tooltip, text));
    }

    /** Localized label plus the platform-neutral key hint, e.g. {@code "Save (Ctrl+S)"}. */
    private static String withShortcut(String label, String shortcut) {
        return label + " (" + shortcut + ")";
    }
}
