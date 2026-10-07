package org.mesutormanli.jtypewriter.ui.component;

/**
 * Pure decision logic of YOLO mode. Deliberately free of JavaFX types so the
 * policy can be unit tested in headless environments where JavaFX controls
 * cannot be instantiated (no toolkit, no display).
 */
public final class YoloPolicy {

    /** What {@code EditorArea.replaceText} must do with a requested edit. */
    public enum EditAction {
        /** Handled unchanged by the superclass. */
        APPLY,
        /** Pure deletion - ignored while YOLO mode is on. */
        BLOCK,
        /** Replacement - insert the new text without removing the old one. */
        INSERT_WITHOUT_DELETION
    }

    private YoloPolicy() {
    }

    /**
     * @param yoloMode whether YOLO mode is active
     * @param start    start index of the replaced range
     * @param end      end index (exclusive) of the replaced range
     * @param text     the replacement text (may be {@code null})
     * @param length   current document length
     */
    public static EditAction decide(boolean yoloMode, int start, int end, String text, int length) {
        if (text == null || start < 0 || start > end || end > length) {
            return EditAction.APPLY; // let the superclass raise its usual NPE / IAE / IOOBE
        }
        if (!yoloMode || start == end) {
            return EditAction.APPLY;
        }
        if (text.isEmpty()) {
            return EditAction.BLOCK;
        }
        return EditAction.INSERT_WITHOUT_DELETION;
    }
}