package com.assignment.hazards;

import com.assignment.core.TeamColors;
import com.assignment.objects.Player1;
import com.assignment.objects.Player;
import javafx.scene.paint.Color;

// Player 2's river: safe for P2, drowns P1; drawn in P2's water colours.
public class Player2River extends River {
    public Player2River(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    // kills Player 1 (the wrong colour)
    @Override
    public boolean killsPlayer(Player p) {
        return p instanceof Player1;
    }

    // P2 water colours
    @Override
    public Color getSurfaceColor() { return TeamColors.p2Surface(); }

    @Override
    public Color getDeepColor()    { return TeamColors.p2Deep(); }
}
