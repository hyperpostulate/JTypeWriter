/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The check's plumbing, not its decision: the supplied address and the injected
 * dispatcher stand in for GitHub and the FX thread, so no test opens a socket or
 * starts a toolkit.
 */
class UpdateServiceTest {

    private static final String TAG_PAGE =
            "https://github.com/hyperpostulate/JTypeWriter/releases/tag/v1.1.6";

    @Test
    void aNewerReleaseIsDeliveredToTheDispatcher() {
        var service = new UpdateService("1.1.5", Runnable::run,
                () -> Optional.of(TAG_PAGE));
        var result = new CompletableFuture<UpdateCheck.Result>();

        service.checkAsync(result::complete);

        var answered = result.orTimeout(5, TimeUnit.SECONDS).join();
        assertEquals(UpdateCheck.Status.NEWER, answered.status());
        assertEquals("1.1.6", answered.latestVersion());
        assertFalse(service.isChecking());
    }

    @Test
    void anUnreadableAddressBecomesAFailedCheckRatherThanAnException() {
        var service = new UpdateService("1.1.5", Runnable::run,
                Optional::<String>empty);
        var result = new CompletableFuture<UpdateCheck.Result>();

        service.checkAsync(result::complete);

        assertEquals(UpdateCheck.Status.FAILED,
                result.orTimeout(5, TimeUnit.SECONDS).join().status());
    }

    @Test
    void aSupplierThatThrowsIsAlsoJustAFailedCheck() {
        var service = new UpdateService("1.1.5", Runnable::run,
                () -> { throw new IllegalStateException("no network"); });
        var result = new CompletableFuture<UpdateCheck.Result>();

        service.checkAsync(result::complete);

        assertEquals(UpdateCheck.Status.FAILED,
                result.orTimeout(5, TimeUnit.SECONDS).join().status(),
                "a menu click must never surface a stack trace");
    }

    @Test
    void aSecondClickWhileACheckIsRunningIsIgnoredRatherThanQueued() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var results = new AtomicInteger();
        Consumer<UpdateCheck.Result> counter = ignored -> results.incrementAndGet();

        var service = new UpdateService("1.1.5", Runnable::run, () -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return Optional.of(TAG_PAGE);
        });

        service.checkAsync(counter);
        assertTrue(entered.await(5, TimeUnit.SECONDS), "the check never started");
        service.checkAsync(counter);
        release.countDown();

        // The first check's result arrives asynchronously; only the count matters.
        var deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (results.get() == 0 && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(1, calls.get(), "the second click must not start a second request");
        assertEquals(1, results.get(), "exactly one result is delivered");
    }
}
