package com.assignment.objects;

import com.assignment.core.InputManager;
import com.assignment.objects.pickups.Shield;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.List;

public abstract class Player extends Entity {
    public static final double MOVE_SPEED = 220.0;
    public static final double JUMP_VELOCITY = -480.0;
    public static final double GRAVITY = 1200.0;
    public static final double MAX_FALL_SPEED = 700.0;

    // Flappy-Bird mode, switched on by the level while a player is inside a "flap
    //  zone": the jump key becomes a repeatable mid-air flap, and gravity is
    //  gentler so the rise/fall feels floaty and controllable.
    public static final double FLAP_VELOCITY  = -300.0;
    public static final double FLAP_GRAVITY   = 700.0;
    public static final double FLAP_MAX_FALL  = 360.0;
    private boolean flapMode = false;
    public void setFlapMode(boolean on) { this.flapMode = on; }
    public boolean isFlapMode() { return flapMode; }
    private double flapAnim = 0;   // free-running clock for the ship's thruster flicker

    // Resize mechanic (Pico Park-style +/- pads): a player grows or shrinks while
    //  standing on a size pad. Scaling keeps the FEET and horizontal centre fixed,
    //  so it never sinks into the floor or pops off it. Everything drawn/collided
    //  derives from width/height, so the whole player scales with it.
    public static final double BASE_WIDTH  = 32;
    public static final double BASE_HEIGHT = 40;
    public static final double MIN_SCALE = 0.5;
    public static final double MAX_SCALE = 1.8;
    private double scale = 1.0;

    // Grows (positive) or shrinks (negative) by dScale, clamped.
    public void resizeBy(double dScale) { setScale(scale + dScale); }

    public void setScale(double s) {
        s = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
        if (s == scale) return;
        double cx = x + width / 2;
        double feet = y + height;
        width  = BASE_WIDTH  * s;
        height = BASE_HEIGHT * s;
        x = cx - width / 2;
        y = feet - height;
        scale = s;
    }

    // Snaps back to normal size (used on respawn and when entering a new match).
    public void resetScale() { setScale(1.0); }

    protected final Color color;
    protected boolean jumpHeld;
    protected Shield carriedShield;
    protected int facing = 1;

    // death -> auto-respawn lifecycle: the particles burst at the death spot,
    // fly back to the original spawn, then the player re-forms there
    private enum Phase { ALIVE, RESPAWN }
    private static final double RESPAWN_TIME = DeathEffect.DURATION;
    private static final double KITTY_FADE_START = 0.8;   // player fades in over the tail
    private Phase phase = Phase.ALIVE;
    private double phaseTime = 0;
    private DeathEffect effect;
    private double spawnX, spawnY;                   // original level-start spot
    private boolean spawnLocked = false;             // pinned once settled on ground
    private double deathX;                            // left-x where it burst, for the camera

    // ribbon trail: a smooth tapering streak of colour that flows behind while moving
    private static final double TRAIL_LIFE = 0.32;
    private static final double TRAIL_WIDTH = 20;     // full width at the head
    private final java.util.List<TrailPoint> trail = new java.util.ArrayList<>();

    // twinkling sparkles sprinkled along the ribbon
    private static final double SPARKLE_LIFE = 0.5;
    private static final double SPARKLE_INTERVAL = 0.05;
    private final java.util.List<Sparkle> sparkles = new java.util.ArrayList<>();
    private double sparkTimer = 0;

    protected Player(double x, double y, Color color) {
        super(x, y, 32, 40);
        this.color = color;
        this.jumpHeld = false;
        this.spawnX = x;
        this.spawnY = y;
    }

    @Override
    public void kill() {
        if (phase != Phase.ALIVE) return;            // already dying
        super.kill();                                // alive = false (intangible)
        deathX = x;
        double deathCx = x + width / 2;
        double deathCy = y + height / 2;
        // snap back to the original spawn straight away (the player stays hidden);
        // the particles burst at the death spot and fly home to here
        x = spawnX;
        y = spawnY;
        width = BASE_WIDTH;          // re-form at normal size (drop any +/- pad scaling)
        height = BASE_HEIGHT;
        scale = 1.0;
        velocityX = 0;
        velocityY = 0;
        onGround = false;
        phase = Phase.RESPAWN;
        phaseTime = 0;
        effect = new DeathEffect(deathCx, deathCy, x + width / 2, y + height / 2, color);
    }

