import net.java.games.input.*;

public class XboxControllerExample {
    public static void main(String[] args) {
        try {
            // Holen Sie sich alle Controller im System
            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

            Controller xboxController = null;

            // Überprüfen, ob ein Xbox-Controller angeschlossen ist
            for (Controller controller : controllers) {
                if (controller.getType() == Controller.Type.GAMEPAD) {
                    System.out.println("Xbox Controller gefunden: " + controller.getName());
                    xboxController = controller;
                    break;
                }
            }

            if (xboxController == null) {
                System.out.println("Kein Xbox-Controller gefunden.");
                return;
            }

            // Polling Schleife: Abrufen und Auswerten der Eingabewerte
            while (true) {
                xboxController.poll(); // Polling der Eingabewerte

                // Verarbeitung der Eingabewerte
                EventQueue eventQueue = xboxController.getEventQueue();
                Event event = new Event();

                while (eventQueue.getNextEvent(event)) {
                    Component component = event.getComponent();
                    float value = event.getValue(); // Der Wert der Eingabe

                    // Überprüfen, ob es sich um eine gedrückte Taste handelt
                    if (component.isAnalog()) {
                        System.out.printf("Analog-Input %s: %.2f\n", component.getName(), value);
                    } else {
                        System.out.printf("Taste %s: %s\n", component.getName(),
                                (value == 1.0f ? "gedrückt" : "losgelassen"));
                    }
                }

                // Kurze Pause, um die CPU nicht zu überlasten
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
