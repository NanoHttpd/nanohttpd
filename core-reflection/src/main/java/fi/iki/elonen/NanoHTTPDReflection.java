package fi.iki.elonen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * An extension to NanoHTTPD that allows for binding a method to a URI through reflection.
 * <p/>
 * <p/>
 * NanoHTTPDReflection
 * <p></p>Copyright (c) 2013 by Martin M Reed</p>
 * <p/>
 * <p/>
 * <b>How to use: </b>
 * <ul>
 * <p/>
 * <li>Use @Path("/some/uri") to assign your method to a URI using reflection</li>
 * <p/>
 * </ul>
 */
public abstract class NanoHTTPDReflection extends NanoHTTPD {
    protected NanoHTTPDReflection(int port) {
        super(port);
    }

    protected NanoHTTPDReflection(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public final Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {
        java.lang.reflect.Method _method = findMethod(uri, String.class, Method.class, Map.class, Map.class, Map.class);

        if (_method == null) {
            return notFound(uri, method, headers, parms);
        }

        try {
            return (Response) _method.invoke(this, uri, method, headers, parms, files);
        } catch (Exception e) {
            return error(uri, method, headers, parms, e);
        }
    }

    @Override
    protected final Response serve(HTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, String> parms = session.getParms();
        Map<String, String> headers = session.getHeaders();

        java.lang.reflect.Method _method = findMethod(uri);
        if (_method == null) {
            return notFound(uri, method, headers, parms);
        }

        AuthorizationRequired authorizationRequired = _method.getAnnotation(AuthorizationRequired.class);
        if (authorizationRequired != null && !authorized(session)) {
            return forbidden(uri, method, headers, parms);
        }

        Response expectParamResponse = expectParam(_method, parms);
        if (expectParamResponse != null) {
            return expectParamResponse;
        }

        Response expectHeaderResponse = expectHeader(_method, headers);
        if (expectHeaderResponse != null) {
            return expectHeaderResponse;
        }

        _method = findMethod(uri, HTTPSession.class);

        if (_method == null) {
            return super.serve(session);
        }

        try {
            return (Response) _method.invoke(this, session);
        } catch (Exception e) {
            return error(uri, method, headers, parms, e);
        }
    }

    protected Response error(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Exception exception) {
        String stacktrace = exception.toString();
        //String stacktrace = android.util.Log.getStackTraceString(exception);
        //stacktrace = "<pre>" + stacktrace + "</pre>";

        Response response = new Response(stacktrace);
        response.setStatus(Status.INTERNAL_ERROR);
        return response;
    }

    protected Response notFound(String uri, Method method, Map<String, String> headers, Map<String, String> parms) {
        Response response = new Response(null);
        response.setStatus(Status.NOT_FOUND);
        return response;
    }

    protected Response forbidden(String uri, Method method, Map<String, String> headers, Map<String, String> parms) {
        Response response = new Response(null);
        response.setStatus(Status.FORBIDDEN);
        return response;
    }

    /**
     * Verify that the session has the correct authorization
     * 
     * @param session
     * @return true if authorized, false if not authorized
     */
    protected boolean authorized(HTTPSession session) {
        return true;
    }

    /**
     * Find a method with the matching URI and method parameter types
     * 
     * @param uri
     * @param parameterTypes
     * @return JAVA method used with reflection
     */
    private java.lang.reflect.Method findMethod(String uri, Class<?>... parameterTypes) {
        boolean anyMatchingUri = parameterTypes == null || parameterTypes.length == 0;

        for (java.lang.reflect.Method method : getClass().getMethods()) {
            Path path = method.getAnnotation(Path.class);
            if (path != null && uri.equals(path.value())) {
                Class<?>[] _parameterTypes = method.getParameterTypes();
                if (anyMatchingUri || Arrays.equals(_parameterTypes, parameterTypes)) {
                    return method;
                }
            }
        }

        return null;
    }

    /**
     * Check that the given method has the expected parameters available
     * 
     * @param method
     * @param parms
     * @return HTTP 400 Bad Request if there is a parameter missing, or null otherwise
     */
    private Response expectParam(java.lang.reflect.Method method, Map<String, String> parms) {
        ExpectParam expectParam = method.getAnnotation(ExpectParam.class);

        if (expectParam != null) {
            for (String param : expectParam.value()) {
                if (!parms.containsKey(param)) {
                    Response response = new Response("Expected parameter: " + param);
                    response.setStatus(Status.BAD_REQUEST);
                    return response;
                }
            }
        }

        return null;
    }

    /**
     * Check that the given method has the expected headers available
     * 
     * @param method
     * @param headers
     * @return HTTP 400 Bad Request if there is a header missing, or null otherwise
     */
    private Response expectHeader(java.lang.reflect.Method method, Map<String, String> headers) {
        ExpectHeader expectHeader = method.getAnnotation(ExpectHeader.class);

        if (expectHeader != null) {
            for (String header : expectHeader.value()) {
                if (!headers.containsKey(header.toLowerCase())) {
                    Response response = new Response("Expected header: " + header);
                    response.setStatus(Status.BAD_REQUEST);
                    return response;
                }
            }
        }

        return null;
    }

    /**
     * Path matching the expected URI
     */
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Path {
        String value();
    }

    /**
     * Trigger a call to the authorized(HTTPSession) method
     */
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface AuthorizationRequired {
        // do nothing
    }

    /**
     * Assert that a given header is expected
     */
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExpectHeader {
        String[] value();
    }

    /**
     * Assert that a given parameter is expected
     */
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExpectParam {
        String[] value();
    }
}