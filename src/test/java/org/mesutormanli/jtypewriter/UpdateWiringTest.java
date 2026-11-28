/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The update check's wiring contract, checked in the sources rather than by
 * starting a window: the promise "nothing leaves the machine unless you ask"
 * is a property of where the code is reachable from, so that is what is
 * asserted.
 *
 * <p>Three claims, each one a way the promise could quietly break:</p>
 * <ul>
 *   <li>no class outside {@code UpdateService} opens a connection, so a new
 *       feature cannot make the product phone home as a side effect;</li>
 *   <li>the request is started only from the Help entry, so nothing at startup,
 *       on a timer or in the background can call it;</li>
 *   <li>the entry sits directly under About, which is where the user was told
 *       to look for it.</li>
 * </ul>
 *
 * <p>Read-only and headless: source text only, no toolkit, no socket.</p>
 */
class UpdateWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "org", "mesutormanli",
            "jtypewriter");

    @Test
    void onlyTheUpdateServiceOpensAConnection() {
        var offenders = new ArrayList<String>();
        for (var path : sources()) {
            var name = path.getFileName().toString();
            if (name.equals("UpdateService.java")) {
                continue;
            }
            var source = read(path);
            for (var marker : List.of("HttpURLConnection", "HttpClient", "URLConnection",
                    "openConnection(", "openStream(")) {
                if (source.contains(marker)) {
                    offenders.add(name + " (" + marker + ")");
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "these sources reach the network outside UpdateService: " + offenders);
    }

    @Test
    void theRequestIsStartedOnlyFromTheMenuItem() {
        // The controller installs the runner; the check itself is started in
        // exactly one method, reached from that runner.
        var controller = read(MAIN.resolve("ui").resolve("MainController.java"));
        assertEquals(1, count(controller, "updateService.checkAsync("),
                "the check should be started in one place only");
        assertTrue(controller.contains("mainMenu.setUpdateRunner(this::checkForUpdates)"),
                "the Help entry is not wired to the controller's check");
        assertTrue(controller.contains("private void checkForUpdates()"),
                "the check is started from something other than the menu runner");

        // No startup, timer or lifecycle hook may call it.
        var graph = read(MAIN.resolve("AppGraph.java"));
        assertFalse(graph.contains("checkAsync("),
                "the graph starts a check at construction instead of on a click");
        assertFalse(graph.contains("scheduleAtFixedRate("),
                "a background poll is not an opt-in check");
        assertTrue(graph.contains("UpdateService.background("),
                "the update service is not assembled in the composition root");
    }

    @Test
    void theHelpEntrySitsDirectlyUnderAbout() {
        var menu = read(MAIN.resolve("ui").resolve("MainMenu.java"));
        var helpBlock = menu.substring(menu.indexOf("if (spec.page() == Page.HELP)"));
        helpBlock = helpBlock.substring(0, helpBlock.indexOf('}') + 1);
        assertTrue(helpBlock.contains("aboutItem(), updateItem()"),
                "the update entry must be added immediately after About: " + helpBlock);
    }

    private static List<Path> sources() {
        try (Stream<Path> walk = Files.walk(MAIN)) {
            return walk.filter(path -> path.toString().endsWith(".java")).toList();
        } catch (IOException error) {
            throw new UncheckedIOException("cannot walk the main sources", error);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException("cannot read " + path, error);
        }
    }

    private static int count(String haystack, String needle) {
        var total = 0;
        var at = haystack.indexOf(needle);
        while (at >= 0) {
            total++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return total;
    }
}
