package com.winedine.data;

import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;

import java.util.List;

/** Source of restaurant data and location lookup (the Product of the Factory Method pattern). */
public interface RestaurantDataProvider {

    /** Returns the candidate restaurants for a search. Filtering is done by the recommendation service. */
    List<Restaurant> findRestaurants(SearchConstraints constraints);

    /** Returns the restaurant with this id, or null if unknown. */
    Restaurant getRestaurant(String id);

    /**
     * Turns the location text in the constraints (e.g. "Osgoode Station") into coordinates and
     * stores them in the constraints. Does nothing if there is no location text or coordinates already exist.
     *
     * @throws LocationNotFoundException if the place is not known
     */
    void resolveLocation(SearchConstraints constraints);
}
