package org.mesutormanli.jtypewriter.ui.component;

import javafx.scene.control.TextArea;
import org.springframework.stereotype.Component;

@Component
public class EditorArea extends TextArea {

    private boolean yoloMode;

    public EditorArea() {
        setWrapText(true);
        setPrefRowCount(40);
        getStyleClass().add("editor-area");
    }

    public boolean isYoloMode() {
        return yoloMode;
    }

    public void setYoloMode(boolean enabled) {
        this.yoloMode = enabled;
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("yolo"), enabled);
    }

    public void toggleYoloMode() {
        setYoloMode(!yoloMode);
    }
}
