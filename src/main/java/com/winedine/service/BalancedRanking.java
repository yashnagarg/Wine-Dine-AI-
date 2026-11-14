package com.winedine.service;

import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;

/** Default ranking: preference matches count most, then distance and cost. */
public class BalancedRanking implements RankingStrategy {
    @Override
    public double score(SearchResult result, SearchConstraints c) {
        return 10 * ScoreParts.matchRatio(result, c) - 1.0 * ScoreParts.distanceKm(result) - 0.2 * ScoreParts.cost(result) - ScoreParts.noDealPenalty(result, c);
    }
}