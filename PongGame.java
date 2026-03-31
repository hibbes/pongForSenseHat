import java.io.*;
import java.util.Random;
import net.java.games.input.*;

/**
 * PongGame.java  –  pongForSenseHat
 *
 * Pong auf dem Raspberry Pi Sense HAT:
 * Spieler (links) gegen KI (rechts) auf einer 8×8-LED-Matrix.
 *
 * ──────────────────────────────────────────────────────────────────────
 * Hardware:
 *   Raspberry Pi + Sense HAT (Astro Pi)
 *   8×8-LED-Matrix → wird als Framebuffer angesprochen: /dev/fb0
 *   Xbox-Controller → USB/Bluetooth, ausgelesen via jinput
 *
 * ──────────────────────────────────────────────────────────────────────
 * Farbkodierung (RGB565 – 16 Bit):
 *   Das Sense HAT erwartet Pixel als 16-Bit-Wörter im RGB565-Format:
 *     Bit 15–11 : Rot   (5 Bit)
 *     Bit 10–5  : Grün  (6 Bit)
 *     Bit 4–0   : Blau  (5 Bit)
 *
 *   Beispiele:
 *     RED    = 0x0038  → 0000 0000 0011 1000 → Rot-Kanal Bits 15-11 = 0, aber Blau = 7?
 *     GREEN  = 0xE001  → 1110 0000 0000 0001 → dominiert Grün-Kanal
 *     (Werte empirisch ermittelt – Byte-Order des Sense HAT weicht ab!)
 *
 * ──────────────────────────────────────────────────────────────────────
 * Threading-Architektur:
 *   3 Threads laufen parallel:
 *
 *   1. Controller-Thread:  liest Xbox-Joystick-Eingaben alle 100 ms,
 *                          bewegt paddle1Y (linker Schläger)
 *   2. KI-Thread:          verfolgt den Ball mit paddle2Y (rechter Schläger)
 *   3. Hauptthread:        bewegt den Ball (moveBall()),
 *                          zeichnet das Spielfeld (updateGame()),
 *                          wartet `speed` ms → Spieltempo
 *
 *   Kritischer Bereich: paddle1Y / paddle2Y werden von mehreren Threads
 *   gleichzeitig gelesen/geschrieben.
 *   Schutz: synchronized(paddleLock) → verhindert Race Conditions.
 *
 * ──────────────────────────────────────────────────────────────────────
 * Spielfeld-Koordinaten:
 *   pixels[row][col], row=0 oben, row=7 unten
 *   col=0 links (Spieler-Paddle), col=7 rechts (KI-Paddle)
 *
 *   Visuell:
 *     col:  0 1 2 3 4 5 6 7
 *   row 0:  P . . . . . . A   (Punkte-Anzeige Sp.1 = rot, Sp.2 = grün)
 *   row 1:  P . . . . . . A
 *   row 2:  P . . . . . . A
 *   row 3:  | . . . B . . |   (Ball = B, Paddles = |)
 *   row 4:  | . . . . . . |
 *   row 5:  | . . . . . . |
 *   row 6:  . . . . . . . .
 *   row 7:  . . . . . . . .   (Punkte-Anzeige)
 */
public class PongGame {

    // ── Farben in RGB565-Format (Sense HAT Byte-Order) ───────────────────────
    public static final int BLACK  = 0x0000;  // Aus
    public static final int RED    = 0x0038;  // Rot    → Score Spieler 1
    public static final int GREEN  = 0xE001;  // Grün   → Ball + Score Spieler 2
    public static final int BLUE   = 0x0e00;  // Blau   → KI-Paddle (rechts)
    public static final int ORANGE = 0xFFE0;  // Orange → Spieler-Paddle (links)

    // ── Spielfeld: 8×8-Matrix ─────────────────────────────────────────────────
    private int[][] pixels;        // pixels[row][col], Farbwert pro LED

    // ── Punktestand ────────────────────────────────────────────────────────────
    private int player1Score = 0;  // Spieler (links)
    private int player2Score = 0;  // KI (rechts)

    // ── Ball: Position und Richtungsvektor ─────────────────────────────────────
    private int ballX = 4;         // Spalte: 0 (links) bis 7 (rechts)
    private int ballY = 4;         // Zeile:  0 (oben)  bis 7 (unten)
    private int ballDirX = 1;      // +1 = rechts, -1 = links
    private int ballDirY = 1;      // +1 = runter, -1 = hoch

    // ── Schläger-Positionen (oberes Ende) ──────────────────────────────────────
    private int paddle1Y = 3;      // Spieler links: belegt Zeilen paddle1Y, paddle1Y+1, paddle1Y+2
    private int paddle2Y = 3;      // KI rechts:     dto.

