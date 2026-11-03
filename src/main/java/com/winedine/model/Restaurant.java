package com.winedine.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** A Toronto venue with its deals. Loaded from the curated JSON dataset. */
public class Restaurant {
    private String id;
    private String name;
    private String address;
    private Double latitude;   // null = no valid location
    private Double longitude;
    private String cuisine;
    private List<String> tags = new ArrayList<>();
    private LocalTime opensAt;
    private LocalTime closesAt;
    private LocalDate lastUpdated;
    private List<HappyHourDeal> deals = new ArrayList<>();

    public Restaurant() { }

    public boolean hasValidLocation() {
        return latitude != null && longitude != null
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    /** Haversine distance in km. Returns Double.NaN if this restaurant has no valid location. */
    public double distanceTo(double lat, double lng) {
        if (!hasValidLocation()) return Double.NaN;
        double r = 6371.0;
        double dLat = Math.toRadians(lat - latitude);
        double dLng = Math.toRadians(lng - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    public boolean isOpenAt(LocalTime time) {
        if (time == null || opensAt == null || closesAt == null) return true;
        if (closesAt.isAfter(opensAt)) return !time.isBefore(opensAt) && time.isBefore(closesAt);
        return !time.isBefore(opensAt) || time.isBefore(closesAt); // crosses midnight
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getCuisine() { return cuisine; }
    public List<String> getTags() { return tags; }
    public LocalTime getOpensAt() { return opensAt; }
    public LocalTime getClosesAt() { return closesAt; }
    public LocalDate getLastUpdated() { return lastUpdated; }
    public List<HappyHourDeal> getDeals() { return deals; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setOpensAt(LocalTime opensAt) { this.opensAt = opensAt; }
    public void setClosesAt(LocalTime closesAt) { this.closesAt = closesAt; }
    public void setLastUpdated(LocalDate lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setDeals(List<HappyHourDeal> deals) { this.deals = deals; }
}
