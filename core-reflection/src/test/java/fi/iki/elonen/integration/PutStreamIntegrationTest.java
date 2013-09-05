package fi.iki.elonen.integration;

import static org.junit.Assert.assertEquals;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fi.iki.elonen.NanoHTTPDReflection;
import fi.iki.elonen.NanoHTTPDReflection.ExpectParam;
import fi.iki.elonen.NanoHTTPDReflection.Path;

import fi.iki.elonen.NanoHTTPD;

public class PutStreamIntegrationTest {

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
    public void testSimplePutRequest() throws Exception {
        String expected = "This HttpPut request has a content-length of 48.";

        HttpPut httpput = new HttpPut("http://localhost:8080/put/message");
        httpput.setEntity(new ByteArrayEntity(expected.getBytes()));
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpput, responseHandler);

        assertEquals("PUT:" + expected, responseBody);
    }

    public static class TestServer extends NanoHTTPDReflection {
        public TestServer() {
            super(8080);
        }

        @Path("/put/message")
        public Response someMethod(HTTPSession session) {
            Method method = session.getMethod();
            Map<String, String> headers = session.getHeaders();
            int contentLength = Integer.parseInt(headers.get("content-length"));

            byte[] body;
            try {
                DataInputStream dataInputStream = new DataInputStream(session.getInputStream());
                body = new byte[contentLength];
                dataInputStream.readFully(body, 0, contentLength);
            }
            catch(IOException e) {
                return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
            }
            
            String response = String.valueOf(method) + ':' + new String(body);
            return new Response(response);
        }
    }
}
