package com.winedine.service;

import com.winedine.data.ProviderFactory;
import com.winedine.data.RestaurantDataProvider;
import com.winedine.model.Freshness;
import com.winedine.model.HappyHourDeal;
import com.winedine.model.RankingProfile;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;
import com.winedine.model.SearchResult;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Finds restaurants for a search: loads candidates from the data provider, applies the constraint
 * filters as a pipeline, builds a {@link SearchResult} for each survivor and ranks them with the
 * {@link RankingStrategy} chosen by the user's ranking profile (features F03 to F07).
 */
public class RecommendationService {
    /** Results whose scores differ by less than this are shown as comparable alternatives. */
    static final double COMPARABLE_SCORE_DELTA = 0.5;

    private final RestaurantDataProvider provider;
    private final List<ConstraintFilter> filters;
    private final Map<RankingProfile, RankingStrategy> strategies = new EnumMap<>(RankingProfile.class);
    private final Clock clock;

    public RecommendationService(ProviderFactory factory) {
        this(factory, defaultFilters(), Clock.systemDefaultZone());
    }

    public RecommendationService(ProviderFactory factory, List<ConstraintFilter> filters, Clock clock) {
        this.provider = factory.createProvider();
        this.filters = new ArrayList<>(filters);
        this.clock = clock;
        strategies.put(RankingProfile.BALANCED, new BalancedRanking());
        strategies.put(RankingProfile.BUDGET_FIRST, new BudgetFirstRanking());
        strategies.put(RankingProfile.DISTANCE_FIRST, new DistanceFirstRanking());
    }

    public static List<ConstraintFilter> defaultFilters() {
        List<ConstraintFilter> list = new ArrayList<>();
        list.add(new BudgetFilter());
        list.add(new TimeFilter());
        list.add(new LocationFilter());
        list.add(new PreferenceFilter());
        return list;
    }

    public RestaurantDataProvider getProvider() { return provider; }

    /**
     * Restaurants that satisfy every filter, ranked best first. Resolves the search location first.
     *
     * @throws com.winedine.data.LocationNotFoundException if the location text is unknown
     */
    public List<SearchResult> findMatches(SearchConstraints constraints) {
        provider.resolveLocation(constraints);
        List<Restaurant> remaining = provider.findRestaurants(constraints);
        for (ConstraintFilter filter : filters) {
            remaining = filter.filter(remaining, constraints);
        }
        List<SearchResult> results = new ArrayList<>();
        for (Restaurant r : remaining) {
            results.add(buildResult(r, constraints, false, null));
        }
        return rank(results, constraints);
    }

    /**
     * Closest alternatives: restaurants that fail exactly one rule, each with a warning that names
     * the rule it does not meet. Used when {@link #findMatches} returns nothing.
     */
    public List<SearchResult> findNearMatches(SearchConstraints constraints) {
        provider.resolveLocation(constraints);
        List<Restaurant> candidates = provider.findRestaurants(constraints);
        List<SearchResult> results = new ArrayList<>();
        for (Restaurant r : candidates) {
            ConstraintFilter failed = null;
            int failures = 0;
            for (ConstraintFilter filter : filters) {
                if (!filter.passes(r, constraints)) {
                    failures++;
                    failed = filter;
                }
            }
            if (failures == 1) {
                results.add(buildResult(r, constraints, true, failed.name()));
            }
        }
        return rank(results, constraints);
    }

    RankingStrategy strategyFor(RankingProfile profile) {
        return strategies.get(profile);
    }

    private List<SearchResult> rank(List<SearchResult> results, SearchConstraints constraints) {
        RankingStrategy strategy = strategyFor(constraints.getRankingProfile());
        for (SearchResult result : results) {
            result.setScore(strategy.score(result, constraints));
        }
        results.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());
        for (int i = 1; i < results.size(); i++) {
            if (Math.abs(results.get(i).getScore() - results.get(i - 1).getScore()) < COMPARABLE_SCORE_DELTA) {
                results.get(i).setComparableAlternative(true);
                results.get(i - 1).setComparableAlternative(true);
            }
        }
        return results;
    }

    private SearchResult buildResult(Restaurant r, SearchConstraints c, boolean nearMatch, String failedRule) {
        HappyHourDeal deal = DealSelector.bestDeal(r, c);
        List<String> warnings = new ArrayList<>();
        if (nearMatch) {
            warnings.add("Does not meet the " + failedRule + " requirement");
        }
        boolean budgetGiven = c.getMaxBudgetPerPerson() != null || c.getMaxDrinkPrice() != null;
        if (deal == null) {
            if (c.hasTimeWindow()) {
                warnings.add("No happy-hour deal found for the requested time");
            }
            if (budgetGiven) {
                warnings.add("Price unavailable, budget could not be verified");
            }
        } else {
            boolean costUnknown = c.getMaxBudgetPerPerson() != null && !deal.isCostKnown();
            boolean drinkUnknown = c.getMaxDrinkPrice() != null && !deal.isDrinkPriceKnown();
            if (costUnknown || drinkUnknown) {
                warnings.add("Price unavailable, budget could not be verified");
            }
            Freshness freshness = deal.getFreshness(LocalDate.now(clock));
            if (freshness == Freshness.STALE) {
                warnings.add("Deal information may be outdated (last updated " + deal.getLastUpdated() + ")");
            } else if (freshness == Freshness.UNKNOWN) {
                warnings.add("Deal update date is unavailable");
            }
        }
        Double distance = null;
        if (r.hasValidLocation()) {
            if (c.hasLocationCoordinates()) {
                distance = r.distanceTo(c.getSearchLatitude(), c.getSearchLongitude());
            }
        } else {
            warnings.add("No map location available");
        }
        return new SearchResult(r, deal, PreferenceFilter.matchedPreferences(r, c), warnings, distance, nearMatch);
    }
}
