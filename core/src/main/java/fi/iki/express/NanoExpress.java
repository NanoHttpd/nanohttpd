package fi.iki.express;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * This is a generic express modeled server for use on the standard platform.
 * Created by James on 6/8/2015.
 */
public abstract class NanoExpress extends NanoHTTPD {

    public static boolean outputStackTrace = true;
    private final HashMap<String, Router> routerArray;
    private final ArrayList<String> route_priority;

    /**
     * Constructs an HTTP server on given port.
     *
     * @param port
     */
    public NanoExpress(int port) {
        super(port);
        this.routerArray = new HashMap<String, Router>();
        this.route_priority = new ArrayList<String>();
        try {
            outputStackTrace = Boolean.parseBoolean(System.getProperty("outputStackTrace"));
        } catch (Exception e) {
        }
    }

    public NanoExpress(int port, ArrayList<String> routePriority, Map<String, Router> routerArray) {
        super(port);
        if (routePriority != null) {
            this.route_priority = routePriority;
        } else {
            this.route_priority = new ArrayList<String>();
        }
        if (routerArray != null) {
            this.routerArray = new HashMap<String, Router>(routerArray);
        } else {
            this.routerArray = new HashMap<String, Router>();
        }
        try {
            outputStackTrace = Boolean.parseBoolean(System.getProperty("outputStackTrace"));
        } catch (Exception e) {
        }
    }

    /**
     * Stop the server.
     */
    @Override
    public void stop() {
        super.stop();
        for (Router r : routerArray.values()) {
            r.tearDown();
        }
    }

    public final void addMappings(Router route) {
        if (route != null) {
            addMappings(route.getDefaultURIPath(), route);
        }
    }

    public final int getRouterCount() {
        return this.routerArray.size();
    }

    /**
     * The small priority indicate the route will be run and checked first.
     * @param routePath
     * @return Priority of the route
     * @throws RouteNotFoundException
     */
    public final int getRoutePriority(String routePath) throws RouteNotFoundException {
        if (this.routerArray.get(routePath) == null) {
            throw new RouteNotFoundException(routePath);
        }
        return this.route_priority.indexOf(routePath);
    }

    public final synchronized void addMappings(String path, Router route) {
        //If a URI Path is already specified, it will be over written.
        //It will rejoin the queue at the end.
        if (path != null && route != null) {
            if (this.route_priority.contains(path)) {
                this.route_priority.remove(path);
            }
            this.route_priority.add(path);
            this.routerArray.put(path, route);
            route.setup();
        }
    }

    /**
     * User Define the mechanism of how he/she wants to load the mapping.
     * Perhaps from the properties files etc, config files.
     *
     * Use AddMapping to add them into NanoExpress.
     */
    public abstract void loadMappings();
    /**
     * 1) Read Config
     * 2) Iterate and add Mappings
     *
     * NB: It is
     */

    /**
     * The server will serves content based on the added routes.
     * The server will serve the first route that matches and return a non null response.
     * <p/>
     * User can either programmatically add the mapping using one of the constructor or define the @loadMappings method to
     * instruct the server how to load that mapping.
     * <p/>
     * Implementor of Router Interface will have full control of what will be served out from NanoHTTPD.
     * <p/>
     * <p/>
     * (By default, this returns a 404 "Not Found" plain text error response.)
     *
     * @param session The HTTP session
     * @return HTTP response, see class Response for details
     */
    @Override
    public NanoHTTPD.Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        NanoHTTPD.Response res = null;
        try {
            for (String uri : route_priority) {
                Router r = routerArray.get(uri);
                //This will follow the methods defined in NanoHTTPD.Method
                switch (method) {
                    case GET:
                        res = r.doGet(session);
                        break;
                    case PUT:
                        res = r.doPut(session);
                        break;
                    case POST:
                        res = r.doPost(session);
                        break;
                    case DELETE:
                        res = r.doDelete(session);
                        break;
                    case HEAD:
                        res = r.doHead(session);
                        break;
                    case OPTIONS:
                        res = r.doOptions(session);
                        break;
                    case TRACE:
                        res = r.doTrace(session);
                        break;
                    case CONNECT:
                        res = r.doConnect(session);
                        break;
                    case PATCH:
                        res = r.doPatch(session);
                        break;
                    default:
                        res = new IStatusResponse(Response.Status.METHOD_NOT_ALLOWED);
                }
                if (res != null) return res;
            }
        } catch (Exception e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            if (outputStackTrace) {
                return new IStatusResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, new ByteArrayInputStream(baos.toByteArray()), baos.toByteArray().length);
            } else {
                return new IStatusResponse(Response.Status.INTERNAL_ERROR);
            }
        }
        return new IStatusResponse(Response.Status.NOT_FOUND);
    }


    public static class IStatusResponse extends NanoHTTPD.Response {
        private IStatusResponse(IStatus status, String mimeType, InputStream data, long totalBytes) {
            super(status, mimeType, data, totalBytes);
        }

        public IStatusResponse(NanoHTTPD.Response.Status status) {
            this(status, NanoHTTPD.MIME_PLAINTEXT, new ByteArrayInputStream(status.getDescription().getBytes()), (long) status.getDescription().getBytes().length);
        }
    }

    /**
     * This is to facilitate the creation of the response object.
     */
    public static class Response extends NanoHTTPD.Response {
        public Response(IStatus status, String mimeType, InputStream data, long totalBytes) {
            super(status, mimeType, data, totalBytes);
        }
    }

    static class RouteNotFoundException extends Exception {
        public RouteNotFoundException() {
            super("The specified route is not registered.");
        }

        public RouteNotFoundException(String message) {
            super("The " + message + " route is not registered.");
        }
    }
}
