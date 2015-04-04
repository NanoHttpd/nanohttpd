package fi.iki.elonen.samples.echo;

import java.io.IOException;

import fi.iki.elonen.NanoWebSocketServer;

public class EchoSocketSample {
    public static void main(String[] args) throws IOException {
        final boolean debugMode = args.length >= 2 && args[1].toLowerCase().equals("-d");
        NanoWebSocketServer ws = new DebugWebSocketServer(args.length > 0 ? Integer.parseInt(args[0]) : 9090, debugMode);
        ws.start();
        System.out.println("Server started, hit Enter to stop.\n");
        try {
            System.in.read();
        } catch (IOException ignored) {
        }
        ws.stop();
        System.out.println("Server stopped.\n");
    }

}

