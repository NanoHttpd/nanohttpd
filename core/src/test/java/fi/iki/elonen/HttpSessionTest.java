package fi.iki.elonen;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

public class HttpSessionTest  extends HttpServerTest{
	private static final String DUMMY_REQUEST_CONTENT = "dummy request content";
	private static final TestTempFileManager TEST_TEMP_FILE_MANAGER = new TestTempFileManager();

	@Test
	public void testSessionRemoteHostnameLocalhost() throws UnknownHostException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(HttpSessionTest.DUMMY_REQUEST_CONTENT.getBytes());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
		NanoHTTPD.HTTPSession session = this.testServer.createSession(HttpSessionTest.TEST_TEMP_FILE_MANAGER, inputStream, outputStream, inetAddress);
		assertEquals("localhost", session.getRemoteHostName());
	}

	@Test
	public void testSessionRemoteHostname() throws UnknownHostException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(HttpSessionTest.DUMMY_REQUEST_CONTENT.getBytes());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		InetAddress inetAddress = InetAddress.getByName("google.com");
		NanoHTTPD.HTTPSession session = this.testServer.createSession(HttpSessionTest.TEST_TEMP_FILE_MANAGER, inputStream, outputStream, inetAddress);
		assertEquals("google.com", session.getRemoteHostName());
	}

	@Test
	public void testSessionRemoteIPAddress() throws UnknownHostException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(HttpSessionTest.DUMMY_REQUEST_CONTENT.getBytes());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
		NanoHTTPD.HTTPSession session = this.testServer.createSession(HttpSessionTest.TEST_TEMP_FILE_MANAGER, inputStream, outputStream, inetAddress);
		assertEquals("127.0.0.1", session.getRemoteIpAddress());
	}
}
