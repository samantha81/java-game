package com.assignment.objects.pickups;

import com.assignment.objects.Player;
import com.assignment.objects.StaticObject;

// Base for collectible items a player can carry (the shields); remembers its start
// spot so it can be restored when the players respawn.
public abstract class Pickup extends StaticObject {
    protected boolean collected = false;
    protected Player carrier;
    private final double originX, originY;   // start spot, restored on respawn

    protected Pickup(double x, double y, double w, double h) {
        super(x, y, w, h);
        this.originX = x;
        this.originY = y;
    }

    // state / start position
    public boolean isCollected() { return collected; }
    public double getOriginX()   { return originX; }

    // Pick up onto a player.
    public void pickUp(Player p) {
        this.collected = true;
        this.carrier = p;
    }

    // Let go.
    public void drop() {
        this.collected = false;
        this.carrier = null;
    }

    // Return to the start spot, uncollected (on respawn).
    public void reset() {
        this.collected = false;
        this.carrier = null;
        this.x = originX;
        this.y = originY;
    }
}
