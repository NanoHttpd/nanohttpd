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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoWSD.WebSocketFrame;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketResponseHandlerTest {

    @Mock
    private IHTTPSession session;

    private NanoWSD nanoWebSocketServer;

    private Map<String, String> headers;

    private static class MockedWSD extends NanoWSD {

        public MockedWSD(int port) {
            super(port);
        }

        public MockedWSD(String hostname, int port) {
            super(hostname, port);
        }

        @Override
        protected WebSocket openWebSocket(IHTTPSession handshake) {
            return new WebSocket(handshake) { // Dummy websocket inner class.

                @Override
                protected void onPong(WebSocketFrame pong) {
                }

                @Override
                protected void onOpen() {
                }

                @Override
                protected void onMessage(WebSocketFrame message) {
                }

                @Override
                protected void onException(IOException exception) {
                }

                @Override
                protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
                }
            };
        }
    }

    @Before
    public void setUp() {
        this.nanoWebSocketServer = Mockito.mock(MockedWSD.class, Mockito.CALLS_REAL_METHODS);

        this.headers = new HashMap<String, String>();
        this.headers.put("upgrade", "websocket");
        this.headers.put("connection", "Upgrade");
        this.headers.put("sec-websocket-key", "x3JJHMbDL1EzLkh9GBhXDw==");
        this.headers.put("sec-websocket-protocol", "chat, superchat");
        this.headers.put("sec-websocket-version", "13");

        when(this.session.getHeaders()).thenReturn(this.headers);
    }

    @Test
    public void testConnectionHeaderHandlesKeepAlive_FixingFirefoxConnectIssue() {
        this.headers.put("connection", "keep-alive, Upgrade");
        Response handshakeResponse = this.nanoWebSocketServer.serve(this.session);

        assertNotNull(handshakeResponse);
    }

    @Test
    public void testHandshakeReturnsResponseWithExpectedHeaders() {
        Response handshakeResponse = this.nanoWebSocketServer.serve(this.session);

        assertNotNull(handshakeResponse);

        assertEquals(handshakeResponse.getHeader(NanoWSD.HEADER_WEBSOCKET_ACCEPT), "HSmrc0sMlYUkAGmm5OPpG2HaGWk=");
        assertEquals(handshakeResponse.getHeader(NanoWSD.HEADER_WEBSOCKET_PROTOCOL), "chat");
    }

    @Test
    public void testMissingKeyReturnsErrorResponse() {
        this.headers.remove("sec-websocket-key");

        Response handshakeResponse = this.nanoWebSocketServer.serve(this.session);

        assertNotNull(handshakeResponse);
        assertEquals(Response.Status.BAD_REQUEST, handshakeResponse.getStatus());
    }

    @Test
    public void testWrongConnectionHeaderReturnsNullResponse() {
        this.headers.put("connection", "Junk");
        Response handshakeResponse = this.nanoWebSocketServer.serve(this.session);
        assertNull(handshakeResponse.getHeader(NanoWSD.HEADER_UPGRADE));
    }

    @Test
    public void testWrongUpgradeHeaderReturnsNullResponse() {
        this.headers.put("upgrade", "not a websocket");
        Response handshakeResponse = this.nanoWebSocketServer.serve(this.session);
        assertNull(handshakeResponse.getHeader(NanoWSD.HEADER_UPGRADE));
    }

    @Test
    public void testWrongWebsocketVersionReturnsErrorResponse() {
        this.headers.put("sec-websocket-version", "12");

        Response handshakeResponse = this.nanoWebSocketServer.serve(this.session);

        assertNotNull(handshakeResponse);
        assertEquals(Response.Status.BAD_REQUEST, handshakeResponse.getStatus());
    }
}
