package com.winedine.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.winedine.model.RankingProfile;
import com.winedine.model.SearchConstraints;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockLLMClientTest {
    // Monday 9 Nov 2026
    private final MockLLMClient llm = new MockLLMClient(Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void extractsAFullRequest() {
        SearchConstraints c = llm.extractConstraints("cheap beer and wings near Osgoode Station on Friday 5-7pm under $15");
        assertEquals(Double.valueOf(15.0), c.getMaxBudgetPerPerson());
        assertEquals(DayOfWeek.FRIDAY, c.getDay());
        assertEquals(LocalTime.of(17, 0), c.getStartTime());
        assertEquals(LocalTime.of(19, 0), c.getEndTime());
        assertEquals("osgoode station", c.getLocation());
        assertEquals(List.of("wings"), c.getFoodPreferences());
        assertEquals(List.of("beer"), c.getDrinkPreferences());
    }

    @Test
    void readsGroupSizeAndDrinkPrice() {
        SearchConstraints c = llm.extractConstraints("table for 4 near union, pints under $7 tonight");
        assertEquals(Integer.valueOf(4), c.getGroupSize());
        assertEquals(Double.valueOf(7.0), c.getMaxDrinkPrice());
        assertNull(c.getMaxBudgetPerPerson());
        assertEquals(DayOfWeek.MONDAY, c.getDay());
        assertEquals("union", c.getLocation());
    }

    @Test
    void nothingMentionedLeavesEverythingUnset() {
        SearchConstraints c = llm.extractConstraints("hello");
        assertEquals(List.of("budget", "time", "location"), c.getMissingFields());
    }

    @Test
    void aNumberRangeWithoutAmPmIsNotATime() {
        assertNull(llm.extractConstraints("something between 10 and 20 dollars").getStartTime());
    }

    @Test
    void afterTimeSetsOnlyAStart() {
        SearchConstraints c = llm.extractConstraints("anything after 6pm");
        assertEquals(LocalTime.of(18, 0), c.getStartTime());
        assertEquals(LocalTime.of(23, 59), c.getEndTime());
    }

    @Test
    void rankingPhrases() {
        assertEquals(RankingProfile.BUDGET_FIRST, llm.extractConstraints("cheapest first").getRankingProfile());
        assertEquals(RankingProfile.DISTANCE_FIRST, llm.extractConstraints("closest please").getRankingProfile());
    }

    @Test
    void refinementChangesOnlyWhatWasSaid() {
        SearchConstraints current = new SearchConstraints().setMaxBudgetPerPerson(15.0).setLocation("osgoode");
        SearchConstraints changes = llm.interpretRefinement("make it $20", current);
        assertEquals(Double.valueOf(20.0), changes.getMaxBudgetPerPerson());
        assertNull(changes.getLocation());
    }

    @Test
    void bareAnswersToClarificationQuestions() {
        SearchConstraints noBudget = new SearchConstraints().setLocation("x");
        assertEquals(Double.valueOf(15.0), llm.interpretRefinement("$15", noBudget).getMaxBudgetPerPerson());
        SearchConstraints noLocation = new SearchConstraints().setMaxBudgetPerPerson(10.0);
        assertEquals("Osgoode Station", llm.interpretRefinement("Osgoode Station", noLocation).getLocation());
    }

    @Test
    void clarificationQuestionNamesTheMissingDetails() {
        String q = llm.askClarification(List.of("budget", "time"), new SearchConstraints());
        assertTrue(q.contains("budget"));
        assertTrue(q.contains("time"));
    }

    @Test
    void twentyFourHourValuesAndNoonAreAccepted() {
        SearchConstraints c = llm.extractConstraints("what about time 12:00 pm to 22:00pm");
        assertEquals(LocalTime.of(12, 0), c.getStartTime());
        assertEquals(LocalTime.of(22, 0), c.getEndTime());
    }
}