package com.assignment.levels;

import com.assignment.hazards.Player2River;
import com.assignment.hazards.HorizontalLaser;
import com.assignment.hazards.VerticalLaser;
import com.assignment.hazards.Player1River;
import com.assignment.objects.Ball;
import com.assignment.objects.Player2Block;
import com.assignment.objects.Player2;
import com.assignment.objects.Platform;
import com.assignment.objects.Portal;
import com.assignment.objects.Player1Block;
import com.assignment.objects.Player1;
import com.assignment.objects.pickups.FrontShield;
import com.assignment.objects.pickups.TopShield;

// Level 3: a stair climb, a flappy gauntlet, size-pad and carry puzzles, a laser
// corridor, and a tower climb to the Cup. Collect every football to open it.
public class Level3 extends Level {
    private static final double GROUND_TOP_Y = 540;
    private static final double FLOOR_HEIGHT = 60;
    private static final double BLOCK_SIZE = 48;

    // stand-climb / early-ground origin (match 1).
    private static final double A = 0;
    // corridor + tower origin — shifted right to open room for the flappy match,
    //  the size-pad match, AND the carry-&-strike match that follow it.
    private static final double F = 4100;

    public Level3(double width, double height) {
        super(width, height);
    }

    // Build the stage: platforms, blocks, hazards, pickups, footballs, gates and matches.
    @Override
    protected void build() {
        worldWidth = 7400;

        player1 = new Player1(60, GROUND_TOP_Y - 80);
        player2  = new Player2(100, GROUND_TOP_Y - 80);

        // outer walls
        platforms.add(new Platform(0, 0, 20, height));
        platforms.add(new Platform(worldWidth - 20, 0, 20, height));

        // =====================================================================
        // THE LEVEL (matches 1-3): stair climb, penalty corridor, climb to the cup
        // =====================================================================
        // ground: stand-climb start, then the corridor/tower ground further right
        platforms.add(new Platform(A + 0,    GROUND_TOP_Y, 980,  FLOOR_HEIGHT)); // stand-climb start
        platforms.add(new Platform(A + 1360, GROUND_TOP_Y, 440,  FLOOR_HEIGHT)); // up to the flappy launch
        platforms.add(new Platform(F + 2100, GROUND_TOP_Y, 1100, FLOOR_HEIGHT)); // under the (shifted) corridor/tower

        // =====================================================================
        // MATCH 1 — Climb the Stand (push-block stair climb)
        // =====================================================================
        // Player 2's block: push it to the base of the stairs to step up onto the wall
        blocks.add(new Player2Block(A + 440, GROUND_TOP_Y - BLOCK_SIZE));

        // ascending stair climb
        platforms.add(new Platform(A + 980,  GROUND_TOP_Y - 100, 20, 100 + FLOOR_HEIGHT));
        platforms.add(new Platform(A + 1040, GROUND_TOP_Y - 140, 80, 140 + FLOOR_HEIGHT));
        platforms.add(new Platform(A + 1160, GROUND_TOP_Y - 180, 80, 180 + FLOOR_HEIGHT));
        platforms.add(new Platform(A + 1280, GROUND_TOP_Y - 220, 80, 220 + FLOOR_HEIGHT));
        platforms.add(new Platform(A + 1400, GROUND_TOP_Y - 260, 60, 170));
        blocks.add(new Player1Block(A + 1440, GROUND_TOP_Y - BLOCK_SIZE - 260));
        platforms.add(new Platform(A + 1520, GROUND_TOP_Y - 300, 60, 250)); // trimmed: widen the drop channel so the 48px block fits
        platforms.add(new Platform(A + 1640, GROUND_TOP_Y - 340, 60, 80)); // short: underside at y280 clears the jump
        blocks.add(new Player2Block(A + 1696, GROUND_TOP_Y - BLOCK_SIZE - 340));
        // inverse-L (⅃): a grassy foot + a grassy pillar to the ceiling. The
        // pillar's top is pushed above the screen so its grass cap is hidden.
        platforms.add(new Platform(A + 1680, GROUND_TOP_Y - 340, 100, 80)); // foot (short: underside at y280)
        platforms.add(new Platform(A + 1780, -16, 20, 336));                 // pillar (cap hidden off-screen)

        // =====================================================================
        // MATCH 2 — Flap Through the Gaps (co-op Flappy Bird)
        //   Two players flap through split-gap pipes over a pit: tap jump to flap
        //   (works mid-air here), hold right to fly forward. Each pipe has a TOP
        //   lane and a BOTTOM lane, so the pair splits up and weaves through. One
        //   death restarts the pair, so BOTH must clear every pipe together.
        // =====================================================================
        platforms.add(new Platform(1800, GROUND_TOP_Y - 180, 280, 30));      // launch ledge off the stand
        // (no visible ceiling bar; a top clamp in the flap zone stops fly-overs)
        // flap is live across the gauntlet; the auto-scroll fly run carries both
        // players forward together once the leader clears the ledge (x 2080) and
        // ends when the trailing player glides out onto the size-puzzle floor (x 3340)
        addFlapZone(1820, 3360, 2080, 3340);
        flappyPipe(2280, 150, 410);   // (x, top-gap centre, bottom-gap centre)
        flappyPipe(2510, 190, 450);
        flappyPipe(2740, 150, 400);
        flappyPipe(2970, 200, 440);
        flappyPipe(3170, 160, 420);
        // no floor under the pipes — a missed flap drops into the pit (= restart)

        // =====================================================================
        // MATCH 3 — Size Up, Size Down (Pico Park +/- pads, then a co-op boost)
        //   Beat 1 — SHRINK (red -) small to crawl under the low wall (both players).
        //   Beat 2 — at a GROW (green +) pad, grow one player into a step: the still
        //            -small partner climbs its head up onto the high ledge to grab
        //            the football (small needs big to get up).
        // =====================================================================
        platforms.add(new Platform(3290, GROUND_TOP_Y, 2610, FLOOR_HEIGHT));  // size + carry floor 3290..5900

        // beat 1 — shrink, then crawl under the low wall (only a 30px floor gap)
        addSizePad(3760, GROUND_TOP_Y, 80, -1);   // SHRINK pad
        platforms.add(new Platform(3900, -16, 150, (GROUND_TOP_Y - 30) - (-16)));

        // beat 2 — boost to the football. Stay small from the crawl, then GROW your
        // partner into a step: the small player climbs the grown player's head up onto
        // the high ledge (top y390, too high for ANY solo jump) to grab the football.
        addSizePad(4180, GROUND_TOP_Y, 80, +1);   // GROW pad (make the booster)
        platforms.add(new Platform(4420, GROUND_TOP_Y - 150, 150, 20));   // ledge holding the football

        // =====================================================================
        // MATCH 4 — Carry & Strike (co-op tower + double launch)
        //   1) drop the carriable block onto the bottom player's head,
        //   2) the top player climbs onto the block,
        //   3) the bottom player jumps (lifting block + rider),
        //   4) at the top of the lift the rider ALSO jumps off the block to strike
        //      the high football (placed in the gates block below, at y235).
        // =====================================================================
        platforms.add(new Platform(5220, 490, 100, 16));                   // block-staging shelf
        Player1Block carryBlock = new Player1Block(5230, 490 - BLOCK_SIZE);  // sits on the shelf
        carryBlock.setCarriable(true);
        blocks.add(carryBlock);

        // steps up from the carry floor onto the (shifted) corridor stand
        platforms.add(new Platform(5660, GROUND_TOP_Y - 60,  100, 20));
        platforms.add(new Platform(5780, GROUND_TOP_Y - 120, 120, 20));   // joins the corridor stand

        // his penalty-laser corridor
        platforms.add(new Platform(F + 1800, GROUND_TOP_Y - 180, 300, 180 + FLOOR_HEIGHT));
        pickups.add(new FrontShield(F + 1960, GROUND_TOP_Y - 245));
        pickups.add(new TopShield(F + 2000, GROUND_TOP_Y - 220));
        hazards.add(new HorizontalLaser(F + 2180, GROUND_TOP_Y - 30, 60, 10, true));
        platforms.add(new Platform(F + 2220, GROUND_TOP_Y - 80, 200, 40));
        hazards.add(new VerticalLaser(F + 2240, 0, GROUND_TOP_Y));
        hazards.add(new HorizontalLaser(F + 2260, GROUND_TOP_Y - 30, 60, 10, true));
        platforms.add(new Platform(F + 2300, GROUND_TOP_Y - 80, 200, 40));
        hazards.add(new VerticalLaser(F + 2320, 0, GROUND_TOP_Y));
        hazards.add(new HorizontalLaser(F + 2340, GROUND_TOP_Y - 30, 60, 10, true));
        platforms.add(new Platform(F + 2380, GROUND_TOP_Y - 80, 200, 40));
        hazards.add(new VerticalLaser(F + 2400, 0, GROUND_TOP_Y));
        hazards.add(new HorizontalLaser(F + 2420, GROUND_TOP_Y - 30, 60, 10, true));
        platforms.add(new Platform(F + 2460, GROUND_TOP_Y - 80, 200, 40));
        hazards.add(new VerticalLaser(F + 2480, 0, GROUND_TOP_Y));
        hazards.add(new HorizontalLaser(F + 2500, GROUND_TOP_Y - 30, 60, 10, true));
        platforms.add(new Platform(F + 2540, GROUND_TOP_Y - 80, 200, 40));
        hazards.add(new VerticalLaser(F + 2560, 0, GROUND_TOP_Y));
        hazards.add(new HorizontalLaser(F + 2580, GROUND_TOP_Y - 30, 60, 10, true));
        platforms.add(new Platform(F + 2620, GROUND_TOP_Y - 80, 200, 40));
        hazards.add(new VerticalLaser(F + 2640, 0, GROUND_TOP_Y));
        // drop-hole here (x2820..2880) so the upper player can come down off the
        // walkway (or jump across to the tower)

        // his vertical climb to the cup
        platforms.add(new Platform(F + 2880, GROUND_TOP_Y - 80, 100, 20));
        hazards.add(new Player2River(F + 3000, GROUND_TOP_Y - 80, 40, 20));
        platforms.add(new Platform(F + 2920, GROUND_TOP_Y - 160, 100, 20));
        platforms.add(new Platform(F + 2880, GROUND_TOP_Y - 240, 100, 20));
        platforms.add(new Platform(F + 2920, GROUND_TOP_Y - 320, 100, 20));
        platforms.add(new Platform(F + 2880, GROUND_TOP_Y - 400, 100, 20));
        platforms.add(new Platform(F + 2920, GROUND_TOP_Y - 460, 100, 20));
        platforms.add(new Platform(F + 2860, 20, 20, 460));
        platforms.add(new Platform(F + 3020, 20, 20, 480));
        hazards.add(new Player1River(F + 3020, GROUND_TOP_Y - 40, 20, 40));
        blocks.add(new Player1Block(F + 3060, GROUND_TOP_Y - BLOCK_SIZE));    // P1 step up to the first right-column ledge
        platforms.add(new Platform(F + 3040, GROUND_TOP_Y - 100, 100, 20));
        platforms.add(new Platform(F + 3080, GROUND_TOP_Y - 180, 100, 20));
        platforms.add(new Platform(F + 3140, GROUND_TOP_Y - 240, 20, 20));
        platforms.add(new Platform(F + 3160, GROUND_TOP_Y - 300, 20, 20));
        platforms.add(new Platform(F + 3040, GROUND_TOP_Y - 360, 100, 20));
        platforms.add(new Platform(F + 3080, GROUND_TOP_Y - 420, 60, 20));   // helper step: breaks the big jump up to the top
        platforms.add(new Platform(F + 3060, GROUND_TOP_Y - 480, 20, 20));
        platforms.add(new Platform(F + 3120, GROUND_TOP_Y - 480, 100, 20));   // summit (widened so both players fit under the cup)

        goal = new Portal(F + 3138, 0);                    // the Cup, at the uppermost part — right above the tower summit
        ((Portal) goal).lock();                            // opens once all footballs are collected — then win!

        // ---- match footballs: one per match, each just before that match's exit
        // gate. Collect every football to unlock the final portal and win --------
        Ball b1 = addBall(1748,     170);                 // Match 1 (top of the climb, just right of the upper block)
        Ball bF = addBall(2430,     380);                 // Match 2 (Flappy — mid-flight, bottom lane)
        Ball bS = addBall(4470,     360);                 // Match 3 (Size — on the high ledge; boost a partner up to it)
        Ball bC = addBall(5050,     235);                 // Match 4 (Carry & Strike — high; needs the double launch)
        Ball b2 = addBall(F + 2700, GROUND_TOP_Y - 110);  // Match 5 (right end of the corridor walkway)
        addBall(F + 3070, GROUND_TOP_Y - 130);            // Match 6 (on the first right-column ledge)

        // ---- match-boundary energy gates: solid until that match's football is in
        addGate(1800,     GROUND_TOP_Y - 180, b1::isCollected);   // -> Match 2 (the flappy gauntlet)
        addGate(3270,     GROUND_TOP_Y - 180, bF::isCollected);   // -> Match 3 (the size pads)
        addGate(4760,     GROUND_TOP_Y,       bS::isCollected);   // -> Match 4 (carry & strike)
        addGate(5570,     GROUND_TOP_Y,       bC::isCollected);   // -> Match 5 (penalty corridor)
        addGate(F + 2840, GROUND_TOP_Y,       b2::isCollected);   // -> Match 6 (the tower)

        // ---- matches (checkpointed sections), left to right ------------------
        addMatch("Climb the Stand",            A + 20,   A + 40,   GROUND_TOP_Y - 80,
                "Push the blocks to the foot of the stairs and climb the steps.\n"
              + "Use the upper block as a final step to reach the football at\n"
              + "the top of the climb. (Each block is pushed only by the player\n"
              + "whose kit colour it matches.)");
        addMatch("Flap Through the Gaps",      1840,     1900,     GROUND_TOP_Y - 220,
                "Tap JUMP to flap (it works mid-air). Once you leave the ledge\n"
              + "you both auto-fly forward together — you only steer up/down.\n"
              + "Split the lanes: one takes the top gap, one the bottom. One\n"
              + "death restarts the pair, so clear every pipe together.");
        addMatch("Size Up, Size Down",         3300,     3360,     GROUND_TOP_Y - 80,
                "Both players step on the − (shrink) pad to go small and crawl\n"
              + "under the low wall. Stay small, then one player steps on the\n"
              + "+ (grow) pad to become a tall step — the small partner climbs\n"
              + "onto their head and up to the high ledge for the football.");
        addMatch("Carry & Strike",             4850,     4900,     GROUND_TOP_Y - 80,
                "Pick up the carriable block and drop it on the lower player's\n"
              + "head. The top player climbs onto the block; the lower one jumps\n"
              + "to lift them, and at the peak the rider jumps off too — a double\n"
              + "launch — to strike the high football.");
        addMatch("Laser Corridor",             F + 1800, F + 1850, GROUND_TOP_Y - 180 - 80,
                "Grab the FRONT and TOP shields at the entrance, then leapfrog\n"
              + "the walkway platforms. The front shield eats the side lasers,\n"
              + "the top shield the falling ones. Grab the football at the end,\n"
              + "then drop through the hole.");
        addMatch("Lift the Cup",               F + 2820, F + 2900, GROUND_TOP_Y - 80,
                "Climb the zig-zag tower, crossing each small river with the\n"
              + "player whose kit colour matches it (the other would drown). Use\n"
              + "the block to start the climb and the helper step near the top.\n"
              + "With every football collected, reach the Cup portal at the top —\n"
              + "it opens into the penalty shoot-out.");
    }

    // One Flappy-Bird pipe: a top gap and a bottom gap (two lanes to split
    //  between) over the pit. topGapC/botGapC are the gap y-centres.
    //  Built from the same grassy dirt platforms as the rest of the level, so the
    //  gauntlet stays visually consistent.
    private void flappyPipe(double x, double topGapC, double botGapC) {
        double w = 46, gap = 130, ceil = -16, bottom = 640;  // pipe tops run off-screen (no green caps showing)
        platforms.add(new Platform(x, ceil, w, (topGapC - gap / 2) - ceil));
        platforms.add(new Platform(x, topGapC + gap / 2,
                w, (botGapC - gap / 2) - (topGapC + gap / 2)));
        platforms.add(new Platform(x, botGapC + gap / 2, w, bottom - (botGapC + gap / 2)));
    }

    @Override
    public String getName() {
        return "Meowhen United ⚽ Level 3";
    }
}
