package fi.iki.elonen;

import java.util.Map;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com)
 *         On: 3/8/13 at 9:44 PM
 */
public class DebugServer extends NanoHTTPD {
    /**
     * Constructs an HTTP server on given port.
     */
    public DebugServer() {
        super(8080);
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<head><title>Debug Server</title></head>");
        sb.append("<body>");
        sb.append("<h1>Response</h1>");
        sb.append("<p><blockquote><b>URI -</b> ").append(uri).append("<br />");
        sb.append("<b>Method -</b> ").append(method).append("</blockquote></p>");
        sb.append("<h3>Headers</h3><p><blockquote>").append(header).append("</blockquote></p>");
        sb.append("<h3>Parms</h3><p><blockquote>").append(parms).append("</blockquote></p>");
        sb.append("<h3>Files</h3><p><blockquote>").append(files).append("</blockquote></p>");
        sb.append("</body>");
        sb.append("</html>");
        return new Response(sb.toString());
    }

    public static void main(String[] args) {
        ServerRunner.run(DebugServer.class);
    }
}
