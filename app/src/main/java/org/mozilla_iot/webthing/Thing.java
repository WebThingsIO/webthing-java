package org.mozilla_iot.webthing;

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

public class Thing {
    private String type;
    private String name;
    private String description;
    private Map<String, Property> properties;
    private Map<String, AvailableAction> availableActions;
    private Map<String, AvailableEvent> availableEvents;
    private Map<String, List<Action>> actions;
    private List<Event> events;
    private Set<Object> subscribers;

    private class AvailableEvent {
        private String description;
        private Set<Object> subscribers;

        public AvailableEvent(String description) {
            this.description = description;
            this.subscribers = new HashSet<>();
        }

        public String getDescription() {
            return this.description;
        }

        public void addSubscriber(Object ws) {
            this.subscribers.add(ws);
        }

        public void removeSubscriber(Object ws) {
            if (this.subscribers.contains(ws)) {
                this.subscribers.remove(ws);
            }
        }
    }

    private class AvailableAction {
        private String description;
        private Class cls;

        public AvailableAction(String description, Class cls) {
            this.description = description;
            this.cls = cls;
        }

        public String getDescription() {
            return this.description;
        }

        public Class getCls() {
            return this.cls;
        }
    }

    public Thing() {
        this("", "thing", "");
    }

    public Thing(String name) {
        this(name, "thing", "");
    }

    public Thing(String name, String type) {
        this(name, type, "");
    }

    public Thing(String name, String type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.properties = new HashMap<>();
        this.availableEvents = new HashMap<>();
        this.events = new ArrayList<>();
    }

    public JSONObject asThingDescription(String wsPath, String uiPath) {
        JSONObject obj = new JSONObject();
        JSONObject actions = new JSONObject();
        JSONObject events = new JSONObject();

        this.availableActions.forEach((name, value) -> {
            JSONObject inner = new JSONObject();

            try {
                inner.put("description", value.getDescription());
                actions.put(name, inner);
            } catch (JSONException e) {
            }
        });

        this.availableEvents.forEach((name, value) -> {
            JSONObject inner = new JSONObject();

            try {
                inner.put("description", value.getDescription());
                events.put(name, inner);
            } catch (JSONException e) {
            }
        });

        try {
            obj.put("name", this.getName());
            obj.put("href", "/");
            obj.put("type", this.getType());
            obj.put("properties", this.getPropertyDescriptions());
            obj.put("actions", actions);
            obj.put("events", events);

            JSONObject propertiesLink = new JSONObject();
            propertiesLink.put("rel", "properties");
            propertiesLink.put("href", "/properties");
            obj.accumulate("links", propertiesLink);

            JSONObject actionsLink = new JSONObject();
            actionsLink.put("rel", "actions");
            actionsLink.put("href", "/actions");
            obj.accumulate("links", actionsLink);

            JSONObject eventsLink = new JSONObject();
            eventsLink.put("rel", "events");
            eventsLink.put("href", "/events");
            obj.accumulate("links", eventsLink);

            if (wsPath != null) {
                JSONObject wsLink = new JSONObject();
                wsLink.put("rel", "alternate");
                wsLink.put("href", wsPath);
                obj.accumulate("links", wsLink);
            }

            if (uiPath != null) {
                JSONObject uiLink = new JSONObject();
                uiLink.put("rel", "alternate");
                uiLink.put("mediaType", "text/html");
                uiLink.put("href", uiPath);
                obj.accumulate("links", uiLink);
            }

            return obj;
        } catch (JSONException e) {
            return null;
        }
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

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

    public JSONArray getActionDescriptions() {
        JSONArray array = new JSONArray();
        this.actions.forEach((actionName, list) -> {
            list.forEach((action) -> {
                array.put(action.asActionDescription());
            });
        });
        return array;
    }

    public JSONArray getEventDescriptions() {
        JSONArray array = new JSONArray();
        this.events.forEach((event) -> {
            array.put(event.asEventDescription());
        });
        return array;
    }

    public void addProperty(Property property) {
        this.properties.put(property.getName(), property);
    }

    public void removeProperty(Property property) {
        if (this.properties.containsKey(property.getName())) {
            this.properties.remove(property.getName());
        }
    }

    public Property findProperty(String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            return this.properties.get(propertyName);
        }

        return null;
    }

    public <T> T getProperty(String propertyName) {
        Property<T> prop = this.findProperty(propertyName);
        if (prop != null) {
            return prop.getValue();
        }

        return null;
    }

    public boolean hasProperty(String propertyName) {
        return this.properties.containsKey(propertyName);
    }

    public <T> void setProperty(String propertyName, T value) {
        Property<T> prop = this.findProperty(propertyName);
        if (prop == null) {
            return;
        }

        prop.setValue(value);
    }

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

    public void addEvent(Event event) {
        this.events.add(event);
        this.eventNotify(event);
    }

    public void addEventDescription(String name, String description) {
        this.availableEvents.put(name, new AvailableEvent(description));
    }

    public Action performAction(String actionName, JSONObject args) {
        if (!this.availableActions.containsKey(actionName)) {
            return null;
        }

        Class cls = this.availableActions.get(actionName).getCls();
        try {
            Constructor constructor = cls.getConstructor(JSONObject.class);
            Action action = (Action)constructor.newInstance(new Object[]{args});
            this.actions.get(actionName).add(action);
            return action;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public void addActionDescription(String name,
                                     String description,
                                     Class cls) {
        this.availableActions.put(name, new AvailableAction(description, cls));
        this.actions.put(name, new ArrayList<Action>());
    }

    public void addSubscriber(Object ws) {
        this.subscribers.add(ws);
    }

    public void removeSubscriber(Object ws) {
        if (this.subscribers.contains(ws)) {
            this.subscribers.remove(ws);
        }

        this.availableEvents.forEach((name, value) -> {
            value.removeSubscriber(ws);
        });
    }

    public void addEventSubscriber(String name, Object ws) {
        if (this.availableEvents.containsKey(name)) {
            this.availableEvents.get(name).addSubscriber(ws);
        }
    }

    public void removeEventSubscriber(String name, Object ws) {
        if (this.availableEvents.containsKey(name)) {
            this.availableEvents.get(name).removeSubscriber(ws);
        }
    }

    public void propertyNotify(Property property) {

    }

    public void actionNotify(Action action) {

    }

    public void eventNotify(Event event) {

    }
}