import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class PongGameRef {

    // Farben in Java Byte-Order
    private static final int BLACK = 0x0000, RED = 0x0038, GREEN = 0xE001, BLUE = 0x0E00, ORANGE = 0xFFE0;
    private final int[][] pixels = new int[8][8];
    private int player1Score = 0, player2Score = 0, ballX = 4, ballY = 4, ballDirX = 1, ballDirY = 1, paddle1Y = 3,
            paddle2Y = 3;
    private final int speed = 500;
    private final Random random = new Random();
    private final Controller xboxController;
    private final Object paddleLock = new Object();

    public PongGameRef() {
        xboxController = getXboxController();
    }

    private Controller getXboxController() {
        for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
            if (controller.getType() == Controller.Type.GAMEPAD)
                return controller;
        }
        System.out.println("Kein Xbox-Controller gefunden!");
        return null;
    }

    public void play() {
        startControllerThread(); // Controller-Thread starten
        startAIThread(); // KI-Thread starten
        startPaddleUpdateThread(); // Schläger-Update-Thread starten
        startPaddleDrawThread(); // Thread für Schlägerzeichnen starten
        // startDisplayPixelsThread(); // Optional: Eigenständiger Thread für
        // Pixelanzeige

        while (true) {
            moveBall(); // Ball bewegen
            updateGame(); // Spielfeld aktualisieren (verhindert, dass Ball und Schläger gleichzeitig
                          // überschreiben)
            sleep(speed); // Spielgeschwindigkeit
        }
    }

    private void startPaddleUpdateThread() {
        new Thread(() -> {
            while (true) {
                synchronized (paddleLock) {
                    // Y-Wert des Controllers abrufen und Schläger unabhängig vom Ball aktualisieren
                    float yValue = getControllerYValue();
                    updateControllerInput(yValue); // Schlägerbewegung basierend auf Controller-Werten
                }
                sleep(speed / 4); // Schläger wird 4x häufiger als der Ball aktualisiert
            }
        }, "Paddle-Update-Thread").start();
    }

    private void moveBall() {
        ballX += ballDirX;
        ballY += ballDirY;
        if (ballY <= 0 || ballY >= 7)
            ballDirY *= -1;

        synchronized (paddleLock) {
            if (ballX == 1 && ballY >= paddle1Y && ballY < paddle1Y + 3)
                ballDirX = 1;
            if (ballX == 6 && ballY >= paddle2Y && ballY < paddle2Y + 3)
                ballDirX = -1;
        }

        if (ballX <= 0) {
            player2Score++;
            resetBall();
        } else if (ballX >= 7) {
            player1Score++;
            resetBall();
        }
    }

    private void resetBall() {
        ballX = 4;
        ballY = 4;
        ballDirX = random.nextBoolean() ? 1 : -1;
        ballDirY = random.nextBoolean() ? 1 : -1;

        if (player1Score >= 8 || player2Score >= 8) {
            showEndGameBlinking();
            player1Score = 0;
            player2Score = 0;
        }
    }

    private void showEndGameBlinking() {
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
        for (int i = 0; i < player1Score && i < 8; i++)
            pixels[0][i] = RED;
        for (int i = 0; i < player2Score && i < 8; i++)
            pixels[7][i] = GREEN;
    }

    private void updateGame() {
        clear();
        drawScores();
        drawPaddles();
        drawBall();
        displayPixels();
    }

    private void drawBall() {
        if (ballY >= 0 && ballY < 8 && ballX >= 0 && ballX < 8)
            pixels[ballY][ballX] = GREEN;
    }

    private void drawPaddles() {
        for (int i = 0; i < 3; i++) {
            if (paddle1Y + i >= 0 && paddle1Y + i < 8)
                pixels[paddle1Y + i][0] = ORANGE;
            if (paddle2Y + i >= 0 && paddle2Y + i < 8)
                pixels[paddle2Y + i][7] = BLUE;
        }
    }

    private void updateControllerInput(float yValue) {
        synchronized (paddleLock) {
            if (yValue < -0.3 && paddle1Y > 0)
                paddle1Y--;
            else if (yValue > 0.3 && paddle1Y < 5)
                paddle1Y++;
        }
    }

    private void startPaddleDrawThread() {
        new Thread(() -> {
            while (true) {
                drawPaddles(); // Schläger zeichnen
                displayPixels(); // Pixel anzeigen
                sleep(100); // Alle 100ms zeichnen
            }
        }, "Paddle-Draw-Thread").start();
    }

    private void startDisplayPixelsThread() {
        new Thread(() -> {
            while (true) {
                displayPixels(); // Spielfeld anzeigen
                sleep(100); // Alle 100ms anzeigen
            }
        }, "Display-Pixels-Thread").start();
    }

    private float getControllerYValue() {
        if (xboxController == null)
            return 0.0f;

        xboxController.poll(); // Eingabewerte aktualisieren
        for (Component comp : xboxController.getComponents()) {
            if (comp.getName().equalsIgnoreCase("y")) {
                return comp.getPollData(); // Y-Wert des Joysticks zurückgeben
            }
        }
        return 0.0f; // Rückgabewert, falls kein Y-Wert gefunden wurde
    }

    private void displayPixels() {
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream("/dev/fb0"))) {
            for (int row = 7; row >= 0; row--) {
                for (int col = 0; col < 8; col++)
                    os.writeShort(pixels[row][col]);
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Anzeigen der Pixel: " + e.getMessage());
        }
    }

    private void clear() {
        for (int[] row : pixels) {
            for (int i = 0; i < row.length; i++)
                row[i] = BLACK;
        }
    }

    private void startControllerThread() {
        if (xboxController == null)
            return;
        new Thread(() -> {
            while (true) {
                xboxController.poll(); // Eingabewerte aktualisieren
                float yValue = 0.0f;

                // Durchlaufe die Komponenten des Controllers
                for (Component comp : xboxController.getComponents()) {
                    if (comp.getName().equalsIgnoreCase("y")) {
                        yValue = comp.getPollData(); // Y-Wert des Joysticks
                        break;
                    }
                }

                updateControllerInput(yValue); // Steuere den Schläger basierend auf der Eingabe
                sleep(100); // Eingaben alle 100 ms verarbeiten
            }
        }, "Controller-Thread").start();
    }

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
        }, "AI-Thread").start();
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread unterbrochen: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new PongGame().play();
    }
}