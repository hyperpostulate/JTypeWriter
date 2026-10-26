/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

/**
 * What an exit path may do with this session's recovery draft.
 *
 * <p>The draft is often the only copy of a document the user never saved, so
 * the decision cannot be left to whichever lifecycle event happens to fire
 * first. Two signals decide it:</p>
 *
 * <ul>
 *   <li><strong>Was a close request answered?</strong> Only the window's close
 *       request ({@code MainStage}) may delete the draft, because only that
 *       path asked the user about unsaved changes and got an answer ("save"
 *       that succeeded, or an explicit "don't save"). {@code Platform.exit()},
 *       a session log-out or a plain {@code stage.hide()} never asked anything,
 *       so they must not delete the text either.</li>
 *   <li><strong>Is the slot frozen?</strong> A postponed draft whose parking
 *       failed still occupies the only slot; touching it would overwrite the
 *       text the user asked to keep.</li>
 * </ul>
 *
 * <p>Kept as a pure enum next to {@link RecoveryChoice} so the whole exit
 * matrix is testable without a toolkit or a stage.</p>
 */
public enum ExitDisposition {

    /**
     * A close request was answered in favour of closing: the document is on
     * disk or the user explicitly discarded it, so the draft may go.
     */
    CLEAR,

    /**
     * No close request was answered - the process is going away without
     * asking. Never delete: move the draft out of the live slot so the next
     * start offers it for recovery.
     */
    PARK,

    /**
     * The slot holds a postponed draft that could not be parked. Leave the
     * file exactly as it is; it is the only copy of that text.
     */
    KEEP;

    /**
     * The exit matrix. A frozen slot wins over every other signal: it means a
     * postponed draft is still sitting in the slot.
     */
    static ExitDisposition decide(boolean closeRequestAnswered, boolean draftFrozen) {
        if (draftFrozen) {
            return KEEP;
        }
        return closeRequestAnswered ? CLEAR : PARK;
    }
}
