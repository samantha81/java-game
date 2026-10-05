package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

// A springboard: launches a player straight up far harder than a jump; squashes when it fires.
public class BouncePad extends Platform {
    private final double launchSpeed;   // negative = upward
    private double squash = 0;          // 1 = compressed, decays to 0

    public BouncePad(double x, double y, double width, double height, double launchSpeed) {
        super(x, y, width, height, Color.web("#34c759"));
        this.launchSpeed = launchSpeed;
    }

    // launch speed / fire the squash animation
    public double getLaunchSpeed() { return launchSpeed; }
    public void trigger() { squash = 1; }

    // Ease the squash back out.
    @Override
    public void update(double dt) {
        if (squash > 0) squash = Math.max(0, squash - dt * 4);
    }

    // Draw the base, spring coils, cushion and up-arrow.
    @Override
    public void render(GraphicsContext gc) {
        double comp = squash * 6;            // how far the cushion presses down
        double topY = y + comp;
        double cushionH = Math.max(8, (height - comp) * 0.55);

        // base block
        gc.setFill(Color.web("#444b53"));
        gc.fillRect(x, y + height - 6, width, 7);

        // spring coils
        gc.setStroke(Color.web("#9aa3ad"));
        gc.setLineWidth(3);
        for (double cyl = y + height - 7; cyl > topY + cushionH; cyl -= 6) {
            gc.strokeLine(x + 8, cyl, x + width - 8, cyl - 3);
        }

        // top cushion
        gc.setFill(new LinearGradient(0, topY, 0, topY + cushionH, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#5fe08a")),
                new Stop(1, Color.web("#2fae52"))));
        gc.fillRoundRect(x, topY, width, cushionH, 8, 8);
        gc.setStroke(Color.web("#1f7d3a"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, topY, width, cushionH, 8, 8);

        // up-arrow
        gc.setStroke(Color.web("#ffffff", 0.9));
        gc.setLineWidth(2);
        double ax = x + width / 2;
        gc.strokeLine(ax, topY + 12, ax, topY + 4);
        gc.strokeLine(ax - 5, topY + 9, ax, topY + 4);
        gc.strokeLine(ax + 5, topY + 9, ax, topY + 4);
    }
}
