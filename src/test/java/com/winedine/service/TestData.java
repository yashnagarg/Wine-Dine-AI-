package com.winedine.service;

import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;
import com.winedine.model.SearchConstraints;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Small builders shared by the service tests. */
final class TestData {
    static final double OSGOODE_LAT = 43.6508;
    static final double OSGOODE_LON = -79.3868;
    static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    private TestData() { }

    static HappyHourDeal fridayDeal(String description, Double drink, Double cost, LocalDate updated) {
        return new HappyHourDeal(DayOfWeek.FRIDAY, LocalTime.of(16, 0), LocalTime.of(19, 0),
                description, drink, cost, updated);
    }

    /** A restaurant roughly {@code lat} degrees north of Osgoode station. */
    static Restaurant restaurant(String id, String name, Double lat, Double lon, List<String> tags,
                                 List<HappyHourDeal> deals) {
        return new Restaurant(id, name, "1 Test St", lat, lon, "Pub", tags,
                LocalTime.of(11, 0), LocalTime.of(23, 0), TODAY.minusDays(3), deals);
    }

    /** Friday 17:00-19:00, $15 per person, near Osgoode Station with coordinates already resolved. */
    static SearchConstraints fridayEvening() {
        return new SearchConstraints()
                .setDay(DayOfWeek.FRIDAY)
                .setStartTime(LocalTime.of(17, 0))
                .setEndTime(LocalTime.of(19, 0))
                .setMaxBudgetPerPerson(15.0)
                .setLocation("Osgoode Station")
                .setSearchLatitude(OSGOODE_LAT)
                .setSearchLongitude(OSGOODE_LON);
    }
}
