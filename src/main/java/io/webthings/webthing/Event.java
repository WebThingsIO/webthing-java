/**
 * High-level Event base class implementation.
 */
package io.webthings.webthing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An Event represents an individual event from a thing.
 *
 * @param <T> The type of the event data.
 */
public class Event<T> {
    private Thing thing;
    private String name;
    private T data;
    private String time;

    /**
     * Initialize the object.
     *
     * @param thing Thing this event belongs to
     * @param name  Name of the event
     */
    public Event(Thing thing, String name) {
        this(thing, name, null);
    }

    /**
     * Initialize the object.
     *
     * @param thing Thing this event belongs to
     * @param name  Name of the event
     * @param data  Data associated with the event
     */
    public Event(Thing thing, String name, T data) {
        this.thing = thing;
        this.name = name;
        this.data = data;
        this.time = Utils.timestamp();
    }

    /**
     * Get the event description.
     *
     * @return Description of the event as a JSONObject.
     */
    public JSONObject asEventDescription() {
        JSONObject obj = new JSONObject();
        JSONObject inner = new JSONObject();
        try {
            inner.put("timestamp", this.time);

            if (this.data != null) {
                inner.put("data", this.data);
            }

            obj.put(this.name, inner);
            return obj;
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Get the thing associated with this event.
     *
     * @return The thing.
     */
    public Thing getThing() {
        return this.thing;
    }

    /**
     * Get the event's name.
     *
     * @return The name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the event's data.
     *
     * @return The data.
     */
    public T getData() {
        return this.data;
    }

    /**
     * Get the event's timestamp.
     *
     * @return The time.
     */
    public String getTime() {
        return this.time;
    }
}
