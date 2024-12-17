import java.io.*;
import java.util.Random;

public class AiPong {

    // Definierte Farben für den Framebuffer
    public static final int BLACK = 0x0000; // Schwarz
    public static final int WHITE = 0xFFFF; // Weiß
    public static final int RED = 0xF800; // Rot
    public static final int GREEN = 0xE001; // Grün
    public static final int BLUE = 0x001F; // Blau
    public static final int YELLOW = 0xFFE0; // Gelb
    public static final int MAGENTA = 0xF81F; // Magenta
    public static final int CYAN = 0x07FF; // Cyan

    private int[][] pixels; // 8x8 Matrix für das Display
    private Random rand; // Zufallszahlengenerator für zufällige Ereignisse

    // Spielvariablen
    private int ballX = 4, ballY = 4; // Startposition des Balls
    private int ballSpeedX = 1, ballSpeedY = 1; // Geschwindigkeit des Balls in X- und Y-Richtung
    private int leftPaddleY = 3; // Y-Position des linken Schlägers
    private int paddleHeight = 3; // Höhe des linken Schlägers
    private int gameSpeed = 300; // Anfangsgeschwindigkeit (300ms)
    private long lastSpeedIncrease = System.currentTimeMillis(); // Zeitpunkt der letzten Geschwindigkeitserhöhung

    private int collisionCount = 0; // Zähler für Kollisionen
    private int backgroundColor = BLACK; // Startfarbe des Hintergrunds

    // Initialisierung der Spielumgebung
    public AiPong() {
        pixels = new int[8][8]; // Erstellen der 8x8 Pixelmatrix
        rand = new Random(); // Initialisieren des Zufallszahlengenerators
    }

