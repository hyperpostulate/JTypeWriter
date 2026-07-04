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
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.stats.SessionStats;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class StatsBar extends BorderPane {

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

    private final SessionStats sessionStats;
    private final Messages messages;
    private final Label clockLabel;
    private final Label statsLabel;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private Size currentSize = Size.MEDIUM;

    public StatsBar(SessionStats sessionStats, Messages messages) {
        this.sessionStats = sessionStats;
        this.messages = messages;

        getStyleClass().add("stats-bar");

        clockLabel = new Label();
        clockLabel.getStyleClass().add("stats-clock");

        statsLabel = new Label();
        statsLabel.getStyleClass().add("stats-label");

        var leftBox = new HBox(clockLabel);
        leftBox.getStyleClass().add("stats-left");

        var rightBox = new HBox(statsLabel);
        rightBox.getStyleClass().add("stats-right");

        setLeft(leftBox);
        setRight(rightBox);

        applySize();

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                currentSize = currentSize.next();
                applySize();
            }
        });

        var timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> update())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void applySize() {
        setPadding(new Insets(currentSize.padding, 16, currentSize.padding, 16));
        var fontSize = currentSize.fontSize;
        clockLabel.setStyle("-fx-font-size: " + fontSize + "px;");
        statsLabel.setStyle("-fx-font-size: " + fontSize + "px;");
    }

    private void update() {
        var now = LocalDateTime.now();
        clockLabel.setText(now.format(dateFormatter) + "  " + now.format(timeFormatter));

        var wordCount = sessionStats.getWordCount();
        var charCount = sessionStats.getCharCount();
        var charNoSpace = sessionStats.getCharCountNoSpaces();
        var lineCount = sessionStats.getLineCount();
        var keystrokes = sessionStats.getTotalKeystrokes();
        var duration = sessionStats.getFormattedDuration();
        var wpm = sessionStats.getWpm();
        statsLabel.setText(messages.statsFormat(wordCount, charCount, charNoSpace, lineCount, keystrokes, duration, wpm));
    }
}