    // Moves this player's respawn point to a match checkpoint, so a later death
    // re-forms it here instead of back at the level start. Locks the spawn so the
    // "settle on ground" auto-pin won't overwrite it.
    public void setCheckpoint(double x, double y) {
        this.spawnX = x;
        this.spawnY = y;
        this.spawnLocked = true;
    }

    // Advances the death/respawn animation while the player is not alive.
    public void updateRespawn(double dt) {
        if (effect != null) effect.update(dt);
        tickTrail(dt);                       // let the trail fade out
        phaseTime += dt;
        if (phase == Phase.RESPAWN && phaseTime >= RESPAWN_TIME) {
            phase = Phase.ALIVE;
            alive = true;
            effect = null;
        }
    }

    public int getFacing() { return facing; }

    // Where the camera should look. Normally the player's position, but while
    // respawning it lingers at the death spot through the burst + hold, then
    // glides home with the particles (so the camera doesn't snap to the spawn).
    public double getFocusX() {
        if (phase != Phase.RESPAWN) return x;
        double rp = DeathEffect.returnProgress(phaseTime / RESPAWN_TIME);
        return deathX + (spawnX - deathX) * rp;
    }

    public boolean canPickUp(Shield s) {
        return carriedShield == null && s != null && !s.isCollected();
    }

    public void pickUp(Shield s) {
        if (canPickUp(s)) {
            this.carriedShield = s;
            s.pickUp(this);
        }
    }

    // Let go of any carried shield (used when a new match/checkpoint begins).
    public void clearShield() {
        if (carriedShield != null) {
            carriedShield.drop();
            carriedShield = null;
        }
    }

    protected abstract KeyCode leftKey();
    protected abstract KeyCode rightKey();
    protected abstract KeyCode jumpKey();

    public void handleInput(InputManager input) {
        double vx = 0;
        if (input.isDown(leftKey()))  { vx -= MOVE_SPEED; facing = -1; }
        if (input.isDown(rightKey())) { vx += MOVE_SPEED; facing = 1; }
        velocityX = vx;

        boolean jumpDown = input.isDown(jumpKey());
        if (jumpDown && !jumpHeld && (onGround || flapMode)) {
            velocityY = flapMode ? FLAP_VELOCITY : JUMP_VELOCITY;   // flap repeats mid-air
            onGround = false;
        }
        jumpHeld = jumpDown;
    }

    @Override
    public void update(double deltaTime) {
        double g       = flapMode ? FLAP_GRAVITY  : GRAVITY;
        double maxFall = flapMode ? FLAP_MAX_FALL : MAX_FALL_SPEED;
        velocityY += g * deltaTime;
        if (velocityY > maxFall) velocityY = maxFall;
        flapAnim += deltaTime;

        x += velocityX * deltaTime;
        y += velocityY * deltaTime;

        // drop an after-image at a steady rate while actually moving (running,
        // jumping or falling) — staying still leaves no trail
        double speed = Math.abs(velocityX) + Math.abs(velocityY);
        if (speed > 60) {
            // drop a point at the body centre to extend the ribbon
            trail.add(new TrailPoint(x + width / 2, y + height / 2));
            // sprinkle a sparkle onto the ribbon now and then
            sparkTimer -= deltaTime;
            if (sparkTimer <= 0) {
                spawnSparkle();
                sparkTimer = SPARKLE_INTERVAL;
            }
        } else {
            sparkTimer = 0;
        }
        tickTrail(deltaTime);
    }

    public void resolveCollisions(List<Platform> platforms) {
        onGround = false;
        for (Platform p : platforms) {
            resolveAgainst(p);
        }
        // pin the respawn point to where the player first settles on the ground,
        // so it re-forms standing there instead of dropping in from above
        if (onGround && !spawnLocked) {
            spawnX = x;
            spawnY = y;
            spawnLocked = true;
        }
    }

