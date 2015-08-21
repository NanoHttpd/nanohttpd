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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.Test;

public class HttpDeleteRequestTest extends HttpServerTest {

    @Test
    public void testDeleteRequestThatDoesntSendBackResponseBody_EmptyString() throws Exception {
        this.testServer.response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NO_CONTENT, NanoHTTPD.MIME_HTML, "");

        ByteArrayOutputStream outputStream = invokeServer("DELETE " + HttpServerTest.URI + " HTTP/1.1");

        String[] expected = {
            "HTTP/1.1 204 No Content",
            "Content-Type: text/html",
            "Date: .*",
            "Connection: keep-alive",
            "Content-Length: 0",
            ""
        };

        assertResponse(outputStream, expected);
    }

    @Test
    public void testDeleteRequestThatDoesntSendBackResponseBody_NullInputStream() throws Exception {
        this.testServer.response = NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.NO_CONTENT, NanoHTTPD.MIME_HTML, (InputStream) null);

        ByteArrayOutputStream outputStream = invokeServer("DELETE " + HttpServerTest.URI + " HTTP/1.1");

        String[] expected = {
            "HTTP/1.1 204 No Content",
            "Content-Type: text/html",
            "Date: .*",
            "Connection: keep-alive",
            "Content-Length: 0",
            ""
        };

        assertResponse(outputStream, expected);
    }

    @Test
    public void testDeleteRequestThatDoesntSendBackResponseBody_NullString() throws Exception {
        this.testServer.response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NO_CONTENT, NanoHTTPD.MIME_HTML, (String) null);

        ByteArrayOutputStream outputStream = invokeServer("DELETE " + HttpServerTest.URI + " HTTP/1.1");

        String[] expected = {
            "HTTP/1.1 204 No Content",
            "Content-Type: text/html",
            "Date: .*",
            "Connection: keep-alive",
            "Content-Length: 0",
            ""
        };

        assertResponse(outputStream, expected);
    }

    @Test
    public void testDeleteRequestThatSendsBackResponseBody_Accepted() throws Exception {
        this.testServer.response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.ACCEPTED, "application/xml", "<body />");

        ByteArrayOutputStream outputStream = invokeServer("DELETE " + HttpServerTest.URI + " HTTP/1.1");

        String[] expected = {
            "HTTP/1.1 202 Accepted",
            "Content-Type: application/xml",
            "Date: .*",
            "Connection: keep-alive",
            "Content-Length: 8",
            "",
            "<body />"
        };

        assertResponse(outputStream, expected);
    }

    @Test
    public void testDeleteRequestThatSendsBackResponseBody_Success() throws Exception {
        this.testServer.response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/xml", "<body />");

        ByteArrayOutputStream outputStream = invokeServer("DELETE " + HttpServerTest.URI + " HTTP/1.1");

        String[] expected = {
            "HTTP/1.1 200 OK",
            "Content-Type: application/xml",
            "Date: .*",
            "Connection: keep-alive",
            "Content-Length: 8",
            "",
            "<body />"
        };

        assertResponse(outputStream, expected);
    }
}