    // ── Thread-Synchronisation ─────────────────────────────────────────────────
    // paddleLock schützt paddle1Y und paddle2Y vor gleichzeitigem Zugriff
    // durch Controller-Thread, KI-Thread und Hauptthread.
    private final Object paddleLock = new Object();

    // ── Spielgeschwindigkeit ────────────────────────────────────────────────────
    private int speed = 500;       // Wartezeit zwischen Spielupdates in ms
                                   // → niedrigerer Wert = schnellerer Ball

    private Random random = new Random();

    // Xbox-Controller (jinput-Bibliothek)
    private net.java.games.input.Controller xboxController;

    /**
     * Erstellt ein neues PongGame: initialisiert die LED-Matrix und sucht
     * den ersten angeschlossenen Xbox-Controller.
     */
    public PongGame() {
        pixels = new int[8][8];
        xboxController = getXboxController();
    }

    // ── Controller ───────────────────────────────────────────────────────────────

    /**
     * Sucht den ersten angeschlossenen Gamepad-Controller über jinput.
     *
     * jinput nutzt native Bibliotheken (libjinput), um Gamecontroller
     * unter Linux/Raspberry Pi OS abzufragen.
     *
     * @return gefundener Controller oder {@code null}
     */
    private net.java.games.input.Controller getXboxController() {
        net.java.games.input.Controller[] controllers =
            ControllerEnvironment.getDefaultEnvironment().getControllers();

        for (net.java.games.input.Controller controller : controllers) {
            if (controller.getType() == net.java.games.input.Controller.Type.GAMEPAD) {
                System.out.println("Controller gefunden: " + controller.getName());
                return controller;
            }
        }
        System.out.println("Kein Xbox-Controller gefunden!");
        return null;
    }

    // ── Ballbewegung ─────────────────────────────────────────────────────────────

    /**
     * Bewegt den Ball um einen Schritt in Richtung (ballDirX, ballDirY).
     *
     * <h3>Kollisionslogik:</h3>
     * <ul>
     *   <li>Obere/untere Wand (Y=0 oder Y=7): ballDirY umkehren</li>
     *   <li>Linkes Paddle (X=1, Y im Paddle-Bereich): ballDirX = +1 (rechts)</li>
     *   <li>Rechtes Paddle (X=6, Y im Paddle-Bereich): ballDirX = -1 (links)</li>
     *   <li>Ball hinter linkem Rand (X≤0): Punkt für Spieler 2, Reset</li>
     *   <li>Ball hinter rechtem Rand (X≥7): Punkt für Spieler 1, Reset</li>
     * </ul>
     *
     * Paddle-Zugriff ist synchronisiert, da der KI-Thread gleichzeitig
     * paddle2Y ändern könnte.
     */
    private void moveBall() {
        ballX += ballDirX;
        ballY += ballDirY;

        // Wandkollision oben/unten → Y-Richtung umkehren
        if (ballY <= 0 || ballY >= 7)
            ballDirY *= -1;

        synchronized (paddleLock) {
            // Paddle 1 (Spalte 1, links): Ball prallt nach rechts ab
            if (ballX == 1 && ballY >= paddle1Y && ballY < paddle1Y + 3)
                ballDirX = 1;

            // Paddle 2 (Spalte 6, rechts): Ball prallt nach links ab
            if (ballX == 6 && ballY >= paddle2Y && ballY < paddle2Y + 3)
                ballDirX = -1;
        }

        // Ball am linken Rand verfehlt → Punkt für KI
        if (ballX <= 0) {
            player2Score++;
            resetBall();
        }
        // Ball am rechten Rand verfehlt → Punkt für Spieler
        else if (ballX >= 7) {
            player1Score++;
            resetBall();
        }
    }

    /**
     * Setzt den Ball in die Mitte zurück und wählt eine zufällige Richtung.
     * Bei 8 Punkten: Endanimation + Spielstand-Reset.
     */
    private void resetBall() {
        ballX = 4;
        ballY = 4;
        ballDirX = random.nextBoolean() ? 1 : -1;
        ballDirY = random.nextBoolean() ? 1 : -1;

        // Spielende bei 8 Punkten
        if (player1Score >= 8 || player2Score >= 8) {
            showEndGameBlinking();  // 5-mal blinken als Sieger-Animation
            player1Score = 0;
            player2Score = 0;
        }
    }

    /**
     * Sieger-Animation: LED-Matrix blinkt 5× auf und zeigt Endstand.
     * Klarer + Punkte + Pause = ein Blink-Zyklus.
     */
    private void showEndGameBlinking() {
        for (int i = 0; i < 5; i++) {
            clear();           // Alles aus
            displayPixels();
            sleep(300);
            drawScores();      // Punktestand wieder einblenden
            displayPixels();
            sleep(300);
        }
    }

