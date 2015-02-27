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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketResponseHandlerTest {

    @Mock
    private IHTTPSession session;
    @Mock
    private WebSocket webSocket;
    @Mock
    private IWebSocketFactory webSocketFactory;
    @Mock
    private Response response;
    @Captor
    private ArgumentCaptor<String> headerNameCaptor;
    @Captor
    private ArgumentCaptor<String> headerCaptor;

    private Map<String, String> headers;

    private WebSocketResponseHandler responseHandler;

    @Before
    public void setUp() {
        headers = new HashMap<String, String>();
        headers.put("upgrade", "websocket");
        headers.put("connection", "Upgrade");
        headers.put("sec-websocket-key", "x3JJHMbDL1EzLkh9GBhXDw==");
        headers.put("sec-websocket-protocol", "chat, superchat");
        headers.put("sec-websocket-version", "13");

        when(session.getHeaders()).thenReturn(headers);
        when(webSocketFactory.openWebSocket(any(IHTTPSession.class))).thenReturn(webSocket);
        when(webSocket.getHandshakeResponse()).thenReturn(response);

        responseHandler = new WebSocketResponseHandler(webSocketFactory);
    }

    @Test
    public void testHandshakeReturnsResponseWithExpectedHeaders() {
        Response handshakeResponse = responseHandler.serve(session);

        verify(webSocket).getHandshakeResponse();
        assertNotNull(handshakeResponse);
        assertSame(response, handshakeResponse);

        verify(response, atLeast(1)).addHeader(headerNameCaptor.capture(), headerCaptor.capture());
        assertHeader(0, "sec-websocket-accept", "HSmrc0sMlYUkAGmm5OPpG2HaGWk=");
        assertHeader(1, "sec-websocket-protocol", "chat");
    }

    @Test
    public void testWrongWebsocketVersionReturnsErrorResponse() {
        headers.put("sec-websocket-version", "12");

        Response handshakeResponse = responseHandler.serve(session);

        assertNotNull(handshakeResponse);
        assertEquals(Response.Status.BAD_REQUEST, handshakeResponse.getStatus());
    }

    @Test
    public void testMissingKeyReturnsErrorResponse() {
        headers.remove("sec-websocket-key");

        Response handshakeResponse = responseHandler.serve(session);

        assertNotNull(handshakeResponse);
        assertEquals(Response.Status.BAD_REQUEST, handshakeResponse.getStatus());
    }

    @Test
    public void testWrongUpgradeHeaderReturnsNullResponse() {
        headers.put("upgrade", "not a websocket");
        Response handshakeResponse = responseHandler.serve(session);
        assertNull(handshakeResponse);
    }

    @Test
    public void testWrongConnectionHeaderReturnsNullResponse() {
        headers.put("connection", "Junk");
        Response handshakeResponse = responseHandler.serve(session);
        assertNull(handshakeResponse);
    }

    @Test
    public void testConnectionHeaderHandlesKeepAlive_FixingFirefoxConnectIssue() {
        headers.put("connection", "keep-alive, Upgrade");
        Response handshakeResponse = responseHandler.serve(session);

        verify(webSocket).getHandshakeResponse();
        assertNotNull(handshakeResponse);
        assertSame(response, handshakeResponse);
    }

    private void assertHeader(int index, String name, String value) {
        assertEquals(name, headerNameCaptor.getAllValues().get(index));
        assertEquals(value, headerCaptor.getAllValues().get(index));
    }
}
