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
        assertLinesOfText(expected, lines);
    }

    protected void assertLinesOfText(String[] expected, List<String> lines) {
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
        return readLinesFromFile(reader);
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

    public static class TestServer extends NanoHTTPD {
        public Response response = new Response("");
        public String uri;
        public Method method;
        public Map<String, String> header;
        public Map<String, String> parms;
        public Map<String, String> files;
        public Map<String, List<String>> decodedParamters;
        public Map<String, List<String>> decodedParamtersFromParameter;

        public TestServer() {
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
