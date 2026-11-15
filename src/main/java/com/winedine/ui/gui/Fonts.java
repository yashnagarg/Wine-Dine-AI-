package com.winedine.ui.gui;

import javafx.scene.text.Font;

import java.util.HashSet;
import java.util.Set;

/**
 * The app's font choices in one place. Each role lists fonts in order of preference and the first one
 * installed on the computer is used, so the app still looks fine on a machine without them.
 * To try other fonts, change the names below (the CSS file lists the same families for the Swing-free parts).
 */
final class Fonts {
    private static final Set<String> INSTALLED = new HashSet<>(Font.getFamilies());

    /** Big headings: a vintage slab serif, like an old menu. */
    static final String DISPLAY = pick("Rockwell", "Georgia", "Times New Roman");
    /** Reading text. */
    static final String BODY = pick("Constantia", "Georgia", "Times New Roman");
    /** Handwritten flourishes such as "We the North". */
    static final String SCRIPT = pick( "Script", "Lucida Handwriting","Brush Script", "Georgia");
    /** Labels, coordinates and buttons: the techy half of the brochure. */
    static final String MONO = pick("Consolas", "Lucida Console", "Courier New");

    private Fonts() { }

    private static String pick(String... names) {
        for (String n : names) {
            if (INSTALLED.contains(n)) {
                return n;
            }
        }
        return names[names.length - 1];
    }
}