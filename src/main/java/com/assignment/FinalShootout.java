package com.assignment;

import com.assignment.core.InputManager;
import com.assignment.core.TeamColors;
import com.assignment.objects.Ball;
import com.assignment.objects.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import static com.assignment.GameApp.WINDOW_WIDTH;
import static com.assignment.GameApp.WINDOW_HEIGHT;
import static com.assignment.GameApp.formatTime;

// Penalty shoot-out finale: reached when both players clear Level 3 and enter the Cup
// portal. Each takes 3 kicks, turn by turn (press SPACE); an aim line sweeps the goal
// past a pacing keeper. Most goals wins, a tie means both win. GameApp drives it via
// update/render/isComplete/updateResult/renderResult.
class FinalShootout {
    private static final int REGULATION_SHOTS = 3;
    private static final int AIM_LANES = 8;
    private static final double GOAL_LEFT = 278;
    private static final double GOAL_RIGHT = 682;
    private static final double GOAL_Y = 112;
    private static final double BALL_START_X = WINDOW_WIDTH / 2;
    private static final double BALL_START_Y = 500;
    private static final double BASE_KEEPER_REACH = 54;

    private final long levelTimeMs;
    private final int[] scores = new int[2];
    private final int[] shots = new int[2];
    private final int[][] regulationResults = new int[2][REGULATION_SHOTS];
    private int currentPlayer = 0;
    private double aimLane = 0;
    private double aimDir = 1;
    private double keeperX = WINDOW_WIDTH / 2;
    private double keeperLastX = WINDOW_WIDTH / 2;
    private double keeperDir = 1;
    private double keeperDive = 0;
    private double sceneTime = 0;
    private boolean shootHeld = false;   // SPACE shoots for whoever's turn it is
    private boolean resolving = false;
    private boolean complete = false;
    private boolean lastGoal = false;
    private double feedbackTimer = 0;
    private double cheerTimer = 0;
    private double ballX = BALL_START_X;
    private double ballY = BALL_START_Y;
    private double shotTargetX = BALL_START_X;
    private Color cheerColor = Color.WHITE;
    private String message = "Press SPACE to shoot";

    FinalShootout(long levelTimeMs) {
        this.levelTimeMs = levelTimeMs;
    }

    void update(double dt, InputManager input) {
        if (complete) return;

        sceneTime += dt;
        cheerTimer = Math.max(0, cheerTimer - dt);
        keeperLastX = keeperX;
        keeperX += keeperDir * getKeeperSpeed() * dt;
        if (keeperX < GOAL_LEFT + 62) {
            keeperX = GOAL_LEFT + 62;
            keeperDir = 1;
        } else if (keeperX > GOAL_RIGHT - 62) {
            keeperX = GOAL_RIGHT - 62;
            keeperDir = -1;
        }
        keeperDive = Math.max(0, keeperDive - dt * 2.8);

        if (resolving) {
            feedbackTimer -= dt;
            double t = 1 - Math.max(0, feedbackTimer) / 1.0;
            double eased = 1 - Math.pow(1 - t, 2);
            ballX = BALL_START_X + (shotTargetX - BALL_START_X) * eased;
            ballY = BALL_START_Y + (GOAL_Y - BALL_START_Y) * eased;
            if (feedbackTimer <= 0) advanceTurn();
            return;
        }

        aimLane += aimDir * getAimSpeed() * dt;
        if (aimLane < 0) {
            aimLane = 0;
            aimDir = 1;
        } else if (aimLane > AIM_LANES - 1) {
            aimLane = AIM_LANES - 1;
            aimDir = -1;
        }
        ballX = BALL_START_X;
        ballY = BALL_START_Y;

        boolean shootDown = input.isDown(KeyCode.SPACE);
        boolean pressed = shootDown && !shootHeld;
        shootHeld = shootDown;
        if (pressed) shoot();
    }

