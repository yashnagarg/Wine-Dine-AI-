package com.winedine.agent;

import com.winedine.data.DataLoadException;
import com.winedine.data.LocationNotFoundException;
import com.winedine.llm.LLMClient;
import com.winedine.llm.LLMException;
import com.winedine.llm.MockLLMClient;
import com.winedine.model.AgentResponse;
import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;
import com.winedine.service.RecommendationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The agent: decides the next step of a conversation. The LLM understands and phrases text;
 * all searching, filtering and ranking is done by {@link RecommendationService}.
 * One agent instance holds the state of one ongoing search.
 */
public class RestaurantAgent {
    /** How many times the agent asks for missing details before it searches with what it has. */
    public static final int MAX_CLARIFICATION_ROUNDS = 2;

    private final LLMClient llm;
    private final RecommendationService service;

    private SearchConstraints constraints = new SearchConstraints();
    private List<SearchResult> results = new ArrayList<>();
    private int clarificationRounds;
    private boolean awaitingClarification;

    public RestaurantAgent(LLMClient llm, RecommendationService service) {
        this.llm = llm;
        this.service = service;
    }

    public SearchConstraints getConstraints() { return constraints.copy(); }

    public List<SearchResult> getResults() { return new ArrayList<>(results); }

    public boolean isAwaitingClarification() { return awaitingClarification; }

    public boolean hasActiveSearch() { return awaitingClarification || !results.isEmpty() || constraints.getLocation() != null; }

    public String describeLlm() { return llm.describe(); }

    /** Starts a new search from a free-text request (F01). */
    public AgentResponse handleRequest(String text) {
        reset();
        try {
            constraints.update(llm.extractConstraints(text));
        } catch (LLMException e) {
            return AgentResponse.error(e.getMessage());
        }
        return advance();
    }

    /** Applies the user's answer to a clarification question (F02). */
    public AgentResponse answerClarification(String text) {
        clarificationRounds++;
        if (!isSkip(text)) {
            try {
                constraints.update(llm.interpretRefinement(text, constraints));
            } catch (LLMException e) {
                return AgentResponse.error(e.getMessage());
            }
        } else {
            clarificationRounds = MAX_CLARIFICATION_ROUNDS;
        }
        return advance();
    }

    /** Changes the current search, e.g. "make it $20" or "closer please", and searches again (F09). */
    public AgentResponse refine(String text) {
        try {
            constraints.update(llm.interpretRefinement(text, constraints));
        } catch (LLMException e) {
            return AgentResponse.error(e.getMessage());
        }
        clarificationRounds = MAX_CLARIFICATION_ROUNDS; // a refinement never triggers new questions
        return advance();
    }

    /** Searches again with the same constraints (used by "refresh" for outdated history). */
    public AgentResponse rerun() {
        clarificationRounds = MAX_CLARIFICATION_ROUNDS;
        return advance();
    }

    /** Loads a saved search so it can be refined or refreshed. */
    public void restore(SearchConstraints saved, List<SearchResult> savedResults) {
        reset();
        constraints = saved.copy();
        results = new ArrayList<>(savedResults);
        clarificationRounds = MAX_CLARIFICATION_ROUNDS;
    }

    public void reset() {
        constraints = new SearchConstraints();
        results = new ArrayList<>();
        clarificationRounds = 0;
        awaitingClarification = false;
    }

    /** Explanation of one result (F08). Falls back to a plain template if the LLM is unavailable. */
    public String explain(SearchResult result) {
        try {
            return llm.explain(result, constraints);
        } catch (LLMException e) {
            return MockLLMClient.templateExplanation(result);
        }
    }

    // ---- internal ----

    private AgentResponse advance() {
        List<String> missing = constraints.getMissingFields();
        boolean mustAsk = missing.contains("location"); // a search is impossible without a place
        boolean mayAsk = !missing.isEmpty() && clarificationRounds < MAX_CLARIFICATION_ROUNDS;
        if (mustAsk || mayAsk) {
            awaitingClarification = true;
            String question;
            try {
                question = llm.askClarification(missing, constraints);
            } catch (LLMException e) {
                question = "Please tell me your " + String.join(", ", missing) + ". (Type 'skip' to search without them.)";
            }
            return AgentResponse.clarification(question, constraints.copy());
        }
        awaitingClarification = false;
        return search();
    }

    private AgentResponse search() {
        try {
            results = service.findMatches(constraints);
            String notice = "";
            if (results.isEmpty()) {
                results = service.findNearMatches(constraints);
                notice = results.isEmpty()
                        ? "No restaurants matched, even loosely. Try a larger budget or a different place or time."
                        : "No exact matches. These are the closest alternatives; each shows which requirement it misses.";
            }
            return AgentResponse.results(results, constraints.copy(), notice);
        } catch (LocationNotFoundException e) {
            constraints.setLocation(null).setSearchLatitude(null).setSearchLongitude(null);
            awaitingClarification = true;
            return AgentResponse.clarification("I don't know the place \"" + e.getLocationText()
                    + "\". Try a Toronto station or neighbourhood, for example Osgoode Station or Kensington Market.",
                    constraints.copy());
        } catch (DataLoadException e) {
            return AgentResponse.error("The restaurant data could not be loaded: " + e.getMessage());
        }
    }

    private static boolean isSkip(String text) {
        String t = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return t.equals("skip") || t.equals("any") || t.equals("no preference") || t.equals("whatever")
                || t.equals("doesn't matter") || t.equals("doesnt matter");
    }
}
