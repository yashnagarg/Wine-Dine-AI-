package com.winedine.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A restaurant or bar with its location, opening hours and happy-hour deals. */
public class Restaurant {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final String id;
    private final String name;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final String cuisine;
    private final List<String> tags;
    private final LocalTime opensAt;
    private final LocalTime closesAt;
    private final LocalDate lastUpdated;
    private final List<HappyHourDeal> deals;

    public Restaurant(String id, String name, String address, Double latitude, Double longitude,
                      String cuisine, List<String> tags, LocalTime opensAt, LocalTime closesAt,
                      LocalDate lastUpdated, List<HappyHourDeal> deals) {
        this.id = id;
        this.name = name;
        this.address = address == null ? "" : address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cuisine = cuisine == null ? "" : cuisine;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.lastUpdated = lastUpdated;
        this.deals = deals == null ? new ArrayList<>() : new ArrayList<>(deals);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getCuisine() { return cuisine; }
    public List<String> getTags() { return Collections.unmodifiableList(tags); }
    public LocalTime getOpensAt() { return opensAt; }
    public LocalTime getClosesAt() { return closesAt; }
    public LocalDate getLastUpdated() { return lastUpdated; }
    public List<HappyHourDeal> getDeals() { return Collections.unmodifiableList(deals); }

    /** True if coordinates are present and within valid latitude/longitude ranges. */
    public boolean hasValidLocation() {
        return latitude != null && longitude != null
                && !latitude.isNaN() && !longitude.isNaN()
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    /** Great-circle distance in km to a point, or NaN if this restaurant has no valid location. */
    public double distanceTo(double lat, double lon) {
        if (!hasValidLocation()) {
            return Double.NaN;
        }
        double dLat = Math.toRadians(lat - latitude);
        double dLon = Math.toRadians(lon - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * True if the restaurant is open at some point in the time window.
     * Unknown opening hours or an unspecified window count as open.
     */
    public boolean isOpenDuring(LocalTime from, LocalTime to) {
        if (opensAt == null || closesAt == null || from == null || to == null) {
            return true;
        }
        if (closesAt.isAfter(opensAt)) {
            return opensAt.isBefore(to) && from.isBefore(closesAt);
        }
        // Closes after midnight (e.g. 11:00 to 02:00).
        return to.isAfter(opensAt) || from.isBefore(closesAt);
    }

    public Freshness getFreshness(LocalDate today) {
        return Freshness.of(lastUpdated, today);
    }
}
