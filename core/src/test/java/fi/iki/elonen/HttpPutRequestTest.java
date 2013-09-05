package fi.iki.elonen;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.util.List;

import static junit.framework.Assert.*;

public class HttpPutRequestTest extends HttpServerTest {

    @Test
    public void testPutRequestSendsContent() throws Exception {
        ByteArrayOutputStream outputStream = invokeServer("PUT " + URI + " HTTP/1.1\r\n\r\nBodyData 1\nLine 2");

        String[] expectedOutput = {
                "HTTP/1.1 200 OK",
                "Content-Type: text/html",
                "Date: .*",
                "Connection: keep-alive",
                "Content-Length: 0",
                ""
        };

        assertResponse(outputStream, expectedOutput);

        assertTrue(testServer.files.containsKey("content"));
        BufferedReader reader = null;
        try {
            String[] expectedInputToServeMethodViaFile = {
                    "BodyData 1",
                    "Line 2"
            };
            reader = new BufferedReader(new FileReader(testServer.files.get("content")));
            List<String> lines = readLinesFromFile(reader);
            assertLinesOfText(expectedInputToServeMethodViaFile, lines);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
