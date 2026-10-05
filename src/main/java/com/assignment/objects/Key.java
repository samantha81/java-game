package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// A collectible key. Either player grabs it by touching it; collecting every key
// in the level unlocks the exit portal.
public class Key extends StaticObject {
    private boolean collected = false;
    private double bob = 0;      // bobbing animation clock

    public Key(double x, double y) {
        super(x, y, 30, 30);
    }

    // collected state
    public boolean isCollected() { return collected; }
    public void collect()        { collected = true; }

    // Advance the bob animation.
    public void update(double deltaTime) {
        bob += deltaTime * 3.0;
    }

    // Draw the bobbing gold key.
    @Override
    public void render(GraphicsContext gc) {
        if (collected) return;

        double yOff = Math.sin(bob) * 3;
        double cx = x + width / 2;
        double cy = y + height / 2 + yOff;

        // soft glow
        gc.setFill(Color.web("#ffe98a", 0.35));
        gc.fillOval(cx - 18, cy - 18, 36, 36);

        gc.save();
        gc.translate(cx, cy);

        Color gold = Color.web("#f5c542");
        Color goldDark = Color.web("#b8860b");

        // ring head
        gc.setStroke(gold);
        gc.setLineWidth(5);
        gc.strokeOval(-11, -12, 14, 14);
        gc.setStroke(goldDark);
        gc.setLineWidth(1.5);
        gc.strokeOval(-11, -12, 14, 14);

        // shaft
        gc.setFill(gold);
        gc.fillRect(-2, -2, 4, 14);
        // teeth
        gc.fillRect(2, 6, 5, 3);
        gc.fillRect(2, 10, 4, 3);
        gc.setStroke(goldDark);
        gc.setLineWidth(1);
        gc.strokeRect(-2, -2, 4, 14);

        gc.restore();
    }
}
