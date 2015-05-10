package fi.iki.elonen;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * A simple, tiny, nicely embeddable HTTP server in Java
 * <p/>
 * <p/>
 * NanoHTTPD
 * <p>
 * Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen,
 * 2010 by Konstantinos Togias
 * </p>
 * <p/>
 * <p/>
 * <b>Features + limitations: </b>
 * <ul>
 * <p/>
 * <li>Only one Java file</li>
 * <li>Java 5 compatible</li>
 * <li>Released as open source, Modified BSD licence</li>
 * <li>No fixed config files, logging, authorization etc. (Implement yourself if
 * you need them.)</li>
 * <li>Supports parameter parsing of GET and POST methods (+ rudimentary PUT
 * support in 1.25)</li>
 * <li>Supports both dynamic content and file serving</li>
 * <li>Supports file upload (since version 1.2, 2010)</li>
 * <li>Supports partial content (streaming)</li>
 * <li>Supports ETags</li>
 * <li>Never caches anything</li>
 * <li>Doesn't limit bandwidth, request time or simultaneous connections</li>
 * <li>Default code serves files and shows all HTTP parameters and headers</li>
 * <li>File server supports directory listing, index.html and index.htm</li>
 * <li>File server supports partial content (streaming)</li>
 * <li>File server supports ETags</li>
 * <li>File server does the 301 redirection trick for directories without '/'</li>
 * <li>File server supports simple skipping for files (continue download)</li>
 * <li>File server serves also very long files without memory overhead</li>
 * <li>Contains a built-in list of most common MIME types</li>
 * <li>All header names are converted to lower case so they don't vary between
 * browsers/clients</li>
 * <p/>
 * </ul>
 * <p/>
 * <p/>
 * <b>How to use: </b>
 * <ul>
 * <p/>
 * <li>Subclass and implement serve() and embed to your own program</li>
 * <p/>
 * </ul>
 * <p/>
 * See the separate "LICENSE.md" file for the distribution license (Modified BSD
 * licence)
 */
public abstract class NanoHTTPD {

    /**
     * Pluggable strategy for asynchronously executing requests.
     */
    public interface AsyncRunner {

        void exec(Runnable code);
    }

    public static class Cookie {

        public static String getHTTPTime(int days) {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            calendar.add(Calendar.DAY_OF_MONTH, days);
            return dateFormat.format(calendar.getTime());
        }

        private final String n, v, e;

        public Cookie(String name, String value) {
            this(name, value, 30);
        }

        public Cookie(String name, String value, int numDays) {
            this.n = name;
            this.v = value;
            this.e = getHTTPTime(numDays);
        }

        public Cookie(String name, String value, String expires) {
            this.n = name;
            this.v = value;
            this.e = expires;
        }

        public String getHTTPHeader() {
            String fmt = "%s=%s; expires=%s";
            return String.format(fmt, this.n, this.v, this.e);
        }
    }

    /**
     * Provides rudimentary support for cookies. Doesn't support 'path',
     * 'secure' nor 'httpOnly'. Feel free to improve it and/or add unsupported
     * features.
     * 
     * @author LordFokas
     */
    public class CookieHandler implements Iterable<String> {

        private final HashMap<String, String> cookies = new HashMap<String, String>();

        private final ArrayList<Cookie> queue = new ArrayList<Cookie>();

        public CookieHandler(Map<String, String> httpHeaders) {
            String raw = httpHeaders.get("cookie");
            if (raw != null) {
                String[] tokens = raw.split(";");
                for (String token : tokens) {
                    String[] data = token.trim().split("=");
                    if (data.length == 2) {
                        this.cookies.put(data[0], data[1]);
                    }
                }
            }
        }

        /**
         * Set a cookie with an expiration date from a month ago, effectively
         * deleting it on the client side.
         * 
         * @param name
         *            The cookie name.
         */
        public void delete(String name) {
            set(name, "-delete-", -30);
        }

        @Override
        public Iterator<String> iterator() {
            return this.cookies.keySet().iterator();
        }

        /**
         * Read a cookie from the HTTP Headers.
         * 
         * @param name
         *            The cookie's name.
         * @return The cookie's value if it exists, null otherwise.
         */
        public String read(String name) {
            return this.cookies.get(name);
        }

        public void set(Cookie cookie) {
            this.queue.add(cookie);
        }

        /**
         * Sets a cookie.
         * 
         * @param name
         *            The cookie's name.
         * @param value
         *            The cookie's value.
         * @param expires
         *            How many days until the cookie expires.
         */
        public void set(String name, String value, int expires) {
            this.queue.add(new Cookie(name, value, Cookie.getHTTPTime(expires)));
        }

        /**
         * Internally used by the webserver to add all queued cookies into the
         * Response's HTTP Headers.
         * 
         * @param response
         *            The Response object to which headers the queued cookies
         *            will be added.
         */
        public void unloadQueue(Response response) {
            for (Cookie cookie : this.queue) {
                response.addHeader("Set-Cookie", cookie.getHTTPHeader());
            }
        }
    }

    /**
     * Default threading strategy for NanoHTTPD.
     * <p/>
     * <p>
     * By default, the server spawns a new Thread for every incoming request.
     * These are set to <i>daemon</i> status, and named according to the request
     * number. The name is useful when profiling the application.
     * </p>
     */
    public static class DefaultAsyncRunner implements AsyncRunner {

        private long requestCount;

        @Override
        public void exec(Runnable code) {
            ++this.requestCount;
            Thread t = new Thread(code);
            t.setDaemon(true);
            t.setName("NanoHttpd Request Processor (#" + this.requestCount + ")");
            t.start();
        }
    }

