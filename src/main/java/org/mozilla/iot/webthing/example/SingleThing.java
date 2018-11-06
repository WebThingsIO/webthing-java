package org.mozilla.iot.webthing.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.iot.webthing.Action;
import org.mozilla.iot.webthing.Event;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.Value;
import org.mozilla.iot.webthing.WebThingServer;
import org.mozilla.iot.webthing.errors.PropertyError;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class SingleThing {
    public static Thing makeThing() {
        Thing thing = new Thing("My Lamp",
                                new JSONArray(Arrays.asList("OnOffSwitch",
                                                            "Light")),
                                "A web connected lamp");

        JSONObject onDescription = new JSONObject();
        onDescription.put("@type", "OnOffProperty");
        onDescription.put("label", "On/Off");
        onDescription.put("type", "boolean");
        onDescription.put("description", "Whether the lamp is turned on");
        // noop for state change
        thing.addProperty(new Property(thing,
                                       "on",
                                       new Value(true),
                                       onDescription));

        JSONObject brightnessDescription = new JSONObject();
        brightnessDescription.put("@type", "BrightnessProperty");
        brightnessDescription.put("label", "Brightness");
        brightnessDescription.put("type", "number");
        brightnessDescription.put("description",
                                  "The level of light from 0-100");
        brightnessDescription.put("minimum", 0);
        brightnessDescription.put("maximum", 100);
        brightnessDescription.put("unit", "percent");
        // noop consumer for brightness
        thing.addProperty(new Property(thing,
                                       "brightness",
                                       new Value(50),
                                       brightnessDescription));

        JSONObject fadeMetadata = new JSONObject();
        JSONObject fadeInput = new JSONObject();
        JSONObject fadeProperties = new JSONObject();
        JSONObject fadeBrightness = new JSONObject();
        JSONObject fadeDuration = new JSONObject();
        fadeMetadata.put("label", "Fade");
        fadeMetadata.put("description", "Fade the lamp to a given level");
        fadeInput.put("type", "object");
        fadeInput.put("required",
                      new JSONArray(Arrays.asList("brightness", "duration")));
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
        thing.addAvailableAction("fade", fadeMetadata, FadeAction.class);

        JSONObject overheatedMetadata = new JSONObject();
        overheatedMetadata.put("description",
                               "The lamp has exceeded its safe operating temperature");
        overheatedMetadata.put("type", "number");
        overheatedMetadata.put("unit", "celsius");
        thing.addAvailableEvent("overheated", overheatedMetadata);

        return thing;
    }

    public static void main(String[] args) {
        Thing thing = makeThing();
        WebThingServer server;

        try {
            // If adding more than one thing, use MultipleThings() with a name.
            // In the single thing case, the thing's name will be broadcast.
            server = new WebThingServer(new WebThingServer.SingleThing(thing),
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
