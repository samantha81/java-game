package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;

// Base for everything in the world: a positioned, sized box that updates and draws.
public abstract class GameObject {
    protected double x;
    protected double y;
    protected double width;
    protected double height;

    public GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Advance one frame.
    public abstract void update(double deltaTime);

    // Draw to the canvas.
    public abstract void render(GraphicsContext gc);

    // AABB overlap test against another object.
    public boolean intersects(GameObject other) {
        return x < other.x + other.width
            && x + width > other.x
            && y < other.y + other.height
            && y + height > other.y;
    }

    // accessors
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
}
