/**
 * High-level Thing base class implementation.
 */
package org.mozilla.iot.webthing;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Web Thing.
 */
public class Thing {
    private String type;
    private String name;
    private String description;
    private Map<String, Property> properties;
    private Map<String, AvailableAction> availableActions;
    private Map<String, AvailableEvent> availableEvents;
    private Map<String, List<Action>> actions;
    private List<Event> events;
    private Set<WebThingServer.ThingHandler.ThingWebSocket> subscribers;
    private String hrefPrefix;
    private String wsHref;
    private String uiHref;

    /**
     * Initialize the object.
     *
     * @param name The thing's name
     */
    public Thing(String name) {
        this(name, "thing", "");
    }

    /**
     * Initialize the object.
     *
     * @param name The thing's name
     * @param type The thing's type
     */
    public Thing(String name, String type) {
        this(name, type, "");
    }

    /**
     * Initialize the object.
     *
     * @param name        The thing's name
     * @param type        The thing's type
     * @param description Description of the thing
     */
    public Thing(String name, String type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.properties = new HashMap<>();
        this.availableActions = new HashMap<>();
        this.availableEvents = new HashMap<>();
        this.actions = new HashMap<>();
        this.events = new ArrayList<>();
        this.subscribers = new HashSet<>();
        this.hrefPrefix = "";
        this.wsHref = null;
        this.uiHref = null;
    }

