import rpi.sensehat.api.SenseHat;
import rpi.sensehat.api.LEDMatrix;
import rpi.sensehat.api.dto.Color;

public class SenseHatTest {
    public static void main(String[] args) {
        try {
            // SenseHat initialisieren
            SenseHat senseHat = new SenseHat();

            // LED-Matrix verwenden
            LEDMatrix ledMatrix = senseHat.ledMatrix;

            // Nachricht auf der LED-Matrix anzeigen
            ledMatrix.showMessage("Hello, Sense HAT!");

            // Erstellen eines 8x8-Color-Arrays mit einer vordefinierten Farbe
            Color[] redPixels = new Color[64];
            for (int i = 0; i < redPixels.length; i++) {
                redPixels[i] = Color.RED; // Verwenden Sie eine vordefinierte Farbe
            }
            ledMatrix.setPixels(redPixels); // Komplettes Pixel-Array setzen

            // LED-Matrix nach 3 Sekunden löschen
            Thread.sleep(3000);
            ledMatrix.clear();

            System.out.println("Test erfolgreich abgeschlossen.");
        } catch (Exception e) {
            System.err.println("Ein Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

