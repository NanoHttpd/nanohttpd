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

/**
 * Created by vnnv on 7/17/15.
 * Simple httpd server based on NanoHTTPD
 * Read the source. Everything is there.
 */

import fi.iki.elonen.ServerRunner;

import java.io.IOException;

public class App extends RouterNanoHTTPD {

	private static final int PORT = 8081;

	/**
	 Create the server instance
	 */
	public App() throws IOException {
		super(PORT);
		addMappings();
		System.out.println("\nRunning! Point your browers to http://localhost:" + PORT + "/ \n");
	}

	/**
	 * Add the routes
	 * Every route is an absolute path
	 * Parameters starts with ":"
	 * Handler class should implement @UriResponder interface
	 * If the handler not implement UriResponder interface - toString() is used
	 */
	@Override
	public void addMappings() {
		super.addMappings();
		addRoute("/user", UserHandler.class);
		addRoute("/user/:id", UserHandler.class);
		addRoute("/user/help", GeneralHandler.class);
		addRoute("/photos/:customer_id/:photo_id", null);
		addRoute("/test", String.class);
	}

	/**
	 * Main entry point
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ServerRunner.run(App.class);
		} catch (Exception ioe) {
			System.err.println("Couldn't start server:\n" + ioe);
		}
	}
}