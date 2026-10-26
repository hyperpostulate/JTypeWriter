/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui.component;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.AutosaveHealth;
import org.mesutormanli.jtypewriter.stats.SessionStats;

import java.time.LocalDateTime;

public class StatsBar extends BorderPane {

    private final StatsBarModel model;
    private final StatsBarState state;
    private final Label clockLabel;
    private final Label statsLabel;
    private final Label saveStatus;
    private final Label warningLabel;

    /**
     * RW-17: the status bar must start at >= 13 px without the user cycling
     * sizes, so the default is MEDIUM (13 px), never SMALL. At runtime the
     * actual startup size is whatever {@link StatsBarState} restored from the
     * last session (RW-18); this method pins the factory default that that
     * restore falls back to. Double-click cycling through all three sizes is
     * untouched.
     */
    static Size defaultSize() {
        return Size.MEDIUM;
    }

    /** The default status text size in px, for headless assertions (RW-17). */
    public static int defaultStatusFontSizePx() {
        return defaultSize().fontSize;
    }

    public StatsBar(SessionStats sessionStats, Messages messages, StatsBarState state,
                    LocaleManager localeManager) {
        this.state = state;
        this.model = new StatsBarModel(sessionStats, messages, localeManager);

        getStyleClass().add("stats-bar");

        clockLabel = new Label();
        clockLabel.getStyleClass().add("stats-clock");

        statsLabel = new Label();
        statsLabel.getStyleClass().add("stats-label");

        // RW-19: "Saved 12:04" appears next to the clock on a successful save
        // and is cleared by the next edit; unlike the stats text it only
        // changes when MainController says so, never on the one-second tick.
        saveStatus = new Label();
        saveStatus.getStyleClass().add("stats-save");

        // V4/PW-27: "autosave is not working" while a recovery write fails.
        // Unlike the save confirmation this is not a moment in time but a
        // standing condition, so it lives next to the statistics the user is
        // already reading and disappears only when a write succeeds again.
        warningLabel = new Label();
        warningLabel.getStyleClass().add("stats-warning");
        warningLabel.setVisible(false);
        warningLabel.setManaged(false);

        // Statistics sit on the left, date and time (with the save confirmation
        // that belongs to them) on the right.
        var leftBox = new HBox(warningLabel, statsLabel);
        leftBox.getStyleClass().add("stats-left");

        var rightBox = new HBox(clockLabel, saveStatus);
        rightBox.getStyleClass().add("stats-right");
        rightBox.setSpacing(12);

        setLeft(leftBox);
        setRight(rightBox);

        // U9: paint the first frame from the model's constructor-time text
        // instead of leaving the labels blank until the first tick.
        applyTexts();
        // U10: repaint synchronously inside the locale-change event.
        model.addListener(this::applyTexts);

        applySize();

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                state.nextSize(); // persists, so the next session starts at this size
                applySize();
            }
        });

        var timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    model.refresh(LocalDateTime.now());
                    applyTexts();
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void applySize() {
        var size = state.getSize();
        setPadding(new Insets(size.padding, 16, size.padding, 16));
        var fontSize = size.fontSize;
        var style = "-fx-font-size: " + fontSize + "px;";
        clockLabel.setStyle(style);
        statsLabel.setStyle(style);
        saveStatus.setStyle(style);
        warningLabel.setStyle(style);
    }

    /**
     * RW-19: shows the localised save status (e.g. {@code "Saved 12:04"}) in
     * the stats bar. {@code null} or blank clears it. MainController owns the
     * lifecycle: set on the dirty→clean transition of a real save, cleared on
     * the next edit and on open/new, so a stale "saved" claim never sits under
     * a dirty document. The autosave draft snapshot does not pass through here
     * - it does not clean the document.
     */
    public void setSaveStatus(String text) {
        var value = text == null ? "" : text;
        if (!value.equals(saveStatus.getText())) { // runs on every keystroke; stay quiet when unchanged
            saveStatus.setText(value);
        }
    }

    /**
     * V4/PW-27 / V17/PW-28: the standing warning kind. {@code null} means the
     * safety net is healthy; the bundle text that matches the kind is chosen in
     * {@link StatsBarModel}. The label is hidden while the text is blank, so a
     * healthy session never pays for the space.
     */
    public void setAutosaveProblem(AutosaveHealth.Problem problem) {
        model.setAutosaveProblem(problem);
        applyTexts(); // the model already notified, but be explicit for a direct caller
    }

    /** Copies the model's current texts onto the labels - the only rendering step. */
    private void applyTexts() {
        clockLabel.setText(model.clockText());
        statsLabel.setText(model.statsText());
        var warning = model.warningText();
        var visible = !warning.isBlank();
        warningLabel.setText(warning);
        warningLabel.setVisible(visible);
        warningLabel.setManaged(visible);
    }

    public enum Size {
        SMALL(4, 11),
        MEDIUM(7, 13),
        LARGE(11, 15);

        final int padding;
        final int fontSize;

        Size(int padding, int fontSize) {
            this.padding = padding;
            this.fontSize = fontSize;
        }

        Size next() {
            Size[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}
