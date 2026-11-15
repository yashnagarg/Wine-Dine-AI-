package com.winedine.ui.gui;

import javafx.animation.AnimationTimer;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * The cover page: a Toronto restaurant brochure. By day it is parchment with ink line-art; at night it
 * becomes a blueprint. Everything is vector-drawn on a 320 x 180 page that scales to the window.
 * Click the sun/moon, the CN Tower, the streetcar or the drink icons; use the menu or the keyboard.
 */
final class CoverView extends Pane {
    private static final int W = 320;
    private static final int H = 180;
    private static final double TOWER_X = 60;
    private static final double SUN_X = 60;
    private static final double SUN_Y = 62;

    private static final Color DAY_PAPER1 = Color.web("#f7edd3");
    private static final Color DAY_PAPER2 = Color.web("#dfc896");
    private static final Color DAY_INK = Color.web("#3b2a1a");
    private static final Color DAY_SOFT = Color.web("#7b6747");
    private static final Color DAY_RED = Color.web("#b3262e");
    private static final Color DAY_GOLD = Color.web("#c68a1d");
    private static final Color NIGHT_PAPER1 = Color.web("#12305a");
    private static final Color NIGHT_PAPER2 = Color.web("#071731");
    private static final Color NIGHT_INK = Color.web("#d6ecff");
    private static final Color NIGHT_SOFT = Color.web("#8db6dc");
    private static final Color NIGHT_RED = Color.web("#ff7b7b");
    private static final Color NIGHT_GOLD = Color.web("#ffd36b");

    private record Hot(String id, double x, double y, double w, double h) {
        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private static final class Particle {
        double x, y, vx, vy;
        int life;
        Color color;
    }

    /** Menu items from top to bottom on screen: enter, mode switch, quit. */
    private static final int[] ORDER = {0, 1, 2};

