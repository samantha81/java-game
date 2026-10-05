package com.assignment.levels;

import com.assignment.core.InputManager;
import com.assignment.hazards.Hazard;
import com.assignment.objects.Ball;
import com.assignment.objects.BouncePad;
import com.assignment.objects.CarryPlatform;
import com.assignment.objects.ColoredBlock;
import com.assignment.objects.GameObject;
import com.assignment.objects.Gate;
import com.assignment.objects.Goal;
import com.assignment.objects.Key;
import com.assignment.objects.MatchFlag;
import com.assignment.objects.Platform;
import com.assignment.objects.Player;
import com.assignment.objects.Portal;
import com.assignment.objects.SizePad;
import com.assignment.objects.pickups.Shield;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class Level {
    protected final List<Platform> platforms = new ArrayList<>();
    protected final List<ColoredBlock> blocks = new ArrayList<>();
    protected final List<Shield> pickups = new ArrayList<>();
    protected final List<Hazard> hazards = new ArrayList<>();
    protected final List<GameObject> decorations = new ArrayList<>();
    protected Player player1;
    protected Player player2;
    protected Goal goal;

    // Reusable scratch buffers, refilled each frame, so the update loop doesn't
    //  allocate (and churn the GC) every tick.
    private final List<Shield> activeShields = new ArrayList<>();
    private final Player[] bothPlayers = new Player[2];

    // "Flap zones": x-ranges where the jump key becomes a repeatable mid-air
    //  flap (Flappy-Bird style). A level registers them in build(). Each entry is
    //  {xStart, xEnd, autoStart, autoEnd}; autoStart < 0 means the zone has no
    //  auto-scroll.
    private final List<double[]> flapZones = new ArrayList<>();
    // A flap zone with an auto-scroll "fly run": once the LEADING player's centre
    //  passes autoStart (i.e. it launches off the ledge over the pit), both
    //  players are carried forward together at a constant speed until the TRAILING
    //  player reaches autoEnd (they touch down on the far side).
    protected void addFlapZone(double xStart, double xEnd, double autoStart, double autoEnd) {
        flapZones.add(new double[]{xStart, xEnd, autoStart, autoEnd});
    }
    private boolean inFlapZone(Player p) {
        if (p == null) return false;
        double cx = p.getX() + p.getWidth() / 2;
        for (double[] z : flapZones) if (cx >= z[0] && cx <= z[1]) return true;
        return false;
    }

    // Auto-scroll "fly run" state. While active, both players are pushed forward at
    //  FLY_SCROLL_SPEED and their left/right input is ignored, so the pair
    //  scrolls through the pipes together and can never separate horizontally
    //  (Flappy-Bird style: you only steer up/down).
    private boolean flyRunActive = false;
    private double flyAutoEnd = 0;
    private static final double FLY_SCROLL_SPEED = 150;

    // Engages/disengages the auto-scroll fly run from the players' positions.
    private void updateFlyRun() {
        boolean bothAlive = player1 != null && player2 != null
                && player1.isAlive() && player2.isAlive();
        if (!bothAlive) { flyRunActive = false; return; }
        double yc = player1.getX() + player1.getWidth() / 2;
        double gc = player2.getX()  + player2.getWidth()  / 2;
        double leading  = Math.max(yc, gc);
        double trailing = Math.min(yc, gc);
        for (double[] z : flapZones) {
            if (z[2] < 0) continue;                       // zone has no auto-scroll
            double autoStart = z[2], autoEnd = z[3];
            if (!flyRunActive) {
                // the moment the leader commits past the launch point, the run is on
                if (leading >= autoStart && leading <= autoEnd) {
                    flyRunActive = true;
                    flyAutoEnd = autoEnd;
                }
            } else if (trailing >= flyAutoEnd) {
                flyRunActive = false;                     // BOTH reached the far side
            }
            return;
        }
        flyRunActive = false;
    }

    // True while p should still be auto-carried forward. A player stops the
    //  instant IT personally reaches the far side (lands in the next match), even
    //  if its partner is still flying -- so a landed player isn't shoved onward.
    private boolean flyingNow(Player p) {
        if (!flyRunActive || p == null || !p.isAlive()) return false;
        return p.getX() + p.getWidth() / 2 < flyAutoEnd;
    }

    // Size pads (Pico Park-style +/- buttons): a player standing on one grows or
    //  shrinks while it stays. Registered by a level in build().
    protected final List<SizePad> sizePads = new ArrayList<>();
    // Scale change per second while standing on a pad.
    private static final double SIZE_RATE = 1.3;

    protected SizePad addSizePad(double x, double surfaceY, double width, int sign) {
        SizePad pad = new SizePad(x, surfaceY, width, sign);
        sizePads.add(pad);
        return pad;
    }

    // Grows/shrinks a player that is standing on a size pad this frame.
    private void applySizePads(Player p, double dt) {
        if (p == null || !p.isAlive() || !p.isOnGround()) return;
        double feet = p.getY() + p.getHeight();
        double pl = p.getX(), pr = p.getX() + p.getWidth();
        for (SizePad pad : sizePads) {
            if (Math.abs(feet - pad.getSurfaceY()) > 8) continue;       // not resting on it
            boolean xOverlap = pr > pad.getX() + 2 && pl < pad.getX() + pad.getWidth() - 2;
            if (xOverlap) p.resizeBy(pad.getSign() * SIZE_RATE * dt);
        }
    }

    // Portal-unlock collectibles. A level uses ONE kind: keys (Levels 1 & 2) or
    //  footballs (Level 3). Collecting every one unlocks the level's Portal.
    //  These do NOT affect respawn — checkpoints are armed by entering matches.
    protected final List<Ball> balls = new ArrayList<>();
    protected final List<Key>  keys  = new ArrayList<>();

    // World Cup "matches": ordered sections of the stage, each with a checkpoint
    //  at its start. Crossing into a match moves both players' respawn point there,
    //  so a death restarts the current match rather than the whole stage.
    protected final List<Match> matches = new ArrayList<>();
    private int currentMatch = 0;
    private static final double MATCH_BANNER_TIME = 2.6;
    private double matchBannerTimer = 0;
    protected final double width;
    protected final double height;
    // Total level size in world units. Defaults to one screen (no scrolling);
    //  a level can set worldWidth larger in build() to enable horizontal scroll.
    protected double worldWidth;
    protected double cameraX = 0;
    private boolean completed = false;
    private boolean failed = false;
    // Time spent playing this level; stops counting once cleared or failed.
    private double elapsedSeconds = 0;
    // The player currently pressed against a closed gate (null if none); drives the
    //  "grab the key/football" prompt.
    private Player blockedPlayer = null;

    protected Level(double width, double height) {
        this.width = width;
        this.height = height;
        this.worldWidth = width;
        build();
    }

    protected abstract void build();
    public abstract String getName();

    public void update(double dt, InputManager input) {
        if (completed) return;
        elapsedSeconds += dt;

        // detect a head-stacked pair up-front, while positions are still last
        // frame's resting state -- so it stays reliable even once the base starts
        // to rise on a jump (by which point the head-ride test would miss)
        Player stackTop = null, stackBase = null;
        if (player1 != null && player2 != null
                && player1.isAlive() && player2.isAlive()) {
            if (isHeadRiding(player1, player2)) { stackTop = player1; stackBase = player2; }
            else if (isHeadRiding(player2, player1)) { stackTop = player2; stackBase = player1; }
        }
        double baseStartX = (stackBase != null) ? stackBase.getX() : 0;

        // move any platforms first and drag along whoever is standing on them
        updateDynamicPlatforms(dt);

        // flap zones: the jump key becomes a repeatable mid-air flap inside them
        if (player1 != null) player1.setFlapMode(inFlapZone(player1));
        if (player2  != null) player2.setFlapMode(inFlapZone(player2));

        // auto-scroll fly run: once engaged, both players are carried forward at the
        // same constant speed and steer only up/down, so they fly as a locked pair
        updateFlyRun();

        if (player1 != null && player1.isAlive()) {
            player1.handleInput(input);
            if (flyingNow(player1)) player1.setVelocityX(FLY_SCROLL_SPEED); // forward only
            player1.update(dt);
        }
        if (player2 != null && player2.isAlive()) {
            player2.handleInput(input);
            if (flyingNow(player2)) player2.setVelocityX(FLY_SCROLL_SPEED);  // forward only
            player2.update(dt);
        }

        // dead players run their death->respawn animation, then come back
        if (player1 != null && !player1.isAlive()) player1.updateRespawn(dt);
        if (player2  != null && !player2.isAlive())  player2.updateRespawn(dt);

        for (ColoredBlock b : blocks) b.update(dt);
        for (ColoredBlock b : blocks) b.resolveAgainstPlatforms(platforms);
        for (ColoredBlock b : blocks) {
            for (ColoredBlock o : blocks) b.resolveAgainstBlock(o);
        }
        for (ColoredBlock b : blocks) {
            b.restOnPlayer(player1, dt);
            b.restOnPlayer(player2, dt);
        }

        if (player1 != null && player1.isAlive()) handlePushing(player1);
        if (player2  != null && player2.isAlive())  handlePushing(player2);

        if (player1 != null && player1.isAlive()) {
            player1.resolveCollisions(platforms);
            for (ColoredBlock b : blocks) player1.resolveAgainstBlock(b);
            applyBouncePads(player1);
        }
        if (player2 != null && player2.isAlive()) {
            player2.resolveCollisions(platforms);
            for (ColoredBlock b : blocks) player2.resolveAgainstBlock(b);
            applyBouncePads(player2);
        }

        if (player1 != null && player2 != null
                && player1.isAlive() && player2.isAlive()) {
            resolveStackedPlayers(stackTop, stackBase, baseStartX);
        }

        // size pads: grow/shrink a player standing on a +/- button
        applySizePads(player1, dt);
        applySizePads(player2, dt);

        for (Shield s : pickups) {
            if (!s.isCollected()) {
                if (player1 != null && player1.canPickUp(s) && player1.intersects(s)) {
                    player1.pickUp(s);
                } else if (player2 != null && player2.canPickUp(s) && player2.intersects(s)) {
                    player2.pickUp(s);
                }
            }
            s.update(dt);
        }

        activeShields.clear();
        for (Shield s : pickups) {
            if (s.isCollected()) activeShields.add(s);
        }
        for (Hazard h : hazards) {
            h.update(dt);
            h.setShields(activeShields);
            if (player1 != null) h.checkPlayer(player1, activeShields);
            if (player2  != null) h.checkPlayer(player2, activeShields);
        }

        // portal-unlock collectibles: keys (Levels 1 & 2) or footballs (Level 3).
        // collect every one to open the level's portal.
        for (Key k : keys) {
            k.update(dt);
            if (!k.isCollected()
                    && ((player1 != null && player1.isAlive() && player1.intersects(k))
                     || (player2  != null && player2.isAlive()  && player2.intersects(k)))) {
                k.collect();
            }
        }
        for (Ball b : balls) {
            b.update(dt);
            if (!b.isCollected()
                    && ((player1 != null && player1.isAlive() && player1.intersects(b))
                     || (player2  != null && player2.isAlive()  && player2.intersects(b)))) {
                b.collect();
            }
        }
        // once all of this level's collectibles are in, the portal opens
        if (goal instanceof Portal p && p.isLocked()
                && (keys.size() + balls.size()) > 0
                && getKeysCollected()  == keys.size()
                && getBallsCollected() == balls.size()) {
            p.unlock();
        }

        // light up a closed gate wherever a player is pressed flush against it, so
        // the field visibly reacts to a blocked push; also remember that player so we
        // can prompt it to grab the key/football
        Player pressing = null;
        bothPlayers[0] = player1;
        bothPlayers[1] = player2;
        for (Platform plat : platforms) {
            if (!(plat instanceof Gate g) || g.isOpen()) continue;
            Player toucher = null;
            boolean onRight = false;
            for (Player player : bothPlayers) {
                if (player == null || !player.isAlive()) continue;
                double br = player.getX() + player.getWidth();
                double gl = g.getX(), gr = g.getX() + g.getWidth();
                boolean leftSide  = player.getX() < gl && Math.abs(br - gl) <= 4;
                boolean rightSide = br > gr && Math.abs(player.getX() - gr) <= 4;
                if (leftSide || rightSide) {
                    toucher = player;
                    onRight = rightSide;
                    break;
                }
            }
            if (toucher != null) {
                g.setContact(toucher.getY() + toucher.getHeight() / 2, onRight);
                pressing = toucher;
            } else {
                g.clearContact();
            }
        }
        blockedPlayer = pressing;

        // a match-boundary gate powers down once its collectible is taken, so a
        // skipped key/football leaves the players physically walled out of the next
        // match until they go back for it
        platforms.removeIf(p -> p instanceof Gate g && g.isOpen());

        if (goal != null) goal.update(dt);

        if (player1 != null && player1.isAlive()) clampToBounds(player1);
        if (player2 != null && player2.isAlive())  clampToBounds(player2);

        updateMatchProgress(dt);

        // co-op rule: when one player dies, both reset together from their
        // original spawns (so a section is always retried as a pair)
        boolean someoneDown = (player1 != null && !player1.isAlive())
                           || (player2  != null && !player2.isAlive());
        if (someoneDown) {
            if (player1 != null && player1.isAlive()) player1.kill();
            if (player2  != null && player2.isAlive())  player2.kill();
            // give the shields back: drop whatever the players were carrying and
            // return every shield to its pickup spot, so the section's lasers
            // can be re-crossed after respawning
            if (player1 != null) player1.clearShield();
            if (player2  != null) player2.clearShield();
            for (Shield s : pickups) s.reset();
            // restore every block to its start spot too, so a botched block
            // puzzle is restocked exactly as it began
            for (ColoredBlock b : blocks) b.reset();
        }

        updateCamera(dt);

        checkObjectives();
    }

    // Advances each moving/carry platform and bounce pad. For carry platforms,
    // riders are detected from their resting position BEFORE the platform moves,
    // then shifted by the platform's delta so they stay glued on top.
    private void updateDynamicPlatforms(double dt) {
        for (Platform p : platforms) {
            if (p instanceof CarryPlatform cp) {
                boolean yRiding = isRiding(player1, cp);
                boolean gRiding = isRiding(player2, cp);
                cp.advance(dt, yRiding && gRiding);
                if (yRiding) {
                    player1.setX(player1.getX() + cp.getDx());
                    player1.setY(player1.getY() + cp.getDy());
                }
                if (gRiding) {
                    player2.setX(player2.getX() + cp.getDx());
                    player2.setY(player2.getY() + cp.getDy());
                }
            } else if (p instanceof BouncePad bp) {
                bp.update(dt);
            }
        }
    }

    // Extra launch multiplier the base gives the player stacked on its head.
    private static final double STACK_BOUNCE_BOOST = 1.25;

    // Launches a grounded player off a bounce pad. If another player is stacked on
    // its head, that top player gets a much stronger boost (a co-op double-bounce),
    // so only a stack flings someone high enough to reach a far-up key.
    private void applyBouncePads(Player p) {
        if (p == null || !p.isAlive() || !p.isOnGround()) return;
        for (Platform plat : platforms) {
            if (!(plat instanceof BouncePad bp)) continue;
            if (!isRiding(p, bp)) continue;
            bp.trigger();
            Player top = topRiderOf(p);
            if (top != null) {
                top.setVelocityY(bp.getLaunchSpeed() * STACK_BOUNCE_BOOST);
                top.setOnGround(false);
            }
            p.setVelocityY(bp.getLaunchSpeed());
            p.setOnGround(false);
            break;
        }
    }

    // The other player, if it's currently standing on this one's head; else null.
    private Player topRiderOf(Player base) {
        Player other = (base == player1) ? player2 : player1;
        if (other == null || !other.isAlive()) return null;
        return isHeadRiding(other, base) ? other : null;
    }

    // True when top is standing on (and overlapping) base's head.
    private boolean isHeadRiding(Player top, Player base) {
        double feet = top.getY() + top.getHeight();
        boolean onTop = feet >= base.getY() - 4 && feet <= base.getY() + 8;
        boolean xOverlap = top.getX() + top.getWidth() > base.getX() + 1
                        && top.getX() < base.getX() + base.getWidth() - 1;
        return onTop && xOverlap;
    }

    // Keeps a head-stacked pair behaving as one tower. While the rider sits on
    // the base's head it follows the base up (so the base can jump while carrying
    // it), down, and sideways. The rider is released the instant it jumps off
    // (rising faster than the base). When the two are merely side by side they
    // just push apart.
    private void resolveStackedPlayers(Player top, Player base, double baseStartX) {
        if (top != null && top.isAlive() && base.isAlive()) {
            boolean ridingAway = top.getVelocityY() < base.getVelocityY() - 1;
            if (!ridingAway) {
                top.setX(top.getX() + (base.getX() - baseStartX)); // follow sideways
                top.setY(base.getY() - top.getHeight());           // stay on the head
                top.setVelocityY(base.getVelocityY());             // rise/fall as one
                top.setOnGround(base.isOnGround());                // so it can jump off
                separateRiderFromPlatforms(top);                   // never clip a wall/ledge
                return;
            }
        }
        // side by side (or the rider is leaving): just keep them from overlapping
        player1.resolveAgainstPlayer(player2);
        player2.resolveAgainstPlayer(player1);
    }

    // The rider is glued straight onto the base's head above, which ignores level
    // geometry -- so a tower tall enough to reach a wall or ledge the base walks
    // under would clip into it. This shoves the rider back out of any platform it
    // now overlaps (position only, so the jump-off state set just above survives).
    private void separateRiderFromPlatforms(Player p) {
        for (Platform plat : platforms) {
            if (!p.intersects(plat)) continue;
            double ol = (p.getX() + p.getWidth()) - plat.getX();
            double or = (plat.getX() + plat.getWidth()) - p.getX();
            double ot = (p.getY() + p.getHeight()) - plat.getY();
            double ob = (plat.getY() + plat.getHeight()) - p.getY();
            double m = Math.min(Math.min(ol, or), Math.min(ot, ob));
            if (m == ot)      p.setY(plat.getY() - p.getHeight());
            else if (m == ob) p.setY(plat.getY() + plat.getHeight());
            else if (m == ol) p.setX(plat.getX() - p.getWidth());
            else              p.setX(plat.getX() + plat.getWidth());
        }
    }

    // True when the player is standing on top of (and overlapping) the platform.
    private boolean isRiding(Player p, Platform plat) {
        if (p == null || !p.isAlive()) return false;
        double feet = p.getY() + p.getHeight();
        boolean onTop = feet >= plat.getY() - 4 && feet <= plat.getY() + 6;
        boolean xOverlap = p.getX() + p.getWidth() > plat.getX() + 1
                        && p.getX() < plat.getX() + plat.getWidth() - 1;
        return onTop && xOverlap;
    }

    private void handlePushing(Player player) {
        // a block can only be shoved by walking into it on the ground. We also
        // reject the push while the player is rising (velocityY < 0): the on-ground
        // flag is a frame stale, so on the very first jump frame it still reads
        // true -- without this a player could spam-jump into a block and nudge it
        // sideways with its head. Grounded walking has velocityY >= 0 (gravity),
        // so legitimate pushes are unaffected.
        if (!player.isOnGround() || player.getVelocityY() < 0) return;
        for (ColoredBlock block : blocks) {
            if (!block.isPushableBy(player)) continue;
            if (!player.intersects(block)) continue;

            double overlapLeft   = (player.getX() + player.getWidth()) - block.getX();
            double overlapRight  = (block.getX() + block.getWidth()) - player.getX();
            double overlapTop    = (player.getY() + player.getHeight()) - block.getY();
            double overlapBottom = (block.getY() + block.getHeight()) - player.getY();

            double minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
                                         Math.min(overlapTop, overlapBottom));

            if (minOverlap == overlapLeft && player.getVelocityX() > 0) {
                attemptBlockPush(block, overlapLeft);
            } else if (minOverlap == overlapRight && player.getVelocityX() < 0) {
                attemptBlockPush(block, -overlapRight);
            }
        }
    }

    private void attemptBlockPush(ColoredBlock block, double dx) {
        // gather the pushed block plus everything stacked on top of it, so pushing
        // a base block slides its whole tower along instead of sliding out from
        // under it (the upper blocks would otherwise be left behind and topple)
        List<ColoredBlock> group = new ArrayList<>();
        collectStack(block, group);

        for (ColoredBlock b : group) b.setX(b.getX() + dx);

        // if any moved block now hits a wall, an outside block, or a player that
        // can't be pushed through, the whole shove is illegal -- revert it all
        if (stackBlocked(group)) {
            for (ColoredBlock b : group) b.setX(b.getX() - dx);
        }
    }

    // Adds base plus every block resting (transitively) on top of it.
    private void collectStack(ColoredBlock base, List<ColoredBlock> out) {
        if (out.contains(base)) return;
        out.add(base);
        for (ColoredBlock other : blocks) {
            if (other == base || out.contains(other)) continue;
            if (restsOnBlock(other, base)) collectStack(other, out);
        }
    }

    // True when upper is sitting directly on top of lower
    //  (even a small overlap counts, so an offset stack still travels together).
    private boolean restsOnBlock(ColoredBlock upper, ColoredBlock lower) {
        boolean onTop = Math.abs((upper.getY() + upper.getHeight()) - lower.getY()) < 1.5;
        boolean xOverlap = upper.getX() + upper.getWidth() > lower.getX() + 0.5
                        && upper.getX() < lower.getX() + lower.getWidth() - 0.5;
        return onTop && xOverlap;
    }

    // True if any block in the moved group overlaps a wall, a block outside the
    //  group, or a player that can't push it.
    private boolean stackBlocked(List<ColoredBlock> group) {
        for (ColoredBlock b : group) {
            for (Platform p : platforms) {
                if (b.intersects(p)) return true;
            }
            for (ColoredBlock other : blocks) {
                if (group.contains(other)) continue;
                if (b.intersects(other)) return true;
            }
            if (player1 != null && !b.isPushableBy(player1) && b.intersects(player1)) return true;
            if (player2 != null && !b.isPushableBy(player2) && b.intersects(player2)) return true;
        }
        return false;
    }

    // How far from the left screen edge the trailing player is kept.
    private static final double CAMERA_LEFT_MARGIN = 150;
    // How quickly the view glides toward the respawn point (higher = faster).
    private static final double CAMERA_FOLLOW_RATE = 12;

    // Screen-locked camera (Pico Park style): the view follows the TRAILING
    // (leftmost) player, so it only advances when the player who is behind moves
    // forward. If one player races ahead while the other stays put, the screen
    // stays and the leader is stopped at the right edge until the other catches
    // up. With worldWidth == width this stays at 0 and nothing scrolls.
    protected void updateCamera(double dt) {
        if (worldWidth <= width) { cameraX = 0; return; }
        boolean anyoneAlive = (player1 != null && player1.isAlive())
                           || (player2  != null && player2.isAlive());
        // normally follow the trailing ALIVE player; while both are respawning
        // follow their current spots so the view glides to where they re-form
        double leftmost = Double.MAX_VALUE;
        if (player1 != null && (player1.isAlive() || !anyoneAlive))
            leftmost = Math.min(leftmost, player1.getFocusX());
        if (player2 != null && (player2.isAlive() || !anyoneAlive))
            leftmost = Math.min(leftmost, player2.getFocusX());
        if (leftmost == Double.MAX_VALUE) return;
        double target = leftmost - CAMERA_LEFT_MARGIN;
        if (target < 0) target = 0;
        if (target > worldWidth - width) target = worldWidth - width;
        if (anyoneAlive) {
            cameraX = target;                       // screen-locked: snap as before
        } else {
            // both respawning: ease smoothly toward the respawn point
            cameraX += (target - cameraX) * (1 - Math.exp(-CAMERA_FOLLOW_RATE * dt));
        }
    }

    protected void clampToBounds(Player p) {
        double left = cameraX;
        double right = cameraX + width;
        if (p.getX() < left) p.setX(left);
        if (p.getX() + p.getWidth() > right) p.setX(right - p.getWidth());
        if (p.getY() > height) p.kill();
        // a flap zone has no visible ceiling now -- keep players from flapping up off
        // the top of the screen and sailing over the pipes
        if (p.isFlapMode() && p.getY() < 0) { p.setY(0); p.setVelocityY(0); }
    }

    protected void checkObjectives() {
        if (goal != null
                && goal.isReachedBy(player1)
                && goal.isReachedBy(player2)) {
            completed = true;
        }
    }

    // ------------------------------------------------------- matches (World Cup)

    // One section of a stage. startX is the world-x where it begins;
    //  spawnX/spawnY is the (Player 1) checkpoint the pair re-forms at.
    protected static final class Match {
        final String name;
        final double startX, spawnX, spawnY;
        // Multi-line "how to clear this match" text shown by the in-game ? button.
        final String help;
        Match(String name, double startX, double spawnX, double spawnY, String help) {
            this.name = name; this.startX = startX; this.spawnX = spawnX; this.spawnY = spawnY;
            this.help = help;
        }
    }

    // Registers a match (call these in build order, left to right). The first
    //  match's checkpoint is just the level spawn, so list it with the players'
    //  start spot. help is the "how to clear this match" text the in-game
    //  ? button shows (use \n to break it into lines; null for none).
    protected void addMatch(String name, double startX, double spawnX, double spawnY, String help) {
        // the first match is where the players start, so kick it off with its own
        // banner too (otherwise only matches 2+ ever get an entry announcement)
        if (matches.isEmpty()) matchBannerTimer = MATCH_BANNER_TIME;
        matches.add(new Match(name, startX, spawnX, spawnY, help));
        // plant a pennant at this match's checkpoint as a world-anchored "MATCH n" cue
        decorations.add(new MatchFlag(spawnX, spawnY + 80, matches.size()));
    }

    // Places a portal-unlock football (collect them all to open the Portal).
    protected Ball addBall(double x, double y) {
        Ball b = new Ball(x, y);
        balls.add(b);
        return b;
    }

    // Places a portal-unlock key (collect them all to open the Portal).
    protected Key addKey(double x, double y) {
        Key k = new Key(x, y);
        keys.add(k);
        return k;
    }

    // Plants a solid energy gate at a match boundary. It blocks like a wall until
    //  openWhen is true — typically someKey::isCollected — then the
    //  level drops it. The field spans the full screen height (top to bottom), so it
    //  can't be jumped over — but the visible field runs from the screen top down
    //  to surfaceY (the floor it guards), so it doesn't bleed into it.
    protected Gate addGate(double x, double surfaceY, BooleanSupplier openWhen) {
        Gate g = new Gate(x, 0, 16, height, 0, surfaceY, openWhen);
        platforms.add(g);
        return g;
    }

    public int getBallTotal()      { return balls.size(); }
    public int getBallsCollected() {
        int n = 0;
        for (Ball b : balls) if (b.isCollected()) n++;
        return n;
    }

    public int getKeyTotal()       { return keys.size(); }
    public int getKeysCollected() {
        int n = 0;
        for (Key k : keys) if (k.isCollected()) n++;
        return n;
    }

    // Advances the active match (for the HUD/banner) only once BOTH players have
    //  crossed into the next one. A single player can't drag the section forward on
    //  its own, and nothing advances while a player is dead/respawning.
    private void updateMatchProgress(double dt) {
        if (matchBannerTimer > 0) matchBannerTimer -= dt;
        if (matches.isEmpty()) return;

        // both players must be alive and past the boundary -- use the trailing
        // (furthest-back) player, so the section only flips when the slower one
        // has also crossed.
        if (player1 == null || player2 == null
                || !player1.isAlive() || !player2.isAlive()) return;
        double trailing = Math.min(player1.getX(), player2.getX());

        // the next match is "entered" only once the trailing player reaches that
        // match's flag (planted at its spawnX), so the trigger lines up exactly
        // with the pennant the players can see -- not the off-screen startX.
        int idx = currentMatch;
        while (idx + 1 < matches.size() && trailing >= matches.get(idx + 1).spawnX) idx++;
        if (idx > currentMatch) {
            currentMatch = idx;
            Match m = matches.get(idx);
            // entering a match arms it as the pair's respawn point, so a death
            // restarts the current match rather than the whole level
            if (player1 != null) player1.setCheckpoint(m.spawnX, m.spawnY);
            if (player2  != null) player2.setCheckpoint(m.spawnX + 40, m.spawnY);
            // snap both players back to normal size, so any +/- pad scaling from the
            // finished match doesn't carry into the next one
            if (player1 != null) player1.resetScale();
            if (player2  != null) player2.resetScale();
            // also clear shields from the finished section (only those whose home
            // spot is before this match), and announce the new match
            if (player1 != null) player1.clearShield();
            if (player2  != null) player2.clearShield();
            pickups.removeIf(s -> s.getOriginX() < m.startX);
            matchBannerTimer = MATCH_BANNER_TIME;
        }
    }

    public int getMatchCount()  { return matches.size(); }
    public int getMatchNumber() { return matches.isEmpty() ? 0 : currentMatch + 1; }
    public String getMatchName() { return matches.isEmpty() ? "" : matches.get(currentMatch).name; }
    // "How to clear this match" text for the active match (null if none set).
    public String getMatchHelp() { return matches.isEmpty() ? null : matches.get(currentMatch).help; }

    // 0..1 opacity for the big "MATCH N" banner: fades in, holds, fades out.
    public double getMatchBannerAlpha() {
        double t = matchBannerTimer;
        if (t <= 0) return 0;
        double a = 1;
        if (t > MATCH_BANNER_TIME - 0.3) a = (MATCH_BANNER_TIME - t) / 0.3; // fade in
        else if (t < 0.7) a = t / 0.7;                                       // fade out
        return Math.max(0, Math.min(1, a));
    }

    // True if the object's horizontal span overlaps the visible camera window
    //  (with a margin), so we can skip rendering everything that's off-screen.
    private boolean onScreen(GameObject o) {
        double margin = 80;
        double oLeft = o.getX();
        double oRight = o.getX() + o.getWidth();
        return oRight >= cameraX - margin && oLeft <= cameraX + width + margin;
    }

    public void render(GraphicsContext gc) {
        drawBackground(gc);

        // shift the world by the camera so off-screen parts scroll into view
        gc.save();
        gc.translate(-cameraX, 0);

        for (GameObject d : decorations) if (onScreen(d)) d.render(gc);
        for (Platform p : platforms) if (onScreen(p)) p.render(gc);
        for (SizePad sp : sizePads) if (onScreen(sp)) sp.render(gc);
        for (ColoredBlock b : blocks) if (onScreen(b)) b.render(gc);
        for (Shield s : pickups) if (onScreen(s)) s.render(gc);
        for (Hazard h : hazards) if (onScreen(h)) h.render(gc);
        for (Key k : keys) if (onScreen(k)) k.render(gc);
        for (Ball b : balls) if (onScreen(b)) b.render(gc);
        if (goal != null) goal.render(gc);
        if (player1 != null) player1.render(gc);
        if (player2 != null) player2.render(gc);

        renderBlockedBubble(gc);

        gc.restore();
    }

    // While a player is pushing a closed gate, prompt it to grab the collectible.
    private void renderBlockedBubble(GraphicsContext gc) {
        if (blockedPlayer == null || !blockedPlayer.isAlive()) return;
        String item = keys.isEmpty() ? "football" : "key";
        drawThoughtBubble(gc, blockedPlayer, "Grab the " + item + "!");
    }

    private void drawThoughtBubble(GraphicsContext gc, Player p, String text) {
        Font font = Font.font("Verdana", FontWeight.BOLD, 13);
        Text probe = new Text(text);
        probe.setFont(font);
        double tw = probe.getLayoutBounds().getWidth();
        double padX = 12, h = 30;
        double w = tw + padX * 2;
        double cx = p.getX() + p.getWidth() / 2;
        double bx = cx - w / 2;
        double by = p.getY() - h - 24;

        // thought puffs leading up to the bubble
        gc.setFill(Color.web("#ffffff", 0.95));
        gc.fillOval(cx - 4, by + h + 14, 7, 7);
        gc.fillOval(cx - 1, by + h + 5, 10, 10);

        gc.fillRoundRect(bx, by, w, h, 14, 14);
        gc.setStroke(Color.web("#3a3d42", 0.55));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(bx, by, w, h, 14, 14);

        gc.setFill(Color.web("#2b2b2b"));
        gc.setFont(font);
        gc.fillText(text, bx + padX, by + h / 2 + 5);
    }

    // Sky gradient, cached so we don't reallocate it every frame.
    private LinearGradient skyGradient;

    // Gradient sky + parallax sun, clouds and rolling hills behind the level.
    private void drawBackground(GraphicsContext gc) {
        if (skyGradient == null) {
            skyGradient = new LinearGradient(0, 0, 0, height, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#5bb8e8")),
                    new Stop(0.55, Color.web("#bfe9ff")),
                    new Stop(1, Color.web("#eafaff")));
        }
        gc.setFill(skyGradient);
        gc.fillRect(0, 0, width, height);

        // sun (fixed, with a soft glow)
        gc.setFill(Color.web("#fff7c2", 0.35));
        gc.fillOval(width - 150, 20, 130, 130);
        gc.setFill(Color.web("#ffe98a"));
        gc.fillOval(width - 128, 42, 86, 86);

        // far hills (slow parallax) — only step through the visible window
        gc.save();
        gc.translate(-cameraX * 0.30, 0);
        gc.setFill(Color.web("#bfe6b0"));
        double v0 = cameraX * 0.30;                       // left edge in this layer's space
        for (double hx = alignStart(-160, 280, v0 - 360); hx < v0 + width + 40; hx += 280) {
            gc.fillOval(hx, height - 150, 360, 300);
        }
        gc.restore();

        // near hills (faster parallax)
        gc.save();
        gc.translate(-cameraX * 0.55, 0);
        gc.setFill(Color.web("#92d27f"));
        double v1 = cameraX * 0.55;
        for (double hx = alignStart(-240, 320, v1 - 400); hx < v1 + width + 40; hx += 320) {
            gc.fillOval(hx, height - 110, 400, 260);
        }
        gc.restore();

        // clouds
        gc.save();
        gc.translate(-cameraX * 0.22, 0);
        double v2 = cameraX * 0.22;
        for (double cxp = alignStart(60, 360, v2 - 110); cxp < v2 + width + 40; cxp += 360) {
            int idx = (int) Math.round((cxp - 60) / 360);
            drawCloud(gc, cxp, 50 + (Math.floorMod(idx, 3)) * 34);
        }
        gc.restore();
    }

    // First value of the series base + k*step that is >= from.
    private static double alignStart(double base, double step, double from) {
        if (from <= base) return base;
        double k = Math.ceil((from - base) / step);
        return base + k * step;
    }

    private void drawCloud(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.web("#ffffff", 0.9));
        gc.fillOval(x, y + 10, 46, 30);
        gc.fillOval(x + 26, y, 56, 40);
        gc.fillOval(x + 60, y + 12, 44, 28);
        gc.fillOval(x + 18, y + 20, 80, 24);
    }

    public boolean isCompleted() { return completed; }
    public boolean isFailed()    { return failed; }
    public long getElapsedMillis()    { return Math.round(elapsedSeconds * 1000); }
}