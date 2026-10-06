# JumpNRun x Minecraft 2D

A small Java 2D platformer created as a university programming-practicum project within 3 days. It combines classic jump-and-run gameplay with Minecraft-inspired elements.

## Features

- Side-scrolling levels loaded from bitmap images
- Player movement, jumping, combat, and animated sprites
- Coins, water, springboards, breakable blocks, and Creeper enemies
- Lives, score, pause, win, and restart states

## Requirements

- Java 17 or newer
- A desktop environment that supports Swing/AWT

The project has no external Java dependencies or build-tool configuration.

## Run

Run these commands from the project root:

```sh
javac -d out src/*.java
java -cp out Main
```

When the game starts, select a level bitmap. `testLevel.bmp` and `Tutorial.bmp` are included as sample levels. Keep the working directory at the project root so the game can find its `assets/` folder.

## Controls

| Key          | Action                                     |
| ------------ | ------------------------------------------ |
| Left / Right | Move                                       |
| Space        | Jump                                       |
| W / S        | Face up / down                             |
| F            | Punch                                      |
| E            | Sword attack                               |
| D            | Place a block collected by breaking blocks |
| P            | Toggle sound effects                       |
| Esc          | Pause / resume                             |
| R            | Restart after dying                        |
| Enter        | Restart after winning                      |

## Level format

Levels are bitmap images where each pixel represents one tile. The game identifies tiles by exact pixel colors; the current color mapping is implemented in `src/Level.java`. Use an uncompressed BMP image and preserve the mapped colors when editing or creating a level.

## Assets and licensing

The repository includes art, sprites, and sound effects. `assets/license.txt` documents the CC0 license for the Kenney platformer graphics described there. It does not establish the license for every other asset in the repository. Check the provenance and reuse terms for the remaining assets before redistributing this repository or its contents.

There is no `LICENSE` file for the source code, so no reuse license for the code is stated. Choose and add one if you want others to be able to reuse it.

## Project context

This is a completed student project rather than an actively maintained game. The source is kept intentionally close to its original scope and does not include automated tests or a packaged release.
