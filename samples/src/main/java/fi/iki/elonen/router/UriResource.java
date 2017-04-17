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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vnnv on 7/20/15.
 * Represent single uri
 */
public class UriResource {
	private boolean hasParameters;
	private int uriParamsCount;
	private String uri;
	private List<UriPart> uriParts;
	private Class handler;

	public UriResource(String uri, Class<?> handler) {
		this.hasParameters = false;
		this.handler = handler;
		uriParamsCount = 0;
		if (uri != null) {
			this.uri = StringUtils.normalizeUri(uri);
			parse();
		}
	}

	private void parse(){
		String[] parts = uri.split("/");
		uriParts = new ArrayList<UriPart>();
		for (String part : parts) {
			boolean isParam = part.startsWith(":");
			UriPart uriPart = null;
			if (isParam) {
				hasParameters = true;
				uriParamsCount++;
				uriPart = new UriPart(part.substring(1), true);
			}else{
				uriPart = new UriPart(part, false);
			}
			uriParts.add(uriPart);
		}

	}

	public RouterResponse process(Map<String, String> urlParams, NanoHTTPD.IHTTPSession session) {
		String error = "General error!";
		if (handler != null) {
			try {
				Object object = handler.newInstance();
				if (object instanceof UriResponder) {
					UriResponder responder = (UriResponder) object;
					switch (session.getMethod()) {
						case GET: return responder.get(this, urlParams, session);
						case POST: return responder.post(this, urlParams, session);
						case PUT: return responder.put(this, urlParams, session);
						case DELETE: return responder.delete(this, urlParams, session);
						default: return responder.other(session.getMethod().toString(), this, urlParams, session);
					}
				}else {
					// return toString()
					String text = "Return: " + handler.getCanonicalName() + ".toString() -> " + object.toString();
					RouterResponse res = new RouterResponse();
					res.setStatus(NanoHTTPD.Response.Status.OK);
					res.setMimeType("text/plain");
					res.setData(new ByteArrayInputStream(text.getBytes()));
					res.setSize(text.getBytes().length);
					return res;
				}
			} catch (InstantiationException e) {
				error = "Error: " + InstantiationException.class.getName() + " : " + e.getMessage();
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				error = "Error: " + IllegalAccessException.class.getName() + " : " + e.getMessage();
				e.printStackTrace();
			}
		}

		RouterResponse res = new RouterResponse();
		res.setStatus(NanoHTTPD.Response.Status.INTERNAL_ERROR);
		res.setMimeType("text/plain");
		res.setData(new ByteArrayInputStream(error.getBytes()));
		res.setSize(error.getBytes().length);
		return res;
	}


	@Override
	public String toString() {
		return "UrlResource{" +
				"hasParameters=" + hasParameters +
				", uriParamsCount=" + uriParamsCount +
				", uri='" + (uri != null ? "/" : "") + uri + '\'' +
				", urlParts=" + uriParts +
				'}';
	}

	public boolean hasParameters() {
		return hasParameters;
	}

	public String getUri() {
		return uri;
	}

	public List<UriPart> getUriParts() {
		return uriParts;
	}

	public int getUriParamsCount() {
		return uriParamsCount;
	}

}
