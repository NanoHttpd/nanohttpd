package fi.iki.elonen;

import java.io.File;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

/**
* @author Paul S. Hawke (paul.hawke@gmail.com)
*         On: 9/14/13 at 8:09 AM
*/
public interface WebServerPlugin {

    void initialize(Map<String, String> commandLineOptions);

    boolean canServeUri(String uri, File rootDir);

    NanoHTTPD.Response serveFile(String uri, Map<String, String> headers, IHTTPSession session, File file, String mimeType);
}
