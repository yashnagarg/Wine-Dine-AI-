package com.winedine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RestaurantTest {
    private static Restaurant at(Double lat, Double lon, String opens, String closes) {
        return new Restaurant("r1", "Test Pub", "1 Test St", lat, lon, "Pub", List.of("patio"),
                opens == null ? null : LocalTime.parse(opens), closes == null ? null : LocalTime.parse(closes),
                null, List.of());
    }

    @Test
    void validLocationNeedsCoordinatesInRange() {
        assertTrue(at(43.65, -79.38, null, null).hasValidLocation());
        assertFalse(at(null, null, null, null).hasValidLocation());
        assertFalse(at(91.0, -79.38, null, null).hasValidLocation());
        assertFalse(at(43.65, -181.0, null, null).hasValidLocation());
    }

    @Test
    void distanceBetweenOsgoodeAndUnionIsAboutEightHundredMetres() {
        Restaurant union = at(43.6453, -79.3806, null, null);
        double km = union.distanceTo(43.6508, -79.3868);
        assertTrue(km > 0.7 && km < 0.9, "distance was " + km);
    }

    @Test
    void distanceIsNaNWithoutLocation() {
        assertTrue(Double.isNaN(at(null, null, null, null).distanceTo(43.65, -79.38)));
    }

    @Test
    void distanceToSamePointIsZero() {
        assertEquals(0.0, at(43.65, -79.38, null, null).distanceTo(43.65, -79.38), 0.0001);
    }

    @Test
    void openDuringNormalHours() {
        Restaurant r = at(43.65, -79.38, "11:00", "22:00");
        assertTrue(r.isOpenDuring(LocalTime.of(17, 0), LocalTime.of(19, 0)));
        assertFalse(r.isOpenDuring(LocalTime.of(22, 30), LocalTime.of(23, 30)));
    }

    @Test
    void openDuringHoursThatPassMidnight() {
        Restaurant r = at(43.65, -79.38, "11:00", "02:00");
        assertTrue(r.isOpenDuring(LocalTime.of(23, 0), LocalTime.of(23, 59)));
        assertFalse(r.isOpenDuring(LocalTime.of(3, 0), LocalTime.of(4, 0)));
    }

    @Test
    void unknownHoursCountAsOpen() {
        assertTrue(at(43.65, -79.38, null, null).isOpenDuring(LocalTime.of(3, 0), LocalTime.of(4, 0)));
    }
}
