package com.assignment.hazards;

import com.assignment.objects.GameObject;
import com.assignment.objects.Player;
import com.assignment.objects.pickups.Shield;

import java.util.List;

// Base for anything that can kill a player (rivers, lasers, blink beams).
public abstract class Hazard extends GameObject {
    protected List<Shield> shields = List.of();   // active shields this frame, for beam occlusion

    protected Hazard(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    // Give the hazard the shields active this frame.
    public void setShields(List<Shield> shields) {
        this.shields = (shields != null) ? shields : List.of();
    }

    // Kill the player if it's touching and unprotected.
    public abstract void checkPlayer(Player p, List<Shield> activeShields);

    @Override
    public void update(double deltaTime) {
        // default: no animation
    }
}
