package com.winedine.ui;

import com.winedine.model.Restaurant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapLinksTest {
    private static Restaurant r(String name, String address, Double lat, Double lon) {
        return new Restaurant("x", name, address, lat, lon, "Pub", List.of(), null, null, null, List.of());
    }

    @Test
    void googleLinkSearchesByNameAndAddress() {
        String url = MapLinks.googleUrl(r("Bar Volo", "587 Yonge St, Toronto", 43.66, -79.38));
        assertTrue(url.startsWith("https://www.google.com/maps/search/?api=1&query="));
        assertTrue(url.contains("Bar+Volo"));
        assertTrue(url.contains("587+Yonge+St"));
    }

    @Test
    void googleLinkFallsBackToCoordinatesThenName() {
        assertEquals("https://www.google.com/maps/search/?api=1&query=43.650000%2C-79.380000",
                MapLinks.googleUrl(r("A", "", 43.65, -79.38)));
        assertTrue(MapLinks.googleUrl(r("Place & Co", "", null, null)).endsWith("Place+%26+Co%2C+Toronto"));
    }
}