package com.winedine.ui;

import com.winedine.ui.gui.IconKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The icon paths are text and the picking rule is plain logic, so no window is needed. */
class IconKindTest {

    @Test
    void everyIconHasAPathThatStartsWithAMoveCommand() {
        for (IconKind k : IconKind.values()) {
            assertFalse(k.path().isBlank(), k.name());
            assertTrue(k.path().startsWith("M"), k.name());
        }
    }

    @Test
    void everyIconHasABadgeColour() {
        for (IconKind k : IconKind.values()) {
            assertTrue(k.color() > 0 && k.color() <= 0xFFFFFF, k.name());
        }
    }

    @Test
    void onlyTheMapleLeafIsFilled() {
        for (IconKind k : IconKind.values()) {
            assertEquals(k == IconKind.MAPLE, k.isFilled(), k.name());
        }
    }

    @Test
    void venueTextPicksTheMatchingIcon() {
        assertEquals(IconKind.PIZZA, IconKind.choose(restaurant("Pizzeria Libretto", "Italian"), null));
        assertEquals(IconKind.WINE, IconKind.choose(restaurant("Cibo Wine Bar", "Wine bar"), null));
        assertEquals(IconKind.BEER, IconKind.choose(restaurant("Bellwoods Brewery", "Brewery"), null));
        assertEquals(IconKind.MARTINI, IconKind.choose(restaurant("Voodoo Child", "Cocktail bar"), null));
        assertEquals(IconKind.MAPLE, IconKind.choose(restaurant("Somewhere", "Diner"), null));
    }

    private static com.winedine.model.Restaurant restaurant(String name, String cuisine) {
        return new com.winedine.model.Restaurant("x", name, "addr", 43.65, -79.38, cuisine,
                java.util.List.of(), null, null, null, java.util.List.of());
    }
}