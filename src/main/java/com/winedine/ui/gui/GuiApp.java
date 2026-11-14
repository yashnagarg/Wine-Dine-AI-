package com.winedine.ui.gui;

import com.winedine.config.AppConfig;
import com.winedine.controller.AgentController;
import com.winedine.model.AgentResponse;
import com.winedine.model.ResponseType;
import com.winedine.model.SearchHistory;
import com.winedine.model.SearchResult;
import com.winedine.ui.MapLinks;
import com.winedine.ui.ResultFormatter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * JavaFX window (the View). It only shows data and forwards user actions to {@link AgentController}.
 * Layout: chat log and input at the bottom, result list on the left, details and map on the right.
 */
public class GuiApp extends Application {
    private AgentController controller;

    private final TextArea chat = new TextArea();
    private final TextField input = new TextField();
    private final Button send = new Button("Send");
    private final ListView<String> resultList = new ListView<>();
    private final TextArea details = new TextArea();
    private final WebView map = new WebView();
    private final Label mapNote = new Label();
    private final Button explainButton = new Button("Explain why");
    private final Button refreshButton = new Button("Refresh search");

    @Override
    public void start(Stage stage) {
        try {
            controller = AgentController.create(AppConfig.load());
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, "Could not start: " + e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }

        chat.setEditable(false);
        chat.setWrapText(true);
        chat.setPrefRowCount(7);
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(9);
        input.setPromptText("e.g. cheap beer and wings near Osgoode Station on Friday 5-7pm under $15");
        HBox.setHgrow(input, Priority.ALWAYS);
        explainButton.setDisable(true);
        refreshButton.setDisable(true);
        VBox.setVgrow(map, Priority.ALWAYS);

        Button newSearch = new Button("New search");
        Button history = new Button("History");
        HBox inputRow = new HBox(8, input, send, newSearch, history);
        HBox detailButtons = new HBox(8, explainButton, refreshButton, mapNote);

        VBox left = new VBox(6, new Label("Results"), resultList);
        VBox.setVgrow(resultList, Priority.ALWAYS);
        left.setPrefWidth(320);
        VBox right = new VBox(6, new Label("Details"), details, detailButtons, map);
        VBox bottom = new VBox(6, chat, inputRow);

        BorderPane root = new BorderPane();
        root.setTop(new Label("Wine&Dine AI - language model: " + controller.describeLlm()));
        root.setLeft(left);
        root.setCenter(right);
        root.setBottom(bottom);
        BorderPane.setMargin(left, new Insets(0, 8, 0, 0));
        root.setPadding(new Insets(10));

        send.setOnAction(e -> submit());
        input.setOnAction(e -> submit());
        newSearch.setOnAction(e -> {
            controller.newSearch();
            resultList.getItems().clear();
            details.clear();
            map.getEngine().loadContent("");
            log("Starting a new search. What are you looking for?");
        });
        history.setOnAction(e -> showHistory());
        explainButton.setOnAction(e -> explainSelected());
        refreshButton.setOnAction(e -> runAsync(controller::refresh));
        resultList.getSelectionModel().selectedIndexProperty().addListener((obs, old, now) -> showSelected(now.intValue()));

        log("Hi! Tell me what you're after: where, when, and your budget per person.");
        stage.setTitle("Wine&Dine AI");
        stage.setScene(new Scene(root, 1100, 720));
        stage.show();
    }

    // ---- actions ----

    private void submit() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        input.clear();
        log("You: " + text);
        runAsync(() -> controller.send(text));
    }

    /** Runs a controller call off the UI thread (the LLM call can take seconds) and shows the response. */
    private void runAsync(Supplier<AgentResponse> call) {
        send.setDisable(true);
        Task<AgentResponse> task = new Task<>() {
            @Override
            protected AgentResponse call() {
                return call.get();
            }
        };
        task.setOnSucceeded(e -> {
            send.setDisable(false);
            show(task.getValue());
        });
        task.setOnFailed(e -> {
            send.setDisable(false);
            log("Error: " + task.getException().getMessage());
        });
        Thread t = new Thread(task, "agent-call");
        t.setDaemon(true);
        t.start();
    }

    private void show(AgentResponse response) {
        if (response.getType() == ResponseType.RESULTS) {
            if (!response.getMessage().isEmpty()) {
                log("Agent: " + response.getMessage());
            } else {
                log("Agent: found " + response.getResults().size() + " place(s). Select one to see details.");
            }
            log("Tip: type a change to refine these results (e.g. \"make it $20\"), or press New search to start a different search.");
            fillResults();
        } else if (response.getType() == ResponseType.CLARIFICATION) {
            log("Agent: " + response.getMessage());
        } else {
            log("Error: " + response.getMessage());
        }
    }

    private void fillResults() {
        List<SearchResult> results = controller.getResults();
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            lines.add(ResultFormatter.line(i + 1, results.get(i)));
        }
        resultList.getItems().setAll(lines);
        refreshButton.setDisable(results.isEmpty());
        if (!results.isEmpty()) {
            resultList.getSelectionModel().select(0);
        }
    }

    private void showSelected(int zeroBasedIndex) {
        SearchResult r = controller.getResult(zeroBasedIndex + 1);
        explainButton.setDisable(r == null);
        if (r == null) {
            return;
        }
        details.setText(ResultFormatter.details(r));
        String embed = MapLinks.embedUrl(r.getRestaurant());
        if (embed == null) {
            map.getEngine().loadContent("<html><body style='font-family:sans-serif'>No map location available for this restaurant.</body></html>");
            mapNote.setText("No map location available");
        } else {
            map.getEngine().load(embed);
            mapNote.setText("");
        }
    }

    private void explainSelected() {
        int index = resultList.getSelectionModel().getSelectedIndex() + 1;
        if (index < 1) {
            return;
        }
        explainButton.setDisable(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return controller.explain(index);
            }
        };
        task.setOnSucceeded(e -> {
            explainButton.setDisable(false);
            log("Why #" + index + ": " + task.getValue());
        });
        task.setOnFailed(e -> explainButton.setDisable(false));
        Thread t = new Thread(task, "explain-call");
        t.setDaemon(true);
        t.start();
    }

    private void showHistory() {
        List<SearchHistory> all = controller.getHistory();
        if (all.isEmpty()) {
            log("No saved searches yet.");
            return;
        }
        List<String> labels = new ArrayList<>();
        for (SearchHistory h : all) {
            labels.add(h.getSummary() + (controller.isOutdated(h) ? "  [outdated]" : ""));
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle("Search history");
        dialog.setHeaderText("Open a saved search");
        Optional<String> choice = dialog.showAndWait();
        if (choice.isEmpty()) {
            return;
        }
        SearchHistory entry = controller.reopen(labels.indexOf(choice.get()) + 1);
        if (entry != null) {
            log("Opened: " + entry.getSummary());
            if (controller.isOutdated(entry)) {
                log("Warning: this search is more than 7 days old, so the deals may have changed. Press \"Refresh search\" to search again.");
            }
            fillResults();
        }
    }

    private void log(String line) {
        chat.appendText(line + "\n");
    }
}