    /**
     * Return the thing state as a Thing Description.
     *
     * @return Current thing state.
     */
    public JSONObject asThingDescription() {
        JSONObject obj = new JSONObject();
        JSONObject actions = new JSONObject();
        JSONObject events = new JSONObject();

        this.availableActions.forEach((name, value) -> {
            actions.put(name, value.getMetadata());
        });

        this.availableEvents.forEach((name, value) -> {
            events.put(name, value.getMetadata());
        });

        try {
            obj.put("name", this.getName());
            obj.put("href",
                    this.hrefPrefix.length() > 0 ? this.hrefPrefix : "/");
            obj.put("type", this.getType());
            obj.put("properties", this.getPropertyDescriptions());
            obj.put("actions", actions);
            obj.put("events", events);

            if (this.description != null) {
                obj.put("description", this.getDescription());
            }

            JSONObject propertiesLink = new JSONObject();
            propertiesLink.put("rel", "properties");
            propertiesLink.put("href",
                               String.format("%s/properties", this.hrefPrefix));
            obj.accumulate("links", propertiesLink);

            JSONObject actionsLink = new JSONObject();
            actionsLink.put("rel", "actions");
            actionsLink.put("href",
                            String.format("%s/actions", this.hrefPrefix));
            obj.accumulate("links", actionsLink);

            JSONObject eventsLink = new JSONObject();
            eventsLink.put("rel", "events");
            eventsLink.put("href", String.format("%s/events", this.hrefPrefix));
            obj.accumulate("links", eventsLink);

            if (this.wsHref != null) {
                JSONObject wsLink = new JSONObject();
                wsLink.put("rel", "alternate");
                wsLink.put("href", this.wsHref);
                obj.accumulate("links", wsLink);
            }

            if (this.uiHref != null) {
                JSONObject uiLink = new JSONObject();
                uiLink.put("rel", "alternate");
                uiLink.put("mediaType", "text/html");
                uiLink.put("href", this.uiHref);
                obj.accumulate("links", uiLink);
            }

            return obj;
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Set the prefix of any hrefs associated with this thing.
     *
     * @param prefix The prefix
     */
    public void setHrefPrefix(String prefix) {
        this.hrefPrefix = prefix;

        this.availableActions.forEach((name, value) -> {
            value.setHrefPrefix(prefix);
        });

        this.availableEvents.forEach((name, value) -> {
            value.setHrefPrefix(prefix);
        });

        this.properties.forEach((name, value) -> {
            value.setHrefPrefix(prefix);
        });

        this.actions.forEach((actionName, list) -> {
            list.forEach((action) -> {
                action.setHrefPrefix(prefix);
            });
        });
    }

    /**
     * Set the href of this thing's websocket.
     *
     * @param href The href
     */
    public void setWsHref(String href) {
        this.wsHref = href;
    }

    /**
     * Set the href of this thing's custom UI.
     *
     * @param href The href
     */
    public void setUiHref(String href) {
        this.uiHref = href;
    }

    /**
     * Get the name of the thing.
     *
     * @return The name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the type of the thing.
     *
     * @return The type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Get the description of the thing.
     *
     * @return The description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Get the thing's properties as a JSONObject.
     *
     * @return Properties, i.e. name: description.
     */
    public JSONObject getPropertyDescriptions() {
        JSONObject obj = new JSONObject();

        this.properties.forEach((name, value) -> {
            try {
                obj.put(name, value.asPropertyDescription());
            } catch (JSONException e) {
            }
        });

        return obj;
    }

    /**
     * Get the thing's actions as a JSONArray.
     *
     * @return Action descriptions.
     */
    public JSONArray getActionDescriptions() {
        JSONArray array = new JSONArray();
        this.actions.forEach((actionName, list) -> {
            list.forEach((action) -> {
                array.put(action.asActionDescription());
            });
        });
        return array;
    }

    /**
     * Get the thing's events as a JSONArray.
     *
     * @return Event descriptions.
     */
    public JSONArray getEventDescriptions() {
        JSONArray array = new JSONArray();
        this.events.forEach((event) -> {
            array.put(event.asEventDescription());
        });
        return array;
    }

    /**
     * Add a property to this thing.
     *
     * @param property Property to add.
     */
    public void addProperty(Property property) {
        property.setHrefPrefix(this.hrefPrefix);
        this.properties.put(property.getName(), property);
    }

    /**
     * Remove a property from this thing.
     *
     * @param property Property to remove.
     */
    public void removeProperty(Property property) {
        if (this.properties.containsKey(property.getName())) {
            this.properties.remove(property.getName());
        }
    }

    /**
     * Find a property by name.
     *
     * @param propertyName Name of the property to find
     * @return Property if found, else null.
     */
    public Property findProperty(String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            return this.properties.get(propertyName);
        }

        return null;
    }

    /**
     * Get a property's value.
     *
     * @param propertyName Name of the property to get the value of
     * @param <T>          Type of the property value
     * @return Current property value if found, else null.
     */
    public <T> T getProperty(String propertyName) {
        Property<T> prop = this.findProperty(propertyName);
        if (prop != null) {
            return prop.getValue();
        }

        return null;
    }

    /**
     * Determine whether or not this thing has a given property.
     *
     * @param propertyName The property to look for
     * @return Indication of property presence.
     */
    public boolean hasProperty(String propertyName) {
        return this.properties.containsKey(propertyName);
    }

    /**
     * Set a property value.
     *
     * @param propertyName Name of the property to set
     * @param value        Value to set
     * @param <T>          Type of the property value
     */
    public <T> void setProperty(String propertyName, T value) {
        Property<T> prop = this.findProperty(propertyName);
        if (prop == null) {
            return;
        }

        prop.setValue(value);
    }

    /**
     * Get an action.
     *
     * @param actionName Name of the action
     * @param actionId   ID of the action
     * @return The requested action if found, else null.
     */
    public Action getAction(String actionName, String actionId) {
        if (!this.actions.containsKey(actionName)) {
            return null;
        }

        List<Action> actions = this.actions.get(actionName);
        for (int i = 0; i < actions.size(); ++i) {
            Action action = actions.get(i);
            if (actionId.equals(action.getId())) {
                return action;
            }
        }

        return null;
    }

    /**
     * Add a new event and notify subscribers.
     *
     * @param event The event that occurred.
     */
    public void addEvent(Event event) {
        this.events.add(event);
        this.eventNotify(event);
    }

    /**
     * Add an available event.
     *
     * @param name     Name of the event
     * @param metadata Event metadata, i.e. type, description, etc., as a Map
     */
    public void addAvailableEvent(String name, Map<String, Object> metadata) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        metadata.put("href", String.format("/events/%s", name));

        this.availableEvents.put(name, new AvailableEvent(metadata));
    }

