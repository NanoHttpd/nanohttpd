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

import fi.iki.elonen.NanoHTTPD;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vnnv on 7/21/15.
 */
public class RouterNanoHTTPD extends NanoHTTPD {

    public interface UriResponder {

        public NanoHTTPD.Response get(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session);

        public NanoHTTPD.Response put(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session);

        public NanoHTTPD.Response post(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session);

        public NanoHTTPD.Response delete(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session);

        public NanoHTTPD.Response other(String method, UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session);
    } // end interface UriResponder

    public static abstract class DefaultHandler implements UriResponder {

        public abstract String getText();

        public abstract String getMimeType();

        public abstract NanoHTTPD.Response.IStatus getStatus();

        public NanoHTTPD.Response get(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            String text = getText();
            ByteArrayInputStream inp = new ByteArrayInputStream(text.getBytes());
            int size = text.getBytes().length;
            return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), inp, size);
        }

        public NanoHTTPD.Response post(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        public NanoHTTPD.Response put(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        public NanoHTTPD.Response delete(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }

        public NanoHTTPD.Response other(String method, UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            return get(uriResource, urlParams, session);
        }
    } // end Default Handler

    public static class GeneralHandler extends DefaultHandler {

        @Override
        public String getText() {
            return null;
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public NanoHTTPD.Response.IStatus getStatus() {
            return NanoHTTPD.Response.Status.OK;
        }

        public NanoHTTPD.Response get(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            String text = "<html><body>";
            text += "<h1>Url: " + session.getUri() + "</h1><br>";
            Map<String, String> queryParams = session.getParms();
            if (queryParams.size() > 0) {
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    text += "<p>Param '" + key + "' = " + value + "</p>";
                }
            } else {
                text += "<p>no params in url</p><br>";
            }

            InputStream inp = new ByteArrayInputStream(text.getBytes());
            int length = text.getBytes().length;

            NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), inp, length);
            return res;
        }
    } // end General Handler

    /**
     * Handling error 404 - unrecognized urls
     */
    public static class Error404UriHandler extends DefaultHandler {

        public String getText() {
            String res = "<html><body><h3>Error 404: " + "the requested page doesn't exist.</h3></body></html>";
            return res;
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public Response.IStatus getStatus() {
            return Response.Status.NOT_FOUND;
        }
    } // End Error404UriHandler

    /**
     * Handling index
     */
    public static class IndexHandler extends DefaultHandler {

        public String getText() {
            String res = "<html><body><h2>Hello world!</h3></body></html>";
            return res;
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public Response.IStatus getStatus() {
            return Response.Status.OK;
        }

    } // End IndexHanfler

    public static class NotImplementedHandler extends DefaultHandler {

        public String getText() {
            String res = "<html><body><h2>The uri is mapped in the router, " + "but no handler is specified. <br> " + "Status: Not implemented!</h3></body></html>";
            return res;
        }

        @Override
        public String getMimeType() {
            return "text/html";
        }

        @Override
        public Response.IStatus getStatus() {
            return Response.Status.OK;
        }
    } // End NotImplementedHandler

    public static class UriUtils {

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
            return "UriPart{" + "name='" + name + '\'' + ", isParam=" + isParam + '}';
        }

        public boolean isParam() {
            return isParam;
        }

        public String getName() {
            return name;
        }

    } // End UriPart

    public static class UriResource {

        private boolean hasParameters;

        private int uriParamsCount;

        private String uri;

        private List<UriPart> uriParts;

        private Class handler;

        public UriResource(String uri, Class<?> handler) {
            this.hasParameters = false;
            this.handler = handler;
            uriParamsCount = 0;
            if (uri != null) {
                this.uri = UriUtils.normalizeUri(uri);
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

        public NanoHTTPD.Response process(Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
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
                        // return toString()
                        String text = "Return: " + handler.getCanonicalName() + ".toString() -> " + object.toString();
                        NanoHTTPD.Response res =
                                NanoHTTPD
                                        .newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", new ByteArrayInputStream(text.getBytes()), text.getBytes().length);
                        return res;
                    }
                } catch (InstantiationException e) {
                    error = "Error: " + InstantiationException.class.getName() + " : " + e.getMessage();
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    error = "Error: " + IllegalAccessException.class.getName() + " : " + e.getMessage();
                    e.printStackTrace();
                }
            }

            NanoHTTPD.Response res =
                    NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", new ByteArrayInputStream(error.getBytes()),
                            error.getBytes().length);

            return res;
        }

        @Override
        public String toString() {
            return "UrlResource{" + "hasParameters=" + hasParameters + ", uriParamsCount=" + uriParamsCount + ", uri='" + (uri != null ? "/" : "") + uri + '\''
                    + ", urlParts=" + uriParts + '}';
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

    } // End UriResource

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

            String work = UriUtils.normalizeUri(url);

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
                } // for - iterate incoming url parts
                if (match) {
                    resultList.add(u); // current match
                }
            } // end iterate over all rules
            if (resultList.size() > 0) {
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
            if (mappings.contains(url)) {
                mappings.remove(url);
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

            String workUri = UriUtils.normalizeUri(uri);
            String[] parts = workUri.split("/");

            for (int i = 0; i < parts.length; i++) {
                UriPart currentPart = route.getUriParts().get(i);
                if (currentPart.isParam()) {
                    result.put(currentPart.getName(), parts[i]);
                }
            }
            return result;
        }
    } // End UriRouter

    private UriRouter router;

    public RouterNanoHTTPD(int port) {
        super(port);
        router = new UriRouter();
    }

    public void addMappings() {
        router.setNotImplemented(NotImplementedHandler.class);
        router.setNotFoundHandler(Error404UriHandler.class);
        // router.setNotFoundHandler(GeneralHandler.class); // You can use this
        // instead of Error404UriHandler
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
        // Extract uri parameters
        Map<String, String> urlParams = router.getUrlParams(uriResource, session.getUri());
        // Process the uri
        return uriResource.process(urlParams, session);
    }
}
