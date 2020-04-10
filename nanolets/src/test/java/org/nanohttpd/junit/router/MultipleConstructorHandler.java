package org.nanohttpd.junit.router;

/*
 * #%L
 * NanoHttpd-nano application server
 * %%
 * Copyright (C) 2012 - 2020 nanohttpd
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

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.IStatus;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.router.RouterNanoHTTPD;

import java.util.Map;

public class MultipleConstructorHandler extends RouterNanoHTTPD.DefaultHandler {

    String constructorParam = "none";

    public MultipleConstructorHandler() {
        super();
    }

    public MultipleConstructorHandler(String stringParam) {
        super();
        constructorParam = stringParam;
    }

    public MultipleConstructorHandler(Integer intParam) {
        super();
        constructorParam = Integer.toString(intParam);
    }

    public MultipleConstructorHandler(Boolean booleanParam) {
        super();
        constructorParam = Boolean.toString(booleanParam);
    }

    public MultipleConstructorHandler(String stringParam, Integer intParam, Boolean booleanParam) {
        super();
        constructorParam = stringParam + intParam.toString() + booleanParam.toString();
    }

    @Override
    public String getMimeType() {
        return null;
    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public IStatus getStatus() {
        return Status.OK;
    }

    public Response get(RouterNanoHTTPD.UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        return Response.newFixedLengthResponse(getStatus(), getMimeType(), constructorParam);
    }
}
