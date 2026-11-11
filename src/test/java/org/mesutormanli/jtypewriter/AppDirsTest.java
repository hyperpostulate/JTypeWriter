/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The per-user directory layout must exist even when the launcher does not
 * provide a usable {@code user.home}. Throwing here would take the instance
 * lock, settings, drafts and logs down with it.
 */
class AppDirsTest {

    @Test
    void aBlankHomeFallsBackToATempBasedAbsoluteDataDir() {
        var original = System.getProperty("user.home");
        try {
            for (var blank : new String[]{"", "   "}) {
                System.setProperty("user.home", blank);

                var dataDir = AppDirs.dataDir();

                assertTrue(dataDir.isAbsolute(), "the fallback must be absolute: " + dataDir);
                assertEquals(".jtypewriter", dataDir.getFileName().toString(),
                        "the layout name is kept even on the fallback: " + dataDir);
            }
        } finally {
            restoreHome(original);
        }
    }

    @Test
    void aRealHomeProducesTheExpectedLayout(@TempDir Path home) {
        var original = System.getProperty("user.home");
        try {
            System.setProperty("user.home", home.toString());

            var dataDir = AppDirs.dataDir();

            assertEquals(home.resolve(".jtypewriter"), dataDir);
            assertEquals(dataDir.resolve("recovery"), AppDirs.recoveryDir());
            assertEquals(dataDir.resolve("logs"), AppDirs.logsDir());
        } finally {
            restoreHome(original);
        }
    }

    private static void restoreHome(String original) {
        if (original == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", original);
        }
    }
}
