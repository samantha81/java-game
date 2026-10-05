package com.assignment.objects;

import com.assignment.core.TeamColors;
import javafx.scene.paint.Color;

// A pushable block in Player 2's colour — only Player 2 can push it.
public class Player2Block extends ColoredBlock {
    public Player2Block(double x, double y) {
        super(x, y);
    }

    // renders in P2's kit colour
    @Override
    public Color getColor() {
        return TeamColors.p2;
    }

    // only Player 2 may push it
    @Override
    public boolean isPushableBy(Player p) {
        return p instanceof Player2;
    }
}
