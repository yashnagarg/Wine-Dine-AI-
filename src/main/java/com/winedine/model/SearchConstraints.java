package com.winedine.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Structured search requirements extracted from natural language (F01, F02, F09). */
public class SearchConstraints {

    private Integer groupSize;
    private Double maxBudgetPerPerson;
    private Double maxDrinkPrice;
    private DayOfWeek day;
    private LocalTime startTime;
    private LocalTime endTime;
    private String locationText;
    private Double latitude;     // set after location resolution
    private Double longitude;
    private Double maxDistanceKm;
    private List<String> preferences = new ArrayList<>();
    private boolean happyHourRequired;
    private RankingProfile rankingProfile = RankingProfile.BALANCED;

    public SearchConstraints() { }

    /** Required constraints that are still missing (plain Java, no AI). */
    public List<String> getMissingFields() {
        List<String> missing = new ArrayList<>();
        if (maxBudgetPerPerson == null && maxDrinkPrice == null) missing.add("budget");
        if (startTime == null || endTime == null) missing.add("time");
        if (locationText == null || locationText.isBlank()) missing.add("location");
        return missing;
    }

    public boolean isComplete() {
        return getMissingFields().isEmpty();
    }

    public boolean hasResolvedLocation() {
        return latitude != null && longitude != null;
    }

    /** Merge changed constraints into this one; null/empty fields in {@code changes} are left untouched (F09). */
    public void update(SearchConstraints changes) {
        if (changes == null) return;
        if (changes.groupSize != null) groupSize = changes.groupSize;
        if (changes.maxBudgetPerPerson != null) maxBudgetPerPerson = changes.maxBudgetPerPerson;
        if (changes.maxDrinkPrice != null) maxDrinkPrice = changes.maxDrinkPrice;
        if (changes.day != null) day = changes.day;
        if (changes.startTime != null) startTime = changes.startTime;
        if (changes.endTime != null) endTime = changes.endTime;
        if (changes.locationText != null && !changes.locationText.isBlank()) {
            locationText = changes.locationText;
            latitude = null;   // must be re-resolved
            longitude = null;
        }
        if (changes.maxDistanceKm != null) maxDistanceKm = changes.maxDistanceKm;
        if (changes.preferences != null && !changes.preferences.isEmpty()) {
            for (String p : changes.preferences) {
                if (!preferences.contains(p)) preferences.add(p);
            }
        }
        if (changes.happyHourRequired) happyHourRequired = true;
        if (changes.rankingProfile != null && changes.rankingProfile != RankingProfile.BALANCED) {
            rankingProfile = changes.rankingProfile;
        }
    }

    public Integer getGroupSize() { return groupSize; }
    public Double getMaxBudgetPerPerson() { return maxBudgetPerPerson; }
    public Double getMaxDrinkPrice() { return maxDrinkPrice; }
    public DayOfWeek getDay() { return day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getLocationText() { return locationText; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getMaxDistanceKm() { return maxDistanceKm; }
    public List<String> getPreferences() { return preferences; }
    public boolean isHappyHourRequired() { return happyHourRequired; }
    public RankingProfile getRankingProfile() { return rankingProfile; }

    public void setGroupSize(Integer v) { groupSize = v; }
    public void setMaxBudgetPerPerson(Double v) { maxBudgetPerPerson = v; }
    public void setMaxDrinkPrice(Double v) { maxDrinkPrice = v; }
    public void setDay(DayOfWeek v) { day = v; }
    public void setStartTime(LocalTime v) { startTime = v; }
    public void setEndTime(LocalTime v) { endTime = v; }
    public void setLocationText(String v) { locationText = v; }
    public void setLatitude(Double v) { latitude = v; }
    public void setLongitude(Double v) { longitude = v; }
    public void setMaxDistanceKm(Double v) { maxDistanceKm = v; }
    public void setPreferences(List<String> v) { preferences = v; }
    public void setHappyHourRequired(boolean v) { happyHourRequired = v; }
    public void setRankingProfile(RankingProfile v) { rankingProfile = v; }
    
}
