package com.assignment;

import javafx.application.Application;

// Entry point: boots the JavaFX app (separate class so the module launches cleanly).
public class Launcher {
    public static void main(String[] args) {
        Application.launch(GameApp.class, args);
    }
}
