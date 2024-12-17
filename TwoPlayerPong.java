import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Component;
import java.util.Random;

public class TwoPlayerPong {

    private Controller xboxController1;
    private Controller xboxController2;
    private final Object paddleLock = new Object();
    private int paddle1Y = 3, paddle2Y = 3;
    private int ballX = 4, ballY = 4, ballDirX = 1, ballDirY = 1;
    private final int speed = 500;
    private final Random random = new Random();

    public TwoPlayerPong() {
        xboxController1 = getXboxController(1);
        xboxController2 = getXboxController(2);
    }

    private Controller getXboxController(int playerNumber) {
        for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
            if ((controller.getType() == Controller.Type.GAMEPAD || controller.getType() == Controller.Type.STICK)) {
                return controller; // Du kannst auch spezifische Logik hier einbauen, wenn du den Controller
                                   // unterscheiden willst
            }
        }
        System.out.println("Kein Xbox-Controller gefunden für Spieler " + playerNumber);
        return null;
    }

    public void play() {
        startControllerThread(xboxController1, 1); // Controller 1-Thread starten
        startControllerThread(xboxController2, 2); // Controller 2-Thread starten
        while (true) {
            moveBall(); // Ball bewegen
            updateGame(); // Spielfeld aktualisieren
            sleep(speed); // Spielgeschwindigkeit
        }
    }

    private void startControllerThread(Controller xboxController, int player) {
        new Thread(() -> {
            while (true) {
                if (xboxController == null)
                    continue;
                xboxController.poll(); // Eingabewerte aktualisieren
                float yValue = 0.0f;

                // Durchlaufe die Komponenten des Controllers
                for (Component comp : xboxController.getComponents()) {
                    if (comp.getName().equalsIgnoreCase("y")) {
                        yValue = comp.getPollData(); // Y-Wert des Joysticks
                        break;
                    }
                }

                updateControllerInput(yValue, player); // Steuere den Schläger basierend auf der Eingabe
                sleep(100); // Eingaben alle 100 ms verarbeiten
            }
        }, "Controller-" + player + "-Thread").start();
    }

    private void updateControllerInput(float yValue, int player) {
        synchronized (paddleLock) {
            if (Math.abs(yValue) < 0.1) {
                return; // Kleine Bewegungen ignorieren
            }

            if (player == 1) {
                if (yValue < -0.3 && paddle1Y > 0)
                    paddle1Y--;
                else if (yValue > 0.3 && paddle1Y < 5)
                    paddle1Y++;
            } else {
                if (yValue < -0.3 && paddle2Y > 0)
                    paddle2Y--;
                else if (yValue > 0.3 && paddle2Y < 5)
                    paddle2Y++;
            }

            System.out.printf("Spieler %d Schläger Y-Wert: %d%n", player, player == 1 ? paddle1Y : paddle2Y);
        }
    }

    private void moveBall() {
        ballX += ballDirX;
        ballY += ballDirY;

        if (ballY <= 0 || ballY >= 7)
            ballDirY *= -1;

        synchronized (paddleLock) {
            if (ballX == 1 && ballY >= paddle1Y && ballY < paddle1Y + 3)
                ballDirX = 1;
            if (ballX == 6 && ballY >= paddle2Y && ballY < paddle2Y + 3)
                ballDirX = -1;
        }

        if (ballX <= 0) {
            resetBall();
        } else if (ballX >= 7) {
            resetBall();
        }
    }

    private void resetBall() {
        ballX = 4;
        ballY = 4;
        ballDirX = random.nextBoolean() ? 1 : -1;
        ballDirY = random.nextBoolean() ? 1 : -1;
    }

    private void updateGame() {
        // Hier können alle Logiken zum Zeichnen des Balls, der Schläger und der
        // Punktanzeige implementiert werden
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread unterbrochen: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new TwoPlayerPong().play();
    }
}
