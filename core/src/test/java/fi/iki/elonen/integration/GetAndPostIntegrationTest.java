package fi.iki.elonen.integration;

import fi.iki.elonen.NanoHTTPD;
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

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com)
 *         On: 5/19/13 at 5:36 PM
 */
public class GetAndPostIntegrationTest extends IntegrationTestBase<GetAndPostIntegrationTest.TestServer> {

    @Test
    public void testSimpleGetRequest() throws Exception {
        testServer.response = "testSimpleGetRequest";

        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpget, responseHandler);

        assertEquals("GET:testSimpleGetRequest", responseBody);
    }

    @Test
    public void testGetRequestWithParameters() throws Exception {
        testServer.response = "testGetRequestWithParameters";

        HttpGet httpget = new HttpGet("http://localhost:8192/?age=120&gender=Male");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpget, responseHandler);

        assertEquals("GET:testGetRequestWithParameters-params=2;age=120;gender=Male", responseBody);
    }

    @Test
    public void testPostWithNoParameters() throws Exception {
        testServer.response = "testPostWithNoParameters";

        HttpPost httppost = new HttpPost("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httppost, responseHandler);

        assertEquals("POST:testPostWithNoParameters", responseBody);
    }

    @Test
    public void testPostRequestWithFormEncodedParameters() throws Exception {
        testServer.response = "testPostRequestWithFormEncodedParameters";

        HttpPost httppost = new HttpPost("http://localhost:8192/");
        List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("age", "120"));
        postParameters.add(new BasicNameValuePair("gender", "Male"));
        httppost.setEntity(new UrlEncodedFormEntity(postParameters));

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httppost, responseHandler);

        assertEquals("POST:testPostRequestWithFormEncodedParameters-params=2;age=120;gender=Male", responseBody);
    }

    @Test
    public void testPostRequestWithMultipartEncodedParameters() throws Exception {
        testServer.response = "testPostRequestWithMultipartEncodedParameters";

        HttpPost httppost = new HttpPost("http://localhost:8192/");
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
        reqEntity.addPart("age", new StringBody("120"));
        reqEntity.addPart("gender", new StringBody("Male"));
        httppost.setEntity(reqEntity);

        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httppost, responseHandler);

        assertEquals("POST:testPostRequestWithMultipartEncodedParameters-params=2;age=120;gender=Male", responseBody);
    }

    @Override public TestServer createTestServer() {
        return new TestServer();
    }

    public static class TestServer extends NanoHTTPD {
        public String response;

        public TestServer() {
            super(8192);
        }

        @Override
        public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
            StringBuilder sb = new StringBuilder(String.valueOf(method) + ':' + response);

            if (parms.size() > 1) {
                parms.remove("NanoHttpd.QUERY_STRING");
                sb.append("-params=").append(parms.size());
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
