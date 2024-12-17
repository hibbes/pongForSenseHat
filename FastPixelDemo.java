import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FastPixelDemo {
    private static final int MATRIX_SIZE = 8;
    private static final String FRAMEBUFFER_FILE = "/dev/fb0"; // Framebuffer der Sense HAT
    private static final int PIXEL_SIZE = 2; // RGB565: 2 Bytes pro Pixel

    public static void main(String[] args) {
        try (RandomAccessFile fbFile = new RandomAccessFile(FRAMEBUFFER_FILE, "rw")) {
            FileChannel fbChannel = fbFile.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(MATRIX_SIZE * MATRIX_SIZE * PIXEL_SIZE);

            // Matrix initial leeren
            clearMatrix(fbChannel, buffer);

            int lastX = 0;
            int lastY = 0;

            // Pixel schnell durch alle Koordinaten bewegen
            while (true) {
                for (int y = 0; y < MATRIX_SIZE; y++) { // Y zuerst
                    for (int x = 0; x < MATRIX_SIZE; x++) { // X-Koordinate
                        // Lösche den letzten Pixel
                        setPixel(fbChannel, buffer, lastX, lastY, 0, 0, 0); // Schwarz

                        // Setze den neuen Pixel
                        setPixel(fbChannel, buffer, x, y, 255, 0, 0); // Rot

                        // Debug-Ausgabe für aktuelle Position
                        System.out.println("Pixel bei: (" + x + ", " + y + ")");

                        // Speichere die aktuelle Position als letzte Position
                        lastX = x;
                        lastY = y;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler bei der Pixelbewegung: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setPixel(FileChannel fbChannel, ByteBuffer buffer, int x, int y, int r, int g, int b) throws Exception {
        // Offsets basieren auf Zeilenbreite und Pixelgröße
        int offset = (y * MATRIX_SIZE + x) * PIXEL_SIZE;
        short rgb565 = rgbTo565(r, g, b);

        buffer.clear();
        buffer.putShort(rgb565);
        buffer.flip();

        fbChannel.write(buffer, offset);
    }

    private static short rgbTo565(int r, int g, int b) {
        int red = (r >> 3) & 0x1F;
        int green = (g >> 2) & 0x3F;
        int blue = (b >> 3) & 0x1F;
        return (short) ((red << 11) | (green << 5) | blue);
    }

    private static void clearMatrix(FileChannel fbChannel, ByteBuffer buffer) throws Exception {
        buffer.clear();
        for (int i = 0; i < MATRIX_SIZE * MATRIX_SIZE; i++) {
            buffer.putShort((short) 0); // Schwarz
        }
        buffer.flip();
        fbChannel.write(buffer, 0);
    }
}
