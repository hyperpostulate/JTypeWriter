/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import java.awt.Desktop;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mesutormanli.jtypewriter.AppLogging;

/**
 * Hands an https address to the user's own browser - the only way the
 * application leaves the machine other than the mail link in the About card.
 *
 * <p>One implementation, because the same two failure modes have to be handled
 * the same way everywhere: {@link Desktop} is absent on a headless JVM and
 * unsupported under some Linux desktops, and {@code browse} throws
 * {@link java.io.IOException} when no browser is registered. Both are logged at
 * {@code FINE} and reported to the caller as {@code false} - a user who asked to
 * open a page deserves a "could not open" sentence, not a stack trace and not
 * silence. The dialogs that use this already show the address as text, so the
 * page is still reachable by copying it.</p>
 *
 * <p>The address is checked before it is handed over: only {@code http} and
 * {@code https} leave this class, so nothing can turn an unexpected string into
 * a {@code file:} or {@code jar:} handler invocation.</p>
 */
public final class SystemBrowser {

    private static final Logger LOG = AppLogging.logger(SystemBrowser.class);

    private SystemBrowser() {
    }

    /**
     * Opens the address in the default browser.
     *
     * @return whether a browser was handed the address
     */
    public static boolean open(String address) {
        if (!isWebAddress(address)) {
            LOG.log(Level.FINE, "refusing to open a non-web address");
            return false;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                LOG.log(Level.FINE, "no desktop integration, cannot open " + address);
                return false;
            }
            var desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                LOG.log(Level.FINE, "browsing is not supported on this desktop");
                return false;
            }
            desktop.browse(URI.create(address));
            return true;
        } catch (Exception error) {
            // IOException from browse, UnsupportedOperationException from a
            // half-implemented desktop, IllegalArgumentException from a URI the
            // platform rejects: all mean the same thing to the caller.
            LOG.log(Level.FINE, "could not open " + address, error);
            return false;
        }
    }

    static boolean isWebAddress(String address) {
        if (address == null) {
            return false;
        }
        var lower = address.strip().toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }
}
