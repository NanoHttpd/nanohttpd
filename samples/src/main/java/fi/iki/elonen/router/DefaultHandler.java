package fi.iki.elonen.router;
/*
 * #%L
 * NanoHttpd-Samples
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

import fi.iki.elonen.NanoHTTPD;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * Created by vnnv on 7/21/15.
 *
 */
public abstract class DefaultHandler implements UriResponder {

	public abstract String getText();
	public abstract String getMimeType();
	public abstract NanoHTTPD.Response.IStatus getStatus();

	public RouterResponse get(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
		String text = getText();
		ByteArrayInputStream inp = new ByteArrayInputStream(text.getBytes());

		RouterResponse result = new RouterResponse();
		result.setData(inp);
		result.setMimeType(getMimeType());
		result.setSize(text.getBytes().length);
		result.setStatus(getStatus());

		return result;
	}

	public RouterResponse post(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
		return get(uriResource, urlParams, session);
	}

	public RouterResponse put(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
		return get(uriResource, urlParams, session);
	}

	public RouterResponse delete(UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
		return get(uriResource, urlParams, session);
	}
	public RouterResponse other(String method, UriResource uriResource, Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
		return get(uriResource, urlParams, session);
	}

}
