package org.nanohttpd.junit.protocols.http;

import java.io.File;

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

import java.io.IOException;
import java.util.Arrays;

import javax.net.ssl.SSLServerSocket;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.sockets.SecureServerSocketFactory;

public class SSLServerSocketFactoryTest extends HttpServerTest {

    @Test
    public void testSSLConnection() throws ClientProtocolException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpTrace httphead = new HttpTrace("https://localhost:9043/index.html");
        HttpResponse response = httpclient.execute(httphead);
        HttpEntity entity = response.getEntity();
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());

        Assert.assertEquals(9043, this.testServer.getListeningPort());
        Assert.assertTrue(this.testServer.isAlive());
    }

    @Test
    public void testCreatePassesTheProtocolsToServerSocket() throws IOException {
        // first find the supported protocols
        SecureServerSocketFactory secureServerSocketFactory = new SecureServerSocketFactory(NanoHTTPD.makeSSLSocketFactory("/keystore.jks", "password".toCharArray()), null);
        SSLServerSocket socket = (SSLServerSocket) secureServerSocketFactory.create();
        String[] protocols = socket.getSupportedProtocols();

        // remove one element from supported protocols
        if (protocols.length > 0) {
            protocols = Arrays.copyOfRange(protocols, 0, protocols.length - 1);
        }

        // test
        secureServerSocketFactory = new SecureServerSocketFactory(NanoHTTPD.makeSSLSocketFactory("/keystore.jks", "password".toCharArray()), protocols);
        socket = (SSLServerSocket) secureServerSocketFactory.create();
        Assert.assertArrayEquals("Enabled protocols specified in the factory were not set to the socket.", protocols, socket.getEnabledProtocols());
    }

    @Before
    public void setUp() throws Exception {
        System.setProperty("javax.net.ssl.trustStore", new File("src/test/resources/keystore.jks").getAbsolutePath());
        this.testServer = new TestServer(9043);
        this.testServer.setServerSocketFactory(new SecureServerSocketFactory(NanoHTTPD.makeSSLSocketFactory("/keystore.jks", "password".toCharArray()), null));
        this.tempFileManager = new TestTempFileManager();
        this.testServer.start();
        try {
            long start = System.currentTimeMillis();
            Thread.sleep(100L);
            while (!this.testServer.wasStarted()) {
                Thread.sleep(100L);
                if (System.currentTimeMillis() - start > 2000) {
                    Assert.fail("could not start server");
                }
            }
        } catch (InterruptedException e) {
        }
    }

    @After
    public void tearDown() {
        this.testServer.stop();
    }
}
