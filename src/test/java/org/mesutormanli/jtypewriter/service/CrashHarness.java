package org.mesutormanli.jtypewriter.service;

import org.mesutormanli.jtypewriter.AppSettings;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Child JVM for the real process-kill recovery test (PW-31).
 *
 * <p>The application is JavaFX and CI has no display, so this harness drives
 * the <em>real object graph</em> the application wires -
 * {@link AppSettings} &rarr; {@link LocaleManager} &rarr; {@link Messages} &rarr;
 * {@link DialogService} &rarr; {@link FileService} &rarr; {@link RecoveryStore}
 * &rarr; {@link AutoSaveService} - without starting the toolkit. The guarantee
 * under test is the persistence path across a real {@code SIGKILL}: the draft
 * written by the production {@code AutoSaveService.snapshot} survives the
 * process, and the production {@code RecoveryStore} finds it again in a fresh
 * JVM. The GUI layer is deliberately out of the forked process; it only calls
 * these same methods, so its wiring cannot change what the disk shows.</p>
 *
 * <p>Every path is passed explicitly (AGENTS.md: {@code AppSettings} and
 * {@code RecoveryStore} take explicit paths), and the parent sets
 * {@code user.home} to a temporary directory, so {@code AppLogging} cannot
 * write into the developer's real {@code ~/.jtypewriter}.</p>
 *
 * <p>Protocol: exactly one ASCII marker line on stdout, flushed, because the
 * parent blocks on it. Text is carried as Base64 so no stage depends on the
 * platform charset.</p>
 */
public final class CrashHarness {

    /** Non-ASCII and multi-line, so an encoding slip on the way to disk is caught. */
    static final String TEXT = "satır bir\nikinci satır: ğüşiöç\n";

    private CrashHarness() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            printMarker("FAILED usage: CrashHarness <dataDir> <mode>");
            System.exit(2);
            return;
        }
        var dataDir = Path.of(args[0]);
        var mode = args[1];
        try {
            switch (mode) {
                case "dirty" -> dirty(dataDir);
                case "clean" -> clean(dataDir);
                case "recover" -> recover(dataDir);
                default -> {
                    printMarker("FAILED unknown mode: " + mode);
                    System.exit(2);
                }
            }
        } catch (IOException e) {
            // The parent fails with this text instead of hanging on a marker
            // that will never arrive.
            printMarker("FAILED " + e.getMessage());
            System.exit(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            printMarker("FAILED interrupted");
            System.exit(2);
        }
    }

    /**
     * A dirty document is snapshotted through the production autosave path and
     * the process then stays alive until the parent kills it. {@code shutdown()}
     * is never called: the process must die the way {@code kill -9} kills it,
     * so no shutdown hook and no {@code WINDOW_HIDDEN} cleanup gets a chance.
     */
    private static void dirty(Path dataDir) throws IOException, InterruptedException {
        var autoSave = graph(dataDir);
        autoSave.fileService().setCurrentContent(TEXT);
        autoSave.autoSave().snapshot(TEXT);
        printMarker("SNAPSHOTTED");
        Thread.sleep(120_000);
    }

    /** The answered-close exit path: the draft must not survive this one. */
    private static void clean(Path dataDir) throws IOException {
        var autoSave = graph(dataDir);
        autoSave.fileService().setCurrentContent(TEXT);
        autoSave.autoSave().snapshot(TEXT);
        autoSave.autoSave().approveExit();
        autoSave.autoSave().shutdown();
        printMarker("CLEANED");
    }

    /** Reads the store the way the next startup would, without any UI. */
    private static void recover(Path dataDir) throws IOException {
        var draft = new RecoveryStore(dataDir.resolve("recovery")).loadDraft();
        if (draft.isEmpty()) {
            printMarker("NO_DRAFT");
            return;
        }
        var encoded = Base64.getEncoder()
                .encodeToString(draft.get().content().getBytes(StandardCharsets.UTF_8));
        printMarker("DRAFT " + encoded);
    }

    /** Holds the graph so it is not collected before the harness is done with it. */
    private record Graph(FileService fileService, AutoSaveService autoSave) {
    }

    private static Graph graph(Path dataDir) {
        var settings = new AppSettings(dataDir.resolve("settings.properties"));
        var localeManager = new LocaleManager(settings);
        var messages = new Messages(localeManager);
        var dialogService = new DialogService(messages, settings);
        var fileService = new FileService(dialogService, messages);
        var store = new RecoveryStore(dataDir.resolve("recovery"));
        return new Graph(fileService,
                new AutoSaveService(store, fileService, dialogService, messages));
    }

    private static void printMarker(String marker) {
        System.out.print(marker + "\n");
        System.out.flush();
    }
}
