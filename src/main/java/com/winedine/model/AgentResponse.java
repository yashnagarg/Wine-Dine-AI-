package com.winedine.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What the agent returns to the GUI or CLI: a clarification question, results, or an error. */
public final class AgentResponse {
    private final ResponseType type;
    private final String message;
    private final SearchConstraints constraints;
    private final List<SearchResult> results;

    private AgentResponse(ResponseType type, String message, SearchConstraints constraints,
                          List<SearchResult> results) {
        this.type = type;
        this.message = message == null ? "" : message;
        this.constraints = constraints;
        this.results = results == null ? new ArrayList<>() : new ArrayList<>(results);
    }

    public static AgentResponse clarification(String question, SearchConstraints constraints) {
        return new AgentResponse(ResponseType.CLARIFICATION, question, constraints, null);
    }

    /** {@code notice} may be empty; it carries messages such as "showing near matches". */
    public static AgentResponse results(List<SearchResult> results, SearchConstraints constraints, String notice) {
        return new AgentResponse(ResponseType.RESULTS, notice, constraints, results);
    }

    public static AgentResponse error(String message) {
        return new AgentResponse(ResponseType.ERROR, message, null, null);
    }

    public ResponseType getType() { return type; }
    public String getMessage() { return message; }
    public SearchConstraints getConstraints() { return constraints; }
    public List<SearchResult> getResults() { return Collections.unmodifiableList(results); }
}
