package com.winedine.ui.gui;

import com.winedine.config.AppConfig;
import com.winedine.controller.AgentController;
import com.winedine.model.AgentResponse;
import com.winedine.model.HappyHourDeal;
import com.winedine.model.ResponseType;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchHistory;
import com.winedine.model.SearchResult;
import com.winedine.ui.MapLinks;
import com.winedine.ui.ResultFormatter;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * JavaFX window (the View). It only shows data and forwards user actions to {@link AgentController}.
 * Layout: header on top, result cards on the left, details and map on the right, chat and input at the bottom.
 * Colours and fonts live in {@code winedine.css}.
 */
public class GuiApp extends Application {
    private AgentController controller;

    private final TextArea chat = new TextArea();
    private final TextField input = new TextField();
    private final Button send = new Button("Send");
    private final ListView<SearchResult> resultList = new ListView<>();
    private final TextArea details = new TextArea();
    private final Label mapNote = new Label();
    private final Label resultCount = new Label("RESULTS");
    private final Button explainButton = new Button("Explain why");
    private final Button refreshButton = new Button("Refresh search");
    private final Button openMapButton = new Button("Open in Google Maps");
    private String llmName = "";
    private final Button themeButton = new Button();
    private AtlasMapView pixelMap;
    private StackPane sceneRoot;
    private CoverView splash;
    private SearchResult current;
    private boolean night = false;

    @Override
    public void start(Stage stage) {
        try {
            controller = AgentController.create(AppConfig.load());
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, "Could not start: " + e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }

        // header with the title and a pill showing which language model is active
        Label title = new Label("Wine&Dine AI");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Toronto happy-hour finder");
        subtitle.getStyleClass().add("app-subtitle");
        VBox titles = new VBox(2, title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        String llm = controller.describeLlm();
        llmName = llm;
        Label llmBadge = new Label(llm);
        llmBadge.getStyleClass().addAll("badge", llm.startsWith("OpenAI") ? "badge-ai" : "badge-mock");
        Node logo = Glyphs.icon(IconKind.TOWER, 44);
        themeButton.getStyleClass().add("theme-button");
        themeButton.setOnAction(e -> applyTheme(!night));
        Button homeButton = new Button("Home");
        homeButton.setOnAction(e -> showCover(true));
        Button quitButton = new Button("Quit");
        quitButton.getStyleClass().add("danger");
        quitButton.setOnAction(e -> Platform.exit());
        HBox header = new HBox(10, logo, titles, spacer, llmBadge, themeButton, homeButton, quitButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        // results (left)
        resultCount.getStyleClass().add("section-title");
        resultList.setCellFactory(lv -> new ResultCell());
        resultList.setPlaceholder(new Label("Your results will appear here"));
        VBox.setVgrow(resultList, Priority.ALWAYS);
        VBox left = new VBox(8, resultCount, resultList);
        left.getStyleClass().add("panel");
        left.setPrefWidth(400);
        left.setMinWidth(340);

        // details and map (right)
        Label detailsTitle = new Label("DETAILS");
        detailsTitle.getStyleClass().add("section-title");
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(9);
        details.getStyleClass().add("details");
        explainButton.setDisable(true);
        refreshButton.setDisable(true);
        openMapButton.setDisable(true);
        openMapButton.setOnAction(e -> openInMaps());
        mapNote.getStyleClass().add("map-note");
        HBox detailButtons = new HBox(8, explainButton, refreshButton, openMapButton, mapNote);
        detailButtons.setAlignment(Pos.CENTER_LEFT);
        pixelMap = new AtlasMapView(i -> resultList.getSelectionModel().select(i), i -> openInMaps());
        StackPane mapFrame = new StackPane(pixelMap);
        mapFrame.getStyleClass().add("map-frame");
        VBox.setVgrow(mapFrame, Priority.ALWAYS);
        VBox right = new VBox(8, detailsTitle, details, detailButtons, mapFrame);
        right.getStyleClass().add("panel");
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox middle = new HBox(12, left, right);

        // chat and input (bottom)
        chat.setEditable(false);
        chat.setWrapText(true);
        chat.setPrefRowCount(6);
        chat.getStyleClass().add("chat");
        input.setPromptText("e.g. cheap beer and wings near Osgoode Station on Friday 5-7pm under $15");
        HBox.setHgrow(input, Priority.ALWAYS);
        send.getStyleClass().add("primary");
        Button newSearch = new Button("New search");
        Button history = new Button("History");
        HBox inputRow = new HBox(8, input, send, newSearch, history);
        VBox bottom = new VBox(8, chat, inputRow);
        bottom.getStyleClass().add("panel");

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(middle);
        root.setBottom(bottom);
        BorderPane.setMargin(middle, new Insets(12, 0, 12, 0));
        root.setPadding(new Insets(14));

        send.setOnAction(e -> submit());
        input.setOnAction(e -> submit());
        newSearch.setOnAction(e -> {
            controller.newSearch();
            resultList.getItems().clear();
            resultCount.setText("RESULTS");
            current = null;
            pixelMap.setResults(List.of());
            details.clear();
            mapNote.setText("");
            openMapButton.setDisable(true);
            explainButton.setDisable(true);
            refreshButton.setDisable(true);
            log("Starting a new search. What are you looking for?");
        });
        history.setOnAction(e -> showHistory());
        explainButton.setOnAction(e -> explainSelected());
        refreshButton.setOnAction(e -> runAsync(controller::refresh));
        resultList.getSelectionModel().selectedIndexProperty()
                .addListener((obs, old, now) -> showSelected(now.intValue()));

        log("Hi! Tell me what you're after: where, when, and your budget per person.");
        sceneRoot = new StackPane(root);
        showCover(false);
        Scene scene = new Scene(sceneRoot, 1200, 780);
        URL css = getClass().getResource("/winedine.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        applyTheme(false);
        stage.setTitle("Wine&Dine AI");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
        Platform.runLater(() -> {
            if (splash != null) {
                splash.requestFocus();
            }
        });
    }

    /** Switches between the night (dark) and day (light) look. */
    private void applyTheme(boolean nightMode) {
        night = nightMode;
        sceneRoot.getStyleClass().remove("blueprint");
        if (night) {
            sceneRoot.getStyleClass().add("blueprint");
        }
        pixelMap.setNight(night);
        themeButton.setText(night ? "Parchment mode" : "Blueprint mode");
        if (splash != null) {
            splash.setNight(night);
        }
    }

    /** Shows the cover page on top of the app (at start-up, or when Home is pressed). */
    private void showCover(boolean fadeIn) {
        if (splash != null) {
            return;
        }
        splash = new CoverView(night, llmName, this::enterApp, Platform::exit, this::applyTheme);
        sceneRoot.getChildren().add(splash);
        if (fadeIn) {
            splash.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(300), splash);
            fade.setToValue(1);
            fade.play();
            splash.requestFocus();
        }
    }

    /** Opens the selected restaurant in Google Maps in the user's browser. */
    private void openInMaps() {
        if (current == null) {
            return;
        }
        getHostServices().showDocument(MapLinks.googleUrl(current.getRestaurant()));
        mapNote.setText("Opened in your browser");
    }

    /** Fades the title screen away and shows the app. */
    private void enterApp() {
        if (splash == null) {
            return;
        }
        CoverView s = splash;
        splash = null;
        s.setMouseTransparent(true);
        FadeTransition fade = new FadeTransition(Duration.millis(450), s);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            s.stop();
            sceneRoot.getChildren().remove(s);
            input.requestFocus();
        });
        fade.play();
    }

