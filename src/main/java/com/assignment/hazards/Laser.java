package com.assignment.hazards;

import com.assignment.objects.Player;
import com.assignment.objects.pickups.Shield;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

// Base for a deadly laser beam with a wall/ceiling emitter; a matching shield blocks it.
public abstract class Laser extends Hazard {
    protected static final Color BEAM_FILL = Color.web("#ff2020", 0.92);
    protected static final Color BEAM_CORE = Color.web("#ffe0e0");

    private static final Color HOUSING_FILL = Color.web("#3c4048");
    private static final Color HOUSING_EDGE = Color.web("#202329");
    private static final Color LENS_COLOR   = Color.web("#ff3b30");
    private static final double HOUSING_DEPTH = 18; // how far the housing juts out
    private static final double HOUSING_SPAN  = 30; // housing length across the beam
    private static final long   START = System.nanoTime();

    protected Laser(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    // For a horizontal beam, which end the emitter sits on (overridden by subclasses).
    protected boolean emitterOnRight() {
        return true;
    }

    // Kill an unprotected player touching the beam.
    @Override
    public void checkPlayer(Player p, List<Shield> activeShields) {
        if (p == null || !p.isAlive()) return;
        if (!intersects(p)) return;
        if (isProtected(p, activeShields)) return;
        p.kill();
    }

    // whether a shield protects the player (per subclass)
    protected abstract boolean isProtected(Player p, List<Shield> activeShields);

    // The still-visible part of the beam after shields clip it (whole beam by default).
    protected double[] visibleBeam() {
        return new double[]{x, y, width, height};
    }

    // Draw the beam body + glowing core, then the emitter.
    @Override
    public void render(GraphicsContext gc) {
        // orientation from the full beam, not the (possibly clipped) visible part
        boolean horizontal = width >= height;

        double[] r = visibleBeam();
        if (r != null && r[2] > 0 && r[3] > 0) {
            double bx = r[0], by = r[1], bw = r[2], bh = r[3];

            // beam body
            gc.setFill(BEAM_FILL);
            gc.fillRect(bx, by, bw, bh);

            // glowing core line
            gc.setStroke(BEAM_CORE);
            gc.setLineWidth(Math.max(2, (horizontal ? bh : bw) * 0.35));
            if (horizontal) {
                double cy = by + bh / 2;
                gc.strokeLine(bx, cy, bx + bw, cy);
            } else {
                double cx = bx + bw / 2;
                gc.strokeLine(cx, by, cx, by + bh);
            }
        }

        // emitter stays put even when a shield blocks the beam, so its source is clear
        renderEmitter(gc, horizontal);
    }

    // A wall/ceiling housing with a pulsing lens on the source side.
    private void renderEmitter(GraphicsContext gc, boolean horizontal) {
        double t = (System.nanoTime() - START) / 1_000_000_000.0;
        double pulse = 0.55 + 0.45 * Math.abs(Math.sin(t * 6));
        Color lens = Color.color(LENS_COLOR.getRed(), LENS_COLOR.getGreen(),
                                 LENS_COLOR.getBlue(), pulse);

        if (horizontal) {
            double beamY = y + height / 2;
            boolean right = emitterOnRight();
            double hx = right ? (x + width) : (x - HOUSING_DEPTH);
            double hy = beamY - HOUSING_SPAN / 2;

            gc.setFill(HOUSING_FILL);
            gc.fillRect(hx, hy, HOUSING_DEPTH, HOUSING_SPAN);
            gc.setStroke(HOUSING_EDGE);
            gc.setLineWidth(2);
            gc.strokeRect(hx, hy, HOUSING_DEPTH, HOUSING_SPAN);

            gc.setFill(lens);
            double lensX = right ? hx + 2 : hx + HOUSING_DEPTH - 12;
            gc.fillOval(lensX, beamY - 5, 10, 10);
        } else {
            // ceiling emitter at the top of the beam
            double beamX = x + width / 2;
            double hx = beamX - HOUSING_SPAN / 2;
            double hy = Math.max(0, y - HOUSING_DEPTH);

            gc.setFill(HOUSING_FILL);
            gc.fillRect(hx, hy, HOUSING_SPAN, HOUSING_DEPTH);
            gc.setStroke(HOUSING_EDGE);
            gc.setLineWidth(2);
            gc.strokeRect(hx, hy, HOUSING_SPAN, HOUSING_DEPTH);

            // lens on the beam-facing side
            gc.setFill(lens);
            gc.fillOval(beamX - 5, hy + HOUSING_DEPTH - 12, 10, 10);
        }
    }
}
