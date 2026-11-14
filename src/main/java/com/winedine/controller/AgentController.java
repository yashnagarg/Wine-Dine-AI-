package com.winedine.controller;

import com.winedine.agent.RestaurantAgent;
import com.winedine.config.AppConfig;
import com.winedine.data.CuratedProviderFactory;
import com.winedine.data.ProviderFactory;
import com.winedine.llm.LLMClient;
import com.winedine.llm.MockLLMClient;
import com.winedine.llm.OpenAILLMClient;
import com.winedine.model.AgentResponse;
import com.winedine.model.ResponseType;
import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchHistory;
import com.winedine.model.SearchResult;
import com.winedine.service.HistoryService;
import com.winedine.service.RecommendationService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Facade and MVC controller: the single entry point used by both the CLI and the GUI.
 * It hides the agent, recommendation service and history service behind a few methods.
 */
public class AgentController {
    private final RestaurantAgent agent;
    private final HistoryService history;
    private String originalQuery = "";

    public AgentController(RestaurantAgent agent, HistoryService history) {
        this.agent = agent;
        this.history = history;
    }

    /** Builds the whole application from the configuration (the only place where the parts are wired). */
    public static AgentController create(AppConfig config) {
        ProviderFactory factory = new CuratedProviderFactory(Path.of(config.getDatasetPath()));
        RecommendationService service = new RecommendationService(factory);
        LLMClient llm = (config.isUseMockLlm() || !config.hasApiKey())
                ? new MockLLMClient()
                : new OpenAILLMClient(config.getLlmApiKey(), config.getLlmModel());
        RestaurantAgent agent = new RestaurantAgent(llm, service);
        HistoryService history = new HistoryService(Path.of(config.getHistoryPath()), service.getProvider(),
                Clock.systemDefaultZone());
        return new AgentController(agent, history);
    }

    /**
     * Handles any message from the user: a new request, an answer to a clarification question,
     * or a refinement of the current results.
     */
    public AgentResponse send(String text) {
        AgentResponse response;
        if (agent.isAwaitingClarification()) {
            response = agent.answerClarification(text);
        } else if (!agent.getResults().isEmpty()) {
            response = agent.refine(text);
        } else {
            originalQuery = text;
            response = agent.handleRequest(text);
        }
        saveIfResults(response);
        return response;
    }

    /** Forgets the current search so the next message starts a new one. */
    public void newSearch() {
        agent.reset();
        originalQuery = "";
    }

    public List<SearchResult> getResults() { return agent.getResults(); }

    public SearchConstraints getConstraints() { return agent.getConstraints(); }

    public boolean isAwaitingClarification() { return agent.isAwaitingClarification(); }

    public String describeLlm() { return agent.describeLlm(); }

    /** Result number {@code index} (1-based) or null. */
    public SearchResult getResult(int index) {
        List<SearchResult> results = agent.getResults();
        return index >= 1 && index <= results.size() ? results.get(index - 1) : null;
    }

    public String explain(int index) {
        SearchResult r = getResult(index);
        return r == null ? "There is no result number " + index + "." : agent.explain(r);
    }

    public List<SearchHistory> getHistory() { return history.getAll(); }

    public boolean isOutdated(SearchHistory entry) { return history.isOutdated(entry); }

    /** Opens saved search number {@code index} (1-based, newest first) so it can be refined or refreshed. */
    public SearchHistory reopen(int index) {
        List<SearchHistory> all = history.getAll();
        if (index < 1 || index > all.size()) {
            return null;
        }
        SearchHistory entry = all.get(index - 1);
        agent.restore(entry.getConstraints(), entry.getResults());
        originalQuery = entry.getOriginalQuery();
        return entry;
    }

    /** Runs the current search again against the current data (F11). */
    public AgentResponse refresh() {
        AgentResponse response = agent.rerun();
        saveIfResults(response);
        return response;
    }

    private void saveIfResults(AgentResponse response) {
        if (response.getType() == ResponseType.RESULTS && !response.getResults().isEmpty()) {
            history.save(new SearchHistory(originalQuery, response.getConstraints(), response.getResults(),
                    LocalDateTime.now(history.getClock())));
        }
    }
}
