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
    private Thing thing;
    private boolean isTls;
    private JmDNS jmdns;

    /**
     * Initialize the WebThingServer on port 80.
     *
     * @param thing The Thing managed by this server
     * @throws IOException
     */
    public WebThingServer(Thing thing) throws IOException {
        this(thing, 80, null);
    }

    /**
     * Initialize the WebThingServer.
     *
     * @param thing The Thing managed by this server
     * @param port  Port to listen on
     * @throws IOException
     */
    public WebThingServer(Thing thing, Integer port) throws IOException {
        this(thing, port, null);
    }

    /**
     * Initialize the WebThingServer.
     *
     * @param thing      The Thing managed by this server
     * @param port       Port to listen on
     * @param sslOptions SSL options to pass to the NanoHTTPD server
     * @throws IOException
     */
    public WebThingServer(Thing thing, Integer port, SSLOptions sslOptions)
            throws IOException {
        super(port);
        this.port = port;
        this.thing = thing;
        this.ip = Utils.getIP();
        this.isTls = sslOptions != null;

        if (this.isTls) {
            super.makeSecure(sslOptions.getSocketFactory(),
                             sslOptions.getProtocols());
        }

        this.setRoutePrioritizer(new InsertionOrderRoutePrioritizer());

        // These are matched in the order they are added.
        addRoute("/properties/:propertyName",
                 PropertyHandler.class,
                 this.thing);
        addRoute("/properties", PropertiesHandler.class, this.thing);
        addRoute("/actions/:actionName/:actionId",
                 ActionIDHandler.class,
                 this.thing);
        addRoute("/actions/:actionName", ActionHandler.class, this.thing);
        addRoute("/actions", ActionsHandler.class, this.thing);
        addRoute("/events/:eventName", EventHandler.class, this.thing);
        addRoute("/events", EventsHandler.class, this.thing);
        addRoute("/",
                 ThingHandler.class,
                 this.thing,
                 this.ip,
                 this.port,
                 this.isTls);
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
                                                     this.thing.getName(),
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
         * @throws IOException
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
            Thing thing = uriResource.initParameter(0, Thing.class);
            String ip = uriResource.initParameter(1, String.class);
            Integer port = uriResource.initParameter(2, Integer.class);
            Boolean isTls = uriResource.initParameter(3, Boolean.class);

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

                    this.sendMessage(inner.toString());

                    return;
                }

                String messageType = json.getString("messageType");
                JSONObject messageData = json.getJSONObject("data");
                switch (messageType) {
                    case "setProperty":
                        JSONArray propertyNames = messageData.names();
                        for (int i = 0; i < propertyNames.length(); ++i) {
                            String propertyName = propertyNames.getString(i);
                            this.thing.setProperty(propertyName,
                                                   messageData.get(propertyName));
                        }
                        break;
                    case "requestAction":
                        JSONArray actionNames = messageData.names();
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

                            (new ActionRunner(action)).start();
                        }
                        break;
                    case "addEventSubscription":
                        JSONArray eventNames = messageData.names();
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

                        this.sendMessage(inner.toString());
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
            Thing thing = uriResource.initParameter(0, Thing.class);
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
                    String actionName = actionNames.getString(i);
                    JSONObject params = json.getJSONObject(actionName);
                    JSONObject input = null;
                    if (params.has("input")) {
                        input = params.getJSONObject("input");
                    }

                    Action action = thing.performAction(actionName, input);
                    response.put(actionName,
                                 action.asActionDescription()
                                       .getJSONObject(actionName));

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
            Thing thing = uriResource.initParameter(0, Thing.class);
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
            // TODO: this is not yet defined in the spec
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK,
                                                    "application/json",
                                                    "");

        }
    }
}