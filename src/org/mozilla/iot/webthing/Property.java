/**
 * High-level Property base class implementation.
 */

package org.mozilla.iot.webthing;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * A Property represents an individual state value of a thing.
 *
 * @param <T> The type of the property value.
 */
public class Property<T> {
    private Thing thing;
    private String name;
    private PropertyDescription description;
    private T value;

    /**
     * Initialize the object.
     *
     * @param thing       Thing this property belongs to
     * @param name        Name of the property
     * @param description Description of the property
     */
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
            this.description.description =
                    (String)description.get("description");
        }

        if (description.containsKey("min")) {
            this.description.min = (T)description.get("min");
        }

        if (description.containsKey("max")) {
            this.description.max = (T)description.get("max");
        }
    }

    /**
     * Get the property description.
     *
     * @return Description of the property as an object.
     */
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

    /**
     * Set the cached value of the property, making adjustments as necessary.
     *
     * @param value The value to set.
     * @return The value that was set.
     */
    public T setCachedValue(T value) {
        this.value = value;
        this.thing.propertyNotify(this);
        return this.value;
    }

    /**
     * Get the current property value.
     *
     * @return The current value.
     */
    public T getValue() {
        return this.value;
    }

    /**
     * Set the current value of the property.
     *
     * @param value The value to set
     */
    public void setValue(T value) {
        this.setCachedValue(value);
    }

    /**
     * Get the name of this property.
     *
     * @return The proeprty name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the thing associated with this property.
     *
     * @return The thing.
     */
    public Thing getThing() {
        return this.thing;
    }

    /**
     * Object to hold the property description metadata.
     *
     * @param <T> The type of the property value.
     */
    private static class PropertyDescription<T> {
        private String type;
        private String unit;
        private String description;
        private String href;
        private T min;
        private T max;
    }
}