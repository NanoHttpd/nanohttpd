package fi.iki.elonen;

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
