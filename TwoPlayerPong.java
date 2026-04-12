import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Component;
import java.util.Random;

/**
 * TwoPlayerPong.java  –  pongForSenseHat
 *
 * Zwei-Spieler-Version des Pong-Spiels auf dem Raspberry Pi Sense HAT.
 * Beide Schläger werden von je einem Xbox-Controller gesteuert.
 *
 * ──────────────────────────────────────────────────────────────────────
 * Unterschied zu PongGame.java:
 *   PongGame:       Spieler vs. KI (1 Controller)
 *   TwoPlayerPong:  Spieler 1 vs. Spieler 2 (2 Controller)
 *
 * ──────────────────────────────────────────────────────────────────────
 * Threading:
 *   2 Controller-Threads (einer pro Spieler) + Hauptthread
 *   Jeder Controller-Thread heißt "Controller-N-Thread" (benannter Thread
 *   → leichter im Debugger zu identifizieren).
 *
 * ──────────────────────────────────────────────────────────────────────
 * Bekannte Einschränkung:
 *   getXboxController(int) unterscheidet die Controller NICHT anhand einer
 *   ID – beide Male wird der erste gefundene Gamepad-Controller zurückgegeben.
 *   Für echtes 2-Spieler-Spiel: Durchlaufzähler ergänzen, sodass
 *   Spieler 1 den 1. und Spieler 2 den 2. Controller bekommt.
 *
 * ──────────────────────────────────────────────────────────────────────
 * updateGame()-Methode ist ein Stub:
 *   Das Zeichnen (Framebuffer-Ausgabe) fehlt hier – in der Produktivversion
 *   muss es aus PongGame.java übernommen werden.
 */
public class TwoPlayerPong {

    // ── Controller ────────────────────────────────────────────────────────────
    private Controller xboxController1;  // linker Schläger
    private Controller xboxController2;  // rechter Schläger

    // ── Thread-Synchronisation ────────────────────────────────────────────────
    // paddleLock schützt paddle1Y und paddle2Y vor gleichzeitigem Schreiben
    // durch Controller-Thread 1, Controller-Thread 2 und Hauptthread.
    private final Object paddleLock = new Object();

    // ── Schläger-Positionen ───────────────────────────────────────────────────
    private int paddle1Y = 3;   // oberes Ende Schläger 1 (Zeile 3–5)
    private int paddle2Y = 3;   // oberes Ende Schläger 2 (Zeile 3–5)

    // ── Ball ──────────────────────────────────────────────────────────────────
    private int ballX = 4;      // Spalte des Balls (0=links, 7=rechts)
    private int ballY = 4;      // Zeile  des Balls (0=oben,  7=unten)
    private int ballDirX = 1;   // +1 = rechts, -1 = links
    private int ballDirY = 1;   // +1 = runter, -1 = hoch

    // ── Spieltempo ────────────────────────────────────────────────────────────
    private final int speed = 500;  // ms Pause zwischen Ball-Schritten

    private final Random random = new Random();

    // ── Punktestand ───────────────────────────────────────────────────────────
    private int player1Score = 0;
    private int player2Score = 0;

    // ── Framebuffer ───────────────────────────────────────────────────────────
    // 8×8-Matrix an RGB565-Werten, die bei jedem updateGame-Durchlauf an das
    // Sense HAT übertragen wird. Farben: in Java-Byte-Order vorberechnet.
    private static final int BLACK  = 0x0000;
    private static final int RED    = 0x0038;
    private static final int GREEN  = 0xE001;
    private static final int BLUE   = 0x0E00;
    private static final int ORANGE = 0xFFE0;
    private final int[][] pixels = new int[8][8];

    /**
     * Initialisiert das Spiel und sucht beide Controller.
     */
    public TwoPlayerPong() {
        xboxController1 = getXboxController(1);
        xboxController2 = getXboxController(2);
    }

    // ── Controller-Initialisierung ────────────────────────────────────────────

