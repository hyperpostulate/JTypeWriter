/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The window's top-right corner as a single slot: at most one overlay card is
 * visible there at a time.
 *
 * <p><strong>Why one slot instead of stacking.</strong> Four views want that
 * corner - the find panel, the welcome card, the recent-files card and the
 * About card - and they are all taller than a comfortable offset stack: the
 * welcome card alone is 750 px on a 1080 px screen. Stacking them would push
 * the lower one off the window (or squeeze both), while placing them beside
 * each other would cover the editor twice. Hiding the one that is being
 * replaced also keeps the corner honest: whatever the user sees there is
 * exactly the card they last asked for, never a half-covered hybrid.</p>
 *
 * <p><strong>Why the members hide themselves.</strong> Each card owns its own
 * visibility, focus hand-back and state, so this object is only the rule that
 * turns "I am about to show" into "everyone else hides". It holds no JavaFX
 * type and is pure enough to test headlessly: {@link #aboutToShow(Object)}
 * is the whole behaviour.</p>
 */
public final class OverlaySlot {

    /**
     * Hide actions keyed by the owning card, in registration order. A
     * {@link LinkedHashMap} so the hide order - and therefore the focus
     * hand-back order when several cards are open - is deterministic.
     */
    private final Map<Object, Runnable> members = new LinkedHashMap<>();

    /**
     * Registers the action that hides {@code member}. Registering the same
     * member twice replaces its action instead of hiding it twice.
     */
    public void register(Object member, Runnable hide) {
        members.put(Objects.requireNonNull(member, "member"), Objects.requireNonNull(hide, "hide"));
    }

    /**
     * Hides every card except {@code shown}, which is about to become visible.
     * An owner that was never registered hides all of them, so a caller that
     * forgets to register can only cause an extra hide, never an overlap.
     */
    public void aboutToShow(Object shown) {
        for (var member : members.entrySet()) {
            if (member.getKey() != shown) {
                member.getValue().run();
            }
        }
    }
}
