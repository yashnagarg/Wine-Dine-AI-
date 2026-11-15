package com.winedine.ui;

import com.winedine.model.Restaurant;
import com.winedine.model.SearchResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Map links without any API key: an OpenStreetMap URL for a restaurant's coordinates (F06).
 * A restaurant without coordinates has no map link and the UI says so.
 */
public final class MapLinks {
    private MapLinks() { }

    /** OpenStreetMap URL centred on the restaurant with a marker, or null if it has no valid location. */
    public static String url(Restaurant r) {
        if (!r.hasValidLocation()) {
            return null;
        }
        return String.format(Locale.ROOT, "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f",
                r.getLatitude(), r.getLongitude(), r.getLatitude(), r.getLongitude());
    }

    /** OpenStreetMap embed URL (for the GUI's map pane), or null if the restaurant has no valid location. */
    public static String embedUrl(Restaurant r) {
        if (!r.hasValidLocation()) {
            return null;
        }
        double d = 0.004; // about 400 m around the marker
        return String.format(Locale.ROOT,
                "https://www.openstreetmap.org/export/embed.html?bbox=%.6f,%.6f,%.6f,%.6f&layer=mapnik&marker=%.6f,%.6f",
                r.getLongitude() - d, r.getLatitude() - d, r.getLongitude() + d, r.getLatitude() + d,
                r.getLatitude(), r.getLongitude());
    }

    /**
     * Google Maps search link (no API key needed) for the venue by name and address, so it opens the real
     * place page. Falls back to the coordinates when the venue has no address.
     */
    public static String googleUrl(Restaurant r) {
        String query;
        if (!r.getAddress().isBlank()) {
            query = r.getName() + ", " + r.getAddress();
        } else if (r.hasValidLocation()) {
            query = String.format(Locale.ROOT, "%.6f,%.6f", r.getLatitude(), r.getLongitude());
        } else {
            query = r.getName() + ", Toronto";
        }
        return "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    public static String describe(SearchResult result) {
        String url = url(result.getRestaurant());
        return url == null ? result.getRestaurant().getName() + ": no map location available"
                : result.getRestaurant().getName() + ": " + url;
    }
}