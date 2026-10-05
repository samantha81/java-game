package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// A floor button (Pico Park-style) that grows (green +) or shrinks (red −) a player on it.
// Not solid — the level checks who's standing on it and scales them while they stay.
public class SizePad extends StaticObject {
    private final int sign;        // +1 = grow, -1 = shrink
    private final double surfaceY; // floor top the pad sits flush on

    public SizePad(double x, double surfaceY, double width, int sign) {
        super(x, surfaceY - 10, width, 16);
        this.sign = sign;
        this.surfaceY = surfaceY;
    }

    // grow/shrink sign, and the floor level it sits on
    public int getSign() { return sign; }
    public double getSurfaceY() { return surfaceY; }

    // Draw the button with its + or − symbol.
    @Override
    public void render(GraphicsContext gc) {
        boolean grow = sign > 0;
        Color base = grow ? Color.web("#2f9e4f") : Color.web("#c43d2c");
        Color top  = grow ? Color.web("#5bd07a") : Color.web("#ef6a52");

        // shadow on the floor
        gc.setFill(Color.web("#000000", 0.18));
        gc.fillRoundRect(x - 2, y + 8, width + 4, 12, 10, 10);
        // button body + raised cap
        gc.setFill(base);
        gc.fillRoundRect(x, y, width, 16, 10, 10);
        gc.setFill(top);
        gc.fillRoundRect(x + 3, y + 1, width - 6, 8, 8, 8);

        // + / − symbol
        gc.setFill(Color.WHITE);
        double cx = x + width / 2, cy = y + 8;
        double bar = Math.min(width * 0.36, 22), t = 4.5;
        gc.fillRect(cx - bar / 2, cy - t / 2, bar, t);            // horizontal stroke
        if (grow) gc.fillRect(cx - t / 2, cy - bar / 2, t, bar);  // vertical stroke for +
    }
}
