package fi.iki.elonen;

import static junit.framework.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Test;

public class HttpKeepAliveTest extends HttpServerTest {

    @Test
    public void testManyGetRequests() throws Exception {
        String request = "GET " + URI + " HTTP/1.1\r\n\r\n";
        String[] expected = {
                "HTTP/1.1 200 OK",
                "Content-Type: text/html",
                "Date: .*",
                "Connection: keep-alive",
                "Content-Length: 0",
                ""
        };
        testManyRequests(request, expected);
    }
    
    @Test
    public void testManyPutRequests() throws Exception {
        String data = "BodyData 1\nLine 2";
        String request = "PUT " + URI + " HTTP/1.1\r\nContent-Length: " + data.length() + "\r\n\r\n" + data;
        String[] expected = {
                "HTTP/1.1 200 OK",
                "Content-Type: text/html",
                "Date: .*",
                "Connection: keep-alive",
                "Content-Length: 0",
                ""
        };
        testManyRequests(request, expected);
    }

    private Throwable error = null;
    
    /**
     * Issue the given request many times to check whether an error occurs.
     * For this test, a small stack size is used, since a stack overflow is among the possible errors.
     * @param request The request to issue
     * @param expected The expected response
     */
    public void testManyRequests(final String request, final String[] expected) throws Exception {
        Runnable r = new Runnable() {
            public void run() {
                try {
                    PipedOutputStream requestStream = new PipedOutputStream();
                    PipedInputStream inputStream = new PipedInputStream(requestStream);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    NanoHTTPD.HTTPSession session = testServer.createSession(new TestTempFileManager(), inputStream, outputStream);
                    for (int i = 0; i < 2048; i++) {
                        requestStream.write(request.getBytes());
                        requestStream.flush();
                        session.execute();
                        assertResponse(outputStream, expected);
                    }
                } catch (Throwable t) {
                    error = t;
                }
            }
        };
        Thread t = new Thread(null, r, "Request Thread", 1 << 17);
        t.start();
        t.join();
        if (error != null) {
            fail(""+error);
            error.printStackTrace();
        }
    }
}
