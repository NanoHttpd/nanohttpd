package fi.iki.elonen.router;

/*
 * #%L
 * NanoHttpd-Samples
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * @author vnnv
 * @author ritchieGitHub
 */
public class RouterNanoHTTPD extends NanoHTTPD {

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(RouterNanoHTTPD.class.getName());

    public interface UriResponder {

        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);

        public Response put(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);

        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);

        public Response delete(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);

        public Response other(String method, UriResource uriResource, Map<String, String> urlParams, IHTTPSession session);
    }

    /**
     * General nanolet to inherit from if you provide stream data, only chucked
     * responses will be generated.
     */
    public static abstract class DefaultStreamHandler implements UriResponder {

        public abstract String getMimeType();

        public abstract IStatus getStatus();

        public abstract InputStream getData();

        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return NanoHTTPD.newChunkedResponse(getStatus(), getMimeType(), getData());
        }

        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        public Response put(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        public Response delete(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        public Response other(String method, UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }
    }

    /**
     * General nanolet to inherit from if you provide text or html data, only
     * fixed size responses will be generated.
     */
    public static abstract class DefaultHandler extends DefaultStreamHandler {

        public abstract String getText();

        public abstract IStatus getStatus();

        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), getText());
        }

        @Override
        public InputStream getData() {
            throw new IllegalStateException("this method should not be called in a text based nanolet");
        }
    }

    /**
     * General nanolet to print debug info's as a html page.
     */
    public static class GeneralHandler extends DefaultHandler {

        @Override
        public String getText() {
            throw new IllegalStateException("this method should not be called");
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public IStatus getStatus() {
            return Status.OK;
        }

        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            StringBuilder text = new StringBuilder("<html><body>");
            text.append("<h1>Url: ");
            text.append(session.getUri());
            text.append("</h1><br>");
            Map<String, String> queryParams = session.getParms();
            if (queryParams.size() > 0) {
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    text.append("<p>Param '");
                    text.append(key);
                    text.append("' = ");
                    text.append(value);
                    text.append("</p>");
                }
            } else {
                text.append("<p>no params in url</p><br>");
            }
            return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), text.toString());
        }
    }

    /**
     * General nanolet to print debug info's as a html page.
     */
    public static class StaticPageHandler extends DefaultHandler {

        private static String[] getPathArray(String uri) {
            String array[] = uri.split("/");
            ArrayList<String> pathArray = new ArrayList<String>();

            for (String s : array) {
                if (s.length() > 0)
                    pathArray.add(s);
            }

            return pathArray.toArray(new String[]{});

        }

        @Override
        public String getText() {
            throw new IllegalStateException("this method should not be called");
        }

        @Override
        public String getMimeType() {
            throw new IllegalStateException("this method should not be called");
        }

        @Override
        public IStatus getStatus() {
            return Status.OK;
        }

        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            String baseUri = uriResource.getUri();
            String realUri = normalizeUri(session.getUri());
            for (int index = 0; index < Math.min(baseUri.length(), realUri.length()); index++) {
                if (baseUri.charAt(index) != realUri.charAt(index)) {
                    realUri = normalizeUri(realUri.substring(index));
                    break;
                }
            }
            File fileOrdirectory = uriResource.initParameter(File.class);
            for (String pathPart : getPathArray(realUri)) {
                fileOrdirectory = new File(fileOrdirectory, pathPart);
            }
            if (fileOrdirectory.isDirectory()) {
                fileOrdirectory = new File(fileOrdirectory, "index.html");
                if (!fileOrdirectory.exists()) {
                    fileOrdirectory = new File(fileOrdirectory.getParentFile(), "index.htm");
                }
            }
            if (!fileOrdirectory.exists() || !fileOrdirectory.isFile()) {
                return new Error404UriHandler().get(uriResource, urlParams, session);
            } else {
                try {
                    return NanoHTTPD.newChunkedResponse(getStatus(), getMimeTypeForFile(fileOrdirectory.getName()), fileToInputStream(fileOrdirectory));
                } catch (IOException ioe) {
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.REQUEST_TIMEOUT, "text/plain", null);
                }
            }
        }

        protected BufferedInputStream fileToInputStream(File fileOrdirectory) throws IOException {
            return new BufferedInputStream(new FileInputStream(fileOrdirectory));
        }
    }

    /**
     * Handling error 404 - unrecognized urls
     */
    public static class Error404UriHandler extends DefaultHandler {

        public String getText() {
            return "<html><body><h3>Error 404: the requested page doesn't exist.</h3></body></html>";
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public IStatus getStatus() {
            return Status.NOT_FOUND;
        }
    }

    /**
     * Handling index
     */
    public static class IndexHandler extends DefaultHandler {

        public String getText() {
            return "<html><body><h2>Hello world!</h3></body></html>";
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public IStatus getStatus() {
            return Status.OK;
        }

    }

    public static class NotImplementedHandler extends DefaultHandler {

        public String getText() {
            return "<html><body><h2>The uri is mapped in the router, but no handler is specified. <br> Status: Not implemented!</h3></body></html>";
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public IStatus getStatus() {
            return Status.OK;
        }
    }

    public static String normalizeUri(String value) {
        if (value == null) {
            return value;
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;

    }

    public static class UriResource {

        private static final Pattern PARAM_PATTERN = Pattern.compile("(?<=(^|/)):[a-zA-Z0-9_-]+(?=(/|$))");

        private static final String PARAM_MATCHER = "([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=]+)";

        private static final Map<String, String> EMPTY = Collections.unmodifiableMap(new HashMap<String, String>());

        private final String uri;

        private final Pattern uriPattern;

        private final int priority;

        private final Class<?> handler;

        private final Object[] initParameter;

        private List<String> uriParams = new ArrayList<String>();

        public UriResource(String uri, int priority, Class<?> handler, Object... initParameter) {
            this.handler = handler;
            this.initParameter = initParameter;
            if (uri != null) {
                this.uri = normalizeUri(uri);
                parse();
                this.uriPattern = createUriPattern();
            } else {
                this.uriPattern = null;
                this.uri = null;
            }
            this.priority = priority + uriParams.size() * 1000;
        }

        private void parse() {
        }

        private Pattern createUriPattern() {
            String patternUri = uri;
            Matcher matcher = PARAM_PATTERN.matcher(patternUri);
            int start = 0;
            while (matcher.find(start)) {
                uriParams.add(patternUri.substring(matcher.start() + 1, matcher.end()));
                patternUri = new StringBuilder(patternUri.substring(0, matcher.start()))//
                        .append(PARAM_MATCHER)//
                        .append(patternUri.substring(matcher.end())).toString();
                start = matcher.start() + PARAM_MATCHER.length();
                matcher = PARAM_PATTERN.matcher(patternUri);
            }
            return Pattern.compile(patternUri);
        }

        public Response process(Map<String, String> urlParams, IHTTPSession session) {
            String error = "General error!";
            if (handler != null) {
                try {
                    Object object = handler.newInstance();
                    if (object instanceof UriResponder) {
                        UriResponder responder = (UriResponder) object;
                        switch (session.getMethod()) {
                            case GET:
                                return responder.get(this, urlParams, session);
                            case POST:
                                return responder.post(this, urlParams, session);
                            case PUT:
                                return responder.put(this, urlParams, session);
                            case DELETE:
                                return responder.delete(this, urlParams, session);
                            default:
                                return responder.other(session.getMethod().toString(), this, urlParams, session);
                        }
                    } else {
                        return NanoHTTPD.newFixedLengthResponse(Status.OK, "text/plain", //
                                new StringBuilder("Return: ")//
                                        .append(handler.getCanonicalName())//
                                        .append(".toString() -> ")//
                                        .append(object)//
                                        .toString());
                    }
                } catch (Exception e) {
                    error = "Error: " + e.getClass().getName() + " : " + e.getMessage();
                    LOG.log(Level.SEVERE, error, e);
                }
            }
            return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", error);
        }

        @Override
        public String toString() {
            return new StringBuilder("UrlResource{uri='").append((uri == null ? "/" : uri))//
                    .append("', urlParts=").append(uriParams)//
                    .append('}')//
                    .toString();
        }

        public String getUri() {
            return uri;
        }

        public <T> T initParameter(Class<T> paramClazz) {
            return initParameter(0, paramClazz);
        }

        public <T> T initParameter(int parameterIndex, Class<T> paramClazz) {
            if (initParameter.length > parameterIndex) {
                return paramClazz.cast(initParameter[parameterIndex]);
            }
            LOG.severe("init parameter index not available " + parameterIndex);
            return null;
        }

        public Map<String, String> match(String url) {
            Matcher matcher = uriPattern.matcher(url);
            if (matcher.matches()) {
                if (uriParams.size() > 0) {
                    Map<String, String> result = new HashMap<String, String>();
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        result.put(uriParams.get(i - 1), matcher.group(i));
                    }
                    return result;
                } else {
                    return EMPTY;
                }
            }
            return null;
        }

    }

    public static class UriRouter {

        private List<UriResource> mappings;

        private UriResource error404Url;

        private Class<?> notImplemented;

        public UriRouter() {
            mappings = new ArrayList<UriResource>();
        }

        /**
         * Search in the mappings if the given url matches some of the rules If
         * there are more than one marches returns the rule with less parameters
         * e.g. mapping 1 = /user/:id mapping 2 = /user/help if the incoming uri
         * is www.example.com/user/help - mapping 2 is returned if the incoming
         * uri is www.example.com/user/3232 - mapping 1 is returned
         * 
         * @param url
         * @return
         */
        public Response process(IHTTPSession session) {
            String work = normalizeUri(session.getUri());
            Map<String, String> params = null;
            UriResource uriResource = error404Url;
            for (UriResource u : mappings) {
                params = u.match(work);
                if (params != null) {
                    uriResource = u;
                    break;
                }
            }
            return uriResource.process(params, session);
        }

        private void addRoute(String url, int priority, Class<?> handler, Object... initParameter) {
            if (url != null) {
                if (handler != null) {
                    mappings.add(new UriResource(url, priority + mappings.size(), handler, initParameter));
                } else {
                    mappings.add(new UriResource(url, priority + mappings.size(), notImplemented));
                }
                sortMappings();
            }
        }

        private void sortMappings() {
            Collections.sort(mappings, new Comparator<UriResource>() {

                @Override
                public int compare(UriResource o1, UriResource o2) {
                    return o1.priority - o2.priority;
                }
            });
        }

        private void removeRoute(String url) {
            String uriToDelete = normalizeUri(url);
            Iterator<UriResource> iter = mappings.iterator();
            while (iter.hasNext()) {
                UriResource uriResource = iter.next();
                if (uriToDelete.equals(uriResource.getUri())) {
                    iter.remove();
                    break;
                }
            }
        }

        public void setNotFoundHandler(Class<?> handler) {
            error404Url = new UriResource(null, 100, handler);
        }

        public void setNotImplemented(Class<?> handler) {
            notImplemented = handler;
        }

    }

    private UriRouter router;

    public RouterNanoHTTPD(int port) {
        super(port);
        router = new UriRouter();
    }

    /**
     * default routings, they are over writable.
     * 
     * <pre>
     * router.setNotFoundHandler(GeneralHandler.class);
     * </pre>
     */

    public void addMappings() {
        router.setNotImplemented(NotImplementedHandler.class);
        router.setNotFoundHandler(Error404UriHandler.class);
        router.addRoute("/", Integer.MAX_VALUE / 2, IndexHandler.class);
        router.addRoute("/index.html", Integer.MAX_VALUE / 2, IndexHandler.class);
    }

    public void addRoute(String url, Class<?> handler, Object... initParameter) {
        router.addRoute(url, 100, handler, initParameter);
    }

    public void removeRoute(String url) {
        router.removeRoute(url);
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Try to find match
        return router.process(session);
    }
}
