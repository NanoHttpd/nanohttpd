package fi.iki.elonen.integration;

import static org.junit.Assert.assertEquals;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Test;

import fi.iki.elonen.NanoHTTPD;

public class PutStreamIntegrationTest extends IntegrationTestBase<PutStreamIntegrationTest.TestServer> {

    @Test
    public void testSimplePutRequest() throws Exception {
        String expected = "This HttpPut request has a content-length of 48.";

        HttpPut httpput = new HttpPut("http://localhost:8192/");
        httpput.setEntity(new ByteArrayEntity(expected.getBytes()));
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody = httpclient.execute(httpput, responseHandler);

        assertEquals("PUT:" + expected, responseBody);
    }

    @Override public TestServer createTestServer() {
        return new TestServer();
    }

    public static class TestServer extends NanoHTTPD {
        public TestServer() {
            super(8192);
        }

        @Override
        public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Response serve(IHTTPSession session) {
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
