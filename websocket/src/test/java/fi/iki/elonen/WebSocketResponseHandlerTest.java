package fi.iki.elonen;

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
