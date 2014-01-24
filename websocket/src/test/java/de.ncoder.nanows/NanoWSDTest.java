package de.ncoder.nanows;

import java.io.IOException;

public class NanoWSDTest {
    public static void main(String[] args) throws IOException {
        NanoWSD ws = new NanoWSD(Integer.parseInt(args[0])) {
            @Override
            protected WebSocket openWebSocket(IHTTPSession handshake) {
                return new WebSocket(handshake) {
                    @Override
                    protected void onPong(WebSocketFrame pongFrame) {
                        //System.out.println("PONG");
                    }

                    @Override
                    protected void onMessage(WebSocketFrame messageFrame) {
                        //String text = messageFrame.getTextPayload();
                        //if (text.length() > 100) {
                        //text = text.substring(0, 100);
                        //}
                        //System.out.println(messageFrame.getOpCode() + ": " + text);
                        try {
                            messageFrame.setUnmasked();
                            sendFrame(messageFrame);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
                        //System.out.println("Closed ");
                        if (code != WebSocketFrame.CloseCode.NormalClosure && initiatedByRemote) {
                            System.err.println("code = [" + code + "], reason = [" + reason + "], initiatedByRemote = [" + initiatedByRemote + "]");
                        }
                    }

                    @Override
                    protected void onException(IOException e) {
                        e.printStackTrace(System.err);
                    }
                };
            }
        };

        ws.start();
        System.out.println("Server started, Hit Enter to stop.\n");
        try {
            System.in.read();
        } catch (IOException ignored) {
        }
        ws.stop();
        System.out.println("Server stopped.\n");
    }
}

