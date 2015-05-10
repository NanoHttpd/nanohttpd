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

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

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

@RunWith(MockitoJUnitRunner.class)
public class WebSocketResponseHandlerTest {

    @Mock
    private IHTTPSession session;

    private NanoWebSocketServer nanoWebSocketServer;

    private Map<String, String> headers;

    @Before
    public void setUp() {
        nanoWebSocketServer = Mockito.mock(NanoWebSocketServer.class, Mockito.CALLS_REAL_METHODS);

        headers = new HashMap<String, String>();
        headers.put("upgrade", "websocket");
        headers.put("connection", "Upgrade");
        headers.put("sec-websocket-key", "x3JJHMbDL1EzLkh9GBhXDw==");
        headers.put("sec-websocket-protocol", "chat, superchat");
        headers.put("sec-websocket-version", "13");

        when(session.getHeaders()).thenReturn(headers);
    }

    @Test
    public void testHandshakeReturnsResponseWithExpectedHeaders() {
        Response handshakeResponse = nanoWebSocketServer.serve(session);

        assertNotNull(handshakeResponse);

        assertEquals(handshakeResponse.getHeader(NanoWebSocketServer.HEADER_WEBSOCKET_ACCEPT), "HSmrc0sMlYUkAGmm5OPpG2HaGWk=");
        assertEquals(handshakeResponse.getHeader(NanoWebSocketServer.HEADER_WEBSOCKET_PROTOCOL), "chat");
    }

    @Test
    public void testWrongWebsocketVersionReturnsErrorResponse() {
        headers.put("sec-websocket-version", "12");

        Response handshakeResponse = nanoWebSocketServer.serve(session);

        assertNotNull(handshakeResponse);
        assertEquals(Response.Status.BAD_REQUEST, handshakeResponse.getStatus());
    }

    @Test
    public void testMissingKeyReturnsErrorResponse() {
        headers.remove("sec-websocket-key");

        Response handshakeResponse = nanoWebSocketServer.serve(session);

        assertNotNull(handshakeResponse);
        assertEquals(Response.Status.BAD_REQUEST, handshakeResponse.getStatus());
    }

    @Test
    public void testWrongUpgradeHeaderReturnsNullResponse() {
        headers.put("upgrade", "not a websocket");
        Response handshakeResponse = nanoWebSocketServer.serve(session);
        assertNull(handshakeResponse.getHeader(NanoWebSocketServer.HEADER_UPGRADE));
    }

    @Test
    public void testWrongConnectionHeaderReturnsNullResponse() {
        headers.put("connection", "Junk");
        Response handshakeResponse = nanoWebSocketServer.serve(session);
        assertNull(handshakeResponse.getHeader(NanoWebSocketServer.HEADER_UPGRADE));
    }

    @Test
    public void testConnectionHeaderHandlesKeepAlive_FixingFirefoxConnectIssue() {
        headers.put("connection", "keep-alive, Upgrade");
        Response handshakeResponse = nanoWebSocketServer.serve(session);

        assertNotNull(handshakeResponse);
    }
}
