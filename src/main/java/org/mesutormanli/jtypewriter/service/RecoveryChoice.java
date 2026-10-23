package org.mesutormanli.jtypewriter.service;

/**
 * The answer to the "recover unsaved draft" prompt. {@link #LATER} is the
 * safe default: it never deletes the draft and never restores it, so closing
 * the prompt by accident cannot destroy the only copy of the text.
 */
public enum RecoveryChoice {
    /** Load the draft into the editor. */
    RESTORE,
    /** Delete the draft because the user explicitly does not want it. */
    DISCARD,
    /** Keep the draft for the next start without restoring it now. */
    LATER
}
