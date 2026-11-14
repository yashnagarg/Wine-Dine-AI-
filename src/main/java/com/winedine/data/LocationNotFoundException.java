package com.winedine.data;

/** Thrown when a place name cannot be resolved to coordinates. */
public class LocationNotFoundException extends RuntimeException {
    private final String locationText;

    public LocationNotFoundException(String locationText) {
        super("Location not found: " + locationText);
        this.locationText = locationText;
    }

    public String getLocationText() { return locationText; }
}
