package com.winedine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParseException;
import com.winedine.model.RankingProfile;
import com.winedine.model.SearchConstraints;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConstraintsJsonTest {
    @Test
    void roundTripKeepsAllValues() {
        SearchConstraints original = new SearchConstraints().setMaxBudgetPerPerson(15.0).setDay(DayOfWeek.FRIDAY)
                .setStartTime(LocalTime.of(17, 0)).setEndTime(LocalTime.of(19, 0)).setLocation("Osgoode Station")
                .setFoodPreferences(List.of("tacos")).setRankingProfile(RankingProfile.BUDGET_FIRST)
                .setHappyHourRequired(true);
        SearchConstraints back = ConstraintsJson.from(original).toConstraints();
        assertEquals(Double.valueOf(15.0), back.getMaxBudgetPerPerson());
        assertEquals(DayOfWeek.FRIDAY, back.getDay());
        assertEquals(LocalTime.of(19, 0), back.getEndTime());
        assertEquals(List.of("tacos"), back.getFoodPreferences());
        assertEquals(RankingProfile.BUDGET_FIRST, back.getRankingProfile());
        assertEquals(true, back.isHappyHourRequired());
    }

    @Test
    void parseFindsTheJsonInsideExtraText() {
        ConstraintsJson j = ConstraintsJson.parse("Here you go:\n{\"groupSize\": 3}\nHope that helps");
        assertEquals(Integer.valueOf(3), j.toConstraints().getGroupSize());
    }

    @Test
    void negativeAndInvalidValuesAreIgnored() {
        ConstraintsJson j = ConstraintsJson.parse("{\"maxBudgetPerPerson\": -5, \"day\": \"funday\", \"rankingProfile\": \"x\"}");
        SearchConstraints c = j.toConstraints();
        assertEquals(List.of("budget", "time", "location"), c.getMissingFields());
        assertEquals(RankingProfile.BALANCED, c.getRankingProfile());
    }

    @Test
    void textWithoutJsonIsRejected() {
        assertThrows(JsonParseException.class, () -> ConstraintsJson.parse("sorry"));
    }
}