    /**
     * Perform an action on the thing.
     *
     * @param actionName Name of the action
     * @param input      Any action inputs
     * @return The action that was created.
     */
    public Action performAction(String actionName, JSONObject input) {
        if (!this.availableActions.containsKey(actionName)) {
            return null;
        }

        AvailableAction actionType = this.availableActions.get(actionName);
        if (!actionType.validateActionInput(input)) {
            return null;
        }

        Class cls = actionType.getCls();
        try {
            Constructor constructor =
                    cls.getConstructor(Thing.class, JSONObject.class);
            Action action =
                    (Action)constructor.newInstance(new Object[]{this, input});
            action.setHrefPrefix(this.hrefPrefix);
            this.actionNotify(action);
            this.actions.get(actionName).add(action);
            return action;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.out.println(e);
            return null;
        }
    }

    /**
     * Remove an existing action.
     *
     * @param actionName name of the action
     * @param actionId   ID of the action
     * @return Boolean indicating the presence of the action.
     */
    public boolean removeAction(String actionName, String actionId) {
        Action action = this.getAction(actionName, actionId);
        if (action == null) {
            return false;
        }

        action.cancel();
        this.actions.get(actionName).remove(action);
        return true;
    }

    /**
     * Add an available action.
     *
     * @param name     Name of the action
     * @param metadata Action metadata, i.e. type, description, etc., as a Map
     * @param cls      Class to instantiate for this action
     */
    public void addAvailableAction(String name,
                                   Map<String, Object> metadata,
                                   Class cls) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        metadata.put("href", String.format("/actions/%s", name));

