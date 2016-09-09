package org.nanohttpd.junit.protocols.http;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2016 nanohttpd
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
import java.io.IOException;
import java.io.PipedInputStream;

import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

public class HttpChunkedResponseTest extends HttpServerTest {

    private static class ChunkedInputStream extends PipedInputStream {

        int chunk = 0;

        String[] chunks;

        private ChunkedInputStream(String[] chunks) {
            this.chunks = chunks;
        }

        @Override
        public synchronized int read(byte[] buffer, int off, int len) throws IOException {
            // Too implementation-linked, but...
            for (int i = 0; i < this.chunks[this.chunk].length(); ++i) {
                buffer[i] = (byte) this.chunks[this.chunk].charAt(i);
            }
            return this.chunks[this.chunk++].length();
        }
    }

    @org.junit.Test
    public void thatChunkedContentIsChunked() throws Exception {
        PipedInputStream pipedInputStream = new ChunkedInputStream(new String[]{
            "some",
            "thing which is longer than sixteen characters",
            "whee!",
            ""
        });
        String[] expected = {
            "HTTP/1.1 200 OK",
            "Content-Type: what/ever",
            "Date: .*",
            "Connection: keep-alive",
            "Transfer-Encoding: chunked",
            "",
            "4",
            "some",
            "2d",
            "thing which is longer than sixteen characters",
            "5",
            "whee!",
            "0",
            ""
        };
        this.testServer.response = Response.newChunkedResponse(Status.OK, "what/ever", pipedInputStream);
        this.testServer.response.setChunkedTransfer(true);

        ByteArrayOutputStream byteArrayOutputStream = invokeServer("GET / HTTP/1.1");

        assertResponse(byteArrayOutputStream, expected);
    }
}
