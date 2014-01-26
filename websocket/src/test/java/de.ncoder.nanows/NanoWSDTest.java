package de.ncoder.nanows;

import java.io.IOException;

public class NanoWSDTest {
    public static void main(String[] args) throws IOException {
        final boolean DEBUG = args.length >= 2 && args[1].toLowerCase().equals("-d");
        NanoWSD ws = new NanoWSD(Integer.parseInt(args[0])) {
            @Override
            protected WebSocket openWebSocket(IHTTPSession handshake) {
                return new WebSocket(handshake) {
                    @Override
                    protected void onPong(WebSocketFrame pongFrame) {
                        //System.out.println("P " + pongFrame);
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

