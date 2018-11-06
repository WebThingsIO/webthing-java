/**
 * High-level Property base class implementation.
 */
package org.mozilla.iot.webthing;

import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.iot.webthing.errors.PropertyError;

import java.util.List;

/**
 * A Property represents an individual state value of a thing.
 *
 * @param <T> The type of the property value.
 */
public class Property<T> {
    private Thing thing;
    private String name;
    private String hrefPrefix;
    private String href;
    private JSONObject metadata;
    private Value<T> value;

    /**
     * Initialize the object.
     *
     * @param thing Thing this property belongs to
     * @param name  Name of the property
     * @param value Value object to hold the property value
     */
    public Property(Thing thing, String name, Value<T> value) {
        this(thing, name, value, null);
    }

    /**
     * Initialize the object.
     *
     * @param thing    Thing this property belongs to
     * @param name     Name of the property
     * @param value    Value object to hold the property value
     * @param metadata Property metadata, i.e. type, description, unit, etc., as
     *                 a Map
     */
    public Property(Thing thing,
                    String name,
                    Value<T> value,
                    JSONObject metadata) {
        this.thing = thing;
        this.name = name;
        this.value = value;
        this.hrefPrefix = "";
        this.href = String.format("/properties/%s", this.name);

        if (metadata == null) {
            this.metadata = new JSONObject();
        } else {
            this.metadata = metadata;
        }

        // Add the property change observer to notify the Thing about a
        // property change
        this.value.addObserver((a, b) -> this.thing.propertyNotify(this));
    }

    /**
     * Validate new property value before setting it.
     *
     * @param value New value
     * @throws PropertyError On validation error.
     */
    private void validateValue(T value) throws PropertyError {
        if (this.metadata.has("type")) {
            switch (this.metadata.getString("type")) {
                case "null":
                    if (!JSONObject.NULL.equals(value)) {
                        throw new PropertyError("Value must be null");
                    }
                    break;
                case "boolean":
                    if (!(Boolean.FALSE.equals(value) ||
                                  Boolean.TRUE.equals(value) ||
                                  (value instanceof String &&
                                           (((String)value).equalsIgnoreCase(
                                                   "true") ||
                                                    ((String)value).equalsIgnoreCase(
                                                            "false"))))) {
                        throw new PropertyError("Value must be a boolean");
                    }
                    break;
                case "object":
                    if (!(value instanceof JSONObject)) {
                        throw new PropertyError("Value must be an object");
                    }
                    break;
                case "array":
                    if (!(value instanceof JSONArray)) {
                        throw new PropertyError("Value must be an array");
                    }
                    break;
                case "number":
                    if (!(value instanceof Number)) {
                        throw new PropertyError("Value must be a number");
                    }
                    break;
                case "integer":
                    if (!(value instanceof Number)) {
                        throw new PropertyError("Value must be an integer");
                    }

                    double v = ((Number)value).doubleValue();
                    if (Math.abs(v - Math.round(v)) <= 0.000001) {
                        throw new PropertyError("Value must be an integer");
                    }
                    break;
                case "string":
                    if (!(value instanceof String)) {
                        throw new PropertyError("Value must be a string");
                    }
                    break;
            }
        }

        if (this.metadata.has("readOnly") &&
                this.metadata.getBoolean("readOnly")) {
            throw new PropertyError("Read-only property");
        }

        if (this.metadata.has("minimum")) {
            double minimum = this.metadata.getDouble("minimum");
            double v = ((Number)value).doubleValue();

            if (v < minimum) {
                throw new PropertyError(String.format(
                        "Value less than minimum: %f",
                        minimum));
            }
        }

        if (this.metadata.has("maximum")) {
            double maximum = this.metadata.getDouble("maximum");
            double v = ((Number)value).doubleValue();

            if (v > maximum) {
                throw new PropertyError(String.format(
                        "Value greater than maximum: %f",
                        maximum));
            }
        }

        if (this.metadata.has("enum") && this.metadata.has("type")) {
            List e = this.metadata.getJSONArray("enum").toList();

            switch (this.metadata.getString("type")) {
                case "number": {
                    double v = ((Number)value).doubleValue();
                    if (e.size() > 0 && !e.contains(v)) {
                        throw new PropertyError("Invalid enum value");
                    }
                    break;
                }
                case "integer": {
                    int v = ((Number)value).intValue();
                    if (e.size() > 0 && !e.contains(v)) {
                        throw new PropertyError("Invalid enum value");
                    }
                    break;
                }
                case "string": {
                    String v = (String)value;
                    if (e.size() > 0 && !e.contains(v)) {
                        throw new PropertyError("Invalid enum value");
                    }
                    break;
                }
            }
        }
    }

    /**
     * Get the property description.
     *
     * @return Description of the property as an object.
     */
    public JSONObject asPropertyDescription() {
        List<String> keyList = Lists.newArrayList(this.metadata.keys());
        String[] keys = keyList.toArray(new String[keyList.size()]);
        JSONObject description = new JSONObject(this.metadata, keys);
        description.put("href", this.hrefPrefix + this.href);
        return description;
    }

    /**
     * Set the prefix of any hrefs associated with this property.
     *
     * @param prefix The prefix
     */
    public void setHrefPrefix(String prefix) {
        this.hrefPrefix = prefix;
    }

    /**
     * Get the href of this property.
     *
     * @return The href.
     */
    public String getHref() {
        return this.hrefPrefix + this.href;
    }

    /**
     * Get the current property value.
     *
     * @return The current value.
     */
    public T getValue() {
        return this.value.get();
    }

    /**
     * Set the current value of the property.
     *
     * @param value The value to set
     * @throws PropertyError If value could not be set.
     */
    public void setValue(T value) throws PropertyError {
        this.validateValue(value);
        this.value.set(value);
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
     * Get the metadata associated with this property.
     *
     * @return The metadata.
     */
    public JSONObject getMetadata() {
        return this.metadata;
    }
}