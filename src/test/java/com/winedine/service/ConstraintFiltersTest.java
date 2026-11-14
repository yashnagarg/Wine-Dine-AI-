package com.winedine.service;

import static com.winedine.service.TestData.OSGOODE_LAT;
import static com.winedine.service.TestData.OSGOODE_LON;
import static com.winedine.service.TestData.TODAY;
import static com.winedine.service.TestData.fridayDeal;
import static com.winedine.service.TestData.fridayEvening;
import static com.winedine.service.TestData.restaurant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Each filter is tested on its own, which is the point of the Strategy-style filter interface. */
class ConstraintFiltersTest {
    private final BudgetFilter budget = new BudgetFilter();
    private final TimeFilter time = new TimeFilter();
    private final LocationFilter location = new LocationFilter();
    private final PreferenceFilter preference = new PreferenceFilter();

    private static Restaurant withDeals(HappyHourDeal... deals) {
        return restaurant("r1", "Pub", OSGOODE_LAT, OSGOODE_LON, List.of("patio"), List.of(deals));
    }

    // ---- BudgetFilter ----

    @Test
    void budgetKeepsAffordableAndRemovesExpensiveDeals() {
        SearchConstraints c = fridayEvening();
        assertTrue(budget.passes(withDeals(fridayDeal("cheap", 6.0, 14.0, TODAY)), c));
        assertFalse(budget.passes(withDeals(fridayDeal("pricey", 9.0, 20.0, TODAY)), c));
    }

    @Test
    void budgetKeepsRestaurantsWithUnknownPricesInsteadOfAssumingOne() {
        assertTrue(budget.passes(withDeals(fridayDeal("no prices", null, null, TODAY)), fridayEvening()));
    }

    @Test
    void budgetKeepsRestaurantsWithNoDealAtTheRequestedTime() {
        HappyHourDeal monday = new HappyHourDeal(DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(18, 0),
                "x", 6.0, 50.0, TODAY);
        assertTrue(budget.passes(withDeals(monday), fridayEvening()));
    }

    @Test
    void budgetChecksTheDrinkPriceLimitToo() {
        SearchConstraints c = fridayEvening().setMaxDrinkPrice(7.0);
        assertTrue(budget.passes(withDeals(fridayDeal("ok", 6.0, 10.0, TODAY)), c));
        assertFalse(budget.passes(withDeals(fridayDeal("drinks too dear", 9.0, 10.0, TODAY)), c));
    }

    @Test
    void budgetDoesNothingWhenNoLimitIsSet() {
        SearchConstraints c = fridayEvening().setMaxBudgetPerPerson(null);
        assertTrue(budget.passes(withDeals(fridayDeal("pricey", 30.0, 90.0, TODAY)), c));
    }

    // ---- TimeFilter ----

    @Test
    void timeRequiresAnOverlappingDealForHappyHourSearches() {
        SearchConstraints c = fridayEvening().setHappyHourRequired(true);
        assertTrue(time.passes(withDeals(fridayDeal("deal", 6.0, 14.0, TODAY)), c));
        assertFalse(time.passes(withDeals(), c));
        HappyHourDeal monday = new HappyHourDeal(DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(19, 0),
                "x", 6.0, 14.0, TODAY);
        assertFalse(time.passes(withDeals(monday), c));
    }

    @Test
    void timeDoesNotRequireADealForNormalSearches() {
        assertTrue(time.passes(withDeals(), fridayEvening()));
    }

    @Test
    void timeRemovesRestaurantsThatAreClosed() {
        Restaurant closedEvenings = new Restaurant("r2", "Lunch Spot", "x", 43.65, -79.38, "Cafe", List.of(),
                LocalTime.of(7, 0), LocalTime.of(15, 0), TODAY, List.of());
        assertFalse(time.passes(closedEvenings, fridayEvening()));
    }

    // ---- LocationFilter ----

    @Test
    void locationUsesTheDefaultTwoKilometreRadius() {
        SearchConstraints c = fridayEvening();
        Restaurant near = restaurant("n", "Near", 43.6529, -79.3836, List.of(), List.of());
        Restaurant far = restaurant("f", "Far", 43.7807, -79.4152, List.of(), List.of());
        assertTrue(location.passes(near, c));
        assertFalse(location.passes(far, c));
    }

    @Test
    void locationRespectsAnExplicitDistance() {
        Restaurant far = restaurant("f", "Far", 43.7807, -79.4152, List.of(), List.of());
        assertTrue(location.passes(far, fridayEvening().setMaxDistanceKm(20.0)));
    }

    @Test
    void locationCannotFilterWithoutCoordinatesOrWithoutARestaurantLocation() {
        Restaurant far = restaurant("f", "Far", 43.7807, -79.4152, List.of(), List.of());
        assertTrue(location.passes(far, new SearchConstraints()));
        Restaurant nowhere = restaurant("x", "Nowhere", null, null, List.of(), List.of());
        assertTrue(location.passes(nowhere, fridayEvening()));
    }

    // ---- PreferenceFilter ----

    @Test
    void preferencesAreIgnoredWhenNoneAreGiven() {
        assertTrue(preference.passes(withDeals(), fridayEvening()));
    }

    @Test
    void preferenceMatchesTagsAndDealDescriptionsIgnoringCase() {
        Restaurant r = withDeals(fridayDeal("Half-price Truffle Fries", 6.0, 14.0, TODAY));
        SearchConstraints c = fridayEvening()
                .setFoodPreferences(List.of("truffle fries"))
                .setVenuePreferences(List.of("PATIO", "live music"));
        assertTrue(preference.passes(r, c));
        assertEquals(List.of("truffle fries", "PATIO"), PreferenceFilter.matchedPreferences(r, c));
    }

    @Test
    void preferenceRemovesRestaurantsMatchingNothing() {
        SearchConstraints c = fridayEvening().setVenuePreferences(List.of("karaoke"));
        assertFalse(preference.passes(withDeals(), c));
    }

    // ---- shared behaviour of the interface ----

    @Test
    void filterMethodKeepsOnlyPassingRestaurants() {
        Restaurant cheap = withDeals(fridayDeal("cheap", 6.0, 14.0, TODAY));
        Restaurant pricey = restaurant("r9", "Pricey", OSGOODE_LAT, OSGOODE_LON, List.of(),
                List.of(fridayDeal("pricey", 9.0, 40.0, TODAY)));
        List<Restaurant> kept = budget.filter(List.of(cheap, pricey), fridayEvening());
        assertEquals(1, kept.size());
        assertEquals("r1", kept.get(0).getId());
    }
}
