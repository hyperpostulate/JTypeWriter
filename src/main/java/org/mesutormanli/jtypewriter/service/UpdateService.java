/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mesutormanli.jtypewriter.AppLogging;
import org.mesutormanli.jtypewriter.AppVersion;

/**
 * The network half of the update check: one GET against GitHub's
 * {@code /releases/latest}, on a thread of its own, whose only output is an
 * {@link UpdateCheck.Result}.
 *
 * <p><strong>It never runs on its own.</strong> The application makes no request
 * at startup and none in the background: this is reachable only through the
 * Help menu's "Check for updates", so the one outbound connection the product
 * makes is one the user asked for. That is also what the README promises, and
 * the wording there was changed together with this class rather than left
 * claiming the opposite.</p>
 *
 * <p><strong>Why its own thread, not {@code IoExecutor}.</strong> The I/O worker
 * is deliberately a single serialised thread because document saves and backup
 * rotation must not interleave. A stalled network read has no business behind
 * or in front of a save, so the check gets a daemon thread that dies with the
 * process and cannot delay writing to disk by even one rename.</p>
 *
 * <p><strong>Timeouts, not patience.</strong> Five seconds to connect and five to
 * read: a captive portal or a blocked host must turn into "could not check" in
 * seconds rather than leaving the menu item disabled for a minute.</p>
 *
 * <p><strong>What the user's network sees.</strong> The request goes to
 * {@code github.com} and carries a normal GitHub-bound {@code User-Agent}
 * ({@code JTypeWriter/<version>}); like any HTTPS request, the host sees the
 * machine's IP address. Nothing about the document, the file name or the
 * preferences is sent - the URL is a constant and the only header that mentions
 * this application is the version already visible in the downloaded artefact.
 * {@code THIRD-PARTY-NOTICES} needs no entry: no library is involved.</p>
 *
 * <p><strong>Testable without a socket.</strong> Tests pass a supplier returning
 * a canned address and a synchronous dispatcher, so the decision path -
 * including the "a check is already running" guard - is covered headlessly,
 * with no toolkit and no network. The dispatcher is injected rather than
 * imported so this package stays JavaFX-free: {@code AppGraph} supplies
 * {@code Platform::runLater}, exactly as it supplies {@code IoExecutor}'s FX
 * boundary.</p>
 */
public final class UpdateService {

    /** Where the newest published release lives; GitHub redirects this to its tag. */
    public static final String LATEST_RELEASE_PAGE =
            "https://github.com/hyperpostulate/JTypeWriter/releases/latest";

    /** The releases index, used by the About card and the failure dialog. */
    public static final String RELEASES_PAGE =
            "https://github.com/hyperpostulate/JTypeWriter/releases";

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    private static final Logger LOG = AppLogging.logger(UpdateService.class);

    private final String currentVersion;
    private final Consumer<Runnable> fxDispatcher;
    private final Supplier<Optional<String>> latestReleasePage;
    private final AtomicBoolean inFlight = new AtomicBoolean();

    /** Production wiring: the running version, real GitHub, the caller's FX thread. */
    public static UpdateService background(Consumer<Runnable> fxDispatcher) {
        return new UpdateService(AppVersion.get(), fxDispatcher, UpdateService::fetchLatestReleasePage);
    }

    /**
     * @param currentVersion      the version this build runs
     * @param fxDispatcher        where the callback is delivered (the FX thread
     *                            in production, inline in tests)
     * @param latestReleasePage   resolves the address the newest release
     *                            redirects to; empty when the check failed
     */
    public UpdateService(String currentVersion, Consumer<Runnable> fxDispatcher,
                         Supplier<Optional<String>> latestReleasePage) {
        this.currentVersion = currentVersion;
        this.fxDispatcher = fxDispatcher;
        this.latestReleasePage = latestReleasePage;
    }

    /** Whether a check is running; the menu item is disabled while it is. */
    public boolean isChecking() {
        return inFlight.get();
    }

    /**
     * Starts a check unless one is already running, and delivers exactly one
     * result to {@code onResult} on the injected FX dispatcher. A second call
     * while a check is in flight is ignored rather than queued: the answer would
     * be identical and the user only clicked twice.
     */
    public void checkAsync(Consumer<UpdateCheck.Result> onResult) {
        if (!inFlight.compareAndSet(false, true)) {
            return;
        }
        Thread.ofPlatform().daemon().name("jtypewriter-update-check").start(() -> {
            UpdateCheck.Result result;
            try {
                result = latestReleasePage.get()
                        .flatMap(page -> UpdateCheck.evaluate(page, currentVersion))
                        .orElseGet(UpdateCheck.Result::failed);
            } catch (RuntimeException error) {
                // A supplier is allowed to throw (a malformed address, an
                // unexpected runtime failure in the connection layer); that is
                // a failed check, never an exception on a menu click.
                LOG.log(Level.FINE, "the update check could not be completed", error);
                result = UpdateCheck.Result.failed();
            } finally {
                // Cleared before the dispatch so the menu is live again by the
                // time the user can click anything.
                inFlight.set(false);
            }
            var completed = result;
            fxDispatcher.accept(() -> onResult.accept(completed));
        });
    }

    /**
     * Follows GitHub's {@code /releases/latest} redirect and returns the address
     * it ended at - the release's tag page. Empty on any non-success: the caller
     * reports "could not check", which is a different sentence from "up to date"
     * on purpose.
     */
    private static Optional<String> fetchLatestReleasePage() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(LATEST_RELEASE_PAGE).toURL().openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            // GitHub rejects a request without a User-Agent; naming the version
            // is the least it can say and is already public in the download.
            connection.setRequestProperty("User-Agent", "JTypeWriter/" + AppVersion.get());
            // Ask for the HTML page, not the API representation.
            connection.setRequestProperty("Accept", "text/html");

            var code = connection.getResponseCode();
            if (code >= 200 && code < 300) {
                return Optional.of(connection.getURL().toString());
            }
            if (code >= 300 && code < 400) {
                // A redirect the JDK refused to follow across protocols still
                // names its destination; resolve it against the request so the
                // comparison does not depend on that detail.
                var location = connection.getHeaderField("Location");
                if (location == null || location.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(URI.create(LATEST_RELEASE_PAGE).resolve(location).toString());
            }
            LOG.log(Level.FINE, "the release page answered with HTTP " + code);
            return Optional.empty();
        } catch (IOException | RuntimeException error) {
            LOG.log(Level.FINE, "the release page could not be read", error);
            return Optional.empty();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