    private void shoot() {
        shotTargetX = getAimTargetX();
        lastGoal = Math.abs(shotTargetX - keeperX) > getKeeperReach();
        cheerTimer = 1.45;                   // flashes GOAL!/SAVED! in the centre either way
        if (lastGoal) {
            scores[currentPlayer]++;
            cheerColor = currentPlayer == 0 ? TeamColors.p1 : TeamColors.p2;
        }
        shots[currentPlayer]++;
        if (shots[currentPlayer] <= REGULATION_SHOTS) {
            regulationResults[currentPlayer][shots[currentPlayer] - 1] = lastGoal ? 1 : -1;
        }
        resolving = true;
        feedbackTimer = 1.0;
        keeperDive = 1.0;
        keeperDir = shotTargetX >= keeperX ? 1 : -1;
    }

    private void advanceTurn() {
        resolving = false;
        ballX = BALL_START_X;
        ballY = BALL_START_Y;

        // once both players have taken all three kicks the match is over: more goals
        // wins, and a tie means BOTH players lift the cup (no sudden death).
        if (shots[0] >= REGULATION_SHOTS && shots[1] >= REGULATION_SHOTS) {
            complete = true;
            return;
        }

        currentPlayer = 1 - currentPlayer;
        message = "Press SPACE to shoot";
    }

    boolean isComplete() {
        return complete;
    }

    void updateResult(double dt) {
        sceneTime += dt;
        cheerTimer = Math.max(0, cheerTimer - dt);
    }

    void render(GraphicsContext gc) {
        drawPitch(gc);
        drawScoreboard(gc);
        drawGoal(gc);
        drawKeeper(gc);
        drawShooter(gc);
        drawFanAim(gc);
        drawBall(gc, ballX, ballY, 16);
        drawGoalCheer(gc);
        drawShootoutInstructions(gc);
    }

