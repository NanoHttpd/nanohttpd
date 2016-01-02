package fi.iki.elonen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.junit.Test;

import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.Response;

public class CookieHandlerTest extends HttpServerTest {

	@Test
	public void testCookieHeaderCorrectlyParsed() throws IOException {
		StringBuilder requestBuilder = new StringBuilder();
		requestBuilder.append("GET " + HttpServerTest.URI + " HTTP/1.1").append(System.getProperty("line.separator"))
				.append("Cookie: theme=light; sessionToken=abc123");

		ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBuilder.toString().getBytes());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		NanoHTTPD.HTTPSession session = this.testServer.createSession(this.tempFileManager, inputStream, outputStream);
		session.execute();
		Set<String> allCookies = new HashSet<String>();
		CookieHandler cookieHandler = session.getCookies();
		for (String cookie : cookieHandler) {
			allCookies.add(cookie);
		}
		assertTrue("cookie specified in header not correctly parsed", allCookies.contains("theme"));
		assertTrue("cookie specified in header not correctly parsed", allCookies.contains("sessionToken"));
		assertEquals("cookie value not correctly parsed", "light", cookieHandler.read("theme"));
		assertEquals("cookie value not correctly parsed", "abc123", cookieHandler.read("sessionToken"));

	}
	
	@Test
	public void testCookieHeaderWithSpecialCharactersCorrectlyParsed() throws IOException {
		StringBuilder requestBuilder = new StringBuilder();
		//not including ; = and ,
		requestBuilder.append("GET " + HttpServerTest.URI + " HTTP/1.1").append(System.getProperty("line.separator"))
				.append("Cookie: theme=light; sessionToken=abc123!@#$%^&*()-_+{}[]\\|:\"'<>.?/");

		ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBuilder.toString().getBytes());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		NanoHTTPD.HTTPSession session = this.testServer.createSession(this.tempFileManager, inputStream, outputStream);
		session.execute();
		Set<String> allCookies = new HashSet<String>();
		CookieHandler cookieHandler = session.getCookies();
		for (String cookie : cookieHandler) {
			allCookies.add(cookie);
		}
		assertTrue("cookie specified in header not correctly parsed", allCookies.contains("theme"));
		assertTrue("cookie specified in header not correctly parsed", allCookies.contains("sessionToken"));
		assertEquals("cookie value not correctly parsed", "light", cookieHandler.read("theme"));
		assertEquals("cookie value not correctly parsed", "abc123!@#$%^&*()-_+{}[]\\|:\"'<>.?/", cookieHandler.read("sessionToken"));

	}

	@Test
	public void testUnloadQueue() throws IOException {
		StringBuilder requestBuilder = new StringBuilder();
		requestBuilder.append("GET " + HttpServerTest.URI + " HTTP/1.1").append(System.getProperty("line.separator"))
				.append("Cookie: theme=light; sessionToken=abc123");

		ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBuilder.toString().getBytes());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		NanoHTTPD.HTTPSession session = this.testServer.createSession(this.tempFileManager, inputStream, outputStream);
		session.execute();
		CookieHandler cookieHandler = session.getCookies();
		Response response = NanoHTTPD.newFixedLengthResponse("");
		cookieHandler.set("name", "value", 30);
		cookieHandler.unloadQueue(response);
		String setCookieHeader = response.getHeader("Set-Cookie");
		assertTrue("unloadQueue did not set the cookies correctly", setCookieHeader.startsWith("name=value; expires="));
	}

	@Test
	public void testDelete() throws IOException, ParseException {
		StringBuilder requestBuilder = new StringBuilder();
		requestBuilder.append("GET " + HttpServerTest.URI + " HTTP/1.1").append(System.getProperty("line.separator"))
				.append("Cookie: theme=light; sessionToken=abc123");

		ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBuilder.toString().getBytes());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		NanoHTTPD.HTTPSession session = this.testServer.createSession(this.tempFileManager, inputStream, outputStream);
		session.execute();
		CookieHandler cookieHandler = session.getCookies();

		Response response = NanoHTTPD.newFixedLengthResponse("");
		cookieHandler.delete("name");
		cookieHandler.unloadQueue(response);

		String setCookieHeader = response.getHeader("Set-Cookie");
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		String dateString = setCookieHeader.split(";")[1].split("=")[1].trim();
		Date date = dateFormat.parse(dateString);
		assertTrue("Deleted cookie's expiry time should be a time in the past", date.compareTo(new Date()) < 0);
	}
}
