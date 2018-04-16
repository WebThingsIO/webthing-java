package org.mozilla.iot.webthing.example;

import org.json.JSONObject;
import org.mozilla.iot.webthing.Action;
import org.mozilla.iot.webthing.Event;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.Value;
import org.mozilla.iot.webthing.WebThingServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MultipleThings {
    public static void main(String[] args) {
        // Create a thing that represents a dimmable light
        Thing light = new ExampleDimmableLight().getThing();

        // Create a thing that represents a humidity sensor
        Thing sensor = new FakeGpioHumiditySensor().getThing();

        try {
            List<Thing> things = new ArrayList<>();
            things.add(light);
            things.add(sensor);

            // If adding more than one thing here, be sure to set the second
            // parameter to some string, which will be broadcast via mDNS.
            // In the single thing case, the thing's name will be broadcast.
            WebThingServer server =
                    new WebThingServer(things, "LightAndTempDevice", 8888);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    server.stop();
                }
            });

            server.start(false);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    /**
     * A dimmable light that logs received commands to std::out.
     */
    public static class ExampleDimmableLight {
        private final Thing thing;

        public ExampleDimmableLight() {
            this.thing = new Thing("My Lamp",
                                   "dimmableLight",
                                   "A web connected lamp");

            Map<String, Object> fadeMetadata = new HashMap<>();
            Map<String, Object> fadeInput = new HashMap<>();
            Map<String, Object> fadeProperties = new HashMap<>();
            Map<String, Object> fadeLevel = new HashMap<>();
            Map<String, Object> fadeDuration = new HashMap<>();
            fadeMetadata.put("description", "Fade the lamp to a given level");
            fadeInput.put("type", "object");
            fadeInput.put("required", new String[]{"level", "duration"});
            fadeLevel.put("type", "number");
            fadeLevel.put("minimum", 0);
            fadeLevel.put("maximum", 100);
            fadeDuration.put("type", "number");
            fadeDuration.put("unit", "milliseconds");
            fadeProperties.put("level", fadeLevel);
            fadeProperties.put("duration", fadeDuration);
            fadeInput.put("properties", fadeProperties);
            fadeMetadata.put("input", fadeInput);
            thing.addAvailableAction("fade", fadeMetadata, FadeAction.class);

            Map<String, Object> overheatedMetadata = new HashMap<>();
            overheatedMetadata.put("description",
                                   "The lamp has exceeded its safe operating temperature");
            overheatedMetadata.put("type", "number");
            overheatedMetadata.put("unit", "celsius");
            thing.addAvailableEvent("overheated", overheatedMetadata);

            thing.addProperty(getOnProperty());
            thing.addProperty(getLevelProperty());
        }

        private Property getOnProperty() {
            Map<String, Object> onDescription = new HashMap<>();
            onDescription.put("type", "boolean");
            onDescription.put("description", "Whether the lamp is turned on");

            Value<Boolean> on = new Value<>(true,
                                            // Here, you could send a signal to
                                            // the GPIO that switches the lamp
                                            // off
                                            v -> System.out.printf(
                                                    "On-State is now %s\n",
                                                    v));

            return new Property(thing, "on", on, onDescription);
        }

        private Property getLevelProperty() {
            Map<String, Object> levelDescription = new HashMap<>();
            levelDescription.put("type", "number");
            levelDescription.put("description",
                                 "The level of light from 0-100");
            levelDescription.put("minimum", 0);
            levelDescription.put("maximum", 100);

            Value<Double> level = new Value<>(0.0,
                                              // Here, you could send a signal
                                              // to the GPIO that controls the
                                              // brightness
                                              l -> System.out.printf(
                                                      "New light level is %s",
                                                      l));

            return new Property(thing, "level", level, levelDescription);
        }

        public Thing getThing() {
            return this.thing;
        }

        public static class OverheatedEvent extends Event {
            public OverheatedEvent(Thing thing, int data) {
                super(thing, "overheated", data);
            }
        }

        public static class FadeAction extends Action {
            public FadeAction(Thing thing, JSONObject input) {
                super(UUID.randomUUID().toString(), thing, "fade", input);
            }

            @Override
            public void performAction() {
                Thing thing = this.getThing();
                JSONObject input = this.getInput();
                try {
                    Thread.sleep(input.getInt("duration"));
                } catch (InterruptedException e) {
                }

                thing.setProperty("level", input.getInt("level"));
                thing.addEvent(new OverheatedEvent(thing, 102));
            }
        }
    }

    /**
     * A humidity sensor which updates its measurement every few seconds.
     */
    public static class FakeGpioHumiditySensor {
        private final Thing thing;
        private final Value<Double> level;

        public FakeGpioHumiditySensor() {
            this.thing = new Thing("My Humidity Sensor",
                                   "multiLevelSensor",
                                   "A web connected humidity sensor");

            Map<String, Object> onDescription = new HashMap<>();
            onDescription.put("type", "boolean");
            onDescription.put("description", "Whether the sensor is on");
            Value<Boolean> on = new Value<>(true);
            thing.addProperty(new Property(thing, "on", on, onDescription));

            Map<String, Object> levelDescription = new HashMap<>();
            levelDescription.put("type", "number");
            levelDescription.put("description", "The current humidity in %");
            levelDescription.put("unit", "%");
            this.level = new Value<>(0.0);
            thing.addProperty(new Property(thing,
                                           "level",
                                           level,
                                           levelDescription));

            // Start a thread that polls the sensor reading every 3 seconds
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(3000);
                        // Update the underlying value, which in turn notifies
                        // all listeners
                        this.level.notifyOfExternalUpdate(readFromGPIO());
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }).start();
        }

        /**
         * Mimic an actual sensor updating its reading every couple seconds.
         */
        private double readFromGPIO() {
            return 70.0d * Math.random() * (-0.5 + Math.random());
        }

        public Thing getThing() {
            return this.thing;
        }
    }
}