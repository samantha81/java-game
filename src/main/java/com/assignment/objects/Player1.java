package com.assignment.objects;

import com.assignment.core.TeamColors;
import javafx.scene.input.KeyCode;

// Player 1: uses the P1 kit colour and the A / D / W control keys.
public class Player1 extends Player {
    public Player1(double x, double y) {
        super(x, y, TeamColors.p1);
    }

    // control keys
    @Override protected KeyCode leftKey()  { return KeyCode.A; }
    @Override protected KeyCode rightKey() { return KeyCode.D; }
    @Override protected KeyCode jumpKey()  { return KeyCode.W; }
}
