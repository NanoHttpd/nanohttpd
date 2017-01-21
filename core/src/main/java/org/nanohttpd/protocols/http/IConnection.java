package org.nanohttpd.protocols.http;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IConnection {

    /**
     * Blocking I/O: suspends the thread until a new request arrives. The
     * received request will then be handled. This method is meant to be called
     * <b>exclusively</b> by the thread listening on the socket for requests
     * packets.
     */
    public void handleNextRequest() throws IOException;

    /** @return this socket's remote IP address */
    public String getRemoteIPAddress();

    /**
     * Caution: Calling this method may take some time as the hostname
     * resolution is a potentially lengthy process.
     * 
     * @return this client's hostname.
     */
    public String getRemoteHostname();

    /**
     * A more advanced set of functionality we really don't want to have the
     * average joe messing with. By design all connections are IConnectionIO but
     * are cast to IConnection masking out the streams which are meant only for
     * more advanced functionality like WebSockets and other derivative
     * protocols.<br>
     * <br>
     * <b>Using these streams in normal requests **WILL** have serious and
     * unintended consequences (protocol-wise). Use at your own risk.<br>
     * <br>
     * Consider yourselves warned.</b>
     * 
     * @author LordFokas
     */
    public static interface IConnectionIO extends IConnection {

        /** @return an input stream to read from this client. */
        public InputStream getInputStream();

        /** @return an output stream to write to this client. */
        public OutputStream getOutputStream();
    }
}
