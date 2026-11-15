package com.winedine.ui.gui;

import com.winedine.model.Restaurant;
import com.winedine.model.SearchResult;
import com.winedine.ui.ResultFormatter;
import javafx.animation.AnimationTimer;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A hand-drawn style atlas of downtown Toronto (parchment by day, blueprint at night): Lake Ontario, the
 * islands, parks, the tilted street grid and landmarks, with one numbered pin per search result.
 * It is drawn from latitude/longitude, so pins land roughly where the restaurants are.
 * Hover a pin for its name; click it to select that result.
 */
final class AtlasMapView extends Pane {
    private static final double LAT0 = 43.650;
    private static final double LON0 = -79.392;
    private static final double KM_PER_LON = 80.5;
    private static final double KM_PER_LAT = 110.57;
    private static final double THETA = Math.toRadians(17);
    private static final double QX = (-79.3792 - LON0) * KM_PER_LON;
    private static final double QY = (43.6524 - LAT0) * KM_PER_LAT;

    private static final double[][] SHORE = {
            {-79.60, 43.600}, {-79.47, 43.622}, {-79.44, 43.630}, {-79.415, 43.6335}, {-79.395, 43.6355},
            {-79.380, 43.6365}, {-79.365, 43.6385}, {-79.350, 43.6425}, {-79.340, 43.6445}, {-79.30, 43.655},
            {-79.20, 43.680}};
    private static final double[][] ISLAND = {
            {-79.410, 43.6205}, {-79.385, 43.6235}, {-79.365, 43.6255}, {-79.352, 43.6275}, {-79.352, 43.6262},
            {-79.365, 43.6240}, {-79.385, 43.6215}, {-79.408, 43.6192}};
    /** lat, lon, half-height and half-width in degrees. */
    private static final double[][] PARKS = {
            {43.6479, -79.4155, 0.0030, 0.0042}, {43.6640, -79.3920, 0.0028, 0.0030},
            {43.6620, -79.3745, 0.0014, 0.0022}, {43.6556, -79.3700, 0.0010, 0.0014},
            {43.6640, -79.4210, 0.0022, 0.0030}};
    /** Offset east of Yonge in km, and 1 for a main street. */
    private static final double[][] NS = {
            {0.0, 1}, {-0.35, 0}, {-0.75, 1}, {-1.36, 1}, {-2.15, 1}, {-3.25, 1}, {-4.0, 1},
            {0.4, 0}, {0.75, 0}, {1.15, 1}, {1.85, 1}, {2.6, 0}, {-2.75, 0}};
    /** Offset north of Queen in km, 1 for a main street, 1 for the Gardiner. */
    private static final double[][] EW = {
            {0.0, 1, 0}, {-0.4, 1, 0}, {-0.79, 1, 0}, {0.45, 1, 0}, {1.04, 1, 0}, {2.11, 1, 0},
            {0.75, 0, 0}, {-0.2, 0, 0}, {-1.15, 1, 1}, {3.0, 1, 0}, {1.55, 0, 0}, {-1.5, 0, 0}};
    private static final Object[][] STREET_NAMES = {
            {"QUEEN ST", 0.0, -1.2}, {"KING ST", -0.4, 1.6}, {"DUNDAS ST", 0.45, -1.3},
            {"COLLEGE ST", 1.04, 0.6}, {"BLOOR ST", 2.11, -0.5}};
    private static final Object[][] NS_NAMES = {
            {"YONGE ST", 0.0, 1.2}, {"SPADINA AVE", -1.36, 1.5}, {"BATHURST ST", -2.15, 1.6}};
    private static final Object[][] LANDMARKS = {
            {"CN TOWER", 43.6426, -79.3871}, {"UNION STATION", 43.6453, -79.3806},
            {"YORKVILLE", 43.6709, -79.393}, {"KENSINGTON", 43.6547, -79.4005},
            {"ST LAWRENCE MKT", 43.6487, -79.3715}, {"DISTILLERY", 43.6503, -79.3596},
            {"LIBERTY VILLAGE", 43.6377, -79.4208}, {"OSSINGTON", 43.6477, -79.4195}};

