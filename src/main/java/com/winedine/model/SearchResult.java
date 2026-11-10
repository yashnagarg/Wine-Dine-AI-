package com.winedine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** One recommended restaurant with its deal and the reasons it matched. */
public class SearchResult {
    private final Restaurant restaurant;
    private final HappyHourDeal happyHourDeal;
    private final List<String> matchedPreferences;
    private final List<String> warnings;
    private final Double distanceKm;
    private final boolean nearMatch;
    private double score;
    private boolean comparableAlternative;

    public SearchResult(Restaurant restaurant, HappyHourDeal happyHourDeal, List<String> matchedPreferences,
                        List<String> warnings, Double distanceKm, boolean nearMatch) {
        this.restaurant = restaurant;
        this.happyHourDeal = happyHourDeal;
        this.matchedPreferences = matchedPreferences == null ? new ArrayList<>() : new ArrayList<>(matchedPreferences);
        this.warnings = warnings == null ? new ArrayList<>() : new ArrayList<>(warnings);
        this.distanceKm = distanceKm;
        this.nearMatch = nearMatch;
    }

    public Restaurant getRestaurant() { return restaurant; }
    public HappyHourDeal getHappyHourDeal() { return happyHourDeal; }
    public List<String> getMatchedPreferences() { return Collections.unmodifiableList(matchedPreferences); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public Double getDistanceKm() { return distanceKm; }
    public boolean isNearMatch() { return nearMatch; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public boolean isComparableAlternative() { return comparableAlternative; }
    public void setComparableAlternative(boolean v) { this.comparableAlternative = v; }

    /**
     * The verified facts about this recommendation, one per line. This text is the only
     * information the LLM may use when it phrases an explanation (feature F08).
     */
    public String getMatchSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Restaurant: ").append(restaurant.getName());
        if (!restaurant.getCuisine().isEmpty()) {
            sb.append(" (").append(restaurant.getCuisine()).append(")");
        }
        sb.append("\n");
        sb.append("Address: ").append(restaurant.getAddress().isEmpty() ? "unavailable" : restaurant.getAddress()).append("\n");
        sb.append("Distance: ").append(distanceKm == null ? "unavailable"
                : String.format(Locale.ROOT, "%.1f km", distanceKm)).append("\n");
        sb.append("Matched preferences: ").append(matchedPreferences.isEmpty() ? "none"
                : String.join(", ", matchedPreferences)).append("\n");
        if (happyHourDeal == null) {
            sb.append("Deal: no happy-hour deal at the requested time\n");
        } else {
            sb.append("Deal: ").append(happyHourDeal.getDescription());
            if (happyHourDeal.getDay() != null) {
                sb.append(" (").append(happyHourDeal.getDay()).append(" ")
                        .append(happyHourDeal.getStartTime()).append("-").append(happyHourDeal.getEndTime()).append(")");
            }
            sb.append("\n");
            sb.append("Drink price: ").append(money(happyHourDeal.getDrinkPrice())).append("\n");
            sb.append("Average cost per person: ").append(money(happyHourDeal.getAvgCostPerPerson())).append("\n");
            sb.append("Deal last updated: ").append(happyHourDeal.getLastUpdated() == null ? "unavailable"
                    : happyHourDeal.getLastUpdated() + " (" + happyHourDeal.getFreshness() + ")").append("\n");
        }
        if (nearMatch) {
            sb.append("Near match: this is the closest alternative, not an exact match\n");
        }
        for (String w : warnings) {
            sb.append("Warning: ").append(w).append("\n");
        }
        return sb.toString();
    }

    private static String money(Double v) {
        return v == null ? "unavailable" : String.format(Locale.ROOT, "$%.2f", v);
    }
}
