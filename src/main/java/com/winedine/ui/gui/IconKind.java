package com.winedine.ui.gui;

import com.winedine.model.HappyHourDeal;
import com.winedine.model.Restaurant;

import java.util.Locale;

/**
 * The line-art icons used across the app (24 x 24 SVG paths) and the rule that picks one for a venue.
 * Plain Java, so the picking rule is easy to test; the GUI classes turn the paths into shapes.
 */
public enum IconKind {
    WINE("M7 3H17L16.3 9A4.3 4.3 0 0 1 7.7 9Z M12 13.3V20 M8 21H16", false, 0xB3262E),
    MARTINI("M4 4H20L12 13Z M12 13V20 M8 21H16 M11 8A1.5 1.5 0 1 0 14 8A1.5 1.5 0 1 0 11 8Z", false, 0xC68A1D),
    BEER("M5 6H15V20H5Z M15 9H18.5A1.5 1.5 0 0 1 20 10.5V14.5A1.5 1.5 0 0 1 18.5 16H15 "
            + "M5 6A2.5 2.5 0 0 1 8 3A2.5 2.5 0 0 1 12 3A2.5 2.5 0 0 1 15 6 M8.5 10V16 M11.5 10V16", false, 0xC68A1D),
    PIZZA("M3 5Q12 1 21 5L12 21Z M9 9.5H9.1 M14.5 9H14.6 M12 14H12.1", false, 0xB3262E),
    WINGS("M15 3A6 6 0 1 1 10.5 13.5 M10.5 13.5L5.5 18.5 M4 17L7 20", false, 0xC68A1D),
    MAPLE("M12 2L14 7L18 5L17 11L21 10L18 14L19 16L13 15.5L13 21L11 21L11 15.5L5 16L6 14L3 10L7 11L6 5L10 7Z", true, 0xB3262E),
    TOWER("M12 2V8 M10.5 8H13.5 M8.5 9.5H15.5V12H8.5Z M11 12L10 21 M13 12L14 21 M7 21H17 M10.4 16.5H13.6", false, 0x3B2A1A);

    private final String path;
    private final boolean filled;
    private final int color;

    IconKind(String path, boolean filled, int color) {
        this.path = path;
        this.filled = filled;
        this.color = color;
    }

    /** The badge colour (0xRRGGBB) behind the icon. */
    public int color() {
        return color;
    }

    public String path() {
        return path;
    }

    public boolean isFilled() {
        return filled;
    }

    /** Picks an icon from the venue's name, cuisine, tags and deal text; the maple leaf is the fallback. */
    public static IconKind choose(Restaurant r, HappyHourDeal deal) {
        StringBuilder sb = new StringBuilder(r.getName()).append(' ').append(r.getCuisine());
        for (String t : r.getTags()) {
            sb.append(' ').append(t);
        }
        if (deal != null) {
            sb.append(' ').append(deal.getDescription());
        }
        return fromText(sb.toString());
    }

    static IconKind fromText(String text) {
        String s = text.toLowerCase(Locale.ROOT);
        if (has(s, "pizza", "pizzeria")) {
            return PIZZA;
        }
        if (has(s, "wing")) {
            return WINGS;
        }
        if (has(s, "wine")) {
            return WINE;
        }
        if (has(s, "beer", "pint", "lager", "brew", "pub", "draft", "irish", "tap")) {
            return BEER;
        }
        if (has(s, "cocktail", "martini", "spritz", "negroni", "margarita", "caesar", "mimosa", "old fashioned")) {
            return MARTINI;
        }
        return MAPLE;
    }

    private static boolean has(String s, String... words) {
        for (String w : words) {
            if (s.contains(w)) {
                return true;
            }
        }
        return false;
    }
}