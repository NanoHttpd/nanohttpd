package fi.iki.elonen;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public interface IWebSocketFactory {
    WebSocket openWebSocket(IHTTPSession handshake);
}
