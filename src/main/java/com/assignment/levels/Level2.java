package com.assignment.levels;

import com.assignment.hazards.BlinkBeam;
import com.assignment.hazards.Player2River;
import com.assignment.hazards.HorizontalLaser;
import com.assignment.hazards.VerticalLaser;
import com.assignment.hazards.Player1River;
import com.assignment.objects.BouncePad;
import com.assignment.objects.CoopLift;
import com.assignment.objects.Player2Block;
import com.assignment.objects.Player2;
import com.assignment.objects.Key;
import com.assignment.objects.MovingPlatform;
import com.assignment.objects.Platform;
import com.assignment.objects.Portal;
import com.assignment.objects.Player1Block;
import com.assignment.objects.Player1;
import com.assignment.objects.pickups.FrontShield;
import com.assignment.objects.pickups.TopShield;

// Level 2: a laser/shield opening and a co-op-lift + moving-platform crossing, then
// a stack climb, twin rivers, and a laser + blink-beam run to the cup.
public class Level2 extends Level {
    private static final double GROUND_TOP_Y = 540;
    private static final double FLOOR_HEIGHT = 60;
    private static final double BLOCK_SIZE = 48;

    // x-shift applied to the merged-in teammate's Level 2.
    private static final double F = 2900;

    public Level2(double width, double height) {
        super(width, height);
    }

