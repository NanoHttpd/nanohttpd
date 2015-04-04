package fi.iki.elonen.samples.echo;

import java.io.IOException;

import fi.iki.elonen.NanoWebSocketServer;

/**
* @author Paul S. Hawke (paul.hawke@gmail.com)
*         On: 4/23/14 at 10:31 PM
*/
public class DebugWebSocketServer extends NanoWebSocketServer {
    private final boolean debug;

    public DebugWebSocketServer(int port, boolean debug) {
        super(port);
        this.debug = debug;
    }

    @Override
    protected void onPong(WebSocket socket, WebSocketFrame pongFrame) {
        if (debug) {
            System.out.println("P " + pongFrame);
        }
    }

    @Override
    protected void onMessage(WebSocket socket, WebSocketFrame messageFrame) {
        try {
            messageFrame.setUnmasked();
            socket.sendFrame(messageFrame);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onClose(WebSocket socket, WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        if (debug) {
            System.out.println("C [" + (initiatedByRemote ? "Remote" : "Self") + "] " +
                    (code != null ? code : "UnknownCloseCode[" + code + "]") +
                    (reason != null && !reason.isEmpty() ? ": " + reason : ""));
        }
    }

    @Override
    protected void onException(WebSocket socket, IOException e) {
        e.printStackTrace();
    }

    @Override
    protected void onFrameReceived(WebSocketFrame frame) {
        if (debug) {
            System.out.println("R " + frame);
        }
    }

    @Override
    public void onSendFrame(WebSocketFrame frame) {
        if (debug) {
            System.out.println("S " + frame);
        }
    }
}
