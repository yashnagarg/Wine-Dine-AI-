package com.winedine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchConstraintsTest {
    private static SearchConstraints complete() {
        return new SearchConstraints()
                .setMaxBudgetPerPerson(15.0)
                .setStartTime(LocalTime.of(17, 0))
                .setEndTime(LocalTime.of(19, 0))
                .setLocation("Osgoode Station");
    }

    @Test
    void emptyConstraintsAreMissingBudgetTimeAndLocation() {
        SearchConstraints c = new SearchConstraints();
        assertEquals(List.of("budget", "time", "location"), c.getMissingFields());
        assertFalse(c.isComplete());
    }

    @Test
    void timeNeedsBothStartAndEnd() {
        SearchConstraints c = complete().setEndTime(null);
        assertEquals(List.of("time"), c.getMissingFields());
    }

    @Test
    void blankLocationCountsAsMissing() {
        assertEquals(List.of("location"), complete().setLocation("  ").getMissingFields());
    }

    @Test
    void completeConstraintsHaveNothingMissing() {
        assertTrue(complete().isComplete());
    }

    @Test
    void updateReplacesOnlyTheChangedValues() {
        SearchConstraints c = complete().setGroupSize(4).setFoodPreferences(List.of("truffle fries"));
        c.update(new SearchConstraints().setMaxBudgetPerPerson(20.0));
        assertEquals(Double.valueOf(20.0), c.getMaxBudgetPerPerson());
        assertEquals(Integer.valueOf(4), c.getGroupSize());
        assertEquals("Osgoode Station", c.getLocation());
        assertEquals(List.of("truffle fries"), c.getFoodPreferences());
    }

    @Test
    void changingLocationClearsOldCoordinates() {
        SearchConstraints c = complete().setSearchLatitude(43.65).setSearchLongitude(-79.38);
        c.update(new SearchConstraints().setLocation("College Station"));
        assertEquals("College Station", c.getLocation());
        assertNull(c.getSearchLatitude());
        assertNull(c.getSearchLongitude());
    }

    @Test
    void sameLocationKeepsCoordinates() {
        SearchConstraints c = complete().setSearchLatitude(43.65).setSearchLongitude(-79.38);
        c.update(new SearchConstraints().setLocation("osgoode station"));
        assertEquals(Double.valueOf(43.65), c.getSearchLatitude());
    }

    @Test
    void newPreferencesReplaceOldOnesOfTheSameKind() {
        SearchConstraints c = complete().setDrinkPreferences(List.of("beer"));
        c.update(new SearchConstraints().setDrinkPreferences(List.of("cocktails")));
        assertEquals(List.of("cocktails"), c.getDrinkPreferences());
    }

    @Test
    void rankingProfileDefaultsToBalanced() {
        assertEquals(RankingProfile.BALANCED, new SearchConstraints().getRankingProfile());
        SearchConstraints c = new SearchConstraints();
        c.update(new SearchConstraints().setRankingProfile(RankingProfile.BUDGET_FIRST));
        assertEquals(RankingProfile.BUDGET_FIRST, c.getRankingProfile());
    }

    @Test
    void copyIsIndependentOfTheOriginal() {
        SearchConstraints original = complete().setDay(DayOfWeek.FRIDAY).setFoodPreferences(List.of("tacos"));
        SearchConstraints copy = original.copy();
        copy.update(new SearchConstraints().setMaxBudgetPerPerson(30.0));
        assertEquals(Double.valueOf(15.0), original.getMaxBudgetPerPerson());
        assertEquals(DayOfWeek.FRIDAY, copy.getDay());
        assertEquals(List.of("tacos"), copy.getFoodPreferences());
    }

    @Test
    void allPreferencesCombinesFoodDrinkAndVenue() {
        SearchConstraints c = new SearchConstraints()
                .setFoodPreferences(List.of("tacos"))
                .setDrinkPreferences(List.of("beer"))
                .setVenuePreferences(List.of("patio"));
        assertEquals(List.of("tacos", "beer", "patio"), c.allPreferences());
    }

    @Test
    void defaultDistanceIsUsedWhenNoneGiven() {
        assertEquals(SearchConstraints.DEFAULT_MAX_DISTANCE_KM, new SearchConstraints().getMaxDistanceKmOrDefault(), 0.0001);
        assertEquals(5.0, new SearchConstraints().setMaxDistanceKm(5.0).getMaxDistanceKmOrDefault(), 0.0001);
    }
}
