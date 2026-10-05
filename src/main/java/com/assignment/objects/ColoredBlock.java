package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

// A pushable, gravity-affected block in a player's colour. Only its owner can push it;
// blocks can stack and be carried on a player's head.
public abstract class ColoredBlock extends Entity {
    public static final double GRAVITY = 1200.0;
    public static final double MAX_FALL_SPEED = 700.0;
    public static final double BLOCK_SIZE = 48;

    // when true, a block resting on a head rides along with the carrier (opt-in)
    private boolean carriable = false;

    // the player carrying this block this frame / last frame (so a carried block keeps
    // riding through a jump, but a player can't newly lift one by jumping into it)
    private Player headCarrier;
    private Player prevHeadCarrier;

    private final double originX, originY;   // start spot, restored on a match restart

    protected ColoredBlock(double x, double y) {
        super(x, y, BLOCK_SIZE, BLOCK_SIZE);
        this.originX = x;
        this.originY = y;
    }

    // mark this block carriable (rides a head)
    public void setCarriable(boolean carriable) { this.carriable = carriable; }

    // Snap the block back to its start spot and clear motion/carry state.
    public void reset() {
        x = originX;
        y = originY;
        velocityY = 0;
        onGround = false;
        headCarrier = null;
        prevHeadCarrier = null;
    }

    // colour and which player may push it (per subclass)
    public abstract Color getColor();
    public abstract boolean isPushableBy(Player p);

    // Apply gravity for the frame.
    @Override
    public void update(double deltaTime) {
        prevHeadCarrier = headCarrier;   // remember last frame's carrier
        headCarrier = null;              // re-established below if still carried
        velocityY += GRAVITY * deltaTime;
        if (velocityY > MAX_FALL_SPEED) velocityY = MAX_FALL_SPEED;
        y += velocityY * deltaTime;
    }

    // Land on / stop against platforms (min-overlap push-out).
    public void resolveAgainstPlatforms(List<Platform> platforms) {
        onGround = false;
        for (Platform p : platforms) {
            if (!intersects(p)) continue;

            double overlapLeft   = (x + width) - p.getX();
            double overlapRight  = (p.getX() + p.getWidth()) - x;
            double overlapTop    = (y + height) - p.getY();
            double overlapBottom = (p.getY() + p.getHeight()) - y;

            double minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
                                         Math.min(overlapTop, overlapBottom));

            if (minOverlap == overlapTop) {
                y = p.getY() - height;
                velocityY = 0;
                onGround = true;
            } else if (minOverlap == overlapBottom) {
                y = p.getY() + p.getHeight();
                velocityY = 0;
            } else if (minOverlap == overlapLeft) {
                x = p.getX() - width;
            } else if (minOverlap == overlapRight) {
                x = p.getX() + p.getWidth();
            }
        }
    }

    // Stacking: a falling block rests on top of a block below it (never pushes it).
    public void resolveAgainstBlock(ColoredBlock other) {
        if (other == this || !intersects(other)) return;
        if (velocityY >= 0 && y < other.getY()) {
            y = other.getY() - height; // rest on top of the lower block
            velocityY = 0;
            onGround = true;
        }
    }

    // A falling block settles on a player's head instead of clipping through them.
    public void restOnPlayer(Player p, double deltaTime) {
        if (p == null || !p.isAlive() || !intersects(p)) return;
        // only settle onto a head coming DOWN onto a player who isn't rising into it,
        // unless this player was already carrying it (so a carried block rides up on a jump)
        boolean alreadyCarried = (prevHeadCarrier == p);
        if (!alreadyCarried && p.getVelocityY() < 0) return;
        if (velocityY >= 0 && y + height <= p.getY() + p.getHeight() * 0.5) {
            y = p.getY() - height;
            velocityY = 0;
            onGround = true;
            headCarrier = p;
            // a carriable block rides along with the head; ordinary blocks stay put
            if (carriable) {
                x += p.getVelocityX() * deltaTime;
            }
        }
    }

    // Draw the coloured block with an outline and highlight lines.
    @Override
    public void render(GraphicsContext gc) {
        Color base = getColor();
        gc.setFill(base);
        gc.fillRect(x, y, width, height);

        gc.setStroke(base.darker().darker());
        gc.setLineWidth(3);
        gc.strokeRect(x + 1, y + 1, width - 2, height - 2);

        gc.setStroke(base.darker());
        gc.setLineWidth(1);
        gc.strokeLine(x + 8, y + 8, x + width - 8, y + 8);
        gc.strokeLine(x + 8, y + height - 8, x + width - 8, y + height - 8);
    }
}
