package com.winedine.model;

import java.util.ArrayList;
import java.util.List;

/** Carries the outcome of an action from the controller to a view. */
public class AgentResponse {

    private final ResponseType type;
    private final String message;
    private final List<SearchResult> results;
    private final List<String> notices;

    private AgentResponse(ResponseType type, String message, List<SearchResult> results, List<String> notices) {
        this.type = type;
        this.message = message;
        this.results = results;
        this.notices = notices;
    }

    public static AgentResponse clarification(String question) {
        return new AgentResponse(ResponseType.CLARIFICATION, question, new ArrayList<>(), new ArrayList<>());
    }

    public static AgentResponse results(List<SearchResult> results, List<String> notices) {
        return new AgentResponse(ResponseType.RESULTS, null, results, notices);
    }

    public static AgentResponse error(String message) {
        return new AgentResponse(ResponseType.ERROR, message, new ArrayList<>(), new ArrayList<>());
    }

    public ResponseType getType() { return type; }
    public String getMessage() { return message; }
    public List<SearchResult> getResults() { return results; }
    public List<String> getNotices() { return notices; }

}
