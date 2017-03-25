package org.nanohttpd.junit.protocols.http;

import static org.junit.Assert.assertEquals;

/*
 * #%L
 * NanoHttpd-Core
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.tempfiles.DefaultTempFileManager;
import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com) On: 3/10/13 at 8:32 PM
 */
public class HttpServerTest {

    public static class TestServer extends NanoHTTPD {

        public Response response = Response.newFixedLengthResponse("");

        public String uri;

        public Method method;

        public Map<String, String> header;

        public Map<String, String> parms;

        public Map<String, List<String>> parameters;

        public Map<String, String> files;

        public Map<String, List<String>> decodedParamters;

        public Map<String, List<String>> decodedParamtersFromParameter;

        public String queryParameterString;

        public TestServer() {
            super(8192);
        }

        public TestServer(int port) {
            super(port);
        }

        public HTTPSession createSession(ITempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream) {
            return new HTTPSession(this, tempFileManager, inputStream, outputStream);
        }

        public HTTPSession createSession(ITempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream, InetAddress inetAddress) {
            return new HTTPSession(this, tempFileManager, inputStream, outputStream, inetAddress);
        }

        @Override
        public Response serve(IHTTPSession session) {
            this.uri = session.getUri();
            this.method = session.getMethod();
            this.header = session.getHeaders();
            this.files = new HashMap<String, String>();
            try {
                session.parseBody(this.files);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.parms = session.getParms();
            this.parameters = session.getParameters();
            this.queryParameterString = session.getQueryParameterString();
            this.decodedParamtersFromParameter = decodeParameters(this.queryParameterString);
            this.decodedParamters = decodeParameters(session.getQueryParameterString());

            return this.response;
        }
    }

    public static class TestTempFileManager extends DefaultTempFileManager {

        public void _clear() {
            super.clear();
        }

        @Override
        public void clear() {
            // ignore
        }
    }

    public static final String URI = "http://www.myserver.org/pub/WWW/someFile.html";

    protected TestServer testServer;

    protected TestTempFileManager tempFileManager;

    protected void assertLinesOfText(String[] expected, List<String> lines) {
        // assertEquals(expected.length, lines.size());
        for (int i = 0; i < expected.length; i++) {
            String line = lines.get(i);
            assertTrue("Output line " + i + " doesn't match expectation.\n" + "  Output: " + line + "\n" + "Expected: " + expected[i], line.matches(expected[i]));
        }
    }

    protected void assertResponse(ByteArrayOutputStream outputStream, String[] expected) throws IOException {
        List<String> lines = getOutputLines(outputStream);
        assertLinesOfText(expected, lines);
    }

    protected List<String> getOutputLines(ByteArrayOutputStream outputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
        return readLinesFromFile(reader);
    }

    protected ByteArrayOutputStream invokeServer(String request) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HTTPSession session = this.testServer.createSession(this.tempFileManager, inputStream, outputStream);
        try {
            session.execute();
        } catch (IOException e) {
            fail("" + e);
            e.printStackTrace();
        }
        return outputStream;
    }

    protected List<String> readLinesFromFile(BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<String>();
        String line = "";
        while (line != null) {
            line = reader.readLine();
            if (line != null) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    @Before
    public void setUp() throws Exception {
        this.testServer = new TestServer();
        this.tempFileManager = new TestTempFileManager();
    }

    @After
    public void tearDown() {
        this.tempFileManager._clear();
    }

    @Test
    public void testServerExists() {
        assertNotNull(this.testServer);
    }

    @Test
    public void testMultipartFormData() throws IOException {
        final int testPort = 4589;
        NanoHTTPD server = null;

        try {
            server = new NanoHTTPD(testPort) {

                final Map<String, String> files = new HashMap<String, String>();

                @Override
                public Response serve(IHTTPSession session) {
                    StringBuilder responseMsg = new StringBuilder();

                    try {
                        session.parseBody(this.files);
                        for (String key : files.keySet()) {
                            responseMsg.append(key);
                        }
                    } catch (Exception e) {
                        responseMsg.append(e.getMessage());
                    }

                    return Response.newFixedLengthResponse(responseMsg.toString());
                }
            };
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://localhost:" + testPort);

            final String fileName = "file-upload-test.htm";
            FileBody bin = new FileBody(new File(getClass().getClassLoader().getResource(fileName).getFile()));
            StringBody comment = new StringBody("Filename: " + fileName);

            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("bin", bin);
            reqEntity.addPart("comment", comment);
            httppost.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStream instream = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
                String line = reader.readLine();
                assertNotNull(line, "Invalid server reponse");
                assertEquals("Server failed multi-part data parse" + line, "bincomment", line);
                reader.close();
                instream.close();
            }
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    @Test
    public void testTempFileInterface() throws IOException {
        final int testPort = 4589;
        NanoHTTPD server = new NanoHTTPD(testPort) {

            final Map<String, String> files = new HashMap<String, String>();

            @Override
            public Response serve(IHTTPSession session) {
                String responseMsg = "pass";

                try {
                    session.parseBody(this.files);
                    for (String key : files.keySet()) {
                        if (!(new File(files.get(key))).exists()) {
                            responseMsg = "fail";
                        }
                    }
                } catch (Exception e) {
                    responseMsg = e.getMessage();
                }

                return Response.newFixedLengthResponse(responseMsg.toString());
            }
        };
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://localhost:" + testPort);

        final String fileName = "file-upload-test.htm";
        FileBody bin = new FileBody(new File(getClass().getClassLoader().getResource(fileName).getFile()));
        StringBody comment = new StringBody("Filename: " + fileName);

        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart("bin", bin);
        reqEntity.addPart("comment", comment);
        httppost.setEntity(reqEntity);

        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            InputStream instream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
            String line = reader.readLine();
            assertNotNull(line, "Invalid server reponse");
            assertEquals("Server file check failed: " + line, "pass", line);
            reader.close();
            instream.close();
        } else {
            fail("No server response");
        }
        server.stop();
    }
}
