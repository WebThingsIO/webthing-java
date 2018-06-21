/**
 * Java Web Thing server implementation.
 */
package org.mozilla.iot.webthing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.net.ssl.SSLServerSocketFactory;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import fi.iki.elonen.router.RouterNanoHTTPD;

/**
 * Server to represent a Web Thing over HTTP.
 */
public class WebThingServer extends RouterNanoHTTPD {
    private int port;
    private String ip;
    private ThingsType things;
    private String name;
    private boolean isTls;
    private JmDNS jmdns;

    /**
     * Initialize the WebThingServer on port 80.
     *
     * @param things List of Things managed by this server
     * @throws IOException          If server fails to bind.
     * @throws NullPointerException If something bad happened.
     */
    public WebThingServer(ThingsType things)
            throws IOException, NullPointerException {
        this(things, 80, null);
    }

    /**
     * Initialize the WebThingServer.
     *
     * @param things List of Things managed by this server
     * @param port   Port to listen on
     * @throws IOException          If server fails to bind.
     * @throws NullPointerException If something bad happened.
     */
    public WebThingServer(ThingsType things, Integer port)
            throws IOException, NullPointerException {
        this(things, port, null);
    }

    /**
     * Initialize the WebThingServer.
     *
     * @param things     List of Things managed by this server
     * @param port       Port to listen on
     * @param sslOptions SSL options to pass to the NanoHTTPD server
     * @throws IOException          If server fails to bind.
     * @throws NullPointerException If something bad happened.
     */
    public WebThingServer(ThingsType things,
                          Integer port,
                          SSLOptions sslOptions)
            throws IOException, NullPointerException {
        super(port);
        this.port = port;
        this.things = things;
        this.name = things.getName();
        this.ip = Utils.getIP();
        this.isTls = sslOptions != null;

        if (this.isTls) {
            super.makeSecure(sslOptions.getSocketFactory(),
                             sslOptions.getProtocols());
        }

        this.setRoutePrioritizer(new InsertionOrderRoutePrioritizer());

        if (MultipleThings.class.isInstance(things)) {
            String wsBase = String.format("%s://%s:%d/",
                                          this.isTls ? "wss" : "ws",
                                          this.ip,
                                          this.port);

            List<Thing> list = things.getThings();
            for (int i = 0; i < list.size(); ++i) {
                Thing thing = list.get(i);
                thing.setHrefPrefix(String.format("/%d", i));
                thing.setWsHref(wsBase + Integer.toString(i));
            }

            // These are matched in the order they are added.
            addRoute("/:thingId/properties/:propertyName",
                     PropertyHandler.class,
                     this.things);
            addRoute("/:thingId/properties",
                     PropertiesHandler.class,
                     this.things);
            addRoute("/:thingId/actions/:actionName/:actionId",
                     ActionIDHandler.class,
                     this.things);
            addRoute("/:thingId/actions/:actionName",
                     ActionHandler.class,
                     this.things);
            addRoute("/:thingId/actions", ActionsHandler.class, this.things);
            addRoute("/:thingId/events/:eventName",
                     EventHandler.class,
                     this.things);
            addRoute("/:thingId/events", EventsHandler.class, this.things);
            addRoute("/:thingId", ThingHandler.class, this.things);
            addRoute("/", ThingsHandler.class, this.things);
        } else {
            String wsHref = String.format("%s://%s:%d",
                                          this.isTls ? "wss" : "ws",
                                          this.ip,
                                          this.port);

            Thing thing = things.getThing(0);
            thing.setWsHref(wsHref);

            // These are matched in the order they are added.
            addRoute("/properties/:propertyName",
                     PropertyHandler.class,
                     this.things);
            addRoute("/properties", PropertiesHandler.class, this.things);
            addRoute("/actions/:actionName/:actionId",
                     ActionIDHandler.class,
                     this.things);
            addRoute("/actions/:actionName", ActionHandler.class, this.things);
            addRoute("/actions", ActionsHandler.class, this.things);
            addRoute("/events/:eventName", EventHandler.class, this.things);
            addRoute("/events", EventsHandler.class, this.things);
            addRoute("/", ThingHandler.class, this.things);
        }
    }

