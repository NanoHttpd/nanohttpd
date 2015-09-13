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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static class UriPart {

        private String name;

        private boolean isParam;

        public UriPart(String name, boolean isParam) {
            this.name = name;
            this.isParam = isParam;
        }

        @Override
        public String toString() {
            return new StringBuilder("UriPart{name='").append(name)//
                    .append("\', isParam=").append(isParam)//
                    .append('}').toString();
        }

        public boolean isParam() {
            return isParam;
        }

        public String getName() {
            return name;
        }

    }

    public static class UriResource {

        private boolean hasParameters;

        private int uriParamsCount;

        private String uri;

        private List<UriPart> uriParts;

        private Class<?> handler;

        public UriResource(String uri, Class<?> handler) {
            this.hasParameters = false;
            this.handler = handler;
            uriParamsCount = 0;
            if (uri != null) {
                this.uri = normalizeUri(uri);
                parse();
            }
        }

        private void parse() {
            String[] parts = uri.split("/");
            uriParts = new ArrayList<UriPart>();
            for (String part : parts) {
                boolean isParam = part.startsWith(":");
                UriPart uriPart = null;
                if (isParam) {
                    hasParameters = true;
                    uriParamsCount++;
                    uriPart = new UriPart(part.substring(1), true);
                } else {
                    uriPart = new UriPart(part, false);
                }
                uriParts.add(uriPart);
            }

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
            return new StringBuilder("UrlResource{hasParameters=").append(hasParameters)//
                    .append(", uriParamsCount=").append(uriParamsCount)//
                    .append(", uri='").append((uri == null ? "/" : uri))//
                    .append("', urlParts=").append(uriParts)//
                    .append('}')//
                    .toString();
        }

        public boolean hasParameters() {
            return hasParameters;
        }

        public String getUri() {
            return uri;
        }

        public List<UriPart> getUriParts() {
            return uriParts;
        }

        public int getUriParamsCount() {
            return uriParamsCount;
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
        public UriResource matchUrl(String url) {
            String work = normalizeUri(url);
            String[] parts = work.split("/");
            List<UriResource> resultList = new ArrayList<UriResource>();
            for (UriResource u : mappings) {
                // Check if count of parts is the same
                if (parts.length != u.getUriParts().size()) {
                    continue; // different
                }
                List<UriPart> uriParts = u.getUriParts();
                boolean match = true;
                for (int i = 0; i < parts.length; i++) {
                    String currentPart = parts[i];
                    UriPart uriPart = uriParts.get(i);
                    boolean goOn = false;
                    if (currentPart.equals(uriPart.getName())) {
                        // exact match
                        goOn = true;
                    } else {
                        // not match
                        if (uriPart.isParam()) {
                            goOn = true;
                        } else {
                            match = false;
                            goOn = false;
                        }
                    }
                    if (!goOn) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    // current match
                    resultList.add(u);
                }
            }
            if (!resultList.isEmpty()) {
                // some results
                UriResource result = null;
                if (resultList.size() > 1) {
                    // return the rule with less parameters
                    int params = 1024;
                    for (UriResource u : resultList) {
                        if (!u.hasParameters()) {
                            result = u;
                            break;
                        } else {
                            if (u.getUriParamsCount() <= params) {
                                result = u;
                            }
                        }
                    }
                    return result;
                } else {
                    return resultList.get(0);
                }
            }
            return error404Url;
        }

        public void addRoute(String url, Class<?> handler) {
            if (url != null) {
                if (handler != null) {
                    mappings.add(new UriResource(url, handler));
                } else {
                    mappings.add(new UriResource(url, notImplemented));
                }
            }
        }

        public void removeRoute(String url) {
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
            error404Url = new UriResource(null, handler);
        }

        public void setNotImplemented(Class<?> handler) {
            notImplemented = handler;
        }

        /**
         * Extract parameters and their values for the given route
         * 
         * @param route
         * @param uri
         * @return
         */
        public Map<String, String> getUrlParams(UriResource route, String uri) {
            Map<String, String> result = new HashMap<String, String>();
            if (route.getUri() == null) {
                return result;
            }
            String workUri = normalizeUri(uri);
            String[] parts = workUri.split("/");
            for (int i = 0; i < parts.length; i++) {
                UriPart currentPart = route.getUriParts().get(i);
                if (currentPart.isParam()) {
                    result.put(currentPart.getName(), parts[i]);
                }
            }
            return result;
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
        router.addRoute("/", IndexHandler.class);
        router.addRoute("/index.html", IndexHandler.class);
    }

    public void addRoute(String url, Class<?> handler) {
        router.addRoute(url, handler);
    }

    public void removeRoute(String url) {
        router.removeRoute(url);
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Try to find match
        UriResource uriResource = router.matchUrl(session.getUri());
        // Process the uri
        return uriResource.process(router.getUrlParams(uriResource, session.getUri()), session);
    }
}