        this.availableActions.put(name, new AvailableAction(metadata, cls));
        this.actions.put(name, new ArrayList<>());
    }

    /**
     * Add a new websocket subscriber.
     *
     * @param ws The websocket
     */
    public void addSubscriber(WebThingServer.ThingHandler.ThingWebSocket ws) {
        this.subscribers.add(ws);
    }

    /**
     * Remove a websocket subscriber.
     *
     * @param ws The websocket
     */
    public void removeSubscriber(WebThingServer.ThingHandler.ThingWebSocket ws) {
        if (this.subscribers.contains(ws)) {
            this.subscribers.remove(ws);
        }

        this.availableEvents.forEach((name, value) -> {
            this.removeEventSubscriber(name, ws);
        });
    }

    /**
     * Add a new websocket subscriber to an event.
     *
     * @param name Name of the event
     * @param ws   The websocket
     */
    public void addEventSubscriber(String name,
                                   WebThingServer.ThingHandler.ThingWebSocket ws) {
        if (this.availableEvents.containsKey(name)) {
            this.availableEvents.get(name).addSubscriber(ws);
        }
    }

    /**
     * Remove a websocket subscriber from an event.
     *
     * @param name Name of the event
     * @param ws   The websocket
     */
    public void removeEventSubscriber(String name,
                                      WebThingServer.ThingHandler.ThingWebSocket ws) {
        if (this.availableEvents.containsKey(name)) {
            this.availableEvents.get(name).removeSubscriber(ws);
        }
    }

    /**
     * Notify all subscribers of a property change.
     *
     * @param property The property that changed
     */
    public void propertyNotify(Property property) {
        JSONObject json = new JSONObject();
        JSONObject inner = new JSONObject();

        inner.put(property.getName(), property.getValue());
        json.put("messageType", "propertyStatus");
        json.put("data", inner);

        String message = json.toString();

        this.subscribers.forEach((subscriber) -> {
            subscriber.sendMessage(message);
        });
    }

    /**
     * Notify all subscribers of an action status change.
     *
     * @param action The action whose status changed
     */
    public void actionNotify(Action action) {
        JSONObject json = new JSONObject();

        json.put("messageType", "actionStatus");
        json.put("data", action.asActionDescription());

        String message = json.toString();

        this.subscribers.forEach((subscriber) -> {
            subscriber.sendMessage(message);
        });
    }

    /**
     * Notify all subscribers of an event.
     *
     * @param event The event that occurred
     */
    public void eventNotify(Event event) {
        String eventName = event.getName();
        if (!this.availableEvents.containsKey(eventName)) {
            return;
        }

        JSONObject json = new JSONObject();

        json.put("messageType", "event");
        json.put("data", event.asEventDescription());

        String message = json.toString();

        this.availableEvents.get(eventName)
                            .getSubscribers()
                            .forEach((subscriber) -> {
                                subscriber.sendMessage(message);
                            });
    }

    /**
     * Class to describe an event available for subscription.
     */
    private class AvailableEvent {
        private Map<String, Object> metadata;
        private Set<WebThingServer.ThingHandler.ThingWebSocket> subscribers;

        /**
         * Initialize the object.
         *
         * @param metadata The event metadata
         */
        public AvailableEvent(Map<String, Object> metadata) {
            this.metadata = metadata;
            this.subscribers = new HashSet<>();
        }

        /**
         * Set the prefix of this action's href.
         *
         * @param prefix The prefix
         */
        public void setHrefPrefix(String prefix) {
            String href = (String)this.metadata.get("href");
            this.metadata.put("href", prefix + href);
        }

        /**
         * Get the event metadata.
         *
         * @return The metadata.
         */
        public Map<String, Object> getMetadata() {
            return this.metadata;
        }

        /**
         * Add a websocket subscriber to the event.
         *
         * @param ws The websocket
         */
        public void addSubscriber(WebThingServer.ThingHandler.ThingWebSocket ws) {
            this.subscribers.add(ws);
        }

        /**
         * Remove a websocket subscriber from the event.
         *
         * @param ws The websocket
         */
        public void removeSubscriber(WebThingServer.ThingHandler.ThingWebSocket ws) {
            if (this.subscribers.contains(ws)) {
                this.subscribers.remove(ws);
            }
        }

        /**
         * Get the set of subscribers for the event.
         *
         * @return The set of subscribers.
         */
        public Set<WebThingServer.ThingHandler.ThingWebSocket> getSubscribers() {
            return this.subscribers;
        }
    }

    /**
     * Class to describe an action available to be taken.
     */
    private class AvailableAction {
        private Map<String, Object> metadata;
        private Class cls;
        private Schema schema;

        /**
         * Initialize the object.
         *
         * @param metadata The action metadata
         * @param cls      Class to instantiate for the action
         */
        public AvailableAction(Map<String, Object> metadata, Class cls) {
            this.metadata = metadata;
            this.cls = cls;

            if (metadata.containsKey("input")) {
                JSONObject rawSchema =
                        new JSONObject((Map<String, Object>)metadata.get("input"));
                this.schema = SchemaLoader.load(rawSchema);
            } else {
                this.schema = null;
            }
        }

        /**
         * Set the prefix of this action's href.
         *
         * @param prefix The prefix
         */
        public void setHrefPrefix(String prefix) {
            String href = (String)this.metadata.get("href");
            this.metadata.put("href", prefix + href);
        }

        /**
         * Get the action metadata.
         *
         * @return The metadata.
         */
        public Map<String, Object> getMetadata() {
            return this.metadata;
        }

        /**
         * Get the class to instantiate for the action.
         *
         * @return The class.
         */
        public Class getCls() {
            return this.cls;
        }

        /**
         * Validate the input for a new action.
         *
         * @param actionInput The input to validate
         * @return Boolean indicating validation success.
         */
        public boolean validateActionInput(JSONObject actionInput) {
            if (this.schema == null) {
                return true;
            }

            try {
                this.schema.validate(actionInput);
            } catch (ValidationException e) {
                return false;
            }

            return true;
        }
    }
}