    private final Canvas canvas = new Canvas();
    private final AnimationTimer timer;
    private final Consumer<Integer> onSelect;
    private final Consumer<Integer> onOpen;
    private final List<SearchResult> results = new ArrayList<>();
    private final List<double[]> pinSpots = new ArrayList<>();
    private GraphicsContext g;
    private boolean night;
    private int selected = -1;
    private int hover = -1;
    private long frame;
    private long lastNs;

    /** onSelect: a pin was clicked; onOpen: the already selected pin was clicked again. */
    AtlasMapView(Consumer<Integer> onSelect, Consumer<Integer> onOpen) {
        this.onSelect = onSelect;
        this.onOpen = onOpen;
        getChildren().add(canvas);
        setOnMouseMoved(this::moved);
        setOnMouseClicked(this::clicked);
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastNs < 50_000_000L || !isVisible()) {
                    return;
                }
                lastNs = now;
                frame++;
                draw();
            }
        };
        timer.start();
    }

    void setNight(boolean value) {
        night = value;
    }

    void setResults(List<SearchResult> list) {
        results.clear();
        results.addAll(list);
        selected = -1;
        hover = -1;
    }

    /** Selects a result by zero-based index (does not call back). */
    void select(int index) {
        selected = index;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
    }

    // ---- colours ----

    private Color land() { return night ? Color.web("#0f2a4d") : Color.web("#efe3c6"); }
    private Color water() { return night ? Color.web("#0a1d38") : Color.web("#b9d3d0"); }
    private Color park() { return night ? Color.web("#133f4a") : Color.web("#cdd9a6"); }
    private Color sand() { return night ? Color.web("#1d4573") : Color.web("#f4e8c8"); }
    private Color ink() { return night ? Color.web("#cfefff") : Color.web("#3b2a1a"); }
    private Color soft() { return night ? Color.web("#7fb2dd") : Color.web("#7b6747"); }
    private Color road() { return night ? Color.web("#2b5f95") : Color.web("#fbf5e6"); }
    private Color casing() { return night ? Color.web("#5fd0ff") : Color.web("#a88f5c"); }
    private Color red() { return night ? Color.web("#ff7b7b") : Color.web("#b3262e"); }
    private Color gold() { return night ? Color.web("#ffd36b") : Color.web("#c68a1d"); }

    private static Color a(Color c, double alpha) {
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // ---- projection ----

    private double ppk() {
        return Math.max(20, Math.min(getHeight() / 6.0, getWidth() / 9.0));
    }

    private double screenX(double lon) {
        return getWidth() / 2 + (lon - LON0) * KM_PER_LON * ppk();
    }

    private double screenY(double lat) {
        return getHeight() / 2 - (lat - LAT0) * KM_PER_LAT * ppk();
    }

    /** Screen x of a point given in km relative to the map centre. */
    private double kx(double xKm) {
        return getWidth() / 2 + xKm * ppk();
    }

    private double ky(double yKm) {
        return getHeight() / 2 - yKm * ppk();
    }

    private static double shoreLat(double lon) {
        if (lon <= SHORE[0][0]) {
            return SHORE[0][1];
        }
        for (int i = 1; i < SHORE.length; i++) {
            if (lon <= SHORE[i][0]) {
                double t = (lon - SHORE[i - 1][0]) / (SHORE[i][0] - SHORE[i - 1][0]);
                return SHORE[i - 1][1] + t * (SHORE[i][1] - SHORE[i - 1][1]);
            }
        }
        return SHORE[SHORE.length - 1][1];
    }

    // ---- input ----

    private int pinAt(double x, double y) {
        for (int k = pinSpots.size() - 1; k >= 0; k--) {
            double[] p = pinSpots.get(k);
            if (Math.abs(x - p[1]) < 11 && y > p[2] - 27 && y < p[2] + 3) {
                return (int) p[0];
            }
        }
        return -1;
    }

    private void moved(MouseEvent e) {
        hover = pinAt(e.getX(), e.getY());
        setCursor(hover >= 0 ? Cursor.HAND : Cursor.DEFAULT);
    }

    private void clicked(MouseEvent e) {
        int i = pinAt(e.getX(), e.getY());
        if (i < 0) {
            return;
        }
        if (i == selected) {
            onOpen.accept(i);
        } else {
            selected = i;
            onSelect.accept(i);
        }
    }

    // ---- text ----

    private Font mono(double size, boolean bold) {
        return Font.font(Fonts.MONO, bold ? FontWeight.BOLD : FontWeight.NORMAL, FontPosture.REGULAR, size);
    }

    private void text(String t, double x, double y, Font f, Color c, TextAlignment align) {
        g.setFont(f);
        g.setFill(c);
        g.setTextAlign(align);
        g.fillText(t, x, y);
    }

    /** Text with a paper-coloured halo so it stays readable over streets. */
    private void halo(String t, double x, double y, Font f, Color c, TextAlignment align) {
        g.setFont(f);
        g.setTextAlign(align);
        g.setLineWidth(3);
        g.setLineJoin(StrokeLineJoin.ROUND);
        g.setStroke(a(land(), 0.9));
        g.strokeText(t, x, y);
        g.setFill(c);
        g.fillText(t, x, y);
    }

    private double width(String t, Font f) {
        Text x = new Text(t);
        x.setFont(f);
        return x.getLayoutBounds().getWidth();
    }

    private void icon(IconKind kind, double x, double y, double size, Color c, double lineWidth) {
        g.save();
        g.translate(x, y);
        double k = size / 24.0;
        g.scale(k, k);
        g.setLineWidth(lineWidth / k);
        g.setStroke(c);
        g.setFill(c);
        g.beginPath();
        g.appendSVGPath(kind.path());
        if (kind.isFilled()) {
            g.fill();
        } else {
            g.stroke();
        }
        g.restore();
    }

    // ---- drawing ----

    private void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w < 40 || h < 40) {
            return;
        }
        g = canvas.getGraphicsContext2D();
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        g.setLineDashes((double[]) null);
        g.setFill(land());
        g.fillRect(0, 0, w, h);
        drawGraticule(w, h);
        drawParks();
        drawStreets();
        drawWater(w, h);
        drawStreetNames();
        drawLandmarks(w, h);
        drawFurniture(w, h);
        drawPins(w, h);
    }

    private void drawGraticule(double w, double h) {
        g.setLineWidth(0.5);
        g.setStroke(a(ink(), night ? 0.10 : 0.07));
        double step = ppk();
        for (double x = w / 2 % step; x < w; x += step) {
            g.strokeLine(x, 0, x, h);
        }
        for (double y = h / 2 % step; y < h; y += step) {
            g.strokeLine(0, y, w, y);
        }
    }

    private void drawParks() {
        for (double[] p : PARKS) {
            double rx = p[3] * KM_PER_LON * ppk();
            double ry = p[2] * KM_PER_LAT * ppk();
            double cx = screenX(p[1]);
            double cy = screenY(p[0]);
            g.setFill(park());
            g.fillOval(cx - rx, cy - ry, 2 * rx, 2 * ry);
            g.setStroke(a(ink(), 0.35));
            g.setLineWidth(0.6);
            g.strokeOval(cx - rx, cy - ry, 2 * rx, 2 * ry);
            g.setStroke(a(ink(), 0.18));
            for (double t = -ry + 4; t < ry; t += 4) {
                double half = rx * Math.sqrt(Math.max(0, 1 - (t * t) / (ry * ry)));
                g.strokeLine(cx - half + 2, cy + t, cx + half - 2, cy + t + 3);
            }
        }
    }

    private void street(double x1, double y1, double x2, double y2, double width, boolean gardiner) {
        g.setLineDashes((double[]) null);
        g.setLineWidth(width + 1.6);
        g.setStroke(a(casing(), night ? 0.55 : 0.8));
        g.strokeLine(x1, y1, x2, y2);
        g.setLineWidth(width);
        g.setStroke(gardiner ? gold() : road());
        if (gardiner) {
            g.setLineDashes(6, 3);
        }
        g.strokeLine(x1, y1, x2, y2);
        g.setLineDashes((double[]) null);
    }

    private void drawStreets() {
        double ppk = ppk();
        double cos = Math.cos(THETA);
        double sin = Math.sin(THETA);
        double reach = 14;
        for (double[] st : NS) {
            double bx = QX + st[0] * cos;
            double by = QY + st[0] * sin;
            street(kx(bx + reach * -sin), ky(by + reach * cos), kx(bx - reach * -sin), ky(by - reach * cos),
                    (st[1] == 1 ? 0.075 : 0.045) * ppk, false);
        }
        for (double[] st : EW) {
            double bx = QX + st[0] * -sin;
            double by = QY + st[0] * cos;
            street(kx(bx + reach * cos), ky(by + reach * sin), kx(bx - reach * cos), ky(by - reach * sin),
                    (st[2] == 1 ? 0.07 : st[1] == 1 ? 0.075 : 0.045) * ppk, st[2] == 1);
        }
    }

    private void drawWater(double w, double h) {
        int n = SHORE.length;
        double[] xs = new double[n + 2];
        double[] ys = new double[n + 2];
        for (int i = 0; i < n; i++) {
            xs[i] = screenX(SHORE[i][0]);
            ys[i] = screenY(SHORE[i][1]);
        }
        xs[n] = xs[n - 1];
        ys[n] = h + 200;
        xs[n + 1] = xs[0];
        ys[n + 1] = h + 200;
        g.setFill(water());
        g.fillPolygon(xs, ys, n + 2);
        // engraved shoreline: the coast plus two softer echoes
        g.setLineDashes((double[]) null);
        g.setStroke(ink());
        g.setLineWidth(1.1);
        g.strokePolyline(xs, ys, n);
        for (int k = 1; k <= 2; k++) {
            double[] ys2 = new double[n];
            for (int i = 0; i < n; i++) {
                ys2[i] = ys[i] + k * 7;
            }
            g.setStroke(a(ink(), 0.4 / k));
            g.setLineWidth(0.6);
            g.strokePolyline(xs, ys2, n);
        }
        // little waves
        g.setStroke(a(night ? Color.web("#5fd0ff") : Color.web("#4f7f86"), 0.6));
        g.setLineWidth(0.8);
        for (int k = 0; k < 26; k++) {
            double x = (k * 97.0 + frame * 0.4 * (1 + k % 3 * 0.3)) % (w + 40) - 20;
            double y = h - 12 - (k * 41.0) % Math.max(40, h / 3);
            double lon = LON0 + (x - w / 2) / ppk() / KM_PER_LON;
            double lat = LAT0 - (y - h / 2) / ppk() / KM_PER_LAT;
            if (lat < shoreLat(lon) - 0.0012) {
                g.strokeLine(x, y, x + 3, y - 1.5);
                g.strokeLine(x + 3, y - 1.5, x + 6, y);
            }
        }
        // islands
        double[] ix = new double[ISLAND.length];
        double[] iy = new double[ISLAND.length];
        for (int i = 0; i < ISLAND.length; i++) {
            ix[i] = screenX(ISLAND[i][0]);
            iy[i] = screenY(ISLAND[i][1]);
        }
        g.setFill(sand());
        g.fillPolygon(ix, iy, ISLAND.length);
        g.setStroke(ink());
        g.setLineWidth(0.9);
        g.strokePolygon(ix, iy, ISLAND.length);
        text("L A K E   O N T A R I O", screenX(-79.425), screenY(43.6285), mono(13, true), soft(), TextAlignment.CENTER);
        halo("TORONTO ISLANDS", screenX(-79.378), screenY(43.6225), mono(9, false), soft(), TextAlignment.CENTER);
    }

    private void drawStreetNames() {
        double cos = Math.cos(THETA);
        double sin = Math.sin(THETA);
        Font f = mono(8.5, true);
        for (Object[] n : STREET_NAMES) {
            double off = (Double) n[1];
            double t = (Double) n[2];
            double bx = QX + off * -sin + t * cos;
            double by = QY + off * cos + t * sin;
            label((String) n[0], kx(bx), ky(by), -Math.toDegrees(THETA), f);
        }
        for (Object[] n : NS_NAMES) {
            double off = (Double) n[1];
            double t = (Double) n[2];
            double bx = QX + off * cos + t * -sin;
            double by = QY + off * sin + t * cos;
            label((String) n[0], kx(bx), ky(by), -(90 - Math.toDegrees(THETA)), f);
        }
    }

    private void label(String t, double x, double y, double angle, Font f) {
        if (x < 0 || x > getWidth() || y < 0 || y > getHeight()) {
            return;
        }
        g.save();
        g.translate(x, y);
        g.rotate(angle);
        halo(t, 0, 3, f, soft(), TextAlignment.CENTER);
        g.restore();
    }

    private void drawLandmarks(double w, double h) {
        for (Object[] lm : LANDMARKS) {
            String name = (String) lm[0];
            double x = screenX((Double) lm[2]);
            double y = screenY((Double) lm[1]);
            if (x < 0 || x > w || y < 0 || y > h) {
                continue;
            }
            if (name.equals("CN TOWER")) {
                icon(IconKind.TOWER, x - 14, y - 34, 28, ink(), 1.3);
                halo(name, x, y + 12, mono(10, true), ink(), TextAlignment.CENTER);
            } else {
                g.setFill(gold());
                g.fillOval(x - 3, y - 3, 6, 6);
                g.setStroke(ink());
                g.setLineWidth(0.9);
                g.strokeOval(x - 3, y - 3, 6, 6);
                halo(name, x, y + 14, mono(8.5, true), ink(), TextAlignment.CENTER);
            }
        }
    }

    private void drawFurniture(double w, double h) {
        // compass rose with a maple-leaf north mark
        double cx = w - 46;
        double cy = 52;
        g.setFill(a(land(), 0.85));
        g.fillOval(cx - 26, cy - 26, 52, 52);
        g.setStroke(ink());
        g.setLineWidth(0.8);
        g.strokeOval(cx - 26, cy - 26, 52, 52);
        g.setLineWidth(0.4);
        g.strokeOval(cx - 20, cy - 20, 40, 40);
        g.setFill(ink());
        g.fillPolygon(new double[] {cx, cx - 5, cx + 5}, new double[] {cy - 24, cy, cy}, 3);
        g.setFill(a(ink(), 0.35));
        g.fillPolygon(new double[] {cx, cx - 5, cx + 5}, new double[] {cy + 24, cy, cy}, 3);
        g.setStroke(ink());
        g.strokeLine(cx - 22, cy, cx + 22, cy);
        text("N", cx, cy - 28, mono(12, true), ink(), TextAlignment.CENTER);
        text("THE NORTH", cx, cy + 40, mono(8, true), red(), TextAlignment.CENTER);
        icon(IconKind.MAPLE, cx - 5, cy - 4, 10, red(), 1);

        // title cartouche
        g.setFill(a(land(), 0.92));
        g.fillRect(12, 12, 232, 34);
        g.setStroke(ink());
        g.setLineWidth(1.2);
        g.strokeRect(12, 12, 232, 34);
        g.setLineWidth(0.4);
        g.strokeRect(15, 15, 226, 28);
        text("Atlas of Happy Hours", 22, 32, Font.font("Georgia", FontWeight.BOLD, FontPosture.ITALIC, 15), ink(),
                TextAlignment.LEFT);
        text("DOWNTOWN TORONTO  ·  43.65° N", 22, 41, mono(7.5, false), soft(), TextAlignment.LEFT);

        // scale bar: 1 km
        double bar = ppk();
        double bx = w - 20 - bar;
        double by = h - 22;
        g.setStroke(ink());
        g.setLineWidth(1.4);
        g.strokeLine(bx, by, bx + bar, by);
        g.strokeLine(bx, by - 4, bx, by + 4);
        g.strokeLine(bx + bar, by - 4, bx + bar, by + 4);
        text("1 KM", bx + bar / 2, by - 6, mono(8.5, true), ink(), TextAlignment.CENTER);

        // legend
        String[] names = {"FRESH", "STALE", "UNKNOWN", "NEAR MATCH"};
        int[] kinds = {0, 1, 2, 3};
        double lx = 14;
        double ly = h - 14;
        double lw = 0;
        for (String n : names) {
            lw += 22 + width(n, mono(8, true));
        }
        g.setFill(a(land(), 0.9));
        g.fillRect(lx - 6, ly - 12, lw + 8, 18);
        g.setStroke(a(ink(), 0.5));
        g.setLineWidth(0.6);
        g.strokeRect(lx - 6, ly - 12, lw + 8, 18);
        for (int i = 0; i < names.length; i++) {
            g.setFill(pinColor(kinds[i]));
            g.fillOval(lx, ly - 7, 9, 9);
            g.setStroke(ink());
            g.strokeOval(lx, ly - 7, 9, 9);
            text(names[i], lx + 13, ly + 1, mono(8, true), ink(), TextAlignment.LEFT);
            lx += 22 + width(names[i], mono(8, true));
        }
    }

    private Color pinColor(int kind) {
        return switch (kind) {
            case 0 -> night ? Color.web("#4de3a0") : Color.web("#5a8f29");
            case 1 -> night ? Color.web("#ffd36b") : Color.web("#d99a1e");
            case 3 -> night ? Color.web("#ff8fab") : Color.web("#c2415b");
            default -> night ? Color.web("#9fb4cc") : Color.web("#9a9488");
        };
    }

    private int pinKind(SearchResult r) {
        if (r.isNearMatch()) {
            return 3;
        }
        String f = ResultFormatter.freshness(r).name();
        if (f.equals("FRESH")) {
            return 0;
        }
        return f.equals("STALE") ? 1 : 2;
    }

    private void drawPins(double w, double h) {
        pinSpots.clear();
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < results.size(); i++) {
                if ((i == selected) != (pass == 1)) {
                    continue;
                }
                SearchResult r = results.get(i);
                Restaurant rest = r.getRestaurant();
                if (rest.getLatitude() == null || rest.getLongitude() == null) {
                    continue;
                }
                double x = screenX(rest.getLongitude());
                double y = screenY(rest.getLatitude());
                boolean sel = i == selected;
                double lift = sel ? Math.abs(Math.sin(frame / 3.0)) * 7 : 0;
                if (sel) {
                    double pulse = 9 + (frame % 20) * 0.6;
                    g.setStroke(a(red(), 1 - (frame % 20) / 20.0));
                    g.setLineWidth(1.4);
                    g.strokeOval(x - pulse, y - pulse * 0.4, pulse * 2, pulse * 0.8);
                }
                g.setFill(a(ink(), 0.25));
                g.fillOval(x - 5, y - 1.5, 10, 3);
                drawPin(x, y - lift, pinColor(pinKind(r)), i + 1, sel);
                pinSpots.add(new double[] {i, x, y - lift});
            }
        }
        if (hover >= 0 && hover < results.size()) {
            for (double[] p : pinSpots) {
                if ((int) p[0] == hover) {
                    String name = results.get(hover).getRestaurant().getName();
                    Font f = Font.font(Fonts.DISPLAY, FontWeight.BOLD, FontPosture.REGULAR, 13);
                    String hint = hover == selected ? "CLICK AGAIN: OPEN IN GOOGLE MAPS" : "CLICK TO SELECT";
                    double tw = Math.max(width(name, f), width(hint, mono(8, true))) + 18;
                    double bx = Math.max(6, Math.min(w - tw - 6, p[1] - tw / 2));
                    double by = Math.max(6, p[2] - 66);
                    g.setFill(land());
                    g.fillRect(bx, by, tw, 34);
                    g.setStroke(red());
                    g.setLineWidth(1.2);
                    g.strokeRect(bx, by, tw, 34);
                    text(name, bx + tw / 2, by + 15, f, ink(), TextAlignment.CENTER);
                    text(hint, bx + tw / 2, by + 27, mono(8, true), red(), TextAlignment.CENTER);
                }
            }
        }
        if (results.isEmpty()) {
            String msg = "Search to drop pins on the atlas";
            Font f = Font.font(Fonts.DISPLAY, FontWeight.BOLD, FontPosture.REGULAR, 15);
            double tw = width(msg, f) + 24;
            double bx = (w - tw) / 2;
            double by = h / 2 - 60;
            g.setFill(a(land(), 0.95));
            g.fillRect(bx, by, tw, 28);
            g.setStroke(red());
            g.setLineWidth(1.2);
            g.strokeRect(bx, by, tw, 28);
            text(msg, w / 2, by + 19, f, red(), TextAlignment.CENTER);
        }
    }

    /** A teardrop pin whose tip is at (x, y), with the result number inside. */
    private void drawPin(double x, double y, Color fill, int number, boolean sel) {
        g.save();
        g.translate(x, y);
        double k = sel ? 1.2 : 1.0;
        g.scale(k, k);
        g.beginPath();
        g.appendSVGPath("M0 0C-3 -7 -10 -10 -10 -17A10 10 0 1 1 10 -17C10 -10 3 -7 0 0Z");
        g.setFill(fill);
        g.fill();
        g.setStroke(ink());
        g.setLineWidth(1.4);
        g.stroke();
        g.setFill(a(Color.WHITE, 0.95));
        g.fillOval(-6, -23, 12, 12);
        text(String.valueOf(number), 0, -13.5, mono(number > 9 ? 8 : 10, true), Color.web("#2a2230"),
                TextAlignment.CENTER);
        g.restore();
    }
}