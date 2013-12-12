package fi.iki.elonen;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static junit.framework.Assert.*;

public class HttpHeadRequestTest extends HttpServerTest {
    @Override
    public void setUp() {
        super.setUp();
        String responseBody = "Success!";
        testServer.response = new NanoHTTPD.Response(responseBody);
    }

    @Test
    public void testHeadRequestDoesntSendBackResponseBody() throws Exception {
        ByteArrayOutputStream outputStream = invokeServer("HEAD " + URI + " HTTP/1.1");

        String[] expected = {
                "HTTP/1.1 200 OK",
                "Content-Type: text/html",
                "Date: .*",
                "Connection: keep-alive",
                "Content-Length: 8",
                ""
        };

        assertResponse(outputStream, expected);
    }

    @Test
    public void testEmptyHeadersSuppliedToServeMethodFromSimpleWorkingGetRequest() {
        invokeServer("HEAD " + URI + " HTTP/1.1");
        assertNotNull(testServer.parms);
        assertNotNull(testServer.header);
        assertNotNull(testServer.files);
        assertNotNull(testServer.uri);
    }

    @Test
    public void testSingleUserAgentHeaderSuppliedToServeMethodFromSimpleWorkingGetRequest() {
        String userAgent = "jUnit 4.8.2 Unit Test";
        invokeServer("HEAD " + URI + " HTTP/1.1\nUser-Agent: " + userAgent + "\n");
        assertEquals(userAgent, testServer.header.get("user-agent"));
        assertEquals(NanoHTTPD.Method.HEAD, testServer.method);
        assertEquals(URI, testServer.uri);
    }

    @Test
    public void testMultipleHeaderSuppliedToServeMethodFromSimpleWorkingGetRequest() {
        String userAgent = "jUnit 4.8.2 Unit Test";
        String accept = "text/html";
        invokeServer("HEAD " + URI + " HTTP/1.1\nUser-Agent: " + userAgent + "\nAccept: " + accept);
        assertEquals(userAgent, testServer.header.get("user-agent"));
        assertEquals(accept, testServer.header.get("accept"));
    }

    @Test
    public void testSingleGetParameter() {
        invokeServer("HEAD " + URI + "?foo=bar HTTP/1.1");
        assertEquals("bar", testServer.parms.get("foo"));
    }

    @Test
    public void testSingleGetParameterWithNoValue() {
        invokeServer("HEAD " + URI + "?foo HTTP/1.1");
        assertEquals("", testServer.parms.get("foo"));
    }

    @Test
    public void testMultipleGetParameters() {
        invokeServer("HEAD " + URI + "?foo=bar&baz=zot HTTP/1.1");
        assertEquals("bar", testServer.parms.get("foo"));
        assertEquals("zot", testServer.parms.get("baz"));
    }

    @Test
    public void testMultipleGetParametersWithMissingValue() {
        invokeServer("HEAD " + URI + "?foo=&baz=zot HTTP/1.1");
        assertEquals("", testServer.parms.get("foo"));
        assertEquals("zot", testServer.parms.get("baz"));
    }

    @Test
    public void testMultipleGetParametersWithMissingValueAndRequestHeaders() {
        invokeServer("HEAD " + URI + "?foo=&baz=zot HTTP/1.1\nAccept: text/html");
        assertEquals("", testServer.parms.get("foo"));
        assertEquals("zot", testServer.parms.get("baz"));
        assertEquals("text/html", testServer.header.get("accept"));
    }

    @Test
    public void testDecodingParametersWithSingleValue() {
        invokeServer("HEAD " + URI + "?foo=bar&baz=zot HTTP/1.1");
        assertEquals("foo=bar&baz=zot", testServer.queryParameterString);
        assertTrue(testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(1, testServer.decodedParamters.get("foo").size());
        assertEquals("bar", testServer.decodedParamters.get("foo").get(0));
        assertTrue(testServer.decodedParamters.get("baz") instanceof List);
        assertEquals(1, testServer.decodedParamters.get("baz").size());
        assertEquals("zot", testServer.decodedParamters.get("baz").get(0));
    }

    @Test
    public void testDecodingParametersWithSingleValueAndMissingValue() {
        invokeServer("HEAD " + URI + "?foo&baz=zot HTTP/1.1");
        assertEquals("foo&baz=zot", testServer.queryParameterString);
        assertTrue(testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(0, testServer.decodedParamters.get("foo").size());
        assertTrue(testServer.decodedParamters.get("baz") instanceof List);
        assertEquals(1, testServer.decodedParamters.get("baz").size());
        assertEquals("zot", testServer.decodedParamters.get("baz").get(0));
    }

    @Test
    public void testDecodingFieldWithEmptyValueAndFieldWithMissingValueGiveDifferentResults() {
        invokeServer("HEAD " + URI + "?foo&bar= HTTP/1.1");
        assertTrue(testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(0, testServer.decodedParamters.get("foo").size());
        assertTrue(testServer.decodedParamters.get("bar") instanceof List);
        assertEquals(1, testServer.decodedParamters.get("bar").size());
        assertEquals("", testServer.decodedParamters.get("bar").get(0));
    }

    @Test
    public void testDecodingSingleFieldRepeated() {
        invokeServer("HEAD " + URI + "?foo=bar&foo=baz HTTP/1.1");
        assertTrue(testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(2, testServer.decodedParamters.get("foo").size());
        assertEquals("bar", testServer.decodedParamters.get("foo").get(0));
        assertEquals("baz", testServer.decodedParamters.get("foo").get(1));
    }

    @Test
    public void testDecodingMixtureOfParameters() {
        invokeServer("HEAD " + URI + "?foo=bar&foo=baz&zot&zim= HTTP/1.1");
        assertTrue(testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(2, testServer.decodedParamters.get("foo").size());
        assertEquals("bar", testServer.decodedParamters.get("foo").get(0));
        assertEquals("baz", testServer.decodedParamters.get("foo").get(1));
        assertTrue(testServer.decodedParamters.get("zot") instanceof List);
        assertEquals(0, testServer.decodedParamters.get("zot").size());
        assertTrue(testServer.decodedParamters.get("zim") instanceof List);
        assertEquals(1, testServer.decodedParamters.get("zim").size());
        assertEquals("", testServer.decodedParamters.get("zim").get(0));
    }

    @Test
    public void testDecodingParametersFromParameterMap() {
        invokeServer("HEAD " + URI + "?foo=bar&foo=baz&zot&zim= HTTP/1.1");
        assertEquals(testServer.decodedParamters, testServer.decodedParamtersFromParameter);
    }
    // -------------------------------------------------------------------------------------------------------- //

}
