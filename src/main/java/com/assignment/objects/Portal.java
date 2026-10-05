package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

// The level-exit portal: a spinning grey "black hole" when open, a locked door when locked.
// Both players must stand in it to clear the level.
public class Portal extends Goal {
    private static final double SIZE = 64;
    private double phase = 0;         // spin animation clock
    private boolean locked = false;   // locked = closed door, can't be entered

    public Portal(double x, double y) {
        super(x, y, SIZE, SIZE);
    }

    // lock state
    public void lock()        { locked = true; }
    public void unlock()      { locked = false; }
    public boolean isLocked() { return locked; }

    // Only reachable once unlocked.
    @Override
    public boolean isReachedBy(Player p) {
        return !locked && super.isReachedBy(p);
    }

    // Advance the spin.
    @Override
    public void update(double deltaTime) {
        phase += deltaTime * 3.2;
    }

    // Draw the spinning portal, or the locked door.
    @Override
    public void render(GraphicsContext gc) {
        if (locked) {
            renderDoor(gc);
            return;
        }
        double cx = x + width / 2;
        double cy = y + height / 2;
        double r = width / 2;

        gc.save();
        gc.translate(cx, cy);

        // grey disk fading to a black core
        gc.setFill(new RadialGradient(0, 0, 0, 0, r, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.BLACK),
                new Stop(0.45, Color.web("#1b1d20")),
                new Stop(0.78, Color.web("#6b7177")),
                new Stop(1.0, Color.web("#c3c8cd"))));
        gc.fillOval(-r, -r, r * 2, r * 2);

        // spiral arms swirling inward, rotating with the phase
        int arms = 3;
        int steps = 56;
        double turns = 2.3;
        double maxT = turns * 2 * Math.PI;
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        for (int a = 0; a < arms; a++) {
            double offset = a * (2 * Math.PI / arms) + phase;
            gc.beginPath();
            for (int s = 0; s <= steps; s++) {
                double t = maxT * s / steps;
                double frac = (double) s / steps;          // 0 center -> 1 rim
                double rr = r * 0.96 * frac;
                double ang = t + offset;
                double px = rr * Math.cos(ang);
                double py = rr * Math.sin(ang);
                if (s == 0) gc.moveTo(px, py); else gc.lineTo(px, py);
            }
            gc.setStroke(Color.web("#e7eaee", 0.85));
            gc.setLineWidth(2.6);
            gc.stroke();
        }

        // black core
        double core = r * 0.42;
        gc.setFill(new RadialGradient(0, 0, 0, 0, core, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.BLACK),
                new Stop(0.8, Color.BLACK),
                new Stop(1, Color.web("#2a2d31", 0))));
        gc.fillOval(-core, -core, core * 2, core * 2);

        gc.restore();
    }

    // Closed metal door shown while locked.
    private void renderDoor(GraphicsContext gc) {
        double cx = x + width / 2;

        // stone frame
        gc.setFill(Color.web("#3a3d42"));
        gc.fillRoundRect(x - 4, y - 4, width + 8, height + 8, 10, 10);

        // door slab
        gc.setFill(new LinearGradient(x, y, x + width, y, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8a9099")),
                new Stop(0.5, Color.web("#b9c0c8")),
                new Stop(1, Color.web("#8a9099"))));
        gc.fillRoundRect(x + 4, y + 2, width - 8, height - 4, 8, 8);
        gc.setStroke(Color.web("#5b6066"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 4, y + 2, width - 8, height - 4, 8, 8);

        // panel seam + rivets
        gc.setStroke(Color.web("#6b7177"));
        gc.setLineWidth(1.5);
        gc.strokeLine(cx, y + 8, cx, y + height - 8);
        gc.setFill(Color.web("#5b6066"));
        for (double ry = y + 12; ry < y + height - 8; ry += 16) {
            gc.fillOval(x + 9, ry, 4, 4);
            gc.fillOval(x + width - 13, ry, 4, 4);
        }

        // gold keyhole
        double kx = cx, ky = y + height / 2;
        gc.setFill(Color.web("#ffe98a", 0.5));
        gc.fillOval(kx - 9, ky - 9, 18, 18);
        gc.setFill(Color.web("#caa000"));
        gc.fillOval(kx - 5, ky - 7, 10, 10);
        gc.fillRect(kx - 2, ky - 1, 4, 9);
    }
}
