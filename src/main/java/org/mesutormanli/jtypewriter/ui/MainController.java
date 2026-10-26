package org.mesutormanli.jtypewriter.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.AutoSaveService;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;
import org.mesutormanli.jtypewriter.ui.component.EditorKeyHandler;
import org.mesutormanli.jtypewriter.ui.component.StatsBar;
import org.mesutormanli.jtypewriter.ui.component.ToolbarView;

import java.net.URL;
import java.text.MessageFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class MainController implements Initializable {

    private static final DateTimeFormatter SAVE_STATUS_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final EditorArea editorArea;
    private final ToolbarView toolbarView;
    private final StatsBar statsBar;
    private final FileService fileService;
    private final Messages messages;
    private final LocaleManager localeManager;
    private final KeyboardShortcutHandler shortcutHandler;
    private final EditorKeyHandler editorKeyHandler;
    private final AutoSaveService autoSaveService;
    private final AppSettings settings;
    private final Supplier<LocalTime> clock;

    private boolean toolbarVisible;
    private Stage stage;
    /**
     * When the last save happened, or {@code null} while the document is dirty
     * or was never saved. Kept so a language switch can re-render the status
     * in the new language instead of leaving the old one on screen.
     */
    private LocalTime lastSavedAt;
    /**
     * The title last handed to the native window, so a keystroke that only
     * changes the text is not turned into a {@code stage.setTitle} call.
     */
    private String lastTitle;

    @FXML
    private BorderPane rootPane;

    public MainController(EditorArea editorArea, ToolbarView toolbarView, StatsBar statsBar,
                          FileService fileService, Messages messages, LocaleManager localeManager,
                          KeyboardShortcutHandler shortcutHandler,
                          EditorKeyHandler editorKeyHandler,
                          AutoSaveService autoSaveService,
                          AppSettings settings, Supplier<LocalTime> clock) {
        this.editorArea = editorArea;
        this.toolbarView = toolbarView;
        this.statsBar = statsBar;
        this.fileService = fileService;
        this.messages = messages;
        this.localeManager = localeManager;
        this.shortcutHandler = shortcutHandler;
        this.editorKeyHandler = editorKeyHandler;
        this.autoSaveService = autoSaveService;
        this.settings = settings;
        this.clock = clock;
        // RW-18: the toolbar comes back exactly as visible (or hidden) as the
        // user left it; applied in initialize(), i.e. before the first paint.
        this.toolbarVisible = settings.isToolbarVisible();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        toolbarView.setStage(stage);
        toolbarView.setOnYoloChange(this::refreshTitle);
        shortcutHandler.bind(editorArea, toolbarView, stage, this::toggleToolbar, toolbarView::cycleTheme);
        editorKeyHandler.bind(editorArea);
        fileService.addListener(new FileService.Listener() {
            @Override
            public void onLoaded(java.nio.file.Path file) {
                clearSaveStatus(); // a freshly opened file was not saved here
                refreshTitle();
            }

            @Override
            public void onSaved(java.nio.file.Path file) {
                // RW-19: onSaved is the dirty→clean seam in FileService (the
                // autosave draft snapshot never reaches it), so this is the
                // one honest moment to show "Saved 12:04" in the stats bar.
                lastSavedAt = clock.get();
                refreshSaveStatus();
                refreshTitle();
            }

            @Override
            public void onNewDocument() {
                clearSaveStatus(); // a never-saved document has nothing to claim
                refreshTitle();
            }
        });
        // The title and the save status are composed from bundle texts, so a
        // language switch has to rebuild both; the toolbar updates its own
        // labels through the same event.
        localeManager.addListener(language -> {
            refreshTitle();
            refreshSaveStatus();
            refreshAutosaveWarning(); // the warning is bundle text too
        });
        autoSaveService.health().addListener(this::refreshAutosaveWarning);
        refreshTitle();
        // A failure can precede the wiring: parking the draft happens inside
        // offerRecovery, before this controller sees the stage.
        refreshAutosaveWarning();
    }

    /**
     * V4/PW-27: the safety net's state, rendered as a persistent warning in the
     * status bar. Autosave failures used to be a {@code WARNING} line in a log
     * file nobody opens, while the product kept promising crash protection it
     * could no longer deliver; the bar now says so for as long as it is true,
     * and clears itself once a write succeeds again.
     */
    private void refreshAutosaveWarning() {
        statsBar.setAutosaveFailing(autoSaveService.health().isFailing());
    }

    /** Drops the "Saved ..." claim: the document is dirty or was never saved. */
    private void clearSaveStatus() {
        lastSavedAt = null;
        statsBar.setSaveStatus(null);
    }

    /**
     * Renders the stored save time in the current language, or clears the
     * status when there is nothing to claim.
     */
    private void refreshSaveStatus() {
        statsBar.setSaveStatus(lastSavedAt == null
                ? null
                : savedStatusFor(messages.savedStatusTemplate(), lastSavedAt, messages.locale()));
    }

    /**
     * The title carries the document name, a {@code *} while there are unsaved
     * changes and the persistent YOLO badge (the toolbar may be hidden), so the
     * user can always tell what is open and whether it is safe on disk.
     */
    private void refreshTitle() {
        if (stage == null) {
            return;
        }
        var path = fileService.getCurrentFilePath();
        var documentName = path != null ? path.getFileName().toString() : messages.windowUntitled();
        var title = titleFor(messages.appName(), documentName,
                fileService.hasUnsavedChanges(), editorArea.isYoloMode());
        if (titleChanged(lastTitle, title)) {
            lastTitle = title;
            stage.setTitle(title);
        }
    }

    /** Only a real dirty↔clean, name, locale or YOLO change reaches the native window. */
    static boolean titleChanged(String previousTitle, String nextTitle) {
        return !Objects.equals(previousTitle, nextTitle);
    }

    /** Pure title composition, kept out of the stage so it can be unit tested. */
    static String titleFor(String appName, String documentName, boolean dirty, boolean yolo) {
        return appName + " - " + documentName + (dirty ? " *" : "") + (yolo ? "   [YOLO]" : "");
    }

    /**
     * RW-19: pure save-status composition, the {@link #titleFor} precedent for
     * headless tests. {@code template} is the localized {@code MessageFormat}
     * pattern from the bundle ({@code savedStatus}, {@code {0}} is the time)
     * and {@code savedAt} the already-resolved save time - the clock is
     * injected, mirroring {@code SessionStats}, so no test needs a toolkit or
     * the wall clock. The time is {@code HH:mm}, matching the app's other
     * readouts; only the wording lives in the bundles. {@code locale} is the
     * UI language's locale ({@code messages.locale()}), passed explicitly so
     * {@code MessageFormat} does not fall back to the system locale.
     */
    static String savedStatusFor(String template, LocalTime savedAt, Locale locale) {
        return new MessageFormat(template, locale)
                .format(new Object[]{savedAt.format(SAVE_STATUS_TIME)});
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rootPane.setCenter(editorArea);
        rootPane.setTop(toolbarView);
        rootPane.setBottom(statsBar);

        toolbarView.setVisible(toolbarVisible);
        toolbarView.setManaged(toolbarVisible);
        toolbarView.setEditorArea(editorArea);

        toolbarView.setOnStyleChange(this::applyEditorStyle);
        applyEditorStyle();

        editorArea.textProperty().addListener((obs, oldVal, newVal) -> {
            fileService.setCurrentContent(newVal);
            refreshTitle(); // keep the dirty marker in the title in step with the text
            // RW-19: the document is dirty again the moment it changes, so a
            // "Saved ..." claim would be a lie - drop it with the first edit.
            clearSaveStatus();
        });
    }

    private void toggleToolbar() {
        toolbarVisible = !toolbarVisible;
        toolbarView.setVisible(toolbarVisible);
        toolbarView.setManaged(toolbarVisible);
        // RW-18: write through immediately, like every other setting
        settings.setToolbarVisible(toolbarVisible);
        if (handOffFocusToEditor(toolbarVisible)) {
            // The focused control just vanished; without this the caret is lost
            // and the next keystroke goes nowhere until the user clicks.
            editorArea.requestFocus();
        }
    }

    /**
     * Pure focus policy for the toolbar toggle: hiding the toolbar must hand
     * focus to the editor, showing it leaves the focus where the user put it.
     * Extracted so the rule is testable without a toolkit.
     */
    static boolean handOffFocusToEditor(boolean toolbarVisible) {
        return !toolbarVisible;
    }

    private void applyEditorStyle() {
        if (editorArea == null) {
            return;
        }
        var style = "-fx-font-size: " + toolbarView.getCurrentFontSize() + "px;";
        // Theme-dependent: each TextColor has a dark-canvas and a light-canvas hex.
        var cssColor = toolbarView.getCurrentTextCssColor();
        if (cssColor != null) {
            style += " -fx-text-fill: " + cssColor + ";";
        }
        editorArea.setStyle(style);
    }
}
