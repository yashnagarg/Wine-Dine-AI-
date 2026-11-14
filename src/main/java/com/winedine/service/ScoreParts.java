package com.winedine.service;

import com.winedine.model.HappyHourDeal;
import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;

/** The measurable parts of a result that the ranking strategies weigh differently. */
final class ScoreParts {
    static final double UNKNOWN_DISTANCE_KM = 10.0;
    static final double UNKNOWN_COST = 25.0;
    static final double NO_DEAL_PENALTY = 3.0;

    private ScoreParts() { }

    /** Share of the user's preferences that matched (0 when none were given). */
    static double matchRatio(SearchResult result, SearchConstraints c) {
        int total = c.allPreferences().size();
        return total == 0 ? 0.0 : (double) result.getMatchedPreferences().size() / total;
    }

    static double distanceKm(SearchResult result) {
        return result.getDistanceKm() == null ? UNKNOWN_DISTANCE_KM : result.getDistanceKm();
    }

    static double cost(SearchResult result) {
        HappyHourDeal deal = result.getHappyHourDeal();
        return deal == null || deal.getAvgCostPerPerson() == null ? UNKNOWN_COST : deal.getAvgCostPerPerson();
    }

    /** Places with no deal at the requested time rank below places that have one. */
    static double noDealPenalty(SearchResult result, SearchConstraints c) {
        return result.getHappyHourDeal() == null && c.hasTimeWindow() ? NO_DEAL_PENALTY : 0.0;
    }
}