    // Build the stage: platforms, blocks, hazards, pickups, keys, gates and matches.
    @Override
    protected void build() {
        worldWidth = 5800;

        player1 = new Player1(60, GROUND_TOP_Y - 80);
        player2  = new Player2(100, GROUND_TOP_Y - 80);

        // outer walls
        platforms.add(new Platform(0, 0, 20, height));
        platforms.add(new Platform(worldWidth - 20, 0, 20, height));

        // =====================================================================
        // MATCH 1 — Lasers & the Crossing (lasers, shields, then the crossing)
        // =====================================================================
        // ground: a ferry pit at 1000..1200, then the gauntlet chasm after 1900
        platforms.add(new Platform(0, GROUND_TOP_Y, 1000, FLOOR_HEIGHT));
        platforms.add(new Platform(1200, GROUND_TOP_Y, 460, FLOOR_HEIGHT));   // shorter ground: less walking on each side of the co-op lift
        platforms.add(new MovingPlatform(1000, GROUND_TOP_Y, 100, 22,
                1100, GROUND_TOP_Y, 70));

        pickups.add(new TopShield(250, GROUND_TOP_Y - 12));   // wide (90px) shield, clear of Player 2's spawn so it isn't auto-grabbed
        pickups.add(new FrontShield(220, GROUND_TOP_Y - 70));

        hazards.add(new VerticalLaser(720, 0, GROUND_TOP_Y));                 // TOP shield
        hazards.add(new BlinkBeam(540, 505, 380, 8, 1.5, 1.0, 0.35, false)); // jump together

        // the crossing: a CO-OP LIFT (both must board to rise up to the key),
        // then a shorter-travel moving platform to the landing ledge
        platforms.add(new CoopLift(1360, GROUND_TOP_Y, 90, 18, 280, 110));
        platforms.add(new MovingPlatform(1740, 500, 100, 20, 2040, 500, 105)); // carries you most of the way to the bounce pad
        platforms.add(new Platform(2160, 478, 440, 18));                     // bounce-pad ledge
        platforms.add(new BouncePad(2490, 478, 80, 18, -780));               // spring up
        platforms.add(new Platform(2600, 330, 260, 18));                     // exit ledge -> drop right

        // =====================================================================
        // MATCHES 2-3 (teammate's design), shifted by F — kept intact
        //   Match 2 = the stand climb + twin rapids; Match 3 = the final whistle
        // =====================================================================
        // his ground (drop onto it from my exit ledge)
        platforms.add(new Platform(F + 0,   GROUND_TOP_Y, 120, FLOOR_HEIGHT));
        platforms.add(new Platform(F + 140, GROUND_TOP_Y, 200, FLOOR_HEIGHT));
        platforms.add(new Platform(F + 360, GROUND_TOP_Y, 360, FLOOR_HEIGHT));

        // his block-stack climb onto the stand
        platforms.add(new Platform(F + 120, GROUND_TOP_Y - 80, 20, 80 + FLOOR_HEIGHT));
        blocks.add(new Player2Block(F + 180, GROUND_TOP_Y - BLOCK_SIZE));
        platforms.add(new Platform(F + 340, GROUND_TOP_Y - 100, 20, 100 + FLOOR_HEIGHT));
        platforms.add(new Platform(F + 435, GROUND_TOP_Y - 80, 85, 20));
        blocks.add(new Player2Block(F + 440, GROUND_TOP_Y - BLOCK_SIZE - 90));
        blocks.add(new Player1Block(F + 480, GROUND_TOP_Y - BLOCK_SIZE));
        platforms.add(new Platform(F + 720, GROUND_TOP_Y - 180, 2100, 180 + FLOOR_HEIGHT)); // the stand

        // his twin stacked rivers (cross them with the right colour on top)
        hazards.add(new Player1River(F + 830,  GROUND_TOP_Y - 220, 200, 40));   // pushed right: room to land & stack
        hazards.add(new Player2River(F + 830,   GROUND_TOP_Y - 260, 200, 40));
        hazards.add(new Player2River(F + 1160,  GROUND_TOP_Y - 220, 200, 40));   // nudged right to keep the inter-river gap
        hazards.add(new Player1River(F + 1160, GROUND_TOP_Y - 260, 200, 40));

        // his laser run + the key
        pickups.add(new FrontShield(F + 1560, GROUND_TOP_Y - 245));   // clear of the respawn on the left and the laser on the right
        pickups.add(new TopShield(F + 1580, GROUND_TOP_Y - 220));
        hazards.add(new HorizontalLaser(F + 1680, GROUND_TOP_Y - 210, 220, 10, true));
        hazards.add(new VerticalLaser(F + 1800, 0, 360));
        platforms.add(new Platform(F + 1860, GROUND_TOP_Y - 280, 120, 40));
        platforms.add(new Platform(F + 1980, GROUND_TOP_Y - 360, 20, 80));

        // his blink-beam finish, then the portal
        hazards.add(new BlinkBeam(F + 2180, GROUND_TOP_Y - 210, 400, 8, 1.5, 1.0, 0.4, false));
        platforms.add(new Platform(F + 2320, GROUND_TOP_Y - 260, 20, 30));
        platforms.add(new Platform(F + 2480, GROUND_TOP_Y - 260, 20, 30));

        goal = new Portal(F + 2700, GROUND_TOP_Y - 244);   // the Cup
        ((Portal) goal).lock();                            // opens once all keys are collected

        // ---- match keys: one per match, each just before that match's exit gate.
        // Collect them all to open the portal ------------------------------------
        Key k1 = addKey(1405,     240);                 // Match 1 (high up — ride the co-op lift together to reach it)
        Key k2 = addKey(F + 1090, 300);                 // Match 2 (Stand + Rapids — between the two rivers)
        addKey(F + 1880, GROUND_TOP_Y - 310);           // Match 3 (Final Whistle — up on the laser-finish stand)

        // ---- match-boundary energy gates: solid until that match's key is in ---
        addGate(2880,     330,                k1::isCollected);   // -> Match 2 (drop off the exit ledge onto the stand)
        addGate(F + 1420, GROUND_TOP_Y - 180, k2::isCollected);   // -> Match 3 (final whistle, on the stand)

        // ---- matches (checkpointed sections), left to right ------------------
        addMatch("Lasers, then Bounce Across", 0,       60,       GROUND_TOP_Y - 80,
                "Grab both shields. Pass the vertical laser under the TOP shield,\n"
              + "jump the blink-beam together when it blinks off, ride the ferry\n"
              + "platform, then bounce-pad up and across to the high key.");
        addMatch("Climb & Cross the Rivers",  2880,     F + 20,   GROUND_TOP_Y - 80,
                "Stack the blocks up the little pillars onto the stand. Cross the\n"
              + "twin rivers on the one that matches your kit — each player can\n"
              + "only take their own colour. The key sits between the two rivers.");
        addMatch("Lasers to the Cup",         F + 1450, F + 1455, GROUND_TOP_Y - 180 - 80,
                "Grab the FRONT and TOP shields, run the laser pair, then jump\n"
              + "through the blink-beam together on an off-flash. Take the last\n"
              + "key — collect them all to open the Cup portal.");
    }

    @Override
    public String getName() {
        return "Meowhen United ⚽ Level 2";
    }
}
