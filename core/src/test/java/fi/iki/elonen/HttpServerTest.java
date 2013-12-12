package fi.iki.elonen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com)
 *         On: 3/10/13 at 8:32 PM
 */
public class HttpServerTest {
    public static final String URI = "http://www.myserver.org/pub/WWW/someFile.html";
    protected TestServer testServer;
    private TestTempFileManager tempFileManager;

    @Before
    public void setUp() {
        testServer = new TestServer();
        tempFileManager = new TestTempFileManager();
    }

    @After
    public void tearDown() {
        tempFileManager._clear();
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
//        assertEquals(expected.length, lines.size());
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
        NanoHTTPD.HTTPSession session = testServer.createSession(tempFileManager, inputStream, outputStream);
        try {
            session.execute();
        } catch (IOException e) {
            fail(""+e);
            e.printStackTrace();
        }
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

    public static class TestTempFileManager extends NanoHTTPD.DefaultTempFileManager {
        public void _clear() {
            super.clear();
        }

        @Override
        public void clear() {
            // ignore
        }
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
        public String queryParameterString;

        public TestServer() {
            super(8192);
        }

        public HTTPSession createSession(TempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream) {
            return new HTTPSession(tempFileManager, inputStream, outputStream);
        }

        public HTTPSession createSession(TempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream, InetAddress inetAddress) {
            return new HTTPSession(tempFileManager, inputStream, outputStream, inetAddress);
        }

        @Override public Response serve(IHTTPSession session) {
            this.uri = session.getUri();
            this.method = session.getMethod();
            this.header = session.getHeaders();
            this.parms = session.getParms();
            this.files = new HashMap<String, String>();
            try {
                session.parseBody(files);
            } catch (Exception e) {
                e.printStackTrace();
            }
            queryParameterString = session.getQueryParameterString();
            this.decodedParamtersFromParameter = decodeParameters(queryParameterString);
            this.decodedParamters = decodeParameters(session.getQueryParameterString());
            return response;
        }

        @Override
        public String decodePercent(String str) {
            return super.decodePercent(str);
        }
    }
}
