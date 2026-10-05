package com.assignment.hazards;

import com.assignment.objects.Player;
import com.assignment.objects.pickups.Shield;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

// A timed horizontal beam: SAFE -> WARNING (flashing) -> FIRE (deadly). Survive a FIRE
// phase by being airborne above the beam line, jumping together off the warning.
public class BlinkBeam extends Hazard {
    private enum Phase { SAFE, WARNING, FIRE }

    private static final Color WARN_COLOR = Color.web("#ff3b30");
    private static final Color FIRE_FILL   = Color.web("#ff2020", 0.9);
    private static final Color FIRE_EDGE   = Color.web("#ffd6d6");

    private static final double HOUSING_W = 22;
    private static final double HOUSING_H = 56;

    private final double safeTime;
    private final double warnTime;
    private final double fireTime;
    private final boolean emitterOnRight;   // which end the beam fires from

    private Phase phase = Phase.SAFE;
    private double timer = 0;

    public BlinkBeam(double x, double y, double width, double height,
                     double safeTime, double warnTime, double fireTime,
                     boolean emitterOnRight) {
        super(x, y, width, height);
        this.safeTime = safeTime;
        this.warnTime = warnTime;
        this.fireTime = fireTime;
        this.emitterOnRight = emitterOnRight;
    }

    // Advance the SAFE -> WARNING -> FIRE cycle.
    @Override
    public void update(double deltaTime) {
        timer += deltaTime;
        switch (phase) {
            case SAFE:    if (timer >= safeTime) { phase = Phase.WARNING; timer = 0; } break;
            case WARNING: if (timer >= warnTime) { phase = Phase.FIRE;    timer = 0; } break;
            case FIRE:    if (timer >= fireTime) { phase = Phase.SAFE;    timer = 0; } break;
        }
    }

    // Kill a player in the beam during FIRE, unless a FRONT shield blocks it.
    @Override
    public void checkPlayer(Player p, List<Shield> activeShields) {
        if (phase != Phase.FIRE || p == null || !p.isAlive()) return;
        if (!intersects(p)) return;
        if (isBlocked(p, activeShields)) return;
        p.kill();
    }

    // A FRONT shield held between the player and the emitter blocks the beam.
    private boolean isBlocked(Player p, List<Shield> activeShields) {
        for (Shield s : activeShields) {
            if (s.getOrientation() != Shield.Orientation.FRONT) continue;
            boolean verticallyOverlaps = s.getY() + s.getHeight() > p.getY()
                                      && s.getY() < p.getY() + p.getHeight();
            if (!verticallyOverlaps) continue;
            if (emitterOnRight) {
                if (s.getX() >= p.getX() + p.getWidth()) return true;
            } else {
                if (s.getX() + s.getWidth() <= p.getX()) return true;
            }
        }
        return false;
    }

    // Draw the warning flash or the firing beam, then the emitter.
    @Override
    public void render(GraphicsContext gc) {
        double beamY = y + height / 2;
        gc.save();

        if (phase == Phase.WARNING) {
            // flashing warning line where the beam will appear
            double pulse = 0.35 + 0.45 * Math.abs(Math.sin(timer * 14));
            gc.setStroke(Color.color(WARN_COLOR.getRed(), WARN_COLOR.getGreen(),
                                     WARN_COLOR.getBlue(), pulse));
            gc.setLineWidth(height);
            gc.setLineDashes(18, 12);
            gc.strokeLine(x, beamY, x + width, beamY);
            gc.setLineDashes(0);
        } else if (phase == Phase.FIRE) {
            // beam runs from the emitter and stops at a FRONT shield in its path
            double left = x, right = x + width;
            for (Shield s : shields) {
                if (s.getOrientation() != Shield.Orientation.FRONT) continue;
                boolean overlapsY = s.getY() + s.getHeight() > y && s.getY() < y + height;
                if (!overlapsY) continue;
                if (emitterOnRight) {
                    double sR = s.getX() + s.getWidth();
                    if (sR > left && sR < right) left = sR;
                } else {
                    double sL = s.getX();
                    if (sL < right && sL > left) right = sL;
                }
            }
            gc.setFill(FIRE_FILL);
            gc.fillRect(left, y, right - left, height);
            gc.setStroke(FIRE_EDGE);
            gc.setLineWidth(2);
            gc.strokeLine(left, beamY, right, beamY);
        }

        renderEmitter(gc, beamY);
        gc.restore();
    }

    // Wall housing the beam fires from; its lens colour tracks the phase.
    private void renderEmitter(GraphicsContext gc, double beamY) {
        double hx = emitterOnRight ? (x + width) : (x - HOUSING_W);
        double hy = beamY - HOUSING_H / 2;

        gc.setFill(Color.web("#3c4048"));
        gc.fillRect(hx, hy, HOUSING_W, HOUSING_H);
        gc.setStroke(Color.web("#202329"));
        gc.setLineWidth(2);
        gc.strokeRect(hx, hy, HOUSING_W, HOUSING_H);

        Color lens;
        if (phase == Phase.FIRE) {
            lens = Color.web("#ff2020");
        } else if (phase == Phase.WARNING) {
            lens = Color.color(1, 0.23, 0.19, 0.4 + 0.5 * Math.abs(Math.sin(timer * 14)));
        } else {
            lens = Color.web("#6e2a2a");
        }
        double lensX = emitterOnRight ? hx + 3 : hx + HOUSING_W - 17;
        gc.setFill(lens);
        gc.fillOval(lensX, beamY - 7, 14, 14);
    }
}
