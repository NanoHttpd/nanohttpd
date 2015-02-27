package fi.iki.elonen;

/*
 * #%L
 * NanoHttpd-Websocket
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


public class NanoWebSocketServer extends NanoHTTPD implements IWebSocketFactory {
    public static final String MISSING_FACTORY_MESSAGE = "You must either override this method or supply a WebSocketFactory in the constructor";

    private final WebSocketResponseHandler responseHandler;

    public NanoWebSocketServer(int port) {
        super(port);
        responseHandler = new WebSocketResponseHandler(this);
    }

    public NanoWebSocketServer(String hostname, int port) {
        super(hostname, port);
        responseHandler = new WebSocketResponseHandler(this);
    }

    public NanoWebSocketServer(int port, IWebSocketFactory webSocketFactory) {
        super(port);
        responseHandler = new WebSocketResponseHandler(webSocketFactory);
    }

    public NanoWebSocketServer(String hostname, int port, IWebSocketFactory webSocketFactory) {
        super(hostname, port);
        responseHandler = new WebSocketResponseHandler(webSocketFactory);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response candidate = responseHandler.serve(session);
        return candidate == null ? super.serve(session) : candidate;
    }

    public WebSocket openWebSocket(IHTTPSession handshake) {
        throw new Error(MISSING_FACTORY_MESSAGE);
    }
}

