package com.winedine.service;

import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;

/** Keeps restaurants that are open in the requested window and, for happy-hour searches, have a deal then (F03). */
public class TimeFilter implements ConstraintFilter {

    @Override
    public String name() { return "time"; }

    @Override
    public boolean passes(Restaurant restaurant, SearchConstraints c) {
        if (c.isHappyHourRequired() && DealSelector.relevantDeals(restaurant, c).isEmpty()) {
            return false;
        }
        return restaurant.isOpenDuring(c.getStartTime(), c.getEndTime());
    }
}
