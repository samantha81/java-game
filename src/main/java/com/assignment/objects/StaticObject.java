package com.assignment.objects;

// A non-moving object: its update() is a no-op.
public abstract class StaticObject extends GameObject {
    public StaticObject(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    @Override
    public void update(double deltaTime) {
        // static objects don't move
    }
}
