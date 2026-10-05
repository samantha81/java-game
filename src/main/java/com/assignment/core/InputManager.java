package com.assignment.core;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import java.util.EnumSet;
import java.util.Set;

// Tracks which keys are currently held, so the game loop can poll them each frame.
public class InputManager {
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);

    // Start listening for key presses/releases on the scene.
    public void attach(Scene scene) {
        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
    }

    // Is this key currently held?
    public boolean isDown(KeyCode key) {
        return pressedKeys.contains(key);
    }

    // Forget all held keys.
    public void clear() {
        pressedKeys.clear();
    }
}
