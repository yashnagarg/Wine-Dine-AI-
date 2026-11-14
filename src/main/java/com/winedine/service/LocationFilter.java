package com.winedine.service;

import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;

/** Keeps restaurants within the maximum distance of the searched location (F06). */
public class LocationFilter implements ConstraintFilter {

    @Override
    public String name() { return "location"; }

    @Override
    public boolean passes(Restaurant restaurant, SearchConstraints c) {
        if (!c.hasLocationCoordinates() || !restaurant.hasValidLocation()) {
            return true; // nothing to compare; a missing map location is flagged in the result
        }
        double km = restaurant.distanceTo(c.getSearchLatitude(), c.getSearchLongitude());
        return km <= c.getMaxDistanceKmOrDefault();
    }
}
