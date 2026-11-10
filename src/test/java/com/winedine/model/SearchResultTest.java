package com.winedine.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchResultTest {
    private static Restaurant restaurant() {
        return new Restaurant("r1", "Maple Pub", "10 Test St", 43.65, -79.38, "Pub",
                List.of("patio"), null, null, null, List.of());
    }

    @Test
    void summaryReportsMissingFactsAsUnavailable() {
        HappyHourDeal deal = new HappyHourDeal(DayOfWeek.FRIDAY, LocalTime.of(16, 0), LocalTime.of(18, 0),
                "$6 pints", null, null, null);
        SearchResult result = new SearchResult(restaurant(), deal, List.of(), List.of(), null, false);
        String summary = result.getMatchSummary();
        assertTrue(summary.contains("Maple Pub"));
        assertTrue(summary.contains("Distance: unavailable"));
        assertTrue(summary.contains("Drink price: unavailable"));
        assertTrue(summary.contains("Average cost per person: unavailable"));
        assertTrue(summary.contains("Deal last updated: unavailable"));
        assertTrue(summary.contains("Matched preferences: none"));
    }

    @Test
    void summaryShowsKnownFactsAndWarnings() {
        HappyHourDeal deal = new HappyHourDeal(DayOfWeek.FRIDAY, LocalTime.of(16, 0), LocalTime.of(18, 0),
                "$6 pints", 6.0, 14.0, null);
        SearchResult result = new SearchResult(restaurant(), deal, List.of("patio"),
                List.of("Deal update date is unavailable"), 0.4, true);
        String summary = result.getMatchSummary();
        assertTrue(summary.contains("Distance: 0.4 km"));
        assertTrue(summary.contains("Drink price: $6.00"));
        assertTrue(summary.contains("Matched preferences: patio"));
        assertTrue(summary.contains("Near match"));
        assertTrue(summary.contains("Warning: Deal update date is unavailable"));
    }

    @Test
    void summaryHandlesMissingDeal() {
        SearchResult result = new SearchResult(restaurant(), null, List.of(), List.of(), 1.0, false);
        assertTrue(result.getMatchSummary().contains("no happy-hour deal at the requested time"));
    }
}
