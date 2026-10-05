package com.assignment.objects.pickups;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// A top shield: blocks falling (vertical) beams; rides above the carrier's head.
public class TopShield extends Shield {
    private static final double SHIELD_W = 90;
    private static final double SHIELD_H = 12;
    private static final Color FILL   = Color.web("#bdc3c7");
    private static final Color BORDER = Color.web("#34495e");

    public TopShield(double x, double y) {
        super(x, y, SHIELD_W, SHIELD_H);
    }

    // blocks vertical beams
    @Override
    public Orientation getOrientation() {
        return Orientation.TOP;
    }

    // Sit above the carrier's head, centred horizontally.
    @Override
    protected void attachToCarrier() {
        x = carrier.getX() + (carrier.getWidth() - SHIELD_W) / 2;
        y = carrier.getY() - SHIELD_H - 6;
    }

    // Draw the wide slab with a "TOP" label.
    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(FILL);
        gc.fillRoundRect(x, y, width, height, 4, 4);
        gc.setStroke(BORDER);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, width, height, 4, 4);

        if (!collected) {
            gc.setFill(Color.WHITE);
            gc.setLineWidth(1);
            gc.fillText("TOP", x + 12, y + 8);
        }
    }
}
