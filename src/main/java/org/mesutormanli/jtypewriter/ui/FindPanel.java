/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import javafx.animation.PauseTransition;
import javafx.scene.AccessibleAction;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mesutormanli.jtypewriter.locale.LocaleManager;
import org.mesutormanli.jtypewriter.locale.Messages;
import org.mesutormanli.jtypewriter.service.DialogService;
import org.mesutormanli.jtypewriter.ui.component.EditorArea;

/**
 * The floating find/replace card over the editor, modelled on Notepad++'s
 * compact dialog and painted from the theme tokens.
 *
 * <p><strong>Why a card and not a dialog.</strong> Finding is a mode, not a
 * one-shot task: the user types a query, presses {@code F3} repeatedly, edits
 * the replace text and searches again. A modal window would have to be
 * re-opened and re-positioned between every one of those steps, and would
 * cover the very text it is navigating. A small card anchored by
 * {@code MainController} over the editor keeps the document visible and can be
 * hidden without losing what the user typed.</p>
 *
 * <p><strong>Why the scan is delegated.</strong> {@link FindModel#findAll} is
 * O(document length) and the editor accepts documents up to 10 MB. Running it
 * on the FX thread for every keystroke in the query field would freeze the
 * window. The card therefore submits the scan to {@link FindScanService} on a
 * worker, tags it with a generation counter, and installs the result on the FX
 * thread only if it still describes the current document, query and case mode.
 * The model makes the same check itself; the counter additionally drops a
 * completion that a newer scan has already superseded.</p>
 *
 * <p><strong>Why replacement bypasses {@code EditorArea.replaceText}.</strong>
 * YOLO mode rewrites a replacement into an insertion so that no existing
 * character is lost. That is the right answer for typing over a selection, but
 * the find panel's whole job is to remove the matched characters: routing the
 * edit through the YOLO funnel would leave the old match in place and insert
 * the replacement next to it. {@link EditorArea#replaceConfirmed} is the one
 * explicit bypass, and the card only calls it after the YOLO consent prompt
 * has been answered.</p>
 *
 * <p><strong>Why YOLO needs consent.</strong> The mode's promise is that a user
 * edit never deletes text. A replace is exactly such a deletion, so silently
 * bypassing the promise would make the mode lie. Asking once per operation
 * keeps the promise honest, and a cancelled prompt writes nothing.</p>
 */
public class FindPanel extends VBox implements FindCommands {

    /** How long a burst of typing in a text field is coalesced before re-scanning. */
    private static final Duration SCAN_DEBOUNCE = Duration.millis(120);

    private final Messages messages;
    private final LocaleManager localeManager;
    private final EditorArea editorArea;
    private final DialogService dialogService;
    private final FindScanService scanService;

    private final Label titleLabel;
    private final Label findWhatLabel;
    private final Label replaceLabel;
    private final TextField findField;
    private final TextField findReplaceField;
    private final CheckBox findCaseCheck;
    private final Button findNextButton;
    private final Button findPreviousButton;
    private final Button replaceButton;
    private final Button replaceAllButton;
    private final Button closeButton;
    private final Label findStatus;

    private final FindModel model = new FindModel();
    private final PauseTransition debounce = new PauseTransition(SCAN_DEBOUNCE);

    /** Incremented for every submitted scan; a callback with an old value is dropped. */
    private long generation;

    /** Whether the newest submitted scan has not delivered its result yet. */
    private boolean scanning;

    /** The navigation to run once an in-flight scan installs its matches. */
    private Runnable pendingNavigation;

    private Stage stage;

