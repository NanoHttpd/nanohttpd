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

/**
 * Created by vnnv on 7/17/15.
 * Simple httpd server based on NanoHTTPD
 * Read the source. Everything is there.
 */

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.util.ServerRunner;

public class AppNanolets extends RouterNanoHTTPD {

    private static final int PORT = 9090;

    public static class UserHandler extends DefaultHandler {

        @Override
        public String getText() {
            return "not implemented";
        }

        public String getText(Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
            String text = "<html><body>User handler. Method: " + session.getMethod().toString() + "<br>";
            text += "<h1>Uri parameters:</h1>";
            for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                text += "<div> Param: " + key + "&nbsp;Value: " + value + "</div>";
            }
            text += "<h1>Query parameters:</h1>";
            for (Map.Entry<String, String> entry : session.getParms().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                text += "<div> Query Param: " + key + "&nbsp;Value: " + value + "</div>";
            }
            text += "</body></html>";

            return text;
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
            String text = getText(urlParams, session);
            ByteArrayInputStream inp = new ByteArrayInputStream(text.getBytes());
            int size = text.getBytes().length;
            return NanoHTTPD.newFixedLengthResponse(getStatus(), getMimeType(), inp, size);
        }

    }

    static public class StreamUrl extends DefaultStreamHandler {

        @Override
        public String getMimeType() {
            return "text/plain";
        }

        @Override
        public IStatus getStatus() {
            return Status.OK;
        }

        @Override
        public InputStream getData() {
            return new ByteArrayInputStream("a stream of data ;-)".getBytes());
        }

    }

    static class StaticPageTestHandler extends StaticPageHandler {

        @Override
        protected BufferedInputStream fileToInputStream(File fileOrdirectory) throws IOException {
            if (fileOrdirectory.getName().equals("exception.html")) {
                throw new IOException("trigger something wrong");
            }
            return super.fileToInputStream(fileOrdirectory);
        }
    }

    /**
     * Create the server instance
     */
    public AppNanolets() throws IOException {
        super(PORT);
        addMappings();
        System.out.println("\nRunning! Point your browers to http://localhost:" + PORT + "/ \n");
    }

    /**
     * Add the routes Every route is an absolute path Parameters starts with ":"
     * Handler class should implement @UriResponder interface If the handler not
     * implement UriResponder interface - toString() is used
     */
    @Override
    public void addMappings() {
        super.addMappings();
        addRoute("/user", UserHandler.class);
        addRoute("/user/:id", UserHandler.class);
        addRoute("/user/help", GeneralHandler.class);
        addRoute("/general/:param1/:param2", GeneralHandler.class);
        addRoute("/photos/:customer_id/:photo_id", null);
        addRoute("/test", String.class);
        addRoute("/interface", UriResponder.class); // this will cause an error
                                                    // when called
        addRoute("/toBeDeleted", String.class);
        removeRoute("/toBeDeleted");
        addRoute("/stream", StreamUrl.class);
        addRoute("/browse/(.)+", StaticPageTestHandler.class, new File("src/test/resources").getAbsoluteFile());
    }

    /**
     * Main entry point
     * 
     * @param args
     */
    public static void main(String[] args) {
        ServerRunner.run(AppNanolets.class);
    }
}
