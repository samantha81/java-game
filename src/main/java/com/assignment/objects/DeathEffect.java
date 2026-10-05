package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// The death->respawn particle animation in a player's colour: burst at the death spot,
// hold, then streak back to the spawn and converge. Each particle follows a curved path.
public class DeathEffect {
    private static final Random RNG = new Random();
    public static final double DURATION = 1.25;        // total burst-and-return time, seconds
    // timeline fractions: quick explode, a held pause, then the streak home
    private static final double EXPLODE_FRAC = 0.14;
    private static final double HOLD_FRAC = 0.22;      // death-moment pause
    private static final double RING_FRACTION = 0.30;  // shockwave "poof" lasts this share

    // one particle: origin (death) -> control (burst) -> target (spawn)
    private static final class Particle {
        double ox, oy;
        double bx, by;
        double tx, ty;
        double size;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Color color;
    private final double originX, originY;
    private double time = 0;

    // Build the particles: burst out from (fromX,fromY) and return to (toX,toY).
    public DeathEffect(double fromX, double fromY, double toX, double toY, Color color) {
        this.originX = fromX;
        this.originY = fromY;
        this.color = color;
        for (int i = 0; i < 28; i++) {
            Particle p = new Particle();
            p.size = 4 + RNG.nextDouble() * 6;
            p.ox = fromX;
            p.oy = fromY;
            double ang = RNG.nextDouble() * Math.PI * 2;
            double r = 45 + RNG.nextDouble() * 55;         // how far it explodes out
            p.bx = fromX + Math.cos(ang) * r;
            p.by = fromY + Math.sin(ang) * r - 30;         // slight upward bias
            double sr = RNG.nextDouble() * 16;             // scatter around the spawn
            double sa = RNG.nextDouble() * Math.PI * 2;
            p.tx = toX + Math.cos(sa) * sr;
            p.ty = toY + Math.sin(sa) * sr;
            particles.add(p);
        }
    }

    // Advance the animation clock.
    public void update(double dt) {
        time += dt;
    }

    // How far along the "fly home" leg (0..1): 0 during explode+hold, then smoothsteps
    // to 1. The camera uses this to linger on the burst, then glide home.
    public static double returnProgress(double f) {
        double start = EXPLODE_FRAC + HOLD_FRAC;
        if (f <= start) return 0;
        double k = (f - start) / (1 - start);
        return k * k * (3 - 2 * k);                     // smoothstep
    }

    // Draw the shockwave ring and the moving particles for the current time.
    public void render(GraphicsContext gc) {
        double f = Math.min(1, time / DURATION);

        // shockwave ring at the death spot
        if (f < RING_FRACTION) {
            double rt = f / RING_FRACTION;
            double r = 55 * rt;
            gc.setStroke(Color.color(color.getRed(), color.getGreen(), color.getBlue(),
                    (1 - rt) * 0.7));
            gc.setLineWidth(4 * (1 - rt) + 1);
            gc.strokeOval(originX - r, originY - r, r * 2, r * 2);
        }

        // explode out, hold, then return
        double ep = (f < EXPLODE_FRAC) ? f / EXPLODE_FRAC : 1;
        ep = 1 - (1 - ep) * (1 - ep);                  // ease-out
        double rp = returnProgress(f);

        for (Particle p : particles) {
            double exX = p.ox + (p.bx - p.ox) * ep;    // exploded-out position
            double exY = p.oy + (p.by - p.oy) * ep;
            double x = exX + (p.tx - exX) * rp;        // then streak to target
            double y = exY + (p.ty - exY) * rp;
            double a = (f > 0.85) ? (1 - f) / 0.15 : 1;  // fade out as they arrive
            gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(),
                    Math.max(0, a)));
            gc.fillOval(x - p.size / 2, y - p.size / 2, p.size, p.size);
        }
    }
}
