package com.assignment;

import com.assignment.core.InputManager;
import com.assignment.core.RecordStore;
import com.assignment.core.TeamColors;
import com.assignment.levels.Level;
import com.assignment.levels.Level1;
import com.assignment.levels.Level2;
import com.assignment.levels.Level3;
import com.assignment.objects.Ball;
import com.assignment.objects.Player2;
import com.assignment.objects.Player;
import com.assignment.objects.Player1;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Scale;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class GameApp extends Application {
    public static final double WINDOW_WIDTH = 960;
    public static final double WINDOW_HEIGHT = 600;
    private static final int TOTAL_LEVELS = 3;
    // Seconds the "LEVEL CLEARED!" banner holds before auto-advancing to the next level.
    private static final double AUTO_NEXT_DELAY = 2.0;

    private final InputManager input = new InputManager();
    private final RecordStore records = new RecordStore();

    private final StackPane root = new StackPane();
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer loop;

    private enum GameMode { PLAYING_LEVEL, FINAL_SHOOTOUT, SHOOTOUT_RESULT }
    private GameMode mode = GameMode.PLAYING_LEVEL;
    private FinalShootout shootout;
    private long finalLevelTimeMs = 0;   // Level 3 finish time, shown on the shoot-out result

    private Level currentLevel;
    private int currentLevelIndex = 1;
    private long lastNanoTime = -1;
    private boolean resetHeld = false;
    private boolean escHeld = false;

    private boolean submitted = false;   // best-time recorded for the current level?
    private boolean newBest = false;     // was the current clear a new record?
    private double clearedTimer = 0;     // time since this level was cleared (drives auto-advance)
    private boolean helpOpen = false;    // is the "how to clear this match" panel showing? (pauses play)

    // Clickable "?" help badge, sitting just right of the centred MATCH pill.
    private static final double HELP_BTN_X = 596, HELP_BTN_Y = 12, HELP_BTN_W = 30, HELP_BTN_H = 30;

    // ---- animated menu scene ----
    private Canvas menuCanvas;
    private GraphicsContext menuGc;
    private AnimationTimer menuLoop;
    private long menuLastNano = -1;
    private double menuTime = 0;           // seconds, drives all menu animation
    private Player menuP1, menuP2;         // mascots, in the players' chosen kits
    private Ball menuBall;                 // football bouncing between them
    // Top of the grassy pitch strip in the menu scene.
    private static final double MENU_GRASS_Y = 506;

    @Override
    public void start(Stage stage) {
        canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // Mouse clicks land in the canvas's own 960x600 logical space (the window
        // scale lives on a parent transform), so we can hit-test the HUD directly.
        // Click the "?" badge to open the match's how-to panel; click anywhere to
        // close it. While it's open, the game is paused so nobody dies mid-read.
        canvas.setOnMouseClicked(e -> {
            if (helpOpen) { helpOpen = false; canvas.requestFocus(); return; }
            if (currentLevel == null || currentLevel.getMatchHelp() == null) return;
            if (currentLevel.isCompleted() || currentLevel.isFailed()) return;
            double mx = e.getX(), my = e.getY();
            if (mx >= HELP_BTN_X && mx <= HELP_BTN_X + HELP_BTN_W
                    && my >= HELP_BTN_Y && my <= HELP_BTN_Y + HELP_BTN_H) {
                helpOpen = true;
            }
        });

        // Everything is laid out / drawn at a fixed 960x600 logical size; we then
        // scale that whole thing uniformly to fill the window. One even zoom means
        // nothing shifts relative to anything else — the alignment stays identical,
        // it just gets bigger (with letterbox bars if the window's ratio differs).
        root.setMinSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setMaxSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        Scale scale = new Scale(1, 1, 0, 0);
        root.getTransforms().add(scale);

        StackPane outer = new StackPane(new Group(root));
        outer.setStyle("-fx-background-color: black;");

        Scene scene = new Scene(outer, 1280, 800);
        input.attach(scene);

        Runnable fit = () -> {
            double s = Math.min(scene.getWidth() / WINDOW_WIDTH,
                                scene.getHeight() / WINDOW_HEIGHT);
            scale.setX(s);
            scale.setY(s);
        };
        scene.widthProperty().addListener((o, a, b) -> fit.run());
        scene.heightProperty().addListener((o, a, b) -> fit.run());

        stage.setTitle("Meowhen United");
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
        fit.run();   // apply the initial scale

        menuCanvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        menuGc = menuCanvas.getGraphicsContext2D();

        buildLoop();
        buildMenuLoop();
        showMenu();
    }

    // Drives the animated menu scene (clouds, hovering mascots, bouncing ball,
    //  waving bunting). Runs only while the main menu is on screen.
    private void buildMenuLoop() {
        menuLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (menuLastNano < 0) { menuLastNano = now; return; }
                double dt = (now - menuLastNano) / 1_000_000_000.0;
                if (dt > 0.05) dt = 0.05;
                menuLastNano = now;
                menuTime += dt;
                if (menuBall != null) menuBall.update(dt);
                drawMenuScene(menuGc, menuTime);
            }
        };
    }

    // ---------------------------------------------------------------- screens

    private void setContent(Parent content) {
        root.getChildren().setAll(content);
    }

    private void showMenu() {
        loop.stop();
        input.clear();

        // mascots wear the players' currently-chosen kits; rebuilt each time we
        // return to the menu so kit changes show up. They stand on the pitch and
        // hover gently (the bob is applied at draw time).
        menuP1 = new Player1(372, MENU_GRASS_Y - 40);
        menuP2 = new Player2(556, MENU_GRASS_Y - 40);
        menuP1.setOnGround(true);
        menuP2.setOnGround(true);
        menuBall = new Ball(465, MENU_GRASS_Y - 42);
        menuTime = 0;
        menuLastNano = -1;

        Label title = new Label("MEOWHEN UNITED");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 60));
        title.setTextFill(Color.web("#fff3b0"));
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.55), 10, 0, 0, 4);");

        // invisible spacer setting the vertical gap below the title
        Label spacer = new Label();
        spacer.setMinHeight(24);
        spacer.setPrefHeight(24);

        Button start = menuButton("▶  KICK OFF", "#2ecc71");
        start.setOnAction(e -> showTeamSelect());

        Button view = menuButton("🏆  BEST TIMES", "#3498db");
        view.setOnAction(e -> showRecords());

        Button howto = menuButton("❔  HOW TO PLAY", "#e67e22");
        howto.setOnAction(e -> showHowToPlay());

        VBox box = new VBox(16, title, spacer, start, view, howto);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(52, 40, 40, 40));
        box.setStyle("-fx-background-color: transparent;");   // let the animated scene show through

        setContent(new StackPane(menuCanvas, box));
        drawMenuScene(menuGc, 0);     // paint frame 0 immediately (no flash)
        menuLoop.start();
    }

    // Static how-to-play screen: controls and the co-op idea.
    private void showHowToPlay() {
        loop.stop();
        menuLoop.stop();
        input.clear();

        Label title = new Label("HOW TO PLAY");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 44));
        title.setTextFill(Color.web("#fff3b0"));
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 8, 0, 0, 3);");

        VBox lines = new VBox(8);
        lines.setAlignment(Pos.CENTER_LEFT);
        lines.setMaxWidth(680);
        String[][] rows = {
            {"Goal",      "Team up through the matches, collect every football, and open the Cup portal."},
            {"Shoot-out", "Then a penalty shoot-out: 3 kicks each, taking turns. Winner takes the Cup (a tie = both win)."},
            {"Co-op",     "You can't win alone — stack, carry and boost each other, and share the keys/footballs."},
            {"Player 1",  "A / D move  ·  W jump"},
            {"Player 2",  "← / → move  ·  ↑ jump"},
            {"Colours",   "Blocks and rivers match their owner's kit — only that player uses them."},
            {"Stuck?",    "Every match has a  ❔  button (top bar) showing how to clear it."},
            {"Keys",      "R restart the current level  ·  Esc menu"},
        };
        for (String[] r : rows) {
            Label tag = new Label(r[0]);
            tag.setFont(Font.font("Verdana", FontWeight.BOLD, 15));
            tag.setTextFill(Color.web("#ffe98a"));
            tag.setMinWidth(84);
            Label val = new Label(r[1]);
            val.setFont(Font.font("Verdana", FontWeight.NORMAL, 14));
            val.setTextFill(Color.WHITE);
            val.setWrapText(true);
            val.setMaxWidth(560);
            HBox row = new HBox(12, tag, val);
            row.setAlignment(Pos.CENTER_LEFT);
            lines.getChildren().add(row);
        }

        VBox card = new VBox(lines);
        card.setPadding(new Insets(20, 26, 20, 26));
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color: rgba(20,24,40,0.55); -fx-background-radius: 18;");

        Button back = menuButton("BACK", "#3498db");
        back.setOnAction(e -> showMenu());

        VBox boxOuter = new VBox(16, title, card, back);
        boxOuter.setAlignment(Pos.CENTER);
        boxOuter.setPadding(new Insets(24));
        boxOuter.setStyle("-fx-background-color: linear-gradient(to bottom, #34618c, #5bb8e8);");
        setContent(boxOuter);
    }

    // Available team-kit swatches (lowercase hex, matched against TeamColors).
    private static final String[] KIT_COLORS = {
        "#f4d03f", "#58d68d", "#e74c3c", "#3498db",
        "#9b59b6", "#e67e22", "#ec407a", "#1abc9c",
    };

    // Pre-match screen: each player clicks a kit colour, then KICK OFF.
    private void showTeamSelect() {
        loop.stop();
        menuLoop.stop();
        input.clear();

        Label title = new Label("PICK YOUR KITS");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 46));
        title.setTextFill(Color.web("#fff3b0"));
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 8, 0, 0, 3);");

        Label p1Label = new Label();
        Label p2Label = new Label();
        p1Label.setFont(Font.font("Verdana", FontWeight.BOLD, 20));
        p2Label.setFont(Font.font("Verdana", FontWeight.BOLD, 20));

        List<Button> p1Btns = new ArrayList<>();
        List<Button> p2Btns = new ArrayList<>();
        HBox p1Row = new HBox(10);
        HBox p2Row = new HBox(10);
        p1Row.setAlignment(Pos.CENTER);
        p2Row.setAlignment(Pos.CENTER);

        Runnable refresh = () -> {
            for (int i = 0; i < KIT_COLORS.length; i++) {
                styleSwatch(p1Btns.get(i), KIT_COLORS[i], toHex(TeamColors.p1).equals(KIT_COLORS[i]));
                styleSwatch(p2Btns.get(i), KIT_COLORS[i], toHex(TeamColors.p2).equals(KIT_COLORS[i]));
            }
            p1Label.setText("PLAYER 1   (A / D move,  W jump)");
            p1Label.setTextFill(TeamColors.p1);
            p2Label.setText("PLAYER 2   (← / → move,  ↑ jump)");
            p2Label.setTextFill(TeamColors.p2);
        };

        for (String hex : KIT_COLORS) {
            Button b1 = swatchButton();
            b1.setOnAction(e -> { pickKit(true, hex); refresh.run(); });
            p1Btns.add(b1); p1Row.getChildren().add(b1);

            Button b2 = swatchButton();
            b2.setOnAction(e -> { pickKit(false, hex); refresh.run(); });
            p2Btns.add(b2); p2Row.getChildren().add(b2);
        }
        refresh.run();

        Button kick = menuButton("KICK OFF", "#2ecc71");
        kick.setOnAction(e -> startGame());
        Button back = menuButton("BACK", "#3498db");
        back.setOnAction(e -> showMenu());

        VBox box = new VBox(16, title, p1Label, p1Row, p2Label, p2Row, kick, back);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(36));
        box.setStyle("-fx-background-color: linear-gradient(to bottom, #5bb8e8, #bfe9ff 60%, #eafaff);");
        setContent(box);
    }

    // Sets a player's kit, swapping the other if they'd clash (must differ).
    private void pickKit(boolean player1, String hex) {
        Color c = Color.web(hex);
        if (player1) {
            if (c.equals(TeamColors.p2)) TeamColors.p2 = TeamColors.p1;
            TeamColors.p1 = c;
        } else {
            if (c.equals(TeamColors.p1)) TeamColors.p1 = TeamColors.p2;
            TeamColors.p2 = c;
        }
    }

    private Button swatchButton() {
        Button b = new Button();
        b.setPrefSize(48, 48);
        b.setMinSize(48, 48);
        return b;
    }

    private void styleSwatch(Button b, String hex, boolean selected) {
        String border = selected
                ? "-fx-border-color: white; -fx-border-width: 4; -fx-border-radius: 10;"
                : "-fx-border-color: rgba(0,0,0,0.30); -fx-border-width: 2; -fx-border-radius: 10;";
        b.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 10; -fx-cursor: hand;" + border);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private void showRecords() {
        loop.stop();
        menuLoop.stop();
        input.clear();

        Label title = new Label("BEST TIMES");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 44));
        title.setTextFill(Color.web("#fff3b0"));
        title.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 8, 0, 0, 3);");

        VBox rows = new VBox(12);
        rows.setAlignment(Pos.CENTER);
        for (int lvl = 1; lvl <= TOTAL_LEVELS; lvl++) {
            long best = records.getBest(lvl);
            String time = best < 0 ? "--:--.--" : formatTime(best);

            Label name = new Label("Level " + lvl);
            name.setFont(Font.font("Verdana", FontWeight.BOLD, 22));
            name.setTextFill(Color.WHITE);

            Label value = new Label(time);
            value.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
            value.setTextFill(best < 0 ? Color.web("#cdd6e0") : Color.web("#46e08a"));

            HBox row = new HBox(40, name, value);
            row.setAlignment(Pos.CENTER);
            row.setPadding(new Insets(10, 28, 10, 28));
            row.setStyle("-fx-background-color: rgba(20,24,40,0.45); -fx-background-radius: 14;");
            row.setMinWidth(360);
            rows.getChildren().add(row);
        }

        Button back = menuButton("BACK", "#3498db");
        back.setOnAction(e -> showMenu());

        VBox box = new VBox(26, title, rows, back);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.setStyle("-fx-background-color: linear-gradient(to bottom, #34618c, #5bb8e8);");

        setContent(box);
    }

    private Button menuButton(String label, String hex) {
        Button b = new Button(label);
        b.setFont(Font.font("Verdana", FontWeight.BOLD, 22));
        b.setPrefWidth(300);
        b.setPrefHeight(56);
        String base = "-fx-background-radius: 28; -fx-text-fill: white; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0, 0, 2);";
        b.setStyle("-fx-background-color: " + hex + ";" + base);
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: derive(" + hex + ", 18%);" + base));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + hex + ";" + base));
        return b;
    }

    // ------------------------------------------------------------------- game

    private void buildLoop() {
        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanoTime < 0) {
                    lastNanoTime = now;
                    return;
                }
                double dt = (now - lastNanoTime) / 1_000_000_000.0;
                if (dt > 0.05) dt = 0.05;
                lastNanoTime = now;

                handleControlKeys();

                // penalty shoot-out finale: once Level 3 is cleared, entering the Cup
                // portal opens into the shoot-out (this drives the whole end-game now).
                if (mode == GameMode.FINAL_SHOOTOUT) {
                    shootout.update(dt, input);
                    shootout.render(gc);
                    if (shootout.isComplete()) mode = GameMode.SHOOTOUT_RESULT;
                    return;
                }
                if (mode == GameMode.SHOOTOUT_RESULT) {
                    shootout.updateResult(dt);
                    shootout.renderResult(gc);
                    return;
                }

                // while the how-to panel is open the game is frozen (no update, no
                // timer, no respawns) so the players can read it safely
                if (!helpOpen) {
                    currentLevel.update(dt, input);

                    if (currentLevel.isCompleted() && !submitted) {
                        finalLevelTimeMs = currentLevel.getElapsedMillis();
                        newBest = records.submit(currentLevelIndex, finalLevelTimeMs);
                        submitted = true;
                        // both players reached the Level 3 portal -> kick off the shoot-out
                        if (currentLevelIndex == TOTAL_LEVELS) {
                            mode = GameMode.FINAL_SHOOTOUT;
                            shootout = new FinalShootout(finalLevelTimeMs);
                            input.clear();
                            return;
                        }
                    }

                    // auto-advance to the next level shortly after clearing (so the
                    // "LEVEL CLEARED!" banner is still seen) -- no key press needed
                    if (currentLevel.isCompleted() && currentLevelIndex < TOTAL_LEVELS) {
                        clearedTimer += dt;
                        if (clearedTimer >= AUTO_NEXT_DELAY) {
                            currentLevelIndex++;
                            loadLevel(currentLevelIndex);
                        }
                    }
                }

                currentLevel.render(gc);
                drawHud(gc);
                if (helpOpen) drawHelpOverlay(gc);
            }
        };
    }

    private void startGame() {
        menuLoop.stop();
        mode = GameMode.PLAYING_LEVEL;
        shootout = null;
        finalLevelTimeMs = 0;
        currentLevelIndex = 1;
        loadLevel(currentLevelIndex);
        setContent(new StackPane(canvas));
        canvas.requestFocus();
        lastNanoTime = -1;
        loop.start();
    }

    private void loadLevel(int index) {
        currentLevel = buildLevel(index);
        submitted = false;
        newBest = false;
        clearedTimer = 0;
        helpOpen = false;
    }

    private void handleControlKeys() {
        boolean escDown = input.isDown(KeyCode.ESCAPE);
        if (escDown && !escHeld) {
            if (helpOpen) {       // Esc first closes the how-to panel, not the level
                helpOpen = false;
            } else {
                showMenu();
            }
            escHeld = true;
            return;
        }
        escHeld = escDown;

        boolean rDown = input.isDown(KeyCode.R);
        if (rDown && !resetHeld) {
            if (mode == GameMode.SHOOTOUT_RESULT) {
                startGame();                       // play the whole run again from Level 1
            } else if (mode == GameMode.PLAYING_LEVEL) {
                loadLevel(currentLevelIndex);
            }
        }
        resetHeld = rDown;
    }

    private Level buildLevel(int index) {
        return switch (index) {
            case 2 -> new Level2(WINDOW_WIDTH, WINDOW_HEIGHT);
            case 3 -> new Level3(WINDOW_WIDTH, WINDOW_HEIGHT);
            default -> new Level1(WINDOW_WIDTH, WINDOW_HEIGHT);
        };
    }

    // -------------------------------------------------------------------- hud

    private void drawHud(GraphicsContext gc) {
        // info panel (kept narrow enough to clear the centred MATCH pill at x370)
        gc.setFill(Color.rgb(20, 24, 40, 0.55));
        gc.fillRoundRect(10, 10, 300, 96, 16, 16);
        gc.setStroke(Color.web("#ffffff", 0.25));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(10, 10, 300, 96, 16, 16);

        gc.setFill(Color.web("#ffe98a"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 15));
        gc.fillText(currentLevel.getName(), 24, 32);

        gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 13));
        gc.setFill(TeamColors.p1);
        gc.fillText("Player 1:  A / D move,  W jump", 24, 56);
        gc.setFill(TeamColors.p2);
        gc.fillText("Player 2:  ← / → move,  ↑ jump", 24, 76);
        gc.setFill(Color.web("#dfe6ee"));
        gc.fillText("R reset level   Esc menu", 24, 96);

        // timer panel (top-right)
        long best = records.getBest(currentLevelIndex);
        gc.setFill(Color.rgb(20, 24, 40, 0.55));
        gc.fillRoundRect(WINDOW_WIDTH - 220, 10, 210, 60, 16, 16);
        gc.setStroke(Color.web("#ffffff", 0.25));
        gc.strokeRoundRect(WINDOW_WIDTH - 220, 10, 210, 60, 16, 16);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 26));
        gc.fillText("⏱ " + formatTime(currentLevel.getElapsedMillis()), WINDOW_WIDTH - 205, 40);
        gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 12));
        gc.setFill(Color.web("#cdd6e0"));
        gc.fillText("Best: " + (best < 0 ? "--:--.--" : formatTime(best)), WINDOW_WIDTH - 205, 60);

        drawMatchHud(gc);

        if (currentLevel.isFailed()) {
            drawOverlay(gc, "BOTH DIED", Color.web("#ff6b5e"), "Press R to restart", null);
        } else if (currentLevel.isCompleted()) {
            String time = formatTime(currentLevel.getElapsedMillis());
            String stamp = (newBest ? "NEW BEST!  " : "Time:  ") + time;
            if (currentLevelIndex < TOTAL_LEVELS) {
                drawOverlay(gc, "LEVEL CLEARED!", Color.web("#46e08a"), "Next level…", stamp);
            } else {
                drawOverlay(gc, "ALL LEVELS CLEARED!", Color.web("#ffd24a"), "Press R to play again", stamp);
            }
        }
    }

    // Centered "Match N / Total" counter, plus a transient banner that flashes
    //  the match name when the players cross into a new match.
    private void drawMatchHud(GraphicsContext gc) {
        if (currentLevel.getMatchCount() <= 0) return;
        int mn = currentLevel.getMatchNumber();
        int mc = currentLevel.getMatchCount();
        double cx = WINDOW_WIDTH / 2.0;

        // persistent counter pill — its border pulses gold when the match changes
        double pulse = currentLevel.getMatchBannerAlpha();
        double pw = 220;
        gc.setFill(Color.rgb(20, 24, 40, 0.55));
        gc.fillRoundRect(cx - pw / 2, 12, pw, 30, 14, 14);
        gc.setStroke(Color.web("#ffd24a", 0.25 + 0.70 * pulse));
        gc.setLineWidth(1.5 + 2.5 * pulse);
        gc.strokeRoundRect(cx - pw / 2, 12, pw, 30, 14, 14);
        gc.setFill(Color.web("#ffe98a"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("⚽  MATCH " + mn + " / " + mc, cx, 33);

        // portal-unlock collectibles pill: keys (Levels 1 & 2) or footballs (Level 3)
        int kt = currentLevel.getKeyTotal();
        boolean useKeys = kt > 0;
        int cc = useKeys ? currentLevel.getKeysCollected()  : currentLevel.getBallsCollected();
        int ct = useKeys ? kt                               : currentLevel.getBallTotal();
        if (ct > 0) {
            boolean done = cc >= ct;
            String label = (useKeys ? "🔑 " : "⚽ ") + cc + " / " + ct + (done ? "  PORTAL OPEN" : "");
            double bw = done ? 220 : 150;
            gc.setFill(Color.rgb(20, 24, 40, 0.55));
            gc.fillRoundRect(cx - bw / 2, 46, bw, 24, 12, 12);
            gc.setStroke(done ? Color.web("#7CFC8A", 0.55) : Color.web("#ffffff", 0.25));
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(cx - bw / 2, 46, bw, 24, 12, 12);
            gc.setFill(done ? Color.web("#7CFC8A") : Color.WHITE);
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
            gc.fillText(label, cx, 63);
        }

        // transient "kick off" banner
        double ba = currentLevel.getMatchBannerAlpha();
        if (ba > 0) {
            gc.setGlobalAlpha(ba);
            gc.setFill(Color.rgb(10, 14, 30, 0.80));
            gc.fillRoundRect(cx - 270, 150, 540, 104, 20, 20);
            gc.setStroke(Color.web("#ffd24a"));
            gc.setLineWidth(2.5);
            gc.strokeRoundRect(cx - 270, 150, 540, 104, 20, 20);
            gc.setFill(Color.web("#ffd24a"));
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 34));
            gc.fillText("MATCH " + mn, cx, 194);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
            gc.fillText(currentLevel.getMatchName(), cx, 228);
            gc.setGlobalAlpha(1);
        }
        gc.setTextAlign(TextAlignment.LEFT);

        drawHelpButton(gc);
    }

    // The "?" badge beside the MATCH pill — click it to open the how-to panel.
    private void drawHelpButton(GraphicsContext gc) {
        if (currentLevel.getMatchHelp() == null) return;
        if (currentLevel.isCompleted() || currentLevel.isFailed()) return;

        gc.setFill(Color.rgb(20, 24, 40, 0.55));
        gc.fillRoundRect(HELP_BTN_X, HELP_BTN_Y, HELP_BTN_W, HELP_BTN_H, 10, 10);
        gc.setStroke(Color.web("#ffd24a", 0.85));
        gc.setLineWidth(2);
        gc.strokeRoundRect(HELP_BTN_X, HELP_BTN_Y, HELP_BTN_W, HELP_BTN_H, 10, 10);
        gc.setFill(Color.web("#ffe98a"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("?", HELP_BTN_X + HELP_BTN_W / 2, HELP_BTN_Y + HELP_BTN_H / 2 + 7);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    // Full-screen "how to clear this match" panel (shown while the game is paused).
    private void drawHelpOverlay(GraphicsContext gc) {
        gc.setFill(Color.rgb(8, 12, 26, 0.82));
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        double pw = 660, ph = 360;
        double px = (WINDOW_WIDTH - pw) / 2, py = (WINDOW_HEIGHT - ph) / 2;
        gc.setFill(Color.rgb(20, 24, 40, 0.97));
        gc.fillRoundRect(px, py, pw, ph, 22, 22);
        gc.setStroke(Color.web("#ffd24a"));
        gc.setLineWidth(2.5);
        gc.strokeRoundRect(px, py, pw, ph, 22, 22);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#ffd24a"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        gc.fillText("HOW TO CLEAR THIS MATCH", WINDOW_WIDTH / 2, py + 38);

        gc.setFill(Color.web("#ffe98a"));
        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 22));
        gc.fillText("MATCH " + currentLevel.getMatchNumber() + " — " + currentLevel.getMatchName(),
                WINDOW_WIDTH / 2, py + 72);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.web("#eef3f9"));
        gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 16));
        String help = currentLevel.getMatchHelp();
        double ly = py + 116;
        for (String line : (help == null ? "" : help).split("\n")) {
            gc.fillText(line, px + 34, ly);
            ly += 28;
        }

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#9fb0c3"));
        gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 13));
        gc.fillText("click anywhere (or press Esc) to close", WINDOW_WIDTH / 2, py + ph - 22);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    // ---------------------------------------------------------- animated menu scene

    // Paints the whole animated kick-off scene behind the menu buttons.
    private void drawMenuScene(GraphicsContext gc, double t) {
        double w = WINDOW_WIDTH, h = WINDOW_HEIGHT, gy = MENU_GRASS_Y;

        // sky
        gc.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#5bb8e8")),
                new Stop(0.55, Color.web("#bfe9ff")),
                new Stop(1, Color.web("#eafaff"))));
        gc.fillRect(0, 0, w, h);

        // sun + soft glow
        gc.setFill(Color.web("#fff7c2", 0.35));
        gc.fillOval(w - 170, 30, 150, 150);
        gc.setFill(Color.web("#ffe98a"));
        gc.fillOval(w - 146, 54, 100, 100);

        // rolling hills
        gc.setFill(Color.web("#bfe6b0"));
        for (double hx = -160; hx < w + 200; hx += 280) gc.fillOval(hx, h - 250, 360, 300);
        gc.setFill(Color.web("#92d27f"));
        for (double hx = -240; hx < w + 200; hx += 320) gc.fillOval(hx, h - 215, 400, 260);

        // drifting clouds (wrap across the screen)
        double span = w + 240;
        double[] cbx = {80, 300, 520, 760}, cby = {78, 132, 68, 150};
        for (int i = 0; i < cbx.length; i++) {
            double x = cbx[i] - (t * 16) % span;
            if (x < -120) x += span;
            drawMenuCloud(gc, x, cby[i]);
        }

        // World Cup bunting waving along the very top
        drawMenuBunting(gc, t, w);

        // grassy pitch strip
        gc.setFill(Color.web("#4f9a3a"));
        gc.fillRect(0, gy, w, h - gy);
        gc.setFill(Color.web("#58a83f"));
        for (double sx = 0; sx < w; sx += 96) gc.fillRect(sx, gy, 48, h - gy);
        gc.setFill(Color.web("#3f7e2e"));
        gc.fillRect(0, gy, w, 4);
        gc.setStroke(Color.web("#ffffff", 0.65));
        gc.setLineWidth(3);
        gc.strokeLine(w / 2, gy, w / 2, h);
        gc.strokeOval(w / 2 - 62, gy + 6, 124, 124);

        // mascots + ball: ground shadows, hovering players (P2 mirrored to face in),
        // and the football bouncing between them
        if (menuP1 != null && menuP2 != null) {
            drawMenuShadow(gc, menuP1.getX() + menuP1.getWidth() / 2, gy, 22);
            drawMenuShadow(gc, menuP2.getX() + menuP2.getWidth() / 2, gy, 22);
            renderMascot(gc, menuP1, Math.sin(t * 2.0) * 4, false);
            renderMascot(gc, menuP2, Math.sin(t * 2.0 + Math.PI) * 4, true);
        }
        if (menuBall != null) {
            double bounce = Math.abs(Math.sin(t * 2.6)) * 30;
            drawMenuShadow(gc, menuBall.getX() + menuBall.getWidth() / 2, gy, 14 - bounce * 0.18);
            gc.save();
            gc.translate(0, -bounce);
            menuBall.render(gc);
            gc.restore();
        }
    }

    private void drawMenuBunting(GraphicsContext gc, double t, double w) {
        String[] cols = {"#e74c3c", "#3498db", "#2ecc71", "#f4d03f", "#ffffff", "#9b59b6"};
        double pw = 46, ph = 30;
        gc.setStroke(Color.web("#2b2b2b", 0.45));
        gc.setLineWidth(2);
        gc.strokeLine(0, 14, w, 14);
        int i = 0;
        for (double x = 4; x < w; x += pw) {
            double wave = Math.sin(t * 2 + x * 0.05) * 4;
            gc.setFill(Color.web(cols[i % cols.length]));
            gc.fillPolygon(new double[]{x, x + pw - 6, x + (pw - 6) / 2},
                           new double[]{14, 14, 14 + ph + wave}, 3);
            i++;
        }
    }

    private void drawMenuCloud(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.web("#ffffff", 0.9));
        gc.fillOval(x, y + 10, 46, 30);
        gc.fillOval(x + 26, y, 56, 40);
        gc.fillOval(x + 60, y + 12, 44, 28);
        gc.fillOval(x + 18, y + 20, 80, 24);
    }

    private void drawMenuShadow(GraphicsContext gc, double cx, double gy, double r) {
        if (r <= 0) return;
        gc.setFill(Color.web("#000000", 0.18));
        gc.fillOval(cx - r, gy - 6, r * 2, 10);
    }

    // Draws a mascot with a vertical hover offset, optionally mirrored so it
    //  faces inward toward its partner.
    private void renderMascot(GraphicsContext gc, Player p, double bob, boolean flip) {
        gc.save();
        if (flip) {
            double cx = p.getX() + p.getWidth() / 2;
            gc.translate(cx, 0);
            gc.scale(-1, 1);
            gc.translate(-cx, 0);
        }
        gc.translate(0, bob);
        p.render(gc);
        gc.restore();
    }

    private void drawOverlay(GraphicsContext gc, String title, Color titleColor,
                             String subtitle, String stamp) {
        gc.setFill(new LinearGradient(0, 0, 0, WINDOW_HEIGHT, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(10, 12, 28, 0.55)),
                new Stop(1, Color.rgb(10, 12, 28, 0.78))));
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        gc.setFont(Font.font("Verdana", FontWeight.BOLD, 52));
        double titleWidth = textWidth(title, gc.getFont());
        double tx = (WINDOW_WIDTH - titleWidth) / 2;
        double ty = WINDOW_HEIGHT / 2 - 30;
        gc.setFill(Color.rgb(0, 0, 0, 0.45));
        gc.fillText(title, tx + 3, ty + 3);
        gc.setFill(titleColor);
        gc.fillText(title, tx, ty);

        if (stamp != null) {
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 30));
            gc.setFill(Color.web("#ffe98a"));
            gc.fillText(stamp, (WINDOW_WIDTH - textWidth(stamp, gc.getFont())) / 2, ty + 46);
        }

        gc.setFont(Font.font("Verdana", FontWeight.NORMAL, 20));
        gc.setFill(Color.web("#eef3f9"));
        gc.fillText(subtitle, (WINDOW_WIDTH - textWidth(subtitle, gc.getFont())) / 2,
                WINDOW_HEIGHT / 2 + 70);
    }

    private static double textWidth(String s, Font font) {
        Text t = new Text(s);
        t.setFont(font);
        return t.getLayoutBounds().getWidth();
    }

    // ms -> mm:ss.cc. Package-visible so the shoot-out screen can reuse it.
    static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long centis = (ms / 10) % 100;
        long secs = (ms / 1000) % 60;
        long mins = ms / 60000;
        return String.format("%02d:%02d.%02d", mins, secs, centis);
    }

    public static void main(String[] args) {
        launch(args);
    }
}