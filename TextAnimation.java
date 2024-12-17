import java.io.*;

/**
 * Displays the text "AI PONG" on the Sense Hat LED display
 * for 1 second each letter in sequence.
 */
public class TextAnimation {

    // Definierungen für Farben im Framebuffer
    public static final int BLACK = 0x0000;
    public static final int RED = 0x0038;
    public static final int GREEN = 0xE001;
    public static final int BLUE = 0x0e00;

    int[][] pixels;

    public TextAnimation() {
        pixels = new int[8][8];
    }

    // Methode zur Anzeige der Pixel auf dem Framebuffer
    public void displayPixels() {
        try {
            FileOutputStream fos = new FileOutputStream("/dev/fb0");
            DataOutputStream os = new DataOutputStream(fos);

            for (int row = 7; row >= 0; row--) {
                for (int col = 0; col < 8; col++) {
                    os.writeShort(pixels[row][col]);
                }
            }

            os.close();
            fos.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // Methode zur Verzögerung von 1 Sekunde
    public void delay() {
        try {
            Thread.sleep(1000); // 1000 ms = 1 Sekunde
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    // Methode zur Darstellung eines einzelnen Zeichens
    public void drawChar(char c) {
        // Zeichen "A"
        if (c == 'A') {
            pixels[0][3] = RED;
            pixels[1][2] = RED;
            pixels[1][4] = RED;
            pixels[2][1] = RED;
            pixels[2][5] = RED;
            pixels[3][0] = RED;
            pixels[3][6] = RED;
            pixels[4][0] = RED;
            pixels[4][6] = RED;
            pixels[5][0] = RED;
            pixels[5][6] = RED;
            pixels[6][0] = RED;
            pixels[6][6] = RED;
            pixels[7][0] = RED;
            pixels[7][6] = RED;
            pixels[4][1] = RED;
            pixels[4][2] = RED;
            pixels[4][3] = RED;
            pixels[4][4] = RED;
            pixels[4][5] = RED;
        }

        // Zeichen "I"
        if (c == 'I') {
            pixels[0][3] = RED;
            pixels[1][3] = RED;
            pixels[2][3] = RED;
            pixels[3][3] = RED;
            pixels[4][3] = RED;
            pixels[5][3] = RED;
            pixels[6][3] = RED;
            pixels[7][3] = RED;
        }
        if (c == 'P') {
            // Obere horizontale Linie
            pixels[0][0] = RED;
            pixels[0][1] = RED;
            pixels[0][2] = RED;
            pixels[0][3] = RED;
            pixels[0][4] = RED;
            pixels[0][5] = RED;
            pixels[0][6] = RED;

            // Vertikale Linie
            pixels[0][7] = RED;
            pixels[1][7] = RED;
            pixels[2][7] = RED;
            pixels[3][7] = RED;
            pixels[4][7] = RED;
            pixels[5][7] = RED;
            pixels[6][7] = RED;
            pixels[7][7] = RED;

            // Horizontale Linie in der Mitte
            pixels[4][0] = RED;
            pixels[4][1] = RED;
            pixels[4][2] = RED;
            pixels[4][3] = RED;
            pixels[4][4] = RED;
            pixels[4][5] = RED;
            pixels[4][6] = RED;

            // Untere vertikale Linie
            pixels[5][0] = RED;
            pixels[6][0] = RED;
            pixels[7][0] = RED;
        }

        // Zeichen "O"
        if (c == 'O') {
            for (int i = 1; i < 7; i++) {
                pixels[0][i] = RED; // Obere Linie (von links nach rechts)
                pixels[7][i] = RED; // Untere Linie (von links nach rechts)
                pixels[i][0] = RED; // Linke Linie (von oben nach unten)
                pixels[i][7] = RED; // Rechte Linie (von oben nach unten)
            }
            // Ecken um einen Pixel in die Mitte verschieben
            pixels[1][1] = RED; // obere linke Ecke
            pixels[1][6] = RED; // obere rechte Ecke
            pixels[6][1] = RED; // untere linke Ecke
            pixels[6][6] = RED; // untere rechte Ecke
        }

        // Zeichen "N"
        if (c == 'N') {
            for (int i = 0; i < 8; i++) {
                pixels[i][0] = RED; // Linke senkrechte Linie
                pixels[i][7] = RED; // Rechte senkrechte Linie
                pixels[i][7 - i] = RED; // Diagonale Linie von oben rechts nach unten links
            }
        }
        if (c == 'G') {
            // Obere horizontale Linie
            for (int i = 0; i < 7; i++) {
                pixels[0][6 - i] = RED; // Obere horizontale Linie (gespiegelt)
            }

            // Linke vertikale Linie
            for (int i = 0; i < 7; i++) {
                pixels[i][0] = RED; // Linke vertikale Linie
            }

            // Horizontale Linie am unteren Bogen
            for (int i = 0; i < 7; i++) {
                pixels[4][6 - i] = RED; // Horizontale Linie am unteren Bogen (gespiegelt)
            }

            // Rechte vertikale Linie für den Bogen
            for (int i = 5; i < 7; i++) {
                pixels[i][6] = RED; // Rechte vertikale Linie für den Bogen
            }
        }

        displayPixels();

    }

    public void displayText() {
        String text = "AIPONG";
        for (char c : text.toCharArray()) {
            drawChar(c);
            try {
                Thread.sleep(1000);
                clear();// Zeigt jedes Zeichen für 1 Sekunde an
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Methode zum Leeren des Displays
    public void clear() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                pixels[row][col] = BLACK;
            }
        }
    }

    // Die Hauptmethode
    public static void main(String[] args) {
        TextAnimation textAnimation = new TextAnimation();
        textAnimation.displayText(); // Zeigt "AI PONG" an
    }
}
