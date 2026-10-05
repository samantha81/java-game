package com.assignment.objects;

import javafx.scene.paint.Color;

// Base for platforms that move and carry riders; records this frame's move (getDx/getDy).
public abstract class CarryPlatform extends Platform {
    protected double dx, dy;   // movement applied this frame

    protected CarryPlatform(double x, double y, double width, double height, Color color) {
        super(x, y, width, height, color);
    }

    // Advance one frame (bothRiding = both players aboard, for co-op movers).
    public abstract void advance(double dt, boolean bothRiding);

    // this frame's movement
    public double getDx() { return dx; }
    public double getDy() { return dy; }
}
