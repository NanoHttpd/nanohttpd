package fi.iki.elonen;

/*
 * #%L
 * NanoHttpd-apache file upload integration
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import fi.iki.elonen.NanoHTTPD.Response.Status;

public class TestNanoFileUpLoad {

    protected TestServer testServer;

    public static class TestServer extends NanoHTTPD {

        public Response response = newFixedLengthResponse("");

        public String uri;

        public Method method;

        public Map<String, String> header;

        public Map<String, String> parms;

        public Map<String, List<FileItem>> files;

        public Map<String, List<String>> decodedParamters;

        public Map<String, List<String>> decodedParamtersFromParameter;

        public String queryParameterString;

        public TestServer() {
            super(8192);
            uploader = new NanoFileUpload(new DiskFileItemFactory());
        }

        public HTTPSession createSession(TempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream) {
            return new HTTPSession(tempFileManager, inputStream, outputStream);
        }

        public HTTPSession createSession(TempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream, InetAddress inetAddress) {
            return new HTTPSession(tempFileManager, inputStream, outputStream, inetAddress);
        }

        @Override
        public String decodePercent(String str) {
            return super.decodePercent(str);
        }

        NanoFileUpload uploader;

        @Override
        public Response serve(IHTTPSession session) {

            this.uri = session.getUri();
            this.method = session.getMethod();
            this.header = session.getHeaders();
            this.parms = session.getParms();
            if (NanoFileUpload.isMultipartContent(session)) {
                try {
                    files = uploader.parseParameterMap(session);
                } catch (FileUploadException e) {
                    this.response.setStatus(Status.INTERNAL_ERROR);
                    e.printStackTrace();
                }
            }
            this.queryParameterString = session.getQueryParameterString();
            this.decodedParamtersFromParameter = decodeParameters(this.queryParameterString);
            this.decodedParamters = decodeParameters(session.getQueryParameterString());
            return this.response;
        }

    }

    @Test
    public void testNormalRequest() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpTrace httphead = new HttpTrace("http://localhost:8192/index.html");
        CloseableHttpResponse response = httpclient.execute(httphead);
        response.close();

    }

    @Ignore
    @Test
    public void testPostWithMultipartFormUpload() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String textFileName = "src/test/java/fi/iki/elonen/TestNanoFileUpLoad.java";
        HttpPost post = new HttpPost("http://localhost:8192/uploadFile");
        FileBody fileBody = new FileBody(new File(textFileName), ContentType.DEFAULT_BINARY);
        StringBody stringBody1 = new StringBody("Message 1", ContentType.MULTIPART_FORM_DATA);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("upfile", fileBody);
        builder.addPart("text1", stringBody1);
        HttpEntity entity = builder.build();
        //
        post.setEntity(entity);
        HttpResponse response = httpclient.execute(post);
        response.toString();
    }

    @Before
    public void setUp() throws IOException {
        this.testServer = new TestServer();
        this.testServer.start();
    }

    @After
    public void tearDown() {
        this.testServer.stop();
    }

}
