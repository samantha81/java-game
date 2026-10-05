package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

// A football collectible: one per match in the final level. Grab them all to unlock
// the Cup portal. Drawn as a classic soccer ball.
public class Ball extends StaticObject {
    private static final double RENDER_SIZE = 28;   // <= 30px hitbox, so it never bleeds into platforms

    private boolean collected = false;
    private double bob = 0;

    public Ball(double x, double y) {
        super(x, y, 30, 30);
    }

    // collected state
    public boolean isCollected() { return collected; }
    public void collect()        { collected = true; }

    // Advance the bob animation.
    public void update(double deltaTime) {
        bob += deltaTime * 3.0;
    }

    // Draw the bobbing football.
    @Override
    public void render(GraphicsContext gc) {
        if (collected) return;
        double yOff = Math.sin(bob) * 3;
        draw(gc, x + width / 2, y + height / 2 + yOff, RENDER_SIZE / 2);
    }

    // Draw a soccer ball (white body, pentagons, seams) at (cx,cy) with radius r.
    // Static so the shoot-out can reuse the same design.
    public static void draw(GraphicsContext gc, double cx, double cy, double r) {
        Color navy  = Color.web("#1b2147");
        Color white = Color.web("#f4f7fa");

        // soft glow
        gc.setFill(Color.web("#ffffff", 0.22));
        gc.fillOval(cx - r - 3, cy - r - 3, (r + 3) * 2, (r + 3) * 2);

        // white ball body
        gc.setFill(white);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        // clip so the patches stay inside the ball
        gc.save();
        gc.beginPath();
        gc.arc(cx, cy, r, r, 0, 360);
        gc.closePath();
        gc.clip();

        gc.setFill(navy);
        gc.setStroke(navy);
        gc.setLineWidth(Math.max(2, r * 0.13));
        gc.setLineCap(StrokeLineCap.ROUND);

        // central pentagon
        fillPentagon(gc, cx, cy, r * 0.42, -90);

        for (int k = 0; k < 5; k++) {
            double tv = Math.toRadians(-90 + k * 72);        // toward a vertex
            double te = Math.toRadians(-90 + 36 + k * 72);   // toward an edge

            // seam out to the rim
            gc.strokeLine(cx + Math.cos(tv) * r * 0.36, cy + Math.sin(tv) * r * 0.36,
                          cx + Math.cos(tv) * r * 1.05, cy + Math.sin(tv) * r * 1.05);

            // rim pentagon
            double rx = cx + Math.cos(te) * r * 0.84;
            double ry = cy + Math.sin(te) * r * 0.84;
            fillPentagon(gc, rx, ry, r * 0.34, Math.toDegrees(te));
        }
        gc.restore();

        // dark rim
        gc.setStroke(Color.web("#11152e"));
        gc.setLineWidth(2);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }

    // one filled pentagon
    private static void fillPentagon(GraphicsContext gc, double cx, double cy, double r, double phiDeg) {
        double[] xs = new double[5];
        double[] ys = new double[5];
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(phiDeg + i * 72);
            xs[i] = cx + r * Math.cos(a);
            ys[i] = cy + r * Math.sin(a);
        }
        gc.fillPolygon(xs, ys, 5);
    }
}
