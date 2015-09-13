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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import org.junit.Ignore;
import org.junit.Test;

public class HttpSessionHeadersTest extends HttpServerTest {

    private static final String DUMMY_REQUEST_CONTENT = "dummy request content";

    private static final TestTempFileManager TEST_TEMP_FILE_MANAGER = new TestTempFileManager();

    @Test
    @Ignore
    public void testHeadersRemoteIp() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(HttpSessionHeadersTest.DUMMY_REQUEST_CONTENT.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String[] ipAddresses = {
            "127.0.0.1",
            "192.168.1.1",
            "192.30.252.129"
        };
        for (String ipAddress : ipAddresses) {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            NanoHTTPD.HTTPSession session = this.testServer.createSession(HttpSessionHeadersTest.TEST_TEMP_FILE_MANAGER, inputStream, outputStream, inetAddress);
            assertEquals(ipAddress, session.getHeaders().get("remote-addr"));
            assertEquals(ipAddress, session.getHeaders().get("http-client-ip"));
        }
    }

}
