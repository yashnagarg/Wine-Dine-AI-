package com.winedine.service;

import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;

import java.util.ArrayList;
import java.util.List;

/**
 * One hard-constraint rule. Filters are combined as a pipeline by {@link RecommendationService};
 * each filter ignores itself when its constraint is not set.
 */
public interface ConstraintFilter {

    /** Short name of the rule, e.g. "budget". Used to explain near matches. */
    String name();

    boolean passes(Restaurant restaurant, SearchConstraints constraints);

    default List<Restaurant> filter(List<Restaurant> restaurants, SearchConstraints constraints) {
        List<Restaurant> kept = new ArrayList<>();
        for (Restaurant r : restaurants) {
            if (passes(r, constraints)) {
                kept.add(r);
            }
        }
        return kept;
    }
}
