import rpi.sensehat.api.SenseHat;
import rpi.sensehat.api.dto.JoystickEvent;

public class JoystickEventLogger {

    public static void main(String[] args) {
        SenseHat senseHat = new SenseHat();
        boolean running = true;

        Thread joystickThread = new Thread(() -> {
            System.out.println("Joystick-Event-Logger gestartet. Bewege den Joystick (Drücke Enter zum Beenden):");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    JoystickEvent event = senseHat.joystick.waitForEvent(false);
                    logEvent(event);
                } catch (Exception e) {
                    System.err.println("Fehler beim Abfragen des Joysticks: " + e.getMessage());
                }
            }
        });

        joystickThread.start();

        try {
            System.in.read(); // Auf Enter warten
        } catch (Exception ignored) {
        } finally {
            joystickThread.interrupt(); // Thread stoppen
        }

        System.out.println("Programm beendet.");
    }

    private static void logEvent(JoystickEvent event) {
        System.out.printf("Action: %s | Direction: %s | Timestamp: %.3f%n",
                event.getAction(), event.getDirection(), event.getTimestamp());
    }
}

