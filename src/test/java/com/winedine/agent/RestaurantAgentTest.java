package com.winedine.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.winedine.data.CuratedProviderFactory;
import com.winedine.llm.LLMException;
import com.winedine.llm.MockLLMClient;
import com.winedine.model.AgentResponse;
import com.winedine.model.ResponseType;
import com.winedine.model.SearchConstraints;
import com.winedine.service.RecommendationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RestaurantAgentTest {
    private static RestaurantAgent agent() {
        RecommendationService service = new RecommendationService(new CuratedProviderFactory(Path.of("src/test/resources/test-restaurants.json")));
        return new RestaurantAgent(new MockLLMClient(), service);
    }

    @Test
    void completeRequestGoesStraightToResults() {
        AgentResponse r = agent().handleRequest("beer near osgoode station on friday 5-7pm under $20");
        assertEquals(ResponseType.RESULTS, r.getType());
        assertFalse(r.getResults().isEmpty());
    }

    @Test
    void missingDetailsTriggerAClarificationQuestion() {
        RestaurantAgent agent = agent();
        AgentResponse r = agent.handleRequest("beer near osgoode station");
        assertEquals(ResponseType.CLARIFICATION, r.getType());
        assertTrue(agent.isAwaitingClarification());
        assertTrue(r.getMessage().contains("budget"));
    }

    @Test
    void skippingSearchesWithWhatIsKnown() {
        RestaurantAgent agent = agent();
        agent.handleRequest("beer near osgoode station");
        AgentResponse r = agent.answerClarification("skip");
        assertEquals(ResponseType.RESULTS, r.getType());
        assertFalse(agent.isAwaitingClarification());
    }

    @Test
    void locationIsAlwaysAskedForEvenAfterSkipping() {
        RestaurantAgent agent = agent();
        assertEquals(ResponseType.CLARIFICATION, agent.handleRequest("happy hour").getType());
        assertEquals(ResponseType.CLARIFICATION, agent.answerClarification("skip").getType());
        assertEquals(ResponseType.RESULTS, agent.answerClarification("Osgoode Station").getType());
    }

    @Test
    void clarificationIsAskedAtMostTwiceForBudgetAndTime() {
        RestaurantAgent agent = agent();
        agent.handleRequest("beer near osgoode station");
        assertEquals(ResponseType.CLARIFICATION, agent.answerClarification("maybe later").getType());
        assertEquals(ResponseType.RESULTS, agent.answerClarification("not sure").getType());
    }

    @Test
    void unknownPlaceIsAskedAgainWithAHelpfulMessage() {
        RestaurantAgent agent = agent();
        AgentResponse r = agent.handleRequest("near atlantis under $15 friday 5-7pm");
        assertEquals(ResponseType.CLARIFICATION, r.getType());
        assertTrue(r.getMessage().toLowerCase().contains("atlantis"));
        assertEquals(ResponseType.RESULTS, agent.answerClarification("Osgoode Station").getType());
    }

    @Test
    void refinementSearchesAgainWithTheChange() {
        RestaurantAgent agent = agent();
        agent.handleRequest("beer near osgoode station on friday 5-7pm under $20");
        AgentResponse r = agent.refine("make it $5");
        assertEquals(Double.valueOf(5.0), agent.getConstraints().getMaxBudgetPerPerson());
        assertEquals(ResponseType.RESULTS, r.getType());
        assertFalse(r.getMessage().isEmpty() && r.getResults().isEmpty());
    }

    @Test
    void failingLlmGivesAnErrorResponseNotACrash() {
        MockLLMClient broken = new MockLLMClient() {
            @Override public SearchConstraints extractConstraints(String t) { throw new LLMException("no network"); }
        };
        RecommendationService service = new RecommendationService(new CuratedProviderFactory(Path.of("src/test/resources/test-restaurants.json")));
        AgentResponse r = new RestaurantAgent(broken, service).handleRequest("anything");
        assertEquals(ResponseType.ERROR, r.getType());
        assertEquals("no network", r.getMessage());
    }

    @Test
    void explanationFallsBackToATemplateWhenTheLlmFails() {
        RestaurantAgent good = agent();
        good.handleRequest("beer near osgoode station on friday 5-7pm under $20");
        var first = good.getResults().get(0);
        MockLLMClient broken = new MockLLMClient() {
            @Override public String explain(com.winedine.model.SearchResult r, SearchConstraints c) { throw new LLMException("down"); }
        };
        RecommendationService service = new RecommendationService(new CuratedProviderFactory(Path.of("src/test/resources/test-restaurants.json")));
        String text = new RestaurantAgent(broken, service).explain(first);
        assertTrue(text.contains(first.getRestaurant().getName()));
    }
}