package com.assignment.hazards;

import com.assignment.core.TeamColors;
import com.assignment.objects.Player2;
import com.assignment.objects.Player;
import javafx.scene.paint.Color;

// Player 1's river: safe for P1, drowns P2; drawn in P1's water colours.
public class Player1River extends River {
    public Player1River(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    // kills Player 2 (the wrong colour)
    @Override
    public boolean killsPlayer(Player p) {
        return p instanceof Player2;
    }

    // P1 water colours
    @Override
    public Color getSurfaceColor() { return TeamColors.p1Surface(); }

    @Override
    public Color getDeepColor()    { return TeamColors.p1Deep(); }
}
