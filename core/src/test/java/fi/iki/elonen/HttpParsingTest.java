package fi.iki.elonen;

import org.junit.Test;

import java.net.URLDecoder;
import java.net.URLEncoder;

import static junit.framework.Assert.assertEquals;

public class HttpParsingTest extends HttpServerTest {
    @Test
    public void testNormalCharacters() throws Exception {
        for (int i = 0x20; i < 0x80; i++) {
            String hex = Integer.toHexString(i);
            String input = "%" + hex;
            char expected = (char) i;
            assertEquals("" + expected, testServer.decodePercent(input));
        }
    }

    @Test
    public void testMultibyteCharacterSupport() throws Exception {
        String expected = "Chinese \u738b Letters";
        String input = "Chinese+%e7%8e%8b+Letters";
        assertEquals(expected, testServer.decodePercent(input));
    }

    @Test
    public void testPlusInQueryParams() throws Exception {
        assertEquals("foo bar", testServer.decodePercent("foo+bar"));
    }
}
