package org.mozilla_iot.webthing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;


public class WebThingServer extends RouterNanoHTTPD {
    private int port;
    private String ip;
    private Thing thing;
    private boolean isTls;

    public class SSLOptions {

    }

    public WebThingServer(Thing thing) {
        this(thing, 80, null);
    }

    public WebThingServer(Thing thing, Integer port) {
        this(thing, port, null);
    }

    public WebThingServer(Thing thing, Integer port, SSLOptions sslOptions) {
        super(port);
        this.port = port;
        this.thing = thing;
        this.ip = Utils.getIP();
        this.isTls = sslOptions != null;

        addRoute("/",
                 ThingHandler.class,
                 this.thing,
                 this.ip,
                 this.port,
                 this.isTls);
        addRoute("/properties", PropertiesHandler.class, this.thing);
        addRoute("/properties/:propertyName",
                 PropertyHandler.class,
                 this.thing);
        addRoute("/actions", ActionsHandler.class, this.thing);
        addRoute("/actions/:actionName/:actionId",
                 ActionHandler.class,
                 this.thing);
        addRoute("/events", EventsHandler.class, this.thing);
    }

    private static class BaseHandler implements UriResponder {
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    "text/plain",
                                                    "");
        }

        public Response put(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    "text/plain",
                                                    "");

        }

        public Response post(UriResource uriResource,
                             Map<String, String> urlParams,
                             IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    "text/plain",
                                                    "");

        }

        public Response delete(UriResource uriResource,
                               Map<String, String> urlParams,
                               IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    "text/plain",
                                                    "");

        }

        public Response other(String method,
                              UriResource uriResource,
                              Map<String, String> urlParams,
                              IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    "text/plain",
                                                    "");

        }

        public String getUriParam(String uri, int index) {
            String[] parts = uri.split("/");
            if (parts.length <= index) {
                return null;
            }

            return parts[index];
        }
    }

    private static class ThingHandler extends BaseHandler {
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String ip = uriResource.initParameter(1, String.class);
            int port = uriResource.initParameter(2, int.class);
            boolean isTls = uriResource.initParameter(3, boolean.class);

            String wsPath = String.format("%s://%s:%d/",
                                          isTls ? "wss" : "ws",
                                          ip,
                                          port);

            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    thing.asThingDescription(
                                                            wsPath,
                                                            null).toString());
        }
    }

    private static class PropertiesHandler extends BaseHandler {
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            // TODO: this is not yet defined in the spec
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    "");
        }
    }

    private static class PropertyHandler extends BaseHandler {
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String propertyName = this.getUriParam(uriResource.getUri(), 1);
            if (!thing.hasProperty(propertyName)) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        "text/plain",
                                                        "");
            }

            JSONObject obj = new JSONObject();
            try {
                obj.put(propertyName, thing.getProperty(propertyName));
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                        "application/json",
                                                        obj.toString());
            } catch (JSONException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                                        "text/plain",
                                                        "");
            }
        }

        public Response put(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String propertyName = this.getUriParam(uriResource.getUri(), 1);
            if (!thing.hasProperty(propertyName)) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        "text/plain",
                                                        "");
            }

            Map<String, String> map = new HashMap<>();
            JSONObject json;
            try {
                session.parseBody(map);
                String data = map.get("content");
                json = new JSONObject(data);
            } catch (IOException | ResponseException | JSONException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                                        "text/plain",
                                                        "");
            }

            if (!json.has(propertyName)) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                                        "text/plain",
                                                        "");
            }

            try {
                thing.setProperty(propertyName, json.get(propertyName));

                JSONObject obj = new JSONObject();
                obj.put(propertyName, thing.getProperty(propertyName));
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                        "application/json",
                                                        obj.toString());
            } catch (JSONException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                                        "text/plain",
                                                        "");
            }
        }
    }

    private static class ActionsHandler extends BaseHandler {
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    thing.getActionDescriptions()
                                                         .toString());
        }

        public Response post(UriResource uriResource,
                             Map<String, String> urlParams,
                             IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);

            Map<String, String> map = new HashMap<>();
            JSONObject json;
            try {
                session.parseBody(map);
                String data = map.get("postData");
                json = new JSONObject(data);
            } catch (IOException | ResponseException | JSONException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                                        "text/plain",
                                                        "");
            }

            try {
                JSONObject response = new JSONObject();
                JSONArray actionNames = json.names();
                for (int i = 0; i < actionNames.length(); ++i) {
                    String actionName = (String)actionNames.get(i);
                    Action action = thing.performAction(actionName,
                                                        json.getJSONObject(
                                                                actionName));

                    JSONObject inner = new JSONObject();
                    inner.put("href", action.getHref());
                    inner.put("status", action.getStatus());
                    response.put(actionName, inner);
                }
                return NanoHTTPD.newFixedLengthResponse(Response.Status.CREATED,
                                                        "application/json",
                                                        response.toString());
            } catch (JSONException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                                        "text/plain",
                                                        "");
            }
        }
    }

    private static class ActionHandler extends BaseHandler {
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String actionName = this.getUriParam(uriResource.getUri(), 1);
            String actionId = this.getUriParam(uriResource.getUri(), 2);

            Action action = thing.getAction(actionName, actionId);
            if (action == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        "text/plain",
                                                        "");
            }

            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    action.asActionDescription()
                                                          .toString());

        }

        public Response put(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            // TODO: this is not yet defined in the spec
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    "");

        }

        public Response delete(UriResource uriResource,
                               Map<String, String> urlParams,
                               IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String actionName = this.getUriParam(uriResource.getUri(), 1);
            String actionId = this.getUriParam(uriResource.getUri(), 2);

            Action action = thing.getAction(actionName, actionId);
            if (action == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        "text/plain",
                                                        "");
            }

            action.cancel();
            return NanoHTTPD.newFixedLengthResponse(Response.Status.NO_CONTENT,
                                                    "text/plain",
                                                    "");

        }
    }

    private static class EventsHandler extends BaseHandler {
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    thing.getEventDescriptions()
                                                         .toString());
        }
    }
}