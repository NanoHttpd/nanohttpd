package fi.iki.elonen;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A simple, tiny, nicely embeddable HTTP server in Java
 * <p/>
 * <p/>
 * NanoHTTPD
 * <p></p>Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias</p>
 * <p/>
 * <p/>
 * <b>Features + limitations: </b>
 * <ul>
 * <p/>
 * <li>Only one Java file</li>
 * <li>Java 5 compatible</li>
 * <li>Released as open source, Modified BSD licence</li>
 * <li>No fixed config files, logging, authorization etc. (Implement yourself if you need them.)</li>
 * <li>Supports parameter parsing of GET and POST methods (+ rudimentary PUT support in 1.25)</li>
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
 * <li>Contains a built-in list of most common mime types</li>
 * <li>All header names are converted lowercase so they don't vary between browsers/clients</li>
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
 * See the separate "LICENSE.md" file for the distribution license (Modified BSD licence)
 */
public abstract class NanoHTTPD {
    /**
     * Common mime type for dynamic content: plain text
     */
    public static final String MIME_PLAINTEXT = "text/plain";
    /**
     * Common mime type for dynamic content: html
     */
    public static final String MIME_HTML = "text/html";
    /**
     * Pseudo-Parameter to use to store the actual query string in the parameters map for later re-processing.
     */
    private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";
    private final String hostname;
    private final int myPort;
    private ServerSocket myServerSocket;
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

    /**
     * Constructs an HTTP server on given hostname and port.
     */
    public NanoHTTPD(String hostname, int port) {
        this.hostname = hostname;
        this.myPort = port;
        setTempFileManagerFactory(new DefaultTempFileManagerFactory());
        setAsyncRunner(new DefaultAsyncRunner());
    }

