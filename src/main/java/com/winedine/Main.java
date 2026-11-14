package com.winedine;

import com.winedine.config.AppConfig;
import com.winedine.controller.AgentController;
import com.winedine.data.DataLoadException;
import com.winedine.ui.CliApp;

import java.io.IOException;

/** Command-line launcher: {@code java -cp target/classes com.winedine.Main} or {@code mvn exec:java}. */
public final class Main {
    private Main() { }

    public static void main(String[] args) throws IOException {
        AppConfig config = AppConfig.load();
        try {
            AgentController controller = AgentController.create(config);
            CliApp.forConsole(controller).run();
        } catch (DataLoadException e) {
            System.err.println("Could not start: " + e.getMessage());
            System.exit(1);
        }
    }
}
