import rpi.sensehat.api.SenseHat;
import rpi.sensehat.api.LEDMatrix;
import rpi.sensehat.api.dto.Color;

public class MatrixAnimationDemo {
    private static final int MATRIX_SIZE = 8;

    public static void main(String[] args) {
        try {
            SenseHat senseHat = new SenseHat();
            LEDMatrix ledMatrix = senseHat.ledMatrix;

            // Matrix initial leeren
            ledMatrix.clear();

            // Animation 1: Farbwechselnde Matrix
            for (int i = 0; i < 10; i++) {
                Color randomColor = Color.of(
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256),
                    (int) (Math.random() * 256)
                );
                fillMatrix(ledMatrix, randomColor);
                Thread.sleep(5);
            }

            // Animation 2: Wanderndes Pixel
            for (int x = 0; x < MATRIX_SIZE; x++) {
                for (int y = 0; y < MATRIX_SIZE; y++) {
                    ledMatrix.clear();
                    ledMatrix.setPixel(x, y, Color.RED);
                    Thread.sleep(1);
                }
            }

            // Animation 3: Wellenbewegung
            for (int t = 0; t < 20; t++) {
                ledMatrix.clear();
                for (int x = 0; x < MATRIX_SIZE; x++) {
                    int y = (int) (4 + 3 * Math.sin((x + t) * Math.PI / 4));
                    ledMatrix.setPixel(x, y, Color.BLUE);
                }
                Thread.sleep(1);
            }

            // Abschließend die Matrix leeren
            ledMatrix.clear();

        } catch (Exception e) {
            System.err.println("Fehler bei der Animation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void fillMatrix(LEDMatrix ledMatrix, Color color) {
        for (int x = 0; x < MATRIX_SIZE; x++) {
            for (int y = 0; y < MATRIX_SIZE; y++) {
                ledMatrix.setPixel(x, y, color);
            }
        }
    }
}