    public void resolveAgainstPlayer(Player other) {
        if (!intersects(other)) return;

        double overlapLeft   = (x + width) - other.getX();
        double overlapRight  = (other.getX() + other.getWidth()) - x;
        double overlapTop    = (y + height) - other.getY();
        double overlapBottom = (other.getY() + other.getHeight()) - y;

        double minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
                                     Math.min(overlapTop, overlapBottom));

        // Unlike a solid wall, the OTHER player can move -- so resolve only THIS
        // player, and only when it's the one moving INTO the other. A stationary
        // teammate is never shoved, so one player can't push the other around.
        // (If both walk into each other, both stop where they meet.)
        if (minOverlap == overlapTop && velocityY >= 0) {
            y = other.getY() - height;
            velocityY = 0;
            onGround = true;
        } else if (minOverlap == overlapBottom && velocityY < 0) {
            y = other.getY() + other.getHeight();
            velocityY = 0;
        } else if (minOverlap == overlapLeft && velocityX > 0) {
            x = other.getX() - width;
            velocityX = 0;
        } else if (minOverlap == overlapRight && velocityX < 0) {
            x = other.getX() + other.getWidth();
            velocityX = 0;
        }
    }

    public void resolveAgainstBlock(ColoredBlock block) {
        resolveAgainst(block);
    }

    private void resolveAgainst(GameObject other) {
        if (!intersects(other)) return;

        double overlapLeft   = (x + width) - other.getX();
        double overlapRight  = (other.getX() + other.getWidth()) - x;
        double overlapTop    = (y + height) - other.getY();
        double overlapBottom = (other.getY() + other.getHeight()) - y;

        double minOverlap = Math.min(Math.min(overlapLeft, overlapRight),
                                     Math.min(overlapTop, overlapBottom));

        // push out along the shallowest axis (minimum translation vector) so the
        // player is ALWAYS separated from a solid -- even when it isn't moving into
        // it (e.g. shoved by a teammate, a block, or a moving platform while
        // standing still). Velocity is only cancelled when it points into the
        // surface, so resting on and sliding along a platform still feel normal.
        if (minOverlap == overlapTop) {
            y = other.getY() - height;
            if (velocityY > 0) velocityY = 0;
            onGround = true;
        } else if (minOverlap == overlapBottom) {
            // only bonk a ceiling/underside when actually moving UP into it. A
            // block resting ON the player's head also overlaps from above, and must
            // NOT shove the player down -- the block's own restOnPlayer keeps it
            // perched there. So when the player isn't rising, leave it be.
            if (velocityY < 0) {
                y = other.getY() + other.getHeight();
                velocityY = 0;
            }
        } else if (minOverlap == overlapLeft) {
            x = other.getX() - width;
            if (velocityX > 0) velocityX = 0;
        } else {
            x = other.getX() + other.getWidth();
            if (velocityX < 0) velocityX = 0;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        renderTrail(gc);                     // trail sits behind the player
        if (phase == Phase.RESPAWN) {
            if (effect != null) effect.render(gc);
            // player fades in over the tail, as the particles arrive and converge
            double a = (phaseTime - KITTY_FADE_START * RESPAWN_TIME)
                    / (RESPAWN_TIME - KITTY_FADE_START * RESPAWN_TIME);
            if (a > 0) {
                gc.setGlobalAlpha(Math.min(1, a));
                boolean wasGround = onGround;
                onGround = true;          // form settled in place, no airborne bob
                drawKitty(gc);
                onGround = wasGround;
                gc.setGlobalAlpha(1);
            }
            return;
        }
        // Geometry-Dash ship for the fly section: only once the player has actually
        // LEFT the platform (airborne), not while it's still standing on the ledge.
        if (flapMode && !onGround) drawShip(gc);
        drawKitty(gc);
    }

    // A little spaceship the player rides while flying through a flap zone (the
    // "ship mode" from Geometry Dash). It hugs the player's lower body so the player
    // looks seated in the cockpit, and the whole craft pitches nose-up while
    // rising and nose-down while falling, with a flickering thruster out the back.
    private void drawShip(GraphicsContext gc) {
        double cx = x + width / 2;
        double cy = y + height / 2;
        // pitch with vertical motion: thrusting up -> nose up, dropping -> nose down
        double tilt = Math.max(-26, Math.min(26, velocityY * 0.06));

        Color hull   = color.darker();
        Color hullHi = color;
        Color metal  = Color.web("#cfd6dc");
        Color metalD = Color.web("#8a9196");
        Color glass  = Color.web("#bfe9ff");

        gc.save();
        gc.translate(cx, cy);
        gc.rotate(tilt);                 // nose (+x, the right) lifts as tilt goes negative

        // thruster flame out the back (left), flickering on a sine + jitter
        double flick = 0.7 + 0.3 * Math.sin(flapAnim * 30) + Math.random() * 0.12;
        double fl = 16 * flick;
        gc.setFill(Color.web("#ff8a1e"));
        gc.fillPolygon(new double[]{-24, -24 - fl, -24}, new double[]{4, 12, 20}, 3);
        gc.setFill(Color.web("#ffe14d"));
        gc.fillPolygon(new double[]{-24, -24 - fl * 0.6, -24}, new double[]{8, 12, 16}, 3);

        // hull: a rounded metal body, nose pointing right
        gc.setFill(metal);
        gc.fillOval(-26, 2, 50, 24);
        gc.setFill(metalD);
        gc.fillOval(-26, 16, 50, 10);    // shaded underbelly
        gc.setFill(hull);                // colour stripe in the player's colour
        gc.fillRect(-20, 9, 40, 6);
        gc.setFill(hullHi);
        gc.fillRect(-20, 9, 40, 2);

        // pointed nose cone on the right
        gc.setFill(metalD);
        gc.fillPolygon(new double[]{20, 34, 20}, new double[]{4, 14, 24}, 3);

        // tail fin up the back
        gc.setFill(hull);
        gc.fillPolygon(new double[]{-22, -10, -10}, new double[]{4, 4, -10}, 3);

        // glass cockpit bubble where the player sits
        gc.setGlobalAlpha(0.55);
        gc.setFill(glass);
        gc.fillOval(-12, -6, 26, 22);
        gc.setGlobalAlpha(1);

        gc.restore();
    }

    // Pixel-art Hello Kitty sprite, 16 columns x 20 rows. Each char is one pixel:
    //   '.' transparent  'o' outline  'W' white  'e' eye  'n' nose
    //   'p' pink cheek    'B' shirt (player colour)  'b' shirt shade
    private static final String[] SPRITE = {
            "...o........o...",
            "..oWo......oWo..",
            ".oWWWo....oWWWo.",
            ".oWWWWWWWWWWWWo.",
            "oWWWWWWWWWWWWWWo",
            "oWWWWWWWWWWWWWWo",
            "oWWWWWWWWWWWWWWo",
            "oWWWeeWWWWeeWWWo",
            "oWWWeeWnnWeeWWWo",
            "oWppWWWnnWWWppWo",
            "oWWWWWWWWWWWWWWo",
            ".oWWWWWWWWWWWWo.",
            "..oWWWWWWWWWWo..",
            ".WoBBBBBBBBBBoW.",
            ".WoBbBBBBBBbBoW.",
            ".WoBBBBBBBBBBoW.",
            ".oBBBBBBBBBBBBo.",
            ".oBBBBBBBBBBBBo.",
            ".....WW..WW.....",
            "....oWWooWWo....",
    };

    // --- ribbon trail -------------------------------------------------------

    private void tickTrail(double dt) {
        for (int i = trail.size() - 1; i >= 0; i--) {
            trail.get(i).life -= dt;
            if (trail.get(i).life <= 0) trail.remove(i);     // oldest tail points expire
        }
        for (int i = sparkles.size() - 1; i >= 0; i--) {
            if (sparkles.get(i).update(dt)) sparkles.remove(i);
        }
    }

    private void renderTrail(GraphicsContext gc) {
        // two passes: a wide soft coloured ribbon, then a bright slim core on top
        drawRibbon(gc, 1.0, color, 0.45);
        drawRibbon(gc, 0.4, color.brighter(), 0.7);
        gc.setGlobalAlpha(1);
        // twinkling stars riding along the ribbon
        for (Sparkle s : sparkles) s.render(gc);
    }

    private static final Color[] SPARKLE_TINTS = {
            Color.web("#ffffff"), Color.web("#fff4b0"), Color.web("#ffe066")
    };

    private void spawnSparkle() {
        // scatter around the body, within the ribbon's width
        double cx = x + width / 2 + (Math.random() - 0.5) * width;
        double cy = y + height / 2 + (Math.random() - 0.5) * height * 0.6;
        double vx = -facing * (6 + Math.random() * 18);
        double vy = -6 - Math.random() * 18;
        double size = 2.5 + Math.random() * 3;
        Color c = (Math.random() < 0.4)
                ? color.brighter()
                : SPARKLE_TINTS[(int) (Math.random() * SPARKLE_TINTS.length)];
        double spin = 7 + Math.random() * 9;            // blink speed
        sparkles.add(new Sparkle(cx, cy, vx, vy, size, c, spin));
    }

    // Stitches the recorded points into a tapering, fading strip.
    private void drawRibbon(GraphicsContext gc, double widthScale, Color col, double alphaScale) {
        int n = trail.size();
        if (n < 2) return;
        gc.setFill(col);
        double[] xs = new double[4], ys = new double[4];
        for (int i = 0; i < n - 1; i++) {
            TrailPoint a = trail.get(i), b = trail.get(i + 1);
            double fa = a.life / TRAIL_LIFE;     // 0 at the tail, 1 at the head
            double fb = b.life / TRAIL_LIFE;
            double dx = b.x - a.x, dy = b.y - a.y;
            double len = Math.hypot(dx, dy);
            if (len < 0.0001) continue;
            double pxn = -dy / len, pyn = dx / len;          // unit perpendicular
            double wa = TRAIL_WIDTH * 0.5 * widthScale * fa;
            double wb = TRAIL_WIDTH * 0.5 * widthScale * fb;
            xs[0] = a.x + pxn * wa; ys[0] = a.y + pyn * wa;
            xs[1] = b.x + pxn * wb; ys[1] = b.y + pyn * wb;
            xs[2] = b.x - pxn * wb; ys[2] = b.y - pyn * wb;
            xs[3] = a.x - pxn * wa; ys[3] = a.y - pyn * wa;
            gc.setGlobalAlpha(alphaScale * (fa + fb) * 0.5);
            gc.fillPolygon(xs, ys, 4);
        }
    }

    // One recorded position along the ribbon's path.
    private static final class TrailPoint {
        private final double x, y;
        private double life = TRAIL_LIFE;

        TrailPoint(double x, double y) { this.x = x; this.y = y; }
    }

    // A twinkling four-point star riding along the ribbon.
    private static final class Sparkle {
        private double x, y, vx, vy, phase;
        private final double size, spin;
        private double life = SPARKLE_LIFE;
        private final Color color;
        private final double[] xs = new double[8];
        private final double[] ys = new double[8];

        Sparkle(double x, double y, double vx, double vy, double size, Color color, double spin) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.size = size; this.color = color; this.spin = spin;
            this.phase = Math.random() * Math.PI * 2;
        }

        // Advances the sparkle; returns true once it has faded out.
        boolean update(double dt) {
            life -= dt;
            x += vx * dt;
            y += vy * dt;
            vx *= (1 - 1.6 * dt);
            vy *= (1 - 1.2 * dt);
            phase += spin * dt;             // makes it blink
            return life <= 0;
        }

        void render(GraphicsContext gc) {
            double t = life / SPARKLE_LIFE;
            if (t <= 0) return;
            double twinkle = 0.2 + 0.8 * Math.abs(Math.sin(phase));   // blink blink
            double r = size * (0.5 + 0.5 * twinkle);

            for (int i = 0; i < 8; i++) {
                double rad = (i % 2 == 0) ? r : r * 0.38;
                double ang = Math.toRadians(45 * i) - Math.PI / 2;
                xs[i] = x + Math.cos(ang) * rad;
                ys[i] = y + Math.sin(ang) * rad;
            }
            double a = Math.min(1, t * twinkle);
            gc.setGlobalAlpha(a);
            gc.setFill(color);
            gc.fillPolygon(xs, ys, 8);
            gc.setGlobalAlpha(a * 0.9);
            gc.setFill(Color.WHITE);
            gc.fillOval(x - r * 0.18, y - r * 0.18, r * 0.36, r * 0.36);
            gc.setGlobalAlpha(1);
        }
    }

    private void drawKitty(GraphicsContext gc) {
        double px = width / 16.0;            // one sprite pixel (2.0 at 32px wide)
        double ox = x;
        double oy = y + (onGround ? 0 : -2); // tiny hop while airborne

        Color outline = Color.web("#8a9196");
        Color white   = Color.web("#fdfdfd");
        Color eye     = Color.web("#2b2b2b");
        Color nose    = Color.web("#f5a623");
        Color pink    = Color.web("#f5a7bb");
        Color shirt   = color;
        Color shirtSh = color.darker();

        // paint the bitmap, one little square per pixel (slight overlap = no seams)
        for (int r = 0; r < SPRITE.length; r++) {
            String row = SPRITE[r];
            for (int c = 0; c < row.length(); c++) {
                Color fill;
                switch (row.charAt(c)) {
                    case 'o': fill = outline; break;
                    case 'W': fill = white;   break;
                    case 'e': fill = eye;     break;
                    case 'n': fill = nose;    break;
                    case 'p': fill = pink;    break;
                    case 'B': fill = shirt;   break;
                    case 'b': fill = shirtSh; break;
                    default:  continue;       // '.' transparent
                }
                gc.setFill(fill);
                gc.fillRect(ox + c * px, oy + r * px, px + 0.6, px + 0.6);
            }
        }

        // whiskers: three soft grey strokes poking out each cheek
        gc.setFill(Color.web("#9aa1a8"));
        double wl = 2.4 * px, wt = 0.55 * px;
        for (int wr : new int[]{6, 8, 10}) {
            double wy = oy + (wr + 0.3) * px;
            gc.fillRect(ox - wl, wy, wl, wt);          // left
            gc.fillRect(ox + 16 * px, wy, wl, wt);     // right
        }

        // bow on the right ear, in the player's colour with a yellow centre knot
        gc.setFill(shirt);
        gc.fillRect(ox + 9 * px,  oy, 3 * px + 0.6, 4 * px + 0.6);   // left lobe
        gc.fillRect(ox + 13 * px, oy, 3 * px + 0.6, 4 * px + 0.6);   // right lobe
        gc.fillRect(ox + 12 * px, oy, px + 0.6, 4 * px + 0.6);       // knot base
        gc.setFill(shirtSh);                                         // lobe edges
        gc.fillRect(ox + 9 * px,  oy,             3 * px, 0.6 * px);
        gc.fillRect(ox + 9 * px,  oy + 3.4 * px,  3 * px, 0.6 * px);
        gc.fillRect(ox + 13 * px, oy,             3 * px, 0.6 * px);
        gc.fillRect(ox + 13 * px, oy + 3.4 * px,  3 * px, 0.6 * px);
        gc.setFill(Color.web("#ffd23f"));                            // yellow accents
        gc.fillRect(ox + 12 * px, oy + px, px + 0.6, 2 * px + 0.6);
        gc.fillRect(ox + 10 * px, oy + px, px, px);
        gc.fillRect(ox + 14 * px, oy + 2 * px, px, px);
    }

    // Draws the same kitty sprite as seen from BEHIND — the face (eyes, nose,
    // cheeks) is blanked to white so only the back of the head shows — at (x,y),
    // scaled so the whole sprite is w wide, using kit as the shirt
    // colour. Static so other screens (e.g. the penalty shoot-out) can render the
    // character on-model without a live Player. Whiskers are dropped (they belong
    // to the face); the ears and bow are kept since they read from the back too.
    public static void drawBackSprite(GraphicsContext gc, double x, double y, double w, Color kit) {
        double px = w / 16.0;
        Color outline = Color.web("#8a9196");
        Color white   = Color.web("#fdfdfd");
        Color shirt   = kit;
        Color shirtSh = kit.darker();

        for (int r = 0; r < SPRITE.length; r++) {
            String row = SPRITE[r];
            for (int c = 0; c < row.length(); c++) {
                Color fill;
                switch (row.charAt(c)) {
                    case 'o': fill = outline; break;
                    case 'W': fill = white;   break;
                    case 'e':                 // no face from behind: eyes / nose /
                    case 'n':                 // cheeks become the back of the white head
                    case 'p': fill = white;   break;
                    case 'B': fill = shirt;   break;
                    case 'b': fill = shirtSh; break;
                    default:  continue;       // '.' transparent
                }
                gc.setFill(fill);
                gc.fillRect(x + c * px, y + r * px, px + 0.6, px + 0.6);
            }
        }

        // bow on the ear, in the player's colour with a yellow centre knot
        gc.setFill(shirt);
        gc.fillRect(x + 9 * px,  y, 3 * px + 0.6, 4 * px + 0.6);
        gc.fillRect(x + 13 * px, y, 3 * px + 0.6, 4 * px + 0.6);
        gc.fillRect(x + 12 * px, y, px + 0.6, 4 * px + 0.6);
        gc.setFill(shirtSh);
        gc.fillRect(x + 9 * px,  y,            3 * px, 0.6 * px);
        gc.fillRect(x + 9 * px,  y + 3.4 * px, 3 * px, 0.6 * px);
        gc.fillRect(x + 13 * px, y,            3 * px, 0.6 * px);
        gc.fillRect(x + 13 * px, y + 3.4 * px, 3 * px, 0.6 * px);
        gc.setFill(Color.web("#ffd23f"));
        gc.fillRect(x + 12 * px, y + px, px + 0.6, 2 * px + 0.6);
        gc.fillRect(x + 10 * px, y + px, px, px);
        gc.fillRect(x + 14 * px, y + 2 * px, px, px);
    }

    // Draws the kitty sprite FRONT-ON (face, whiskers, ears and bow all shown) at
    // (x,y), scaled so the whole sprite is w wide, using kit as the
    // shirt colour. Static so other screens (e.g. the shoot-out's opponent keeper)
    // can render an on-model kitty without a live Player.
    public static void drawFrontSprite(GraphicsContext gc, double x, double y, double w, Color kit) {
        double px = w / 16.0;
        Color outline = Color.web("#8a9196");
        Color white   = Color.web("#fdfdfd");
        Color eye     = Color.web("#2b2b2b");
        Color nose    = Color.web("#f5a623");
        Color pink    = Color.web("#f5a7bb");
        Color shirt   = kit;
        Color shirtSh = kit.darker();

        for (int r = 0; r < SPRITE.length; r++) {
            String row = SPRITE[r];
            for (int c = 0; c < row.length(); c++) {
                Color fill;
                switch (row.charAt(c)) {
                    case 'o': fill = outline; break;
                    case 'W': fill = white;   break;
                    case 'e': fill = eye;     break;
                    case 'n': fill = nose;    break;
                    case 'p': fill = pink;    break;
                    case 'B': fill = shirt;   break;
                    case 'b': fill = shirtSh; break;
                    default:  continue;
                }
                gc.setFill(fill);
                gc.fillRect(x + c * px, y + r * px, px + 0.6, px + 0.6);
            }
        }

        // whiskers
        gc.setFill(Color.web("#9aa1a8"));
        double wl = 2.4 * px, wt = 0.55 * px;
        for (int wr : new int[]{6, 8, 10}) {
            double wy = y + (wr + 0.3) * px;
            gc.fillRect(x - wl, wy, wl, wt);
            gc.fillRect(x + 16 * px, wy, wl, wt);
        }

        // bow on the ear
        gc.setFill(shirt);
        gc.fillRect(x + 9 * px,  y, 3 * px + 0.6, 4 * px + 0.6);
        gc.fillRect(x + 13 * px, y, 3 * px + 0.6, 4 * px + 0.6);
        gc.fillRect(x + 12 * px, y, px + 0.6, 4 * px + 0.6);
        gc.setFill(shirtSh);
        gc.fillRect(x + 9 * px,  y,            3 * px, 0.6 * px);
        gc.fillRect(x + 9 * px,  y + 3.4 * px, 3 * px, 0.6 * px);
        gc.fillRect(x + 13 * px, y,            3 * px, 0.6 * px);
        gc.fillRect(x + 13 * px, y + 3.4 * px, 3 * px, 0.6 * px);
        gc.setFill(Color.web("#ffd23f"));
        gc.fillRect(x + 12 * px, y + px, px + 0.6, 2 * px + 0.6);
        gc.fillRect(x + 10 * px, y + px, px, px);
        gc.fillRect(x + 14 * px, y + 2 * px, px, px);
    }
}