package fi.iki.elonen;

import java.io.File;
import java.util.Map;

/**
* @author Paul S. Hawke (paul.hawke@gmail.com)
*         On: 9/14/13 at 8:09 AM
*/
public interface WebServerPlugin {
    boolean canServeUri(String uri, File rootDir);

    NanoHTTPD.Response serveFile(String uri, Map<String, String> headers, File file, String mimeType);
}