    private static final void safeClose(ServerSocket serverSocket) {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private static final void safeClose(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    private static final void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Start the server.
     *
     * @throws IOException if the socket is in use.
     */
    public void start() throws IOException {
        myServerSocket = new ServerSocket();
        myServerSocket.bind((hostname != null) ? new InetSocketAddress(hostname, myPort) : new InetSocketAddress(myPort));

        myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        final Socket finalAccept = myServerSocket.accept();
                        final InputStream inputStream = finalAccept.getInputStream();
                        if (inputStream == null) {
                            safeClose(finalAccept);
                        } else {
                            asyncRunner.exec(new Runnable() {
                                @Override
                                public void run() {
                                    OutputStream outputStream = null;
                                    try {
                                        outputStream = finalAccept.getOutputStream();
                                        TempFileManager tempFileManager = tempFileManagerFactory.create();
                                        HTTPSession session = new HTTPSession(tempFileManager, inputStream, outputStream);
                                        while (!finalAccept.isClosed()) {
                                            session.execute();
                                        }
                                    } catch (Exception e) {
                                        // When the socket is closed by the client, we throw our own SocketException
                                        // to break the  "keep alive" loop above.
                                        if (!(e instanceof SocketException && "NanoHttpd Shutdown".equals(e.getMessage()))) {
                                            e.printStackTrace();
                                        }
                                    } finally {
                                        safeClose(outputStream);
                                        safeClose(inputStream);
                                        safeClose(finalAccept);
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                    }
                } while (!myServerSocket.isClosed());
            }
        });
        myThread.setDaemon(true);
        myThread.setName("NanoHttpd Main Listener");
        myThread.start();
    }

    /**
     * Stop the server.
     */
    public void stop() {
        try {
            safeClose(myServerSocket);
            myThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public final int getListeningPort() {
        return myServerSocket == null ? -1 : myServerSocket.getLocalPort();
    }

    public final boolean wasStarted() {
        return myServerSocket != null && myThread != null;
    }

    public final boolean isAlive() {
        return wasStarted() && !myServerSocket.isClosed() && myThread.isAlive();
    }

    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this delegates to serveFile() and allows directory listing.)
     *
     * @param uri     Percent-decoded URI without parameters, for example "/index.cgi"
     * @param method  "GET", "POST" etc.
     * @param parms   Parsed, percent decoded parameters from URI and, in case of POST, data.
     * @param headers Header entries, percent decoded
     * @return HTTP response, see class Response for details
     */
    @Deprecated
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms,
                                   Map<String, String> files) {
        return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
    }

    /**
     * Override this to customize the server.
     * <p/>
     * <p/>
     * (By default, this delegates to serveFile() and allows directory listing.)
     *
     * @param session The HTTP session
     * @return HTTP response, see class Response for details
     */
    public Response serve(HTTPSession session) {
        Map<String, String> files = new HashMap<String, String>();
        Method method = session.getMethod();
        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (ResponseException re) {
                return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }

        return serve(session.getUri(), method, session.getHeaders(), session.getParms(), files);
    }

    /**
     * Decode percent encoded <code>String</code> values.
     *
     * @param str the percent encoded <code>String</code>
     * @return expanded form of the input, for example "foo%20bar" becomes "foo bar"
     */
    protected String decodePercent(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return decoded;
    }

    /**
     * Decode parameters from a URL, handing the case where a single parameter name might have been
     * supplied several times, by return lists of values.  In general these lists will contain a single
     * element.
     *
     * @param parms original <b>NanoHttpd</b> parameters values, as passed to the <code>serve()</code> method.
     * @return a map of <code>String</code> (parameter name) to <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected Map<String, List<String>> decodeParameters(Map<String, String> parms) {
        return this.decodeParameters(parms.get(QUERY_STRING_PARAMETER));
    }

    /**
     * Decode parameters from a URL, handing the case where a single parameter name might have been
     * supplied several times, by return lists of values.  In general these lists will contain a single
     * element.
     *
     * @param queryString a query string pulled from the URL.
     * @return a map of <code>String</code> (parameter name) to <code>List&lt;String&gt;</code> (a list of the values supplied).
     */
    protected Map<String, List<String>> decodeParameters(String queryString) {
        Map<String, List<String>> parms = new HashMap<String, List<String>>();
        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                String propertyName = (sep >= 0) ? decodePercent(e.substring(0, sep)).trim() : decodePercent(e).trim();
                if (!parms.containsKey(propertyName)) {
                    parms.put(propertyName, new ArrayList<String>());
                }
                String propertyValue = (sep >= 0) ? decodePercent(e.substring(sep + 1)) : null;
                if (propertyValue != null) {
                    parms.get(propertyName).add(propertyValue);
                }
            }
        }
        return parms;
    }

    // ------------------------------------------------------------------------------- //
    //
    // Threading Strategy.
    //
    // ------------------------------------------------------------------------------- //

    /**
     * Pluggable strategy for asynchronously executing requests.
     *
     * @param asyncRunner new strategy for handling threads.
     */
    public void setAsyncRunner(AsyncRunner asyncRunner) {
        this.asyncRunner = asyncRunner;
    }

    // ------------------------------------------------------------------------------- //
    //
    // Temp file handling strategy.
    //
    // ------------------------------------------------------------------------------- //

    /**
     * Pluggable strategy for creating and cleaning up temporary files.
     *
     * @param tempFileManagerFactory new strategy for handling temp files.
     */
    public void setTempFileManagerFactory(TempFileManagerFactory tempFileManagerFactory) {
        this.tempFileManagerFactory = tempFileManagerFactory;
    }

    /**
     * HTTP Request methods, with the ability to decode a <code>String</code> back to its enum value.
     */
    public enum Method {
        GET, PUT, POST, DELETE, HEAD;

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
     * Pluggable strategy for asynchronously executing requests.
     */
    public interface AsyncRunner {
        void exec(Runnable code);
    }

    /**
     * Factory to create temp file managers.
     */
    public interface TempFileManagerFactory {
        TempFileManager create();
    }

    // ------------------------------------------------------------------------------- //

