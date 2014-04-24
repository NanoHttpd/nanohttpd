package fi.iki.elonen;

/**
* @author Paul S. Hawke (paul.hawke@gmail.com)
*         On: 4/23/14 at 10:31 PM
*/
class DebugWebSocketServer extends NanoWebSocketServer {
    private final boolean debug;

    public DebugWebSocketServer(int port, boolean debug) {
        super(port);
        this.debug = debug;
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new DebugWebSocket(handshake, debug);
    }
}
