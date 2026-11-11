/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import java.util.Optional;

/**
 * Whether the autosave safety net is currently working, as a plain, JavaFX-free
 * value.
 *
 * <p>Autosave used to fail by writing one {@code WARNING} line to a log file
 * nobody reads: the recovery directory could become read-only (full disk, a
 * permissions change, a synced folder) and the application kept running with
 * its single promise silently withdrawn. Every failure path now records a
 * {@link Problem} here, so the status bar can say "autosave is not working"
 * for as long as it is true, and the first occurrence can raise a dialog.</p>
 *
 * <p>The state is deliberately sticky, not sampled: a failure stands until a
 * later snapshot or park actually succeeds, at which point the warning
 * disappears on its own. Listeners fire on real transitions only, so a tick
 * that keeps failing does not repaint the bar every 30 seconds.</p>
 */
public final class AutosaveHealth {

    /** The distinct ways the safety net can stop working. */
    public enum Problem {
        /** A periodic snapshot could not be written to the recovery directory. */
        DRAFT_WRITE,
        /**
         * A postponed draft could not be moved out of the live slot, so
         * snapshots stay suppressed to protect its only copy.
         */
        DRAFT_PARK,
        /**
         * A draft that should have been dropped - after a save, a new document
         * or an answered exit - could not be deleted, so a stale copy stays on
         * disk and would be offered for recovery again.
         */
        DRAFT_DELETE
    }

    private Problem current;
    private Runnable listener = () -> {
    };

    /** {@code true} while at least one failure stands. */
    public boolean isFailing() {
        return current != null;
    }

    /** The failure that currently stands, empty while the safety net works. */
    public Optional<Problem> problem() {
        return Optional.ofNullable(current);
    }

    /** Records a failure; notifies only when the state or the problem changes. */
    void fail(Problem problem) {
        if (current == problem) {
            return;
        }
        current = problem;
        listener.run();
    }

    /**
     * Clears the warning only when {@code problem} is the one currently
     * standing. A successful snapshot says nothing about an earlier failed
     * delete, so withdrawing on any success would hide a stale draft that is
     * still on disk. Notifies on the transition only.
     */
    void clear(Problem problem) {
        if (current == null || current != problem) {
            return;
        }
        current = null;
        listener.run();
    }

    /**
     * Records that the safety net is working again, whatever had failed.
     *
     * <p>Kept for callers that genuinely mean "nothing is wrong any more"; a
     * caller reporting a specific success should use {@link #clear(Problem)}
     * so one subsystem's success cannot erase another's warning.</p>
     */
    void recover() {
        if (current == null) {
            return;
        }
        current = null;
        listener.run();
    }

    /** Registers the consumer that repaints whatever displays this state. */
    public void addListener(Runnable listener) {
        this.listener = listener == null ? () -> {
        } : listener;
    }
}
