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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nanohttpd.protocols.http.ClientHandler;
import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.nanohttpd.junit.protocols.http.ClientHandlerTest.SimpleServer.FIXED_RESPONSE;
import static org.nanohttpd.protocols.http.NanoHTTPD.safeClose;
import static org.nanohttpd.protocols.http.response.Response.newFixedLengthResponse;

public class ClientHandlerTest {

    private static final int SMALL_REQUEST_SIZE = 4096;

    private static final int LARGE_REQUEST_SIZE = HTTPSession.BUFSIZE + 128;

    private static final int SOCKET_TIMEOUT = 5000;

    private static final int TEST_PORT = 8321;

    private ServerSocket serverSocket;

    private Socket clientSocket;

    private PrintWriter writer;

    private OutputStream outputStream;

    static class SimpleServer extends NanoHTTPD {

        public static final String FIXED_RESPONSE = "Hello serverSocket";

        public SimpleServer() throws IOException {
            super(8320);
        }

        @Override
        public Response serve(IHTTPSession session) {
            return newFixedLengthResponse(FIXED_RESPONSE);
        }
    }

    @Before
    public void setUp() throws IOException {
        serverSocket = new ServerSocket(TEST_PORT);
        clientSocket = new Socket("localhost", TEST_PORT);
        outputStream = clientSocket.getOutputStream();
        writer = new PrintWriter(outputStream, true);
    }

    @After
    public void tearDown() {
        safeClose(writer);
        safeClose(outputStream);
        safeClose(serverSocket);
        safeClose(clientSocket);
    }

    private String handleRequest(int uriSize) throws IOException, InterruptedException {
        writer.print("GET ");
        for (int i = 0; i < uriSize; i++) {
            writer.print('a');
        }
        writer.print("\n\n");
        writer.flush();
        outputStream.flush();

        Socket acceptSocket = serverSocket.accept();
        acceptSocket.setSoTimeout(SOCKET_TIMEOUT);

        ClientHandler clientHandler = new ClientHandler(new SimpleServer(), acceptSocket.getInputStream(), acceptSocket);
        clientHandler.run();

        BufferedInputStream inputStream = new BufferedInputStream(clientSocket.getInputStream());
        byte[] buffer = new byte[1024];
        inputStream.read(buffer, 0, 1024);
        return new String(buffer);
    }

    @Test(timeout = 15000)
    public void smallRequestShouldNotTimeout() throws IOException, InterruptedException {
        String response = handleRequest(SMALL_REQUEST_SIZE);
        assertThat(response, containsString(FIXED_RESPONSE));
    }

    @Test(timeout = 15000)
    public void largeRequestShouldNotTimeout() throws IOException, InterruptedException {
        String response = handleRequest(LARGE_REQUEST_SIZE);
        assertThat(response, containsString(FIXED_RESPONSE));
    }

}
