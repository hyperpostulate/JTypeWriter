/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.control.TextInputControl;
import org.mesutormanli.jtypewriter.AppLogging;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clears the undo/redo history of a {@link TextInputControl}.
 *
 * <p>JavaFX exposes no public API for this (see JDK-8090123), so the history is
 * cleared reflectively on the instance: the stacked changes become {@code null},
 * the next edit is forced to open a new undo record, and the {@code undoable} /
 * {@code redoable} wrappers are set to {@code false} without calling
 * {@code updateUndoRedoState()} (that method dereferences {@code undoChange.next}
 * and would throw on a null change).</p>
 *
 * <p>This works only because JavaFX is launched from the classpath (unnamed
 * module), where the internal fields are accessible. On the module path - for
 * example a jlink image - the reflective access is refused; the fallback
 * is a plain {@code setText}, which keeps the previous document's history
 * entries. Behaviour then degrades but nothing breaks.</p>
 */
public final class UndoHistoryReset {

    private static final Logger LOG = AppLogging.logger(UndoHistoryReset.class);

    private UndoHistoryReset() {
    }

    /**
     * Attempts to clear the undo and redo history of {@code control}.
     *
     * @return {@code true} when the history was cleared; {@code false} when
     *         reflective access failed, in which case the control is left
     *         untouched (no partially reset state)
     */
    public static boolean reset(TextInputControl control) {
        try {
            // Resolve and validate everything first: a failure half-way through
            // the mutation could leave isUndoable() == true with a null undo
            // change, which would NPE on the next Ctrl+Z.
            var undoChangeHead = field("undoChangeHead");
            var undoChange = field("undoChange");
            var createNewUndoRecord = field("createNewUndoRecord");
            var undoable = field("undoable");
            var redoable = field("redoable");

            var undoableWrapper = (ReadOnlyBooleanWrapper) undoable.get(control);
            var redoableWrapper = (ReadOnlyBooleanWrapper) redoable.get(control);

            undoChangeHead.set(control, null);
            undoChange.set(control, null);
            createNewUndoRecord.setBoolean(control, true);
            undoableWrapper.set(false);
            redoableWrapper.set(false);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.log(Level.WARNING, "undo history could not be cleared: " + e, e);
            return false;
        }
    }

    private static Field field(String name) throws NoSuchFieldException {
        var field = TextInputControl.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
