package com.winedine;

import com.winedine.ui.gui.GuiApp;
import javafx.application.Application;

/**
 * GUI launcher. It is a separate class from the JavaFX Application so the app also starts
 * from a plain classpath: {@code mvn javafx:run} or {@code java -cp ... com.winedine.GuiMain}.
 */
public final class GuiMain {
    private GuiMain() { }

    public static void main(String[] args) {
        Application.launch(GuiApp.class, args);
    }
}
