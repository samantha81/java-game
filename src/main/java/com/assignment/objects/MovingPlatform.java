package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

// A platform that ping-pongs between two points, carrying any rider on top.
public class MovingPlatform extends CarryPlatform {
    private final double ax, ay;   // endpoint A
    private final double bx, by;   // endpoint B
    private final double speed;
    private final double segLen;
    private double t = 0;          // progress A->B
    private int dir = 1;

    public MovingPlatform(double x, double y, double width, double height,
                          double bx, double by, double speed) {
        super(x, y, width, height, Color.web("#7d8794"));
        this.ax = x;
        this.ay = y;
        this.bx = bx;
        this.by = by;
        this.speed = speed;
        this.segLen = Math.hypot(bx - x, by - y);
    }

    // Advance one frame along the A<->B path, recording the move delta so riders follow.
    @Override
    public void advance(double dt, boolean bothRiding) {
        if (segLen < 1e-6) { dx = 0; dy = 0; return; }
        double oldX = x, oldY = y;
        t += dir * (speed / segLen) * dt;
        if (t >= 1) { t = 1; dir = -1; }
        else if (t <= 0) { t = 0; dir = 1; }
        x = ax + (bx - ax) * t;
        y = ay + (by - ay) * t;
        dx = x - oldX;
        dy = y - oldY;
    }

    // Draw the metal slab platform.
    @Override
    public void render(GraphicsContext gc) {
        // metal slab body
        gc.setFill(new LinearGradient(0, y, 0, y + height, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#aab4c0")),
                new Stop(0.5, Color.web("#7d8794")),
                new Stop(1, Color.web("#5a626d"))));
        gc.fillRoundRect(x, y, width, height, 8, 8);
        gc.setStroke(Color.web("#3c424b"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, width, height, 8, 8);

        // top edge
        gc.setStroke(Color.web("#d7dee6", 0.8));
        gc.setLineWidth(2);
        gc.strokeLine(x + 5, y + 3, x + width - 5, y + 3);

        // corner bolts
        gc.setFill(Color.web("#3c424b"));
        double r = 3;
        gc.fillOval(x + 6, y + height / 2 - r, r * 2, r * 2);
        gc.fillOval(x + width - 12, y + height / 2 - r, r * 2, r * 2);

        // motion stripes
        gc.setStroke(Color.web("#f4c542", 0.9));
        gc.setLineWidth(2);
        double midY = y + height / 2;
        for (double sx = x + width / 2 - 12; sx <= x + width / 2 + 8; sx += 6) {
            gc.strokeLine(sx, midY - 4, sx + 4, midY + 4);
        }
    }
}
