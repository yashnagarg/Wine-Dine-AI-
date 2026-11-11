package com.winedine.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/** A happy-hour deal at a restaurant. Prices and the update date may be unknown (null). */
public class HappyHourDeal {
    private final DayOfWeek day;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String description;
    private final Double drinkPrice;
    private final Double avgCostPerPerson;
    private final LocalDate lastUpdated;

    public HappyHourDeal(DayOfWeek day, LocalTime startTime, LocalTime endTime, String description,
                         Double drinkPrice, Double avgCostPerPerson, LocalDate lastUpdated) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description == null ? "" : description;
        this.drinkPrice = drinkPrice;
        this.avgCostPerPerson = avgCostPerPerson;
        this.lastUpdated = lastUpdated;
    }

    public DayOfWeek getDay() { return day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getDescription() { return description; }
    public Double getDrinkPrice() { return drinkPrice; }
    public Double getAvgCostPerPerson() { return avgCostPerPerson; }
    public LocalDate getLastUpdated() { return lastUpdated; }

    /**
     * True if this deal runs on the given day and overlaps the requested time window.
     * A null day or null time means "no restriction" for that part.
     */
    public boolean isAvailableAt(DayOfWeek requestedDay, LocalTime from, LocalTime to) {
        if (requestedDay != null && day != null && requestedDay != day) {
            return false;
        }
        if (from == null || to == null || startTime == null || endTime == null) {
            return true;
        }
        for (int[] deal : segments(startTime, endTime)) {
            for (int[] window : segments(from, to)) {
                if (deal[0] < window[1] && window[0] < deal[1]) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Minute-of-day intervals covered by a time range. A range that ends at midnight or at an earlier
     * time than it starts (for example 22:00-02:00) runs past midnight and is split in two.
     */
    private static int[][] segments(LocalTime start, LocalTime end) {
        int s = start.getHour() * 60 + start.getMinute();
        int e = end.getHour() * 60 + end.getMinute();
        if (e == 0) {
            return new int[][] {{s, 24 * 60}};
        }
        if (e < s) {
            return new int[][] {{s, 24 * 60}, {0, e}};
        }
        return new int[][] {{s, e}};
    }

    public boolean isCostKnown() { return avgCostPerPerson != null; }

    public boolean isDrinkPriceKnown() { return drinkPrice != null; }

    /** True only if the cost is known and at most the given per-person budget. */
    public boolean matchesBudget(double maxPerPerson) {
        return avgCostPerPerson != null && avgCostPerPerson <= maxPerPerson;
    }

    /** True only if the drink price is known and at most the given limit. */
    public boolean matchesDrinkPrice(double maxDrinkPrice) {
        return drinkPrice != null && drinkPrice <= maxDrinkPrice;
    }

    public Freshness getFreshness(LocalDate today) {
        return Freshness.of(lastUpdated, today);
    }

    public Freshness getFreshness() {
        return getFreshness(LocalDate.now());
    }
}