package org.mozilla.iot.webthing;

import org.json.JSONException;
import org.json.JSONObject;

public class Action {
    protected String id;
    protected Thing thing;
    protected String name;
    protected JSONObject args;
    protected String href;
    protected String status;
    protected String timeRequested;
    protected String timeCompleted;

    public Action(String id, Thing thing, String name) {
        this(id, thing, name, null);
    }

    public Action(String id, Thing thing, String name, JSONObject args) {
        this.id = id;
        this.thing = thing;
        this.name = name;
        this.args = args;
        this.href = String.format("/actions/%s/%s", this.name, this.id);
        this.status = "created";
        this.timeRequested = Utils.timestamp();
        this.thing.actionNotify(this);
    }

    public JSONObject asActionDescription() {
        JSONObject obj = new JSONObject();
        JSONObject inner = new JSONObject();
        try {
            inner.put("href", this.href);
            inner.put("timeRequested", this.timeRequested);
            inner.put("status", this.status);

            if (this.timeCompleted != null) {
                inner.put("timeCompleted", this.timeCompleted);
            }

            obj.put(this.name, inner);
            return obj;

        } catch (JSONException e) {
            return null;
        }
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getHref() {
        return this.href;
    }

    public String getStatus() {
        return this.status;
    }

    public void start() {
        this.status = "pending";
        this.thing.actionNotify(this);
        this.performAction();
        this.finish();
    }

    public void performAction() {
    }

    public void cancel() {
    }

    public void finish() {
        this.status = "completed";
        this.timeCompleted = Utils.timestamp();
        this.thing.actionNotify(this);
    }
}