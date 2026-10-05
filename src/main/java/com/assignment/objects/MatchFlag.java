package com.assignment.objects;

import com.assignment.core.TeamColors;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

// A World Cup pennant planted at each match's start, as a world-anchored "MATCH n" cue.
public class MatchFlag extends StaticObject {
    private static final double POLE_H = 120;
    private static final double PENNANT_W = 46;
    private static final double PENNANT_H = 30;
    private static final long START = System.nanoTime();

    private final int number;
    private final Color pennant;
    private final Color pennantEdge;

    public MatchFlag(double x, double baseY, int number) {
        super(x, baseY - POLE_H, 4, POLE_H);
        this.number = number;
        this.pennant = TeamColors.pickContrastColor();          // distinct from both kits
        this.pennantEdge = pennant.deriveColor(0, 1, 0.6, 1);   // darker outline
    }

    // Draw the pole and the waving numbered pennant.
    @Override
    public void render(GraphicsContext gc) {
        double topY = y;

        // pole
        gc.setFill(Color.web("#d2d8df"));
        gc.fillRect(x, topY, 4, height);
        gc.setStroke(Color.web("#8a929b"));
        gc.setLineWidth(1);
        gc.strokeRect(x, topY, 4, height);

        // finial knob
        gc.setFill(Color.web("#f6c945"));
        gc.fillOval(x - 2, topY - 6, 8, 8);

        // waving pennant (ripple grows toward the tip)
        double bx = x + 4;
        double by = topY + 4;
        double t = (System.nanoTime() - START) * 1e-9;
        double phase = t * 5.0 + x * 0.05;   // per-flag offset so they're out of sync

        int seg = 12;
        int n = seg + 1;
        double[] px = new double[2 * n];
        double[] py = new double[2 * n];
        for (int i = 0; i <= seg; i++) {
            double p = (double) i / seg * PENNANT_W;
            double frac = p / PENNANT_W;
            double halfH = (1 - frac) * (PENNANT_H / 2);
            double wave = Math.sin(phase - p * 0.18) * 5.0 * frac;
            double midY = by + PENNANT_H / 2 + wave;
            px[i] = bx + p;             py[i] = midY - halfH;          // top edge L->R
            px[2 * n - 1 - i] = bx + p; py[2 * n - 1 - i] = midY + halfH; // bottom edge R->L
        }
        gc.setFill(pennant);
        gc.fillPolygon(px, py, 2 * n);
        gc.setStroke(pennantEdge);
        gc.setLineWidth(1.5);
        gc.strokePolygon(px, py, 2 * n);

        // match number near the pole
        double np = PENNANT_W * 0.30;
        double nWave = Math.sin(phase - np * 0.18) * 5.0 * (np / PENNANT_W);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(String.valueOf(number), bx + np, by + PENNANT_H / 2 + nWave + 5.5);
        gc.setTextAlign(TextAlignment.LEFT);
    }
}
