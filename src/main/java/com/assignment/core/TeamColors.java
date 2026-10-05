package com.assignment.core;

import javafx.scene.paint.Color;

// The two player kit colours chosen on the pre-match screen. They drive both looks and
// rules: each player is safe on its own colour of river and pushes its own blocks.
public final class TeamColors {
    private TeamColors() {}

    // kit colours (default yellow / green); reassigned when players pick kits
    public static Color p1 = Color.web("#f4d03f");
    public static Color p2 = Color.web("#58d68d");

    // deep/surface water shades per player, used by the rivers
    public static Color p1Deep()    { return p1; }
    public static Color p1Surface() { return p1.interpolate(Color.WHITE, 0.45); }
    public static Color p2Deep()    { return p2; }
    public static Color p2Surface() { return p2.interpolate(Color.WHITE, 0.45); }

    // palette for neutral props (match flags), kept distinct from the kit colours
    private static final Color[] FLAG_PALETTE = {
            Color.web("#e63946"), // red
            Color.web("#2563c9"), // blue
            Color.web("#7d3cc9"), // purple
            Color.web("#e67e22"), // orange
            Color.web("#138d75"), // teal
            Color.web("#d6336c"), // magenta
    };

    // Pick the palette colour most distinct from BOTH kits, so a flag never blends in.
    public static Color pickContrastColor() {
        Color best = FLAG_PALETTE[0];
        double bestScore = -1;
        for (Color c : FLAG_PALETTE) {
            double d = Math.min(dist(c, p1), dist(c, p2));
            if (d > bestScore) { bestScore = d; best = c; }
        }
        return best;
    }

    // squared RGB distance between two colours
    private static double dist(Color a, Color b) {
        double dr = a.getRed()   - b.getRed();
        double dg = a.getGreen() - b.getGreen();
        double db = a.getBlue()  - b.getBlue();
        return dr * dr + dg * dg + db * db;
    }
}
