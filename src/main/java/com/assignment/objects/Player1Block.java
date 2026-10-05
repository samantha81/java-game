package com.assignment.objects;

import com.assignment.core.TeamColors;
import javafx.scene.paint.Color;

// A pushable block in Player 1's colour — only Player 1 can push it.
public class Player1Block extends ColoredBlock {
    public Player1Block(double x, double y) {
        super(x, y);
    }

    // renders in P1's kit colour
    @Override
    public Color getColor() {
        return TeamColors.p1;
    }

    // only Player 1 may push it
    @Override
    public boolean isPushableBy(Player p) {
        return p instanceof Player1;
    }
}
