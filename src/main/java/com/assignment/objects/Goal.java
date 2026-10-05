package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// The level-exit target; both players must reach it. Portal extends this with a lock + look.
public class Goal extends StaticObject {
    protected Goal(double x, double y, double width, double height) {
        super(x, y, width, height);
    }

    // Reached when an alive player overlaps it.
    public boolean isReachedBy(Player p) {
        return p != null && p.isAlive() && intersects(p);
    }

    // Draw a simple goal marker.
    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(Color.web("#5a3a1f"));
        gc.fillRect(x, y, width, height);

        gc.setStroke(Color.web("#2c1810"));
        gc.setLineWidth(3);
        gc.strokeRect(x, y, width, height);

        gc.setStroke(Color.web("#2c1810"));
        gc.setLineWidth(1);
        gc.strokeLine(x + width / 2, y + 5, x + width / 2, y + height - 5);
        gc.strokeLine(x + 5, y + height / 2, x + width - 5, y + height / 2);

        gc.setFill(Color.GOLD);
        gc.fillOval(x + width - 14, y + height / 2 - 3, 6, 6);

        gc.setFill(Color.WHITE);
        gc.fillText("GOAL", x + 10, y - 5);
    }
}