    /**
     * Temp file manager.
     * <p/>
     * <p>Temp file managers are created 1-to-1 with incoming requests, to create and cleanup
     * temporary files created as a result of handling the request.</p>
     */
    public interface TempFileManager {
        TempFile createTempFile() throws Exception;

        void clear();
    }

    /**
     * A temp file.
     * <p/>
     * <p>Temp files are responsible for managing the actual temporary storage and cleaning
     * themselves up when no longer needed.</p>
     */
    public interface TempFile {
        OutputStream open() throws Exception;

        void delete() throws Exception;

        String getName();
    }

    /**
     * Default threading strategy for NanoHttpd.
     * <p/>
     * <p>By default, the server spawns a new Thread for every incoming request.  These are set
     * to <i>daemon</i> status, and named according to the request number.  The name is
     * useful when profiling the application.</p>
     */
    public static class DefaultAsyncRunner implements AsyncRunner {
        private long requestCount;

        @Override
        public void exec(Runnable code) {
            ++requestCount;
            Thread t = new Thread(code);
            t.setDaemon(true);
            t.setName("NanoHttpd Request Processor (#" + requestCount + ")");
            t.start();
        }
    }

    /**
     * Default strategy for creating and cleaning up temporary files.
     * <p/>
     * <p></p>This class stores its files in the standard location (that is,
     * wherever <code>java.io.tmpdir</code> points to).  Files are added
     * to an internal list, and deleted when no longer needed (that is,
     * when <code>clear()</code> is invoked at the end of processing a
     * request).</p>
     */
    public static class DefaultTempFileManager implements TempFileManager {
        private final String tmpdir;
        private final List<TempFile> tempFiles;

        public DefaultTempFileManager() {
            tmpdir = System.getProperty("java.io.tmpdir");
            tempFiles = new ArrayList<TempFile>();
        }

        @Override
        public TempFile createTempFile() throws Exception {
            DefaultTempFile tempFile = new DefaultTempFile(tmpdir);
            tempFiles.add(tempFile);
            return tempFile;
        }

        @Override
        public void clear() {
            for (TempFile file : tempFiles) {
                try {
                    file.delete();
                } catch (Exception ignored) {
                }
            }
            tempFiles.clear();
        }
    }

    /**
     * Default strategy for creating and cleaning up temporary files.
     * <p/>
     * <p></p></[>By default, files are created by <code>File.createTempFile()</code> in
     * the directory specified.</p>
     */
    public static class DefaultTempFile implements TempFile {
        private File file;
        private OutputStream fstream;

        public DefaultTempFile(String tempdir) throws IOException {
            file = File.createTempFile("NanoHTTPD-", "", new File(tempdir));
            fstream = new FileOutputStream(file);
        }

        @Override
        public OutputStream open() throws Exception {
            return fstream;
        }

        @Override
        public void delete() throws Exception {
            safeClose(fstream);
            file.delete();
        }

        @Override
        public String getName() {
            return file.getAbsolutePath();
        }
    }

    /**
     * HTTP response. Return one of these from serve().
     */
    public static class Response {
        /**
         * HTTP status code after processing, e.g. "200 OK", HTTP_OK
         */
        private Status status;
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
        private Map<String, String> header = new HashMap<String, String>();
        /**
         * The request method that spawned this response.
         */
        private Method requestMethod;
        /**
         * Use chunkedTransfer
         */
        private boolean chunkedTransfer;

        /**
         * Default constructor: response = HTTP_OK, mime = MIME_HTML and your supplied message
         */
        public Response(String msg) {
            this(Status.OK, MIME_HTML, msg);
        }

        /**
         * Basic constructor.
         */
        public Response(Status status, String mimeType, InputStream data) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = data;
        }

        /**
         * Convenience method that makes an InputStream out of given text.
         */
        public Response(Status status, String mimeType, String txt) {
            this.status = status;
            this.mimeType = mimeType;
            try {
                this.data = txt != null ? new ByteArrayInputStream(txt.getBytes("UTF-8")) : null;
            } catch (java.io.UnsupportedEncodingException uee) {
                uee.printStackTrace();
            }
        }

