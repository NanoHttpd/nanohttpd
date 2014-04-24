package fi.iki.elonen;

import java.io.IOException;

/**
* @author Paul S. Hawke (paul.hawke@gmail.com)
*         On: 4/23/14 at 10:34 PM
*/
class DebugWebSocket extends WebSocket {
    private final boolean DEBUG;

    public DebugWebSocket(NanoHTTPD.IHTTPSession handshake, boolean debug) {
        super(handshake);
        DEBUG = debug;
    }

    @Override
    protected void onPong(WebSocketFrame pongFrame) {
        if (DEBUG) {
            System.out.println("P " + pongFrame);
        }
    }

    @Override
    protected void onMessage(WebSocketFrame messageFrame) {
        try {
            messageFrame.setUnmasked();
            sendFrame(messageFrame);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        if (DEBUG) {
            System.out.println("C [" + (initiatedByRemote ? "Remote" : "Self") + "] " + (code != null ? code : "UnknownCloseCode[" + code + "]") + (reason != null && !reason.isEmpty() ? ": " + reason : ""));
        }
    }

    @Override
    protected void onException(IOException e) {
        e.printStackTrace();
    }

    @Override
    protected void handleWebsocketFrame(WebSocketFrame frame) throws IOException {
        if (DEBUG) {
            System.out.println("R " + frame);
        }
        super.handleWebsocketFrame(frame);
    }

    @Override
    public synchronized void sendFrame(WebSocketFrame frame) throws IOException {
        if (DEBUG) {
            System.out.println("S " + frame);
        }
        super.sendFrame(frame);
    }
}
