package com.winedine.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The structured requirements extracted from the user's words.
 * Unset values are null; the agent asks the user to clarify missing required ones.
 */
public class SearchConstraints {
    public static final double DEFAULT_MAX_DISTANCE_KM = 2.0;

    private Integer groupSize;
    private Double maxBudgetPerPerson;
    private Double maxDrinkPrice;
    private DayOfWeek day;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private Double searchLatitude;
    private Double searchLongitude;
    private Double maxDistanceKm;
    private List<String> foodPreferences = new ArrayList<>();
    private List<String> drinkPreferences = new ArrayList<>();
    private List<String> venuePreferences = new ArrayList<>();
    private Boolean happyHourRequired;
    private RankingProfile rankingProfile;

    public Integer getGroupSize() { return groupSize; }
    public Double getMaxBudgetPerPerson() { return maxBudgetPerPerson; }
    public Double getMaxDrinkPrice() { return maxDrinkPrice; }
    public DayOfWeek getDay() { return day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getLocation() { return location; }
    public Double getSearchLatitude() { return searchLatitude; }
    public Double getSearchLongitude() { return searchLongitude; }
    public Double getMaxDistanceKm() { return maxDistanceKm; }
    public List<String> getFoodPreferences() { return new ArrayList<>(foodPreferences); }
    public List<String> getDrinkPreferences() { return new ArrayList<>(drinkPreferences); }
    public List<String> getVenuePreferences() { return new ArrayList<>(venuePreferences); }
    public boolean isHappyHourRequired() { return Boolean.TRUE.equals(happyHourRequired); }
    public RankingProfile getRankingProfile() {
        return rankingProfile == null ? RankingProfile.BALANCED : rankingProfile;
    }

    public SearchConstraints setGroupSize(Integer v) { this.groupSize = v; return this; }
    public SearchConstraints setMaxBudgetPerPerson(Double v) { this.maxBudgetPerPerson = v; return this; }
    public SearchConstraints setMaxDrinkPrice(Double v) { this.maxDrinkPrice = v; return this; }
    public SearchConstraints setDay(DayOfWeek v) { this.day = v; return this; }
    public SearchConstraints setStartTime(LocalTime v) { this.startTime = v; return this; }
    public SearchConstraints setEndTime(LocalTime v) { this.endTime = v; return this; }
    public SearchConstraints setLocation(String v) { this.location = v; return this; }
    public SearchConstraints setSearchLatitude(Double v) { this.searchLatitude = v; return this; }
    public SearchConstraints setSearchLongitude(Double v) { this.searchLongitude = v; return this; }
    public SearchConstraints setMaxDistanceKm(Double v) { this.maxDistanceKm = v; return this; }
    public SearchConstraints setFoodPreferences(List<String> v) { this.foodPreferences = copyOf(v); return this; }
    public SearchConstraints setDrinkPreferences(List<String> v) { this.drinkPreferences = copyOf(v); return this; }
    public SearchConstraints setVenuePreferences(List<String> v) { this.venuePreferences = copyOf(v); return this; }
    public SearchConstraints setHappyHourRequired(Boolean v) { this.happyHourRequired = v; return this; }
    public SearchConstraints setRankingProfile(RankingProfile v) { this.rankingProfile = v; return this; }

    public boolean hasTimeWindow() { return startTime != null && endTime != null; }

    public boolean hasLocationCoordinates() { return searchLatitude != null && searchLongitude != null; }

    public double getMaxDistanceKmOrDefault() {
        return maxDistanceKm == null ? DEFAULT_MAX_DISTANCE_KM : maxDistanceKm;
    }

    /** All food, drink and venue preferences together. */
    public List<String> allPreferences() {
        List<String> all = new ArrayList<>(foodPreferences);
        all.addAll(drinkPreferences);
        all.addAll(venuePreferences);
        return all;
    }

    /** Names of required constraints that are still undefined: "budget", "time", "location". */
    public List<String> getMissingFields() {
        List<String> missing = new ArrayList<>();
        if (maxBudgetPerPerson == null) {
            missing.add("budget");
        }
        if (!hasTimeWindow()) {
            missing.add("time");
        }
        if (location == null || location.isBlank()) {
            missing.add("location");
        }
        return missing;
    }

    public boolean isComplete() { return getMissingFields().isEmpty(); }

    /**
     * Applies a set of changes: every value set in {@code changes} replaces the current one,
     * everything else is kept. Changing the location clears old coordinates so they get re-resolved.
     */
    public void update(SearchConstraints changes) {
        if (changes == null) {
            return;
        }
        if (changes.groupSize != null) groupSize = changes.groupSize;
        if (changes.maxBudgetPerPerson != null) maxBudgetPerPerson = changes.maxBudgetPerPerson;
        if (changes.maxDrinkPrice != null) maxDrinkPrice = changes.maxDrinkPrice;
        if (changes.day != null) day = changes.day;
        if (changes.startTime != null) startTime = changes.startTime;
        if (changes.endTime != null) endTime = changes.endTime;
        if (changes.location != null && !changes.location.isBlank()) {
            String newLocation = changes.location.trim();
            if (!newLocation.equalsIgnoreCase(location == null ? "" : location.trim())) {
                searchLatitude = null;
                searchLongitude = null;
            }
            location = newLocation;
        }
        if (changes.searchLatitude != null) searchLatitude = changes.searchLatitude;
        if (changes.searchLongitude != null) searchLongitude = changes.searchLongitude;
        if (changes.maxDistanceKm != null) maxDistanceKm = changes.maxDistanceKm;
        if (!changes.foodPreferences.isEmpty()) foodPreferences = copyOf(changes.foodPreferences);
        if (!changes.drinkPreferences.isEmpty()) drinkPreferences = copyOf(changes.drinkPreferences);
        if (!changes.venuePreferences.isEmpty()) venuePreferences = copyOf(changes.venuePreferences);
        if (changes.happyHourRequired != null) happyHourRequired = changes.happyHourRequired;
        if (changes.rankingProfile != null) rankingProfile = changes.rankingProfile;
    }

    public SearchConstraints copy() {
        SearchConstraints c = new SearchConstraints();
        c.update(this);
        return c;
    }

    private static List<String> copyOf(List<String> v) {
        return v == null ? new ArrayList<>() : new ArrayList<>(v);
    }
}
