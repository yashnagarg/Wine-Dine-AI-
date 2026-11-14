package com.winedine.service;

import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;

/** "Cheapest first": cost dominates the score. */
public class BudgetFirstRanking implements RankingStrategy {
    @Override
    public double score(SearchResult result, SearchConstraints c) {
        return 4 * ScoreParts.matchRatio(result, c) - 0.3 * ScoreParts.distanceKm(result) - 1.0 * ScoreParts.cost(result) - ScoreParts.noDealPenalty(result, c);
    }
}