package com.winedine.service;

import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;

import java.util.List;

/** Removes restaurants whose deals exceed the per-person budget or the drink price limit (F04). */
public class BudgetFilter implements ConstraintFilter {

    @Override
    public String name() { return "budget"; }

    @Override
    public boolean passes(Restaurant restaurant, SearchConstraints c) {
        if (c.getMaxBudgetPerPerson() == null && c.getMaxDrinkPrice() == null) {
            return true;
        }
        List<HappyHourDeal> deals = DealSelector.relevantDeals(restaurant, c);
        if (deals.isEmpty()) {
            return true; // price unknown: keep the restaurant, a warning is added to its result
        }
        for (HappyHourDeal deal : deals) {
            if (DealSelector.withinBudget(deal, c)) {
                return true;
            }
        }
        return false;
    }
}
