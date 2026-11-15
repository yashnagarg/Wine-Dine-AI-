package com.winedine.ui;

import com.winedine.model.Freshness;
import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchResult;

import java.time.LocalDate;
import java.util.Locale;

/** Turns results into display text. Shared by the CLI and the GUI so both show the same information. */
public final class ResultFormatter {
    private ResultFormatter() { }

    /** How fresh the data behind a result is: the deal's date if there is a deal, else the restaurant's. */
    public static Freshness freshness(SearchResult r) {
        return r.getHappyHourDeal() == null
                ? Freshness.of(r.getRestaurant().getLastUpdated(), LocalDate.now())
                : r.getHappyHourDeal().getFreshness();
    }

    /** One-line summary for a list. */
    public static String line(int number, SearchResult r) {
        Restaurant rest = r.getRestaurant();
        StringBuilder sb = new StringBuilder();
        sb.append(number).append(". ").append(rest.getName());
        if (!rest.getCuisine().isEmpty()) {
            sb.append(" (").append(rest.getCuisine()).append(")");
        }
        sb.append(" - ").append(r.getDistanceKm() == null ? "distance unavailable"
                : String.format(Locale.ROOT, "%.1f km", r.getDistanceKm()));
        HappyHourDeal deal = r.getHappyHourDeal();
        if (deal != null) {
            sb.append(" - ").append(deal.getDescription());
        }
        if (r.isNearMatch()) {
            sb.append(" [near match]");
        } else if (r.isComparableAlternative()) {
            sb.append(" [comparable]");
        }
        return sb.toString();
    }

    /** Full details of one result, including freshness and warnings. */
    public static String details(SearchResult r) {
        Restaurant rest = r.getRestaurant();
        HappyHourDeal deal = r.getHappyHourDeal();
        StringBuilder sb = new StringBuilder();
        sb.append(rest.getName()).append("\n");
        sb.append("  Address:   ").append(rest.getAddress().isEmpty() ? "unavailable" : rest.getAddress()).append("\n");
        sb.append("  Distance:  ").append(r.getDistanceKm() == null ? "unavailable"
                : String.format(Locale.ROOT, "%.1f km", r.getDistanceKm())).append("\n");
        if (deal == null) {
            sb.append("  Deal:      none found for the requested time\n");
        } else {
            sb.append("  Deal:      ").append(deal.getDescription()).append("\n");
            sb.append("  Hours:     ").append(deal.getDay()).append(" ").append(deal.getStartTime())
                    .append("-").append(deal.getEndTime()).append("\n");
            sb.append("  Cost:      ").append(deal.isCostKnown()
                    ? String.format(Locale.ROOT, "about $%.2f per person", deal.getAvgCostPerPerson())
                    : "unavailable").append("\n");
        }
        Freshness freshness = deal == null ? Freshness.of(rest.getLastUpdated(), LocalDate.now())
                : deal.getFreshness();
        sb.append("  Data:      ").append(freshness).append("\n");
        if (!r.getMatchedPreferences().isEmpty()) {
            sb.append("  Matches:   ").append(String.join(", ", r.getMatchedPreferences())).append("\n");
        }
        for (String w : r.getWarnings()) {
            sb.append("  ! ").append(w).append("\n");
        }
        return sb.toString();
    }
}