    private static int indexInOrder(int item) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == item) {
                return i;
            }
        }
        return 1;
    }

    private static final double[][] BUILDINGS = new double[80][3];
    private static int buildingCount;

    static {
        Random r = new Random(5);
        double x = -120;
        while (x < 440 && buildingCount < BUILDINGS.length) {
            double w = 8 + r.nextInt(11);
            BUILDINGS[buildingCount++] = new double[] {x, w, 34 + r.nextInt(70)};
            x += w + r.nextInt(3);
        }
    }

    private final Canvas canvas = new Canvas();
    private final AnimationTimer timer;
    private final String llmChip;
    private final Runnable onStart;
    private final Runnable onQuit;
    private final Consumer<Boolean> onMode;
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Hot> hots = new ArrayList<>();
    private final Map<String, Font> fonts = new HashMap<>();
    private final Map<String, Double> widths = new HashMap<>();

    private GraphicsContext g;
    private double s = 1;
    private double ox;
    private double oy;
    /** Visible edges of the canvas in page units (the page is centred, so these reach past 0..W, 0..H). */
    private double left;
    private double right;
    private double top;
    private double bottom;
    private boolean night;
    private int selected;
    private int hover = -1;
    private long frame;
    private long lastNs;
    private String toast = "";
    private int toastTicks;
    private double toastX;
    private double toastY;

    CoverView(boolean startNight, String llmChip, Runnable onStart, Runnable onQuit, Consumer<Boolean> onMode) {
        this.night = startNight;
        this.llmChip = llmChip == null ? "" : llmChip;
        this.onStart = onStart;
        this.onQuit = onQuit;
        this.onMode = onMode;
        getChildren().add(canvas);
        setFocusTraversable(true);
        setOnMouseMoved(this::moved);
        setOnMouseClicked(this::clicked);
        setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.LEFT || k == KeyCode.UP || k == KeyCode.A || k == KeyCode.W) {
                selected = ORDER[(indexInOrder(selected) + 2) % 3];
            } else if (k == KeyCode.RIGHT || k == KeyCode.DOWN || k == KeyCode.D || k == KeyCode.S) {
                selected = ORDER[(indexInOrder(selected) + 1) % 3];
            } else if (k == KeyCode.ENTER || k == KeyCode.SPACE) {
                activate(selected);
            } else if (k == KeyCode.N) {
                toggleMode();
            }
            e.consume();
        });
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastNs < 33_000_000L) {
                    return;
                }
                lastNs = now;
                frame++;
                draw();
            }
        };
        timer.start();
    }

    void stop() {
        timer.stop();
    }

    void setNight(boolean value) {
        night = value;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
    }

    // ---- input ----

    private void moved(MouseEvent e) {
        double lx = (e.getX() - ox) / s;
        double ly = (e.getY() - oy) / s;
        hover = menuAt(lx, ly);
        if (hover >= 0) {
            selected = hover;
        }
        setCursor(hover >= 0 || hotAt(lx, ly) != null ? Cursor.HAND : Cursor.DEFAULT);
    }

    private void clicked(MouseEvent e) {
        requestFocus();
        double lx = (e.getX() - ox) / s;
        double ly = (e.getY() - oy) / s;
        int m = menuAt(lx, ly);
        if (m >= 0) {
            activate(m);
            return;
        }
        Hot h = hotAt(lx, ly);
        if (h == null) {
            return;
        }
        if (h.id().equals("sky")) {
            toggleMode();
            return;
        }
        toast = switch (h.id()) {
            case "wine" -> "CHEERS!";
            case "martini" -> "SHAKEN, NOT STIRRED";
            case "beer" -> "A COLD ONE FOR THE NORTH";
            case "pizza" -> "A SLICE OF THE 6IX";
            case "wings" -> "WING NIGHT!";
            case "streetcar" -> "504 KING - MIND THE TRACKS";
            case "tower" -> "CN TOWER - 553 M";
            default -> "";
        };
        toastTicks = 80;
        toastX = Math.max(60, Math.min(260, h.x() + h.w() / 2));
        toastY = Math.max(24, h.y() - 6);
        burst(h.x() + h.w() / 2, h.y() + h.h() / 2);
    }

    private Hot hotAt(double lx, double ly) {
        for (int i = hots.size() - 1; i >= 0; i--) {
            if (hots.get(i).contains(lx, ly)) {
                return hots.get(i);
            }
        }
        return null;
    }

    private void activate(int item) {
        if (item == 0) {
            onStart.run();
        } else if (item == 1) {
            toggleMode();
        } else {
            onQuit.run();
        }
    }

    private void toggleMode() {
        night = !night;
        onMode.accept(night);
    }

    private void burst(double x, double y) {
        Color[] palette = {red(), gold(), ink(), Color.web("#2f8f83")};
        for (int i = 0; i < 18; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            double a = random.nextDouble() * Math.PI * 2;
            double v = 0.5 + random.nextDouble() * 1.4;
            p.vx = Math.cos(a) * v;
            p.vy = Math.sin(a) * v - 0.9;
            p.life = 26 + random.nextInt(16);
            p.color = palette[random.nextInt(palette.length)];
            particles.add(p);
        }
    }

    // ---- menu layout (page units) ----

    private String[] menuLabels() {
        return new String[] {"ENTER THE ATLAS", night ? "PARCHMENT MODE" : "BLUEPRINT MODE", "QUIT"};
    }

    /** x, y, w, h of item 0 (enter button), 1 (mode link) and 2 (quit link): a column on the right. */
    private double[][] menuRects() {
        Font link = font(Fonts.MONO, FontWeight.BOLD, FontPosture.REGULAR, 4.6);
        double edge = W - 14;
        double enterW = measure(menuLabels()[0], link) + 16;
        double modeW = measure(menuLabels()[1], link) + 2;
        double quitW = measure(menuLabels()[2], link) + 2;
        return new double[][] {
                {edge - enterW, 94, enterW, 16},
                {edge - modeW, 117, modeW, 8},
                {edge - quitW, 129, quitW, 8}};
    }

    private int menuAt(double lx, double ly) {
        double[][] rects = menuRects();
        for (int i = 0; i < rects.length; i++) {
            double[] r = rects[i];
            if (lx >= r[0] && lx < r[0] + r[2] && ly >= r[1] && ly < r[1] + r[3]) {
                return i;
            }
        }
        return -1;
    }

    // ---- theme colours ----

    private Color paper1() { return night ? NIGHT_PAPER1 : DAY_PAPER1; }
    private Color paper2() { return night ? NIGHT_PAPER2 : DAY_PAPER2; }
    private Color ink() { return night ? NIGHT_INK : DAY_INK; }
    private Color soft() { return night ? NIGHT_SOFT : DAY_SOFT; }
    private Color red() { return night ? NIGHT_RED : DAY_RED; }
    private Color gold() { return night ? NIGHT_GOLD : DAY_GOLD; }

    private static Color a(Color c, double alpha) {
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // ---- text helpers ----

    private Font font(String family, FontWeight w, FontPosture p, double size) {
        String key = family + "|" + w + "|" + p + "|" + size;
        return fonts.computeIfAbsent(key, k -> Font.font(family, w, p, size));
    }

    private double measure(String t, Font f) {
        String key = f.getName() + f.getSize() + "|" + t;
        return widths.computeIfAbsent(key, k -> {
            Text x = new Text(t);
            x.setFont(f);
            return x.getLayoutBounds().getWidth();
        });
    }

    /** Letter-spaces a caps label the way brochures do: "HAPPY HOUR" becomes "H A P P Y   H O U R". */
    private static String track(String t) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == ' ') {
                sb.append("   ");
            } else {
                sb.append(c);
                if (i < t.length() - 1 && t.charAt(i + 1) != ' ') {
                    sb.append(' ');
                }
            }
        }
        return sb.toString();
    }

    private void text(String t, double x, double y, Font f, Color c, TextAlignment align) {
        g.setFont(f);
        g.setFill(c);
        g.setTextAlign(align);
        g.fillText(t, x, y);
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

    // ---- the page ----

    private void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w < 10 || h < 10) {
            return;
        }
        g = canvas.getGraphicsContext2D();
        s = Math.min(w / W, h / H);
        ox = (w - W * s) / 2;
        oy = (h - H * s) / 2;
        g.setFill(new RadialGradient(0, 0, w / 2, h / 2, Math.max(w, h) * 0.75, false, CycleMethod.NO_CYCLE,
                new Stop(0, paper1()), new Stop(1, paper2())));
        g.fillRect(0, 0, w, h);

        g.save();
        g.translate(ox, oy);
        g.scale(s, s);
        left = -ox / s;
        right = W + ox / s;
        top = -oy / s;
        bottom = H + oy / s;
        hots.clear();
        drawGrid();
        drawFrame();
        drawSun();
        drawSkyline();
        drawTower();
        drawStreetcar();
        drawTitleBlock();
        drawIcons();
        drawMenu();
        drawParticles();
        drawToast();
        g.restore();
    }

    private void drawGrid() {
        g.setLineWidth(0.25);
        for (int x = (int) Math.floor(left / 10) * 10; x <= right; x += 10) {
            g.setStroke(a(ink(), x % 50 == 0 ? 0.16 : 0.07));
            g.strokeLine(x, top, x, bottom);
        }
        for (int y = (int) Math.floor(top / 10) * 10; y <= bottom; y += 10) {
            g.setStroke(a(ink(), y % 50 == 0 ? 0.16 : 0.07));
            g.strokeLine(left, y, right, y);
        }
    }

    /** No border box: just small brochure details tucked into the corners of the window. */
    private void drawFrame() {
        Font mono = font(Fonts.MONO, FontWeight.NORMAL, FontPosture.REGULAR, 4.2);
        text("N\u00B0 01  /  HAPPY HOUR ATLAS", left + 14, top + 17, mono, soft(), TextAlignment.LEFT);
        text("43.6426\u00B0 N   79.3871\u00B0 W", right - 14, top + 17, mono, soft(), TextAlignment.RIGHT);
        text("AI  \u00B7  " + llmChip.toUpperCase(Locale.ROOT), W / 2.0, top + 17, mono, soft(), TextAlignment.CENTER);
        text("EST. 2026", left + 14, bottom - 10, mono, soft(), TextAlignment.LEFT);
        text("TORONTO  \u00B7  ONTARIO  \u00B7  CANADA", right - 14, bottom - 10, mono, soft(), TextAlignment.RIGHT);
    }

    private void drawSun() {
        hots.add(new Hot("sky", SUN_X - 32, SUN_Y - 32, 64, 64));
        if (night) {
            g.setFill(a(NIGHT_INK, 0.10));
            g.fillOval(SUN_X - 26, SUN_Y - 26, 52, 52);
            g.setStroke(NIGHT_INK);
            g.setLineWidth(0.7);
            g.strokeOval(SUN_X - 26, SUN_Y - 26, 52, 52);
            g.setLineWidth(0.4);
            g.strokeOval(SUN_X - 20, SUN_Y - 20, 40, 40);
            g.strokeOval(SUN_X - 12, SUN_Y - 18, 8, 8);
            g.strokeOval(SUN_X + 6, SUN_Y + 4, 11, 11);
            g.strokeOval(SUN_X - 16, SUN_Y + 6, 6, 6);
            for (int i = 0; i < 14; i++) {
                double x = 20 + (i * 53) % 280;
                double y = 22 + (i * 37) % 70;
                if (Math.hypot(x - SUN_X, y - SUN_Y) > 34 && x > 20 && x < 300 && (x < 105 || y < 38)) {
                    boolean on = (frame / 10 + i) % 6 != 0;
                    g.setFill(a(NIGHT_INK, on ? 0.8 : 0.25));
                    g.fillRect(x, y, 0.9, 0.9);
                }
            }
        } else {
            g.setFill(a(DAY_GOLD, 0.28));
            g.fillOval(SUN_X - 26, SUN_Y - 26, 52, 52);
            g.setStroke(DAY_INK);
            g.setLineWidth(0.7);
            g.strokeOval(SUN_X - 26, SUN_Y - 26, 52, 52);
            g.setLineWidth(0.35);
            g.strokeOval(SUN_X - 21, SUN_Y - 21, 42, 42);
            double spin = frame * 0.004;
            g.setLineWidth(0.5);
            for (int i = 0; i < 28; i++) {
                double ang = spin + i * Math.PI * 2 / 28;
                double r1 = 29;
                double r2 = i % 2 == 0 ? 36 : 33;
                g.strokeLine(SUN_X + Math.cos(ang) * r1, SUN_Y + Math.sin(ang) * r1,
                        SUN_X + Math.cos(ang) * r2, SUN_Y + Math.sin(ang) * r2);
            }
        }
    }

    private void drawSkyline() {
        Color[] tints = night
                ? new Color[] {Color.web("#2a6aa6"), Color.web("#3a8fc4"), Color.web("#5b6fd1"), Color.web("#57abc2")}
                : new Color[] {Color.web("#e2c95b"), Color.web("#ecbc73"), Color.web("#cad47b"), Color.web("#78dbd5")};
        g.setStroke(a(ink(), 0.4));
        g.setLineWidth(0.5);
        for (int i = 0; i < buildingCount; i++) {
            double[] b = BUILDINGS[i];
            if (b[0] + b[1] < left - 4 || b[0] > right + 4) {
                continue;
            }
            double top = H - b[2];
            double height = bottom + 4 - top;
            g.setFill(a(tints[i % tints.length], 0.40));
            g.fillRect(b[0], top, b[1], height);
            g.strokeRect(b[0], top, b[1], height);
            g.setLineWidth(0.3);
            for (double wy = top + 4; wy < bottom; wy += 5) {
                g.setStroke(a(ink(), 0.16));
                g.strokeLine(b[0] + 2, wy, b[0] + b[1] - 2, wy);
            }
            g.setStroke(a(ink(), 0.4));
            g.setLineWidth(0.5);
            if (i % 4 == 0) {
                g.strokeLine(b[0] + b[1] / 2, top, b[0] + b[1] / 2, top - 5);
            }
        }
    }

    private void drawTower() {
        double cx = TOWER_X;
        g.setFill(paper1());
        g.setStroke(ink());
        g.setLineWidth(0.8);
        // lower shaft
        double foot = Math.max(bottom, H) + 4;
        double spread = 1.6 + (foot - 92) * 0.075;
        g.fillPolygon(new double[] {cx - spread, cx - 1.6, cx + 1.6, cx + spread},
                new double[] {foot, 92, 92, foot}, 4);
        g.strokePolygon(new double[] {cx - spread, cx - 1.6, cx + 1.6, cx + spread},
                new double[] {foot, 92, 92, foot}, 4);
        g.setLineWidth(0.4);
        g.strokeLine(cx, foot, cx, 92);
        // pod
        double[] px = {cx - 8, cx + 8, cx + 10.5, cx + 8, cx - 8, cx - 10.5};
        double[] py = {66, 66, 71.5, 77, 77, 71.5};
        g.setLineWidth(0.8);
        g.fillPolygon(px, py, 6);
        g.strokePolygon(px, py, 6);
        g.setLineWidth(0.4);
        g.setLineDashes(1.2, 0.8);
        g.strokeLine(cx - 9.5, 71.5, cx + 9.5, 71.5);
        g.setLineDashes((double[]) null);
        // shafts, sky pod and spire
        g.setLineWidth(0.8);
        g.fillRect(cx - 1.5, 77, 3, 15);
        g.strokeRect(cx - 1.5, 77, 3, 15);
        g.fillRect(cx - 1.4, 44, 2.8, 22);
        g.strokeRect(cx - 1.4, 44, 2.8, 22);
        g.fillOval(cx - 2.6, 47, 5.2, 4);
        g.strokeOval(cx - 2.6, 47, 5.2, 4);
        g.fillPolygon(new double[] {cx - 1.2, cx + 1.2, cx + 0.3, cx - 0.3},
                new double[] {44, 44, 20, 20}, 4);
        g.strokePolygon(new double[] {cx - 1.2, cx + 1.2, cx + 0.3, cx - 0.3},
                new double[] {44, 44, 20, 20}, 4);
        boolean blink = night && (frame / 14) % 2 == 0;
        g.setFill(blink ? red() : a(red(), night ? 0.35 : 0.9));
        g.fillOval(cx - 1.2, 18.8, 2.4, 2.4);
        g.save();
        g.translate(cx - 13, 112);
        g.rotate(-90);
        text("CN TOWER  \u00B7  553 M", 0, 0, font(Fonts.MONO, FontWeight.BOLD, FontPosture.REGULAR, 4.0), soft(),
                TextAlignment.CENTER);
        g.restore();
        hots.add(new Hot("tower", cx - 12, 18, 24, H - 18));
    }

    private void drawStreetcar() {
        double x = (frame * 0.5) % (W + 60) - 40;
        double y = 158;
        g.setFill(red());
        g.setStroke(ink());
        g.setLineWidth(0.5);
        g.fillRoundRect(x, y, 26, 7, 1.5, 1.5);
        g.strokeRoundRect(x, y, 26, 7, 1.5, 1.5);
        g.setFill(night ? NIGHT_GOLD : DAY_PAPER1);
        for (int k = 0; k < 5; k++) {
            g.fillRect(x + 2.5 + k * 4.6, y + 1.5, 3, 2.4);
        }
        g.setStroke(ink());
        g.strokeLine(x + 13, y, x + 13, y - 3);
        g.strokeLine(x + 10, y - 3, x + 16, y - 3);
        g.setFill(ink());
        g.fillOval(x + 4, y + 6, 2.4, 2.4);
        g.fillOval(x + 19.6, y + 6, 2.4, 2.4);
        hots.add(new Hot("streetcar", x, y - 3, 26, 11));
    }

    private void drawTitleBlock() {
        double x0 = 112;
        double maxW = W - 16 - x0;
        double size = 24;
        Font title = font(Fonts.DISPLAY, FontWeight.BOLD, FontPosture.REGULAR, size);
        while (measure("Wine & Dine AI", title) > maxW && size > 12) {
            size -= 0.5;
            title = font(Fonts.DISPLAY, FontWeight.BOLD, FontPosture.REGULAR, size);
        }
        double w1 = measure("Wine ", title);
        double w2 = measure("& ", title);
        text("Wine ", x0, 47, title, ink(), TextAlignment.LEFT);
        text("&", x0 + w1, 47, title, red(), TextAlignment.LEFT);
        text("Dine AI", x0 + w1 + w2, 47, title, ink(), TextAlignment.LEFT);

        text(track("TORONTO HAPPY HOUR ATLAS"), x0, 59,
                font(Fonts.MONO, FontWeight.BOLD, FontPosture.REGULAR, 4.6), soft(), TextAlignment.LEFT);

        // "We the North" on a ribbon banner
        double ry = 65;
        double rw = 118;
        g.setFill(red());
        g.fillPolygon(new double[] {x0 - 6, x0, x0, x0 - 6, x0 - 3}, new double[] {ry + 2, ry + 2, ry + 15, ry + 15, ry + 8.5}, 5);
        g.fillPolygon(new double[] {x0 + rw, x0 + rw + 6, x0 + rw + 3, x0 + rw + 6, x0 + rw},
                new double[] {ry + 2, ry + 2, ry + 8.5, ry + 15, ry + 15}, 5);
        g.fillRect(x0, ry, rw, 14);
        g.setStroke(ink());
        g.setLineWidth(0.5);
        g.strokeRect(x0, ry, rw, 14);
        g.setStroke(a(Color.WHITE, 0.55));
        g.setLineWidth(0.35);
        g.setLineDashes(1.5, 1);
        g.strokeRect(x0 + 1.6, ry + 1.6, rw - 3.2, 10.8);
        g.setLineDashes((double[]) null);
        icon(IconKind.MAPLE, x0 + 5, ry + 2, 10, Color.web("#fff7e6"), 1);
        icon(IconKind.MAPLE, x0 + rw - 15, ry + 2, 10, Color.web("#fff7e6"), 1);
        text("We the North", x0 + rw / 2, ry + 10.2, font(Fonts.SCRIPT, FontWeight.BOLD, FontPosture.REGULAR, 8.4),
                Color.web("#fff7e6"), TextAlignment.CENTER);

        // the three things the agent asks about, as one quiet line
        Font mono = font(Fonts.MONO, FontWeight.BOLD, FontPosture.REGULAR, 4.8);
        String[] words = {"WHERE", "WHEN", "HOW MUCH"};
        double total = 0;
        for (String w : words) {
            total += measure(w, mono);
        }
        double gap = 9;
        total += gap * 2 + 3.2 * 2;
        double cx = x0 + (rw - total) / 2;
        for (int i = 0; i < words.length; i++) {
            text(words[i], cx, 92, mono, ink(), TextAlignment.LEFT);
            cx += measure(words[i], mono);
            if (i < words.length - 1) {
                g.setFill(red());
                g.fillOval(cx + gap / 2 - 0.2, 90.3, 1.6, 1.6);
                cx += gap + 3.2;
            }
        }
    }

    private void drawIcons() {
        IconKind[] kinds = {IconKind.WINE, IconKind.MARTINI, IconKind.BEER, IconKind.PIZZA, IconKind.WINGS};
        String[] ids = {"wine", "martini", "beer", "pizza", "wings"};
        String[] names = {"WINE", "COCKTAILS", "BEER", "PIZZA", "WINGS"};
        Color[] fills = {red(), gold(), gold(), red(), gold()};
        Font mono = font(Fonts.MONO, FontWeight.BOLD, FontPosture.REGULAR, 3.8);
        double x0 = 112;
        double rw = 118;
        double step = rw / kinds.length;
        for (int i = 0; i < kinds.length; i++) {
            double cx = x0 + step * i + step / 2;
            boolean lifted = (frame / 18) % 5 == i;
            double y = 103 + (lifted ? -1.6 : 0);
            g.save();
            g.translate(cx - 8, y);
            double k = 16 / 24.0;
            g.scale(k, k);
            g.beginPath();
            g.appendSVGPath(kinds[i].path());
            g.setFill(a(fills[i], 0.45));
            g.fill();
            g.setLineWidth(1.5 / k);
            g.setStroke(ink());
            g.stroke();
            g.restore();
            text(names[i], cx, 125, mono, soft(), TextAlignment.CENTER);
            hots.add(new Hot(ids[i], cx - 10, y - 2, 20, 22));
        }
    }

    private void drawMenu() {
        double[][] rects = menuRects();
        String[] labels = menuLabels();
        int active = hover >= 0 ? hover : selected;
        Font mono = font(Fonts.MONO, FontWeight.BOLD, FontPosture.REGULAR, 4.6);

        // enter button (the only boxed item)
        double[] r = rects[0];
        g.setFill(active == 0 ? red() : ink());
        g.fillRect(r[0], r[1], r[2], r[3]);
        g.setStroke(paper1());
        g.setLineWidth(0.3);
        g.strokeRect(r[0] + 1.5, r[1] + 1.5, r[2] - 3, r[3] - 3);
        text(labels[0], r[0] + r[2] / 2, r[1] + 10, mono, paper1(), TextAlignment.CENTER);

        // mode switch and quit: plain text links under it, lined up on the right
        for (int i = 1; i <= 2; i++) {
            double[] l = rects[i];
            boolean hot = active == i;
            text(labels[i], l[0] + l[2], l[1] + 6, mono, hot ? red() : soft(), TextAlignment.RIGHT);
            if (hot) {
                g.setStroke(red());
                g.setLineWidth(0.5);
                g.strokeLine(l[0], l[1] + 8, l[0] + l[2], l[1] + 8);
            }
        }
    }

    private void drawParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.07;
            p.life--;
            if (p.life <= 0) {
                particles.remove(i);
            } else {
                g.setFill(p.color);
                g.fillRect(p.x, p.y, 1.8, 1.8);
            }
        }
    }

    private void drawToast() {
        if (toastTicks <= 0) {
            return;
        }
        toastTicks--;
        Font f = font(Fonts.MONO, FontWeight.BOLD, FontPosture.REGULAR, 5.4);
        double tw = measure(toast, f) + 12;
        g.save();
        g.translate(toastX, toastY);
        g.rotate(-4);
        g.setGlobalAlpha(Math.min(1.0, toastTicks / 15.0));
        g.setFill(paper1());
        g.fillRect(-tw / 2, -8, tw, 12);
        g.setStroke(red());
        g.setLineWidth(1);
        g.strokeRect(-tw / 2, -8, tw, 12);
        text(toast, 0, 0, f, red(), TextAlignment.CENTER);
        g.setGlobalAlpha(1.0);
        g.restore();
    }
}