package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PW-31: the draft a real process wrote must be recovered by a fresh process
 * after the first one is killed with {@code SIGKILL}.
 *
 * <p>The scenario is a single end-to-end one: a child JVM ({@link CrashHarness})
 * builds the real service graph, snapshots a dirty document through the
 * production autosave path, and is then killed without any shutdown chance
 * ({@code Process.destroyForcibly()} on Linux is a {@code SIGKILL}). A second
 * child JVM then reads the recovery store - the same read the next startup
 * performs - and must return the exact text, non-ASCII included.</p>
 *
 * <p>The pair of tests is the falsifiability guarantee: if the recovery write
 * stopped happening, the kill test goes red with {@code NO_DRAFT}; if the
 * answered-close cleanup stopped deleting, the clean test goes red. Neither can
 * pass by accident.</p>
 *
 * <p>Linux only: {@link Process#destroyForcibly()} is the documented mapping to
 * {@code SIGKILL} there, and the kill assertion depends on that (a forcibly
 * killed process must not report a clean exit). The other CI legs run the rest
 * of the suite; this is the one platform where the kernel-level guarantee can
 * actually be exercised.</p>
 *
 * <p>No {@code Thread.sleep} anywhere: the only waits are for the child's
 * stdout marker and for {@code waitFor}, because the roadmap explicitly warns
 * that timing-based waits make this kind of test flaky.</p>
 */
@EnabledOnOs(OS.LINUX)
class ProcessCrashRecoveryTest {

    private static final Duration MARKER_TIMEOUT = Duration.ofSeconds(30);

    @Test
    void killedProcessLeavesTheDraftRecoverable(@TempDir Path dir) throws Exception {
        var dataDir = dir.resolve("data");
        var tmpHome = dir.resolve("home");
        var process = launch(dataDir, tmpHome, "dirty");
        try {
            readUntil(process, "SNAPSHOTTED", MARKER_TIMEOUT);
            process.destroyForcibly();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "the killed child did not exit");
            // A SIGKILL never produces a clean exit status, and that is exactly
            // what proves no shutdown hook, no WINDOW_HIDDEN handler and no
            // Application.stop() cleanup could have run on the way out.
            assertFalse(process.isAlive(), "the child survived SIGKILL");
            assertNotEquals(0, process.exitValue(), "a SIGKILL must not look like a clean exit");
        } finally {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }

        var marker = recover(dataDir, tmpHome);
        var expected = "DRAFT " + Base64.getEncoder()
                .encodeToString(CrashHarness.TEXT.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, marker, "the draft written before the kill was not recovered");
        var decoded = new String(
                Base64.getDecoder().decode(marker.substring("DRAFT ".length())), StandardCharsets.UTF_8);
        assertEquals(CrashHarness.TEXT, decoded, "the recovered text was not byte-identical");

        // The kill happened while the session's slot was live, so the store
        // holds one draft and no parked copy. (The recovering store adopts the
        // killed session's file into its own slot, so this count is stable.)
        var files = recoveryFiles(dataDir);
        assertEquals(1, files.size(), "expected exactly one recovery file, found: " + files);
        assertTrue(files.get(0).getFileName().toString().startsWith("draft-"),
                "the surviving draft must be the live slot, found: " + files.get(0));
    }

    /**
     * The answered-close exit path drops the draft, so a later start finds
     * nothing. This is the falsifiability pair of
     * {@link #killedProcessLeavesTheDraftRecoverable}: if the cleanup stopped
     * deleting, this test goes red; if the kill path started deleting, the
     * kill test goes red.
     */
    @Test
    void anAnsweredExitStillDropsTheDraft(@TempDir Path dir) throws Exception {
        var dataDir = dir.resolve("data");
        var tmpHome = dir.resolve("home");
        var process = launch(dataDir, tmpHome, "clean");
        try {
            readUntil(process, "CLEANED", MARKER_TIMEOUT);
            assertEquals(0, await(process), "the answered close must exit normally");
        } finally {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }

        assertEquals("NO_DRAFT", recover(dataDir, tmpHome),
                "an answered close must not leave a draft behind");
    }

    /**
     * {@code AppLogging} resolves its log directory from {@code user.home}, so
     * the child's {@code -Duser.home} must keep it inside the temporary home -
     * the test can then never pollute the developer's real
     * {@code ~/.jtypewriter} (analysis V19).
     */
    @Test
    void theChildNeverWritesToTheRealUserHome(@TempDir Path dir) throws Exception {
        var dataDir = dir.resolve("data");
        var tmpHome = dir.resolve("home");
        var process = launch(dataDir, tmpHome, "dirty");
        try {
            readUntil(process, "SNAPSHOTTED", MARKER_TIMEOUT);
            process.destroyForcibly();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "the killed child did not exit");
        } finally {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }

        var logs = tmpHome.resolve(".jtypewriter").resolve("logs");
        assertTrue(Files.isDirectory(logs), "AppLogging did not write under the temporary home");
        try (var entries = Files.list(logs)) {
            var logFiles = entries.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".log"))
                    .toList();
            assertFalse(logFiles.isEmpty(), "no log file under the temporary home: " + logs);
        }
    }

    /** Starts a child JVM running {@link CrashHarness} for one mode. */
    private static Process launch(Path dataDir, Path tmpHome, String mode) throws IOException {
        // Surefire forks tests through a booter JAR, so java.class.path does not
        // reliably list the project's own classes; the test class path it
        // computed and handed to the fork does.
        var classpath = System.getProperty("surefire.test.class.path");
        if (classpath == null) {
            classpath = System.getProperty("java.class.path");
        }
        assertNotNull(classpath, "neither surefire.test.class.path nor java.class.path is set");
        var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var builder = new ProcessBuilder(
                java,
                "-Duser.home=" + tmpHome,
                "-cp", classpath,
                CrashHarness.class.getName(),
                dataDir.toString(),
                mode);
        builder.redirectErrorStream(true);
        return builder.start();
    }

    /**
     * Waits for the child's marker line on the merged stdout. The queue lets
     * the reader thread own the stream while this thread only waits on the
     * marker, so a timeout can report every line it saw.
     */
    private static String readUntil(Process process, String marker, Duration timeout) {
        BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            var thread = new Thread(runnable, "crash-harness-stdout");
            thread.setDaemon(true);
            return thread;
        });
        try {
            executor.submit(() -> {
                // US-ASCII on purpose: the harness emits only ASCII markers and
                // Base64, so the parent never depends on the platform charset.
                try (var reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.US_ASCII))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                } catch (IOException e) {
                    // The stream closes when the child is killed; nothing to do.
                }
            });
            var deadline = System.nanoTime() + timeout.toNanos();
            while (true) {
                var remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    break;
                }
                var line = lines.poll(remaining, TimeUnit.NANOSECONDS);
                if (line == null) {
                    break;
                }
                if (line.contains(marker)) {
                    return line;
                }
            }
            throw new AssertionError("no '" + marker + "' marker within " + timeout
                    + "; lines seen: " + new ArrayList<>(lines));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for '" + marker + "'", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Waits for a normal end of the child, and never leaves it running.
     *
     * @return the exit status
     */
    private static int await(Process process) {
        try {
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return fail("the child did not finish within 120 seconds");
            }
            return process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for the child to finish", e);
        } finally {
            process.destroyForcibly();
            try {
                process.waitFor(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Runs the recovery read in a fresh JVM and returns its marker line. */
    private static String recover(Path dataDir, Path tmpHome) throws Exception {
        var process = launch(dataDir, tmpHome, "recover");
        try {
            // "DRAFT" is a prefix of both terminal markers ("DRAFT <base64>"
            // and "NO_DRAFT"), so one wait covers both outcomes.
            var marker = readUntil(process, "DRAFT", MARKER_TIMEOUT);
            assertEquals(0, await(process), "the recovery child failed");
            return marker;
        } finally {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
    }

    private static List<Path> recoveryFiles(Path dataDir) throws IOException {
        try (var entries = Files.list(dataDir.resolve("recovery"))) {
            return entries.filter(Files::isRegularFile).toList();
        }
    }
}
