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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nanohttpd.protocols.http.IConnection;

/**
 * This implementation of the interface actually works like a
 * per-connection singleton to make a compromise between splitting
 * the request from the connection and sparing Garbage Collection efforts.
 * 
 * @author LordFokas
 */
public class Request implements IRequest {
    private final IConnection connection;
    private final Map<String, List<String>> parameters;
    private final Map<String, String> headers;
    
    private Method method;
    private String resource;
    private String query;
    private CookieHandler cookies;

    public Request(IConnection connection){
		this.connection = connection;
		this.parameters = new HashMap<>();
		this.headers = new HashMap<>();
	}
    
    /** Called internally by the Connection to avoid GC recycling. */
    public void recycle() {
        parameters.clear();
        headers.clear();
    }
    
    /** Called internally to set the values for the next actual request. */
    public void setUp(){}

    @Override
    public IConnection getClientConnection() {
        return connection;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public URL getURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getResource() {
        return resource;
    }

    @Override
    public String getQueryString() {
        return query;
    }

    @Override
    public String getFirstParameter(String param) {
        return getParameter(param, 0);
    }

    @Override
    public String getParameter(String param, int index) {
        if(!parameters.containsKey(param))
        	return null;
        List<String> params = parameters.get(param);
        if(index >= params.size())
        	return null;
        return params.get(index);
    }

    @Override
    public Map<String, List<String>> getAllParameters() {
        return parameters;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public CookieHandler getCookies() {
        return cookies;
    }

}