    void renderResult(GraphicsContext gc) {
        drawPitch(gc);
        drawScoreboard(gc);
        drawGoal(gc);
        drawKeeper(gc);
        drawBall(gc, WINDOW_WIDTH / 2, 428, 18);
        drawGoalCheer(gc);

        boolean tie = scores[0] == scores[1];
        String title = tie ? "BOTH WIN!"
                : (scores[0] > scores[1] ? "PLAYER 1 WINS" : "PLAYER 2 WINS");
        Color winnerColor = tie ? Color.web("#ffe98a")
                : (scores[0] > scores[1] ? TeamColors.p1 : TeamColors.p2);
        gc.setFill(new LinearGradient(0, 150, 0, 390, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 12, 22, 0.86)),
                new Stop(1, Color.rgb(12, 36, 22, 0.82))));
        gc.fillRoundRect(230, 150, 500, 250, 18, 18);
        gc.setStroke(Color.web("#ffe98a", 0.70));
        gc.setLineWidth(3);
        gc.strokeRoundRect(230, 150, 500, 250, 18, 18);
        gc.setStroke(Color.web("#ffffff", 0.20));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(242, 162, 476, 226, 14, 14);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#ffe98a", 0.18));
        gc.fillOval(WINDOW_WIDTH / 2 - 170, 168, 340, 78);
        gc.setFill(winnerColor);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 40));
        gc.fillText(title, WINDOW_WIDTH / 2, 222);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 38));
        gc.fillText(scores[0] + " - " + scores[1], WINDOW_WIDTH / 2, 280);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        gc.setFill(Color.web("#ffe98a"));
        gc.fillText("Level 3 Time: " + formatTime(levelTimeMs), WINDOW_WIDTH / 2, 330);
        gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 18));
        gc.setFill(Color.web("#eef3f9"));
        gc.fillText("Press R to play again     Esc menu", WINDOW_WIDTH / 2, 374);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawPitch(GraphicsContext gc) {
        // bright daytime sky over a sunny pitch — same palette as the levels
        gc.setFill(new LinearGradient(0, 0, 0, WINDOW_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0,    Color.web("#5bb8e8")),
                new Stop(0.16, Color.web("#bfe9ff")),
                new Stop(0.24, Color.web("#eafaff")),
                new Stop(0.25, Color.web("#5aa83f")),
                new Stop(1,    Color.web("#7ccb4c"))));
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // sunny grandstand band with a colourful crowd
        gc.setFill(Color.web("#e8eef4", 0.92));
        gc.fillRect(0, 0, WINDOW_WIDTH, 66);
        Color[] crowd = { Color.web("#f4d03f"), Color.web("#58d68d"),
                          Color.web("#e74c3c"), Color.web("#3498db"), Color.web("#ec407a") };
        for (int x = -8; x < WINDOW_WIDTH; x += 22) {
            int m = Math.floorMod(x / 22, crowd.length);
            gc.setFill(crowd[m]);
            gc.fillOval(x, 10 + (m % 3) * 10, 9, 9);
            gc.fillOval(x, 34 + ((m + 1) % 3) * 8, 9, 9);
        }
        gc.setFill(Color.web("#ffffff", 0.85));
        gc.fillRect(0, 66, WINDOW_WIDTH, 5);        // stand rail

        // pitch mow stripes
        for (int y = 152; y < WINDOW_HEIGHT; y += 70) {
            gc.setFill((y / 70) % 2 == 0 ? Color.web("#8fe063", 0.28) : Color.web("#3f9e2e", 0.16));
            gc.fillRect(0, y, WINDOW_WIDTH, 70);
        }

        // white pitch markings
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(4);
        gc.strokeLine(0, 415, WINDOW_WIDTH, 415);
        gc.strokeArc(300, 365, 360, 160, 0, 180, javafx.scene.shape.ArcType.OPEN);
        gc.strokeRect(118, 152, 724, 188);
        gc.strokeLine(0, 152, WINDOW_WIDTH, 152);
        gc.strokeRect(285, 152, 390, 82);
    }

    private void drawScoreboard(GraphicsContext gc) {
        drawScoreBox(gc, 16, 16, "PLAYER 1", 0, TeamColors.p1);
        drawScoreBox(gc, WINDOW_WIDTH - 216, 16, "PLAYER 2", 1, TeamColors.p2);

        gc.setFill(Color.rgb(20, 24, 40, 0.55));
        gc.fillRoundRect(326, 22, 308, 66, 14, 14);
        gc.setStroke(Color.web("#ffffff", 0.30));
        gc.setLineWidth(2);
        gc.strokeRoundRect(326, 22, 308, 66, 14, 14);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#ffe98a"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 17));
        gc.fillText("FINAL SHOOTOUT", WINDOW_WIDTH / 2, 42);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        gc.fillText("BEST OF 3 EACH", WINDOW_WIDTH / 2, 62);
        gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 11));
        gc.setFill(Color.web("#dfe6ee"));
        gc.fillText("Three kicks each — a tie shares the cup", WINDOW_WIDTH / 2, 78);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawScoreBox(GraphicsContext gc, double x, double y, String label, int playerIndex, Color color) {
        double boxW = 200;
        gc.setFill(Color.rgb(20, 24, 40, 0.55));
        gc.fillRoundRect(x, y, boxW, 86, 8, 8);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.strokeRoundRect(x, y, boxW, 86, 8, 8);
        gc.setFill(color);
        gc.fillOval(x + 12, y + 11, 36, 36);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 22));
        gc.fillText(label, x + 58, y + 37);
        drawShotMarkers(gc, x + 68, y + 56, playerIndex);
    }

    private void drawShotMarkers(GraphicsContext gc, double x, double y, int playerIndex) {
        for (int i = 0; i < REGULATION_SHOTS; i++) {
            double cx = x + i * 46;
            drawBall(gc, cx, y, 15);
            int result = regulationResults[playerIndex][i];
            if (result == 0) continue;

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 24));
            gc.setFill(result > 0 ? Color.web("#7CFC8A") : Color.web("#ff6b7a"));
            gc.fillText(result > 0 ? "✓" : "X", cx, y + 9);
            gc.setTextAlign(TextAlignment.LEFT);
        }
    }

    private void drawGoal(GraphicsContext gc) {
        double goalX = GOAL_LEFT;
        double goalW = GOAL_RIGHT - GOAL_LEFT;
        gc.setFill(Color.rgb(8, 12, 20, 0.18));
        gc.fillRect(goalX + 8, 82, goalW - 16, 132);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(5);
        gc.strokeRect(goalX, 86, goalW, 128);
        gc.setLineWidth(1.5);
        gc.setStroke(Color.web("#ffffff", 0.62));
        for (double x = goalX + 20; x < GOAL_RIGHT; x += 24) {
            gc.strokeLine(x, 88, x, 214);
        }
        for (double y = 104; y < 214; y += 18) {
            gc.strokeLine(goalX, y, GOAL_RIGHT, y);
        }
        gc.setLineWidth(4);
        gc.setStroke(Color.web("#d7dee6"));
        gc.strokeLine(goalX - 24, 222, GOAL_RIGHT + 24, 222);
    }

    private void drawKeeper(GraphicsContext gc) {
        double x = keeperX;
        double bob = Math.sin(sceneTime * 11) * 2;
        double reach = getKeeperReach();

        // save-range indicator on the goal line
        gc.setStroke(Color.web("#7CFC8A", 0.30));
        gc.setLineWidth(3);
        gc.strokeLine(x - reach, 224, x + reach, 224);

        // opponent kitty keeper: wears the OTHER player's kit (the one not shooting),
        // facing the shooter and lunging sideways on a dive
        Color keeperKit = currentPlayer == 0 ? TeamColors.p2 : TeamColors.p1;
        double w = 46;
        double spriteH = 20 * (w / 16.0);
        double feetY = 224 + bob;
        double lunge = keeperDir * keeperDive * 22;   // dives toward the shot
        gc.save();
        gc.translate(x + lunge, feetY - spriteH / 2);
        gc.rotate(keeperDir * keeperDive * 20);        // tilts into the dive
        Player.drawFrontSprite(gc, -w / 2, -spriteH / 2, w, keeperKit);
        gc.restore();
    }

    // The shooter: the kitty sprite drawn from behind in the shooting player's kit,
    // leaning toward the ball as the shot resolves.
    private void drawShooter(GraphicsContext gc) {
        Color kit = currentPlayer == 0 ? TeamColors.p1 : TeamColors.p2;
        double w = 52;                       // sprite body width
        double spriteH = 20 * (w / 16.0);    // sprite is 16 wide x 20 tall
        double cx = WINDOW_WIDTH / 2 - 44;   // stands just behind-left of the ball
        double feetY = 524;
        double lean = resolving ? (1 - Math.max(0, feedbackTimer)) * 10 : 0;  // step into the kick

        // ground shadow
        gc.setFill(Color.rgb(0, 0, 0, 0.22));
        gc.fillOval(cx - 24 + lean, feetY - 8, 56, 14);

        Player.drawBackSprite(gc, cx - w / 2 + lean, feetY - spriteH, w, kit);
    }

    private void drawFanAim(GraphicsContext gc) {
        double startX = BALL_START_X;
        double startY = BALL_START_Y - 8;
        double targetX = getAimTargetX();
        double targetY = GOAL_Y + 138;

        // the aim line itself (sweeps left/right; shoot to fire where it points)
        gc.setStroke(Color.web("#ffe98a", 0.28));
        gc.setLineWidth(10);
        gc.strokeLine(startX, startY, targetX, targetY);
        gc.setStroke(Color.web("#ffe98a"));
        gc.setLineWidth(4.5);
        gc.strokeLine(startX, startY, targetX, targetY);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.4);
        gc.strokeLine(startX, startY, targetX, targetY);

        // target tick on the goal line where the shot will land
        gc.setStroke(Color.web("#ffe98a"));
        gc.setLineWidth(3);
        gc.strokeLine(targetX - 24, GOAL_Y + 150, targetX + 24, GOAL_Y + 150);
    }

    // The shoot-out's soccer ball — same design as the Level 3 collectible, centred at (x,y).
    private void drawBall(GraphicsContext gc, double x, double y, double r) {
        Ball.draw(gc, x, y, r);
    }

    private void drawGoalCheer(GraphicsContext gc) {
        if (cheerTimer <= 0) return;

        // celebration (screen tint, glow, confetti) only when it's a GOAL
        if (lastGoal) {
            double t = 1.0 - cheerTimer;
            double glow = Math.sin(Math.min(1.0, t) * Math.PI);
            gc.setFill(Color.rgb(255, 245, 160, 0.16 * glow));
            gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

            gc.setFill(new Color(cheerColor.getRed(), cheerColor.getGreen(), cheerColor.getBlue(), 0.18 * glow));
            gc.fillOval(WINDOW_WIDTH / 2 - 260 * glow, 58 - 34 * glow, 520 * glow, 150 * glow);

            for (int i = 0; i < 28; i++) {
                double seed = i * 17.31;
                double px = 105 + ((i * 73) % 760);
                double py = 78 + ((i * 29) % 72) + t * (46 + (i % 5) * 9);
                double drift = Math.sin(sceneTime * 4 + seed) * 18;
                Color particle = (i % 3 == 0) ? cheerColor : (i % 3 == 1 ? Color.web("#ffe98a") : Color.WHITE);
                gc.setFill(new Color(particle.getRed(), particle.getGreen(), particle.getBlue(), Math.max(0, 0.82 - t * 0.75)));
                gc.fillOval(px + drift, py, 5 + (i % 4), 5 + (i % 4));
            }
        }

        // big centre label: GOAL! on a score, SAVED! on a stop
        double alpha = Math.min(1.0, cheerTimer / 0.4);   // hold, then quick fade-out
        String label = lastGoal ? "GOAL!" : "SAVED!";
        Color labelColor = lastGoal ? Color.web("#ffe98a") : Color.web("#ff8a97");
        gc.setGlobalAlpha(alpha);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 52));
        gc.setFill(Color.rgb(0, 0, 0, 0.40));
        gc.fillText(label, WINDOW_WIDTH / 2 + 3, 246 + 3);
        gc.setFill(labelColor);
        gc.fillText(label, WINDOW_WIDTH / 2, 246);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setGlobalAlpha(1);
    }

    private void drawShootoutInstructions(GraphicsContext gc) {
        // instructions box, centred below the character
        double bx = BALL_START_X - 135, by = 530, bw = 270, bh = 60;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.rgb(20, 24, 40, 0.55));
        gc.fillRoundRect(bx, by, bw, bh, 14, 14);
        gc.setStroke(Color.web("#ffffff", 0.25));
        gc.setLineWidth(2);
        gc.strokeRoundRect(bx, by, bw, bh, 14, 14);
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        gc.setFill(currentPlayer == 0 ? TeamColors.p1 : TeamColors.p2);
        gc.fillText("PLAYER " + (currentPlayer + 1) + " SHOOTS", BALL_START_X, by + 26);
        // the prompt only — the GOAL!/SAVED! result now flashes in the centre instead
        if (!resolving) {
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 15));
            gc.setFill(Color.web("#ffe98a"));
            gc.fillText(message, BALL_START_X, by + 48);
        }
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private double getKeeperSpeed() {
        return 300;
    }

    private double getAimSpeed() {
        return 6.5;
    }

    private double getKeeperReach() {
        return BASE_KEEPER_REACH;
    }

    private double getAimTargetX() {
        return getLaneTargetX(aimLane);
    }

    private double getLaneTargetX(double lane) {
        double usableLeft = GOAL_LEFT + 34;
        double usableRight = GOAL_RIGHT - 34;
        double fraction = lane / (double) (AIM_LANES - 1);
        return usableLeft + (usableRight - usableLeft) * fraction;
    }

}