    /**
     * Start listening for incoming connections.
     *
     * @param daemon Whether or not to daemonize the server
     * @throws IOException on failure to listen on port
     */
    public void start(boolean daemon) throws IOException {
        this.jmdns = JmDNS.create(InetAddress.getLocalHost());

        String url = String.format("url=%s://%s:%d/",
                                   this.isTls ? "https" : "http",
                                   this.ip,
                                   this.port);
        ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local",
                                                     this.name,
                                                     "_webthing",
                                                     this.port,
                                                     url);
        this.jmdns.registerService(serviceInfo);

        super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, daemon);
    }

    /**
     * Stop listening.
     */
    public void stop() {
        this.jmdns.unregisterAllServices();
        super.stop();
    }

    interface ThingsType {
        /**
         * Get the thing at the given index.
         *
         * @return The thing, or null.
         */
        Thing getThing(int idx);

        /**
         * Get the list of things.
         *
         * @return The list of things.
         */
        List<Thing> getThings();

        /**
         * Get the mDNS server name.
         *
         * @return The server name.
         */
        String getName();
    }

    /**
     * Thread to perform an action.
     */
    private static class ActionRunner extends Thread {
        private Action action;

        /**
         * Initialize the object.
         *
         * @param action The action to perform
         */
        public ActionRunner(Action action) {
            this.action = action;
        }

        /**
         * Perform the action.
         */
        public void run() {
            this.action.start();
        }
    }

    /**
     * Class to hold options required by SSL server.
     */
    public static class SSLOptions {
        private String path;
        private String password;
        private String[] protocols;

        /**
         * Initialize the object.
         *
         * @param keystorePath     Path to the Java keystore (.jks) file
         * @param keystorePassword Password to open the keystore
         */
        public SSLOptions(String keystorePath, String keystorePassword) {
            this(keystorePath, keystorePassword, null);
        }

        /**
         * Initialize the object.
         *
         * @param keystorePath     Path to the Java keystore (.jks) file
         * @param keystorePassword Password to open the keystore
         * @param protocols        List of protocols to enable. Documentation
         *                         found here: https://docs.oracle.com/javase/8/docs/api/javax/net/ssl/SSLServerSocket.html#setEnabledProtocols-java.lang.String:A-
         */
        public SSLOptions(String keystorePath,
                          String keystorePassword,
                          String[] protocols) {
            this.path = keystorePath;
            this.password = keystorePassword;
            this.protocols = protocols;
        }

        /**
         * Create an SSLServerSocketFactory as required by NanoHTTPD.
         *
         * @return The socket factory.
         * @throws IOException If server fails to bind.
         */
        public SSLServerSocketFactory getSocketFactory() throws IOException {
            return NanoHTTPD.makeSSLSocketFactory(this.path,
                                                  this.password.toCharArray());
        }

        /**
         * Get the list of enabled protocols.
         *
         * @return The list of protocols.
         */
        public String[] getProtocols() {
            return this.protocols;
        }
    }

    /**
     * Base handler that responds to every request with a 405 Method Not
     * Allowed.
     */
    public static class BaseHandler implements UriResponder {
        /**
         * Handle a GET request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return 405 Method Not Allowed response.
         */
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);
        }

        /**
         * Handle a PUT request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return 405 Method Not Allowed response.
         */
        public Response put(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);
        }

        /**
         * Handle a POST request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return 405 Method Not Allowed response.
         */
        public Response post(UriResource uriResource,
                             Map<String, String> urlParams,
                             IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);
        }

        /**
         * Handle a DELETE request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return 405 Method Not Allowed response.
         */
        public Response delete(UriResource uriResource,
                               Map<String, String> urlParams,
                               IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);
        }

        /**
         * Handle any other request.
         *
         * @param method      The HTTP method
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return 405 Method Not Allowed response.
         */
        public Response other(String method,
                              UriResource uriResource,
                              Map<String, String> urlParams,
                              IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED,
                                                    null,
                                                    null);
        }

        /**
         * Get a parameter from the URI.
         *
         * @param uri   The URI
         * @param index Index of the parameter
         * @return The URI parameter, or null if index was invalid.
         */
        public String getUriParam(String uri, int index) {
            String[] parts = uri.split("/");
            if (parts.length <= index) {
                return null;
            }

            return parts[index];
        }

        /**
         * Parse a JSON body.
         *
         * @param session The HTTP session
         * @return The parsed JSON body as a JSONObject, or null on error.
         */
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

        /**
         * Get the thing this request is for.
         *
         * @param uriResource The URI resource that was matched
         * @param session     The HTTP session
         * @return The thing, or null if not found.
         */
        public Thing getThing(UriResource uriResource, IHTTPSession session) {
            ThingsType things = uriResource.initParameter(0, ThingsType.class);

            String thingId = this.getUriParam(session.getUri(), 1);
            int id;
            try {
                id = Integer.parseInt(thingId);
            } catch (NumberFormatException e) {
                id = 0;
            }

            return things.getThing(id);
        }
    }

    /**
     * Handle a request to / when the server manages multiple things.
     */
    public static class ThingsHandler extends BaseHandler {
        /**
         * Handle a GET request, including websocket requests.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            ThingsType things = uriResource.initParameter(0, ThingsType.class);

            JSONArray list = new JSONArray();
            for (Thing thing : things.getThings()) {
                list.put(thing.asThingDescription());
            }

            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    list.toString());
        }
    }

    /**
     * Handle a request to /.
     */
    public static class ThingHandler extends BaseHandler {
        /**
         * Handle a GET request, including websocket requests.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            Map<String, String> headers = session.getHeaders();
            if (isWebSocketRequested(session)) {
                if (!NanoWSD.HEADER_WEBSOCKET_VERSION_VALUE.equalsIgnoreCase(
                        headers.get(NanoWSD.HEADER_WEBSOCKET_VERSION))) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                                  NanoHTTPD.MIME_PLAINTEXT,
                                                  "Invalid Websocket-Version " +
                                                          headers.get(NanoWSD.HEADER_WEBSOCKET_VERSION));
                }

                if (!headers.containsKey(NanoWSD.HEADER_WEBSOCKET_KEY)) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                                  NanoHTTPD.MIME_PLAINTEXT,
                                                  "Missing Websocket-Key");
                }

                NanoWSD.WebSocket webSocket =
                        new ThingWebSocket(thing, session);
                Response handshakeResponse = webSocket.getHandshakeResponse();
                try {
                    handshakeResponse.addHeader(NanoWSD.HEADER_WEBSOCKET_ACCEPT,
                                                NanoWSD.makeAcceptKey(headers.get(
                                                        NanoWSD.HEADER_WEBSOCKET_KEY)));
                } catch (NoSuchAlgorithmException e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                                                  NanoHTTPD.MIME_PLAINTEXT,
                                                  "The SHA-1 Algorithm required for websockets is not available on the server.");
                }

                if (headers.containsKey(NanoWSD.HEADER_WEBSOCKET_PROTOCOL)) {
                    handshakeResponse.addHeader(NanoWSD.HEADER_WEBSOCKET_PROTOCOL,
                                                headers.get(NanoWSD.HEADER_WEBSOCKET_PROTOCOL)
                                                       .split(",")[0]);
                }

                return handshakeResponse;
            }

            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    thing.asThingDescription()
                                                         .toString());
        }

        /**
         * Determine whether or not this is a websocket connection.
         *
         * @param headers The HTTP request headers
         * @return Boolean indicating whether or not this is a websocket
         * connection.
         */
        private boolean isWebSocketConnectionHeader(Map<String, String> headers) {
            String connection = headers.get(NanoWSD.HEADER_CONNECTION);
            return connection != null && connection.toLowerCase()
                                                   .contains(NanoWSD.HEADER_CONNECTION_VALUE
                                                                     .toLowerCase());
        }

        /**
         * Determine whether or not a websocket was requested.
         *
         * @param session The HTTP session
         * @return Boolean indicating whether or not this is a websocket
         * request.
         */
        private boolean isWebSocketRequested(IHTTPSession session) {
            Map<String, String> headers = session.getHeaders();
            String upgrade = headers.get(NanoWSD.HEADER_UPGRADE);
            boolean isCorrectConnection = isWebSocketConnectionHeader(headers);
            boolean isUpgrade =
                    NanoWSD.HEADER_UPGRADE_VALUE.equalsIgnoreCase(upgrade);
            return isUpgrade && isCorrectConnection;
        }

        /**
         * Class to handle WebSockets to a Thing.
         */
        public static class ThingWebSocket extends NanoWSD.WebSocket {
            private final Thing thing;

            /**
             * Initialize the object.
             *
             * @param thing            The Thing managed by the server
             * @param handshakeRequest The initial handshake request
             */
            public ThingWebSocket(Thing thing, IHTTPSession handshakeRequest) {
                super(handshakeRequest);
                this.thing = thing;
            }

            /**
             * Handle a new connection.
             */
            @Override
            protected void onOpen() {
                this.thing.addSubscriber(this);
            }

            /**
             * Handle a close event on the socket.
             *
             * @param code              The close code
             * @param reason            The close reason
             * @param initiatedByRemote Whether or not the client closed the
             *                          socket
             */
            @Override
            protected void onClose(NanoWSD.WebSocketFrame.CloseCode code,
                                   String reason,
                                   boolean initiatedByRemote) {
                this.thing.removeSubscriber(this);
            }

            /**
             * Handle an incoming message.
             *
             * @param message The message to handle
             */
            @Override
            protected void onMessage(NanoWSD.WebSocketFrame message) {
                message.setUnmasked();
                String data = message.getTextPayload();
                JSONObject json = new JSONObject(data);

                if (!json.has("messageType") || !json.has("data")) {
                    JSONObject error = new JSONObject();
                    JSONObject inner = new JSONObject();

                    inner.put("status", "400 Bad Request");
                    inner.put("message", "Invalid message");
                    error.put("messageType", "error");
                    error.put("data", inner);

                    this.sendMessage(error.toString());

                    return;
                }

                String messageType = json.getString("messageType");
                JSONObject messageData = json.getJSONObject("data");
                switch (messageType) {
                    case "setProperty":
                        JSONArray propertyNames = messageData.names();
                        if (propertyNames == null) {
                            break;
                        }

                        for (int i = 0; i < propertyNames.length(); ++i) {
                            String propertyName = propertyNames.getString(i);
                            try {
                                this.thing.setProperty(propertyName,
                                                       messageData.get(
                                                               propertyName));
                            } catch (IllegalArgumentException e) {
                                JSONObject error = new JSONObject();
                                JSONObject inner = new JSONObject();

                                inner.put("status", "403 Forbidden");
                                inner.put("message", "Read-only property");
                                error.put("messageType", "error");
                                error.put("data", inner);

                                this.sendMessage(error.toString());
                            }
                        }
                        break;
                    case "requestAction":
                        JSONArray actionNames = messageData.names();
                        if (actionNames == null) {
                            break;
                        }

                        for (int i = 0; i < actionNames.length(); ++i) {
                            String actionName = actionNames.getString(i);
                            JSONObject params =
                                    messageData.getJSONObject(actionName);
                            JSONObject input = null;
                            if (params.has("input")) {
                                input = params.getJSONObject("input");
                            }

                            Action action =
                                    this.thing.performAction(actionName, input);
                            if (action != null) {
                                (new ActionRunner(action)).start();
                            } else {
                                JSONObject error = new JSONObject();
                                JSONObject inner = new JSONObject();

                                inner.put("status", "400 Bad Request");
                                inner.put("message", "Invalid action request");
                                error.put("messageType", "error");
                                error.put("data", inner);

                                this.sendMessage(error.toString());
                            }
                        }
                        break;
                    case "addEventSubscription":
                        JSONArray eventNames = messageData.names();
                        if (eventNames == null) {
                            break;
                        }

                        for (int i = 0; i < eventNames.length(); ++i) {
                            String eventName = eventNames.getString(i);
                            this.thing.addEventSubscriber(eventName, this);
                        }
                        break;
                    default:
                        JSONObject error = new JSONObject();
                        JSONObject inner = new JSONObject();

                        inner.put("status", "400 Bad Request");
                        inner.put("message",
                                  "Unknown messageType: " + messageType);
                        error.put("messageType", "error");
                        error.put("data", inner);

                        this.sendMessage(error.toString());
                        break;
                }
            }

            @Override
            protected void onPong(NanoWSD.WebSocketFrame pong) {
            }

            @Override
            protected void onException(IOException exception) {
            }

            public void sendMessage(String message) {
                try {
                    this.send(message);
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Handle a request to /properties.
     */
    public static class PropertiesHandler extends BaseHandler {
        /**
         * Handle a GET request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            // TODO: this is not yet defined in the spec
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    "");
        }
    }

    /**
     * Handle a request to /properties/&lt;property&gt;.
     */
    public static class PropertyHandler extends BaseHandler {
        /**
         * Get the property name from the URI.
         *
         * @param uriResource The URI resource that was matched
         * @param session     The HTTP session
         * @return The property name.
         */
        public String getPropertyName(UriResource uriResource,
                                      IHTTPSession session) {
            ThingsType things = uriResource.initParameter(0, ThingsType.class);

            if (MultipleThings.class.isInstance(things)) {
                return this.getUriParam(session.getUri(), 3);
            } else {
                return this.getUriParam(session.getUri(), 2);
            }
        }

        /**
         * Handle a GET request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            String propertyName = this.getPropertyName(uriResource, session);
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

        /**
         * Handle a PUT request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response put(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            String propertyName = this.getPropertyName(uriResource, session);
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
            } catch (IllegalArgumentException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.FORBIDDEN,
                                                        null,
                                                        null);
            }
        }
    }

    /**
     * Handle a request to /actions.
     */
    public static class ActionsHandler extends BaseHandler {
        /**
         * Handle a GET request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    thing.getActionDescriptions()
                                                         .toString());
        }

        /**
         * Handle a POST request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response post(UriResource uriResource,
                             Map<String, String> urlParams,
                             IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
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

            try {
                JSONObject response = new JSONObject();
                JSONArray actionNames = json.names();
                if (actionNames == null) {
                    return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST,
                                                            null,
                                                            null);
                }

                for (int i = 0; i < actionNames.length(); ++i) {
                    String actionName = actionNames.getString(i);
                    JSONObject params = json.getJSONObject(actionName);
                    JSONObject input = null;
                    if (params.has("input")) {
                        input = params.getJSONObject("input");
                    }

                    Action action = thing.performAction(actionName, input);
                    if (action != null) {
                        response.put(actionName,
                                     action.asActionDescription()
                                           .getJSONObject(actionName));

                        (new ActionRunner(action)).start();
                    }
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

    /**
     * Handle a request to /actions/&lt;action_name&gt;.
     */
    public static class ActionHandler extends BaseHandler {
        /**
         * Handle a GET request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            // TODO: this is not yet defined in the spec
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    "");
        }
    }

    /**
     * Handle a request to /actions/&lt;action_name&gt;/&lt;action_id&gt;.
     */
    public static class ActionIDHandler extends BaseHandler {
        /**
         * Get the action name from the URI.
         *
         * @param uriResource The URI resource that was matched
         * @param session     The HTTP session
         * @return The property name.
         */
        public String getActionName(UriResource uriResource,
                                    IHTTPSession session) {
            ThingsType things = uriResource.initParameter(0, ThingsType.class);

            if (MultipleThings.class.isInstance(things)) {
                return this.getUriParam(session.getUri(), 3);
            } else {
                return this.getUriParam(session.getUri(), 2);
            }
        }

        /**
         * Get the action ID from the URI.
         *
         * @param uriResource The URI resource that was matched
         * @param session     The HTTP session
         * @return The property name.
         */
        public String getActionId(UriResource uriResource,
                                  IHTTPSession session) {
            ThingsType things = uriResource.initParameter(0, ThingsType.class);

            if (MultipleThings.class.isInstance(things)) {
                return this.getUriParam(session.getUri(), 4);
            } else {
                return this.getUriParam(session.getUri(), 3);
            }
        }

        /**
         * Handle a GET request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            String actionName = this.getActionName(uriResource, session);
            String actionId = this.getActionId(uriResource, session);

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

        /**
         * Handle a PUT request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response put(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            // TODO: this is not yet defined in the spec
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    "");
        }

        /**
         * Handle a DELETE request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response delete(UriResource uriResource,
                               Map<String, String> urlParams,
                               IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            String actionName = this.getActionName(uriResource, session);
            String actionId = this.getActionId(uriResource, session);

            if (thing.removeAction(actionName, actionId)) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NO_CONTENT,
                                                        null,
                                                        null);
            } else {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }
        }
    }

    /**
     * Handle a request to /events.
     */
    public static class EventsHandler extends BaseHandler {
        /**
         * Handle a GET request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    thing.getEventDescriptions()
                                                         .toString());
        }
    }

    /**
     * Handle a request to /events/&lt;event_name&gt;.
     */
    public static class EventHandler extends BaseHandler {
        /**
         * Handle a GET request.
         *
         * @param uriResource The URI resource that was matched
         * @param urlParams   Map of URL parameters
         * @param session     The HTTP session
         * @return The appropriate response.
         */
        @Override
        public Response get(UriResource uriResource,
                            Map<String, String> urlParams,
                            IHTTPSession session) {
            Thing thing = this.getThing(uriResource, session);
            if (thing == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND,
                                                        null,
                                                        null);
            }

            // TODO: this is not yet defined in the spec
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    "");
        }
    }

    /**
     * A container for a single thing.
     */
    public static class SingleThing implements ThingsType {
        private Thing thing;

        /**
         * Initialize the container.
         *
         * @param thing The thing to store
         */
        public SingleThing(Thing thing) {
            this.thing = thing;
        }

        /**
         * Get the thing at the given index.
         *
         * @param idx The index.
         */
        public Thing getThing(int idx) {
            return this.thing;
        }

        /**
         * Get the list of things.
         *
         * @return The list of things.
         */
        public List<Thing> getThings() {
            List<Thing> things = new ArrayList<>();
            things.add(this.thing);
            return things;
        }

        /**
         * Get the mDNS server name.
         *
         * @return The server name.
         */
        public String getName() {
            return this.thing.getName();
        }
    }

    /**
     * A container for multiple things.
     */
    public static class MultipleThings implements ThingsType {
        private List<Thing> things;
        private String name;

        /**
         * Initialize the container.
         *
         * @param things The things to store
         * @param name   The mDNS server name
         */
        public MultipleThings(List<Thing> things, String name) {
            this.things = things;
            this.name = name;
        }

        /**
         * Get the thing at the given index.
         *
         * @param idx The index.
         */
        public Thing getThing(int idx) {
            if (idx < 0 || idx >= this.things.size()) {
                return null;
            }

            return this.things.get(idx);
        }

        /**
         * Get the list of things.
         *
         * @return The list of things.
         */
        public List<Thing> getThings() {
            return this.things;
        }

        /**
         * Get the mDNS server name.
         *
         * @return The server name.
         */
        public String getName() {
            return this.name;
        }
    }
}