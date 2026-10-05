package com.assignment.objects;

import com.assignment.core.TeamColors;
import javafx.scene.input.KeyCode;

// Player 2: uses the P2 kit colour and the arrow-key controls.
public class Player2 extends Player {
    public Player2(double x, double y) {
        super(x, y, TeamColors.p2);
    }

    // control keys
    @Override protected KeyCode leftKey()  { return KeyCode.LEFT; }
    @Override protected KeyCode rightKey() { return KeyCode.RIGHT; }
    @Override protected KeyCode jumpKey()  { return KeyCode.UP; }
}