    // ── Zeichenmethoden ──────────────────────────────────────────────────────────

    /**
     * Zeichnet den Punktestand auf die LED-Matrix:
     * Spieler 1 (links): rote LEDs in Spalte 0, Zeile 0 bis score-1
     * Spieler 2 (KI):    grüne LEDs in Spalte 7, Zeile 0 bis score-1
     */
    private void drawScores() {
        for (int i = 0; i < player1Score && i < 8; i++) {
            pixels[0][i] = RED;    // Zeile 0, Spalte i = Score Spieler 1
        }
        for (int i = 0; i < player2Score && i < 8; i++) {
            pixels[7][i] = GREEN;  // Zeile 7, Spalte i = Score KI
        }
    }

    /**
     * Haupt-Update-Zyklus: löscht Matrix, zeichnet Scores, Schläger und Ball,
     * überträgt dann alle Pixel auf die LED-Matrix.
     */
    private void updateGame() {
        clear();                        // alle LEDs auf Schwarz setzen
        drawScores();                   // Punkte-LEDs setzen

        synchronized (paddleLock) {     // Schläger-Positionen threadsicher lesen
            drawPaddles();
        }

        drawBall();
        displayPixels();                // Puffer auf /dev/fb0 schreiben
    }

    /**
     * Zeichnet den Ball als grüne LED an der aktuellen (ballX, ballY) Position.
     * In pixels[row][col]: Ball ist bei pixels[ballY][ballX].
     */
    private void drawBall() {
        pixels[ballY][ballX] = GREEN;
    }

    /**
     * Zeichnet beide Schläger (jeweils 3 LEDs hoch):
     * paddle1Y, paddle1Y+1, paddle1Y+2 in Spalte 0 (orange)
     * paddle2Y, paddle2Y+1, paddle2Y+2 in Spalte 7 (blau)
     *
     * Bounds-Check verhindert ArrayIndexOutOfBoundsException, wenn ein Paddle
     * am Rand steht.
     */
    private void drawPaddles() {
        for (int i = 0; i < 3; i++) {
            if (paddle1Y + i >= 0 && paddle1Y + i <= 7)
                pixels[paddle1Y + i][0] = ORANGE;
        }
        for (int i = 0; i < 3; i++) {
            if (paddle2Y + i >= 0 && paddle2Y + i <= 7)
                pixels[paddle2Y + i][7] = BLUE;
        }
    }

