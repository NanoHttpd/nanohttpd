package fi.iki.elonen;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public interface WebSocketFactory {
    WebSocket openWebSocket(IHTTPSession handshake);
}
