package fi.iki.elonen;

import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com)
 *         On: 3/10/13 at 8:32 PM
 */
public class HttpServerTest {
    public static final String URI = "http://www.myserver.org/pub/WWW/someFile.html";
    protected TestServer testServer;

    @Before
    public void setUp() {
        testServer = new TestServer();
    }

    @Test
    public void testServerExists() {
        assertNotNull(testServer);
    }

    protected void assertResponse(ByteArrayOutputStream outputStream, String[] expected) throws IOException {
        List<String> lines = getOutputLines(outputStream);
        assertEquals(expected.length, lines.size());
        for (int i = 0; i < expected.length; i++) {
            String line = lines.get(i);
            assertTrue("Output line " + i + " doesn't match expectation.\n" +
                    "  Output: " + line + "\n" +
                    "Expected: " + expected[i], line.matches(expected[i]));
        }
    }

    protected ByteArrayOutputStream invokeServer(String request) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        NanoHTTPD.HTTPSession session = testServer.createSession(new NanoHTTPD.DefaultTempFileManager(), inputStream, outputStream);
        session.run();
        return outputStream;
    }

    protected List<String> getOutputLines(ByteArrayOutputStream outputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
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

    protected static class TestServer extends NanoHTTPD {
        protected Response response = new Response("");
        protected String uri;
        protected Method method;
        protected Map<String, String> header;
        protected Map<String, String> parms;
        protected Map<String, String> files;
        protected Map<String, List<String>> decodedParamters;
        protected Map<String, List<String>> decodedParamtersFromParameter;

        private TestServer() {
            super(8080);
        }

        public HTTPSession createSession(TempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream) {
            return new HTTPSession(tempFileManager, inputStream, outputStream);
        }

        @Override
        public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
            this.uri = uri;
            this.method = method;
            this.header = header;
            this.parms = parms;
            this.files = files;
            this.decodedParamtersFromParameter = decodeParameters(parms);
            this.decodedParamters = decodeParameters(parms.get("NanoHttpd.QUERY_STRING"));
            return response;
        }

        @Override
        public String decodePercent(String str) {
            return super.decodePercent(str);
        }
    }
}
