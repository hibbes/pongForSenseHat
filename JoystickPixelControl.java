import rpi.sensehat.api.SenseHat;
import rpi.sensehat.api.LEDMatrix;
import rpi.sensehat.api.Joystick;
import rpi.sensehat.api.dto.Color;
import rpi.sensehat.api.dto.JoystickEvent;
import rpi.sensehat.api.dto.joystick.Direction;

public class JoystickPixelControl {
    public static void main(String[] args) {
        try {
            SenseHat senseHat = new SenseHat();
            LEDMatrix ledMatrix = senseHat.ledMatrix;
            Joystick joystick = senseHat.joystick;

            // Initiale Position des Pixels
            int x = 3; // Mitte der Matrix
            int y = 3;
            Color pixelColor = Color.GREEN; // Vordefinierte Farbe (Grün)

            while (true) {
                // Pixel zeichnen
                ledMatrix.clear(); // Matrix leeren
                ledMatrix.setPixel(x, y, pixelColor); // Pixel setzen

                // Warten auf ein Joystick-Ereignis
                JoystickEvent event = joystick.waitForEvent();

                if (event != null) {
                    Direction direction = event.getDirection(); // Richtung abrufen
                    switch (direction) {
                        case UP:
                            if (y > 0) y--; // Nach oben bewegen
                            break;
                        case DOWN:
                            if (y < 7) y++; // Nach unten bewegen
                            break;
                        case LEFT:
                            if (x > 0) x--; // Nach links bewegen
                            break;
                        case RIGHT:
                            if (x < 7) x++; // Nach rechts bewegen
                            break;
                        case MIDDLE:
                            // Farbe wechseln, wenn die Mitte gedrückt wird
                            pixelColor = Color.of((int)(Math.random() * 256), (int)(Math.random() * 256), (int)(Math.random() * 256));
                            break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler bei der Joystick-Steuerung: " + e.getMessage());
        }
    }
}
