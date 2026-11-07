/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.application.Platform;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Runs {@link FindModel#findAll(String, String, boolean)} off the FX thread and
 * hands the result back on it.
 *
 * <p>The scan is O(document length), and the panel is opened on documents up
 * to the editor's 10 MB ceiling. Scanning inline would freeze the window for
 * the duration of every keystroke in the query field, so the panel submits the
 * scan here and only installs the result once the dispatcher delivers it. The
 * model itself already refuses a result that no longer describes the current
 * document, query or case mode, so a late completion cannot select stale
 * text.</p>
 *
 * <p>Testable headlessly by construction: production wires the FX boundary to
 * {@link Platform#runLater(Runnable)} / {@link Platform#isFxApplicationThread()},
 * while a test may inject a plain executor, an inline dispatcher and a thread
 * probe. {@link #fxThreadScans()} must stay zero - a non-zero value means a
 * scan leaked back onto the UI thread.</p>
 */
public final class FindScanService implements AutoCloseable {

    private final Executor executor;
    private final Consumer<Runnable> fxDispatcher;
    private final BooleanSupplier fxThreadProbe;
    private final AtomicInteger fxThreadScans = new AtomicInteger();

    /**
     * Uses {@code executor} for the scan and {@code fxDispatcher} to deliver the
     * result. An {@link ExecutorService} is shut down by {@link #close()}; a
     * plain {@link Executor} is not owned by this object.
     */
    public FindScanService(Executor executor, Consumer<Runnable> fxDispatcher,
                           BooleanSupplier fxThreadProbe) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.fxDispatcher = Objects.requireNonNull(fxDispatcher, "fxDispatcher");
        this.fxThreadProbe = Objects.requireNonNull(fxThreadProbe, "fxThreadProbe");
    }

    /**
     * Production configuration: one daemon worker of its own.
     *
     * <p>Deliberately not the shared {@code IoExecutor}: a 10 MB scan must not
     * queue in front of a save, and the I/O worker serialises disk operations
     * for reasons that have nothing to do with searching.</p>
     */
    public static FindScanService background() {
        return new FindScanService(daemonExecutor(), Platform::runLater,
                Platform::isFxApplicationThread);
    }

    /**
     * Scans {@code document} for {@code needle} on the worker and delivers the
     * matches through {@code onResult}, which runs on the FX thread.
     *
     * <p>{@code onResult} may still run after the panel closed or the user
     * typed something newer; the caller must guard with a generation counter or
     * {@link FindModel#install(String, String, boolean, List)}.</p>
     */
    public void scan(String document, String needle, boolean caseSensitive,
                     Consumer<List<FindModel.Match>> onResult) {
        executor.execute(() -> {
            if (fxThreadProbe.getAsBoolean()) {
                fxThreadScans.incrementAndGet();
            }
            var matches = FindModel.findAll(document, needle, caseSensitive);
            fxDispatcher.accept(() -> onResult.accept(matches));
        });
    }

    /** Number of scans that actually executed on the FX thread; must stay zero. */
    public int fxThreadScans() {
        return fxThreadScans.get();
    }

    private static ExecutorService daemonExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            private final AtomicInteger sequence = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                var thread = new Thread(runnable, "jtw-find-" + sequence.incrementAndGet());
                thread.setDaemon(true); // a huge scan must never keep the app alive
                return thread;
            }
        });
    }

    @Override
    public void close() {
        if (executor instanceof ExecutorService service) {
            service.shutdownNow();
        }
    }
}
