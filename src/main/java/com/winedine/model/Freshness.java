package com.winedine.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** How current a piece of restaurant or deal information is (feature F11). */
public enum Freshness {
    FRESH, STALE, UNKNOWN;

    /** Information updated within this many days counts as FRESH. */
    public static final int FRESH_DAYS = 30;

    /**
     * Computes freshness from a last-updated date.
     * A missing date gives UNKNOWN; a date older than {@link #FRESH_DAYS} gives STALE.
     */
    public static Freshness of(LocalDate lastUpdated, LocalDate today) {
        if (lastUpdated == null) {
            return UNKNOWN;
        }
        long ageInDays = ChronoUnit.DAYS.between(lastUpdated, today);
        return ageInDays <= FRESH_DAYS ? FRESH : STALE;
    }
}
