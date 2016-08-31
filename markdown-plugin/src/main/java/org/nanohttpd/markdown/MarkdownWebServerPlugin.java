package org.nanohttpd.markdown;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.webserver.WebServerPlugin;
import org.pegdown.PegDownProcessor;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com) On: 9/13/13 at 4:03 AM
 */
public class MarkdownWebServerPlugin implements WebServerPlugin {

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(MarkdownWebServerPlugin.class.getName());

    private final PegDownProcessor processor;

    public MarkdownWebServerPlugin() {
        this.processor = new PegDownProcessor();
    }

    @Override
    public boolean canServeUri(String uri, File rootDir) {
        File f = new File(rootDir, uri);
        return f.exists();
    }

    @Override
    public void initialize(Map<String, String> commandLineOptions) {
    }

    private String readSource(File file) {
        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            fileReader = new FileReader(file);
            reader = new BufferedReader(fileReader);
            String line = null;
            StringBuilder sb = new StringBuilder();
            do {
                line = reader.readLine();
                if (line != null) {
                    sb.append(line).append("\n");
                }
            } while (line != null);
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            MarkdownWebServerPlugin.LOG.log(Level.SEVERE, "could not read source", e);
            return null;
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
                MarkdownWebServerPlugin.LOG.log(Level.FINEST, "close failed", ignored);
            }
        }
    }

    @Override
    public Response serveFile(String uri, Map<String, String> headers, IHTTPSession session, File file, String mimeType) {
        String markdownSource = readSource(file);
        byte[] bytes;
        try {
            bytes = this.processor.markdownToHtml(markdownSource).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            MarkdownWebServerPlugin.LOG.log(Level.SEVERE, "encoding problem, responding nothing", e);
            bytes = new byte[0];
        }
        return markdownSource == null ? null : Response.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, new ByteArrayInputStream(bytes), bytes.length);
    }
}
