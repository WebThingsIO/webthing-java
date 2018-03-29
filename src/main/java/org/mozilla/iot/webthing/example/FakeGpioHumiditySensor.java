package org.mozilla.iot.webthing.example;

import java.util.HashMap;
import java.util.Map;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.Value;

/**
 * ExampleHumiditySensor.
 * A humidity sensor,which updates its measured humidity every few seconds
 * from a fake GPIO input
 * @author Tim Hinkes (timmeey@timmeey.de)
 */

public class FakeGpioHumiditySensor {
    private final Thing thing;
    private final Value<Double> level;

    public FakeGpioHumiditySensor() {
        this.thing = new Thing(
            "My HumiditySensor",
            "multiLevelSensor",
            "A web connected humidity sensor");

        Map<String, Object> onDescription = new HashMap<>();
        onDescription.put("type", "boolean");
        onDescription.put("description", "Whether the sensor is running");
        Value<Boolean> on = new Value<>(true);
        thing.addProperty(new Property(thing, "on", onDescription, on));

        Map<String, Object> levelDescription = new HashMap<>();
        levelDescription.put("type", "number");
        levelDescription.put("description", "The current humidity in %");
        levelDescription.put("unit", "%");

        this.level = new Value<>(0.0);
        //start a thread that pulls the sensor reading every n-seconds
        new Thread(()->{
            while(true){
                try {
                    Thread.sleep(3000);
                    //updates the underlying value, which in turn notifies all listeners
                    this.level.notifyOfExternalUpdate(readFromGPIO());
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }).start();

        thing.addProperty(new Property(thing, "level", levelDescription,
            level));
    }

    /**
     * This method mimics an actual sensor updating it's reading every couple
     * of seconds.
     */
    private double readFromGPIO() {
        double fakeHumidity = 70.0d * Math.random() * (-0.5 + Math.random());
        return fakeHumidity;
    }

    public Thing getThing() {
        return this.thing;
    }

}
