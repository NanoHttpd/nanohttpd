package fi.iki.elonen;

import java.io.IOException;

import org.junit.Test;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.WebSocketFrame.CloseCode;
import fi.iki.elonen.testutil.MockHttpSession;
import static junit.framework.Assert.*;

public class WebSocketResponseHandlerTest {
    private WebSocketResponseHandler responseHandler = new WebSocketResponseHandler(new DummyWebSocketFactory(
            new WebSocketAdapter(new MockHttpSession())));
    
    @Test
    public void testHandshake_returnsExpectedHeaders() {
        MockHttpSession session = createWebSocketHandshakeRequest();

        Response handshakeResponse = responseHandler.serve(session);
        
        assertNotNull(handshakeResponse);
        assertEquals(101, handshakeResponse.getStatus().getRequestStatus());
        assertEquals("101 Switching Protocols", handshakeResponse.getStatus().getDescription());
        assertEquals("websocket", handshakeResponse.getHeader("upgrade"));
        assertEquals("Upgrade", handshakeResponse.getHeader("connection"));
        assertEquals("HSmrc0sMlYUkAGmm5OPpG2HaGWk=", handshakeResponse.getHeader("sec-websocket-accept"));
        assertEquals("chat", handshakeResponse.getHeader("sec-websocket-protocol"));
    }
    
    @Test
    public void testWrongWebsocketVersion_returnsErrorResponse() {
        MockHttpSession session = createWebSocketHandshakeRequest();
        session.getHeaders().put("sec-websocket-version", "12");

        Response handshakeResponse = responseHandler.serve(session);
        
        assertNotNull(handshakeResponse);
        assertEquals(400, handshakeResponse.getStatus().getRequestStatus());
        assertEquals("400 Bad Request", handshakeResponse.getStatus().getDescription());
    }

    private MockHttpSession createWebSocketHandshakeRequest() {
        // Example headers copied from Wikipedia
        MockHttpSession session = new MockHttpSession();
        session.getHeaders().put("upgrade", "websocket");
        session.getHeaders().put("connection", "Upgrade");
        session.getHeaders().put("sec-websocket-key", "x3JJHMbDL1EzLkh9GBhXDw==");
        session.getHeaders().put("sec-websocket-protocol", "chat, superchat");
        session.getHeaders().put("sec-websocket-version", "13");
        return session;
    }
    
    private static class DummyWebSocketFactory implements WebSocketFactory {
        private final WebSocket webSocket;
        
        private DummyWebSocketFactory(WebSocket webSocket) {
            super();
            this.webSocket = webSocket;
        }

        @Override
        public WebSocket openWebSocket(IHTTPSession handshake) {
            return webSocket;
        }
    }
    
    private static class WebSocketAdapter extends WebSocket {

        public WebSocketAdapter(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onPong(WebSocketFrame pongFrame) {
            throw new Error("this method should not have been called");
        }

        @Override
        protected void onMessage(WebSocketFrame messageFrame) {
            throw new Error("this method should not have been called");
        }

        @Override
        protected void onClose(CloseCode code, String reason,
                boolean initiatedByRemote) {
            throw new Error("this method should not have been called");
        }

        @Override
        protected void onException(IOException e) {
            throw new Error("this method should not have been called");
        }
        
    }
}
