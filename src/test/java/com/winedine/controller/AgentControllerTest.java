package com.winedine.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.winedine.config.AppConfig;
import com.winedine.model.AgentResponse;
import com.winedine.model.ResponseType;
import com.winedine.model.SearchHistory;
import com.winedine.ui.CliApp;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentControllerTest {
    private static AgentController controller(Path historyFile) {
        return AgentController.create(AppConfig.fromMap(Map.of(
                AppConfig.KEY_USE_MOCK, "true",
                AppConfig.KEY_DATASET, "src/test/resources/test-restaurants.json",
                AppConfig.KEY_HISTORY, historyFile.toString())));
    }

    private static Path tempHistory() throws IOException {
        return Files.createTempDirectory("winedine-test").resolve("history.json");
    }

    @Test
    void fullConversationClarifyThenResultsThenRefine() throws IOException {
        AgentController c = controller(tempHistory());
        assertEquals(ResponseType.CLARIFICATION, c.send("beer near osgoode station").getType());
        assertTrue(c.isAwaitingClarification());
        assertEquals(ResponseType.RESULTS, c.send("skip").getType());
        assertFalse(c.getResults().isEmpty());
        assertEquals(ResponseType.RESULTS, c.send("closest please").getType());
        assertEquals("DISTANCE_FIRST", c.getConstraints().getRankingProfile().name());
    }

    @Test
    void explainAndResultLookup() throws IOException {
        AgentController c = controller(tempHistory());
        c.send("beer near osgoode station on friday 5-7pm under $20");
        assertNotNull(c.getResult(1));
        assertNull(c.getResult(0));
        assertTrue(c.explain(1).contains(c.getResult(1).getRestaurant().getName()));
        assertTrue(c.explain(99).contains("no result"));
    }

    @Test
    void searchesAreSavedAndSurviveARestart() throws IOException {
        Path file = tempHistory();
        AgentController first = controller(file);
        first.send("beer near osgoode station on friday 5-7pm under $20");
        int saved = first.getResults().size();

        AgentController second = controller(file);
        assertEquals(1, second.getHistory().size());
        SearchHistory entry = second.reopen(1);
        assertNotNull(entry);
        assertEquals(saved, second.getResults().size());
        assertFalse(second.isOutdated(entry));
        assertEquals(ResponseType.RESULTS, second.refresh().getType());
    }

    @Test
    void newSearchForgetsTheOldOne() throws IOException {
        AgentController c = controller(tempHistory());
        c.send("beer near osgoode station on friday 5-7pm under $20");
        c.newSearch();
        assertTrue(c.getResults().isEmpty());
        AgentResponse r = c.send("hello");
        assertEquals(ResponseType.CLARIFICATION, r.getType());
    }

    @Test
    void reopeningAnUnknownHistoryNumberGivesNull() throws IOException {
        assertNull(controller(tempHistory()).reopen(3));
    }

    @Test
    void cliRunsAConversation() throws IOException {
        String input = "beer near osgoode station on friday 5-7pm under $20\n/show 1\n/explain 1\n/map 1\n/history\n/bogus\n/quit\n";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new CliApp(controller(tempHistory()), new BufferedReader(new StringReader(input)), new PrintStream(bytes)).run();
        String out = bytes.toString();
        assertTrue(out.contains("Wine&Dine AI"));
        assertTrue(out.contains("Searching:"));
        assertTrue(out.contains("Address:"));
        assertTrue(out.contains("openstreetmap.org") || out.contains("no map location"));
        assertTrue(out.contains("Unknown command"));
        assertTrue(out.contains("Bye!"));
    }
}