    /**
     * Gibt den ersten gefundenen Gamepad-Controller zurück.
     *
     * <h3>Bekannte Einschränkung:</h3>
     * Die Methode unterscheidet Controller 1 und 2 nicht anhand eines Index –
     * der Parameter {@code playerNumber} wird nur für die Fehlermeldung genutzt.
     * Beide Aufrufe geben denselben Controller zurück, wenn nur einer angeschlossen ist.
     *
     * Abhilfe: Einen Iterator über alle Controller führen und bei i-tem Treffer
     * zurückgeben (statt immer beim ersten aufzuhören).
     *
     * @param playerNumber  Spielernummer (1 oder 2) – nur für Logging
     * @return gefundener Controller oder {@code null}
     */
    private Controller getXboxController(int playerNumber) {
        for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
            if (controller.getType() == Controller.Type.GAMEPAD
                    || controller.getType() == Controller.Type.STICK) {
                return controller;
            }
        }
        System.out.println("Kein Xbox-Controller gefunden für Spieler " + playerNumber);
        return null;
    }

    // ── Spielschleife ─────────────────────────────────────────────────────────

    /**
     * Startet das Spiel: 2 Controller-Threads + Haupt-Spielschleife.
     *
     * Ablauf pro Iteration:
     * <ol>
     *   <li>moveBall() – Ball bewegen und Kollisionen prüfen</li>
     *   <li>updateGame() – Spielfeld zeichnen (derzeit Stub)</li>
     *   <li>sleep(speed) – Spieltempo</li>
     * </ol>
     */
    public void play() {
        startControllerThread(xboxController1, 1);  // Thread für linken Schläger
        startControllerThread(xboxController2, 2);  // Thread für rechten Schläger

        while (true) {
            moveBall();
            updateGame();
            sleep(speed);
        }
    }

    // ── Controller-Thread ─────────────────────────────────────────────────────

    /**
     * Startet einen benannten Thread, der alle 100 ms den Controller abfragt
     * und das entsprechende Paddle bewegt.
     *
     * <h3>Joystick Y-Achse:</h3>
     * <ul>
     *   <li>-1.0 bis -0.3: Joystick oben → Paddle hoch</li>
     *   <li>-0.1 bis +0.1: Totzone → keine Bewegung (Drift unterdrücken)</li>
     *   <li>+0.3 bis +1.0: Joystick unten → Paddle runter</li>
     * </ul>
     *
     * Paddle-Grenzen: Y=0 (oben) bis Y=5 (unten, 3 LEDs hoch → belegt 5,6,7).
     *
     * @param xboxController der Controller dieses Spielers (kann {@code null} sein)
     * @param player         Spielernummer 1 oder 2
     */
    private void startControllerThread(Controller xboxController, int player) {
        new Thread(() -> {
            while (true) {
                if (xboxController == null) {
                    sleep(100);
                    continue;
                }

                xboxController.poll();   // alle Komponenten aktualisieren
                float yValue = 0.0f;

                for (Component comp : xboxController.getComponents()) {
                    if (comp.getName().equalsIgnoreCase("y")) {
                        yValue = comp.getPollData();  // Y-Achse: -1.0..+1.0
                        break;
                    }
                }

                updateControllerInput(yValue, player);  // Paddle bewegen
                sleep(100);  // 10× pro Sekunde
            }
        }, "Controller-" + player + "-Thread").start();  // benannter Thread
    }

    /**
     * Bewegt das Paddle des angegebenen Spielers basierend auf dem Joystick-Wert.
     *
     * @param yValue  Y-Achsen-Wert des Joysticks (-1.0 bis +1.0)
     * @param player  Spieler 1 oder 2
     */
    private void updateControllerInput(float yValue, int player) {
        synchronized (paddleLock) {
            if (Math.abs(yValue) < 0.1) return;  // Totzone: kleinstes Rauschen ignorieren

            if (player == 1) {
                if (yValue < -0.3 && paddle1Y > 0)  paddle1Y--;  // hoch
                else if (yValue > 0.3 && paddle1Y < 5) paddle1Y++;  // runter
            } else {
                if (yValue < -0.3 && paddle2Y > 0)  paddle2Y--;
                else if (yValue > 0.3 && paddle2Y < 5) paddle2Y++;
            }

            // Debug-Ausgabe (kann für den echten Einsatz auskommentiert werden)
            System.out.printf("Spieler %d Schläger Y: %d%n",
                              player, player == 1 ? paddle1Y : paddle2Y);
        }
    }

    // ── Ballbewegung ─────────────────────────────────────────────────────────────

    /**
     * Bewegt den Ball einen Schritt und prüft Kollisionen.
     *
     * Kollisionsregeln (identisch mit PongGame):
     * Wand oben/unten → Y-Richtung umkehren
     * Paddle links (X=1) → X-Richtung = rechts (+1)
     * Paddle rechts (X=6) → X-Richtung = links (-1)
     * Ball hinter linkem/rechtem Rand → Reset
     */
    private void moveBall() {
        ballX += ballDirX;
        ballY += ballDirY;

        // Wandkollision
        if (ballY <= 0 || ballY >= 7) ballDirY *= -1;

        synchronized (paddleLock) {
            // Paddle 1 (linke Seite, Spalte 1)
            if (ballX == 1 && ballY >= paddle1Y && ballY < paddle1Y + 3)
                ballDirX = 1;
            // Paddle 2 (rechte Seite, Spalte 6)
            if (ballX == 6 && ballY >= paddle2Y && ballY < paddle2Y + 3)
                ballDirX = -1;
        }

        // Ball am Rand verfehlt → Punkt vergeben, Ball zurücksetzen
        if (ballX <= 0 || ballX >= 7) resetBall();
    }

    /**
     * Setzt Ball in die Mitte mit zufälliger Richtung.
     * Punktevergabe ist hier nicht implementiert (im Gegensatz zu PongGame).
     */
    private void resetBall() {
        ballX = 4;
        ballY = 4;
        ballDirX = random.nextBoolean() ? 1 : -1;
        ballDirY = random.nextBoolean() ? 1 : -1;
    }

    // ── Spielfeld ────────────────────────────────────────────────────────────────

    /**
     * Zeichnet den aktuellen Spielzustand auf die LED-Matrix.
     *
     * <h3>Hinweis:</h3>
     * Diese Methode ist derzeit ein Stub (leerer Rumpf).
     * Die vollständige Implementierung ist in PongGame.java (displayPixels,
     * drawPaddles, drawBall, drawScores) zu finden und kann hier direkt
     * übernommen werden.
     */
    private void updateGame() {
        clear();                        // alle LEDs auf Schwarz setzen
        drawScores();                   // Punkte-LEDs setzen
        synchronized (paddleLock) {     // Schläger threadsicher lesen
            drawPaddles();
        }
        drawBall();
        displayPixels();                // Puffer auf /dev/fb0 schreiben
    }

    /**
     * Punktestand in den Eckspalten einblenden:
     * Spieler 1 (Spalte 0, rot), Spieler 2 (Spalte 7, grün).
     */
    private void drawScores() {
        for (int i = 0; i < player1Score && i < 8; i++) {
            pixels[0][i] = RED;
        }
        for (int i = 0; i < player2Score && i < 8; i++) {
            pixels[7][i] = GREEN;
        }
    }

    /** Setzt die Ball-LED (grün). */
    private void drawBall() {
        pixels[ballY][ballX] = GREEN;
    }

    /**
     * Zeichnet beide 3-Pixel-Schläger. Paddle 1 links (orange), Paddle 2
     * rechts (blau). Der Bounds-Check verhindert eine
     * {@link ArrayIndexOutOfBoundsException}, wenn ein Schläger am Rand steht.
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
     * Überträgt den Framebuffer auf die LED-Matrix des Sense HAT
     * (Gerät {@code /dev/fb0}).
     *
     * <p>Pro Pixel wird ein 16-Bit-Wort (RGB565) geschrieben. Die Zeilen
     * werden in umgekehrter Reihenfolge ausgegeben, weil das Sense HAT
     * seine Zeilen von unten nach oben nummeriert.</p>
     */
    private void displayPixels() {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream("/dev/fb0");
             java.io.DataOutputStream  os  = new java.io.DataOutputStream(fos)) {
            for (int row = 7; row >= 0; row--) {
                for (int col = 0; col < 8; col++) {
                    os.writeShort(pixels[row][col]);
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Framebuffer-Ausgabe fehlgeschlagen: " + e.getMessage());
        }
    }

    /** Setzt alle 64 LEDs auf Schwarz. */
    private void clear() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                pixels[r][c] = BLACK;
            }
        }
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────────

    /**
     * Lässt den aktuellen Thread {@code ms} Millisekunden schlafen.
     * Setzt das Interrupt-Flag wieder, wenn der Thread unterbrochen wurde.
     */
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Interrupt-Status wiederherstellen
            System.err.println("Thread unterbrochen: " + e.getMessage());
        }
    }

    /**
     * Einstiegspunkt: erstellt TwoPlayerPong und startet das Spiel.
     *
     * @param args nicht verwendet
     */
    public static void main(String[] args) {
        new TwoPlayerPong().play();
    }
}
