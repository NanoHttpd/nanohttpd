package fi.iki.express;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by James on 13/8/2015.
 */
public abstract class StaticPageRoute implements Router {


    private File directory;

    public StaticPageRoute(File dir) {
        this.directory = dir;

    }

    private static String[] getPathArray(String uri) {
        String array[] = uri.split("/");
        ArrayList<String> pathArray = new ArrayList<String>();

        for (String s : array) {
            if (s.length() > 0) pathArray.add(s);
        }

        return pathArray.toArray(new String[]{});

    }

    @Override
    public void setup() {

    }

    @Override
    public void tearDown() {

    }

    /***
     * Implement the various http methods as see needs.
     *
     * @param session
     * @return
     */
    @Override
    public NanoHTTPD.Response doGet(NanoHTTPD.IHTTPSession session) {
        String pathArray[] = getPathArray(session.getUri());
        if (pathArray[0].length() == 1 && pathArray[0].equalsIgnoreCase(getDefaultURIPath())) {
            File content = new File(directory, "index.html");
            if (!content.exists()) {
                content = new File(directory, "index.htm");
                if (!content.exists()) {
                    return new NanoExpress.IStatusResponse(NanoHTTPD.Response.Status.NOT_FOUND);
                }
            }
            if (content.isFile()) {
                try {
                    FileInputStream ins = new FileInputStream(content);
                    return new NanoExpress.Response(NanoHTTPD.Response.Status.OK, HTML_CONTENT_TYPE, new BufferedInputStream(ins), content.length());
                } catch (IOException ioe) {
                    return new NanoExpress.IStatusResponse(NanoHTTPD.Response.Status.REQUEST_TIMEOUT);
                }
            } else {
                return new NanoExpress.IStatusResponse(NanoHTTPD.Response.Status.NOT_FOUND);
            }
        }
        if (pathArray[0].equalsIgnoreCase(getDefaultURIPath())) {
            File content = directory;
            for (int i = 1; i < pathArray.length; i++) {
                content = new File(content, pathArray[i]);
                if (content.isFile() && content.exists()) {
                    try {
                        FileInputStream ins = new FileInputStream(content);
                        return new NanoExpress.Response(NanoHTTPD.Response.Status.OK, HTML_CONTENT_TYPE, new BufferedInputStream(ins), content.length());
                    } catch (IOException ioe) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ioe.printStackTrace(new PrintStream(baos));
                        if (NanoExpress.outputStackTrace) {
                            return new NanoExpress.Response(NanoExpress.Response.Status.INTERNAL_ERROR, TEXT_CONTENT_TYPE, new ByteArrayInputStream(baos.toByteArray()), baos.toByteArray().length);
                        } else {
                            return new NanoExpress.IStatusResponse(NanoExpress.Response.Status.INTERNAL_ERROR);
                        }
                    }
                }
            }
            return new NanoExpress.IStatusResponse(NanoHTTPD.Response.Status.NOT_FOUND);
        }
        return null;
    }
}
