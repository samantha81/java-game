package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

import java.util.function.BooleanSupplier;

// An energy-field barrier at a match boundary: solid until its key/football is
// collected (isOpen()), then the level removes it.
public class Gate extends Platform {
    private static final long START = System.nanoTime();
    private final BooleanSupplier openWhen;        // true once the gate's collectible is taken
    // visible field spans fieldTopY..fieldBottomY (collision still covers the full bounds)
    private final double fieldTopY;
    private final double fieldBottomY;

    public Gate(double x, double y, double width, double height,
                double fieldTopY, double fieldBottomY, BooleanSupplier openWhen) {
        super(x, y, width, height, Color.web("#5aa0ff")); // colour unused; render overridden
        this.fieldTopY = fieldTopY;
        this.fieldBottomY = fieldBottomY;
        this.openWhen = openWhen;
    }

    // True once the required collectible is collected.
    public boolean isOpen() {
        return openWhen != null && openWhen.getAsBoolean();
    }

    // contact reaction: the field flares where a player pushes against it
    private boolean contacting = false;
    private double contactY = 0;
    private boolean contactOnRight = false;   // player on the gate's right side?

    // Record that a player is pressed flush against the gate this frame.
    public void setContact(double y, boolean playerOnRight) {
        contacting = true;
        contactY = y;
        contactOnRight = playerOnRight;
    }

    // Clear the push contact.
    public void clearContact() {
        contacting = false;
    }

    private static final Color HAZE = Color.web("#efe9da");
    private static final Color WISP = Color.web("#fdfaf2");

    // Draw the translucent force field (flowing wisps), plus a flare where a player pushes.
    @Override
    public void render(GraphicsContext gc) {
        double top = fieldTopY;
        double bottom = fieldBottomY;     // stop at the surface, not the screen floor
        double h = bottom - top;
        if (h <= 0) return;
        double t = (System.nanoTime() - START) / 1_000_000_000.0;
        double pulse = 0.5 + 0.5 * Math.sin(t * 1.4);

        // render the field a touch wider than the 16px collision so the flowing
        // wisps are visible
        double bandW = width + 30;
        double bx = x + width / 2 - bandW / 2;

        gc.save();
        gc.beginPath();
        gc.rect(bx, top, bandW, h);
        gc.clip();

        // faint base haze so the strip isn't empty between wisps
        gc.setFill(new LinearGradient(0, top, 0, bottom, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#efe9da", 0.0)),
                new Stop(0.16, HAZE.deriveColor(0, 1, 1, 0.10)),
                new Stop(0.84, HAZE.deriveColor(0, 1, 1, 0.10)),
                new Stop(1.00, Color.web("#efe9da", 0.0))));
        gc.fillRect(bx, top, bandW, h);

        // flowing caustic filaments: vertical strands whose x weaves with layered
        // sines, scrolling upward over time, with patchy brightness so tendrils
        // appear and dissolve
        gc.setLineCap(StrokeLineCap.ROUND);
        int strands = 6;
        int steps = 16;
        for (int s = 0; s < strands; s++) {
            double f = (strands == 1) ? 0.5 : s / (double) (strands - 1);
            double baseX = bx + bandW * (0.12 + 0.76 * f);
            double ph = s * 1.7;
            double amp = bandW * (0.16 + 0.07 * Math.sin(s * 2.3));
            double freq1 = 0.013 + 0.004 * (s % 3);
            double freq2 = 0.027 + 0.005 * ((s + 1) % 4);
            double speed = 20 + 7 * (s % 3);           // upward drift speed

            double prevX = 0, prevY = 0;
            for (int i = 0; i <= steps; i++) {
                double yy = top + h * i / steps;
                double flowY = yy + t * speed;          // scroll the waveform upward
                double dx = amp * Math.sin(flowY * freq1 + ph)
                          + amp * 0.5 * Math.sin(flowY * freq2 - ph * 1.3 + t * 0.7);
                double px = baseX + dx;
                if (i > 0) {
                    // brightness varies in patches along the strand -> wispy look
                    double patch = 0.5 + 0.5 * Math.sin(flowY * 0.03 + s * 1.9 + t * 1.1);
                    double a = (0.05 + 0.16 * patch) * (0.7 + 0.3 * pulse);
                    gc.setStroke(WISP.deriveColor(0, 1, 1, a));
                    gc.setLineWidth(1.0 + 1.0 * patch);
                    gc.strokeLine(prevX, prevY, px, yy);
                }
                prevX = px;
                prevY = yy;
            }
        }

        gc.restore();

        // contact reaction: a bright flare + expanding ripples where a player pushes
        if (contacting) {
            double edgeX = contactOnRight ? x + width : x;
            double cy = Math.max(top + 8, Math.min(bottom - 8, contactY));
            double dir = contactOnRight ? 1 : -1;     // ripples spill toward the player

            // hot core glow at the push point
            gc.setFill(Color.web("#fffdf6", 0.45 + 0.25 * pulse));
            gc.fillOval(edgeX - 9, cy - 13, 18, 26);
            gc.setFill(Color.web("#ffe9bf", 0.20));
            gc.fillOval(edgeX - 18, cy - 24, 36, 48);

            // a few rings rippling outward from the impact, fading as they grow
            for (int r = 0; r < 3; r++) {
                double rp = (t * 1.7 + r * 0.34) % 1.0;     // 0..1 life
                double rad = 5 + rp * 20;
                double a = (1 - rp) * 0.40;
                gc.setStroke(Color.web("#fff6e6", a));
                gc.setLineWidth(2);
                gc.strokeOval(edgeX + dir * rp * 8 - rad, cy - rad, rad * 2, rad * 2);
            }
        }
    }
}
