package com.winedine.model;

import java.util.ArrayList;
import java.util.List;

/** One matched restaurant with its deal, matched preferences, warnings and distance. */
public class SearchResult {
    private Restaurant restaurant;
    private HappyHourDeal deal;
    private List<String> matchedPreferences = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private double distanceKm = Double.NaN;
    private boolean nearMatch;

    public SearchResult() { }

    public SearchResult(Restaurant restaurant, HappyHourDeal deal) {
        this.restaurant = restaurant;
        this.deal = deal;
    }

    /** Verified facts only; this is what the LLM is allowed to phrase (F08). */
    public String getMatchSummary() {
        StringBuilder sb = new StringBuilder(restaurant.getName());
        if (!matchedPreferences.isEmpty()) sb.append(" | matches: ").append(String.join(", ", matchedPreferences));
        if (deal != null) {
            sb.append(" | drink: ").append(deal.getDrinkPrice() == null ? "unavailable" : "$" + deal.getDrinkPrice());
            sb.append(" | avg/person: ").append(deal.getAvgCostPerPerson() == null ? "unavailable" : "$" + deal.getAvgCostPerPerson());
            sb.append(" | deal: ").append(deal.getStart()).append("-").append(deal.getEnd());
        }
        sb.append(" | distance: ").append(Double.isNaN(distanceKm) ? "unavailable" : String.format("%.1f km", distanceKm));
        if (nearMatch) sb.append(" | NEAR MATCH");
        if (!warnings.isEmpty()) sb.append(" | warnings: ").append(String.join("; ", warnings));
        return sb.toString();
    }

    public Restaurant getRestaurant() { return restaurant; }
    public HappyHourDeal getDeal() { return deal; }
    public List<String> getMatchedPreferences() { return matchedPreferences; }
    public List<String> getWarnings() { return warnings; }
    public double getDistanceKm() { return distanceKm; }
    public boolean isNearMatch() { return nearMatch; }

    public void setDistanceKm(double d) { distanceKm = d; }
    public void setNearMatch(boolean b) { nearMatch = b; }
    public void addWarning(String w) { warnings.add(w); }
    public void addMatchedPreference(String p) { matchedPreferences.add(p); }
}


