/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class MainController implements Initializable {

    private final EditorArea editorArea;
    private final MainMenu mainMenu;
    private final ToolbarView toolbarView;
    private final StatsBar statsBar;
    private final FileService fileService;
    private final Messages messages;
    private final LocaleManager localeManager;
    private final KeyboardShortcutHandler shortcutHandler;
    private final EditorKeyHandler editorKeyHandler;
    private final AutoSaveService autoSaveService;
    private final FindPanel findPanel;
    private final OverlaySlot overlaySlot = new OverlaySlot();
    private final WelcomeCard welcomeCard;
    private final AboutCard aboutCard;
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

    public MainController(EditorArea editorArea, MainMenu mainMenu, ToolbarView toolbarView,
                          StatsBar statsBar,
                          FileService fileService, Messages messages, LocaleManager localeManager,
                          KeyboardShortcutHandler shortcutHandler,
                          EditorKeyHandler editorKeyHandler,
                          AutoSaveService autoSaveService,
                          FindPanel findPanel,
                          WelcomeCard welcomeCard, AboutCard aboutCard,
                          AppSettings settings, Supplier<LocalTime> clock) {
        this.editorArea = editorArea;
        this.mainMenu = mainMenu;
        this.toolbarView = toolbarView;
        this.statsBar = statsBar;
        this.fileService = fileService;
        this.messages = messages;
        this.localeManager = localeManager;
        this.shortcutHandler = shortcutHandler;
        this.editorKeyHandler = editorKeyHandler;
        this.autoSaveService = autoSaveService;
        this.findPanel = findPanel;
        this.welcomeCard = welcomeCard;
        this.aboutCard = aboutCard;
        this.settings = settings;
        this.clock = clock;
        // The toolbar comes back exactly as visible (or hidden) as the
        // user left it; applied in initialize(), i.e. before the first paint.
        this.toolbarVisible = settings.isToolbarVisible();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        toolbarView.setOnYoloChange(this::refreshTitle);
        shortcutHandler.bind(editorArea, toolbarView, stage, this::toggleToolbar, toolbarView::cycleTheme);
        // The toolbar buttons and the key filter share this one runner.
        toolbarView.setActionRunner(shortcutHandler::run);
        mainMenu.setActionRunner(shortcutHandler::run);
        // A recent row on the welcome card names the next document, so it goes
        // through the same unsaved-changes gate as Open and drop: the card must
        // not become a second, quieter way to discard the editor.
        welcomeCard.setOnOpenRecent(path -> fileService.openPathAsync(stage, path)
                .thenAccept(opened -> opened.ifPresent(ignored ->
                        editorArea.setDocument(fileService.getCurrentContent()))));
        // "Show welcome" is the way back out of the checkbox's opt-out: it
        // clears the stored preference before showing the card, so the card's
        // own checkbox is unchecked again.
        mainMenu.setWelcomeRunner(() -> {
            settings.setWelcomeCardEnabled(true);
            welcomeCard.show();
        });
        // The find panel owns its own state; the key filter and the menu only
        // reach it through the four-command port.
        findPanel.setStage(stage);
        shortcutHandler.setFindCommands(findPanel);
        editorKeyHandler.bind(editorArea);
        fileService.addListener(new FileService.Listener() {
            @Override
            public void onLoaded(java.nio.file.Path file) {
                clearSaveStatus(); // a freshly opened file was not saved here
                refreshTitle();
            }

            @Override
            public void onSaved(java.nio.file.Path file) {
                // The onSaved listener is the dirty→clean seam in FileService (the
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

            /**
             * Saves now run on an I/O worker, so the window would otherwise look
             * unchanged for the duration of a slow or network write. The status
             * bar says "Saving..." until the save commits (which re-renders the
             * "Saved 12:04" line) or fails (which leaves the honest previous
             * state in place).
             */
            @Override
            public void onSavingChanged(boolean saving) {
                if (saving) {
                    statsBar.setSaveStatus(messages.savingStatus());
                } else {
                    refreshSaveStatus();
                }
            }
        });
        // The title and the save status are composed from bundle texts, so a
        // language switch has to rebuild both; the toolbar updates its own
        // labels through the same event.
        localeManager.addListener(language -> {
            refreshTitle();
            refreshSaveStatus();
            refreshAutosaveWarning(); // the warning is bundle text too
            // A card that is open is text from the bundle like any other label.
            welcomeCard.relocalise();
            aboutCard.relocalise();
        });
        autoSaveService.health().addListener(this::refreshAutosaveWarning);
        refreshTitle();
        // A failure can precede the wiring: parking the draft happens inside
        // offerRecovery, before this controller sees the stage.
        refreshAutosaveWarning();
    }

    /**
     * The safety net's state, rendered as a persistent
     * warning in the status bar. Autosave failures used to be a {@code WARNING}
     * line in a log file nobody opens, while the product kept promising crash
     * protection it could no longer deliver; the bar now says so for as long as
     * it is true - with text that matches the failure kind - and clears itself
     * once the operation succeeds again.
     */
    private void refreshAutosaveWarning() {
        statsBar.setAutosaveProblem(autoSaveService.health().problem().orElse(null));
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
                fileService.hasUnsavedChanges(), editorArea.isYoloMode(), messages.dashSeparator());
        if (titleChanged(lastTitle, title)) {
            lastTitle = title;
            stage.setTitle(title);
        }
    }

    /** Only a real dirty↔clean, name, locale or YOLO change reaches the native window. */
    static boolean titleChanged(String previousTitle, String nextTitle) {
        return !Objects.equals(previousTitle, nextTitle);
    }

    /**
     * Pure title composition, kept out of the stage so it can be unit tested.
     * {@code separator} is the bundle's {@code dashSeparator}: the dash that
     * joins two names is language typography, not a constant.
     */
    static String titleFor(String appName, String documentName, boolean dirty, boolean yolo,
                           String separator) {
        return appName + separator + documentName + (dirty ? " *" : "") + (yolo ? "   [YOLO]" : "");
    }

    /**
     * Pure save-status composition, the {@link #titleFor} precedent for
     * headless tests. {@code template} is the localized {@code MessageFormat}
     * pattern from the bundle ({@code savedStatus}, {@code {0}} is the time)
     * and {@code savedAt} the already-resolved save time - the clock is
     * injected, mirroring {@code SessionStats}, so no test needs a toolkit or
     * the wall clock. The time itself follows the UI language's short time
     * pattern, so English says {@code "12:04 PM"} while Turkish and German say
     * {@code "12:04"}; only the wording lives in the bundles. {@code locale}
     * is the UI language's locale ({@code messages.locale()}), passed
     * explicitly so neither the formatter nor {@code MessageFormat} falls back
     * to the system locale.
     */
    static String savedStatusFor(String template, LocalTime savedAt, Locale locale) {
        var time = savedAt.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(locale));
        return new MessageFormat(template, locale).format(new Object[]{time});
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // The find panel, the welcome card and the About card all float over
        // the text in the window's top-right corner (Notepad++'s placement for
        // the find panel) instead of taking a row of their own: the editor
        // keeps the full height, and a card only covers text while it is open.
        // They share one corner - OverlaySlot makes sure at most one is up.
        var editorStack = new StackPane(editorArea, findPanel, welcomeCard, aboutCard);
        StackPane.setAlignment(findPanel, Pos.TOP_RIGHT);
        StackPane.setMargin(findPanel, new Insets(OverlayCard.EDGE_MARGIN));
        for (var card : new OverlayCard[]{welcomeCard, aboutCard}) {
            StackPane.setAlignment(card, Pos.TOP_RIGHT);
            StackPane.setMargin(card, new Insets(OverlayCard.EDGE_MARGIN));
            // The window, not the screen, is the card's bound now: a card is
            // taller than a short window, so it shrinks to it and scrolls.
            card.maxHeightProperty().bind(
                    editorStack.heightProperty().subtract(2 * OverlayCard.EDGE_MARGIN));
            card.setOnClosed(editorArea::requestFocus);
        }
        findPanel.setOverlaySlot(overlaySlot);
        welcomeCard.setOverlaySlot(overlaySlot);
        aboutCard.setOverlaySlot(overlaySlot);
        rootPane.setCenter(editorStack);
        // The menu lives inside the toolbar row (see MainMenu), so hiding the
        // toolbar still leaves nothing but the text.
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
            // The document is dirty again the moment it changes, so a
            // "Saved ..." claim would be a lie - drop it with the first edit.
            clearSaveStatus();
        });
    }

    private void toggleToolbar() {
        toolbarVisible = !toolbarVisible;
        toolbarView.setVisible(toolbarVisible);
        toolbarView.setManaged(toolbarVisible);
        // Write through immediately, like every other setting
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
