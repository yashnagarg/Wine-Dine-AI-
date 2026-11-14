package com.winedine.service;

import static com.winedine.service.TestData.OSGOODE_LAT;
import static com.winedine.service.TestData.OSGOODE_LON;
import static com.winedine.service.TestData.TODAY;
import static com.winedine.service.TestData.fridayDeal;
import static com.winedine.service.TestData.fridayEvening;
import static com.winedine.service.TestData.restaurant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.winedine.data.LocationNotFoundException;
import com.winedine.data.ProviderFactory;
import com.winedine.data.RestaurantDataProvider;
import com.winedine.model.RankingProfile;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC);

    /** In-memory provider so the service is tested without any file or network. */
    private static final class FakeProvider implements RestaurantDataProvider {
        private final List<Restaurant> restaurants;

        FakeProvider(List<Restaurant> restaurants) { this.restaurants = restaurants; }

        @Override public List<Restaurant> findRestaurants(SearchConstraints c) { return restaurants; }

        @Override public Restaurant getRestaurant(String id) { return null; }

        @Override
        public void resolveLocation(SearchConstraints c) {
            if (c.getLocation() == null || c.hasLocationCoordinates()) {
                return;
            }
            if (!c.getLocation().toLowerCase().contains("osgoode")) {
                throw new LocationNotFoundException(c.getLocation());
            }
            c.setSearchLatitude(OSGOODE_LAT).setSearchLongitude(OSGOODE_LON);
        }
    }

    private static RecommendationService serviceWith(Restaurant... restaurants) {
        ProviderFactory factory = new ProviderFactory() {
            @Override public RestaurantDataProvider createProvider() { return new FakeProvider(List.of(restaurants)); }
        };
        return new RecommendationService(factory, RecommendationService.defaultFilters(), CLOCK);
    }

    private static Restaurant pub(String id, double latOffset, Double cost, List<String> tags) {
        return restaurant(id, "Pub " + id, OSGOODE_LAT + latOffset, OSGOODE_LON, tags,
                List.of(fridayDeal("deal " + id, 6.0, cost, TODAY.minusDays(3))));
    }

    @Test
    void returnsOnlyRestaurantsThatSatisfyAllFilters() {
        RecommendationService service = serviceWith(
                pub("ok", 0.002, 14.0, List.of()),
                pub("tooExpensive", 0.002, 40.0, List.of()),
                pub("tooFar", 0.2, 14.0, List.of()));
        List<SearchResult> results = service.findMatches(fridayEvening());
        assertEquals(1, results.size());
        assertEquals("ok", results.get(0).getRestaurant().getId());
        assertFalse(results.get(0).isNearMatch());
    }

    @Test
    void resolvesTheLocationBeforeFiltering() {
        SearchConstraints c = fridayEvening().setSearchLatitude(null).setSearchLongitude(null);
        List<SearchResult> results = serviceWith(pub("a", 0.002, 14.0, List.of())).findMatches(c);
        assertEquals(1, results.size());
        assertEquals(Double.valueOf(OSGOODE_LAT), c.getSearchLatitude());
    }

    @Test
    void unknownLocationPropagatesAsAnException() {
        SearchConstraints c = new SearchConstraints().setLocation("Atlantis");
        assertThrows(LocationNotFoundException.class, () -> serviceWith(pub("a", 0, 14.0, List.of())).findMatches(c));
    }

    @Test
    void distanceIsStoredInTheResult() {
        SearchResult r = serviceWith(pub("a", 0.009, 14.0, List.of())).findMatches(fridayEvening()).get(0);
        assertEquals(1.0, r.getDistanceKm().doubleValue(), 0.05);
    }

    @Test
    void unknownPriceKeepsTheRestaurantWithAWarning() {
        List<SearchResult> results = serviceWith(pub("noPrice", 0.002, null, List.of())).findMatches(fridayEvening());
        assertEquals(1, results.size());
        assertTrue(results.get(0).getWarnings().contains("Price unavailable, budget could not be verified"));
    }

    @Test
    void staleAndUndatedDealsGetWarnings() {
        Restaurant stale = restaurant("stale", "Stale", OSGOODE_LAT + 0.002, OSGOODE_LON, List.of(),
                List.of(fridayDeal("old", 6.0, 14.0, TODAY.minusDays(90))));
        Restaurant undated = restaurant("undated", "Undated", OSGOODE_LAT + 0.003, OSGOODE_LON, List.of(),
                List.of(fridayDeal("unknown date", 6.0, 14.0, null)));
        List<SearchResult> results = serviceWith(stale, undated).findMatches(fridayEvening());
        for (SearchResult r : results) {
            String id = r.getRestaurant().getId();
            if (id.equals("stale")) {
                assertTrue(r.getWarnings().stream().anyMatch(w -> w.startsWith("Deal information may be outdated")));
            } else {
                assertTrue(r.getWarnings().contains("Deal update date is unavailable"));
            }
        }
        assertEquals(2, results.size());
    }

    @Test
    void restaurantWithoutMapLocationIsKeptWithAWarning() {
        Restaurant nowhere = restaurant("nowhere", "Nowhere", null, null, List.of(),
                List.of(fridayDeal("deal", 6.0, 14.0, TODAY)));
        SearchResult r = serviceWith(nowhere).findMatches(fridayEvening()).get(0);
        assertNull(r.getDistanceKm());
        assertTrue(r.getWarnings().contains("No map location available"));
    }

    @Test
    void matchedPreferencesAreRecorded() {
        SearchConstraints c = fridayEvening().setVenuePreferences(List.of("patio"));
        SearchResult r = serviceWith(pub("a", 0.002, 14.0, List.of("patio"))).findMatches(c).get(0);
        assertEquals(List.of("patio"), r.getMatchedPreferences());
    }

    @Test
    void rankingProfileChangesTheOrder() {
        Restaurant cheapFar = pub("cheapFar", 0.016, 8.0, List.of());
        Restaurant dearNear = pub("dearNear", 0.001, 14.0, List.of());
        RecommendationService service = serviceWith(cheapFar, dearNear);

        List<SearchResult> byBudget = service.findMatches(fridayEvening().setRankingProfile(RankingProfile.BUDGET_FIRST));
        assertEquals("cheapFar", byBudget.get(0).getRestaurant().getId());

        List<SearchResult> byDistance = service.findMatches(fridayEvening().setRankingProfile(RankingProfile.DISTANCE_FIRST));
        assertEquals("dearNear", byDistance.get(0).getRestaurant().getId());
    }

    @Test
    void nearEqualScoresAreMarkedComparable() {
        RecommendationService service = serviceWith(pub("a", 0.0020, 14.0, List.of()), pub("b", 0.0021, 14.0, List.of()));
        List<SearchResult> results = service.findMatches(fridayEvening());
        assertTrue(results.get(0).isComparableAlternative());
        assertTrue(results.get(1).isComparableAlternative());
    }

    @Test
    void clearlyDifferentScoresAreNotComparable() {
        RecommendationService service = serviceWith(pub("a", 0.002, 8.0, List.of()), pub("b", 0.016, 14.0, List.of()));
        List<SearchResult> results = service.findMatches(fridayEvening());
        assertFalse(results.get(0).isComparableAlternative());
    }

    @Test
    void nearMatchesAreRestaurantsFailingExactlyOneRule() {
        RecommendationService service = serviceWith(
                pub("slightlyOverBudget", 0.002, 18.0, List.of()),
                pub("failsBudgetAndDistance", 0.2, 40.0, List.of()),
                pub("perfect", 0.002, 12.0, List.of()));
        SearchConstraints c = fridayEvening();
        List<SearchResult> near = service.findNearMatches(c);
        assertEquals(1, near.size());
        SearchResult r = near.get(0);
        assertEquals("slightlyOverBudget", r.getRestaurant().getId());
        assertTrue(r.isNearMatch());
        assertTrue(r.getWarnings().contains("Does not meet the budget requirement"));
    }

    @Test
    void nothingMatchesWhenBudgetIsTooLowButNearMatchesExplainWhy() {
        RecommendationService service = serviceWith(pub("a", 0.002, 18.0, List.of()), pub("b", 0.003, 25.0, List.of()));
        SearchConstraints c = fridayEvening().setMaxBudgetPerPerson(5.0);
        assertTrue(service.findMatches(c).isEmpty());
        List<SearchResult> near = service.findNearMatches(c);
        assertEquals(2, near.size());
        assertTrue(near.stream().allMatch(r -> r.getWarnings().contains("Does not meet the budget requirement")));
    }

    @Test
    void strategyIsChosenFromTheRankingProfile() {
        RecommendationService service = serviceWith();
        assertTrue(service.strategyFor(RankingProfile.BALANCED) instanceof BalancedRanking);
        assertTrue(service.strategyFor(RankingProfile.BUDGET_FIRST) instanceof BudgetFirstRanking);
        assertTrue(service.strategyFor(RankingProfile.DISTANCE_FIRST) instanceof DistanceFirstRanking);
    }
}
