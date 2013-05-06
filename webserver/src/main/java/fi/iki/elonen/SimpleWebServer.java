package fi.iki.elonen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class SimpleWebServer extends NanoHTTPD {
    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {{
        put("css", "text/css");
        put("htm", "text/html");
        put("html", "text/html");
        put("xml", "text/xml");
        put("txt", "text/plain");
        put("asc", "text/plain");
        put("gif", "image/gif");
        put("jpg", "image/jpeg");
        put("jpeg", "image/jpeg");
        put("png", "image/png");
        put("mp3", "audio/mpeg");
        put("m3u", "audio/mpeg-url");
        put("mp4", "video/mp4");
        put("ogv", "video/ogg");
        put("flv", "video/x-flv");
        put("mov", "video/quicktime");
        put("swf", "application/x-shockwave-flash");
        put("js", "application/javascript");
        put("pdf", "application/pdf");
        put("doc", "application/msword");
        put("ogg", "application/x-ogg");
        put("zip", "application/octet-stream");
        put("exe", "application/octet-stream");
        put("class", "application/octet-stream");
    }};

    /**
     * The distribution licence
     */
    private static final String LICENCE =
            "Copyright (C) 2001,2005-2011 by Jarno Elonen <elonen@iki.fi>,\n"
                    + "(C) 2010 by Konstantinos Togias <info@ktogias.gr>\n"
                    + "and (C) 2012- by Paul S. Hawke\n"
                    + "\n"
                    + "Redistribution and use in source and binary forms, with or without\n"
                    + "modification, are permitted provided that the following conditions\n"
                    + "are met:\n"
                    + "\n"
                    + "Redistributions of source code must retain the above copyright notice,\n"
                    + "this list of conditions and the following disclaimer. Redistributions in\n"
                    + "binary form must reproduce the above copyright notice, this list of\n"
                    + "conditions and the following disclaimer in the documentation and/or other\n"
                    + "materials provided with the distribution. The name of the author may not\n"
                    + "be used to endorse or promote products derived from this software without\n"
                    + "specific prior written permission. \n"
                    + " \n"
                    + "THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR\n"
                    + "IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES\n"
                    + "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.\n"
                    + "IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,\n"
                    + "INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT\n"
                    + "NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n"
                    + "DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY\n"
                    + "THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n"
                    + "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"
                    + "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";

    private File rootDir;

    public SimpleWebServer(String host, int port, File wwwroot) {
        super(host, port);
        this.rootDir = wwwroot;
    }

    public File getRootDir() {
        return rootDir;
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20' instead of '+'.
     */
    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/"))
                newUri += "/";
            else if (tok.equals(" "))
                newUri += "%20";
            else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return newUri;
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI, ignores all headers and HTTP parameters.
     */
    public Response serveFile(String uri, Map<String, String> header, File homeDir) {
        Response res = null;

        // Make sure we won't die of an exception later
        if (!homeDir.isDirectory())
            res = new Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERRROR: serveFile(): given homeDir is not a directory.");

        if (res == null) {
            // Remove URL arguments
            uri = uri.trim().replace(File.separatorChar, '/');
            if (uri.indexOf('?') >= 0)
                uri = uri.substring(0, uri.indexOf('?'));

            // Prohibit getting out of current directory
            if (uri.startsWith("src/main") || uri.endsWith("src/main") || uri.contains("../"))
                res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");
        }

        File f = new File(homeDir, uri);
        if (res == null && !f.exists())
            res = new Response(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");

        // List the directory, if necessary
        if (res == null && f.isDirectory()) {
            // Browsers get confused without '/' after the
            // directory, send a redirect.
            if (!uri.endsWith("/")) {
                uri += "/";
                res = new Response(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri
                        + "</a></body></html>");
                res.addHeader("Location", uri);
            }

            if (res == null) {
                // First try index.html and index.htm
                if (new File(f, "index.html").exists())
                    f = new File(homeDir, uri + "/index.html");
                else if (new File(f, "index.htm").exists())
                    f = new File(homeDir, uri + "/index.htm");
                    // No index file, list the directory if it is readable
                else if (f.canRead()) {
                    String[] files = f.list();
                    String msg = "<html><body><h1>Directory " + uri + "</h1><br/>";

                    if (uri.length() > 1) {
                        String u = uri.substring(0, uri.length() - 1);
                        int slash = u.lastIndexOf('/');
                        if (slash >= 0 && slash < u.length())
                            msg += "<b><a href=\"" + uri.substring(0, slash + 1) + "\">..</a></b><br/>";
                    }

                    if (files != null) {
                        for (int i = 0; i < files.length; ++i) {
                            File curFile = new File(f, files[i]);
                            boolean dir = curFile.isDirectory();
                            if (dir) {
                                msg += "<b>";
                                files[i] += "/";
                            }

                            msg += "<a href=\"" + encodeUri(uri + files[i]) + "\">" + files[i] + "</a>";

                            // Show file size
                            if (curFile.isFile()) {
                                long len = curFile.length();
                                msg += " &nbsp;<font size=2>(";
                                if (len < 1024)
                                    msg += len + " bytes";
                                else if (len < 1024 * 1024)
                                    msg += len / 1024 + "." + (len % 1024 / 10 % 100) + " KB";
                                else
                                    msg += len / (1024 * 1024) + "." + len % (1024 * 1024) / 10 % 100 + " MB";

                                msg += ")</font>";
                            }
                            msg += "<br/>";
                            if (dir)
                                msg += "</b>";
                        }
                    }
                    msg += "</body></html>";
                    res = new Response(msg);
                } else {
                    res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: No directory listing.");
                }
            }
        }

        try {
            if (res == null) {
                // Get MIME type from file name extension, if possible
                String mime = null;
                int dot = f.getCanonicalPath().lastIndexOf('.');
                if (dot >= 0)
                    mime = MIME_TYPES.get(f.getCanonicalPath().substring(dot + 1).toLowerCase());
                if (mime == null)
                    mime = NanoHTTPD.MIME_DEFAULT_BINARY;

                // Calculate etag
                String etag = Integer.toHexString((f.getAbsolutePath() + f.lastModified() + "" + f.length()).hashCode());

                // Support (simple) skipping:
                long startFrom = 0;
                long endAt = -1;
                String range = header.get("range");
                if (range != null) {
                    if (range.startsWith("bytes=")) {
                        range = range.substring("bytes=".length());
                        int minus = range.indexOf('-');
                        try {
                            if (minus > 0) {
                                startFrom = Long.parseLong(range.substring(0, minus));
                                endAt = Long.parseLong(range.substring(minus + 1));
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                // Change return code and add Content-Range header when skipping is requested
                long fileLen = f.length();
                if (range != null && startFrom >= 0) {
                    if (startFrom >= fileLen) {
                        res = new Response(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                        res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                        res.addHeader("ETag", etag);
                    } else {
                        if (endAt < 0)
                            endAt = fileLen - 1;
                        long newLen = endAt - startFrom + 1;
                        if (newLen < 0)
                            newLen = 0;

                        final long dataLen = newLen;
                        FileInputStream fis = new FileInputStream(f) {
                            @Override
                            public int available() throws IOException {
                                return (int) dataLen;
                            }
                        };
                        fis.skip(startFrom);

                        res = new Response(Response.Status.PARTIAL_CONTENT, mime, fis);
                        res.addHeader("Content-Length", "" + dataLen);
                        res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                } else {
                    if (etag.equals(header.get("if-none-match")))
                        res = new Response(Response.Status.NOT_MODIFIED, mime, "");
                    else {
                        res = new Response(Response.Status.OK, mime, new FileInputStream(f));
                        res.addHeader("Content-Length", "" + fileLen);
                        res.addHeader("ETag", etag);
                    }
                }
            }
        } catch (IOException ioe) {
            res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }

        res.addHeader("Accept-Ranges", "bytes"); // Announce that the file server accepts partial content requestes
        return res;
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
        System.out.println(method + " '" + uri + "' ");

        Iterator<String> e = header.keySet().iterator();
        while (e.hasNext()) {
            String value = e.next();
            System.out.println("  HDR: '" + value + "' = '" + header.get(value) + "'");
        }
        e = parms.keySet().iterator();
        while (e.hasNext()) {
            String value = e.next();
            System.out.println("  PRM: '" + value + "' = '" + parms.get(value) + "'");
        }
        e = files.keySet().iterator();
        while (e.hasNext()) {
            String value = e.next();
            System.out.println("  UPLOADED: '" + value + "' = '" + files.get(value) + "'");
        }

        return serveFile(uri, header, getRootDir());
    }

    /**
     * Starts as a standalone file server and waits for Enter.
     */
    public static void main(String[] args) {
        System.out.println(
                "NanoHttpd 2.0: Command line options: [-h hostname] [-p port] [-d root-dir] [--licence]\n" +
                        "(C) 2001,2005-2011 Jarno Elonen \n" +
                        "(C) 2010 Konstantinos Togias\n" +
                        "(C) 2012- Paul S. Hawke\n");

        // Defaults
        int port = 8080;
        String host = "127.0.0.1";
        File wwwroot = new File(".").getAbsoluteFile();

        // Show licence if requested
        for (int i = 0; i < args.length; ++i)
            if (args[i].equalsIgnoreCase("-h"))
                host = args[i + 1];
            else if (args[i].equalsIgnoreCase("-p"))
                port = Integer.parseInt(args[i + 1]);
            else if (args[i].equalsIgnoreCase("-d"))
                wwwroot = new File(args[i + 1]).getAbsoluteFile();
            else if (args[i].toLowerCase().endsWith("licence")) {
                System.out.println(LICENCE + "\n");
                break;
            }

        ServerRunner.executeInstance(new SimpleWebServer(host, port, wwwroot));
    }
}
