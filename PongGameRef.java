import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public class PongGameRef {

    // Farbkonstanten für die Pixel auf dem Display (in RGB)
    private static final int BLACK = 0x0000, RED = 0x0038, GREEN = 0xE001, BLUE = 0x0E00, ORANGE = 0xFFE0;

    // Das 8x8 Pixel-Display, auf dem das Spiel angezeigt wird
    private final int[][] pixels = new int[8][8];

    // Spielvariablen für Punkte, Ballposition und -bewegung, Schlägerposition
    private int player1Score = 0, player2Score = 0, ballX = 4, ballY = 4, ballDirX = 1, ballDirY = 1;
    private int paddle1Y = 3, paddle2Y = 3;
    private final int speed = 100; // Spielgeschwindigkeit (in Millisekunden)
    private final Random random = new Random();
    private final Controller xboxController;
    private float controllerYValue = 0.0f;
    private long ballStartTime = 0; // Zeitstempel für den Beginn des aktuellen Ballwechsels
    private static int ballMoveDelay = 3; // Verzögerung der Ballbewegung

    // Konstruktor: Initialisiert den Xbox-Controller
    public PongGameRef() {
        xboxController = getXboxController();
    }

    // Methode, die den Xbox-Controller sucht und zurückgibt
    private Controller getXboxController() {
        // Durchlaufe alle Controller und suche einen Gamepad-Controller
        for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
            if (controller.getType() == Controller.Type.GAMEPAD) {
                System.out.println("Controller gefunden: " + controller.getName());
                return controller; // Gibt den gefundenen Controller zurück
            }
        }
        System.out.println("Kein Xbox-Controller gefunden!"); // Fehlermeldung, falls kein Controller gefunden wird
        return null;
    }

    // Die Hauptspielmethode, die den Spielablauf steuert
    public void play() {
        long lastTime = System.currentTimeMillis();
        int ballMoveCounter = 0; // Zähler für die Ballbewegung

        while (true) {
            long currentTime = System.currentTimeMillis();
            // Überprüfe, ob genügend Zeit vergangen ist, um einen neuen Spielzyklus zu
            // starten
            if (currentTime - lastTime >= speed) {
                pollControllerInput(); // Lese die Eingaben des Controllers
                if (ballMoveCounter % ballMoveDelay == 0) {
                    moveBall(); // Bewege den Ball alle 'ballMoveDelay' Zyklen
                }
                moveAI(); // Bewege den "KI-Schläger"
                updateGame(); // Aktualisiere das Spiel (Anzeige, Punktstände)
                checkGameEnd(); // Überprüfe, ob das Spiel zu Ende ist
                ballMoveCounter++; // Zähler erhöhen
                lastTime = currentTime; // Setze die aktuelle Zeit als "lastTime"
            }
            sleep(10); // Kurze Pause, um die CPU nicht unnötig zu belasten
        }
    }

    // Methode, die die Eingaben des Xbox-Controllers überprüft
    private void pollControllerInput() {
        if (xboxController != null) {
            xboxController.poll(); // Polling des Controllers
            // Überprüfe die Eingabe des Y-Controllers (für die Schlägerbewegung)
            for (Component comp : xboxController.getComponents()) {
                if (comp.getName().equalsIgnoreCase("y")) {
                    controllerYValue = comp.getPollData(); // Speichere den aktuellen Y-Wert
                }
            }
        }

        // Schlägerbewegung aktualisieren (umgedrehte Steuerung)
        if (controllerYValue > 0.3 && paddle1Y > 0) {
            paddle1Y--; // Schläger nach oben bewegen
        } else if (controllerYValue < -0.3 && paddle1Y < 5) {
            paddle1Y++; // Schläger nach unten bewegen
        }
    }

    // Methode, die den Ball bewegt
    private void moveBall() {
        if (ballStartTime == 0) {
            ballStartTime = System.currentTimeMillis(); // Setze die Startzeit des Ballwechsels
        }

        long elapsedTime = (System.currentTimeMillis() - ballStartTime) / 1000; // Berechne die vergangene Zeit in
                                                                                // Sekunden

        // Passe die Ballbewegungsverzögerung je nach vergangener Zeit an
        if (elapsedTime >= 30) {
            ballMoveDelay = 0; // Schneller nach 30 Sekunden
        } else if (elapsedTime >= 20) {
            ballMoveDelay = 1; // Schneller nach 20 Sekunden
        } else if (elapsedTime >= 10) {
            ballMoveDelay = 2; // Schneller nach 10 Sekunden
        }

        // Bewege den Ball
        ballX += ballDirX;
        ballY += ballDirY;

        // Überprüfe Kollision mit der oberen und unteren Wand
        if (ballY <= 0 || ballY >= 7)
            ballDirY *= -1; // Richtungsumkehr

        // Überprüfe Kollision mit den Schlägern
        if (ballX == 1 && ballY >= paddle1Y && ballY < paddle1Y + 3)
            ballDirX = 1; // Richtungsumkehr bei Kollision mit dem ersten Schläger
        if (ballX == 6 && ballY >= paddle2Y && ballY < paddle2Y + 3)
            ballDirX = -1; // Richtungsumkehr bei Kollision mit dem zweiten Schläger

        // Punkt für Spieler 2, wenn der Ball das linke Ende erreicht
        if (ballX <= 0) {
            player2Score++;
            resetBall(); // Ball zurücksetzen
        } else if (ballX >= 7) { // Punkt für Spieler 1, wenn der Ball das rechte Ende erreicht
            player1Score++;
            resetBall(); // Ball zurücksetzen
        }
    }

    // Methode für die Bewegung des "KI-Schlägers"
    private void moveAI() {
        if (ballX > 4) { // Nur aktiv werden, wenn der Ball auf den KI-Schläger zukommt
            // Kollisionserkennung mit dem KI-Schläger
            if (ballX == 6 && ballY >= paddle2Y && ballY < paddle2Y + 3) {
                ballDirX = -1; // Richtungsumkehr bei Kollision
            } else if (ballX == 7) {
                player1Score++; // Punkt für Spieler 1
                resetBall(); // Ball zurücksetzen
            }

            // KI-Schlägerbewegung: Der Schläger folgt dem Ball, aber nicht zu schnell
            if (paddle2Y + 1 < ballY && paddle2Y < 5) {
                paddle2Y++; // Schläger nach unten bewegen
            } else if (paddle2Y > ballY && paddle2Y > 0) {
                paddle2Y--; // Schläger nach oben bewegen
            }
        }
    }

    // Setzt den Ball in die Mitte zurück und wählt eine zufällige Richtung
    private void resetBall() {
        ballX = 4;
        ballY = 4;
        ballDirX = random.nextBoolean() ? 1 : -1; // Zufällige Richtungswahl
        ballDirY = random.nextBoolean() ? 1 : -1;
        ballMoveDelay = 3; // BallmoveDelay zurücksetzen
        ballStartTime = 0; // Startzeit zurücksetzen
        blinkDisplay(); // Blinke das Display nach jedem Punktverlust
    }

    // Lässt das Display nach jedem Punktverlust blinken
    private void blinkDisplay() {
        for (int i = 0; i < 3; i++) { // Dreimal blinken
            clear(); // Display löschen
            displayPixels(); // Zeige das leere Display
            sleep(200); // Kurze Pause (200 ms)

            drawScores(); // Zeichne die Punktanzeige
            drawBall(); // Zeichne den Ball
            drawPaddles(); // Zeichne die Schläger
            displayPixels();
            sleep(200); // Kurze Pause (200 ms)
        }
    }

    // Aktualisiert das Spiel (Display, Punktstände, Schläger, Ball)
    private void updateGame() {
        clear(); // Display löschen
        drawScores(); // Punktstände zeichnen
        drawPaddles(); // Schläger zeichnen
        drawBall(); // Ball zeichnen

        displayPixels(); // Zeige das aktualisierte Display
    }

    // Zeichne die Punktstände auf das Display
    private void drawScores() {
        for (int i = 0; i < player1Score && i < 8; i++)
            pixels[0][i] = RED; // Spieler 1 Punktanzeige
        for (int i = 0; i < player2Score && i < 8; i++)
            pixels[7][i] = GREEN; // Spieler 2 Punktanzeige
    }

    // Zeichne den Ball auf das Display
    private void drawBall() {
        pixels[ballY][ballX] = 0xFFFF; // Setze die Ballposition auf das Display
    }

    // Zeichne die Schläger auf das Display
    private void drawPaddles() {
        for (int i = 0; i < 3; i++) {
            if (paddle1Y + i < 8)
                pixels[paddle1Y + i][0] = ORANGE; // Schläger von Spieler 1
            if (paddle2Y + i < 8)
                pixels[paddle2Y + i][7] = BLUE; // Schläger von Spieler 2
        }
    }

    // Zeige das Pixel-Array auf dem Display an (z.B. über ein LCD- oder
    // LED-Display)
    private void displayPixels() {
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream("/dev/fb0"))) {
            for (int row = 7; row >= 0; row--) {
                for (int col = 0; col < 8; col++)
                    os.writeShort(pixels[row][col]); // Schreibe die Pixel in den Ausgabestream
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Anzeigen der Pixel: " + e.getMessage());
        }
    }

    // Lösche das Display (setze alle Pixel auf schwarz)
    private void clear() {
        for (int[] row : pixels) {
            for (int i = 0; i < row.length; i++)
                row[i] = BLACK; // Setze jedes Pixel auf schwarz
        }
    }

    // Überprüfe, ob das Spiel zu Ende ist (Punkte >= 8)
    private void checkGameEnd() {
        if (player1Score >= 8 || player2Score >= 8) {
            System.out.println("Spiel neu starten!");
            resetScores(); // Punktzahlen zurücksetzen
            resetBall(); // Ball zurücksetzen
            sleep(3000); // 3 Sekunden warten, bevor das Spiel fortgesetzt wird
        }
    }

    // Zeige ein Blinken des Displays am Ende des Spiels
    private void showEndGameBlinking() {
        for (int i = 0; i < 5; i++) {
            clear();
            displayPixels();
            sleep(300); // Kurze Pause (300 ms)
            drawScores(); // Punktanzeigen zeichnen
            displayPixels();
            sleep(300); // Kurze Pause (300 ms)
        }
    }

    // Setze die Punktstände zurück
    private void resetScores() {
        player1Score = 0;
        player2Score = 0;
    }

    // Hilfsmethode für eine kurze Pause (Schlafmodus)
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Main-Methode zum Starten des Spiels
    public static void main(String[] args) {
        new PongGameRef().play(); // Erstelle eine Instanz des Spiels und starte es
    }
}
