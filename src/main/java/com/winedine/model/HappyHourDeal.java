package com.winedine.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class HappyHourDeal {

    public static final int FRESH_DAYS = 30;

    private List<DayOfWeek> days;
    private LocalTime start;
    private LocalTime end;
    private Double drinkPrice;        // null = unavailable
    private Double avgCostPerPerson;  // null = unavailable
    private String description;
    private LocalDate lastUpdated;    // null = unknown

    public HappyHourDeal() { }

    public HappyHourDeal(List<DayOfWeek> days, LocalTime start, LocalTime end,
                         Double drinkPrice, Double avgCostPerPerson,
                         String description, LocalDate lastUpdated) {
        this.days = days;
        this.start = start;
        this.end = end;
        this.drinkPrice = drinkPrice;
        this.avgCostPerPerson = avgCostPerPerson;
        this.description = description;
        this.lastUpdated = lastUpdated;
    }

    /** True if the deal window overlaps [from, to] on the given day. */
    public boolean overlaps(DayOfWeek day, LocalTime from, LocalTime to) {
        if (days == null || start == null || end == null) return false;
        if (day != null && !days.contains(day)) return false;
        if (from == null || to == null) return true;
        return start.isBefore(to) && end.isAfter(from);
    }

    /** True if the deal is running at a single moment. */
    public boolean isAvailableAt(DayOfWeek day, LocalTime time) {
        if (days == null || start == null || end == null) return false;
        if (day != null && !days.contains(day)) return false;
        if (time == null) return true;
        return !time.isBefore(start) && time.isBefore(end);
    }

    /** True if the deal fits the budget. Unknown prices never fail here; the caller adds a warning. */
    public boolean matchesBudget(Double maxBudgetPerPerson, Double maxDrinkPrice) {
        if (maxBudgetPerPerson != null && avgCostPerPerson != null
                && avgCostPerPerson > maxBudgetPerPerson) return false;
        if (maxDrinkPrice != null && drinkPrice != null
                && drinkPrice > maxDrinkPrice) return false;
        return true;
    }

    public boolean hasUnknownPrice() {
        return drinkPrice == null || avgCostPerPerson == null;
    }

    public Freshness getFreshness(LocalDate today) {
        if (lastUpdated == null) return Freshness.UNKNOWN;
        long age = ChronoUnit.DAYS.between(lastUpdated, today);
        return age <= FRESH_DAYS ? Freshness.FRESH : Freshness.STALE;
    }

    public List<DayOfWeek> getDays() { return days; }
    public LocalTime getStart() { return start; }
    public LocalTime getEnd() { return end; }
    public Double getDrinkPrice() { return drinkPrice; }
    public Double getAvgCostPerPerson() { return avgCostPerPerson; }
    public String getDescription() { return description; }
    public LocalDate getLastUpdated() { return lastUpdated; }
}
    

