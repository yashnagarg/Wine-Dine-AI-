package com.winedine.model;

import java.time.LocalDateTime;
import java.util.List;

/** One saved search (F12). */
public class SearchHistory {
    public static final int OUTDATED_DAYS = 30;

    private String id;
    private String query;
    private SearchConstraints constraints;
    private List<SearchResult> results;
    private LocalDateTime savedAt;

    public SearchHistory() { }

    public SearchHistory(String id, String query, SearchConstraints constraints,
                         List<SearchResult> results, LocalDateTime savedAt) {
        this.id = id;
        this.query = query;
        this.constraints = constraints;
        this.results = results;
        this.savedAt = savedAt;
    }

    public boolean isOutdated(LocalDateTime now) {
        return savedAt == null || savedAt.plusDays(OUTDATED_DAYS).isBefore(now);
    }

    public String getId() { return id; }
    public String getQuery() { return query; }
    public SearchConstraints getConstraints() { return constraints; }
    public List<SearchResult> getResults() { return results; }
    public LocalDateTime getSavedAt() { return savedAt; }
}