package org.nanohttpd.junit.protocols.http.integration;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com) On: 5/19/13 at 5:36 PM
 */
public class GetAndPostIntegrationTest extends IntegrationTestBase<GetAndPostIntegrationTest.TestServer> {

    public static class TestServer extends NanoHTTPD {

        public String response;

        public TestServer() {
            super(8192);
        }

        @Override
        public Response serve(IHTTPSession session) {
            StringBuilder sb = new StringBuilder(String.valueOf(session.getMethod()) + ':' + this.response);

            Method method = session.getMethod();
            Map<String, String> files = new HashMap<String, String>();
            if (Method.PUT.equals(method) || Method.POST.equals(method)) {
                try {
                    session.parseBody(files);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }

            String uri = session.getUri();
            Map<String, String> parms = session.getParms();
            if (parms.size() > 1) {
                parms.remove("NanoHttpd.QUERY_STRING");
                sb.append("-params=").append(parms.size());
                List<String> p = new ArrayList<String>(parms.keySet());
                Collections.sort(p);
                for (String k : p) {
                    sb.append(';').append(k).append('=').append(parms.get(k));
                }
            }
            if ("/encodingtest".equals(uri)) {
                return Response.newFixedLengthResponse(Status.OK, MIME_HTML, "<html><head><title>Testé ça</title></head><body>Testé ça</body></html>");
            } else if ("/chin".equals(uri)) {
                return Response.newFixedLengthResponse(Status.OK, "application/octet-stream", sb.toString());
            } else {
                return Response.newFixedLengthResponse(sb.toString());
            }
        }
    }

    @Override
    public TestServer createTestServer() {
        return new TestServer();
    }

    @Test
    public void testGetRequestWithParameters() throws Exception {
        this.testServer.response = "testGetRequestWithParameters";

        HttpGet httpget = new HttpGet("http://localhost:8192/?age=120&gender=Male");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = this.httpclient.execute(httpget, responseHandler);

        assertEquals("GET:testGetRequestWithParameters-params=2;age=120;gender=Male", responseBody);
    }

    @Test
    public void testPostRequestWithFormEncodedParameters() throws Exception {
        this.testServer.response = "testPostRequestWithFormEncodedParameters";

        HttpPost httppost = new HttpPost("http://localhost:8192/");
        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("age", "120"));
        postParameters.add(new BasicNameValuePair("gender", "Male"));
        httppost.setEntity(new UrlEncodedFormEntity(postParameters));

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = this.httpclient.execute(httppost, responseHandler);

        assertEquals("POST:testPostRequestWithFormEncodedParameters-params=2;age=120;gender=Male", responseBody);
    }

    @Test
    public void testPostRequestWithMultipartEncodedParameters() throws Exception {
        this.testServer.response = "testPostRequestWithMultipartEncodedParameters";

        HttpPost httppost = new HttpPost("http://localhost:8192/");
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart("age", new StringBody("120"));
        reqEntity.addPart("gender", new StringBody("Male"));
        httppost.setEntity(reqEntity);

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = this.httpclient.execute(httppost, responseHandler);

        assertEquals("POST:testPostRequestWithMultipartEncodedParameters-params=2;age=120;gender=Male", responseBody);
    }

    @Test
    public void testPostWithNoParameters() throws Exception {
        this.testServer.response = "testPostWithNoParameters";

        HttpPost httppost = new HttpPost("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = this.httpclient.execute(httppost, responseHandler);

        assertEquals("POST:testPostWithNoParameters", responseBody);
    }

    @Test
    public void testSimpleGetRequest() throws Exception {
        this.testServer.response = "testSimpleGetRequest";

        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = this.httpclient.execute(httpget, responseHandler);

        assertEquals("GET:testSimpleGetRequest", responseBody);
    }

    @Test
    public void testPostRequestWithMultipartExtremEncodedParameters() throws Exception {
        this.testServer.response = "testPostRequestWithMultipartEncodedParameters";

        HttpPost httppost = new HttpPost("http://localhost:8192/chin");
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, "sfsadfasdf", Charset.forName("UTF-8"));
        reqEntity.addPart("specialString", new StringBody("拖拉图片到浏览器，可以实现预览功能", "text/plain", Charset.forName("UTF-8")));
        reqEntity.addPart("gender", new StringBody("图片名称", Charset.forName("UTF-8")) {

            @Override
            public String getFilename() {
                return "图片名称";
            }
        });
        httppost.setEntity(reqEntity);
        HttpResponse response = this.httpclient.execute(httppost);

        HttpEntity entity = response.getEntity();
        String responseBody = EntityUtils.toString(entity, "UTF-8");

        assertEquals("POST:testPostRequestWithMultipartEncodedParameters-params=2;gender=图片名称;specialString=拖拉图片到浏览器，可以实现预览功能", responseBody);
    }

    @Test
    public void testPostRequestWithEncodedParameters() throws Exception {
        this.testServer.response = "testPostRequestWithEncodedParameters";

        HttpPost httppost = new HttpPost("http://localhost:8192/encodingtest");
        HttpResponse response = this.httpclient.execute(httppost);

        HttpEntity entity = response.getEntity();
        String responseBody = EntityUtils.toString(entity);

        assertEquals("<html><head><title>Testé ça</title></head><body>Testé ça</body></html>", responseBody);
    }
}
