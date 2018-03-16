package org.mozilla_iot.webthing;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;

public class Event {
    private Thing thing;
    private String name;
    private String description;
    private String time;

    public Event(Thing thing, String name) {
        this(thing, name, "");
    }

    public Event(Thing thing, String name, String description) {
        this.thing = thing;
        this.name = name;
        this.description = description;
        this.time = Utils.timestamp();
    }

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
}