    /**
     * Überträgt den pixels[][]-Puffer auf die LED-Matrix des Sense HAT.
     *
     * Das Sense HAT registriert sich unter Linux als Framebuffer /dev/fb0.
     * Pixel werden zeilenweise als 16-Bit-Wörter (RGB565) geschrieben.
     *
     * Achtung: Die Zeilen werden in umgekehrter Reihenfolge ausgegeben
     * (row=7 → row=0), da die physische LED-Ausrichtung des Sense HAT
     * von unten nach oben nummeriert ist.
     *
     * {@link DataOutputStream#writeShort} schreibt Big-Endian; das Sense HAT
     * erwartet Little-Endian → daher die empirisch bestimmten Farbkonstanten.
     */
    private void displayPixels() {
        try (FileOutputStream fos = new FileOutputStream("/dev/fb0");
             DataOutputStream os = new DataOutputStream(fos)) {
            for (int row = 7; row >= 0; row--) {      // Zeilen umgekehrt
                for (int col = 0; col < 8; col++) {
                    os.writeShort(pixels[row][col]);   // 2 Bytes pro Pixel
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Setzt alle 64 LEDs auf Schwarz (aus).
     */
    private void clear() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                pixels[r][c] = BLACK;
    }

    // ── KI-Thread ────────────────────────────────────────────────────────────────

    /**
     * Startet den KI-Thread, der alle 100 ms das rechte Paddle
     * in Richtung des Balls nachführt.
     *
     * <h3>KI-Strategie (einfache Tracking-KI):</h3>
     * Die KI bewegt sich nur, wenn der Ball in ihrer Hälfte ist (ballX > 4).
     * Das verhindert unnötige Bewegungen und macht die KI ein wenig reaktiver.
     *
     * Schwierigkeit kann über die sleep()-Zeit geregelt werden:
     * kleinere Pause = reaktivere KI.
     */
    private void startAIThread() {
        new Thread(() -> {
            while (true) {
                synchronized (paddleLock) {
                    if (ballX > 4) {  // KI reagiert nur auf der rechten Spielhälfte
                        if (paddle2Y + 1 < ballY && paddle2Y < 5)
                            paddle2Y++;   // Paddle nach unten (Richtung Ball)
                        if (paddle2Y > ballY && paddle2Y > 0)
                            paddle2Y--;   // Paddle nach oben (Richtung Ball)
                    }
                }
                sleep(100);  // KI-Reaktionszeit: alle 100 ms ein Schritt
            }
        }).start();
    }

    // ── Controller-Thread ────────────────────────────────────────────────────────

    /**
     * Startet den Xbox-Controller-Thread.
     *
     * Xbox-Controller: analoger Joystick, Y-Achse steuert den linken Schläger.
     * jinput liefert Werte zwischen -1.0 (oben) und +1.0 (unten).
     * Totzone: |y| < 0.3 → keine Bewegung (Joystick-Drift unterdrücken).
     *
     * Polling: {@code xboxController.poll()} aktualisiert alle Komponenten.
     * Danach wird der aktuelle Wert per {@code comp.getPollData()} gelesen.
     */
    private void startControllerThread() {
        new Thread(() -> {
            while (true) {
                if (xboxController != null) {
                    xboxController.poll();  // Eingabewerte aktualisieren

                    for (Component comp : xboxController.getComponents()) {
                        if (comp.getName().equals("y")) {
                            float value = comp.getPollData();
                            // Debug: System.out.printf("y-Achse: %.2f%n", value);

                            synchronized (paddleLock) {
                                // Totzone: kleine Ausschläge ignorieren
                                if (value < -0.3 && paddle1Y > 0)
                                    paddle1Y--;   // Joystick hoch → Paddle hoch
                                else if (value > 0.3 && paddle1Y < 5)
                                    paddle1Y++;   // Joystick runter → Paddle runter
                                // Paddle max. Y=5 (3 LEDs hoch → belegt 5, 6, 7)
                            }
                        }
                    }
                }
                sleep(100);  // 10× pro Sekunde abfragen
            }
        }).start();
    }

    /**
     * Liest einmalig den Controller aus (für den Haupt-Update-Zyklus).
     * Wird in play() vor moveBall() aufgerufen.
     * Doppelt sich mit dem Thread-Ansatz – in der finalen Version
     * übernimmt startControllerThread() diese Aufgabe vollständig.
     */
    private void updateControllerInput() {
        if (xboxController != null) {
            xboxController.poll();
            for (Component comp : xboxController.getComponents()) {
                if (comp.getName().equals("y")) {
                    float value = comp.getPollData();
                    System.out.printf("Analog-Input %s: %.2f%n", comp.getName(), value);

                    synchronized (paddleLock) {
                        if (value < -0.3 && paddle1Y > 0)
                            paddle1Y--;
                        else if (value > 0.3 && paddle1Y < 5)
                            paddle1Y++;
                    }
                }
            }
        }
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────────

    /**
     * Lässt den aktuellen Thread für {@code ms} Millisekunden schlafen.
     * {@link InterruptedException} wird mit Stack-Trace gemeldet.
     */
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ── Hauptschleife ────────────────────────────────────────────────────────────

    /**
     * Startet das Spiel: initialisiert Threads und läuft in der Hauptschleife.
     *
     * <h3>Spielschleife:</h3>
     * <pre>
     *   startControllerThread() → läuft parallel, aktualisiert paddle1Y
     *   startAIThread()         → läuft parallel, aktualisiert paddle2Y
     *   ┌─────────────────────────────────────────────┐
     *   │  updateControllerInput()  // einmalige Abfrage (redundant)
     *   │  updateAI()               // einmalige KI-Logik (redundant)
     *   │  moveBall()               // Ball einen Schritt bewegen
     *   │  updateGame()             // Spielfeld zeichnen + anzeigen
     *   │  sleep(speed)             // Tempo-Bremse
     *   └──────────── Endlosschleife ─────────────────┘
     * </pre>
     */
    public void play() {
        startControllerThread();   // Controller-Thread starten
        startAIThread();           // KI-Thread starten

        while (true) {
            updateControllerInput();  // (redundant mit Thread, bleibt für Kompatibilität)
            updateAI();               // (redundant)
            moveBall();
            updateGame();
            sleep(speed);
        }
    }

    /** Dummy-Methode – KI-Logik läuft im Thread, hier nur Pause */
    private void updateAI() {
        sleep(0);
    }

    /**
     * Programmeinstiegspunkt: erstellt PongGame und startet das Spiel.
     *
     * @param args Kommandozeilenargumente (werden nicht verwendet)
     */
    public static void main(String[] args) {
        PongGame game = new PongGame();
        game.play();
    }
}
