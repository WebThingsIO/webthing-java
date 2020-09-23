/**
 * High-level Property base class implementation.
 */
package io.webthings.webthing;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import io.webthings.webthing.errors.PropertyError;

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
        if (this.metadata.has("readOnly") &&
                this.metadata.getBoolean("readOnly")) {
            throw new PropertyError("Read-only property");
        }

        Schema schema = SchemaLoader.load(this.metadata);
        try {
            schema.validate(value);
        } catch (ValidationException e) {
            throw new PropertyError("Invalid property value");
        }
    }

    /**
     * Get the property description.
     *
     * @return Description of the property as an object.
     */
    public JSONObject asPropertyDescription() {
        JSONObject description = new JSONObject(this.metadata.toString());
        JSONObject link = new JSONObject();
        link.put("rel", "property");
        link.put("href", this.hrefPrefix + this.href);

        if (description.has("links")) {
            description.getJSONArray("links").put(link);
        } else {
            JSONArray links = new JSONArray();
            links.put(link);
            description.put("links", links);
        }

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
