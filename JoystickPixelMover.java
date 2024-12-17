import rpi.sensehat.api.SenseHat;
import rpi.sensehat.api.dto.Color;
import rpi.sensehat.api.dto.JoystickEvent;

public class JoystickPixelMover {
    private static final int MATRIX_SIZE = 8;
    private int x = 0;
    private int y = 0;
    private final SenseHat senseHat;

    public JoystickPixelMover() {
        this.senseHat = new SenseHat();
    }

    public void start() {
        // Clear the matrix initially
        senseHat.ledMatrix.clear();

        // Start a thread to handle joystick events
        new Thread(this::handleJoystick).start();

        // Display the pixel
        updatePixel();
    }

    private void handleJoystick() {
        while (true) {
            JoystickEvent event = senseHat.joystick.waitForEvent(false);
            String direction = event.getDirection(); // Erhalte die Richtung des Events
            switch (direction) {
                case "UP":
                    y = Math.max(0, y - 1);
                    break;
                case "DOWN":
                    y = Math.min(MATRIX_SIZE - 1, y + 1);
                    break;
                case "LEFT":
                    x = Math.max(0, x - 1);
                    break;
                case "RIGHT":
                    x = Math.min(MATRIX_SIZE - 1, x + 1);
                    break;
                case "HELD":
                    // Event wird ignoriert oder hier angepasst
                    break;
                case "RELEASED":
                    // Event wird ignoriert oder hier angepasst
                    break;
                default:
                    System.out.println("Unbekanntes Event: " + direction);
            }
            updatePixel();
        }
    }

    private void updatePixel() {
        // Clear the matrix
        senseHat.ledMatrix.clear();

        // Set the new pixel
        Color pixelColor = Color.create(255, 0, 0); // Red color
        senseHat.ledMatrix.setPixel(x, y, pixelColor);
    }

    public static void main(String[] args) {
        new JoystickPixelMover().start();
    }
}
