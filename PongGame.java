import java.io.*;
import java.util.Random;
import net.java.games.input.*;

public class PongGame {

    // Farben in Java Byte-Order
    public static final int BLACK = 0x0000;
    public static final int RED = 0x0038;
    public static final int GREEN = 0xE001;
    public static final int BLUE = 0x0e00;
    public static final int ORANGE = 0xFFE0; // Orange für den menschlichen Schläger

    // Spielfeld: LED-Matrix 8x8
    private int[][] pixels;
    private int player1Score = 0;
    private int player2Score = 0;

    // Ball-Position und Richtung
    private int ballX = 4;
    private int ballY = 4;
    private int ballDirX = 1;
    private int ballDirY = 1;

    // Paddle-Positionen
    private int paddle1Y = 3; // Spieler (links)
    private int paddle2Y = 3; // KI (rechts)

    private final Object paddleLock = new Object(); // Lock für Threads

    // Spielgeschwindigkeit
    private int speed = 500; // Startgeschwindigkeit in Millisekunden
    private Random random = new Random();
    private net.java.games.input.Controller xboxController;

    public PongGame() {
        pixels = new int[8][8];
        xboxController = getXboxController();
    }

    /**
     * Ermittelt den Xbox-Controller.
     */
    private net.java.games.input.Controller getXboxController() {
        net.java.games.input.Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

        for (net.java.games.input.Controller controller : controllers) {
            if (controller.getType() == net.java.games.input.Controller.Type.GAMEPAD) {
                System.out.println("Controller gefunden: " + controller.getName());
                return controller;
            }
        }
        System.out.println("Kein Xbox-Controller gefunden!");
        return null;
    }

    /**
     * Ballbewegung und Kollisionen.
     */
    private void moveBall() {
        ballX += ballDirX;
        ballY += ballDirY;

        // Kollision mit Wand
        if (ballY <= 0 || ballY >= 7)
            ballDirY *= -1;

        synchronized (paddleLock) {
            // Paddle 1 (links)
            if (ballX == 1 && ballY >= paddle1Y && ballY < paddle1Y + 3)
                ballDirX = 1;

            // Paddle 2 (rechts)
            if (ballX == 6 && ballY >= paddle2Y && ballY < paddle2Y + 3)
                ballDirX = -1;
        }

        // Punkt verloren
        if (ballX <= 0) {
            player2Score++;
            resetBall();
        } else if (ballX >= 7) {
            player1Score++;
            resetBall();
        }
    }

    /**
     * Resettet die Ballposition.
     */
    private void resetBall() {
        ballX = 4;
        ballY = 4;
        ballDirX = random.nextBoolean() ? 1 : -1;
        ballDirY = random.nextBoolean() ? 1 : -1;

        if (player1Score >= 8 || player2Score >= 8) {
            // Schließen des Spiels nach 8 Punkten
            showEndGameBlinking();
            player1Score = 0;
            player2Score = 0;
        }
    }

    private void showEndGameBlinking() {
        // Blinken der Anzeige nach Sieg (optional, könnte mit einer Farbwechsel-Logik
        // erfolgen)
        for (int i = 0; i < 5; i++) {
            clear();
            displayPixels();
            sleep(300);
            drawScores();
            displayPixels();
            sleep(300);
        }
    }

    private void drawScores() {
        // Zeichne die Punkte nach einem 8-Punkte-Sieg
        for (int i = 0; i < player1Score && i < 8; i++) {
            pixels[0][i] = RED; // Punkte von Spieler 1
        }
        for (int i = 0; i < player2Score && i < 8; i++) {
            pixels[7][i] = GREEN; // Punkte von Spieler 2
        }
    }

    /**
     * Aktualisiert das Spielfeld.
     */
    private void updateGame() {
        clear(); // Spielfeld löschen
        drawScores(); // Punkte zeichnen

        // Schläger zeichnen, unabhängig vom Ball
        synchronized (paddleLock) {
            drawPaddles(); // Schläger zeichnen
        }

        drawBall(); // Ball zeichnen
        displayPixels(); // Das Spielfeld anzeigen
    }

    private void updateAI() {
        synchronized (paddleLock) {
            if (ballX > 4) { // KI reagiert nur, wenn der Ball auf ihrer Seite ist
                if (paddle2Y + 1 < ballY && paddle2Y < 5)
                    paddle2Y++; // KI nach unten bewegen
                if (paddle2Y > ballY && paddle2Y > 0)
                    paddle2Y--; // KI nach oben bewegen
            }
        }
        sleep(100); // KI-Logik regelmäßig aufrufen
    }

