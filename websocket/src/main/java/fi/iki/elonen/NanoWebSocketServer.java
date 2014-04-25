package fi.iki.elonen;


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

