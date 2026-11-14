package com.winedine.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.winedine.model.SearchConstraints;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenAILLMClientTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC);

    private static String reply(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"}}]}";
    }

    @Test
    void parsesExtractedConstraintsAndSendsTheModelName() {
        List<String> sent = new ArrayList<>();
        OpenAILLMClient llm = new OpenAILLMClient("test-model", body -> {
            sent.add(body);
            return reply("Sure! {\"maxBudgetPerPerson\": 15, \"day\": \"FRIDAY\", \"startTime\": \"17:00\", "
                    + "\"endTime\": \"19:00\", \"location\": \"Osgoode Station\", \"foodPreferences\": [\"Wings\"]}");
        }, CLOCK);
        SearchConstraints c = llm.extractConstraints("wings near osgoode friday 5-7 under 15");
        assertEquals(Double.valueOf(15.0), c.getMaxBudgetPerPerson());
        assertEquals(DayOfWeek.FRIDAY, c.getDay());
        assertEquals(LocalTime.of(19, 0), c.getEndTime());
        assertEquals(List.of("wings"), c.getFoodPreferences());
        assertTrue(sent.get(0).contains("test-model"));
        assertTrue(sent.get(0).contains("Today is 2026-11-09 (MONDAY)"));
    }

    @Test
    void invalidValuesAreTreatedAsNotGiven() {
        OpenAILLMClient llm = new OpenAILLMClient("m", body -> reply("{\"day\": \"someday\", \"startTime\": \"late\"}"), CLOCK);
        SearchConstraints c = llm.extractConstraints("hi");
        assertEquals(List.of("budget", "time", "location"), c.getMissingFields());
    }

    @Test
    void replyWithoutJsonIsAnLlmException() {
        OpenAILLMClient llm = new OpenAILLMClient("m", body -> reply("I cannot help with that"), CLOCK);
        assertThrows(LLMException.class, () -> llm.extractConstraints("hi"));
    }

    @Test
    void emptyChoicesIsAnLlmException() {
        OpenAILLMClient llm = new OpenAILLMClient("m", body -> "{\"choices\":[]}", CLOCK);
        assertThrows(LLMException.class, () -> llm.askClarification(List.of("budget"), new SearchConstraints()));
    }

    @Test
    void clarificationAndExplanationReturnTheModelText() {
        OpenAILLMClient llm = new OpenAILLMClient("m", body -> reply("  What is your budget?  "), CLOCK);
        assertEquals("What is your budget?", llm.askClarification(List.of("budget"), new SearchConstraints()));
    }

    @Test
    void requestBodyNeverContainsTheApiKey() {
        List<String> sent = new ArrayList<>();
        OpenAILLMClient llm = new OpenAILLMClient("m", body -> { sent.add(body); return reply("ok"); }, CLOCK);
        llm.askClarification(List.of("budget"), new SearchConstraints());
        assertFalse(sent.get(0).contains("Bearer"));
    }
}
