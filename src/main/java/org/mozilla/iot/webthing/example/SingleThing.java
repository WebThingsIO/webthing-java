package org.mozilla.iot.webthing.example;

import org.json.JSONObject;
import org.mozilla.iot.webthing.Action;
import org.mozilla.iot.webthing.Event;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.Value;
import org.mozilla.iot.webthing.WebThingServer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SingleThing {
    public static Thing makeThing() {
        Thing thing = new Thing("My Lamp",
                                Arrays.asList("OnOffSwitch", "Light"),
                                "A web connected lamp");

        Map<String, Object> onDescription = new HashMap<>();
        onDescription.put("@type", "OnOffProperty");
        onDescription.put("label", "On/Off");
        onDescription.put("type", "boolean");
        onDescription.put("description", "Whether the lamp is turned on");
        // noop for state change
        thing.addProperty(new Property(thing, "on", new Value(true, v -> {
        }), onDescription));

        Map<String, Object> brightnessDescription = new HashMap<>();
        brightnessDescription.put("@type", "BrightnessProperty");
        brightnessDescription.put("label", "Brightness");
        brightnessDescription.put("type", "number");
        brightnessDescription.put("description",
                                  "The level of light from 0-100");
        brightnessDescription.put("minimum", 0);
        brightnessDescription.put("maximum", 100);
        brightnessDescription.put("unit", "percent");
        // noop consumer for level
        thing.addProperty(new Property(thing, "brightness", new Value(50, v -> {
        }), brightnessDescription));

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
        thing.addAvailableAction("fade", fadeMetadata, FadeAction.class);

        Map<String, Object> overheatedMetadata = new HashMap<>();
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

            thing.setProperty("brightness", input.getInt("brightness"));
            thing.addEvent(new OverheatedEvent(thing, 102));
        }
    }
}