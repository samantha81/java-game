package com.assignment.levels;

import com.assignment.hazards.Player2River;
import com.assignment.hazards.HorizontalLaser;
import com.assignment.hazards.VerticalLaser;
import com.assignment.hazards.Player1River;
import com.assignment.objects.Player2Block;
import com.assignment.objects.Player2;
import com.assignment.objects.Key;
import com.assignment.objects.Platform;
import com.assignment.objects.Portal;
import com.assignment.objects.Player1Block;
import com.assignment.objects.Player1;
import com.assignment.objects.pickups.FrontShield;
import com.assignment.objects.pickups.TopShield;

// Level 1: a block-stacking wall puzzle, a rivers-and-stack climb, and a laser
// gauntlet that river-hops to the portal. Collect every match's key to unlock it.
public class Level1 extends Level {
    private static final double GROUND_TOP_Y = 540;
    private static final double FLOOR_HEIGHT = 60;
    private static final double BLOCK_SIZE = 48;

    // x-shift applied to the second half of the stage.
    private static final double F = 1660;

    public Level1(double width, double height) {
        super(width, height);
    }

    // Build the stage: platforms, blocks, hazards, pickups, keys, gates and matches.
    @Override
    protected void build() {
        worldWidth = 3560;

        player1 = new Player1(60, GROUND_TOP_Y - 80);
        player2  = new Player2(100, GROUND_TOP_Y - 80);

        // outer walls
        platforms.add(new Platform(0, 0, 20, height));
        platforms.add(new Platform(worldWidth - 20, 0, 20, height));

        // =====================================================================
        // MATCH 1 — Clear the Wall (block-stack puzzle)
        // =====================================================================
        platforms.add(new Platform(0, GROUND_TOP_Y, 1600, FLOOR_HEIGHT));   // first-half ground

        platforms.add(new Platform(900, 370, 120, 170));                    // too-tall wall
        Player1Block p1Block = new Player1Block(500, GROUND_TOP_Y - BLOCK_SIZE); // pushed to the wall
        blocks.add(p1Block);
        platforms.add(new Platform(900, 220, 460, 20));                     // bridge over wall
        Player2Block p2Block = new Player2Block(1150, 220 - BLOCK_SIZE);        // drop onto P1's block
        blocks.add(p2Block);

        platforms.add(new Platform(1080, 460, 120, 20));                    // climb steps
        platforms.add(new Platform(1230, 380, 120, 20));
        platforms.add(new Platform(1380, 300, 120, 20));

        // =====================================================================
        // MATCH 2 — Rapids & the Stand (cross the rivers, then stack up the climb)
        // =====================================================================
        platforms.add(new Platform(1600, GROUND_TOP_Y, 500, FLOOR_HEIGHT)); // rapids ground 1600..2100 (abuts the first-half ground)
        hazards.add(new Player1River(F + 120, GROUND_TOP_Y - 40, 200, 40));
        platforms.add(new Platform(F + 120, GROUND_TOP_Y - 100, 200, 60));
        hazards.add(new Player2River(F + 120, GROUND_TOP_Y - 140, 200, 40));

        // ----- Match 2 cont. — Climb the Stand (block-stack onto the plateau) -----
        platforms.add(new Platform(F + 440, GROUND_TOP_Y - 100, 1000, 100 + FLOOR_HEIGHT)); // raised plateau 2980..3980
        blocks.add(new Player1Block(F + 360, GROUND_TOP_Y - BLOCK_SIZE));
        blocks.add(new Player2Block(F + 500, GROUND_TOP_Y - BLOCK_SIZE - 120));
        platforms.add(new Platform(F + 640, GROUND_TOP_Y - 280, 200, 40));

        // =====================================================================
        // MATCH 3 — Lasers & Final Whistle (laser gauntlet, then river-hop to the cup)
        // =====================================================================
        pickups.add(new FrontShield(F + 1080, GROUND_TOP_Y - 165));   // clear of the respawn on the left and the laser on the right
        pickups.add(new TopShield(F + 1100, GROUND_TOP_Y - 140));
        hazards.add(new HorizontalLaser(F + 1200, GROUND_TOP_Y - 130, 80, 10, true));
        hazards.add(new VerticalLaser(F + 1400, 0, 440));

        // ----- Match 3 cont. — drop off the plateau, river-hop to the portal -----
        platforms.add(new Platform(F + 1440, GROUND_TOP_Y, 210, FLOOR_HEIGHT)); // ground 3980..4190
        hazards.add(new Player2River(F + 1440, GROUND_TOP_Y - 100, 50, 20));
        platforms.add(new Platform(F + 1490, GROUND_TOP_Y - 100, 110, 20));
        hazards.add(new Player1River(F + 1600, GROUND_TOP_Y - 100, 50, 20));
        platforms.add(new Platform(F + 1650, GROUND_TOP_Y - 100, 130, 100 + FLOOR_HEIGHT));

        goal = new Portal(F + 1500, GROUND_TOP_Y - 64);                    // the Cup
        ((Portal) goal).lock();                                            // opens once all keys are collected

        // ---- match keys: one per match, each sitting just before that match's
        // exit gate, so you must grab it to pass. Collect them all to open the
        // portal -----------------------------------------------------------------
        Key k1 = addKey(1240, 190);                 // Match 1 (up on the bridge, just right of the stacked block)
        Key k2 = addKey(F + 700, GROUND_TOP_Y - 330); // Match 2 (up on the high climb ledge)
        addKey(F + 1460, GROUND_TOP_Y - 150);       // Match 3 (raised above the door, by the laser gap)

        // ---- match-boundary energy gates: each stays a solid wall until that
        // match's key is collected, so a skipped key walls you out of the next
        // match until you go back for it -----------------------------------------
        addGate(1540, GROUND_TOP_Y,       k1::isCollected);   // -> Match 2 (the rapids)
        addGate(2620, GROUND_TOP_Y - 100, k2::isCollected);   // -> Match 3 (the laser run, on the plateau)

        // ---- matches (checkpointed sections), left to right ------------------
        addMatch("Stack to Clear the Wall",  0,        60,       GROUND_TOP_Y - 80,
                "The wall is too tall to jump. Player 1 pushes their block to the\n"
              + "base of the wall. Player 2 crosses the bridge above and drops\n"
              + "their block on top of Player 1's to make a 2-high step, then\n"
              + "both climb up and grab the key.\n"
              + "(Each block is coloured like its owner — only that player pushes it.)");
        addMatch("Cross the Rivers & Climb", 1660,     1640,     GROUND_TOP_Y - 80,
                "Each river is coloured like one player's kit, and only that\n"
              + "player can cross it — the other drowns. Take your own river to\n"
              + "the plateau, then stack the blocks to climb onto the high ledge\n"
              + "and grab the key.");
        addMatch("Dodge the Lasers",         F + 960,  F + 980,  GROUND_TOP_Y - 180,
                "Grab the FRONT shield (blocks side lasers) and the TOP shield\n"
              + "(blocks the falling laser). Pass both, drop off the plateau, then\n"
              + "river-hop to the key — each player hops only the river that\n"
              + "matches their own kit colour.");
    }

    @Override
    public String getName() {
        return "Meowhen United ⚽ Level 1";
    }
}
