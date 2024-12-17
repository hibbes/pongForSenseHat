import net.java.games.input.*;

public class GameControllerTest {
    public static void main(String[] args) {
        System.out.println("Suche nach Gamecontrollern...\n");

        // Hole alle verfügbaren Controller
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

        if (controllers.length == 0) {
            System.out.println("Keine Controller gefunden!");
            return;
        }

        // Liste aller Controller
        for (int i = 0; i < controllers.length; i++) {
            System.out.println((i + 1) + ". " + controllers[i].getName() + " - Typ: " + controllers[i].getType());
        }

        // Zwei Controller für den Test auswählen
        Controller controller1 = null;
        Controller controller2 = null;

        for (Controller c : controllers) {
            if (c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK) {
                if (controller1 == null) {
                    controller1 = c;
                } else if (controller2 == null) {
                    controller2 = c;
                    break; // Beide Controller gefunden
                }
            }
        }

        if (controller1 == null || controller2 == null) {
            System.out.println("Nicht genug Controller gefunden. Bitte zwei Gamecontroller anschließen.");
            return;
        }

        System.out.println("\nGefundene Controller:");
        System.out.println("Controller 1: " + controller1.getName());
        System.out.println("Controller 2: " + controller2.getName());

        System.out.println("\nStarte Test... Drücke Tasten oder bewege Joysticks.\n");

        // Echtzeit-Eingabeschleife
        while (true) {
            displayControllerState(controller1, "Controller 1");
            displayControllerState(controller2, "Controller 2");

            // Kurze Pause, um die CPU-Auslastung zu reduzieren
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void displayControllerState(Controller controller, String controllerName) {
        controller.poll(); // Aktualisiere den Status des Controllers

        // Hole die Achsen und Buttons
        Component[] components = controller.getComponents();
        System.out.println(controllerName + " Status:");

        for (Component component : components) {
            String componentName = component.getName();
            float value = component.getPollData();

            // Nur Buttons und Achsen anzeigen, die betätigt werden
            if (Math.abs(value) > 0.1) { // Filtert kleinere Bewegungen
                System.out.printf("   %s: %.2f\n", componentName, value);
            }
        }
        System.out.println();
    }
}

