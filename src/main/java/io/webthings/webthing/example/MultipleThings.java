package io.webthings.webthing.example;

import org.json.JSONArray;
import org.json.JSONObject;
import io.webthings.webthing.Action;
import io.webthings.webthing.Event;
import io.webthings.webthing.Property;
import io.webthings.webthing.Thing;
import io.webthings.webthing.Value;
import io.webthings.webthing.WebThingServer;
import io.webthings.webthing.errors.PropertyError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
            super("urn:dev:ops:my-lamp-1234",
                  "My Lamp",
                  new JSONArray(Arrays.asList("OnOffSwitch", "Light")),
                  "A web connected lamp");

            JSONObject onDescription = new JSONObject();
            onDescription.put("@type", "OnOffProperty");
            onDescription.put("title", "On/Off");
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

            JSONObject brightnessDescription = new JSONObject();
            brightnessDescription.put("@type", "BrightnessProperty");
            brightnessDescription.put("title", "Brightness");
            brightnessDescription.put("type", "integer");
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

            JSONObject fadeMetadata = new JSONObject();
            JSONObject fadeInput = new JSONObject();
            JSONObject fadeProperties = new JSONObject();
            JSONObject fadeBrightness = new JSONObject();
            JSONObject fadeDuration = new JSONObject();
            fadeMetadata.put("title", "Fade");
            fadeMetadata.put("description", "Fade the lamp to a given level");
            fadeInput.put("type", "object");
            fadeInput.put("required",
                          new JSONArray(Arrays.asList("brightness",
                                                      "duration")));
            fadeBrightness.put("type", "integer");
            fadeBrightness.put("minimum", 0);
            fadeBrightness.put("maximum", 100);
            fadeBrightness.put("unit", "percent");
            fadeDuration.put("type", "integer");
            fadeDuration.put("minimum", 1);
            fadeDuration.put("unit", "milliseconds");
            fadeProperties.put("brightness", fadeBrightness);
            fadeProperties.put("duration", fadeDuration);
            fadeInput.put("properties", fadeProperties);
            fadeMetadata.put("input", fadeInput);
            this.addAvailableAction("fade", fadeMetadata, FadeAction.class);

            JSONObject overheatedMetadata = new JSONObject();
            overheatedMetadata.put("description",
                                   "The lamp has exceeded its safe operating temperature");
            overheatedMetadata.put("type", "number");
            overheatedMetadata.put("unit", "degree celsius");
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
            super("urn:dev:ops:my-humidity-sensor-1234",
                  "My Humidity Sensor",
                  new JSONArray(Arrays.asList("MultiLevelSensor")),
                  "A web connected humidity sensor");

            JSONObject levelDescription = new JSONObject();
            levelDescription.put("@type", "LevelProperty");
            levelDescription.put("title", "Humidity");
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
