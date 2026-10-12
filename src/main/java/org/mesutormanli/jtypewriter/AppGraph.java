package org.mesutormanli.jtypewriter;

import javafx.stage.Stage;
import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.AutoSaveService;
import org.mesutormanli.jtypewriter.service.DialogService;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.service.RecoveryStore;
import org.mesutormanli.jtypewriter.stats.SessionStats;
import org.mesutormanli.jtypewriter.ui.AboutDialog;
import org.mesutormanli.jtypewriter.ui.CrashHandler;
import org.mesutormanli.jtypewriter.ui.KeyboardShortcutHandler;
import org.mesutormanli.jtypewriter.ui.MainController;
import org.mesutormanli.jtypewriter.ui.MainStage;
import org.mesutormanli.jtypewriter.ui.WelcomeDialog;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;
import org.mesutormanli.jtypewriter.ui.component.EditorKeyHandler;
import org.mesutormanli.jtypewriter.ui.component.StatsBar;
import org.mesutormanli.jtypewriter.ui.component.ToolbarState;
import org.mesutormanli.jtypewriter.ui.component.ToolbarView;
import org.mesutormanli.jtypewriter.ui.theme.FontSizeManager;
import org.mesutormanli.jtypewriter.ui.theme.TextColorManager;
import org.mesutormanli.jtypewriter.ui.theme.ThemeManager;

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
    public final FileService fileService = new FileService(dialogService, messages);
    public final RecoveryStore recoveryStore = new RecoveryStore(AppDirs.recoveryDir());
    public final AutoSaveService autoSaveService = new AutoSaveService(recoveryStore, fileService, dialogService);

    // --- UI state managers -----------------------------------------------
    public final ThemeManager themeManager = new ThemeManager(settings);
    public final TextColorManager textColorManager = new TextColorManager(settings);
    public final FontSizeManager fontSizeManager = new FontSizeManager(settings);
    public final ToolbarState toolbarState = new ToolbarState(typewriterSound, settings);

    // --- editor and its handlers -----------------------------------------
    public final EditorArea editorArea = new EditorArea();
    public final EditorKeyHandler editorKeyHandler = new EditorKeyHandler(typewriterSound, sessionStats);
    public final KeyboardShortcutHandler shortcutHandler = new KeyboardShortcutHandler(fileService);

    // --- views and dialogs ------------------------------------------------
    public final AboutDialog aboutDialog = new AboutDialog(messages, themeManager);
    public final WelcomeDialog welcomeDialog = new WelcomeDialog(messages, themeManager);
    public final ToolbarView toolbarView = new ToolbarView(
            fileService, themeManager, textColorManager, fontSizeManager,
            toolbarState, messages, localeManager, aboutDialog);
    public final StatsBar statsBar = new StatsBar(sessionStats, messages);
    public final CrashHandler crashHandler = new CrashHandler(messages, () -> this.stage);

    // --- top level assembly ------------------------------------------------
    public final MainController mainController = new MainController(
            editorArea, toolbarView, statsBar, themeManager,
            fileService, shortcutHandler, editorKeyHandler);
    public final MainStage mainStage = new MainStage(fileService, settings);

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
            }
        });
    }

    /** Remembers the main window so dialogs can be owned by it (crash reports). */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
}