    /**
     * Default strategy for creating and cleaning up temporary files.
     * <p/>
     * <p>
     * By default, files are created by <code>File.createTempFile()</code> in
     * the directory specified.
     * </p>
     */
    public static class DefaultTempFile implements TempFile {

        private final File file;

        private final OutputStream fstream;

        public DefaultTempFile(String tempdir) throws IOException {
            this.file = File.createTempFile("NanoHTTPD-", "", new File(tempdir));
            this.fstream = new FileOutputStream(this.file);
        }

        @Override
        public void delete() throws Exception {
            safeClose(this.fstream);
            this.file.delete();
        }

        @Override
        public String getName() {
            return this.file.getAbsolutePath();
        }

        @Override
        public OutputStream open() throws Exception {
            return this.fstream;
        }
    }

    /**
     * Default strategy for creating and cleaning up temporary files.
     * <p/>
     * <p>
     * This class stores its files in the standard location (that is, wherever
     * <code>java.io.tmpdir</code> points to). Files are added to an internal
     * list, and deleted when no longer needed (that is, when
     * <code>clear()</code> is invoked at the end of processing a request).
     * </p>
     */
    public static class DefaultTempFileManager implements TempFileManager {

        private final String tmpdir;

        private final List<TempFile> tempFiles;

        public DefaultTempFileManager() {
            this.tmpdir = System.getProperty("java.io.tmpdir");
            this.tempFiles = new ArrayList<TempFile>();
        }

        @Override
        public void clear() {
            for (TempFile file : this.tempFiles) {
                try {
                    file.delete();
                } catch (Exception ignored) {
                    NanoHTTPD.LOG.log(Level.WARNING, "could not delete file ", ignored);
                }
            }
            this.tempFiles.clear();
        }

        @Override
        public TempFile createTempFile() throws Exception {
            DefaultTempFile tempFile = new DefaultTempFile(this.tmpdir);
            this.tempFiles.add(tempFile);
            return tempFile;
        }
    }

    /**
     * Default strategy for creating and cleaning up temporary files.
     */
    private class DefaultTempFileManagerFactory implements TempFileManagerFactory {

        @Override
        public TempFileManager create() {
            return new DefaultTempFileManager();
        }
    }

    protected class HTTPSession implements IHTTPSession {

        public static final int BUFSIZE = 8192;

        private final TempFileManager tempFileManager;

        private final OutputStream outputStream;

        private final PushbackInputStream inputStream;

        private int splitbyte;

        private int rlen;

        private String uri;

        private Method method;

        private Map<String, String> parms;

        private Map<String, String> headers;

        private CookieHandler cookies;

        private String queryParameterString;

        private String remoteIp;

        public HTTPSession(TempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream) {
            this.tempFileManager = tempFileManager;
            this.inputStream = new PushbackInputStream(inputStream, HTTPSession.BUFSIZE);
            this.outputStream = outputStream;
        }

        public HTTPSession(TempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream, InetAddress inetAddress) {
            this.tempFileManager = tempFileManager;
            this.inputStream = new PushbackInputStream(inputStream, HTTPSession.BUFSIZE);
            this.outputStream = outputStream;
            this.remoteIp = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "127.0.0.1" : inetAddress.getHostAddress().toString();
            this.headers = new HashMap<String, String>();
        }

        /**
         * Decodes the sent headers and loads the data into Key/value pairs
         */
        private void decodeHeader(BufferedReader in, Map<String, String> pre, Map<String, String> parms, Map<String, String> headers) throws ResponseException {
            try {
                // Read the request line
                String inLine = in.readLine();
                if (inLine == null) {
                    return;
                }

                StringTokenizer st = new StringTokenizer(inLine);
                if (!st.hasMoreTokens()) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                }

                pre.put("method", st.nextToken());

                if (!st.hasMoreTokens()) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
                }

                String uri = st.nextToken();

                // Decode parameters from the URI
                int qmi = uri.indexOf('?');
                if (qmi >= 0) {
                    decodeParms(uri.substring(qmi + 1), parms);
                    uri = decodePercent(uri.substring(0, qmi));
                } else {
                    uri = decodePercent(uri);
                }

