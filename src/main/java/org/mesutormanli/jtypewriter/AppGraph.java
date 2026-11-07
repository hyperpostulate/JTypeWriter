/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.AutoSaveService;
import org.mesutormanli.jtypewriter.service.BackupKeeper;
import org.mesutormanli.jtypewriter.service.DialogService;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.service.RecoveryStore;
import org.mesutormanli.jtypewriter.service.StorageFootprint;
import org.mesutormanli.jtypewriter.stats.SessionStats;
import org.mesutormanli.jtypewriter.ui.AboutCard;
import org.mesutormanli.jtypewriter.ui.CrashHandler;
import org.mesutormanli.jtypewriter.ui.DropHandler;
import org.mesutormanli.jtypewriter.ui.FindPanel;
import org.mesutormanli.jtypewriter.ui.FindScanService;
import org.mesutormanli.jtypewriter.ui.KeyboardShortcutHandler;
import org.mesutormanli.jtypewriter.ui.MainController;
import org.mesutormanli.jtypewriter.ui.MainMenu;
import org.mesutormanli.jtypewriter.ui.MainStage;
import org.mesutormanli.jtypewriter.ui.WelcomeCard;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;
import org.mesutormanli.jtypewriter.ui.component.EditorKeyHandler;
import org.mesutormanli.jtypewriter.ui.component.StatsBar;
import org.mesutormanli.jtypewriter.ui.component.StatsBarState;
import org.mesutormanli.jtypewriter.ui.component.ToolbarState;
import org.mesutormanli.jtypewriter.ui.component.ToolbarView;
import org.mesutormanli.jtypewriter.ui.theme.FontSizeManager;
import org.mesutormanli.jtypewriter.ui.theme.TextColorManager;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;

import java.time.LocalTime;

/**
 * Composition root of the application: builds the whole object graph in one
 * place, in dependency order, without any DI framework.
 *
 * <p>Every component is a single instance constructed exactly once (equivalent
 * to the former Spring singleton beans). Field declaration order encodes the
 * dependency order, so a component must be declared after everything it needs;
 * cross-component wiring (listeners) lives in the constructor.</p>
 */
public final class AppGraph {

    // --- persisted user preferences ---------------------------------------
    public final AppSettings settings = AppSettings.load();

    // --- domain services -------------------------------------------------
    public final LocaleManager localeManager = new LocaleManager(settings);
    public final Messages messages = new Messages(localeManager);
    public final SessionStats sessionStats = new SessionStats();
    public final TypewriterSound typewriterSound = new TypewriterSound();
    public final DialogService dialogService = new DialogService(messages, settings);
    /**
     * The one place that performs blocking file I/O. Declared before its users:
     * document saves, recovery snapshots and draft cleanup share this single
     * worker, so they can never race on the same file or backup generation.
     */
    public final IoExecutor ioExecutor = IoExecutor.background();
    public final FileService fileService = new FileService(
            dialogService, messages, new BackupKeeper(), ioExecutor);
    public final RecoveryStore recoveryStore = new RecoveryStore(AppDirs.recoveryDir());
    public final AutoSaveService autoSaveService = new AutoSaveService(
            recoveryStore, fileService, dialogService, messages, ioExecutor);

    // --- UI state managers -----------------------------------------------
    public final ThemeManager themeManager = new ThemeManager(settings);
    public final TextColorManager textColorManager = new TextColorManager(settings);
    public final FontSizeManager fontSizeManager = new FontSizeManager(settings);
    public final ToolbarState toolbarState = new ToolbarState(typewriterSound, settings);
    public final StatsBarState statsBarState = new StatsBarState(settings);

    // --- editor and its handlers -----------------------------------------
    public final EditorArea editorArea = new EditorArea(messages);
    public final EditorKeyHandler editorKeyHandler = new EditorKeyHandler(typewriterSound, sessionStats);
    public final FindScanService findScanService = FindScanService.background();
    public final KeyboardShortcutHandler shortcutHandler = new KeyboardShortcutHandler(fileService);
    /**
     * The find/replace card that floats over the editor. Declared with the
     * editor because it acts on it; the key filter and the command menu reach
     * it through the {@code FindCommands} port the controller installs.
     */
    public final FindPanel findPanel = new FindPanel(
            messages, localeManager, editorArea, dialogService, findScanService);

