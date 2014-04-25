package fi.iki.elonen.samples.echo;

import fi.iki.elonen.NanoWebSocketServer;

import java.io.IOException;

public class EchoSocketSample {
    public static void main(String[] args) throws IOException {
        final boolean debugMode = args.length >= 2 && args[1].toLowerCase().equals("-d");
        NanoWebSocketServer ws = new DebugWebSocketServer(Integer.parseInt(args[0]), debugMode);
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

