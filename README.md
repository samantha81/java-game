# ⚽ Meowhen United

A two-player, local co-op platformer built with **Java** and **JavaFX**. Two cat footballers share one keyboard, work through three puzzle-packed levels ("matches"), and finish with a penalty shoot-out for the Cup.

Nobody can clear a match alone. You have to stack, carry, boost and wait for each other, and you share the keys and footballs that unlock each level's portal.

> Coursework project, developed for an object-oriented programming assignment.

---

## Features

- **Local 2-player co-op** on a single keyboard
- **3 levels**, each split into multiple "matches" with their own puzzle and in-game hint (click the ❔ button in the top bar)
- **Penalty shoot-out finale** after Level 3: 3 kicks each, taking turns; most goals wins, a tie means both win
- **Team kit selection**: each player picks a colour, and that colour drives the rules (see below)
- **Best-time tracking** per level, saved locally
- **Animated main menu** and a resizable window that scales the whole scene uniformly (fixed 960×600 logical canvas)

### Puzzle mechanics

| Mechanic | Description |
|---|---|
| Colour-matched blocks and rivers | Each player can only push their own coloured block and is only safe in their own coloured river |
| Lasers and blink beams | Horizontal and vertical lasers, plus timed blink beams you must cross together |
| Shields | Top and front shield pickups that block the matching laser orientation |
| Co-op lift | Rises only while **both** players stand on it |
| Moving platforms | Ferries riders across gaps |
| Bounce pads | Launch pads for vertical gaps |
| Size pads | Floor buttons that grow (+) or shrink (−) a player |
| Carry and strike | Co-op tower and double-launch finale puzzle in Level 3 |
| Gates | Energy-field barriers that open once a match's key or football is collected |
| Keys and footballs | Collect every key (Levels 1–2) or football (Level 3) to open the portal |

---

## Controls

| Action | Player 1 | Player 2 |
|---|---|---|
| Move | `A` / `D` | `←` / `→` |
| Jump | `W` | `↑` |

| Global key | Action |
|---|---|
| `R` | Restart the current level |
| `Esc` | Return to the menu |
| `Space` | Shoot (during the penalty shoot-out, for whoever's turn it is) |
| Mouse | Click the ❔ badge for the current match's hint; click anywhere to close it |

---

## Getting Started

### Prerequisites

- **JDK 17 or newer** (recommended)
- **JavaFX SDK** matching your JDK (`javafx.controls` and `javafx.graphics` modules)
- Maven or Gradle, or the plain `javac`/`java` instructions below

### Project layout

```
src/main/java/
├── module-info.java
└── com/assignment/
    ├── Launcher.java          # Entry point
    ├── GameApp.java           # JavaFX Application: menus, game loop, HUD
    ├── FinalShootout.java     # Penalty shoot-out finale
    ├── core/                  # InputManager, TeamColors, RecordStore
    ├── levels/                # Level (base), Level1, Level2, Level3
    ├── objects/               # Players, blocks, platforms, gates, pads, portal, ...
    │   └── pickups/           # Shield, TopShield, FrontShield
    └── hazards/               # Lasers, blink beams, rivers
```

### Build and run

The archive contains source only (no `pom.xml` or `build.gradle`), so pick one of the options below.

**Option A: Maven or Gradle (recommended).** Create a standard project with the JavaFX plugin and set the main class to `com.assignment.Launcher`. With Maven's `javafx-maven-plugin`, for example:

```xml
<plugin>
  <groupId>org.openjfx</groupId>
  <artifactId>javafx-maven-plugin</artifactId>
  <version>0.0.8</version>
  <configuration>
    <mainClass>com.assignment/com.assignment.Launcher</mainClass>
  </configuration>
</plugin>
```

```bash
mvn clean javafx:run
```

**Option B: Command line.** Replace `/path/to/javafx-sdk/lib` with your JavaFX SDK path:

```bash
mkdir -p out
javac --module-path /path/to/javafx-sdk/lib \
      --add-modules javafx.controls,javafx.graphics \
      -d out $(find src/main/java -name "*.java")

java --module-path /path/to/javafx-sdk/lib:out \
     --add-modules javafx.controls,javafx.graphics \
     -m com.assignment/com.assignment.Launcher
```

> On Windows, use `;` instead of `:` in the module path and run the `javac` step with a file list (`dir /s /b *.java > sources.txt`, then `javac @sources.txt ...`).

**Option C: IDE.** Import as a Maven/Gradle project in IntelliJ IDEA or Eclipse, add the JavaFX SDK as a library, and run `com.assignment.Launcher`.

> Run `Launcher`, not `GameApp`. The separate launcher class lets the JavaFX module start cleanly.

---

## How to Play

1. From the main menu choose **KICK OFF**.
2. Each player picks a kit colour (the two colours must differ).
3. Work through each match together: collect every key (or football in Level 3) to open the **Cup portal**.
4. Both players must enter the portal to clear the level; the next level starts automatically after the "LEVEL CLEARED!" banner.
5. Clear Level 3 to reach the **penalty shoot-out**. Press `Space` to shoot.

Stuck? Click the ❔ button in the top bar for that match's solution hint.

### Best times

Fastest clear times per level are stored in `records.properties` in the working directory (created on first clear). Delete this file to reset records.

---

## Architecture Notes

- **Object-oriented design:** a `GameObject` / `Entity` / `StaticObject` hierarchy, with `Player` specialised into `Player1` and `Player2`, and `Hazard` into lasers, beams and rivers.
- **Level template:** the abstract `Level` class owns the shared update, collision and match-progress logic; `Level1`–`Level3` override `build()` to lay out their stages.
- **Rendering:** everything is drawn procedurally on a JavaFX `Canvas` driven by an `AnimationTimer`; no external image or audio assets are required.
- **Input:** `InputManager` tracks pressed keys so both players can move simultaneously.
- **Persistence:** `RecordStore` saves best times as a `.properties` file.

---

## License

All rights reserved. Academic coursework.
