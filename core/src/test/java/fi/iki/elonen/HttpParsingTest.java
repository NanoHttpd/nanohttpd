package fi.iki.elonen;

import org.junit.Test;

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
}
