/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scan is O(document length), so it must never run on the FX thread and its
 * result must only be installed through the dispatcher. The production boundary
 * uses {@code Platform.isFxApplicationThread()} / {@code Platform.runLater};
 * these tests inject an executor, a queue and a thread probe so the rule can be
 * proven without starting a toolkit.
 */
class FindScanServiceTest {

    @Test
    void theScanRunsOffTheFxThreadAndTheResultArrivesThroughTheDispatcher() throws Exception {
        var fxThread = Thread.currentThread(); // pretend the JUnit thread is the FX thread
        var worker = Executors.newSingleThreadExecutor();
        var scanThread = new AtomicReference<Thread>();
        var pending = new ArrayDeque<Runnable>();
        var enqueued = new CountDownLatch(1);
        Executor recordingWorker = task -> worker.execute(() -> {
            scanThread.set(Thread.currentThread());
            task.run();
        });

        try (var service = new FindScanService(recordingWorker,
                task -> {
                    pending.add(task);
                    enqueued.countDown();
                },
                () -> Thread.currentThread() == fxThread)) {

            var delivered = new AtomicReference<List<FindModel.Match>>();
            service.scan("aaaa", "aa", true, delivered::set);

            assertTrue(enqueued.await(5, TimeUnit.SECONDS), "the scan never finished");
            assertFalse(scanThread.get() == fxThread, "the scan must not run on the FX thread");
            assertEquals(0, service.fxThreadScans(), "a scan leaked onto the FX thread");
            assertNull(delivered.get(), "the result must wait for the FX dispatcher");

            var delivery = pending.poll();
            assertNotNull(delivery, "no delivery was dispatched");
            delivery.run();

            assertEquals(List.of(new FindModel.Match(0, 2), new FindModel.Match(2, 4)),
                    delivered.get());
        } finally {
            worker.shutdownNow();
        }
    }

    /**
     * A stale-safe scan: the result is a snapshot that cannot overlap itself,
     * so a later replace pass can apply every range exactly once.
     */
    @Test
    void scanningAaaaForAaReportsTwoNonOverlappingMatches() throws Exception {
        var worker = Executors.newSingleThreadExecutor();
        try (var service = new FindScanService(worker, Runnable::run, () -> false)) {
            var result = new CompletableFuture<List<FindModel.Match>>();
            service.scan("aaaa", "aa", true, result::complete);

            assertEquals(List.of(new FindModel.Match(0, 2), new FindModel.Match(2, 4)),
                    result.get(5, TimeUnit.SECONDS));
            assertEquals(0, service.fxThreadScans());
        } finally {
            worker.shutdownNow();
        }
    }
}
