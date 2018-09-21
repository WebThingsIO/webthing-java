package org.mozilla.iot.webthing.example;

import org.json.JSONObject;
import org.mozilla.iot.webthing.Action;
import org.mozilla.iot.webthing.Event;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.Value;
import org.mozilla.iot.webthing.WebThingServer;
import org.mozilla.iot.webthing.errors.PropertyError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MultipleThings {
    public static void main(String[] args) {
        // Create a thing that represents a dimmable light
        Thing light = new ExampleDimmableLight();

        // Create a thing that represents a humidity sensor
        Thing sensor = new FakeGpioHumiditySensor();

        try {
            List<Thing> things = new ArrayList<>();
            things.add(light);
            things.add(sensor);

            // If adding more than one thing, use MultipleThings() with a name.
            // In the single thing case, the thing's name will be broadcast.
            WebThingServer server =
                    new WebThingServer(new WebThingServer.MultipleThings(things,
                                                                         "LightAndTempDevice"),
                                       8888);

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
    public static class ExampleDimmableLight extends Thing {
        public ExampleDimmableLight() {
            super("My Lamp",
                  Arrays.asList("OnOffSwitch", "Light"),
                  "A web connected lamp");

            Map<String, Object> onDescription = new HashMap<>();
            onDescription.put("@type", "OnOffProperty");
            onDescription.put("label", "On/Off");
            onDescription.put("type", "boolean");
            onDescription.put("description", "Whether the lamp is turned on");

            Value<Boolean> on = new Value<>(true,
                                            // Here, you could send a signal to
                                            // the GPIO that switches the lamp
                                            // off
                                            v -> System.out.printf(
                                                    "On-State is now %s\n",
                                                    v));

            this.addProperty(new Property(this, "on", on, onDescription));

            Map<String, Object> brightnessDescription = new HashMap<>();
            brightnessDescription.put("@type", "BrightnessProperty");
            brightnessDescription.put("label", "Brightness");
            brightnessDescription.put("type", "number");
            brightnessDescription.put("description",
                                      "The level of light from 0-100");
            brightnessDescription.put("minimum", 0);
            brightnessDescription.put("maximum", 100);
            brightnessDescription.put("unit", "percent");

            Value<Integer> brightness = new Value<>(50,
                                                    // Here, you could send a signal
                                                    // to the GPIO that controls the
                                                    // brightness
                                                    l -> System.out.printf(
                                                            "Brightness is now %s\n",
                                                            l));

            this.addProperty(new Property(this,
                                          "brightness",
                                          brightness,
                                          brightnessDescription));

            Map<String, Object> fadeMetadata = new HashMap<>();
            Map<String, Object> fadeInput = new HashMap<>();
            Map<String, Object> fadeProperties = new HashMap<>();
            Map<String, Object> fadeBrightness = new HashMap<>();
            Map<String, Object> fadeDuration = new HashMap<>();
            fadeMetadata.put("label", "Fade");
            fadeMetadata.put("description", "Fade the lamp to a given level");
            fadeInput.put("type", "object");
            fadeInput.put("required", new String[]{"brightness", "duration"});
            fadeBrightness.put("type", "number");
            fadeBrightness.put("minimum", 0);
            fadeBrightness.put("maximum", 100);
            fadeBrightness.put("unit", "percent");
            fadeDuration.put("type", "number");
            fadeDuration.put("minimum", 1);
            fadeDuration.put("unit", "milliseconds");
            fadeProperties.put("brightness", fadeBrightness);
            fadeProperties.put("duration", fadeDuration);
            fadeInput.put("properties", fadeProperties);
            fadeMetadata.put("input", fadeInput);
            this.addAvailableAction("fade", fadeMetadata, FadeAction.class);

            Map<String, Object> overheatedMetadata = new HashMap<>();
            overheatedMetadata.put("description",
                                   "The lamp has exceeded its safe operating temperature");
            overheatedMetadata.put("type", "number");
            overheatedMetadata.put("unit", "celsius");
            this.addAvailableEvent("overheated", overheatedMetadata);
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

                try {
                    thing.setProperty("brightness", input.getInt("brightness"));
                    thing.addEvent(new OverheatedEvent(thing, 102));
                } catch (PropertyError e) {
                }
            }
        }
    }

    /**
     * A humidity sensor which updates its measurement every few seconds.
     */
    public static class FakeGpioHumiditySensor extends Thing {
        private final Value<Double> level;

        public FakeGpioHumiditySensor() {
            super("My Humidity Sensor",
                  Arrays.asList("MultiLevelSensor"),
                  "A web connected humidity sensor");

            Map<String, Object> levelDescription = new HashMap<>();
            levelDescription.put("@type", "LevelProperty");
            levelDescription.put("label", "Humidity");
            levelDescription.put("type", "number");
            levelDescription.put("description", "The current humidity in %");
            levelDescription.put("minimum", 0);
            levelDescription.put("maximum", 100);
            levelDescription.put("unit", "percent");
            levelDescription.put("readOnly", true);
            this.level = new Value<>(0.0);
            this.addProperty(new Property(this,
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
                        double newLevel = this.readFromGPIO();
                        System.out.printf("setting new humidity level: %f\n",
                                          newLevel);
                        this.level.notifyOfExternalUpdate(newLevel);
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
            return Math.abs(70.0d * Math.random() * (-0.5 + Math.random()));
        }
    }
}