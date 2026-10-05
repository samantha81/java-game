package com.assignment.objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

// A solid surface: grassy dirt by default, or a plain colour block (walls/ceilings).
public class Platform extends StaticObject {
    private static final Color DIRT_TOP = Color.web("#9c6b3f");
    private static final Color DIRT_BOTTOM = Color.web("#5a3a1f");
    private static final Color GRASS = Color.web("#5fcf6f");
    private static final Color GRASS_DARK = Color.web("#3da34d");

    private final Color color;     // set = plain colour block; null = grassy
    private final boolean grassy;

    // grassy dirt platform
    public Platform(double x, double y, double width, double height) {
        super(x, y, width, height);
        this.color = null;
        this.grassy = true;
    }

    // plain colour block
    public Platform(double x, double y, double width, double height, Color color) {
        super(x, y, width, height);
        this.color = color;
        this.grassy = false;
    }

    // Draw either the plain block or the grassy dirt look.
    @Override
    public void render(GraphicsContext gc) {
        if (!grassy) {
            gc.setFill(color);
            gc.fillRect(x, y, width, height);
            gc.setStroke(Color.web("#222222"));
            gc.setLineWidth(1);
            gc.strokeRect(x, y, width, height);
            return;
        }

        // dirt body
        gc.setFill(new LinearGradient(0, y, 0, y + height, false, CycleMethod.NO_CYCLE,
                new Stop(0, DIRT_TOP), new Stop(1, DIRT_BOTTOM)));
        gc.fillRect(x, y, width, height);

        // dirt speckles
        gc.setFill(Color.web("#caa06f", 0.5));
        for (double sx = x + 12; sx < x + width - 6; sx += 46) {
            gc.fillOval(sx, y + 16, 4, 4);
            gc.fillOval(sx + 22, y + 30, 3, 3);
        }

        // grass cap on top
        double cap = Math.min(10, height);
        gc.setFill(GRASS);
        gc.fillRect(x, y, width, cap);
        // bumpy grass edge
        gc.setFill(GRASS_DARK);
        for (double gx = x; gx < x + width; gx += 12) {
            gc.fillOval(gx, y + cap - 4, 12, 7);
        }
        gc.setFill(GRASS);
        for (double gx = x + 2; gx < x + width; gx += 12) {
            gc.fillOval(gx, y - 2, 9, 6);
        }
    }
}
