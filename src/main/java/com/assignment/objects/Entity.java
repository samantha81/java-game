package com.assignment.objects;

// A moving object: adds velocity, an alive flag, and ground state.
public abstract class Entity extends GameObject {
    protected double velocityX;
    protected double velocityY;
    protected boolean alive;
    protected boolean onGround;

    public Entity(double x, double y, double width, double height) {
        super(x, y, width, height);
        this.velocityX = 0;
        this.velocityY = 0;
        this.alive = true;
        this.onGround = false;
    }

    // accessors
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public boolean isAlive() { return alive; }
    public boolean isOnGround() { return onGround; }

    public void setVelocityX(double vx) { this.velocityX = vx; }
    public void setVelocityY(double vy) { this.velocityY = vy; }
    public void setOnGround(boolean onGround) { this.onGround = onGround; }

    // Mark this entity dead (intangible until it respawns).
    public void kill() {
        this.alive = false;
    }
}
