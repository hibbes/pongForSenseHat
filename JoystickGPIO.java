import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;

public class JoystickGPIO {

    // GPIO-Pins für den Joystick
    private static final Pin PIN_UP = RaspiPin.GPIO_23;  // Beispiel: GPIO Pin 23 für "Up"
    private static final Pin PIN_DOWN = RaspiPin.GPIO_24;  // Beispiel: GPIO Pin 24 für "Down"
    private static final Pin PIN_LEFT = RaspiPin.GPIO_25;  // Beispiel: GPIO Pin 25 für "Left"
    private static final Pin PIN_RIGHT = RaspiPin.GPIO_27;  // Beispiel: GPIO Pin 27 für "Right"

    private GpioController gpio;
    private GpioPinDigitalInput pinUp;
    private GpioPinDigitalInput pinDown;
    private GpioPinDigitalInput pinLeft;
    private GpioPinDigitalInput pinRight;

    public JoystickGPIO() {
        // Initialisieren von Pi4J GPIO Controller
        gpio = GpioFactory.getInstance();
        
        // Setze Pins auf Eingang mit Pull-Down-Widerstand (für LOW bei unbetätigten Tasten)
        pinUp = gpio.provisionDigitalInputPin(PIN_UP, PinPullResistance.PULL_DOWN);
        pinDown = gpio.provisionDigitalInputPin(PIN_DOWN, PinPullResistance.PULL_DOWN);
        pinLeft = gpio.provisionDigitalInputPin(PIN_LEFT, PinPullResistance.PULL_DOWN);
        pinRight = gpio.provisionDigitalInputPin(PIN_RIGHT, PinPullResistance.PULL_DOWN);
    }

    public void readJoystick() {
        while (true) {
            // Überprüfe, ob die Pins auf HIGH sind (Joystick ist in Bewegung)
            if (pinUp.isHigh()) {
                System.out.println("Joystick nach oben bewegt");
            }

            if (pinDown.isHigh()) {
                System.out.println("Joystick nach unten bewegt");
            }

            if (pinLeft.isHigh()) {
                System.out.println("Joystick nach links bewegt");
            }

            if (pinRight.isHigh()) {
                System.out.println("Joystick nach rechts bewegt");
            }

            try {
                Thread.sleep(100);  // Warten für eine kurze Zeit
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        JoystickGPIO joystickGPIO = new JoystickGPIO();
        joystickGPIO.readJoystick();
    }
}
