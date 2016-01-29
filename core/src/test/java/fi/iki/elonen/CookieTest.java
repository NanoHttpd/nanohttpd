package fi.iki.elonen;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import fi.iki.elonen.NanoHTTPD.Cookie;

public class CookieTest {

	@Test
	public void testGetHTTPTime() {
		Random random = new Random();
		int randomExpirationTime = random.nextInt(100);
		assertNotNull("getHTTPTime should return a non-null value for " + randomExpirationTime + " days",
				Cookie.getHTTPTime(randomExpirationTime));
	}

	@Test
	public void testCookieWithNoExplicitExpirationTime() {
		Cookie cookie = new Cookie("CookieKey", "CookieValue");
		assertTrue("Cookie header should contain cookie key", cookie.getHTTPHeader().contains("CookieKey"));
		assertTrue("Cookie header should contain cookie value", cookie.getHTTPHeader().contains("CookieValue"));
	}

	@Test
	public void testCookieWithExplicitExpirationTime() {
		Cookie cookie = new Cookie("CookieKey", "CookieValue", 40);
		assertFalse(
				"The default 30 days expires string should not be avaialbe in the cookie header"
						+ " because the expiry has been specified as 40 days",
				cookie.getHTTPHeader().contains(Cookie.getHTTPTime(30)));
		assertTrue("Cookie header should contain cookie key", cookie.getHTTPHeader().contains("CookieKey"));
		assertTrue("Cookie header should contain cookie value", cookie.getHTTPHeader().contains("CookieValue"));
	}

	@Test
	public void testCookieWithExpiresString() {
		Random random = new Random();
		int randomExpirationTime = random.nextInt(100);
		String expiresString = Cookie.getHTTPTime(randomExpirationTime);
		Cookie cookie = new Cookie("CookieKey", "CookieValue", expiresString);
		assertTrue("Cookie should contain the expirs string passed in the constructor",
				cookie.getHTTPHeader().contains(expiresString));
		assertTrue("Cookie header should contain cookie key", cookie.getHTTPHeader().contains("CookieKey"));
		assertTrue("Cookie header should contain cookie value", cookie.getHTTPHeader().contains("CookieValue"));
	}

}