    // --- views and dialogs ------------------------------------------------
    public final AboutCard aboutCard = new AboutCard(messages);
    public final WelcomeCard welcomeCard = new WelcomeCard(messages, settings);
    public final MainMenu mainMenu = new MainMenu(messages, localeManager, aboutCard);
    public final ToolbarView toolbarView = new ToolbarView(
            themeManager, textColorManager, fontSizeManager,
            toolbarState, messages, localeManager, aboutCard, mainMenu);
    public final StatsBar statsBar = new StatsBar(sessionStats, messages, statsBarState, localeManager);
    public final DropHandler dropHandler = new DropHandler(
            fileService, dialogService, messages, editorArea);
    public final CrashHandler crashHandler = new CrashHandler(dialogService, messages, () -> this.stage);

    // --- top level assembly ------------------------------------------------
    public final MainController mainController = new MainController(
            editorArea, mainMenu, toolbarView, statsBar,
            fileService, messages, localeManager, shortcutHandler, editorKeyHandler,
            autoSaveService, findPanel, welcomeCard, aboutCard, settings, LocalTime::now);
    public final MainStage mainStage = new MainStage(
            fileService, settings, autoSaveService, dropHandler);

    private volatile Stage stage;

    public AppGraph() {
        // session statistics follow the document, not the application lifetime,
        // and the last document is remembered for the next session
        fileService.addListener(new FileService.Listener() {
            @Override
            public void onLoaded(java.nio.file.Path file) {
                sessionStats.reset();
                settings.rememberFile(file);
            }

            @Override
            public void onSaved(java.nio.file.Path file) {
                settings.rememberFile(file);
                announceStorageFootprintOnce();
                announceSettingsWriteFailureOnce();
            }

            @Override
            public void onNewDocument() {
                sessionStats.reset(); // the stats follow the document, not the window
            }
        });
    }

    /** Remembers the main window so dialogs can be owned by it (crash reports). */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Tells the user once what a saved document costs on disk.
     *
     * <p>Hooked to the first successful save rather than to startup: that is
     * when the backup generations actually appear next to the document, so the
     * sentence describes something the user can go and look at. The flag is
     * written <em>after</em> the dialog returns: if showing it fails, the next
     * save tries again instead of the claim being lost without ever having been
     * made.</p>
     *
     * <p>No stage means no window to own the alert - possible in headless tests
     * and before {@code setStage}. Skipping is right there: the notice is not
     * worth an exception on a save path.</p>
     */
    private void announceStorageFootprintOnce() {
        if (settings.isStorageNoticeShown()) {
            return;
        }
        var owner = stage;
        if (owner == null) {
            return;
        }
        dialogService.showStorageFootprintNotice(
                owner, StorageFootprint.persistentCopies(), StorageFootprint.peakCopies());
        settings.setStorageNoticeShown(true);
    }

    /**
     * Tells the user once that the preferences file cannot be written.
     *
     * <p>Deferred for a structural reason: settings are written while the
     * application is still assembling itself - the theme, the restored
     * geometry, the last document - so the failure is often known before there
     * is any window to own an alert. The flag therefore lives in
     * {@link AppSettings} and is drained here, once a stage exists. It is
     * drained again after every document save, because that is when
     * {@code settings.rememberFile} writes and a disk that filled up mid-session
     * becomes visible. The flag is cleared <em>after</em> the dialog returns, so
     * a failed alert retries instead of the warning being lost unshown.</p>
     *
     * <p>No stage means no window: possible in headless tests and before
     * {@link #setStage}. The flag is kept for the next call rather than
     * consumed, exactly like {@link #announceStorageFootprintOnce()}.</p>
     */
    public void announceSettingsWriteFailureOnce() {
        if (!settings.isSettingsWriteFailurePending()) {
            return;
        }
        var owner = stage;
        if (owner == null) {
            return;
        }
        dialogService.showSettingsWriteFailureNotice(owner, settings.getFile());
        settings.clearSettingsWriteFailure();
    }
}
