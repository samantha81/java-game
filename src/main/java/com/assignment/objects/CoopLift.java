package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

// Co-op lift: rises only while BOTH players stand on it, sinks otherwise; carries its riders.
public class CoopLift extends CarryPlatform {
    private final double baseY;   // resting (lowest) position
    private final double topY;    // raised (highest) position
    private final double speed;   // px/sec

    public CoopLift(double x, double y, double width, double height,
                    double riseHeight, double speed) {
        super(x, y, width, height, Color.web("#4a90d9"));
        this.baseY = y;
        this.topY = y - riseHeight;
        this.speed = speed;
    }

    // Rise toward the top while both ride, else sink; record the delta so riders follow.
    @Override
    public void advance(double dt, boolean bothRiding) {
        double oldY = y;
        double target = bothRiding ? topY : baseY;
        if (y > target)      y = Math.max(target, y - speed * dt);  // rise
        else if (y < target) y = Math.min(target, y + speed * dt);  // sink
        dx = 0;
        dy = y - oldY;
    }

    // Draw the lift slab with two footprints on top and up-chevrons on the sides.
    @Override
    public void render(GraphicsContext gc) {
        // metal lift slab
        gc.setFill(new LinearGradient(0, y, 0, y + height, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7fb6ec")),
                new Stop(1, Color.web("#2f6fae"))));
        gc.fillRoundRect(x, y, width, height, 6, 6);
        gc.setStroke(Color.web("#1d4d7e"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, width, height, 6, 6);

        // two footprints on top
        gc.setFill(Color.web("#eaf4ff", 0.9));
        drawFoot(gc, x + width * 0.30 - 5, y - 9);
        drawFoot(gc, x + width * 0.70 - 5, y - 9);

        // up-chevrons on the sides
        gc.setStroke(Color.web("#ffffff", 0.7));
        gc.setLineWidth(2);
        chevron(gc, x + 10, y + height / 2);
        chevron(gc, x + width - 10, y + height / 2);
    }

    // one footprint mark
    private void drawFoot(GraphicsContext gc, double fx, double fy) {
        gc.fillOval(fx, fy, 10, 6);          // sole
        gc.fillOval(fx + 2, fy - 3, 6, 4);   // toe
    }

    // one up-chevron
    private void chevron(GraphicsContext gc, double cx, double cy) {
        gc.strokeLine(cx - 5, cy + 2, cx, cy - 4);
        gc.strokeLine(cx + 5, cy + 2, cx, cy - 4);
    }
}
