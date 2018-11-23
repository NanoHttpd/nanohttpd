package org.nanohttpd.protocols.http.request;

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

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.nanohttpd.protocols.http.IConnection;

public interface IRequest {

    /** @return the connection this request originated from */
    public IConnection getClientConnection();

    /** @return the request method (GET / POST / etc) */
    public Method getMethod();

    /**
     * @return the full URL of this request: protocol, address, port, resource +
     *         query string
     */
    public URL getURL();

    /** @return the path of the requested resource (file) */
    public String getResource();

    /** @return the raw query string */
    public String getQueryString();

    /**
     * Equivalent to calling getParameter(param, 0);
     * 
     * @return the first parameter with this name
     */
    public String getFirstParameter(String param);

    /**
     * @return the parameter at this index, from the set of parameters with this
     *         name
     */
    public String getParameter(String param, int index);

    /** @return the full data structure with all received parameters */
    public Map<String, List<String>> getAllParameters();

    /** @return the map with the received http headers */
    public Map<String, String> getHeaders();

    /**
     * @return the CookieHandler containing all the cookies received in this
     *         request
     */
    public CookieHandler getCookies();
}
