package fi.iki.elonen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

public class BadRequestTest extends HttpServerTest {

	@Test
	public void testEmptyRequest() throws IOException {
		ByteArrayOutputStream outputStream = invokeServer("\n\n");
		String[] expected = new String[] { "HTTP/1.1 400 Bad Request" };
		assertResponse(outputStream, expected);
	}

	@Test
	public void testInvalidMethod() throws IOException {
		ByteArrayOutputStream outputStream = invokeServer("GETT http://example.com");
		String[] expected = new String[] { "HTTP/1.1 400 Bad Request" };
		assertResponse(outputStream, expected);
	}

	@Test
	public void testMissingURI() throws IOException {
		ByteArrayOutputStream outputStream = invokeServer("GET");
		String[] expected = new String[] { "HTTP/1.1 400 Bad Request" };
		assertResponse(outputStream, expected);
	}

}
