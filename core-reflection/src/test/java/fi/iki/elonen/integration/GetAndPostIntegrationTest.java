package fi.iki.elonen.integration;

import fi.iki.elonen.NanoHTTPDReflection;
import fi.iki.elonen.NanoHTTPDReflection.ExpectParam;
import fi.iki.elonen.NanoHTTPDReflection.Path;
import fi.iki.elonen.NanoHTTPDReflection.AuthorizationRequired;

import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GetAndPostIntegrationTest {

    private HttpClient httpclient;
    private TestServer testServer;

    @Before
    public void setUp() {
        testServer = new TestServer();
        httpclient = new DefaultHttpClient();
        try {
            testServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        httpclient.getConnectionManager().shutdown();
        testServer.stop();
    }

    @Test
    public void testSimpleGetRequestWithAuthorizationMissing() throws Exception {
        HttpGet httpget = new HttpGet("http://localhost:8080/authorizedOnly");
        HttpResponse httpResponse = httpclient.execute(httpget);
        assertEquals(403, httpResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testSimpleGetRequestWithAuthorization() throws Exception {
        HttpGet httpget = new HttpGet("http://localhost:8080/authorizedOnly");
        httpget.setHeader("Authorization", "friend");
        HttpResponse httpResponse = httpclient.execute(httpget);
        assertEquals(200, httpResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void testSimpleGetRequest() throws Exception {
        HttpGet httpget = new HttpGet("http://localhost:8080/uri");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpget, responseHandler);
        assertEquals("/uri", responseBody);
    }

    @Test
    public void testSimpleGetRequestWithHeader() throws Exception {
        String headerKey = "someHeader";
        String headerValue = "this is the value";

        HttpGet httpget = new HttpGet("http://localhost:8080/header?key="+headerKey);
        httpget.setHeader(headerKey, headerValue);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpget, responseHandler);

        assertEquals(headerValue, responseBody);
    }

    @Test
    public void testGetRequestWithParameters() throws Exception {
        String message = "hello";

        HttpGet httpget = new HttpGet("http://localhost:8080/echo?message="+message);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpget, responseHandler);

        assertEquals(message, responseBody);
    }

    @Test
    public void testPostWithNoParameters() throws Exception {
        HttpPost httppost = new HttpPost("http://localhost:8080/post/paramless");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httppost, responseHandler);
        assertEquals("POST:/post/paramless", responseBody);
    }

    @Test
    public void testPostRequestWithFormEncodedParameters() throws Exception {
        HttpPost httppost = new HttpPost("http://localhost:8080/post/parameters");
        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("age", "120"));
        postParameters.add(new BasicNameValuePair("gender", "Male"));
        httppost.setEntity(new UrlEncodedFormEntity(postParameters));

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httppost, responseHandler);

        assertEquals("POST:params=2;age=120;gender=Male", responseBody);
    }

    @Test
    public void testPostRequestWithMultipartEncodedParameters() throws Exception {
        HttpPost httppost = new HttpPost("http://localhost:8080/post/parameters");
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart("age", new StringBody("120"));
        reqEntity.addPart("gender", new StringBody("Male"));
        httppost.setEntity(reqEntity);

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httppost, responseHandler);

        assertEquals("POST:params=2;age=120;gender=Male", responseBody);
    }

    public static class TestServer extends NanoHTTPDReflection {
        public TestServer() {
            super(8080);
        }

        @Override
        protected boolean authorized(HTTPSession session) {
            Map<String, String> headers = session.getHeaders();
            String authorization = headers.get("Authorization".toLowerCase());
            return authorization != null && authorization.equals("friend");
        }

        @Path("/authorizedOnly")
        @AuthorizationRequired
        public Response checkFriend(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {
            return new Response(null);
        }

        @Path("/header")
        @ExpectParam("key")
        public Response headerValue(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {
            String key = parms.get("key");
            return new Response(headers.get(key.toLowerCase()));
        }

        @Path("/echo")
        @ExpectParam("message")
        public Response echo(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {
            String message = parms.get("message");
            return new Response(message);
        }

        @Path("/uri")
        public Response whoami(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {
            return new Response(uri);
        }

        @Path("/post/paramless")
        public Response paramless(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {
            return new Response(String.valueOf(method) + ':' + uri);
        }

        @Path("/post/parameters")
        public Response query(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
            StringBuilder sb = new StringBuilder(String.valueOf(method) + ':');

            if (parms.size() > 1) {
                parms.remove("NanoHttpd.QUERY_STRING");
                sb.append("params=").append(parms.size());
                List<String> p = new ArrayList<String>(parms.keySet());
                Collections.sort(p);
                for (String k : p) {
                    sb.append(';').append(k).append('=').append(parms.get(k));
                }
            }

            return new Response(sb.toString());
        }
    }
}
