/**
 * High-level Action base class implementation.
 */
package io.webthings.webthing;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An Action represents an individual action on a thing.
 */
public class Action {
    private String id;
    private Thing thing;
    private String name;
    private JSONObject input;
    private String hrefPrefix;
    private String href;
    private String status;
    private String timeRequested;
    private String timeCompleted;

    /**
     * Initialize the object.
     *
     * @param id    ID of this action
     * @param thing Thing this action belongs to
     * @param name  Name of the action
     */
    public Action(String id, Thing thing, String name) {
        this(id, thing, name, null);
    }

    /**
     * Initialize the object.
     *
     * @param id    ID of this action
     * @param thing Thing this action belongs to
     * @param name  Name of the action
     * @param input Any action inputs
     */
    public Action(String id, Thing thing, String name, JSONObject input) {
        this.id = id;
        this.thing = thing;
        this.name = name;
        this.input = input;
        this.hrefPrefix = "";
        this.href = String.format("/actions/%s/%s", this.name, this.id);
        this.status = "created";
        this.timeRequested = Utils.timestamp();
    }

    /**
     * Get the action description.
     *
     * @return Description of the action as a JSONObject.
     */
    public JSONObject asActionDescription() {
        JSONObject obj = new JSONObject();
        JSONObject inner = new JSONObject();
        try {
            inner.put("href", this.hrefPrefix + this.href);
            inner.put("timeRequested", this.timeRequested);
            inner.put("status", this.status);

            if (this.input != null) {
                inner.put("input", this.input);
            }

            if (this.timeCompleted != null) {
                inner.put("timeCompleted", this.timeCompleted);
            }

            obj.put(this.name, inner);
            return obj;
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Set the prefix of any hrefs associated with this action.
     *
     * @param prefix The prefix
     */
    public void setHrefPrefix(String prefix) {
        this.hrefPrefix = prefix;
    }

    /**
     * Get this action's ID.
     *
     * @return The ID.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Get this action's name.
     *
     * @return The name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get this action's href.
     *
     * @return The href.
     */
    public String getHref() {
        return this.hrefPrefix + this.href;
    }

    /**
     * Get this action's status.
     *
     * @return The status.
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Get the thing associated with this action.
     *
     * @return The thing.
     */
    public Thing getThing() {
        return this.thing;
    }

    /**
     * Get the time the action was requested.
     *
     * @return The time.
     */
    public String getTimeRequested() {
        return this.timeRequested;
    }

    /**
     * Get the time the action was completed.
     *
     * @return The time.
     */
    public String getTimeCompleted() {
        return this.timeCompleted;
    }

    /**
     * Get the inputs for this action.
     *
     * @return The inputs.
     */
    public JSONObject getInput() {
        return input;
    }

    /**
     * Start performing the action.
     */
    public void start() {
        this.status = "pending";
        this.thing.actionNotify(this);
        this.performAction();
        this.finish();
    }

    /**
     * Override this with the code necessary to perform the action.
     */
    public void performAction() {
    }

    /**
     * Override this with the code necessary to cancel the action.
     */
    public void cancel() {
    }

    /**
     * Finish performing the action.
     */
    public void finish() {
        this.status = "completed";
        this.timeCompleted = Utils.timestamp();
        this.thing.actionNotify(this);
    }
}
