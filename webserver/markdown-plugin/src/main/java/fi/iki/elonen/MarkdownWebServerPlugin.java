package fi.iki.elonen;

import org.pegdown.PegDownProcessor;

import java.io.*;
import java.util.Map;

import static fi.iki.elonen.NanoHTTPD.MIME_HTML;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com)
 *         On: 9/13/13 at 4:03 AM
 */
public class MarkdownWebServerPlugin implements WebServerPlugin {

    private final PegDownProcessor processor;

    public MarkdownWebServerPlugin() {
        processor = new PegDownProcessor();
    }

    @Override public void initialize(Map<String, String> commandLineOptions) {
    }

    @Override public boolean canServeUri(String uri, File rootDir) {
        File f = new File(rootDir, uri);
        return f.exists();
    }

    @Override
    public NanoHTTPD.Response serveFile(String uri, Map<String, String> headers, NanoHTTPD.IHTTPSession session, File file, String mimeType) {
        String markdownSource = readSource(file);
        return markdownSource == null ? null :
            new NanoHTTPD.Response(OK, MIME_HTML, processor.markdownToHtml(markdownSource));
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
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {}
        }
    }
}
