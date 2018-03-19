/**
 * High-level Event base class implementation.
 */

package org.mozilla.iot.webthing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An Event represents an individual event from a thing.
 */
public class Event {
    private Thing thing;
    private String name;
    private String description;
    private String time;

    /**
     * Initialize the object.
     *
     * @param thing Thing this event belongs to
     * @param name  Name of the event
     */
    public Event(Thing thing, String name) {
        this(thing, name, "");
    }

    /**
     * Initialize the object.
     *
     * @param thing       Thing this event belongs to
     * @param name        Name of the event
     * @param description Description of the event
     */
    public Event(Thing thing, String name, String description) {
        this.thing = thing;
        this.name = name;
        this.description = description;
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
     * Get the event's description.
     *
     * @return The description.
     */
    public String getDescription() {
        return this.description;
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