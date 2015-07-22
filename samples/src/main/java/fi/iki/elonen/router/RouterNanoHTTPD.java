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

import java.util.Map;

/**
 * Created by vnnv on 7/21/15.
 */
public class RouterNanoHTTPD extends NanoHTTPD {

	/**
	 * Handling error 404 - unrecognized urls
	 */
	public static class Error404UriHandler extends DefaultHandler {

		public String getText() {
			String res = "<html><body><h3>Error 404: "
					+ "the requested page doesn't exist.</h3></body></html>";
			return res;
		}

		@Override
		public String getMimeType() {
			return "text/html";
		}

		@Override
		public Response.IStatus getStatus() {
			return Response.Status.NOT_FOUND;
		}

	}

	/**
	 * Handling index
	 */
	public static class IndexHandler extends DefaultHandler {

		public String getText() {
			String res = "<html><body><h2>Hello world!</h3></body></html>";
			return res;
		}

		@Override
		public String getMimeType() {
			return "text/html";
		}

		@Override
		public Response.IStatus getStatus() {
			return Response.Status.OK;
		}

	}

	public static class NotImplementedHandler extends DefaultHandler {

		public String getText() {
			String res = "<html><body><h2>The uri is mapped in the router, "
					+ "but no handler is specified. <br> "
					+ "Status: Not implemented!</h3></body></html>";
			return res;
		}

		@Override
		public String getMimeType() {
			return "text/html";
		}

		@Override
		public Response.IStatus getStatus() {
			return Response.Status.OK;
		}

	}


	private UriRouter router;

	public RouterNanoHTTPD(int port) {
		super(port);
		router = new UriRouter();
	}

	public void addMappings() {
		router.setNotImplemented(NotImplementedHandler.class);
		router.setNotFoundHandler(Error404UriHandler.class);
//		router.setNotFoundHandler(GeneralHandler.class); // You can use this instead of Error404UriHandler
		router.addRoute("/", IndexHandler.class);
		router.addRoute("/index.html", IndexHandler.class);
	}

	public void addRoute(String url, Class<?> handler) {
		router.addRoute(url, handler);
	}

	public void removeRoute(String url) {
		router.removeRoute(url);
	}

	@Override
	public Response serve(IHTTPSession session) {
		// Try to find match
		UriResource uriResource = router.matchUrl(session.getUri());
		// Extract uri parameters
		Map<String, String> urlParams = router.getUrlParams(uriResource, session.getUri());
		// Process the uri
		RouterResponse result = uriResource.process(urlParams, session);
		// Return the response
		return newFixedLengthResponse(result.getStatus(), result.getMimeType(), result.getData(), result.getSize());
	}
}
