/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

/**
 * The one question the find panel asks {@code YoloPolicy} before it rewrites
 * text: may a replacement remove the characters it matched?
 *
 * <p>YOLO mode's promise is that no user edit removes existing characters
 * (see {@code YoloPolicy}); a replacement is a deletion followed by an
 * insertion, so it is exactly the kind of edit that promise covers. The panel
 * therefore asks for explicit consent once per operation instead of routing the
 * edit through {@code EditorArea.replaceText}, where YOLO would turn it into an
 * insertion and duplicate the text instead of replacing it. Kept pure and
 * JavaFX-free so the rule is testable headlessly, in the same idiom as
 * {@code YoloPolicy}.</p>
 */
public final class FindReplacePolicy {

    /** What the panel must do before applying a replacement. */
    public enum Confirmation {
        /** No prompt: the document is not protected. */
        NONE,
        /** Ask once; only an explicit yes applies the deletion. */
        REQUIRED
    }

    private FindReplacePolicy() {
    }

    /**
     * A replacement always needs the user's explicit blessing while YOLO mode
     * is on; with YOLO off it is an ordinary edit and asks nothing.
     */
    public static Confirmation replacement(boolean yoloMode) {
        return yoloMode ? Confirmation.REQUIRED : Confirmation.NONE;
    }
}
