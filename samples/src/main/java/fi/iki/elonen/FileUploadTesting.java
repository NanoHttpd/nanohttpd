package fi.iki.elonen;

import java.io.IOException;
import java.util.Map;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com)
 *         On: 2/21/13 at 7:05 AM
 */
public class FileUploadTesting extends NanoHTTPD {
    public FileUploadTesting() {
        super(8080);
    }

    public static void main(String[] args) {
        FileUploadTesting server = new FileUploadTesting();

        try {
            server.start();
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }

        System.out.println("Server started, Hit Enter to stop.\n");

        try {
            System.in.read();
        } catch (Throwable ignored) {
        }

        server.stop();
        System.out.println("Server stopped.\n");
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>Request:</h3>");
        sb.append("<blockquote>");
        sb.append("<b>URI:</b>").append(uri).append("<br>");
        sb.append("<b>Method:</b>").append(method).append("<br>");
        sb.append("</blockquote>");
        sb.append("<h3>Headers:</h3>");
        sb.append("<blockquote>");
        sb.append(String.valueOf(header));
        sb.append("</blockquote>");
        sb.append("<h3>Parameters:</h3>");
        sb.append("<blockquote>");
        sb.append(String.valueOf(parms));
        sb.append("</blockquote>");
        sb.append("<h3>Files:</h3>");
        sb.append("<blockquote>");
        sb.append(String.valueOf(files));
        sb.append("</blockquote>");
        return new Response("<html><body>" + sb.toString() + "</body></html>");
    }
}
