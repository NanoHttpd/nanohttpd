package org.nanohttpd.junit.protocols.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;

import javax.net.ssl.SSLServerSocketFactory;

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nanohttpd.protocols.http.NanoHTTPD;

public class LoadKeyStoreTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void loadKeyStoreFromResources() throws Exception {
        String keyStorePath = "/keystore.jks";
        InputStream resourceAsStream = this.getClass().getResourceAsStream(keyStorePath);
        assertNotNull(resourceAsStream);

        SSLServerSocketFactory sslServerSocketFactory = NanoHTTPD.makeSSLSocketFactory(keyStorePath, "password".toCharArray());
        assertNotNull(sslServerSocketFactory);
    }

    @Test
    public void loadKeyStoreFromResourcesWrongPassword() throws Exception {
        String keyStorePath = "/keystore.jks";
        InputStream resourceAsStream = this.getClass().getResourceAsStream(keyStorePath);
        assertNotNull(resourceAsStream);

        thrown.expect(IOException.class);
        NanoHTTPD.makeSSLSocketFactory(keyStorePath, "wrongpassword".toCharArray());
    }

    @Test
    public void loadNonExistentKeyStoreFromResources() throws Exception {
        String nonExistentPath = "/nokeystorehere.jks";
        InputStream resourceAsStream = this.getClass().getResourceAsStream(nonExistentPath);
        assertNull(resourceAsStream);

        thrown.expect(IOException.class);
        NanoHTTPD.makeSSLSocketFactory(nonExistentPath, "".toCharArray());
    }

}
