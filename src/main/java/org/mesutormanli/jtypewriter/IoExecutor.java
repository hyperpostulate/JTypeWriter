/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The single seam through which the application performs blocking file I/O.
 *
 * <p>The FX application thread must never block on a file system: a network
 * share can turn a save into a multi-second freeze, and even a local
 * {@code fsync} is unbounded. Every disk operation (document read/write,
 * backup rotation, recovery snapshot) is therefore submitted here, executed on
 * one dedicated daemon thread, and {@link #completeOnFx(Supplier) completed}
 * back on the FX thread only for the parts that touch UI state or dialogs.</p>
 *
 * <p><strong>One thread, on purpose.</strong> Document saves, backups and
 * autosave snapshots all mutate files in the same directories and the same
 * backup generations. A single worker serialises them without locks, so a
 * snapshot can never rotate a backup underneath a save that is still
 * renaming its temp file. UI work stays on the FX thread and is never
 * serialised behind disk I/O.</p>
 *
 * <p><strong>Observable by tests.</strong> {@link #submittedTasks()} counts
 * submitted operations and {@link #fxThreadTasks()} counts the ones that
 * actually ran on the FX thread. The production dispatcher/probe are
 * {@link Platform#runLater(Runnable)} /
 * {@link Platform#isFxApplicationThread()}; tests inject pure stand-ins so the
 * suite never has to start a toolkit. The contract is simple: no disk
 * operation may ever increment {@code fxThreadTasks}.</p>
 *
 * <p>{@link #synchronous()} is the headless/test configuration: work runs
 * inline on the caller and completions dispatch inline. Service objects
 * default to it so unit tests keep their deterministic, toolkit-free timing;
 * the production composition root ({@code AppGraph}) explicitly injects a
 * shared {@link #background()} executor instead.</p>
 */
public final class IoExecutor implements AutoCloseable {

    private final Executor executor;
    private final Consumer<Runnable> fxDispatcher;
    private final BooleanSupplier fxThreadProbe;
    private final AtomicInteger submittedTasks = new AtomicInteger();
    private final AtomicInteger fxThreadTasks = new AtomicInteger();

    /**
     * Uses the given executor for blocking work and the given dispatcher/probe
     * for the FX boundary. An {@link ExecutorService} is shut down by
     * {@link #close()}; a plain {@link Executor} is not owned by this object.
     */
    public IoExecutor(Executor executor, Consumer<Runnable> fxDispatcher,
                      BooleanSupplier fxThreadProbe) {
        this.executor = executor;
        this.fxDispatcher = fxDispatcher;
        this.fxThreadProbe = fxThreadProbe;
    }

    /** Production configuration: one serialised daemon worker, real FX dispatch. */
    public static IoExecutor background() {
        return new IoExecutor(serialExecutor(), Platform::runLater, Platform::isFxApplicationThread);
    }

    /**
     * Same serialised worker, but with an injected FX boundary. Used by tests
     * and by any future non-JavaFX caller: no toolkit is touched.
     */
    public static IoExecutor background(BooleanSupplier fxThreadProbe, Consumer<Runnable> fxDispatcher) {
        return new IoExecutor(serialExecutor(), fxDispatcher, fxThreadProbe);
    }

    /**
     * Runs work on the calling thread. Only for headless tests (and service
     * defaults used by them): it deliberately does not move anything off the
     * FX thread, which is why production never uses it.
     */
    public static IoExecutor synchronous() {
        return new IoExecutor(Runnable::run, Runnable::run, () -> false);
    }

    private static ExecutorService serialExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            private final AtomicInteger sequence = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                var thread = new Thread(runnable, "jtw-io-" + sequence.incrementAndGet());
                thread.setDaemon(true); // a stuck network write must never keep the app alive
                return thread;
            }
        });
    }

    /** Submits a blocking operation. The returned future completes on the worker. */
    public <T> CompletableFuture<T> submit(String operation, Callable<T> work) {
        submittedTasks.incrementAndGet();
        var future = new CompletableFuture<T>();
        executor.execute(() -> {
            if (fxThreadProbe.getAsBoolean()) {
                fxThreadTasks.incrementAndGet();
            }
            try {
                future.complete(work.call());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    /**
     * Runs {@code action} on the FX thread and completes the returned future
     * with its result. When the caller already is the FX thread (the common
     * case for a completion handler that follows a save) it runs inline, so no
     * pointless event-queue hop is introduced.
     */
    public <T> CompletableFuture<T> completeOnFx(Supplier<T> action) {
        if (fxThreadProbe.getAsBoolean()) {
            try {
                return CompletableFuture.completedFuture(action.get());
            } catch (Throwable failure) {
                return CompletableFuture.failedFuture(failure);
            }
        }
        var future = new CompletableFuture<T>();
        fxDispatcher.accept(() -> {
            try {
                future.complete(action.get());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    /** How many blocking operations were submitted through this executor. */
    public int submittedTasks() {
        return submittedTasks.get();
    }

    /**
     * How many of those ran on the FX thread. Must stay zero: a non-zero value
     * means a disk operation leaked back onto the UI thread.
     */
    public int fxThreadTasks() {
        return fxThreadTasks.get();
    }

    @Override
    public void close() {
        if (executor instanceof ExecutorService service) {
            service.shutdownNow();
        }
    }
}
