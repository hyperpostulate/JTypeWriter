/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The address launcher's threading contract, without opening anything: the
 * platform call is injected, so these tests never touch {@code Desktop}, a
 * toolkit or a display.
 *
 * <p>The contract is the fix for a real hang: {@code Desktop.browse} on Linux
 * enters a native GTK call that only completes while the GTK main loop - the FX
 * thread in a JavaFX process - keeps turning, so calling it on that thread
 * deadlocks the window permanently. Every assertion here is about that: the
 * caller returns at once, the work happens on another thread, and the answer
 * comes back through the dispatcher.</p>
 */
class SystemBrowserTest {

    @Test
    void theCallerNeverRunsThePlatformCallItself() {
        var caller = Thread.currentThread();
        var ranOn = new AtomicReference<Thread>();
        var result = new CompletableFuture<Boolean>();

        SystemBrowser.launchAsync("https://example.com",
                address -> {
                    ranOn.set(Thread.currentThread());
                    return true;
                },
                Runnable::run, result::complete);

        assertTrue(result.orTimeout(5, TimeUnit.SECONDS).join());
        assertNotNull(ranOn.get());
        assertFalse(ranOn.get() == caller,
                "the platform call must not run on the thread that asked for it");
        assertFalse(ranOn.get().isVirtual(),
                "a plain daemon thread is enough for a click-driven action");
    }

    @Test
    void aPlatformFailureArrivesAsFalseRatherThanAnException() {
        var result = new CompletableFuture<Boolean>();

        SystemBrowser.launchAsync("https://example.com",
                address -> {
                    throw new IllegalStateException("no browser");
                },
                Runnable::run, result::complete);

        assertFalse(result.orTimeout(5, TimeUnit.SECONDS).join(),
                "a broken desktop must report a failure, not throw at the click");
    }

    @Test
    void anAddressOutsideTheSupportedSchemeIsRefused() {
        assertTrue(SystemBrowser.isWebAddress("https://github.com/x"));
        assertTrue(SystemBrowser.isWebAddress("HTTP://example.com"));
        assertFalse(SystemBrowser.isWebAddress("file:///etc/passwd"));
        assertFalse(SystemBrowser.isWebAddress("javascript:alert(1)"));
        assertFalse(SystemBrowser.isWebAddress("example.com"));
        assertFalse(SystemBrowser.isWebAddress(null));

        assertTrue(SystemBrowser.isMailAddress("mailto:someone@example.com"));
        assertFalse(SystemBrowser.isMailAddress("https://example.com"));
        assertFalse(SystemBrowser.isMailAddress(null));
    }

    @Test
    void aRefusedSchemeStillAnswersThroughTheDispatcher() {
        var result = new CompletableFuture<Boolean>();

        SystemBrowser.openAsync("file:///etc/passwd", Runnable::run, result::complete);

        assertFalse(result.orTimeout(5, TimeUnit.SECONDS).join());
    }

    @Test
    void mailIsRefusedForAWebAddressAndViceVersa() {
        var web = new CompletableFuture<Boolean>();
        var mail = new CompletableFuture<Boolean>();

        SystemBrowser.mailAsync("https://example.com", Runnable::run, web::complete);
        SystemBrowser.openAsync("mailto:someone@example.com", Runnable::run, mail::complete);

        assertFalse(web.orTimeout(5, TimeUnit.SECONDS).join());
        assertFalse(mail.orTimeout(5, TimeUnit.SECONDS).join());
    }

    /**
     * {@code Desktop} may be named in one file only: this one. The point of that
     * rule is the threading contract above - a second call site would be a
     * second chance to put the native GTK call back on the FX thread, where it
     * never returns. Source scan, so it needs no display.
     */
    @Test
    void noClassOutsideSystemBrowserTouchesTheAwtdesktop() {
        var offenders = new java.util.ArrayList<String>();
        try (var sources = java.nio.file.Files.walk(java.nio.file.Path.of("src", "main", "java"))) {
            for (var path : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (path.getFileName().toString().equals("SystemBrowser.java")) {
                    continue;
                }
                var text = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
                if (text.contains("java.awt.Desktop") || text.contains("Desktop.getDesktop(")) {
                    offenders.add(path.getFileName().toString());
                }
            }
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException("cannot scan the main sources", error);
        }
        assertTrue(offenders.isEmpty(),
                "java.awt.Desktop is reachable from these files; route them through "
                        + "SystemBrowser instead: " + offenders);
    }
}