                // If there's another token, its protocol version,
                // followed by HTTP headers. Ignore version but parse headers.
                // NOTE: this now forces header names lower case since they are
                // case insensitive and vary by client.
                if (!st.hasMoreTokens()) {
                    NanoHTTPD.LOG.log(Level.FINE, "no protocol version specified, strange..");
                }
                String line = in.readLine();
                while (line != null && line.trim().length() > 0) {
                    int p = line.indexOf(':');
                    if (p >= 0) {
                        headers.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1).trim());
                    }
                    line = in.readLine();
                }

                pre.put("uri", uri);
            } catch (IOException ioe) {
                throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
            }
        }

        /**
         * Decodes the Multipart Body data and put it into Key/Value pairs.
         */
        private void decodeMultipartData(String boundary, ByteBuffer fbuf, BufferedReader in, Map<String, String> parms, Map<String, String> files) throws ResponseException {
            try {
                int[] bpositions = getBoundaryPositions(fbuf, boundary.getBytes());
                int boundarycount = 1;
                String mpline = in.readLine();
                while (mpline != null) {
                    if (!mpline.contains(boundary)) {
                        throw new ResponseException(Response.Status.BAD_REQUEST,
                                "BAD REQUEST: Content type is multipart/form-data but next chunk does not start with boundary. Usage: GET /example/file.html");
                    }
                    boundarycount++;
                    Map<String, String> item = new HashMap<String, String>();
                    mpline = in.readLine();
                    while (mpline != null && mpline.trim().length() > 0) {
                        int p = mpline.indexOf(':');
                        if (p != -1) {
                            item.put(mpline.substring(0, p).trim().toLowerCase(Locale.US), mpline.substring(p + 1).trim());
                        }
                        mpline = in.readLine();
                    }
                    if (mpline != null) {
                        String contentDisposition = item.get("content-disposition");
                        if (contentDisposition == null) {
                            throw new ResponseException(Response.Status.BAD_REQUEST,
                                    "BAD REQUEST: Content type is multipart/form-data but no content-disposition info found. Usage: GET /example/file.html");
                        }
                        StringTokenizer st = new StringTokenizer(contentDisposition, ";");
                        Map<String, String> disposition = new HashMap<String, String>();
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken().trim();
                            int p = token.indexOf('=');
                            if (p != -1) {
                                disposition.put(token.substring(0, p).trim().toLowerCase(Locale.US), token.substring(p + 1).trim());
                            }
                        }
                        String pname = disposition.get("name");
                        pname = pname.substring(1, pname.length() - 1);

                        String value = "";
                        if (item.get("content-type") == null) {
                            while (mpline != null && !mpline.contains(boundary)) {
                                mpline = in.readLine();
                                if (mpline != null) {
                                    int d = mpline.indexOf(boundary);
                                    if (d == -1) {
                                        value += mpline;
                                    } else {
                                        value += mpline.substring(0, d - 2);
                                    }
                                }
                            }
                        } else {
                            if (boundarycount > bpositions.length) {
                                throw new ResponseException(Response.Status.INTERNAL_ERROR, "Error processing request");
                            }
                            int offset = stripMultipartHeaders(fbuf, bpositions[boundarycount - 2]);
                            String path = saveTmpFile(fbuf, offset, bpositions[boundarycount - 1] - offset - 4);
                            if (!files.containsKey(pname)) {
                                files.put(pname, path);
                            } else {
                                int count = 2;
                                while (files.containsKey(pname + count)) {
                                    count++;
                                }
                                files.put(pname + count, path);
                            }
                            value = disposition.get("filename");
                            value = value.substring(1, value.length() - 1);
                            do {
                                mpline = in.readLine();
                            } while (mpline != null && !mpline.contains(boundary));
                        }
                        parms.put(pname, value);
                    }
                }
            } catch (IOException ioe) {
                throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
            }
        }

        /**
         * Decodes parameters in percent-encoded URI-format ( e.g.
         * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
         * Map. NOTE: this doesn't support multiple identical keys due to the
         * simplicity of Map.
         */
        private void decodeParms(String parms, Map<String, String> p) {
            if (parms == null) {
                this.queryParameterString = "";
                return;
            }

            this.queryParameterString = parms;
            StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) {
                    p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
                } else {
                    p.put(decodePercent(e).trim(), "");
                }
            }
        }

        @Override
        public void execute() throws IOException {
            try {
                // Read the first 8192 bytes.
                // The full header should fit in here.
                // Apache's default header limit is 8KB.
                // Do NOT assume that a single read will get the entire header
                // at once!
                byte[] buf = new byte[HTTPSession.BUFSIZE];
                this.splitbyte = 0;
                this.rlen = 0;

                int read = -1;
                try {
                    read = this.inputStream.read(buf, 0, HTTPSession.BUFSIZE);
                } catch (Exception e) {
                    safeClose(this.inputStream);
                    safeClose(this.outputStream);
                    throw new SocketException("NanoHttpd Shutdown");
                }
                if (read == -1) {
                    // socket was been closed
                    safeClose(this.inputStream);
                    safeClose(this.outputStream);
                    throw new SocketException("NanoHttpd Shutdown");
                }
                while (read > 0) {
                    this.rlen += read;
                    this.splitbyte = findHeaderEnd(buf, this.rlen);
                    if (this.splitbyte > 0) {
                        break;
                    }
                    read = this.inputStream.read(buf, this.rlen, HTTPSession.BUFSIZE - this.rlen);
                }

                if (this.splitbyte < this.rlen) {
                    this.inputStream.unread(buf, this.splitbyte, this.rlen - this.splitbyte);
                }

                this.parms = new HashMap<String, String>();
                if (null == this.headers) {
                    this.headers = new HashMap<String, String>();
                } else {
                    this.headers.clear();
                }

                if (null != this.remoteIp) {
                    this.headers.put("remote-addr", this.remoteIp);
                    this.headers.put("http-client-ip", this.remoteIp);
                }

                // Create a BufferedReader for parsing the header.
                BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, this.rlen)));

                // Decode the header into parms and header java properties
                Map<String, String> pre = new HashMap<String, String>();
                decodeHeader(hin, pre, this.parms, this.headers);

                this.method = Method.lookup(pre.get("method"));
                if (this.method == null) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error.");
                }

                this.uri = pre.get("uri");

                this.cookies = new CookieHandler(this.headers);

                // Ok, now do the serve()
                Response r = serve(this);
                if (r == null) {
                    throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                } else {
                    this.cookies.unloadQueue(r);
                    r.setRequestMethod(this.method);
                    r.send(this.outputStream);
                }
            } catch (SocketException e) {
                // throw it out to close socket object (finalAccept)
                throw e;
            } catch (SocketTimeoutException ste) {
                // treat socket timeouts the same way we treat socket exceptions
                // i.e. close the stream & finalAccept object by throwing the
                // exception up the call stack.
                throw ste;
            } catch (IOException ioe) {
                Response r = new Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                r.send(this.outputStream);
                safeClose(this.outputStream);
            } catch (ResponseException re) {
                Response r = new Response(re.getStatus(), NanoHTTPD.MIME_PLAINTEXT, re.getMessage());
                r.send(this.outputStream);
                safeClose(this.outputStream);
            } finally {
                this.tempFileManager.clear();
            }
        }

        /**
         * Find byte index separating header from body. It must be the last byte
         * of the first two sequential new lines.
         */
        private int findHeaderEnd(final byte[] buf, int rlen) {
            int splitbyte = 0;
            while (splitbyte + 3 < rlen) {
                if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
                    return splitbyte + 4;
                }
                splitbyte++;
            }
            return 0;
        }

        /**
         * Find the byte positions where multipart boundaries start.
         */
        private int[] getBoundaryPositions(ByteBuffer b, byte[] boundary) {
            int matchcount = 0;
            int matchbyte = -1;
            List<Integer> matchbytes = new ArrayList<Integer>();
            for (int i = 0; i < b.limit(); i++) {
                if (b.get(i) == boundary[matchcount]) {
                    if (matchcount == 0) {
                        matchbyte = i;
                    }
                    matchcount++;
                    if (matchcount == boundary.length) {
                        matchbytes.add(matchbyte);
                        matchcount = 0;
                        matchbyte = -1;
                    }
                } else {
                    i -= matchcount;
                    matchcount = 0;
                    matchbyte = -1;
                }
            }
            int[] ret = new int[matchbytes.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = matchbytes.get(i);
            }
            return ret;
        }

        @Override
        public CookieHandler getCookies() {
            return this.cookies;
        }

        @Override
        public final Map<String, String> getHeaders() {
            return this.headers;
        }

        @Override
        public final InputStream getInputStream() {
            return this.inputStream;
        }

        @Override
        public final Method getMethod() {
            return this.method;
        }

        @Override
        public final Map<String, String> getParms() {
            return this.parms;
        }

        @Override
        public String getQueryParameterString() {
            return this.queryParameterString;
        }

        private RandomAccessFile getTmpBucket() {
            try {
                TempFile tempFile = this.tempFileManager.createTempFile();
                return new RandomAccessFile(tempFile.getName(), "rw");
            } catch (Exception e) {
                throw new Error(e); // we won't recover, so throw an error
            }
        }

        @Override
        public final String getUri() {
            return this.uri;
        }

        @Override
        public void parseBody(Map<String, String> files) throws IOException, ResponseException {
            RandomAccessFile randomAccessFile = null;
            BufferedReader in = null;
            try {

                randomAccessFile = getTmpBucket();

                long size;
                if (this.headers.containsKey("content-length")) {
                    size = Integer.parseInt(this.headers.get("content-length"));
                } else if (this.splitbyte < this.rlen) {
                    size = this.rlen - this.splitbyte;
                } else {
                    size = 0;
                }

                // Now read all the body and write it to f
                byte[] buf = new byte[512];
                while (this.rlen >= 0 && size > 0) {
                    this.rlen = this.inputStream.read(buf, 0, (int) Math.min(size, 512));
                    size -= this.rlen;
                    if (this.rlen > 0) {
                        randomAccessFile.write(buf, 0, this.rlen);
                    }
                }

                // Get the raw body as a byte []
                ByteBuffer fbuf = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length());
                randomAccessFile.seek(0);

                // Create a BufferedReader for easily reading it as string.
                InputStream bin = new FileInputStream(randomAccessFile.getFD());
                in = new BufferedReader(new InputStreamReader(bin));

                // If the method is POST, there may be parameters
                // in data section, too, read it:
                if (Method.POST.equals(this.method)) {
                    String contentType = "";
                    String contentTypeHeader = this.headers.get("content-type");

                    StringTokenizer st = null;
                    if (contentTypeHeader != null) {
                        st = new StringTokenizer(contentTypeHeader, ",; ");
                        if (st.hasMoreTokens()) {
                            contentType = st.nextToken();
                        }
                    }

                    if ("multipart/form-data".equalsIgnoreCase(contentType)) {
                        // Handle multipart/form-data
                        if (!st.hasMoreTokens()) {
                            throw new ResponseException(Response.Status.BAD_REQUEST,
                                    "BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html");
                        }

                        String boundaryStartString = "boundary=";
                        int boundaryContentStart = contentTypeHeader.indexOf(boundaryStartString) + boundaryStartString.length();
                        String boundary = contentTypeHeader.substring(boundaryContentStart, contentTypeHeader.length());
                        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                            boundary = boundary.substring(1, boundary.length() - 1);
                        }

                        decodeMultipartData(boundary, fbuf, in, this.parms, files);
                    } else {
                        String postLine = "";
                        StringBuilder postLineBuffer = new StringBuilder();
                        char pbuf[] = new char[512];
                        int read = in.read(pbuf);
                        while (read >= 0) {
                            postLine = String.valueOf(pbuf, 0, read);
                            postLineBuffer.append(postLine);
                            read = in.read(pbuf);
                        }
                        postLine = postLineBuffer.toString().trim();
                        // Handle application/x-www-form-urlencoded
                        if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
                            decodeParms(postLine, this.parms);
                        } else if (postLine.length() != 0) {
                            // Special case for raw POST data => create a
                            // special files entry "postData" with raw content
                            // data
                            files.put("postData", postLine);
                        }
                    }
                } else if (Method.PUT.equals(this.method)) {
                    files.put("content", saveTmpFile(fbuf, 0, fbuf.limit()));
                }
            } finally {
                safeClose(randomAccessFile);
                safeClose(in);
            }
        }

        /**
         * Retrieves the content of a sent file and saves it to a temporary
         * file. The full path to the saved file is returned.
         */
        private String saveTmpFile(ByteBuffer b, int offset, int len) {
            String path = "";
            if (len > 0) {
                FileOutputStream fileOutputStream = null;
                try {
                    TempFile tempFile = this.tempFileManager.createTempFile();
                    ByteBuffer src = b.duplicate();
                    fileOutputStream = new FileOutputStream(tempFile.getName());
                    FileChannel dest = fileOutputStream.getChannel();
                    src.position(offset).limit(offset + len);
                    dest.write(src.slice());
                    path = tempFile.getName();
                } catch (Exception e) { // Catch exception if any
                    throw new Error(e); // we won't recover, so throw an error
                } finally {
                    safeClose(fileOutputStream);
                }
            }
            return path;
        }

        /**
         * It returns the offset separating multipart file headers from the
         * file's data.
         */
        private int stripMultipartHeaders(ByteBuffer b, int offset) {
            int i;
            for (i = offset; i < b.limit(); i++) {
                if (b.get(i) == '\r' && b.get(++i) == '\n' && b.get(++i) == '\r' && b.get(++i) == '\n') {
                    break;
                }
            }
            return i + 1;
        }
    }

    /**
     * Handles one session, i.e. parses the HTTP request and returns the
     * response.
     */
    public interface IHTTPSession {

        void execute() throws IOException;

        CookieHandler getCookies();

        Map<String, String> getHeaders();

        InputStream getInputStream();

        Method getMethod();

        Map<String, String> getParms();

        String getQueryParameterString();

        /**
         * @return the path part of the URL.
         */
        String getUri();

        /**
         * Adds the files in the request body to the files map.
         * 
         * @param files
         *            map to modify
         */
        void parseBody(Map<String, String> files) throws IOException, ResponseException;
    }

    /**
     * HTTP Request methods, with the ability to decode a <code>String</code>
     * back to its enum value.
     */
    public enum Method {
        GET,
        PUT,
        POST,
        DELETE,
        HEAD,
        OPTIONS;

        static Method lookup(String method) {
            for (Method m : Method.values()) {
                if (m.toString().equalsIgnoreCase(method)) {
                    return m;
                }
            }
            return null;
        }
    }

    /**
     * HTTP response. Return one of these from serve().
     */
    public static class Response {

        public interface IStatus {

            String getDescription();

            int getRequestStatus();
        }

        /**
         * Some HTTP response status codes
         */
        public enum Status implements IStatus {
            SWITCH_PROTOCOL(101, "Switching Protocols"),
            OK(200, "OK"),
            CREATED(201, "Created"),
            ACCEPTED(202, "Accepted"),
            NO_CONTENT(204, "No Content"),
            PARTIAL_CONTENT(206, "Partial Content"),
            REDIRECT(301, "Moved Permanently"),
            NOT_MODIFIED(304, "Not Modified"),
            BAD_REQUEST(400, "Bad Request"),
            UNAUTHORIZED(401, "Unauthorized"),
            FORBIDDEN(403, "Forbidden"),
            NOT_FOUND(404, "Not Found"),
            METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
            RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
            INTERNAL_ERROR(500, "Internal Server Error");

            private final int requestStatus;

            private final String description;

            Status(int requestStatus, String description) {
                this.requestStatus = requestStatus;
                this.description = description;
            }

            @Override
            public String getDescription() {
                return "" + this.requestStatus + " " + this.description;
            }

            @Override
            public int getRequestStatus() {
                return this.requestStatus;
            }
        }

        /**
         * HTTP status code after processing, e.g. "200 OK", Status.OK
         */
        private IStatus status;

        /**
         * MIME type of content, e.g. "text/html"
         */
        private String mimeType;

        /**
         * Data of the response, may be null.
         */
        private InputStream data;

        /**
         * Headers for the HTTP response. Use addHeader() to add lines.
         */
        private final Map<String, String> header = new HashMap<String, String>();

        /**
         * The request method that spawned this response.
         */
        private Method requestMethod;

        /**
         * Use chunkedTransfer
         */
        private boolean chunkedTransfer;

        /**
         * Basic constructor.
         */
        public Response(IStatus status, String mimeType, InputStream data) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = data;
        }

        /**
         * Convenience method that makes an InputStream out of given text.
         */
        public Response(IStatus status, String mimeType, String txt) {
            this.status = status;
            this.mimeType = mimeType;
            try {
                this.data = txt != null ? new ByteArrayInputStream(txt.getBytes("UTF-8")) : null;
            } catch (java.io.UnsupportedEncodingException uee) {
                NanoHTTPD.LOG.log(Level.SEVERE, "encoding problem", uee);
            }
        }

        /**
         * Default constructor: response = Status.OK, mime = MIME_HTML and your
         * supplied message
         */
        public Response(String msg) {
            this(Status.OK, NanoHTTPD.MIME_HTML, msg);
        }

        /**
         * Adds given line to the header.
         */
        public void addHeader(String name, String value) {
            this.header.put(name, value);
        }

        public InputStream getData() {
            return this.data;
        }

        public String getHeader(String name) {
            return this.header.get(name);
        }

        public String getMimeType() {
            return this.mimeType;
        }

        public Method getRequestMethod() {
            return this.requestMethod;
        }

        public IStatus getStatus() {
            return this.status;
        }

        private boolean headerAlreadySent(Map<String, String> header, String name) {
            boolean alreadySent = false;
            for (String headerName : header.keySet()) {
                alreadySent |= headerName.equalsIgnoreCase(name);
            }
            return alreadySent;
        }

        /**
         * Sends given response to the socket.
         */
        protected void send(OutputStream outputStream) {
            String mime = this.mimeType;
            SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

            try {
                if (this.status == null) {
                    throw new Error("sendResponse(): Status can't be null.");
                }
                PrintWriter pw = new PrintWriter(outputStream);
                pw.print("HTTP/1.1 " + this.status.getDescription() + " \r\n");

                if (mime != null) {
                    pw.print("Content-Type: " + mime + "\r\n");
                }

                if (this.header == null || this.header.get("Date") == null) {
                    pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
                }

                if (this.header != null) {
                    for (String key : this.header.keySet()) {
                        String value = this.header.get(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }

                sendConnectionHeaderIfNotAlreadyPresent(pw, this.header);

                if (this.requestMethod != Method.HEAD && this.chunkedTransfer) {
                    sendAsChunked(outputStream, pw);
                } else {
                    int pending = this.data != null ? this.data.available() : 0;
                    pending = sendContentLengthHeaderIfNotAlreadyPresent(pw, this.header, pending);
                    pw.print("\r\n");
                    pw.flush();
                    sendAsFixedLength(outputStream, pending);
                }
                outputStream.flush();
                safeClose(this.data);
            } catch (IOException ioe) {
                NanoHTTPD.LOG.log(Level.SEVERE, "Could not send response to the client", ioe);
            }
        }

        private void sendAsChunked(OutputStream outputStream, PrintWriter pw) throws IOException {
            pw.print("Transfer-Encoding: chunked\r\n");
            pw.print("\r\n");
            pw.flush();
            int BUFFER_SIZE = 16 * 1024;
            byte[] CRLF = "\r\n".getBytes();
            byte[] buff = new byte[BUFFER_SIZE];
            int read;
            while ((read = this.data.read(buff)) > 0) {
                outputStream.write(String.format("%x\r\n", read).getBytes());
                outputStream.write(buff, 0, read);
                outputStream.write(CRLF);
            }
            outputStream.write(String.format("0\r\n\r\n").getBytes());
        }

        private void sendAsFixedLength(OutputStream outputStream, int pending) throws IOException {
            if (this.requestMethod != Method.HEAD && this.data != null) {
                int BUFFER_SIZE = 16 * 1024;
                byte[] buff = new byte[BUFFER_SIZE];
                while (pending > 0) {
                    int read = this.data.read(buff, 0, pending > BUFFER_SIZE ? BUFFER_SIZE : pending);
                    if (read <= 0) {
                        break;
                    }
                    outputStream.write(buff, 0, read);
                    pending -= read;
                }
            }
        }

        protected void sendConnectionHeaderIfNotAlreadyPresent(PrintWriter pw, Map<String, String> header) {
            if (!headerAlreadySent(header, "connection")) {
                pw.print("Connection: keep-alive\r\n");
            }
        }

        protected int sendContentLengthHeaderIfNotAlreadyPresent(PrintWriter pw, Map<String, String> header, int size) {
            for (String headerName : header.keySet()) {
                if (headerName.equalsIgnoreCase("content-length")) {
                    try {
                        return Integer.parseInt(header.get(headerName));
                    } catch (NumberFormatException ex) {
                        return size;
                    }
                }
            }

            pw.print("Content-Length: " + size + "\r\n");
            return size;
        }

        public void setChunkedTransfer(boolean chunkedTransfer) {
            this.chunkedTransfer = chunkedTransfer;
        }

        public void setData(InputStream data) {
            this.data = data;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public void setRequestMethod(Method requestMethod) {
            this.requestMethod = requestMethod;
        }

        public void setStatus(IStatus status) {
            this.status = status;
        }
    }

    public static final class ResponseException extends Exception {

        private static final long serialVersionUID = 6569838532917408380L;

        private final Response.Status status;

        public ResponseException(Response.Status status, String message) {
            super(message);
            this.status = status;
        }

        public ResponseException(Response.Status status, String message, Exception e) {
            super(message, e);
            this.status = status;
        }

        public Response.Status getStatus() {
            return this.status;
        }
    }

    /**
     * A temp file.
     * <p/>
     * <p>
     * Temp files are responsible for managing the actual temporary storage and
     * cleaning themselves up when no longer needed.
     * </p>
     */
    public interface TempFile {

        void delete() throws Exception;

        String getName();

        OutputStream open() throws Exception;
    }

    /**
     * Temp file manager.
     * <p/>
     * <p>
     * Temp file managers are created 1-to-1 with incoming requests, to create
     * and cleanup temporary files created as a result of handling the request.
     * </p>
     */
    public interface TempFileManager {

        void clear();

        TempFile createTempFile() throws Exception;
    }

    /**
     * Factory to create temp file managers.
     */
    public interface TempFileManagerFactory {

        TempFileManager create();
    }

    /**
     * Maximum time to wait on Socket.getInputStream().read() (in milliseconds)
     * This is required as the Keep-Alive HTTP connections would otherwise block
     * the socket reading thread forever (or as long the browser is open).
     */
    public static final int SOCKET_READ_TIMEOUT = 5000;

    /**
     * Common MIME type for dynamic content: plain text
     */
    public static final String MIME_PLAINTEXT = "text/plain";

    /**
     * Common MIME type for dynamic content: html
     */
    public static final String MIME_HTML = "text/html";

    /**
     * Pseudo-Parameter to use to store the actual query string in the
     * parameters map for later re-processing.
     */
    private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(NanoHTTPD.class.getName());

    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and an
     * array of loaded KeyManagers. These objects must properly
     * loaded/initialized by the caller.
     */
    public static SSLServerSocketFactory makeSSLSocketFactory(KeyStore loadedKeyStore, KeyManager[] keyManagers) throws IOException {
        SSLServerSocketFactory res = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(loadedKeyStore);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagers, trustManagerFactory.getTrustManagers(), null);
            res = ctx.getServerSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return res;
    }

    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and a
     * loaded KeyManagerFactory. These objects must properly loaded/initialized
     * by the caller.
     */
    public static SSLServerSocketFactory makeSSLSocketFactory(KeyStore loadedKeyStore, KeyManagerFactory loadedKeyFactory) throws IOException {
        SSLServerSocketFactory res = null;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(loadedKeyStore);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(loadedKeyFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            res = ctx.getServerSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return res;
    }

    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a KeyStore resource with your
     * certificate and passphrase
     */
    public static SSLServerSocketFactory makeSSLSocketFactory(String keyAndTrustStoreClasspathPath, char[] passphrase) throws IOException {
        SSLServerSocketFactory res = null;
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream keystoreStream = NanoHTTPD.class.getResourceAsStream(keyAndTrustStoreClasspathPath);
            keystore.load(keystoreStream, passphrase);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            res = ctx.getServerSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return res;
    }

    private static final void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                NanoHTTPD.LOG.log(Level.SEVERE, "Could not close", e);
            }
        }
    }

    private final String hostname;

    private final int myPort;

    private ServerSocket myServerSocket;

    private final Set<Socket> openConnections = new HashSet<Socket>();

    private SSLServerSocketFactory sslServerSocketFactory;

    private Thread myThread;

    /**
     * Pluggable strategy for asynchronously executing requests.
     */
    private AsyncRunner asyncRunner;

    /**
     * Pluggable strategy for creating and cleaning up temporary files.
     */
    private TempFileManagerFactory tempFileManagerFactory;

    /**
     * Constructs an HTTP server on given port.
     */
    public NanoHTTPD(int port) {
        this(null, port);
    }

    // -------------------------------------------------------------------------------
    // //
    //
    // Threading Strategy.
    //
    // -------------------------------------------------------------------------------
    // //

    /**
     * Constructs an HTTP server on given hostname and port.
     */
    public NanoHTTPD(String hostname, int port) {
        this.hostname = hostname;
        this.myPort = port;
        setTempFileManagerFactory(new DefaultTempFileManagerFactory());
        setAsyncRunner(new DefaultAsyncRunner());
    }

    // -------------------------------------------------------------------------------
    // //
    //
    // Temp file handling strategy.
    //
    // -------------------------------------------------------------------------------
    // //

    /**
     * Forcibly closes all connections that are open.
     */
    public synchronized void closeAllConnections() {
        for (Socket socket : this.openConnections) {
            safeClose(socket);
        }
    }

    /**
     * Decode parameters from a URL, handing the case where a single parameter
     * name might have been supplied several times, by return lists of values.
     * In general these lists will contain a single element.
     * 
     * @param parms
     *            original <b>NanoHTTPD</b> parameters values, as passed to the
     *            <code>serve()</code> method.
     * @return a map of <code>String</code> (parameter name) to
     *         <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected Map<String, List<String>> decodeParameters(Map<String, String> parms) {
        return this.decodeParameters(parms.get(NanoHTTPD.QUERY_STRING_PARAMETER));
    }

    /**
     * Decode parameters from a URL, handing the case where a single parameter
     * name might have been supplied several times, by return lists of values.
     * In general these lists will contain a single element.
     * 
     * @param queryString
     *            a query string pulled from the URL.
     * @return a map of <code>String</code> (parameter name) to
     *         <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected Map<String, List<String>> decodeParameters(String queryString) {
        Map<String, List<String>> parms = new HashMap<String, List<String>>();
        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                String propertyName = sep >= 0 ? decodePercent(e.substring(0, sep)).trim() : decodePercent(e).trim();
                if (!parms.containsKey(propertyName)) {
                    parms.put(propertyName, new ArrayList<String>());
                }
                String propertyValue = sep >= 0 ? decodePercent(e.substring(sep + 1)) : null;
                if (propertyValue != null) {
                    parms.get(propertyName).add(propertyValue);
                }
            }
        }
        return parms;
    }

    /**
     * Decode percent encoded <code>String</code> values.
     * 
     * @param str
     *            the percent encoded <code>String</code>
     * @return expanded form of the input, for example "foo%20bar" becomes
     *         "foo bar"
     */
    protected String decodePercent(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
            NanoHTTPD.LOG.log(Level.WARNING, "Encoding not supported, ignored", ignored);
        }
        return decoded;
    }

    // -------------------------------------------------------------------------------
    // //

    public final int getListeningPort() {
        return this.myServerSocket == null ? -1 : this.myServerSocket.getLocalPort();
    }

    public final boolean isAlive() {
        return wasStarted() && !this.myServerSocket.isClosed() && this.myThread.isAlive();
    }

    /**
     * Call before start() to serve over HTTPS instead of HTTP
     */
    public void makeSecure(SSLServerSocketFactory sslServerSocketFactory) {
        this.sslServerSocketFactory = sslServerSocketFactory;
    }

    /**
     * Registers that a new connection has been set up.
     * 
     * @param socket
     *            the {@link Socket} for the connection.
     */
    public synchronized void registerConnection(Socket socket) {
        this.openConnections.add(socket);
    }

    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this returns a 404 "Not Found" plain text error response.)
     * 
     * @param session
     *            The HTTP session
     * @return HTTP response, see class Response for details
     */
    public Response serve(IHTTPSession session) {
        Map<String, String> files = new HashMap<String, String>();
        Method method = session.getMethod();
        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return new Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (ResponseException re) {
                return new Response(re.getStatus(), NanoHTTPD.MIME_PLAINTEXT, re.getMessage());
            }
        }

        Map<String, String> parms = session.getParms();
        parms.put(NanoHTTPD.QUERY_STRING_PARAMETER, session.getQueryParameterString());
        return serve(session.getUri(), method, session.getHeaders(), parms, files);
    }

    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this returns a 404 "Not Found" plain text error response.)
     * 
     * @param uri
     *            Percent-decoded URI without parameters, for example
     *            "/index.cgi"
     * @param method
     *            "GET", "POST" etc.
     * @param parms
     *            Parsed, percent decoded parameters from URI and, in case of
     *            POST, data.
     * @param headers
     *            Header entries, percent decoded
     * @return HTTP response, see class Response for details
     */
    @Deprecated
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {
        return new Response(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
    }

    /**
     * Pluggable strategy for asynchronously executing requests.
     * 
     * @param asyncRunner
     *            new strategy for handling threads.
     */
    public void setAsyncRunner(AsyncRunner asyncRunner) {
        this.asyncRunner = asyncRunner;
    }

    /**
     * Pluggable strategy for creating and cleaning up temporary files.
     * 
     * @param tempFileManagerFactory
     *            new strategy for handling temp files.
     */
    public void setTempFileManagerFactory(TempFileManagerFactory tempFileManagerFactory) {
        this.tempFileManagerFactory = tempFileManagerFactory;
    }

    /**
     * Start the server.
     * 
     * @throws IOException
     *             if the socket is in use.
     */
    public void start() throws IOException {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT);
    }

    /**
     * Start the server.
     * 
     * @param timeout
     *            timeout to use for socket connections.
     * @throws IOException
     *             if the socket is in use.
     */
    public void start(final int timeout) throws IOException {
        if (this.sslServerSocketFactory != null) {
            SSLServerSocket ss = (SSLServerSocket) this.sslServerSocketFactory.createServerSocket();
            ss.setNeedClientAuth(false);
            this.myServerSocket = ss;
        } else {
            this.myServerSocket = new ServerSocket();
        }
        this.myServerSocket.setReuseAddress(true);
        this.myServerSocket.bind(this.hostname != null ? new InetSocketAddress(this.hostname, this.myPort) : new InetSocketAddress(this.myPort));

        this.myThread = new Thread(new Runnable() {

            @Override
            public void run() {
                do {
                    try {
                        final Socket finalAccept = NanoHTTPD.this.myServerSocket.accept();
                        registerConnection(finalAccept);
                        finalAccept.setSoTimeout(timeout);
                        final InputStream inputStream = finalAccept.getInputStream();
                        NanoHTTPD.this.asyncRunner.exec(new Runnable() {

                            @Override
                            public void run() {
                                OutputStream outputStream = null;
                                try {
                                    outputStream = finalAccept.getOutputStream();
                                    TempFileManager tempFileManager = NanoHTTPD.this.tempFileManagerFactory.create();
                                    HTTPSession session = new HTTPSession(tempFileManager, inputStream, outputStream, finalAccept.getInetAddress());
                                    while (!finalAccept.isClosed()) {
                                        session.execute();
                                    }
                                } catch (Exception e) {
                                    // When the socket is closed by the client,
                                    // we throw our own SocketException
                                    // to break the "keep alive" loop above. If
                                    // the exception was anything other
                                    // than the expected SocketException OR a
                                    // SocketTimeoutException, print the
                                    // stacktrace
                                    if (!(e instanceof SocketException && "NanoHttpd Shutdown".equals(e.getMessage())) && !(e instanceof SocketTimeoutException)) {
                                        NanoHTTPD.LOG.log(Level.FINE, "Communication with the client broken", e);
                                    }
                                } finally {
                                    safeClose(outputStream);
                                    safeClose(inputStream);
                                    safeClose(finalAccept);
                                    unRegisterConnection(finalAccept);
                                }
                            }
                        });
                    } catch (IOException e) {
                        NanoHTTPD.LOG.log(Level.FINE, "Communication with the client broken", e);
                    }
                } while (!NanoHTTPD.this.myServerSocket.isClosed());
            }
        });
        this.myThread.setDaemon(true);
        this.myThread.setName("NanoHttpd Main Listener");
        this.myThread.start();
    }

    /**
     * Stop the server.
     */
    public void stop() {
        try {
            safeClose(this.myServerSocket);
            closeAllConnections();
            if (this.myThread != null) {
                this.myThread.join();
            }
        } catch (Exception e) {
            NanoHTTPD.LOG.log(Level.SEVERE, "Could not stop all connections", e);
        }
    }

    /**
     * Registers that a connection has been closed
     * 
     * @param socket
     *            the {@link Socket} for the connection.
     */
    public synchronized void unRegisterConnection(Socket socket) {
        this.openConnections.remove(socket);
    }

    public final boolean wasStarted() {
        return this.myServerSocket != null && this.myThread != null;
    }
}