    private void drawBall() {
        pixels[ballY][ballX] = GREEN;
    }

    private void drawPaddles() {
        // Schläger 1 (links)
        for (int i = 0; i < 3; i++) {
            // Überprüfen, ob der Schläger in den Bereich des Spielfelds passt
            if (paddle1Y + i >= 0 && paddle1Y + i <= 7) {
                pixels[paddle1Y + i][0] = ORANGE; // Normale Schlägerfarbe
            }
        }

        // Schläger 2 (rechts)
        for (int i = 0; i < 3; i++) {
            // Überprüfen, ob der Schläger in den Bereich des Spielfelds passt
            if (paddle2Y + i >= 0 && paddle2Y + i <= 7) {
                pixels[paddle2Y + i][7] = BLUE; // Normale Schlägerfarbe
            }
        }
    }

    private void displayPixels() {
        try (FileOutputStream fos = new FileOutputStream("/dev/fb0");
                DataOutputStream os = new DataOutputStream(fos)) {
            for (int row = 7; row >= 0; row--) {
                for (int col = 0; col < 8; col++) {
                    os.writeShort(pixels[row][col]);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void clear() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                pixels[r][c] = BLACK;
            }
        }
    }

    private void displayScores() {
        // Player 1 Punkte in der obersten Zeile (rot)
        for (int i = 0; i < player1Score && i < 8; i++) {
            pixels[0][i] = RED;
        }
        // Player 2 Punkte in der untersten Zeile (grün)
        for (int i = 0; i < player2Score && i < 8; i++) {
            pixels[7][i] = GREEN;
        }
    }

    private void updateControllerInput() {
        if (xboxController != null) {
            xboxController.poll(); // Polling der Eingabewerte
            for (Component comp : xboxController.getComponents()) {
                String compName = comp.getName();
                if (compName.equals("y")) {
                    float value = comp.getPollData();
                    System.out.printf("Analog-Input %s: %.2f\n", compName, value); // Debugging-Ausgabe

                    synchronized (paddleLock) {
                        // Empfindlichkeit auf 0.3 gesetzt
                        if (value < -0.3 && paddle1Y > 0) { // Bewegung nach oben
                            paddle1Y--;
                        } else if (value > 0.3 && paddle1Y < 5) { // Bewegung nach unten
                            paddle1Y++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Thread für die Controllersteuerung.
     */
    private void startControllerThread() {
        new Thread(() -> {
            while (true) {
                if (xboxController != null) {
                    xboxController.poll(); // Polling der Eingabewerte
                    for (Component comp : xboxController.getComponents()) {
                        String compName = comp.getName();
                        if (compName.equals("y")) {
                            float value = comp.getPollData();
                            System.out.printf("Analog-Input %s: %.2f\n", compName, value); // Debugging-Ausgabe

                            synchronized (paddleLock) {
                                // Empfindlichkeit auf 0.3 gesetzt
                                if (value < -0.3 && paddle1Y > 0) { // Bewegung nach oben
                                    paddle1Y--;
                                } else if (value > 0.3 && paddle1Y < 5) { // Bewegung nach unten
                                    paddle1Y++;
                                }
                            }
                        }
                    }
                }
                sleep(100); // Alle 250 ms abfragen
            }
        }).start();
    }

    /**
     * Thread für die KI-Steuerung des rechten Paddles.
     */
    private void startAIThread() {
        new Thread(() -> {
            while (true) {
                synchronized (paddleLock) {
                    if (ballX > 4) {
                        if (paddle2Y + 1 < ballY && paddle2Y < 5)
                            paddle2Y++;
                        if (paddle2Y > ballY && paddle2Y > 0)
                            paddle2Y--;
                    }
                }
                sleep(100);
            }
        }).start();
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hauptspiel-Schleife.
     */
    public void play() {
        startControllerThread(); // Controller-Thread starten
        startAIThread(); // KI-Thread starten

        while (true) {
            // Häufige Abfrage der Controller-Eingaben und KI-Steuerung
            updateControllerInput();
            updateAI();

            moveBall(); // Ball bewegen
            updateGame(); // Spielfeld aktualisieren

            sleep(speed); // Spielgeschwindigkeit (Pausen)
        }
    }

    public static void main(String[] args) {
        PongGame game = new PongGame();
        game.play();
    }
}
