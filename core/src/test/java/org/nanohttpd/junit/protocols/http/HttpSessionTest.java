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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;
import org.nanohttpd.protocols.http.HTTPSession;

public class HttpSessionTest extends HttpServerTest {

    private static final String DUMMY_REQUEST_CONTENT = "dummy request content";

    private static final TestTempFileManager TEST_TEMP_FILE_MANAGER = new TestTempFileManager();

    @Test
    public void testSessionRemoteHostnameLocalhost() throws UnknownHostException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(HttpSessionTest.DUMMY_REQUEST_CONTENT.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
        HTTPSession session = this.testServer.createSession(HttpSessionTest.TEST_TEMP_FILE_MANAGER, inputStream, outputStream, inetAddress);
        assertEquals("localhost", session.getRemoteHostName());
    }

    @Test
    public void testSessionRemoteHostname() throws UnknownHostException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(HttpSessionTest.DUMMY_REQUEST_CONTENT.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InetAddress inetAddress = InetAddress.getByName("google.com");
        HTTPSession session = this.testServer.createSession(HttpSessionTest.TEST_TEMP_FILE_MANAGER, inputStream, outputStream, inetAddress);
        assertEquals("google.com", session.getRemoteHostName());
    }

    @Test
    public void testSessionRemoteIPAddress() throws UnknownHostException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(HttpSessionTest.DUMMY_REQUEST_CONTENT.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
        HTTPSession session = this.testServer.createSession(HttpSessionTest.TEST_TEMP_FILE_MANAGER, inputStream, outputStream, inetAddress);
        assertEquals("127.0.0.1", session.getRemoteIpAddress());
    }
}
