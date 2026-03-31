# pongForSenseHat

Pong-Implementierung für den **Raspberry Pi SenseHat** (8×8 LED-Matrix) in Java.
Gespielt wird mit einem Joystick (SenseHat) oder Xbox-Controller.

## Features

| Klasse | Beschreibung |
|--------|-------------|
| `Pong.java` | Basisimplementierung: Ball + zwei manuelle Schläger |
| `PongGame.java` | KI-gesteuerter rechter Schläger, Xbox-Controller für links |
| `PongGameRef.java` | Refactoring-Referenz mit State-Machine (`GameState`) |
| `PongGameRef_old.java` | ältere Referenzversion |
| `AiPong.java` | Reine KI-KI-Simulation |
| `TwoPlayerPong.java` | Zwei menschliche Spieler (Joystick + Xbox) |
| `JoystickGPIO.java` | Joystick-Eingabe via GPIO (SenseHat) |
| `JoystickPixelControl.java` | Joystick steuert einzelne Pixel |
| `JoystickPixelController.java` | Erweiterter Pixel-Controller |
| `JoystickPixelMover.java` | Schneller Pixel-Mover |
| `MatrixAnimationDemo.java` | Demo-Animationen für die LED-Matrix |
| `FastPixelDemo.java` | Performanz-Demo: schnelles Pixel-Setzen |
| `TextAnimation.java` | Textlaufschrift auf der Matrix |
| `Waves.java` | Wellenanimation |
| `GameControllerTest.java` / `JoystickEventLogger.java` | Test-/Debug-Tools |
| `SenseHatTest.java` / `XboxControllerExample.java` | Hardware-Tests |

## Spielfeld

Das SenseHat hat eine **8×8 LED-Matrix**. Das Spielfeld wird als 2D-Array
`int[8][8]` verwaltet. Farben werden als 16-Bit-Werte gespeichert:

```java
public static final int BLACK  = 0x0000;
public static final int RED    = 0x0038;
public static final int GREEN  = 0xE001;
public static final int BLUE   = 0x0e00;
public static final int ORANGE = 0xFFE0;  // menschlicher Schläger
```

## Architektur (PongGame)

```
PongGame
├── Spielfeld: int[8][8] pixels
├── Ball: (ballX, ballY) + Richtung (ballDirX, ballDirY)
├── Paddle 1 (links): paddle1Y – gesteuert via Xbox-Controller
├── Paddle 2 (rechts): paddle2Y – KI (verfolgt den Ball)
└── Threads:
    ├── Ball-Thread: bewegt Ball, prüft Kollisionen
    ├── KI-Thread: aktualisiert Paddle 2
    └── Input-Thread: liest Xbox-Controller-Eingaben
```

## Multithreading

Das Spiel verwendet **drei parallele Threads**:
- **Ball-Loop**: `moveBall()` im 500ms-Takt (wird schneller mit der Zeit)
- **KI-Loop**: `moveAIPaddle()` verfolgt den Ball
- **Input-Loop**: `readXboxController()` verarbeitet Controller-Eingaben

Threads kommunizieren über `synchronized (paddleLock)` – das verhindert
gleichzeitiges Schreiben auf die Paddle-Position.

## Hardware-Voraussetzungen

- Raspberry Pi (beliebiges Modell) mit [SenseHat](https://www.raspberrypi.org/products/sense-hat/)
- [java-sense-hat.jar](java-sense-hat.jar) (im Repo enthalten)
- Optional: Xbox-Controller + `net.java.games.input`-Bibliothek

## Lernziele

- Multithreading: `Thread`, `Runnable`, `synchronized`
- Hardware-Programmierung auf dem Raspberry Pi
- Game-Loop-Muster
- Kollisionserkennung auf einem Rasterfeld