    public FindPanel(Messages messages, LocaleManager localeManager, EditorArea editorArea,
                     DialogService dialogService, FindScanService scanService) {
        this.messages = messages;
        this.localeManager = localeManager;
        this.editorArea = editorArea;
        this.dialogService = dialogService;
        this.scanService = scanService;

        getStyleClass().add("find-panel");
        setVisible(false);
        setManaged(false);
        setMaxWidth(420);
        setMaxHeight(Region.USE_PREF_SIZE);

        titleLabel = new Label();
        titleLabel.getStyleClass().add("find-title");

        closeButton = new Button();
        closeButton.getStyleClass().add("find-close");
        closeButton.setOnAction(e -> hide());

        var headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        var header = new HBox(titleLabel, headerSpacer, closeButton);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        findWhatLabel = new Label();
        findWhatLabel.getStyleClass().add("find-label");
        findField = new TextField();
        findField.getStyleClass().add("find-field");

        replaceLabel = new Label();
        replaceLabel.getStyleClass().add("find-label");
        findReplaceField = new TextField();
        findReplaceField.getStyleClass().add("find-field");

        findCaseCheck = new CheckBox();
        findCaseCheck.getStyleClass().add("find-option");

        findNextButton = new Button();
        findPreviousButton = new Button();
        replaceButton = new Button();
        replaceAllButton = new Button();
        for (var button : new Button[]{findNextButton, findPreviousButton, replaceButton}) {
            button.getStyleClass().add("find-action");
        }
        replaceAllButton.getStyleClass().addAll("find-action", "find-action-primary");

        var actionRow = new HBox(findNextButton, findPreviousButton, replaceButton, replaceAllButton);
        actionRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        findStatus = new Label();
        findStatus.getStyleClass().add("find-status");

        findNextButton.setOnAction(e -> findNext());
        findPreviousButton.setOnAction(e -> findPrevious());
        replaceButton.setOnAction(e -> replaceCurrent());
        replaceAllButton.setOnAction(e -> replaceAll());

        getChildren().addAll(
                header,
                findWhatLabel, findField,
                replaceLabel, findReplaceField,
                findCaseCheck,
                actionRow,
                findStatus);

        // Escape closes the card from anywhere inside it, before a control can
        // consume the key as an editing gesture.
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hide();
                event.consume();
            }
        });

        debounce.setOnFinished(e -> rescan());

        findField.textProperty().addListener((obs, oldValue, newValue) -> scheduleScan());
        // The replace text has no bearing on what is searched, so it does not
        // trigger a scan; it is read only when a replacement is applied.
        findCaseCheck.selectedProperty().addListener((obs, oldValue, newValue) -> rescan());
        // The panel's own replacements rewrite the editor, and the model has
        // already adopted exactly that text (with the cursor parked after the
        // inserted replacement). Re-scanning on those changes would throw that
        // cursor away, so the listener only reacts to text the model does not
        // describe yet - which is the user editing the document.
        editorArea.textProperty().addListener((obs, oldValue, newValue) -> {
            if (isVisible() && !model.document().equals(newValue)) {
                scheduleScan();
            }
        });

        localeManager.addListener(lang -> relocalise());
        relocalise();
        setActionsDisabled(true);
    }

    /** The owner window used by the YOLO confirmation; may be {@code null} in tests. */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void openFind() {
        showPanel();
        focusAndSelect(findField);
    }

    @Override
    public void openReplace() {
        showPanel();
        focusAndSelect(findReplaceField);
    }

    @Override
    public void findNext() {
        navigate(true);
    }

    @Override
    public void findPrevious() {
        navigate(false);
    }

    private void showPanel() {
        setVisible(true);
        setManaged(true);
        model.setDocument(editorArea.getText());
        rescan();
    }

    private void hide() {
        setVisible(false);
        setManaged(false);
        editorArea.requestFocus();
    }

    private static void focusAndSelect(TextField field) {
        field.requestFocus();
        field.selectAll();
    }

    private void scheduleScan() {
        debounce.playFromStart();
    }

    /**
     * Submits the current query for scanning. An empty query clears the model
     * instead of scanning: there is nothing to find, and every action button
     * would be a no-op.
     */
    private void rescan() {
        var needle = findField.getText() == null ? "" : findField.getText();
        var caseSensitive = findCaseCheck.isSelected();
        if (needle.isEmpty()) {
            generation++;
            scanning = false;
            pendingNavigation = null;
            model.setQuery("", caseSensitive);
            setActionsDisabled(true);
            findStatus.setText("");
            return;
        }

        var document = editorArea.getText();
        model.setQuery(needle, caseSensitive);
        model.setDocument(document);
        scanning = true;
        setActionsDisabled(true);
        findStatus.setText(messages.findSearching());

        var issued = ++generation;
        scanService.scan(document, needle, caseSensitive, matches -> {
            if (issued != generation) {
                return; // a newer query/document already owns the state
            }
            if (!model.install(document, needle, caseSensitive, matches)) {
                return; // the model moved on while the scan was running
            }
            scanning = false;
            setActionsDisabled(false);
            renderStatus();
            var navigation = pendingNavigation;
            pendingNavigation = null;
            if (navigation != null) {
                navigation.run();
            }
        });
    }

    /**
     * Moves to the next or previous match, unless the panel has no query (then
     * it opens itself) or a scan is still in flight (then it remembers the
     * direction and runs after the result installs).
     */
    private void navigate(boolean forward) {
        var query = findField.getText() == null ? "" : findField.getText();
        if (query.isEmpty()) {
            openFind();
            return;
        }
        if (rescanIfStale(() -> navigate(forward))) {
            return;
        }

        var match = forward ? model.next() : model.previous();
        match.ifPresent(this::reveal);
        renderStatus();
    }

    /**
     * Whether the model's match list no longer describes what the panel would
     * act on: a scan may still be in flight, the editor text may have moved on,
     * or the query and case mode may have changed since the last scan.
     */
    private boolean stale() {
        var query = findField.getText() == null ? "" : findField.getText();
        return scanning
                || !model.document().equals(editorArea.getText())
                || !model.needle().equals(query)
                || model.isCaseSensitive() != findCaseCheck.isSelected();
    }

    /**
     * Re-scans when the model is stale, then replays {@code retry} from the
     * scan callback. A replacement computed from a stale snapshot would write
     * a document built from text the user has already edited, which would
     * silently discard their typing; navigating on stale ranges would select
     * the wrong words. Both therefore defer instead of acting on the snapshot.
     *
     * @return whether the caller must stop and let the scan replay it
     */
    private boolean rescanIfStale(Runnable retry) {
        if (!stale()) {
            return false;
        }
        pendingNavigation = retry;
        if (!scanning) {
            rescan();
        }
        return true;
    }

    /** Selects and scrolls a match into view. */
    private void reveal(FindModel.Match match) {
        editorArea.selectRange(match.start(), match.end());
        editorArea.positionCaret(match.end());
        // TextArea exposes no follow-caret method; SHOW_TEXT_RANGE is the one
        // public action that asks the skin to scroll the caret's line into view.
        if (editorArea.getSkin() != null) {
            editorArea.executeAccessibleAction(
                    AccessibleAction.SHOW_TEXT_RANGE, match.start(), match.end());
        }
    }

    /**
     * Replaces the selected match. With no match selected it behaves like
     * {@code Find Next}, so the first click of Replace does not rewrite a
     * range the user never saw.
     */
    private void replaceCurrent() {
        if (findField.getText() == null || findField.getText().isEmpty()) {
            openFind();
            return;
        }
        if (rescanIfStale(this::replaceCurrent)) {
            return;
        }
        if (model.current().isEmpty()) {
            findNext();
            return;
        }
        if (!yoloConsentGiven()) {
            return;
        }
        var replacement = findReplaceField.getText() == null ? "" : findReplaceField.getText();
        var result = model.replaceCurrent(replacement);
        if (!result.changed()) {
            renderStatus();
            return;
        }
        editorArea.replaceConfirmed(0, editorArea.getLength(), result.document());
        renderStatus();
        result.current().ifPresent(this::reveal);
    }

    /** Replaces every match of the current scan as one undoable edit step. */
    private void replaceAll() {
        if (findField.getText() == null || findField.getText().isEmpty()) {
            openFind();
            return;
        }
        if (rescanIfStale(this::replaceAll)) {
            return;
        }
        if (!yoloConsentGiven()) {
            return;
        }
        var replacement = findReplaceField.getText() == null ? "" : findReplaceField.getText();
        var result = model.replaceAll(replacement);
        if (result.changed()) {
            editorArea.replaceConfirmed(0, editorArea.getLength(), result.document());
        }
        findStatus.setText(messages.findMatchCount(model.matchCount()));
    }

    /**
     * A replacement deletes the matched characters, so while YOLO mode is on
     * it needs the user's explicit blessing; off, it is an ordinary edit.
     */
    private boolean yoloConsentGiven() {
        return FindReplacePolicy.replacement(editorArea.isYoloMode())
                != FindReplacePolicy.Confirmation.REQUIRED
                || dialogService.confirmReplaceInYolo(stage);
    }

    private void renderStatus() {
        var query = findField.getText() == null ? "" : findField.getText();
        if (query.isEmpty()) {
            findStatus.setText("");
        } else if (scanning) {
            findStatus.setText(messages.findSearching());
        } else if (model.matchCount() == 0) {
            findStatus.setText(messages.findNoMatches());
        } else if (model.current().isPresent()) {
            findStatus.setText(messages.findMatchPosition(
                    model.currentIndex() + 1, model.matchCount()));
        } else {
            findStatus.setText(messages.findMatchCount(model.matchCount()));
        }
    }

    private void setActionsDisabled(boolean disabled) {
        findNextButton.setDisable(disabled);
        findPreviousButton.setDisable(disabled);
        replaceButton.setDisable(disabled);
        replaceAllButton.setDisable(disabled);
    }

    /**
     * Re-reads every bundle string after a language switch, re-renders the
     * current status from the model and re-runs the scan. The query and the
     * document did not change, but the model may not hold a match list yet -
     * the first scan is exactly what a freshly localised card needs.
     */
    private void relocalise() {
        titleLabel.setText(messages.findPanelTitle());
        closeButton.setText(messages.findClose());
        closeButton.setAccessibleText(messages.findClose());
        closeButton.setTooltip(new Tooltip(messages.findClose()));

        findWhatLabel.setText(messages.findWhat());
        replaceLabel.setText(messages.findReplaceWith());
        findField.setPromptText(messages.findWhat());
        findReplaceField.setPromptText(messages.findReplaceWith());
        findField.setAccessibleText(messages.findWhat());
        findReplaceField.setAccessibleText(messages.findReplaceWith());

        findCaseCheck.setText(messages.findMatchCase());

        findNextButton.setText(messages.shortcutFindNext());
        findPreviousButton.setText(messages.shortcutFindPrevious());
        replaceButton.setText(messages.shortcutReplace());
        replaceAllButton.setText(messages.findReplaceAll());
        findNextButton.setTooltip(new Tooltip(messages.shortcutFindNext()));
        findPreviousButton.setTooltip(new Tooltip(messages.shortcutFindPrevious()));
        replaceButton.setTooltip(new Tooltip(messages.shortcutReplace()));
        replaceAllButton.setTooltip(new Tooltip(messages.findReplaceAll()));
        findNextButton.setAccessibleText(messages.shortcutFindNext());
        findPreviousButton.setAccessibleText(messages.shortcutFindPrevious());
        replaceButton.setAccessibleText(messages.shortcutReplace());
        replaceAllButton.setAccessibleText(messages.findReplaceAll());

        renderStatus();
        rescan();
    }
}
