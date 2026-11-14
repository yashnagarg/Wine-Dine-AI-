package com.winedine.service;

import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;

/** "Closest first": distance dominates the score. */
public class DistanceFirstRanking implements RankingStrategy {
    @Override
    public double score(SearchResult result, SearchConstraints c) {
        return 4 * ScoreParts.matchRatio(result, c) - 3.0 * ScoreParts.distanceKm(result) - 0.1 * ScoreParts.cost(result) - ScoreParts.noDealPenalty(result, c);
    }
}