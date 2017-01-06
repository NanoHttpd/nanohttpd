package org.nanohttpd.junit.protocols.http;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2017 nanohttpd
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
import org.nanohttpd.protocols.http.HTTPSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HttpHeaderSizeTest extends HttpServerTest {

    @Test
    public void testURILongerThanHeaderBuffer() throws IOException {
        // Create a URL same size as the max buffer size which will exceed size
        // when we add the GET prefix
        char[] url = new char[HTTPSession.BUFSIZE];
        for (int i = 0; i < url.length; i++) {
            url[i] = 'a';
        }

        ByteArrayOutputStream outputStream = invokeServer("GET /" + new String(url));
        String[] expected = new String[]{
            "HTTP/1.1 414 URI Too Long"
        };
        assertResponse(outputStream, expected);
    }

    @Test
    public void testURISameSizeAsHeaderBuffer() throws IOException {
        String prefix = "GET /";
        char[] url = new char[HTTPSession.BUFSIZE - prefix.length()];
        for (int i = 0; i < url.length; i++) {
            url[i] = 'a';
        }

        ByteArrayOutputStream outputStream = invokeServer(prefix + new String(url));
        String[] expected = new String[]{
            "HTTP/1.1 200 OK"
        };
        assertResponse(outputStream, expected);
    }

    @Test
    public void testURISameSizeAsHeaderBufferWithNewLineRn() throws IOException {
        String prefix = "GET /";
        String newLineSuffix = "\r\n";
        char[] url = new char[HTTPSession.BUFSIZE - prefix.length() - newLineSuffix.length()];
        for (int i = 0; i < url.length; i++) {
            url[i] = 'a';
        }

        ByteArrayOutputStream outputStream = invokeServer(prefix + new String(url) + newLineSuffix);
        String[] expected = new String[]{
            "HTTP/1.1 200 OK"
        };
        assertResponse(outputStream, expected);
    }

    @Test
    public void testURISameSizeAsHeaderBufferWithNewLineN() throws IOException {
        String prefix = "GET /";
        String newLineSuffix = "\n";
        char[] url = new char[HTTPSession.BUFSIZE - prefix.length() - newLineSuffix.length()];
        for (int i = 0; i < url.length; i++) {
            url[i] = 'a';
        }

        ByteArrayOutputStream outputStream = invokeServer(prefix + new String(url) + newLineSuffix);
        String[] expected = new String[]{
            "HTTP/1.1 200 OK"
        };
        assertResponse(outputStream, expected);
    }

    @Test
    public void testURISmallerThanHeaderBuffer() throws IOException {
        String prefix = "GET /";
        char[] url = new char[HTTPSession.BUFSIZE / 2];
        for (int i = 0; i < url.length; i++) {
            url[i] = 'a';
        }

        ByteArrayOutputStream outputStream = invokeServer(prefix + new String(url));
        String[] expected = new String[]{
            "HTTP/1.1 200 OK"
        };
        assertResponse(outputStream, expected);
    }

}
