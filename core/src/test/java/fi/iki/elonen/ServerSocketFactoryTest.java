package fi.iki.elonen;

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
import java.net.ServerSocket;

import org.junit.Assert;
import org.junit.Test;

import fi.iki.elonen.HttpServerTest.TestServer;
import fi.iki.elonen.NanoHTTPD.SecureServerSocketFactory;

public class ServerSocketFactoryTest extends NanoHTTPD {

    public static final int PORT = 8192;

    public ServerSocketFactoryTest() {
        super(PORT);

        this.setServerSocketFactory(new TestFactory());
    }

    @Test
    public void isCustomServerSocketFactory() {
        System.out.println("CustomServerSocketFactory test");
        Assert.assertTrue(this.getServerSocketFactory() instanceof TestFactory);
    }

    @Test
    public void testCreateServerSocket() {
        System.out.println("CreateServerSocket test");
        ServerSocket ss = null;
        try {
            ss = this.getServerSocketFactory().create();
        } catch (IOException e) {
        }
        Assert.assertTrue(ss != null);
    }

    @Test
    public void testSSLServerSocketFail() {
        String[] protocols = {
            ""
        };
        System.setProperty("javax.net.ssl.trustStore", new File("src/test/resources/keystore.jks").getAbsolutePath());
        ServerSocketFactory ssFactory = new SecureServerSocketFactory(null, protocols);
        ServerSocket ss = null;
        try {
            ss = ssFactory.create();
        } catch (Exception e) {
        }
        Assert.assertTrue(ss == null);

    }

    private class TestFactory implements ServerSocketFactory {

        @Override
        public ServerSocket create() {
            try {
                return new ServerSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
