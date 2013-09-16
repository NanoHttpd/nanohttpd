package fi.iki.elonen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;

import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;

public class HttpChunkedResponseTest extends HttpServerTest {
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
        testServer.response = new NanoHTTPD.Response(OK, "what/ever", pipedInputStream);
        testServer.response.setChunkedTransfer(true);

        ByteArrayOutputStream byteArrayOutputStream = invokeServer("GET / HTTP/1.0");

        assertResponse(byteArrayOutputStream, expected);
    }

    private static class ChunkedInputStream extends PipedInputStream {
        int chunk = 0;
        String[] chunks;

        private ChunkedInputStream(String[] chunks) {
            this.chunks = chunks;
        }

        @Override
        public synchronized int read(byte[] buffer) throws IOException {
            // Too implementation-linked, but...
            for (int i = 0; i < chunks[chunk].length(); ++i) {
                buffer[i] = (byte) chunks[chunk].charAt(i);
            }
            return chunks[chunk++].length();
        }
    }
}
