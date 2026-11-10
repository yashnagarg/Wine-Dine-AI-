package com.winedine.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** One saved search: what the user asked, the constraints, the results, and when. */
public class SearchHistory {
    private final String id;
    private final String originalQuery;
    private final SearchConstraints constraints;
    private final List<SearchResult> results;
    private final LocalDateTime timestamp;

    public SearchHistory(String originalQuery, SearchConstraints constraints, List<SearchResult> results,
                         LocalDateTime timestamp) {
        this(UUID.randomUUID().toString(), originalQuery, constraints, results, timestamp);
    }

    public SearchHistory(String id, String originalQuery, SearchConstraints constraints,
                         List<SearchResult> results, LocalDateTime timestamp) {
        this.id = id;
        this.originalQuery = originalQuery == null ? "" : originalQuery;
        this.constraints = constraints == null ? new SearchConstraints() : constraints.copy();
        this.results = results == null ? new ArrayList<>() : new ArrayList<>(results);
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getOriginalQuery() { return originalQuery; }
    public SearchConstraints getConstraints() { return constraints.copy(); }
    public List<SearchResult> getResults() { return Collections.unmodifiableList(results); }
    public LocalDateTime getTimestamp() { return timestamp; }

    /** One-line description for the history list. */
    public String getSummary() {
        return timestamp.truncatedTo(java.time.temporal.ChronoUnit.MINUTES).toString().replace('T', ' ')
                + " | \"" + originalQuery + "\" | " + results.size() + " result(s)";
    }
}