    // Kopiert die Pixel ins Framebuffer (physische Darstellung auf dem Display)
    private void displayPixels() {
        try (FileOutputStream fos = new FileOutputStream("/dev/fb0");
                DataOutputStream os = new DataOutputStream(fos)) {
            // Gehe durch die Pixelmatrix und schreibe die Farben ins Display
            for (int row = 7; row >= 0; row--) { // Beginne bei der untersten Zeile (wegen der Invertierung)
                for (int col = 0; col < 8; col++) {
                    os.writeShort(pixels[row][col]); // Schreibe die Farbe des Pixels
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage()); // Fehlerbehandlung bei IO-Fehlern
        }
    }

    // Löscht das Display und setzt den Hintergrund auf die aktuelle
    // Hintergrundfarbe
    private void clear() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                pixels[row][col] = backgroundColor; // Setzt jedes Pixel auf die Hintergrundfarbe
            }
        }
    }

    // Verzögert das Programm für die angegebene Zeit (in Millisekunden)
    private void delay(int ms) {
        try {
            Thread.sleep(ms); // Schlafe für die angegebene Millisekunden
        } catch (InterruptedException e) {
            System.out.println(e.getMessage()); // Fehlerbehandlung bei Unterbrechung
        }
    }

    // Zeichnet den Ball auf dem Display
    private void drawBall() {
        if (ballX >= 0 && ballX < 8 && ballY >= 0 && ballY < 8) { // Überprüfen, ob der Ball im gültigen Bereich liegt
            pixels[ballY][ballX] = WHITE; // Ball wird in Weiß gezeichnet
        }
    }

    // Gibt eine zufällige Hintergrundfarbe zurück, die nicht der Ball- oder
    // Schlägerfarbe entspricht
    private int getRandomBackgroundColor(int ballColor, int paddleColor) {
        int[] colors = { RED, GREEN, BLUE, YELLOW, MAGENTA, CYAN }; // Mögliche Farben für den Hintergrund
        int newColor;
        do {
            newColor = colors[rand.nextInt(colors.length)]; // Zufällige Farbe wählen
        } while (newColor == ballColor || newColor == paddleColor); // Solange wählen, bis die Farbe nicht mit Ball oder
                                                                    // Schläger übereinstimmt
        return newColor; // Rückgabe der zufälligen Farbe
    }

    // Zeichnet den linken grünen Schläger
    private void drawLeftPaddle() {
        for (int i = 0; i < paddleHeight; i++) {
            pixels[leftPaddleY + i][0] = GREEN; // Setze die Pixel des Schlägers auf Grün
        }
    }

    // Bewegt den Schläger der KI, um den Ball in der Mitte des Schlägers zu treffen
    private void moveAI() {
        int paddleMiddle = leftPaddleY + paddleHeight / 2; // Berechne die Mitte des Schlägers
        if (ballY < paddleMiddle)
            leftPaddleY--; // Wenn der Ball über der Mitte ist, bewege den Schläger nach oben
        else if (ballY > paddleMiddle)
            leftPaddleY++; // Wenn der Ball unter der Mitte ist, bewege den Schläger nach unten

        // Verhindern, dass der Schläger außerhalb des Bildschirms geht
        leftPaddleY = Math.max(0, Math.min(7 - (paddleHeight - 1), leftPaddleY));
    }

    // Bewegt den Ball und überprüft Kollisionen
    private void moveBall() {
        ballX += ballSpeedX; // Ball bewegt sich in X-Richtung
        ballY += ballSpeedY; // Ball bewegt sich in Y-Richtung

        if (ballY <= 0 || ballY >= 7)
            ballSpeedY = -ballSpeedY; // Wenn der Ball oben oder unten den Bildschirm berührt, prallt er ab

        // Wenn der Ball das linke Paddle berührt
        if (ballX == 1 && ballY >= leftPaddleY && ballY < leftPaddleY + paddleHeight) {
            ballSpeedX = -ballSpeedX; // Ball prallt ab
            collisionCount++; // Zähler für Kollisionen erhöhen
            highlightPaddleHit(); // Markiert den getroffenen Bereich des Schlägers
            delay(100); // Kurze Verzögerung
            pixels[leftPaddleY + (ballY - leftPaddleY)][0] = GREEN; // Zurücksetzen des getroffenen Pixels
        }

        if (ballX <= 0 || ballX >= 7)
            resetBall(); // Wenn der Ball den linken oder rechten Rand berührt, wird der Ball
                         // zurückgesetzt

        drawBall(); // Zeichne den Ball
    }

    // Markiert das Segment des Schlägers, das den Ball getroffen hat, rot für einen
    // Frame
    private void highlightPaddleHit() {
        int hitY = ballY - leftPaddleY; // Berechnet die Y-Position des getroffenen Bereichs
        if (hitY >= 0 && hitY < paddleHeight)
            pixels[leftPaddleY + hitY][0] = RED; // Markiere das getroffene Pixel
    }

    // Setzt den Ball zurück
    private void resetBall() {
        ballX = 7; // Setzt die X-Position des Balls an die rechte Wand
        ballY = rand.nextInt(8); // Setzt die Y-Position des Balls zufällig
        ballSpeedX = -1; // Der Ball bewegt sich nach links
        ballSpeedY = rand.nextInt(2) == 0 ? 1 : -1; // Zufällige Y-Richtung
    }

    // Erhöht die Spielgeschwindigkeit alle 10 Sekunden
    private void increaseSpeed() {
        long currentTime = System.currentTimeMillis(); // Hole die aktuelle Zeit
        if (currentTime - lastSpeedIncrease >= 10000) { // Alle 10 Sekunden
            gameSpeed = Math.max(1, gameSpeed - 10); // Erhöhe die Geschwindigkeit
            lastSpeedIncrease = currentTime; // Aktualisiere den Zeitstempel
        }
    }

    // Setzt die Spielgeschwindigkeit zurück, wenn sie den maximalen Wert erreicht
    // hat
    private void resetGameSpeed() {
        if (gameSpeed == 1)
            gameSpeed = 300; // Wenn die Geschwindigkeit 1ms erreicht, setze sie zurück auf 300ms
    }

    // Startet das Pong-Spiel
    public void startGame() {
        // Thread für die KI-Schlägerbewegung
        new Thread(() -> {
            while (collisionCount < 1000) {
                moveAI(); // Bewege den Schläger der KI
                delay(50); // Verzögere die KI-Bewegung
            }
        }).start(); // Starte den Thread für die KI

        while (collisionCount < 1000) {
            clear(); // Lösche das Display
            if (collisionCount % 10 == 0 && collisionCount > 0) {
                backgroundColor = getRandomBackgroundColor(WHITE, GREEN); // Ändere den Hintergrund alle 10 Kollisionen
            }
            moveBall(); // Bewege den Ball
            drawLeftPaddle(); // Zeichne den linken Schläger
            displayPixels(); // Zeige das aktualisierte Bild

            increaseSpeed(); // Erhöhe die Geschwindigkeit
            resetGameSpeed(); // Setze die Geschwindigkeit zurück, wenn nötig
            delay(gameSpeed); // Verzögere basierend auf der aktuellen Geschwindigkeit

            if (collisionCount >= 100) {
                showRainbowAnimation(); // Zeige eine Regenbogen-Animation nach 100 Kollisionen
                break;
            }
        }
    }

    // Regenbogen-Animation
    private void showRainbowAnimation() {
        long startTime = System.currentTimeMillis(); // Startzeit für die Animation
        int[] rainbowColors = { RED, YELLOW, GREEN, CYAN, BLUE, MAGENTA }; // Farben für den Regenbogen
        while (System.currentTimeMillis() - startTime < 10000) { // 10 Sekunden lang
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    pixels[row][col] = rainbowColors[(int) ((System.currentTimeMillis() - startTime) / 100)
                            % rainbowColors.length]; // Wechseln der Farben
                }
            }
            displayPixels(); // Zeige die Regenbogenfarben an
            delay(100); // Verzögern für 100ms
        }
        resetGame(); // Setze das Spiel zurück
    }

    // Setzt das Spiel zurück
    private void resetGame() {
        collisionCount = 0; // Setze die Kollisionen zurück
        backgroundColor = BLACK; // Setze den Hintergrund auf Schwarz
        resetBall(); // Setze den Ball zurück
        startGame(); // Starte das Spiel neu
    }

    // Hauptmethode
    public static void main(String[] args) {
        new AiPong().startGame(); // Starte das Pong-Spiel
    }
}
