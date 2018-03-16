package org.mozilla.iot.webthing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class Property<T> {
    private Thing thing;
    private String name;
    private PropertyDescription description;
    private T value;

    private class PropertyDescription<T> {
        private String type;
        private String unit;
        private String description;
        private String href;
        private T min;
        private T max;
    }

    public Property(Thing thing, String name, Map<String, Object> description) {
        this.thing = thing;
        this.name = name;
        this.description = new PropertyDescription<T>();
        this.description.href = String.format("/properties/%s", this.name);

        if (description.containsKey("type")) {
            this.description.type = (String)description.get("type");
        }

        if (description.containsKey("unit")) {
            this.description.unit = (String)description.get("unit");
        }

        if (description.containsKey("description")) {
            this.description.description = (String)description.get("description");
        }

        if (description.containsKey("min")) {
            this.description.min = (T)description.get("min");
        }

        if (description.containsKey("max")) {
            this.description.max = (T)description.get("max");
        }
    }

    public JSONObject asPropertyDescription() {
        JSONObject obj = new JSONObject();

        try {
            obj.put("href", this.description.href);

            if (this.description.type != null) {
                obj.put("type", this.description.type);
            }

            if (this.description.unit != null) {
                obj.put("unit", this.description.unit);
            }

            if (this.description.description != null) {
                obj.put("description", this.description.description);
            }

            if (this.description.min != null) {
                obj.put("min", this.description.min);
            }

            if (this.description.max != null) {
                obj.put("max", this.description.max);
            }

            return obj;
        } catch (JSONException e) {
            return null;
        }
    }

    public T setCachedValue(T value) {
        this.value = value;
        this.thing.propertyNotify(this);
        return this.value;
    }

    public T getValue() {
        return this.value;
    }

    public T setValue(T value) {
        return this.setCachedValue(value);
    }

    public String getName() {
        return this.name;
    }
}