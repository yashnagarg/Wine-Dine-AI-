package com.winedine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class HappyHourDealTest {
    private static HappyHourDeal fridayDeal(Double drink, Double cost, LocalDate updated) {
        return new HappyHourDeal(DayOfWeek.FRIDAY, LocalTime.of(16, 0), LocalTime.of(18, 0),
                "$6 pints", drink, cost, updated);
    }

    @Test
    void overlappingWindowOnSameDayIsAvailable() {
        HappyHourDeal deal = fridayDeal(6.0, 14.0, null);
        assertTrue(deal.isAvailableAt(DayOfWeek.FRIDAY, LocalTime.of(17, 0), LocalTime.of(19, 0)));
    }

    @Test
    void windowThatOnlyTouchesTheEndIsNotAvailable() {
        HappyHourDeal deal = fridayDeal(6.0, 14.0, null);
        assertFalse(deal.isAvailableAt(DayOfWeek.FRIDAY, LocalTime.of(18, 0), LocalTime.of(20, 0)));
    }

    @Test
    void otherDayIsNotAvailable() {
        HappyHourDeal deal = fridayDeal(6.0, 14.0, null);
        assertFalse(deal.isAvailableAt(DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(18, 0)));
    }

    @Test
    void nullDayOrTimeMeansNoRestriction() {
        HappyHourDeal deal = fridayDeal(6.0, 14.0, null);
        assertTrue(deal.isAvailableAt(null, null, null));
        assertTrue(deal.isAvailableAt(DayOfWeek.FRIDAY, null, null));
    }

    @Test
    void budgetMatchRequiresKnownCostAtOrBelowLimit() {
        assertTrue(fridayDeal(6.0, 14.0, null).matchesBudget(15));
        assertTrue(fridayDeal(6.0, 15.0, null).matchesBudget(15));
        assertFalse(fridayDeal(6.0, 16.0, null).matchesBudget(15));
        assertFalse(fridayDeal(6.0, null, null).matchesBudget(15));
    }

    @Test
    void drinkPriceMatchRequiresKnownPrice() {
        assertTrue(fridayDeal(6.0, null, null).matchesDrinkPrice(10));
        assertFalse(fridayDeal(12.0, null, null).matchesDrinkPrice(10));
        assertFalse(fridayDeal(null, null, null).matchesDrinkPrice(10));
    }

    @Test
    void freshnessUsesLastUpdatedDate() {
        LocalDate today = LocalDate.of(2026, 11, 2);
        assertEquals(Freshness.FRESH, fridayDeal(6.0, 14.0, today.minusDays(5)).getFreshness(today));
        assertEquals(Freshness.STALE, fridayDeal(6.0, 14.0, today.minusDays(60)).getFreshness(today));
        assertEquals(Freshness.UNKNOWN, fridayDeal(6.0, 14.0, null).getFreshness(today));
    }

    @Test
    void dealThatRunsPastMidnightMatchesLateWindows() {
        HappyHourDeal late = new HappyHourDeal(DayOfWeek.TUESDAY, LocalTime.of(22, 0), LocalTime.of(2, 0),
                "late night", 7.0, null, null);
        assertTrue(late.isAvailableAt(DayOfWeek.TUESDAY, LocalTime.of(22, 0), LocalTime.of(23, 30)));
        assertTrue(late.isAvailableAt(DayOfWeek.TUESDAY, LocalTime.of(21, 0), LocalTime.of(23, 0)));
        assertFalse(late.isAvailableAt(DayOfWeek.TUESDAY, LocalTime.of(17, 0), LocalTime.of(19, 0)));
    }

    @Test
    void dealThatEndsAtMidnightDoesNotMatchTheAfternoon() {
        HappyHourDeal evening = new HappyHourDeal(DayOfWeek.FRIDAY, LocalTime.of(21, 0), LocalTime.MIDNIGHT,
                "late", 5.0, null, null);
        assertTrue(evening.isAvailableAt(DayOfWeek.FRIDAY, LocalTime.of(20, 0), LocalTime.of(22, 0)));
        assertFalse(evening.isAvailableAt(DayOfWeek.FRIDAY, LocalTime.of(17, 0), LocalTime.of(19, 0)));
    }

    @Test
    void searchWindowThatCrossesMidnightMatchesLateDeals() {
        HappyHourDeal late = new HappyHourDeal(DayOfWeek.TUESDAY, LocalTime.of(22, 0), LocalTime.of(2, 0),
                "late night", 7.0, null, null);
        assertTrue(late.isAvailableAt(DayOfWeek.TUESDAY, LocalTime.of(22, 0), LocalTime.of(1, 0)));
        assertFalse(fridayDeal(6.0, 14.0, null).isAvailableAt(DayOfWeek.FRIDAY, LocalTime.of(22, 0), LocalTime.of(1, 0)));
    }
}