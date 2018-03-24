package org.mozilla.iot.webthing.example;

import org.json.JSONObject;
import org.mozilla.iot.webthing.Action;
import org.mozilla.iot.webthing.Event;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.WebThingServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestServer {
    public static Thing makeThing() {
        Thing thing = new Thing("My Lamp", "thing", "A web connected lamp");

        Map<String, Object> onDescription = new HashMap<>();
        onDescription.put("type", "boolean");
        onDescription.put("description", "Whether the lamp is turned on");
        thing.addProperty(new Property(thing, "on", onDescription, true));

        Map<String, Object> levelDescription = new HashMap<>();
        levelDescription.put("type", "number");
        levelDescription.put("description", "The level of light from 0-100");
        levelDescription.put("minimum", 0);
        levelDescription.put("maximum", 100);
        thing.addProperty(new Property(thing, "level", levelDescription, 50));

        Map<String, Object> fadeMetadata = new HashMap<>();
        Map<String, Object> fadeInput = new HashMap<>();
        Map<String, Object> fadeProperties = new HashMap<>();
        Map<String, Object> fadeLevel = new HashMap<>();
        Map<String, Object> fadeDuration = new HashMap<>();
        fadeMetadata.put("description", "Fade the lamp to a given level");
        fadeInput.put("type", "object");
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
        overheatedMetadata.put("unit", "celcius");
        thing.addAvailableEvent("overheated", overheatedMetadata);

        return thing;
    }

    public static void main(String[] args) {
        Thing thing = makeThing();
        WebThingServer server;

        try {
            List<Thing> things = new ArrayList<>();
            things.add(thing);

            // If adding more than one thing here, be sure to set the second
            // parameter to some string, which will be broadcast via mDNS.
            // In the single thing case, the thing's name will be broadcast.
            server = new WebThingServer(things, null, 8888);

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

            thing.setProperty("level", input.getInt("level"));
            thing.addEvent(new OverheatedEvent(thing, 102));
        }
    }
}
