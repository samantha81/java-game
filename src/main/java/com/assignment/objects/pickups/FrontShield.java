package com.assignment.objects.pickups;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

// A front shield: blocks side (horizontal) beams; rides on the carrier's facing side.
public class FrontShield extends Shield {
    private static final double SHIELD_W = 12;
    private static final double SHIELD_H = 70;
    private static final Color FILL   = Color.web("#bdc3c7");
    private static final Color BORDER = Color.web("#34495e");

    public FrontShield(double x, double y) {
        super(x, y, SHIELD_W, SHIELD_H);
    }

    // blocks horizontal beams
    @Override
    public Orientation getOrientation() {
        return Orientation.FRONT;
    }

    // Stick to the side the carrier faces, centred vertically.
    @Override
    protected void attachToCarrier() {
        if (carrier.getFacing() >= 0) {
            x = carrier.getX() + carrier.getWidth() + 4;
        } else {
            x = carrier.getX() - SHIELD_W - 4;
        }
        y = carrier.getY() + (carrier.getHeight() - SHIELD_H) / 2;
    }

    // Draw the tall slab with an "F" label.
    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(FILL);
        gc.fillRoundRect(x, y, width, height, 4, 4);
        gc.setStroke(BORDER);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, width, height, 4, 4);

        if (!collected) {
            gc.setFill(Color.WHITE);
            gc.fillText("F", x + 2, y + height / 2);
        }
    }
}
