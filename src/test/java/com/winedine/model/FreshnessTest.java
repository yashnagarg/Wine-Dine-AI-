package com.winedine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FreshnessTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);

    @Test
    void missingDateIsUnknown() {
        assertEquals(Freshness.UNKNOWN, Freshness.of(null, TODAY));
    }

    @Test
    void updatedWithinThirtyDaysIsFresh() {
        assertEquals(Freshness.FRESH, Freshness.of(TODAY, TODAY));
        assertEquals(Freshness.FRESH, Freshness.of(TODAY.minusDays(30), TODAY));
    }

    @Test
    void olderThanThirtyDaysIsStale() {
        assertEquals(Freshness.STALE, Freshness.of(TODAY.minusDays(31), TODAY));
    }
}
