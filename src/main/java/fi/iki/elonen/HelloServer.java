package fi.iki.elonen;

import java.io.*;
import java.util.*;

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class HelloServer extends NanoHTTPD {
    private HelloServer() {
        super(8080, new File("."));
    }

    @Override
    public Response serve(String uri, METHOD method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
        System.out.println(method + " '" + uri + "' ");

        String msg = "<html><body><h1>Hello server</h1>\n";
        if (parms.get("username") == null)
            msg +=
                    "<form action='?' method='get'>\n" +
                            "  <p>Your name: <input type='text' name='username'></p>\n" +
                            "</form>\n";
        else
            msg += "<p>Hello, " + parms.get("username") + "!</p>";

        msg += "</body></html>\n";

        return new NanoHTTPD.Response(msg);
    }

    public static void main(String[] args) {
        HelloServer helloServer = new HelloServer();

        try {
            helloServer.start();
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }

        System.out.println("Listening on port 8080. Hit Enter to stop.\n");
        try {
            System.in.read();
        } catch (Throwable ignored) {
        }

        helloServer.stop();
    }
}
