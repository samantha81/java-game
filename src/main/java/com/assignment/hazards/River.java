package com.assignment.hazards;

import com.assignment.objects.Player;
import com.assignment.objects.pickups.Shield;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

// Base for a colour-locked river: only the matching player crosses safely; the other drowns.
public abstract class River extends Hazard {
    protected River(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    // which player it kills, and its water colours (set per player by subclasses)
    public abstract boolean killsPlayer(Player p);
    public abstract Color getSurfaceColor();
    public abstract Color getDeepColor();

    // Drown a wrong-colour player that touches the water.
    @Override
    public void checkPlayer(Player p, List<Shield> activeShields) {
        if (p == null || !p.isAlive()) return;
        if (!intersects(p)) return;
        if (killsPlayer(p)) p.kill();
    }

    // Draw the water body, surface line and ripples.
    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(getDeepColor());
        gc.fillRect(x, y, width, height);

        gc.setFill(getSurfaceColor());
        gc.fillRect(x, y, width, 6);

        gc.setStroke(getDeepColor().darker());
        gc.setLineWidth(1);
        for (int wx = 0; wx < width; wx += 24) {
            gc.strokeLine(x + wx, y + 3, x + wx + 12, y + 3);
        }
    }
}
