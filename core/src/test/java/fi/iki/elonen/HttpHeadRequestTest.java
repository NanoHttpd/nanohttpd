package fi.iki.elonen;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

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
    // --------------------------------------------------------------------------------------------------------
    // //

}
