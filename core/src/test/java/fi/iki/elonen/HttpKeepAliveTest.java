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

import static junit.framework.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Test;

public class HttpKeepAliveTest extends HttpServerTest {

    private Throwable error = null;

    @Test
    public void testManyGetRequests() throws Exception {
        String request = "GET " + HttpServerTest.URI + " HTTP/1.1\r\n\r\n";
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
        String request = "PUT " + HttpServerTest.URI + " HTTP/1.1\r\nContent-Length: " + data.length() + "\r\n\r\n" + data;
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

    /**
     * Issue the given request many times to check whether an error occurs. For
     * this test, a small stack size is used, since a stack overflow is among
     * the possible errors.
     * 
     * @param request
     *            The request to issue
     * @param expected
     *            The expected response
     */
    public void testManyRequests(final String request, final String[] expected) throws Exception {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    PipedOutputStream requestStream = new PipedOutputStream();
                    PipedInputStream inputStream = new PipedInputStream(requestStream);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    NanoHTTPD.DefaultTempFileManager tempFileManager = new NanoHTTPD.DefaultTempFileManager();
                    try {
                        NanoHTTPD.HTTPSession session = HttpKeepAliveTest.this.testServer.createSession(tempFileManager, inputStream, outputStream);
                        for (int i = 0; i < 2048; i++) {
                            requestStream.write(request.getBytes());
                            requestStream.flush();
                            outputStream.reset();
                            session.execute();
                            assertResponse(outputStream, expected);
                        }

                        // Finally, try "Connection: Close"
                        String closeReq = request.replaceAll("HTTP/1.1", "HTTP/1.1\r\nConnection: Close");
                        expected[3] = "Connection: close";
                        requestStream.write(closeReq.getBytes());
                        outputStream.reset();
                        requestStream.flush();
                        // Server should now close the socket by throwing a
                        // SocketException:
                        try {
                            session.execute();
                        } catch (java.net.SocketException se) {
                            junit.framework.Assert.assertEquals(se.getMessage(), "NanoHttpd Shutdown");
                        }
                        assertResponse(outputStream, expected);

                    } finally {
                        tempFileManager.clear();
                    }
                } catch (Throwable t) {
                    HttpKeepAliveTest.this.error = t;
                }
            }
        };
        Thread t = new Thread(null, r, "Request Thread", 1 << 17);
        t.start();
        t.join();
        if (this.error != null) {
            fail("" + this.error);
            this.error.printStackTrace();
        }
    }
}