    // ---- result card ----

    /** One result shown as a card: name, cuisine and distance, deal text, and coloured badges. */
    private final class ResultCell extends ListCell<SearchResult> {
        @Override
        protected void updateItem(SearchResult r, boolean empty) {
            super.updateItem(r, empty);
            if (empty || r == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Restaurant rest = r.getRestaurant();
            Label name = new Label((getIndex() + 1) + ".  " + rest.getName());
            name.getStyleClass().add("card-title");
            name.setWrapText(true);

            String distance = r.getDistanceKm() == null ? "distance unavailable"
                    : String.format(Locale.ROOT, "%.1f km", r.getDistanceKm());
            String cuisine = rest.getCuisine().isEmpty() ? "" : rest.getCuisine() + "  ·  ";
            Label meta = new Label(cuisine + distance);
            meta.getStyleClass().add("card-meta");

            VBox content = new VBox(5, name, meta);
            HappyHourDeal deal = r.getHappyHourDeal();
            if (deal != null) {
                Label dealText = new Label(deal.getDescription());
                dealText.getStyleClass().add("card-deal");
                dealText.setWrapText(true);
                dealText.setMaxHeight(48);
                content.getChildren().add(dealText);
            }

            HBox badges = new HBox(6);
            String fresh = ResultFormatter.freshness(r).name();
            badges.getChildren().add(badge(fresh, "badge-" + fresh.toLowerCase(Locale.ROOT)));
            if (r.isNearMatch()) {
                badges.getChildren().add(badge("NEAR MATCH", "badge-near"));
            } else if (r.isComparableAlternative()) {
                badges.getChildren().add(badge("COMPARABLE", "badge-best"));
            }
            if (!r.getWarnings().isEmpty()) {
                int n = r.getWarnings().size();
                badges.getChildren().add(badge(n + (n == 1 ? " WARNING" : " WARNINGS"), "badge-warn"));
            }
            content.getChildren().add(badges);
            content.setMinWidth(0);

            Node iconBox = Glyphs.badge(IconKind.choose(rest, deal), 42);
            HBox card = new HBox(12, iconBox, content);
            card.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(content, Priority.ALWAYS);
            card.getStyleClass().add("card");
            // keep the card inside the list so long text wraps instead of widening the list
            card.maxWidthProperty().bind(resultList.widthProperty().subtract(34));
            setText(null);
            setGraphic(card);
        }
    }

    private static Label badge(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", styleClass);
        return l;
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
        resultList.getItems().setAll(results);
        pixelMap.setResults(results);
        resultCount.setText(results.size() + (results.size() == 1 ? " RESULT" : " RESULTS"));
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
        current = r;
        details.setText(ResultFormatter.details(r));
        pixelMap.select(zeroBasedIndex);
        openMapButton.setDisable(false);
        mapNote.setText(r.getRestaurant().hasValidLocation() ? "" : "No coordinates: pin not on the atlas");
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