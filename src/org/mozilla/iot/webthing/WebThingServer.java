package org.mozilla.iot.webthing;

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

    private static class ActionRunner extends Thread {
        private Action action;

        public ActionRunner(Action action) {
            this.action = action;
        }

        public void run() {
            this.action.start();
        }
    }

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

    public static class BaseHandler implements UriResponder {
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);
        }

        public Response put(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);

        }

        public Response post(UriResource uriResource,
                             Map<String, String> urlParams,
                             IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);

        }

        public Response delete(UriResource uriResource,
                               Map<String, String> urlParams,
                               IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);

        }

        public Response other(String method,
                              UriResource uriResource,
                              Map<String, String> urlParams,
                              IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);

        }

        public String getUriParam(String uri, int index) {
            String[] parts = uri.split("/");
            if (parts.length <= index) {
                return null;
            }

            return parts[index];
        }

        public JSONObject parseBody(IHTTPSession session) {
            Integer contentLength = Integer.parseInt(session.getHeaders()
                                                            .get("content-length"));
            byte[] buffer = new byte[contentLength];
            try {
                session.getInputStream().read(buffer, 0, contentLength);
                JSONObject obj = new JSONObject(new String(buffer));
                return obj;
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static class ThingHandler extends BaseHandler {
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String ip = uriResource.initParameter(1, String.class);
            Integer port = uriResource.initParameter(2, Integer.class);
            Boolean isTls = uriResource.initParameter(3, Boolean.class);

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

    public static class PropertiesHandler extends BaseHandler {
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            // TODO: this is not yet defined in the spec
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    "");
        }
    }

    public static class PropertyHandler extends BaseHandler {
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String propertyName = this.getUriParam(session.getUri(), 2);
            if (!thing.hasProperty(propertyName)) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            JSONObject obj = new JSONObject();
            try {
                Object value = thing.getProperty(propertyName);
                if (value == null) {
                    obj.put(propertyName, JSONObject.NULL);
                } else {
                    obj.putOpt(propertyName, value);
                }
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                        "application/json",
                                                        obj.toString());
            } catch (JSONException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                                        null,
                                                        null);
            }
        }

        @Override
        public Response put(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String propertyName = this.getUriParam(session.getUri(), 2);
            if (!thing.hasProperty(propertyName)) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            JSONObject json = this.parseBody(session);
            if (json == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                                        null,
                                                        null);
            }

            if (!json.has(propertyName)) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                                        null,
                                                        null);
            }

            try {
                thing.setProperty(propertyName, json.get(propertyName));

                JSONObject obj = new JSONObject();
                obj.putOpt(propertyName, thing.getProperty(propertyName));
                return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                        "application/json",
                                                        obj.toString());
            } catch (JSONException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                                        null,
                                                        null);
            }
        }
    }

    public static class ActionsHandler extends BaseHandler {
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    thing.getActionDescriptions()
                                                         .toString());
        }

        @Override
        public Response post(UriResource uriResource,
                             Map<String, String> urlParams,
                             IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);

            JSONObject json = this.parseBody(session);
            if (json == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                                        null,
                                                        null);
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

                    (new ActionRunner(action)).start();
                }
                return NanoHTTPD.newFixedLengthResponse(Response.Status.CREATED,
                                                        "application/json",
                                                        response.toString());
            } catch (JSONException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                                        null,
                                                        null);
            }
        }
    }

    public static class ActionHandler extends BaseHandler {
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String actionName = this.getUriParam(session.getUri(), 2);
            String actionId = this.getUriParam(session.getUri(), 3);

            Action action = thing.getAction(actionName, actionId);
            if (action == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    action.asActionDescription()
                                                          .toString());

        }

        @Override
        public Response put(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            // TODO: this is not yet defined in the spec
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    "");

        }

        @Override
        public Response delete(UriResource uriResource,
                               Map<String, String> urlParams,
                               IHTTPSession session) {
            Thing thing = uriResource.initParameter(0, Thing.class);
            String actionName = this.getUriParam(session.getUri(), 2);
            String actionId = this.getUriParam(session.getUri(), 3);

            Action action = thing.getAction(actionName, actionId);
            if (action == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            action.cancel();

            return NanoHTTPD.newFixedLengthResponse(Response.Status.NO_CONTENT,
                                                    null,
                                                    null);

        }
    }

    public static class EventsHandler extends BaseHandler {
        @Override
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