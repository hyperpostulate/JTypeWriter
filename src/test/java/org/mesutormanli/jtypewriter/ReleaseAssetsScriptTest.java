/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code scripts/release-assets.sh} is the step that turns jpackage's output
 * into the names a release publishes, and the first time it ever ran for real
 * was the release dry run - on the Windows leg, where it died with
 * {@code tar (child): Cannot connect to D: resolve failed}.
 *
 * <p>The cause is a collision of grammars, not a wrong path: GNU tar reads
 * {@code host:path} in its archive argument as a remote copy, and a Windows
 * drive letter has exactly that shape, so {@code D:\a\...} is taken for the
 * host {@code D} before anything is written. The script therefore converts
 * every path it hands to tar through {@code cygpath}.</p>
 *
 * <p>This test reproduces the collision on a POSIX host instead of trusting the
 * fix by inspection: the paths it passes are relative and start with a drive
 * label, which is the only form where tar's remote detection actually fires
 * (an absolute path keeps a slash before the colon, so GNU tar treats it as
 * local - which is why the bug survived every local run). A stand-in
 * {@code cygpath} maps those labels onto ordinary directories, so a script that
 * forgot the conversion would hand {@code D:out/...} to tar and fail exactly
 * as the Windows runner did.</p>
 */
class ReleaseAssetsScriptTest {

    private static final Path SCRIPT = Path.of("scripts", "release-assets.sh");

    /**
     * The regression: a drive-lettered path must reach tar only after cygpath
     * has rewritten it. Without that, the packaged archive is never written on
     * Windows and the whole release fails at the staging step.
     */
    @Test
    void thePortableArchiveIsBuiltFromAPathTarCannotReadAsAHostName(@TempDir Path work)
            throws IOException, InterruptedException {
        assumePosixShell();

        // The repository layout the script expects, reachable both under its
        // real name (what the conversion yields) and under the drive-lettered
        // name the runner passes in.
        var releases = Files.createDirectories(work.resolve("releases").resolve("JTypeWriter").resolve("bin"));
        Files.writeString(releases.resolve("JTypeWriter"), "launcher", StandardCharsets.UTF_8);
        Files.writeString(work.resolve("releases").resolve("JTypeWriter-1.0.0.msi"), "installer",
                StandardCharsets.UTF_8);
        Files.createSymbolicLink(work.resolve("D:releases"), Path.of("releases"));
        Files.createDirectories(work.resolve("out"));

        var result = run(work, "1.0.0", "windows", "x64", "D:releases", "D:out");

        assertEquals(0, result.exitCode(), "release-assets.sh failed on a drive-lettered path:\n"
                + result.output());
        var archive = work.resolve("out").resolve("JTypeWriter-1.0.0-windows-x64.tar.gz");
        assertTrue(Files.isRegularFile(archive),
                "no portable archive at " + archive + "; tar was most likely handed " + "'D:out/...' as a "
                        + "remote host name:\n" + result.output());
        assertTrue(archiveHasSingleTopLevelDirectory(work, archive, "JTypeWriter"),
                "the archive does not hold exactly the image directory as its only top level entry");
    }

    /**
     * The other half of the change: an ordinary Linux path has no colon and must
     * still take the plain route, so the script stays one implementation instead
     * of a Windows branch that local builds never exercise.
     */
    @Test
    void plainPathsStillProduceTheArchive(@TempDir Path work)
            throws IOException, InterruptedException {
        assumePosixShell();

        var releases = Files.createDirectories(work.resolve("releases").resolve("JTypeWriter").resolve("bin"));
        Files.writeString(releases.resolve("JTypeWriter"), "launcher", StandardCharsets.UTF_8);
        Files.writeString(work.resolve("releases").resolve("JTypeWriter-1.0.0.msi"), "installer",
                StandardCharsets.UTF_8);

        var result = run(work, "1.0.0", "windows", "x64",
                work.resolve("releases").toString(), work.resolve("dist").toString());

        assertEquals(0, result.exitCode(), "release-assets.sh failed on a plain path:\n"
                + result.output());
        var archive = work.resolve("dist").resolve("JTypeWriter-1.0.0-windows-x64.tar.gz");
        assertTrue(Files.isRegularFile(archive), "no portable archive at " + archive);
        assertTrue(archiveHasSingleTopLevelDirectory(work, archive, "JTypeWriter"),
                "the archive does not hold exactly the image directory as its only top level entry");
    }

    /**
     * Runs the script from the temporary directory, so relative paths in this
     * test are relative to it, with a stand-in {@code cygpath} ahead of
     * everything else. The stand-in only understands the drive-label form this
     * test uses and hands anything else back unchanged.
     */
    private static ScriptResult run(Path work, String... arguments)
            throws IOException, InterruptedException {
        var fakeBin = Files.createDirectories(work.resolve("fake-bin"));
        var cygpath = fakeBin.resolve("cygpath");
        Files.writeString(cygpath, """
                #!/bin/sh
                # -u <path>: strip the drive label, the way cygpath does for MSYS
                case "$2" in
                  D:*) printf '%s' "${2#D:}" ;;
                  *)   printf '%s' "$2" ;;
                esac
                """, StandardCharsets.UTF_8);
        assertTrue(cygpath.toFile().setExecutable(true), "could not make the cygpath stand-in executable");

        var command = new java.util.ArrayList<String>();
        command.add("bash");
        command.add(SCRIPT.toAbsolutePath().toString());
        command.addAll(List.of(arguments));

        var builder = new ProcessBuilder(command).directory(work.toFile()).redirectErrorStream(true);
        builder.environment().put("PATH", fakeBin + ":/usr/bin:/bin");
        builder.environment().remove("LC_ALL");

        var process = builder.start();
        var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ScriptResult(process.waitFor(), output);
    }

    private static boolean archiveHasSingleTopLevelDirectory(Path work, Path archive, String expected)
            throws IOException, InterruptedException {
        var process = new ProcessBuilder("tar", "-tzf", archive.toString())
                .directory(work.toFile())
                .redirectErrorStream(true)
                .start();
        var listing = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor() == 0, "could not list " + archive + ":\n" + listing);

        var entries = listing.lines().filter(line -> !line.isBlank()).toList();
        var topLevel = entries.stream()
                .map(entry -> entry.contains("/") ? entry.substring(0, entry.indexOf('/')) : entry)
                .distinct()
                .toList();
        return topLevel.equals(List.of(expected));
    }

    /**
     * Everything above spawns bash and tar, so it only means something where
     * both exist and paths can contain a drive label. Windows itself is excluded
     * because the stand-in {@code cygpath} is a shell script there.
     */
    private static void assumePosixShell() {
        var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        assumeFalse(os.startsWith("windows"), "this test simulates a Windows path grammar on POSIX");
    }

    private record ScriptResult(int exitCode, String output) {
    }
}