        /**
         * Adds given line to the header.
         */
        public void addHeader(String name, String value) {
            header.put(name, value);
        }

        /**
         * Sends given response to the socket.
         */
        private void send(OutputStream outputStream) {
            String mime = mimeType;
            SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

            try {
                if (status == null) {
                    throw new Error("sendResponse(): Status can't be null.");
                }
                PrintWriter pw = new PrintWriter(outputStream);
                pw.print("HTTP/1.1 " + status.getDescription() + " \r\n");

                if (mime != null) {
                    pw.print("Content-Type: " + mime + "\r\n");
                }

                if (header == null || header.get("Date") == null) {
                    pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
                }

                if (header != null) {
                    for (String key : header.keySet()) {
                        String value = header.get(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }

                pw.print("Connection: keep-alive\r\n");

                if (requestMethod != Method.HEAD && chunkedTransfer) {
                    sendAsChunked(outputStream, pw);
                } else {
                    sendAsFixedLength(outputStream, pw);
                }
                outputStream.flush();
                safeClose(data);
            } catch (IOException ioe) {
                // Couldn't write? No can do.
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
            while ((read = data.read(buff)) > 0) {
                outputStream.write(String.format("%x\r\n", read).getBytes());
                outputStream.write(buff, 0, read);
                outputStream.write(CRLF);
            }
            outputStream.write(String.format("0\r\n\r\n").getBytes());
        }

        private void sendAsFixedLength(OutputStream outputStream, PrintWriter pw) throws IOException {
            int pending = data != null ? data.available() : 0; // This is to support partial sends, see serveFile()
            pw.print("Content-Length: "+pending+"\r\n");

            pw.print("\r\n");
            pw.flush();

            if (requestMethod != Method.HEAD && data != null) {
                int BUFFER_SIZE = 16 * 1024;
                byte[] buff = new byte[BUFFER_SIZE];
                while (pending > 0) {
                    int read = data.read(buff, 0, ((pending > BUFFER_SIZE) ? BUFFER_SIZE : pending));
                    if (read <= 0) {
                        break;
                    }
                    outputStream.write(buff, 0, read);

                    pending -= read;
                }
            }
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public InputStream getData() {
            return data;
        }

        public void setData(InputStream data) {
            this.data = data;
        }

        public Method getRequestMethod() {
            return requestMethod;
        }

        public void setRequestMethod(Method requestMethod) {
            this.requestMethod = requestMethod;
        }

        public void setChunkedTransfer(boolean chunkedTransfer) {
            this.chunkedTransfer = chunkedTransfer;
        }

        /**
         * Some HTTP response status codes
         */
        public enum Status {
            OK(200, "OK"), CREATED(201, "Created"), ACCEPTED(202, "Accepted"), NO_CONTENT(204, "No Content"), PARTIAL_CONTENT(206, "Partial Content"), REDIRECT(301,
                "Moved Permanently"), NOT_MODIFIED(304, "Not Modified"), BAD_REQUEST(400, "Bad Request"), UNAUTHORIZED(401,
                "Unauthorized"), FORBIDDEN(403, "Forbidden"), NOT_FOUND(404, "Not Found"), RANGE_NOT_SATISFIABLE(416,
                "Requested Range Not Satisfiable"), INTERNAL_ERROR(500, "Internal Server Error");
            private final int requestStatus;
            private final String description;

            Status(int requestStatus, String description) {
                this.requestStatus = requestStatus;
                this.description = description;
            }

            public int getRequestStatus() {
                return this.requestStatus;
            }

            public String getDescription() {
                return "" + this.requestStatus + " " + description;
            }
        }
    }

    public static final class ResponseException extends Exception {

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
            return status;
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

    /**
     * Handles one session, i.e. parses the HTTP request and returns the response.
     */
    protected class HTTPSession {
        public static final int BUFSIZE = 8192;
        private final TempFileManager tempFileManager;
        private final OutputStream outputStream;
        private InputStream inputStream;
        private int splitbyte;
        private int rlen;
        private String uri;
        private Method method;
        private Map<String, String> parms;
        private Map<String, String> headers;
        private CookieHandler cookies;

        public HTTPSession(TempFileManager tempFileManager, InputStream inputStream, OutputStream outputStream) {
            this.tempFileManager = tempFileManager;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        public void execute() throws IOException {
            try {
                // Read the first 8192 bytes.
                // The full header should fit in here.
                // Apache's default header limit is 8KB.
                // Do NOT assume that a single read will get the entire header at once!
                byte[] buf = new byte[BUFSIZE];
                splitbyte = 0;
                rlen = 0;
                {
                    int read = inputStream.read(buf, 0, BUFSIZE);
                    if (read == -1) {
                        // socket was been closed
                        throw new SocketException("NanoHttpd Shutdown");
                    }
                    while (read > 0) {
                        rlen += read;
                        splitbyte = findHeaderEnd(buf, rlen);
                        if (splitbyte > 0)
                            break;
                        read = inputStream.read(buf, rlen, BUFSIZE - rlen);
                    }
                }

                if (splitbyte < rlen) {
                    ByteArrayInputStream splitInputStream = new ByteArrayInputStream(buf, splitbyte, rlen - splitbyte);
                    SequenceInputStream sequenceInputStream = new SequenceInputStream(splitInputStream, inputStream);
                    inputStream = sequenceInputStream;
                }

                parms = new HashMap<String, String>();
                headers = new HashMap<String, String>();

                // Create a BufferedReader for parsing the header.
                BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, rlen)));

                // Decode the header into parms and header java properties
                Map<String, String> pre = new HashMap<String, String>();
                decodeHeader(hin, pre, parms, headers);

                method = Method.lookup(pre.get("method"));
                if (method == null) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error.");
                }

                uri = pre.get("uri");

                cookies = new CookieHandler(headers);

                // Ok, now do the serve()
                Response r = serve(this);
                if (r == null) {
                    throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                } else {
                    cookies.unloadQueue(r);
                    r.setRequestMethod(method);
                    r.send(outputStream);
                }
            } catch (SocketException e) {
                // throw it out to close socket object (finalAccept)
                throw e;
            } catch (IOException ioe) {
                Response r = new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                r.send(outputStream);
                safeClose(outputStream);
            } catch (ResponseException re) {
                Response r = new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                r.send(outputStream);
                safeClose(outputStream);
            } finally {
                tempFileManager.clear();
            }
        }

        protected void parseBody(Map<String, String> files) throws IOException, ResponseException {
            RandomAccessFile randomAccessFile = null;
            BufferedReader in = null;
            try {

                randomAccessFile = getTmpBucket();

                long size;
                if (headers.containsKey("content-length")) {
                    size = Integer.parseInt(headers.get("content-length"));
                } else if (splitbyte < rlen) {
                    size = rlen - splitbyte;
                } else {
                    size = 0;
                }

                // Now read all the body and write it to f
                byte[] buf = new byte[512];
                while (rlen >= 0 && size > 0) {
                    rlen = inputStream.read(buf, 0, 512);
                    size -= rlen;
                    if (rlen > 0) {
                        randomAccessFile.write(buf, 0, rlen);
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
                if (Method.POST.equals(method)) {
                    String contentType = "";
                    String contentTypeHeader = headers.get("content-type");

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
                            throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html");
                        }

                        String boundaryStartString = "boundary=";
                        int boundaryContentStart = contentTypeHeader.indexOf(boundaryStartString) + boundaryStartString.length();
                        String boundary = contentTypeHeader.substring(boundaryContentStart, contentTypeHeader.length());
                        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                            boundary = boundary.substring(1, boundary.length() - 1);
                        }

                        decodeMultipartData(boundary, fbuf, in, parms, files);
                    } else {
                        // Handle application/x-www-form-urlencoded
                        String postLine = "";
                        char pbuf[] = new char[512];
                        int read = in.read(pbuf);
                        while (read >= 0 && !postLine.endsWith("\r\n")) {
                            postLine += String.valueOf(pbuf, 0, read);
                            read = in.read(pbuf);
                        }
                        postLine = postLine.trim();
                        decodeParms(postLine, parms);
                    }
                } else if (Method.PUT.equals(method)) {
                    files.put("content", saveTmpFile(fbuf, 0, fbuf.limit()));
                }
            } finally {
                safeClose(randomAccessFile);
                safeClose(in);
            }
        }

        /**
         * Decodes the sent headers and loads the data into Key/value pairs
         */
        private void decodeHeader(BufferedReader in, Map<String, String> pre, Map<String, String> parms, Map<String, String> headers)
            throws ResponseException {
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

                // If there's another token, it's protocol version,
                // followed by HTTP headers. Ignore version but parse headers.
                // NOTE: this now forces header names lowercase since they are
                // case insensitive and vary by client.
                if (st.hasMoreTokens()) {
                    String line = in.readLine();
                    while (line != null && line.trim().length() > 0) {
                        int p = line.indexOf(':');
                        if (p >= 0)
                            headers.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
                        line = in.readLine();
                    }
                }

                pre.put("uri", uri);
            } catch (IOException ioe) {
                throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
            }
        }

