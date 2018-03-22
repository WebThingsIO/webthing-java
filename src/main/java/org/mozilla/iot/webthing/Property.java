/**
 * High-level Property base class implementation.
 */

package org.mozilla.iot.webthing;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
    private Map<String, Object> metadata;
    private T value;

    /**
     * Initialize the object.
     *
     * @param thing Thing this property belongs to
     * @param name  Name of the property
     */
    public Property(Thing thing, String name) {
        this(thing, name, null, null);
    }

    /**
     * Initialize the object.
     *
     * @param thing    Thing this property belongs to
     * @param name     Name of the property
     * @param metadata Property metadata, i.e. type, description, unit, etc., as
     *                 a Map
     */
    public Property(Thing thing, String name, Map<String, Object> metadata) {
        this(thing, name, metadata, null);
    }

    public Property(Thing thing,
                    String name,
                    Map<String, Object> metadata,
                    T value) {
        this.thing = thing;
        this.name = name;
        this.value = value;
        this.hrefPrefix = "";
        this.href = String.format("/properties/%s", this.name);

        if (metadata == null) {
            this.metadata = new HashMap<>();
        } else {
            this.metadata = metadata;
        }
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
     * Get the property description.
     *
     * @return Description of the property as an object.
     */
    public JSONObject asPropertyDescription() {
        JSONObject description = new JSONObject(this.metadata);
        description.put("href", this.hrefPrefix + this.href);
        return description;
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
}