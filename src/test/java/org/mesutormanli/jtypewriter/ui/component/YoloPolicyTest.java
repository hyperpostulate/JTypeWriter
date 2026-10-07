package org.mesutormanli.jtypewriter.ui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for the YOLO mode contract: while it is on, no user edit may
 * remove existing characters - deletions are blocked, insertions keep working,
 * and replacements insert without deleting.
 *
 * <p>The policy is intentionally pure (no JavaFX types) so these tests run in
 * headless environments where JavaFX controls cannot be instantiated.</p>
 */
class YoloPolicyTest {

    private static final int LENGTH = 11; // "Hello World"

    @Test
    void deletionsApplyWhenYoloModeIsOff() {
        assertEquals(YoloPolicy.EditAction.APPLY,
                YoloPolicy.decide(false, 0, 5, "", LENGTH));
    }

    @Test
    void deletionsAreBlockedInYoloMode() {
        // Backspace / Delete single character
        assertEquals(YoloPolicy.EditAction.BLOCK,
                YoloPolicy.decide(true, 10, 11, "", LENGTH));
        // deleting a selection
        assertEquals(YoloPolicy.EditAction.BLOCK,
                YoloPolicy.decide(true, 0, 5, "", LENGTH));
    }

    @Test
    void insertionsAreAlwaysAllowedInYoloMode() {
        assertEquals(YoloPolicy.EditAction.APPLY,
                YoloPolicy.decide(true, LENGTH, LENGTH, "!", LENGTH));
    }

    @Test
    void replacementsInsertWithoutDeletingInYoloMode() {
        // typing over a selection
        assertEquals(YoloPolicy.EditAction.INSERT_WITHOUT_DELETION,
                YoloPolicy.decide(true, 0, 2, "Yo", LENGTH));
        // pasting over a selection (the Cut / paste leg)
        assertEquals(YoloPolicy.EditAction.INSERT_WITHOUT_DELETION,
                YoloPolicy.decide(true, 0, 5, "pasted", LENGTH));
    }

    @Test
    void invalidArgumentsFallThroughToSuperclassContracts() {
        assertEquals(YoloPolicy.EditAction.APPLY,
                YoloPolicy.decide(true, 5, 3, "", LENGTH));   // IllegalArgumentException upstream
        assertEquals(YoloPolicy.EditAction.APPLY,
                YoloPolicy.decide(true, 0, 5, null, LENGTH)); // NullPointerException upstream
        assertEquals(YoloPolicy.EditAction.APPLY,
                YoloPolicy.decide(true, 0, LENGTH + 1, "", LENGTH)); // IndexOutOfBounds upstream
    }
}