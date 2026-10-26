/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import org.junit.jupiter.api.Test;
import org.mesutormanli.jtypewriter.locale.Language;
import org.mesutormanli.jtypewriter.ui.theme.FontSizeManager;
import org.mesutormanli.jtypewriter.ui.theme.TextColor;
import org.mesutormanli.jtypewriter.ui.theme.Theme;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dropdown option lists, pinned headlessly (no JavaFX toolkit, no
 * controls): each list must be complete, ordered and duplicate-free, and the
 * current value must always be an entry of its own list - a dropdown that
 * cannot display its own value is the stale-label bug class of RW-12.
 */
class ToolbarOptionListsTest {

    @Test
    void themeOptionsFollowTheThemeDeclarationOrder() {
        assertEquals(List.of(Theme.LIGHT, Theme.DARK, Theme.SEPIA), ToolbarView.themeOptions());
    }

    @Test
    void toggleOptionsOfferOnBeforeOff() {
        assertEquals(List.of(Boolean.TRUE, Boolean.FALSE), ToolbarView.toggleOptions());
    }

    @Test
    void textColorOptionsFollowTheTextColorDeclarationOrder() {
        assertEquals(
                List.of(TextColor.DEFAULT, TextColor.SAGE, TextColor.LAVENDER,
                        TextColor.SAND, TextColor.STEEL, TextColor.ROSE),
                ToolbarView.textColorOptions());
    }

    @Test
    void languageOptionsFollowTheLanguageDeclarationOrder() {
        assertEquals(
                List.of(Language.EN, Language.TR, Language.DE, Language.FR, Language.IT, Language.ES),
                ToolbarView.languageOptions());
    }

    @Test
    void everyOptionListIsNonEmptyAndFreeOfDuplicates() {
        for (var options : List.<List<?>>of(
                ToolbarView.themeOptions(),
                ToolbarView.toggleOptions(),
                ToolbarView.textColorOptions(),
                ToolbarView.languageOptions(),
                ToolbarView.fontSizeOptions(FontSizeManager.DEFAULT_SIZE))) {
            assertFalse(options.isEmpty());
            assertEquals(options.size(), new HashSet<>(options).size(),
                    "duplicates in " + options);
        }
    }

    /** Whatever is persisted, the font dropdown can display exactly that value. */
    @Test
    void theCurrentFontSizeIsAlwaysAnOption() {
        for (var size : FontSizeManager.options()) {
            assertTrue(ToolbarView.fontSizeOptions(size).contains(size), String.valueOf(size));
        }
        assertTrue(ToolbarView.fontSizeOptions(21).contains(21)); // off-ladder from an older build
    }

    /** The YOLO/Sound dropdowns can display both of their states. */
    @Test
    void theToggleStateIsAlwaysAnOption() {
        assertTrue(ToolbarView.toggleOptions().contains(Boolean.TRUE));
        assertTrue(ToolbarView.toggleOptions().contains(Boolean.FALSE));
    }
}
