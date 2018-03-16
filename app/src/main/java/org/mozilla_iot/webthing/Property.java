package org.mozilla_iot.webthing;

import org.json.JSONException;
import org.json.JSONObject;

public class Property<T> {
    private Thing thing;
    private String name;
    private PropertyDescription description;
    private T value;

    public class PropertyDescription<T> {
        private String type;
        private String unit;
        private String description;
        private String href;
        private T min;
        private T max;

        public void setType(String type) {
            this.type = type;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public void setMin(T min) {
            this.min = min;
        }

        public void setMax(T max) {
            this.max = max;
        }
    }

    public Property(Thing thing, String name, PropertyDescription description) {
        this.thing = thing;
        this.name = name;
        this.description = description;
        this.description.href = String.format("/properties/%s", this.name);
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