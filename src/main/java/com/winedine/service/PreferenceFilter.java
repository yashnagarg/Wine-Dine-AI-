package com.winedine.service;

import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Keeps restaurants that match at least one food, drink or venue preference (F05). */
public class PreferenceFilter implements ConstraintFilter {

    @Override
    public String name() { return "preferences"; }

    @Override
    public boolean passes(Restaurant restaurant, SearchConstraints c) {
        if (c.allPreferences().isEmpty()) {
            return true;
        }
        return !matchedPreferences(restaurant, c).isEmpty();
    }

    /** The preferences from the constraints that this restaurant satisfies. */
    public static List<String> matchedPreferences(Restaurant restaurant, SearchConstraints c) {
        List<String> matched = new ArrayList<>();
        for (String preference : c.allPreferences()) {
            if (matches(restaurant, preference)) {
                matched.add(preference);
            }
        }
        return matched;
    }

    private static boolean matches(Restaurant restaurant, String preference) {
        String p = preference.trim().toLowerCase(Locale.ROOT);
        if (p.isEmpty()) {
            return false;
        }
        if (contains(restaurant.getCuisine(), p) || contains(restaurant.getName(), p)) {
            return true;
        }
        for (String tag : restaurant.getTags()) {
            if (contains(tag, p)) {
                return true;
            }
        }
        for (HappyHourDeal deal : restaurant.getDeals()) {
            if (contains(deal.getDescription(), p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(String text, String lowerPreference) {
        String t = text.toLowerCase(Locale.ROOT);
        return !t.isEmpty() && (t.contains(lowerPreference) || lowerPreference.contains(t));
    }
}
