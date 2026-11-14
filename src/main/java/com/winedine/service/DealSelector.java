package com.winedine.service;

import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;

import java.util.ArrayList;
import java.util.List;

/** Finds the deals of a restaurant that matter for a search. */
final class DealSelector {
    private DealSelector() { }

    /** Deals that run at the requested day/time. If no day or time was given, all deals. */
    static List<HappyHourDeal> relevantDeals(Restaurant restaurant, SearchConstraints c) {
        List<HappyHourDeal> relevant = new ArrayList<>();
        for (HappyHourDeal deal : restaurant.getDeals()) {
            if (deal.isAvailableAt(c.getDay(), c.getStartTime(), c.getEndTime())) {
                relevant.add(deal);
            }
        }
        return relevant;
    }

    /** True if the deal stays within the budget limits (an unknown price is not counted as exceeding). */
    static boolean withinBudget(HappyHourDeal deal, SearchConstraints c) {
        Double maxBudget = c.getMaxBudgetPerPerson();
        Double maxDrink = c.getMaxDrinkPrice();
        boolean costOk = maxBudget == null || !deal.isCostKnown() || deal.matchesBudget(maxBudget);
        boolean drinkOk = maxDrink == null || !deal.isDrinkPriceKnown() || deal.matchesDrinkPrice(maxDrink);
        return costOk && drinkOk;
    }

    /** The deal to show: the first relevant deal that is within budget, else the first relevant deal, else null. */
    static HappyHourDeal bestDeal(Restaurant restaurant, SearchConstraints c) {
        List<HappyHourDeal> relevant = relevantDeals(restaurant, c);
        for (HappyHourDeal deal : relevant) {
            if (withinBudget(deal, c)) {
                return deal;
            }
        }
        return relevant.isEmpty() ? null : relevant.get(0);
    }
}
