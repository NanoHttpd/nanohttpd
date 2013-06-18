package fi.iki.elonen;

import org.junit.Test;

import java.io.ByteArrayOutputStream;

public class HttpErrorsTest extends HttpServerTest {

    @Test
    public void testEmptyRequest() throws Exception {
        ByteArrayOutputStream outputStream = invokeServer("");

        String[] expected = {
                "HTTP/1.1 400 Bad Request",
                "Content-Type: text/plain",
                "Date: .*",
                "Connection: keep-alive",
                "Content-Length: 26",
                "",
                "BAD REQUEST: Syntax error.",
        };

        assertResponse(outputStream, expected);
    }

}
