package com.winedine.service;

import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;

/** Strategy: one way of scoring a result. A higher score ranks higher (F07). */
public interface RankingStrategy {
    double score(SearchResult result, SearchConstraints constraints);
}
