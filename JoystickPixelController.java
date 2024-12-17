import rpi.sensehat.api.SenseHat;
import rpi.sensehat.api.LEDMatrix;
import rpi.sensehat.api.Joystick;
import rpi.sensehat.api.dto.Color;
import rpi.sensehat.api.dto.JoystickEvent;
import rpi.sensehat.api.dto.joystick.Direction;

public class JoystickPixelController {
    private static final int MATRIX_SIZE = 8;
    private final int[] position = {3, 3}; // Startposition des Pixels
    private final int[] lastPosition = {3, 3}; // Letzte Position des Pixels
    private Color pixelColor = Color.GREEN; // Pixel-Farbe
    private volatile boolean updateRequired = false; // Flag zur Steuerung der Matrix-Aktualisierung

    public static void main(String[] args) {
        new JoystickPixelController().start();
    }

    public void start() {
        try {
            SenseHat senseHat = new SenseHat();
            LEDMatrix ledMatrix = senseHat.ledMatrix; // Korrigierter Zugriff auf LEDMatrix
            Joystick joystick = senseHat.joystick; // Korrigierter Zugriff auf Joystick

            // Thread: Joystick-Eingaben
            Thread joystickThread = new Thread(() -> {
                while (true) {
                    try {
                        JoystickEvent event = joystick.waitForEvent();
                        if (event != null) {
                            handleJoystickEvent(event);
                            updateRequired = true; // Signalisiert, dass die Matrix aktualisiert werden muss
                        }
                    } catch (Exception e) {
                        System.err.println("Fehler bei der Joystick-Abfrage: " + e.getMessage());
                    }
                }
            });

            // Thread: Matrix-Aktualisierung
            Thread matrixThread = new Thread(() -> {
                while (true) {
                    try {
                        if (updateRequired) {
                            synchronized (position) {
                                // Lösche den alten Pixel
                                ledMatrix.setPixel(lastPosition[0], lastPosition[1], Color.of(0, 0, 0)); // Schwarz

                                // Zeichne den neuen Pixel
                                ledMatrix.setPixel(position[0], position[1], pixelColor);

                                // Aktualisiere die letzte Position
                                lastPosition[0] = position[0];
                                lastPosition[1] = position[1];
                                updateRequired = false; // Reset des Flags nach der Aktualisierung
                            }
                        }
                        Thread.sleep(10); // Überprüft die Aktualisierung häufiger
                    } catch (Exception e) {
                        System.err.println("Fehler bei der Matrix-Aktualisierung: " + e.getMessage());
                    }
                }
            });

            // Beide Threads starten
            joystickThread.start();
            matrixThread.start();

            // Threads synchronisieren, damit sie nicht sofort beendet werden
            joystickThread.join();
            matrixThread.join();
        } catch (Exception e) {
            System.err.println("Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleJoystickEvent(JoystickEvent event) {
        synchronized (position) {
            switch (event.getDirection()) {
                case UP:
                    if (position[1] > 0) position[1]--; // Nach oben bewegen
                    break;
                case DOWN:
                    if (position[1] < MATRIX_SIZE - 1) position[1]++; // Nach unten bewegen
                    break;
                case LEFT:
                    if (position[0] > 0) position[0]--; // Nach links bewegen
                    break;
                case RIGHT:
                    if (position[0] < MATRIX_SIZE - 1) position[0]++; // Nach rechts bewegen
                    break;
                case MIDDLE:
                    // Farbe ändern
                    pixelColor = Color.of(
                        (int) (Math.random() * 256),
                        (int) (Math.random() * 256),
                        (int) (Math.random() * 256)
                    );
                    break;
            }
        }
    }
}
