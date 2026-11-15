package com.winedine.ui.gui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

/** Builds JavaFX nodes from {@link IconKind}. Colours come from the CSS classes glyph / glyph-light. */
final class Glyphs {
    private Glyphs() { }

    /** A square node of the given size holding the line icon in ink colour, centred. */
    static Node icon(IconKind kind, double size) {
        return build(kind, size, kind.isFilled() ? "glyph-fill" : "glyph");
    }

    /** A coloured round badge with a cream icon on it: the fun version used on result cards. */
    static Node badge(IconKind kind, double size) {
        Circle disc = new Circle(size / 2, Color.rgb((kind.color() >> 16) & 255, (kind.color() >> 8) & 255,
                kind.color() & 255));
        disc.getStyleClass().add("badge-disc");
        Node icon = build(kind, size * 0.62, kind.isFilled() ? "glyph-fill-light" : "glyph-light");
        StackPane box = new StackPane(disc, icon);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    private static Node build(IconKind kind, double size, String styleClass) {
        SVGPath p = new SVGPath();
        p.setContent(kind.path());
        p.getStyleClass().add(styleClass);
        double k = size / 24.0;
        p.setScaleX(k);
        p.setScaleY(k);
        StackPane box = new StackPane(new Group(p));
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }
}