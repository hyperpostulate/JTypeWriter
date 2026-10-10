package org.mesutormanli.jtypewriter;

import org.mesutormanli.jtypewriter.audio.TypewriterSound;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.DialogService;
import org.mesutormanli.jtypewriter.service.FileService;
import org.mesutormanli.jtypewriter.stats.SessionStats;
import org.mesutormanli.jtypewriter.ui.AboutDialog;
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
 * dependency order, so a component must be declared after everything it needs.
 * Construction is fully deterministic and starts with the JavaFX lifecycle
 * instead of an IoC container boot phase.</p>
 */
public final class AppGraph {

    // --- domain services -------------------------------------------------
    public final LocaleManager localeManager = new LocaleManager();
    public final Messages messages = new Messages(localeManager);
    public final SessionStats sessionStats = new SessionStats();
    public final TypewriterSound typewriterSound = new TypewriterSound();
    public final FileService fileService = new FileService(new DialogService(messages), messages);

    // --- UI state managers -----------------------------------------------
    public final ThemeManager themeManager = new ThemeManager();
    public final TextColorManager textColorManager = new TextColorManager();
    public final FontSizeManager fontSizeManager = new FontSizeManager();
    public final ToolbarState toolbarState = new ToolbarState(typewriterSound);

    // --- editor and its handlers -----------------------------------------
    public final EditorArea editorArea = new EditorArea();
    public final EditorKeyHandler editorKeyHandler = new EditorKeyHandler(typewriterSound, sessionStats);
    public final KeyboardShortcutHandler shortcutHandler = new KeyboardShortcutHandler(fileService);

    // --- views and dialogs ------------------------------------------------
    public final AboutDialog aboutDialog = new AboutDialog(messages);
    public final WelcomeDialog welcomeDialog = new WelcomeDialog(messages);
    public final ToolbarView toolbarView = new ToolbarView(
            fileService, themeManager, textColorManager, fontSizeManager,
            toolbarState, messages, localeManager, aboutDialog);
    public final StatsBar statsBar = new StatsBar(sessionStats, messages);

    // --- top level assembly ------------------------------------------------
    public final MainController mainController = new MainController(
            editorArea, toolbarView, statsBar, themeManager,
            fileService, shortcutHandler, editorKeyHandler);
    public final MainStage mainStage = new MainStage(fileService);
}