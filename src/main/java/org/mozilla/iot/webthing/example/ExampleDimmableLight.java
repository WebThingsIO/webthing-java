package org.mozilla.iot.webthing.example;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import org.mozilla.iot.webthing.Action;
import org.mozilla.iot.webthing.Event;
import org.mozilla.iot.webthing.Property;
import org.mozilla.iot.webthing.Thing;
import org.mozilla.iot.webthing.Value;

/**
 * ExampleDimmableLight.
 * A Dimmable light, that loggs received commands on/off etc. to std::out
 */
public class ExampleDimmableLight {

    private final Thing thing;

    public ExampleDimmableLight(){
        this.thing =  new Thing("My Lamp", "dimmableLight", "A web connected lamp");

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



        thing.addProperty(getOnProperty());

        thing.addProperty(getLevelProperty());

        thing.addAvailableEvent("overheated", overheatedMetadata);
    }

    private Property getOnProperty() {
        Map<String, Object> onDescription = new HashMap<>();
        onDescription.put("type", "boolean");
        onDescription.put("description", "Whether the lamp is turned on");
        Value<Boolean> on = new Value<>(
            //here you could send a signal to the GPIO that switches the lamp off
            isOn -> System.out.printf("On-State is now  %s\n",isOn),
            true);
        return new Property(thing, "on", onDescription, on);
    }

    private Property getLevelProperty() {
        Map<String, Object> levelDescription = new HashMap<>();
        levelDescription.put("type", "number");
        levelDescription.put("description", "The level of light from 0-100");
        levelDescription.put("minimum", 0);
        levelDescription.put("maximum", 100);

        Value<Double> level = new Value<>(
            //here you could send a signal to the GPIO that controls the brightness
            l -> System.out.printf("New light level is %s",l),
            0.0);

        return new Property(thing, "level",
            levelDescription, level);
    }

    public Thing getThing(){
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