        /**
         * Decodes the Multipart Body data and put it into Key/Value pairs.
         */
        private void decodeMultipartData(String boundary, ByteBuffer fbuf, BufferedReader in, Map<String, String> parms,
                                         Map<String, String> files) throws ResponseException {
            try {
                int[] bpositions = getBoundaryPositions(fbuf, boundary.getBytes());
                int boundarycount = 1;
                String mpline = in.readLine();
                while (mpline != null) {
                    if (!mpline.contains(boundary)) {
                        throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but next chunk does not start with boundary. Usage: GET /example/file.html");
                    }
                    boundarycount++;
                    Map<String, String> item = new HashMap<String, String>();
                    mpline = in.readLine();
                    while (mpline != null && mpline.trim().length() > 0) {
                        int p = mpline.indexOf(':');
                        if (p != -1) {
                            item.put(mpline.substring(0, p).trim().toLowerCase(), mpline.substring(p + 1).trim());
                        }
                        mpline = in.readLine();
                    }
                    if (mpline != null) {
                        String contentDisposition = item.get("content-disposition");
                        if (contentDisposition == null) {
                            throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but no content-disposition info found. Usage: GET /example/file.html");
                        }
                        StringTokenizer st = new StringTokenizer(contentDisposition, "; ");
                        Map<String, String> disposition = new HashMap<String, String>();
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken();
                            int p = token.indexOf('=');
                            if (p != -1) {
                                disposition.put(token.substring(0, p).trim().toLowerCase(), token.substring(p + 1).trim());
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
                            files.put(pname, path);
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
         * Find byte index separating header from body. It must be the last byte of the first two sequential new lines.
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
                    if (matchcount == 0)
                        matchbyte = i;
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

        /**
         * Retrieves the content of a sent file and saves it to a temporary file. The full path to the saved file is returned.
         */
        private String saveTmpFile(ByteBuffer b, int offset, int len) {
            String path = "";
            if (len > 0) {
                FileOutputStream fileOutputStream = null;
                try {
                    TempFile tempFile = tempFileManager.createTempFile();
                    ByteBuffer src = b.duplicate();
                    fileOutputStream = new FileOutputStream(tempFile.getName());
                    FileChannel dest = fileOutputStream.getChannel();
                    src.position(offset).limit(offset + len);
                    dest.write(src.slice());
                    path = tempFile.getName();
                } catch (Exception e) { // Catch exception if any
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    safeClose(fileOutputStream);
                }
            }
            return path;
        }

        private RandomAccessFile getTmpBucket() {
            try {
                TempFile tempFile = tempFileManager.createTempFile();
                return new RandomAccessFile(tempFile.getName(), "rw");
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            return null;
        }

        /**
         * It returns the offset separating multipart file headers from the file's data.
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

        /**
         * Decodes parameters in percent-encoded URI-format ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and
         * adds them to given Map. NOTE: this doesn't support multiple identical keys due to the simplicity of Map.
         */
        private void decodeParms(String parms, Map<String, String> p) {
            if (parms == null) {
                p.put(QUERY_STRING_PARAMETER, "");
                return;
            }

            p.put(QUERY_STRING_PARAMETER, parms);
            StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) {
                    p.put(decodePercent(e.substring(0, sep)).trim(),
                        decodePercent(e.substring(sep + 1)));
                } else {
                    p.put(decodePercent(e).trim(), "");
                }
            }
        }

        public final Map<String, String> getParms() {
            return parms;
        }

        public final Map<String, String> getHeaders() {
            return headers;
        }

        public final String getUri() {
            return uri;
        }

        public final Method getMethod() {
            return method;
        }

        public final InputStream getInputStream() {
            return inputStream;
        }

        public CookieHandler getCookies() {
            return cookies;
        }
    }

    public static class Cookie {
        private String n, v, e;

        public Cookie(String name, String value, String expires) {
            n = name;
            v = value;
            e = expires;
        }

        public Cookie(String name, String value) {
            this(name, value, 30);
        }

        public Cookie(String name, String value, int numDays) {
            n = name;
            v = value;
            e = getHTTPTime(numDays);
        }

        public String getHTTPHeader() {
            String fmt = "%s=%s; expires=%s";
            return String.format(fmt, n, v, e);
        }

        public static String getHTTPTime(int days) {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            calendar.add(Calendar.DAY_OF_MONTH, days);
            return dateFormat.format(calendar.getTime());
        }
    }

    /**
     * Provides rudimentary support for cookies.
     * Doesn't support 'path', 'secure' nor 'httpOnly'.
     * Feel free to improve it and/or add unsupported features.
     *
     * @author LordFokas
     */
    public class CookieHandler implements Iterable<String> {
        private HashMap<String, String> cookies = new HashMap<String, String>();
        private ArrayList<Cookie> queue = new ArrayList<Cookie>();

        public CookieHandler(Map<String, String> httpHeaders) {
            String raw = httpHeaders.get("cookie");
            if (raw != null) {
                String[] tokens = raw.split(";");
                for (String token : tokens) {
                    String[] data = token.trim().split("=");
                    if (data.length == 2) {
                        cookies.put(data[0], data[1]);
                    }
                }
            }
        }

        @Override public Iterator<String> iterator() {
            return cookies.keySet().iterator();
        }

        /**
         * Read a cookie from the HTTP Headers.
         *
         * @param name The cookie's name.
         * @return The cookie's value if it exists, null otherwise.
         */
        public String read(String name) {
            return cookies.get(name);
        }

        /**
         * Sets a cookie.
         *
         * @param name    The cookie's name.
         * @param value   The cookie's value.
         * @param expires How many days until the cookie expires.
         */
        public void set(String name, String value, int expires) {
            queue.add(new Cookie(name, value, Cookie.getHTTPTime(expires)));
        }

        public void set(Cookie cookie) {
            queue.add(cookie);
        }

        /**
         * Set a cookie with an expiration date from a month ago, effectively deleting it on the client side.
         *
         * @param name The cookie name.
         */
        public void delete(String name) {
            set(name, "-delete-", -30);
        }

        /**
         * Internally used by the webserver to add all queued cookies into the Response's HTTP Headers.
         *
         * @param response The Response object to which headers the queued cookies will be added.
         */
        public void unloadQueue(Response response) {
            for (Cookie cookie : queue) {
                response.addHeader("Set-Cookie", cookie.getHTTPHeader());
            }
        }
    }
}
