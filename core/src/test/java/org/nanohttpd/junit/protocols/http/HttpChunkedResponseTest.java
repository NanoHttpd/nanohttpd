package org.nanohttpd.junit.protocols.http;

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
