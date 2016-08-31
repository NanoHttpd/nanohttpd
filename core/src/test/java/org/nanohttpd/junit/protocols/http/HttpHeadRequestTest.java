package org.nanohttpd.junit.protocols.http;

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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.Test;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;

public class HttpHeadRequestTest extends HttpServerTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String responseBody = "Success!";
        this.testServer.response = Response.newFixedLengthResponse(responseBody);
    }

    @Test
    public void testDecodingFieldWithEmptyValueAndFieldWithMissingValueGiveDifferentResults() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo&bar= HTTP/1.1");
        assertTrue(this.testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(0, this.testServer.decodedParamters.get("foo").size());
        assertTrue(this.testServer.decodedParamters.get("bar") instanceof List);
        assertEquals(1, this.testServer.decodedParamters.get("bar").size());
        assertEquals("", this.testServer.decodedParamters.get("bar").get(0));
    }

    @Test
    public void testDecodingMixtureOfParameters() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo=bar&foo=baz&zot&zim= HTTP/1.1");
        assertTrue(this.testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(2, this.testServer.decodedParamters.get("foo").size());
        assertEquals("bar", this.testServer.decodedParamters.get("foo").get(0));
        assertEquals("baz", this.testServer.decodedParamters.get("foo").get(1));
        assertTrue(this.testServer.decodedParamters.get("zot") instanceof List);
        assertEquals(0, this.testServer.decodedParamters.get("zot").size());
        assertTrue(this.testServer.decodedParamters.get("zim") instanceof List);
        assertEquals(1, this.testServer.decodedParamters.get("zim").size());
        assertEquals("", this.testServer.decodedParamters.get("zim").get(0));
    }

    @Test
    public void testDecodingParametersFromParameterMap() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo=bar&foo=baz&zot&zim= HTTP/1.1");
        assertEquals(this.testServer.decodedParamters, this.testServer.decodedParamtersFromParameter);
    }

    // --------------------------------------------------------------------------------------------------------
    // //

    @Test
    public void testDecodingParametersWithSingleValue() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo=bar&baz=zot HTTP/1.1");
        assertEquals("foo=bar&baz=zot", this.testServer.queryParameterString);
        assertTrue(this.testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(1, this.testServer.decodedParamters.get("foo").size());
        assertEquals("bar", this.testServer.decodedParamters.get("foo").get(0));
        assertTrue(this.testServer.decodedParamters.get("baz") instanceof List);
        assertEquals(1, this.testServer.decodedParamters.get("baz").size());
        assertEquals("zot", this.testServer.decodedParamters.get("baz").get(0));
    }

    @Test
    public void testDecodingParametersWithSingleValueAndMissingValue() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo&baz=zot HTTP/1.1");
        assertEquals("foo&baz=zot", this.testServer.queryParameterString);
        assertTrue(this.testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(0, this.testServer.decodedParamters.get("foo").size());
        assertTrue(this.testServer.decodedParamters.get("baz") instanceof List);
        assertEquals(1, this.testServer.decodedParamters.get("baz").size());
        assertEquals("zot", this.testServer.decodedParamters.get("baz").get(0));
    }

    @Test
    public void testDecodingSingleFieldRepeated() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo=bar&foo=baz HTTP/1.1");
        assertTrue(this.testServer.decodedParamters.get("foo") instanceof List);
        assertEquals(2, this.testServer.decodedParamters.get("foo").size());
        assertEquals("bar", this.testServer.decodedParamters.get("foo").get(0));
        assertEquals("baz", this.testServer.decodedParamters.get("foo").get(1));
    }

    @Test
    public void testEmptyHeadersSuppliedToServeMethodFromSimpleWorkingGetRequest() {
        invokeServer("HEAD " + HttpServerTest.URI + " HTTP/1.1");
        assertNotNull(this.testServer.parms);
        assertNotNull(this.testServer.parameters);
        assertNotNull(this.testServer.header);
        assertNotNull(this.testServer.files);
        assertNotNull(this.testServer.uri);
    }

    @Test
    public void testHeadRequestDoesntSendBackResponseBody() throws Exception {
        ByteArrayOutputStream outputStream = invokeServer("HEAD " + HttpServerTest.URI + " HTTP/1.1");

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
    public void testMultipleGetParameters() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo=bar&baz=zot HTTP/1.1");
        assertEquals("bar", this.testServer.parms.get("foo"));
        assertEquals("zot", this.testServer.parms.get("baz"));
        assertEquals("bar", this.testServer.parameters.get("foo").get(0));
        assertEquals("zot", this.testServer.parameters.get("baz").get(0));
    }

    @Test
    public void testMultipleGetParametersWithMissingValue() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo=&baz=zot HTTP/1.1");
        assertEquals("", this.testServer.parms.get("foo"));
        assertEquals("zot", this.testServer.parms.get("baz"));
        assertEquals("", this.testServer.parameters.get("foo").get(0));
        assertEquals("zot", this.testServer.parameters.get("baz").get(0));
    }

    @Test
    public void testMultipleGetParametersWithMissingValueAndRequestHeaders() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo=&baz=zot HTTP/1.1\nAccept: text/html");
        assertEquals("", this.testServer.parms.get("foo"));
        assertEquals("zot", this.testServer.parms.get("baz"));
        assertEquals("", this.testServer.parameters.get("foo").get(0));
        assertEquals("zot", this.testServer.parameters.get("baz").get(0));
        assertEquals("text/html", this.testServer.header.get("accept"));
    }

    @Test
    public void testMultipleHeaderSuppliedToServeMethodFromSimpleWorkingGetRequest() {
        String userAgent = "jUnit 4.8.2 Unit Test";
        String accept = "text/html";
        invokeServer("HEAD " + HttpServerTest.URI + " HTTP/1.1\nUser-Agent: " + userAgent + "\nAccept: " + accept);
        assertEquals(userAgent, this.testServer.header.get("user-agent"));
        assertEquals(accept, this.testServer.header.get("accept"));
    }

    @Test
    public void testSingleGetParameter() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo=bar HTTP/1.1");
        assertEquals("bar", this.testServer.parms.get("foo"));
        assertEquals("bar", this.testServer.parameters.get("foo").get(0));
    }

    @Test
    public void testMultipleValueGetParameter() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo=bar&foo=baz HTTP/1.1");
        assertEquals("bar", this.testServer.parms.get("foo"));
        assertEquals(2, this.testServer.parameters.get("foo").size());
        assertEquals("bar", this.testServer.parameters.get("foo").get(0));
        assertEquals("baz", this.testServer.parameters.get("foo").get(1));
    }

    @Test
    public void testSingleGetParameterWithNoValue() {
        invokeServer("HEAD " + HttpServerTest.URI + "?foo HTTP/1.1");
        assertEquals("", this.testServer.parms.get("foo"));
        assertEquals("", this.testServer.parameters.get("foo").get(0));
    }

    @Test
    public void testSingleUserAgentHeaderSuppliedToServeMethodFromSimpleWorkingGetRequest() {
        String userAgent = "jUnit 4.8.2 Unit Test";
        invokeServer("HEAD " + HttpServerTest.URI + " HTTP/1.1\nUser-Agent: " + userAgent + "\n");
        assertEquals(userAgent, this.testServer.header.get("user-agent"));
        assertEquals(Method.HEAD, this.testServer.method);
        assertEquals(HttpServerTest.URI, this.testServer.uri);
    }

}
