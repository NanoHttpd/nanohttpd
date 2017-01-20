package org.nanohttpd.protocols.http;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2017 nanohttpd
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.nanohttpd.protocols.http.NanoHTTPD.ResponseException;
import org.nanohttpd.protocols.http.request.CookieHandler;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.request.Request;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;
import org.nanohttpd.util.Pointer;

public class Connection implements IConnection {
	private static final int BUFFER_SIZE = 0x2000; // 8KB
	
    private final NanoHTTPD httpd;
    private final Request request = new Request(this);
    private final Socket socket;
    private final OutputStream outputStream;
    private final InputStream inputStream;
    private final ITempFileManager tempFileManager;
    private String remoteHostname;

    public Connection(NanoHTTPD httpd, Socket socket, ITempFileManager tempFileManager) throws IOException {
        this.httpd = httpd;
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.tempFileManager = tempFileManager;
    }

    @Override
    public void handleNextRequest() throws IOException {
        Response r = null;
        try {
            // Read the first 8192 bytes.
            // The full header should fit in here.
            // Apache's default header limit is 8KB.
            // Do NOT assume that a single read will get the entire header
            // at once!
            byte[] buf = new byte[BUFFER_SIZE];
            int splitbyte = 0;
            int rlen = 0;

            int read = -1;
            inputStream.mark(BUFFER_SIZE);
            try {
                read = inputStream.read(buf, 0, BUFFER_SIZE);
            } catch (SSLException e) {
                throw e;
            } catch (IOException e) {
                NanoHTTPD.safeClose(inputStream);
                NanoHTTPD.safeClose(outputStream);
                throw new SocketException("NanoHttpd Shutdown");
            }
            if (read == -1) {
                // socket was been closed
                NanoHTTPD.safeClose(inputStream);
                NanoHTTPD.safeClose(outputStream);
                throw new SocketException("NanoHttpd Shutdown");
            }
            while (read > 0) {
                rlen += read;
                splitbyte = HTTPUtils.findHeaderEnd(buf, rlen);
                if (splitbyte > 0) {
                    break;
                }
                read = inputStream.read(buf, rlen, BUFFER_SIZE - rlen);
            }

            if (splitbyte < rlen) {
                inputStream.reset();
                inputStream.skip(splitbyte);
            }

            request.recycle();
            Map<String, List<String>> parameters = request.getAllParameters();
            Map<String, String> headers = request.getHeaders();

            // Create a BufferedReader for parsing the header.
            BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, rlen)));

            // Decode the header into parms and header java properties
            Map<String, String> pre = new HashMap<String, String>();
            Pointer<String> protocolVersion = new Pointer<String>();
            Pointer<String> query = new Pointer<String>();
            HTTPUtils.decodeHeader(hin, pre, parameters, headers, protocolVersion, query);

            Method method = Method.lookup(pre.get("method"));
            if (method == null) {
                throw new ResponseException(Status.BAD_REQUEST, "BAD REQUEST: Syntax error. HTTP verb " + pre.get("method") + " unhandled.");
            }

            CookieHandler cookies = new CookieHandler(headers);
            String uri = pre.get("uri");
            URL url = new URL("<protocol>", "<host>", 0, uri); // TODO
            request.setUp(method, uri, query.get(), null, cookies);

            String connection = headers.get("connection");
            boolean keepAlive = "HTTP/1.1".equals(protocolVersion.get()) && (connection == null || !connection.matches("(?i).*close.*"));
            
            

            // Ok, now do the serve()

            // TODO: long body_size = getBodySize();
            // TODO: long pos_before_serve = inputStream.totalRead()
            // (requires implementation for totalRead())
            r = httpd.handle(request);
            // TODO: inputStream.skip(body_size -
            // (inputStream.totalRead() - pos_before_serve))

            if (r == null) {
                throw new ResponseException(Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
            } else {
                String acceptEncoding = headers.get("accept-encoding");
                cookies.unloadQueue(r);
                r.setRequestMethod(method);
                if (acceptEncoding == null || !acceptEncoding.contains("gzip")) {
                    r.setUseGzip(false);
                }
                r.setKeepAlive(keepAlive);
                r.send(outputStream);
            }
            if (!keepAlive || r.isCloseConnection()) {
                throw new SocketException("NanoHttpd Shutdown");
            }
        } catch (SocketException e) {
            // throw it out to close socket object (finalAccept)
            throw e;
        } catch (SocketTimeoutException ste) {
            // treat socket timeouts the same way we treat socket exceptions
            // i.e. close the stream & finalAccept object by throwing the
            // exception up the call stack.
            throw ste;
        } catch (SSLException ssle) {
            Response resp = Response.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SSL PROTOCOL FAILURE: " + ssle.getMessage());
            resp.send(outputStream);
            NanoHTTPD.safeClose(outputStream);
        } catch (IOException ioe) {
            Response resp = Response.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            resp.send(outputStream);
            NanoHTTPD.safeClose(outputStream);
        } catch (ResponseException re) {
            Response resp = Response.newFixedLengthResponse(re.getStatus(), NanoHTTPD.MIME_PLAINTEXT, re.getMessage());
            resp.send(outputStream);
            NanoHTTPD.safeClose(outputStream);
        } finally {
            NanoHTTPD.safeClose(r);
            tempFileManager.clear();
        }
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public String getRemoteIPAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    @Override
    public String getRemoteHostname() {
        if (remoteHostname == null) {
            InetAddress address = socket.getInetAddress();
            if (address.isAnyLocalAddress() || address.isLoopbackAddress())
                remoteHostname = "localhost";
            else
                remoteHostname = address.getHostName();
        }
        return remoteHostname;
    }

}
