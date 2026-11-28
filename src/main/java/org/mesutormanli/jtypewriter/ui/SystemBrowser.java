/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mesutormanli.jtypewriter.AppLogging;

/**
 * Hands an address to the user's own browser or mail client - the only ways the
 * application leaves the machine, and both of them are started by the user.
 *
 * <p><strong>Why nothing here runs on the FX application thread.</strong>
 * {@link Desktop#browse} is not a fire-and-forget call: on Linux it lands in
 * {@code sun.awt.X11.XDesktopPeer.launch}, which takes the AWT toolkit lock and
 * then calls the native {@code gnome_url_show} (GIO's
 * {@code g_app_info_launch_default_for_uri}). That call completes only when the
 * GTK main loop keeps turning - the D-Bus launch round-trip and the startup
 * notification are driven from it. In a JavaFX process that main loop is pumped
 * by the FX application thread itself ({@code GtkApplication._runLoop}), so
 * calling it there blocks the one thread that could let it finish: a permanent
 * self-deadlock, not a slow call. Measured on JDK 25 / JavaFX 26: from a plain
 * {@code main} thread the same call returns in ~120 ms, from the FX thread it
 * never returns, and a {@code jstack} taken 65 s later still shows
 * {@code JavaFX Application Thread ... RUNNABLE at
 * sun.awt.X11.XDesktopPeer.gnome_url_show(Native Method)} with {@code AWT-XAWT}
 * parked on {@code XToolkit.awtLock}. That is the "Click <em>Open release
 * page</em> and the window freezes" bug: the user cannot even close it.</p>
 *
 * <p>The fix is therefore a shape, not a timeout: the AWT call is made on a
 * short-lived daemon thread and only its <em>result</em> comes back, through an
 * injected dispatcher ({@code Platform::runLater} in production, {@code
 * Runnable::run} in tests). Off the FX thread the same call returns in
 * milliseconds, because the FX thread is free to keep pumping GTK while the
 * worker waits.</p>
 *
 * <p><strong>Two address families, two actions.</strong> {@code browse} takes
 * {@code http}/{@code https} and {@code mail} takes {@code mailto}: a caller
 * cannot hand an unexpected string to the platform's handler registry, which is
 * how a {@code file:} or {@code jar:} URL would otherwise become an invocation
 * of a local handler.</p>
 *
 * <p>Failure is reported as {@code false}, never as an exception and never as
 * silence: {@link Desktop} can be absent on a headless JVM, its action can be
 * unsupported on a bare desktop, and the native call can fail when no handler is
 * registered. The dialogs that use this already show the address as selectable
 * text, so the page stays reachable by copying it.</p>
 */
public final class SystemBrowser {

    private static final Logger LOG = AppLogging.logger(SystemBrowser.class);

    private SystemBrowser() {
    }

    /**
     * Opens an {@code http(s)} address in the default browser.
     *
     * <p>Returns immediately; {@code onResult} receives {@code true} when a
     * browser was handed the address. Both the work and the callback are
     * asynchronous by design - see the class comment for the deadlock a
     * synchronous call on the FX thread produces.</p>
     *
     * @param dispatcher where the callback must run (the FX thread in production)
     */
    public static void openAsync(String address, Consumer<Runnable> dispatcher,
                                 Consumer<Boolean> onResult) {
        if (!isWebAddress(address)) {
            LOG.log(Level.FINE, "refusing to open a non-web address");
            dispatcher.accept(() -> onResult.accept(false));
            return;
        }
        launchAsync(address, SystemBrowser::browseWithAwt, dispatcher, onResult);
    }

    /**
     * Hands a {@code mailto:} address to the default mail client, with the same
     * off-thread contract as {@link #openAsync}.
     */
    public static void mailAsync(String address, Consumer<Runnable> dispatcher,
                                 Consumer<Boolean> onResult) {
        if (!isMailAddress(address)) {
            LOG.log(Level.FINE, "refusing to open a non-mail address");
            dispatcher.accept(() -> onResult.accept(false));
            return;
        }
        launchAsync(address, SystemBrowser::mailWithAwt, dispatcher, onResult);
    }

    /**
     * The thread and dispatch half on its own, with the platform call injected,
     * so a test can prove that no {@link Desktop} work happens on the caller's
     * thread without opening a browser, a toolkit or a display.
     */
    static void launchAsync(String address, Predicate<String> launcher,
                            Consumer<Runnable> dispatcher, Consumer<Boolean> onResult) {
        Thread.ofPlatform().daemon().name("jtypewriter-open-address").start(() -> {
            boolean opened;
            try {
                opened = launcher.test(address);
            } catch (RuntimeException error) {
                LOG.log(Level.FINE, "could not open " + address, error);
                opened = false;
            }
            var result = opened;
            dispatcher.accept(() -> onResult.accept(result));
        });
    }

    /** Whether the text is an {@code http(s)} address. */
    static boolean isWebAddress(String address) {
        return hasScheme(address, "http://", "https://");
    }

    /** Whether the text is a {@code mailto} address. */
    static boolean isMailAddress(String address) {
        return hasScheme(address, "mailto:");
    }

    private static boolean hasScheme(String address, String... schemes) {
        if (address == null) {
            return false;
        }
        var normalised = address.strip().toLowerCase(Locale.ROOT);
        for (var scheme : schemes) {
            if (normalised.startsWith(scheme)) {
                return true;
            }
        }
        return false;
    }

    private static boolean browseWithAwt(String address) {
        return withDesktop(Desktop.Action.BROWSE, desktop -> desktop.browse(URI.create(address)), address);
    }

    private static boolean mailWithAwt(String address) {
        return withDesktop(Desktop.Action.MAIL, desktop -> desktop.mail(URI.create(address)), address);
    }

    /**
     * Runs one desktop action, turning every way it can fail into {@code false}.
     * Called from the worker thread, never from the FX thread.
     */
    private static boolean withDesktop(Desktop.Action action, DesktopCall call, String address) {
        try {
            if (!Desktop.isDesktopSupported()) {
                LOG.log(Level.FINE, "no desktop integration, cannot open " + address);
                return false;
            }
            var desktop = Desktop.getDesktop();
            if (!desktop.isSupported(action)) {
                LOG.log(Level.FINE, action + " is not supported on this desktop");
                return false;
            }
            call.accept(desktop);
            return true;
        } catch (Exception error) {
            // IOException from browse/mail, UnsupportedOperationException from a
            // half-implemented desktop, IllegalArgumentException from a URI the
            // platform rejects: all mean the same thing to the caller.
            LOG.log(Level.FINE, "could not open " + address, error);
            return false;
        }
    }

    /** {@link Desktop}'s two calls declare a checked exception; this one may. */
    @FunctionalInterface
    private interface DesktopCall {
        void accept(Desktop desktop) throws Exception;
    }
}
