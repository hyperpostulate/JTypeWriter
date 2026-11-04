/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mesutormanli.jtypewriter.IoExecutor;
import org.mesutormanli.jtypewriter.locale.Messages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The FX thread must never execute file I/O. These tests stand in for the
 * JavaFX toolkit with a pure thread probe: production wires the same seam to
 * {@code Platform.isFxApplicationThread()} / {@code Platform.runLater}, but a
 * test may not start a toolkit. {@link IoExecutor#fxThreadTasks()} is the
 * counter on the I/O wrapper: every disk call goes through it, so a non-zero
 * value means a save, load or snapshot leaked back onto the UI thread.
 */
class FxThreadIoTest {

    @Test
    void submittedWorkRunsOnTheIoWorkerNotOnTheFxThread() {
        var fxThread = Thread.currentThread(); // pretend the JUnit thread is the FX thread
        try (var io = IoExecutor.background(() -> Thread.currentThread() == fxThread,
                Runnable::run)) {
            var ranOnFxThread = io.submit("probe", () -> Thread.currentThread() == fxThread)
                    .join();

            assertFalse(ranOnFxThread, "the disk worker must not be the FX thread");
            assertEquals(1, io.submittedTasks());
            assertEquals(0, io.fxThreadTasks(), "no I/O may be counted on the FX thread");
        }
    }

    @Test
    void savingADocumentWritesThroughTheIoWorkerOnly(@TempDir Path dir) throws Exception {
        var fxThread = Thread.currentThread();
        try (var io = IoExecutor.background(() -> Thread.currentThread() == fxThread,
                Runnable::run)) {
            var dialogService = mock(DialogService.class);
            var service = new FileService(dialogService, mock(Messages.class),
                    new BackupKeeper(), io);
            var target = dir.resolve("async-save.txt");
            when(dialogService.showSaveDialog(any(), any()))
                    .thenReturn(Optional.of(target.toFile()));

            var saved = service.saveFileAsync(null, "written off the FX thread").join();

            assertTrue(saved.isPresent());
            assertEquals("written off the FX thread", Files.readString(target));
            assertTrue(io.submittedTasks() >= 1, "the save must be submitted on the I/O seam");
            assertEquals(0, io.fxThreadTasks(), "FileChannel/fsync never ran on the FX thread");
        }
    }

    @Test
    void openingADocumentReadsThroughTheIoWorkerOnly(@TempDir Path dir) throws Exception {
        var fxThread = Thread.currentThread();
        try (var io = IoExecutor.background(() -> Thread.currentThread() == fxThread,
                Runnable::run)) {
            var target = dir.resolve("async-open.txt");
            Files.writeString(target, "from disk");
            var service = new FileService(mock(DialogService.class), mock(Messages.class),
                    new BackupKeeper(), io);

            var loaded = service.loadFileAsync(null, target).join();

            assertTrue(loaded.isPresent());
            assertEquals("from disk", service.getCurrentContent());
            assertEquals(0, io.fxThreadTasks());
        }
    }

    /**
     * The autosave tick used to call {@code editorArea.getText()} before asking
     * whether anything was dirty, i.e. a 10 MB copy every 30 seconds to then
     * discover there was no work. The supplier is now evaluated only after the
     * dirty check; the assertion fails the tick if a clean document reaches it.
     */
    @Test
    void aCleanAutosaveTickNeverCopiesTheDocumentText() {
        var store = mock(RecoveryStore.class);
        when(store.clear()).thenReturn(true);
        var fileService = mock(FileService.class);
        when(fileService.hasUnsavedChanges()).thenReturn(false);
        var service = new AutoSaveService(store, fileService,
                mock(DialogService.class), mock(Messages.class));

        Supplier<String> forbiddenCopy = () -> {
            throw new AssertionError("a clean tick must not copy the document text");
        };

        service.snapshot(forbiddenCopy);

        verify(store).clear();
    }

    /**
     * Moving the write off the FX thread opens a window in which the user can
     * type. The completion must record what actually reached the disk as the
     * saved text while keeping the newer editor text - otherwise a slow save
     * would either mark a changed document clean or roll the editor back.
     */
    @Test
    void typingWhileASaveIsInFlightKeepsTheNewerTextDirty(@TempDir Path dir) throws Exception {
        var queue = new QueueingExecutor();
        var io = new IoExecutor(queue, Runnable::run, () -> false);
        var dialogService = mock(DialogService.class);
        var service = new FileService(dialogService, mock(Messages.class),
                new BackupKeeper(), io);
        var target = dir.resolve("slow-save.txt");
        when(dialogService.showSaveDialog(any(), any()))
                .thenReturn(Optional.of(target.toFile()));
        service.setCurrentContent("first version");

        var saving = service.saveFileAsync(null, "first version");

        // The I/O is still queued: the FX thread is free to accept more typing.
        service.setCurrentContent("first version, plus one more sentence");
        queue.runAll();
        assertTrue(saving.join().isPresent());

        assertEquals("first version", Files.readString(target),
                "the save writes the snapshot it started with");
        assertEquals("first version, plus one more sentence", service.getCurrentContent(),
                "a finished save must not roll the editor back");
        assertTrue(service.hasUnsavedChanges(), "text typed during the save is still unsaved");
    }

    /** An executor whose queued work runs only when the test says so. */
    private static final class QueueingExecutor implements java.util.concurrent.Executor {
        private final java.util.ArrayDeque<Runnable> tasks = new java.util.ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        void runAll() {
            Runnable task;
            while ((task = tasks.poll()) != null) {
                task.run();
            }
        }
    }
}
