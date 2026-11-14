package com.winedine.service;

import static com.winedine.service.TestData.TODAY;
import static com.winedine.service.TestData.fridayDeal;
import static com.winedine.service.TestData.fridayEvening;
import static com.winedine.service.TestData.restaurant;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class RankingStrategyTest {
    private static SearchResult result(double distanceKm, double cost, List<String> matched) {
        return new SearchResult(
                restaurant("r", "R", 43.65, -79.38, List.of(), List.of()),
                fridayDeal("deal", 6.0, cost, TODAY), matched, List.of(), distanceKm, false);
    }

    private final SearchConstraints twoPreferences = fridayEvening()
            .setVenuePreferences(List.of("patio", "sports bar"));

    @Test
    void balancedPrefersMorePreferenceMatches() {
        RankingStrategy s = new BalancedRanking();
        SearchResult both = result(1.0, 14.0, List.of("patio", "sports bar"));
        SearchResult one = result(1.0, 14.0, List.of("patio"));
        assertTrue(s.score(both, twoPreferences) > s.score(one, twoPreferences));
    }

    @Test
    void budgetFirstPrefersTheCheaperRestaurantEvenIfFarther() {
        RankingStrategy s = new BudgetFirstRanking();
        SearchResult cheapFar = result(1.8, 8.0, List.of());
        SearchResult dearNear = result(0.2, 20.0, List.of());
        assertTrue(s.score(cheapFar, twoPreferences) > s.score(dearNear, twoPreferences));
    }

    @Test
    void distanceFirstPrefersTheCloserRestaurantEvenIfDearer() {
        RankingStrategy s = new DistanceFirstRanking();
        SearchResult cheapFar = result(1.8, 8.0, List.of());
        SearchResult dearNear = result(0.2, 20.0, List.of());
        assertTrue(s.score(dearNear, twoPreferences) > s.score(cheapFar, twoPreferences));
    }

    @Test
    void unknownDistanceAndCostArePenalised() {
        RankingStrategy s = new BalancedRanking();
        SearchResult known = result(1.0, 14.0, List.of());
        SearchResult unknown = new SearchResult(restaurant("r", "R", null, null, List.of(), List.of()),
                null, List.of(), List.of(), null, false);
        assertTrue(s.score(known, twoPreferences) > s.score(unknown, twoPreferences));
    }

    @Test
    void placeWithoutADealAtTheRequestedTimeRanksLower() {
        RankingStrategy s = new BalancedRanking();
        SearchResult withDeal = result(1.0, 14.0, List.of());
        SearchResult noDeal = new SearchResult(restaurant("r2", "R2", 43.65, -79.38, List.of(), List.of()),
                null, List.of(), List.of(), 1.0, false);
        assertTrue(s.score(withDeal, twoPreferences) > s.score(noDeal, twoPreferences));
    }
}