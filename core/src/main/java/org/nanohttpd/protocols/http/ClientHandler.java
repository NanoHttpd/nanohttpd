package org.nanohttpd.protocols.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

import org.nanohttpd.protocols.http.tempfiles.ITempFileManager;

/**
 * The runnable that will be used for every new client connection.
 */
public class ClientHandler implements Runnable {
    private final NanoHTTPD httpd;

    private final InputStream inputStream;

    private final Socket acceptSocket;

    public ClientHandler(NanoHTTPD httpd, InputStream inputStream, Socket acceptSocket) {
        this.httpd = httpd;
    	this.inputStream = inputStream;
        this.acceptSocket = acceptSocket;
    }

    public void close() {
    	NanoHTTPD.safeClose(this.inputStream);
    	NanoHTTPD.safeClose(this.acceptSocket);
    }

    @Override
    public void run() {
        OutputStream outputStream = null;
        try {
            outputStream = this.acceptSocket.getOutputStream();
            ITempFileManager tempFileManager = httpd.getTempFileManagerFactory().create();
            HTTPSession session = new HTTPSession(httpd, tempFileManager, this.inputStream, outputStream, this.acceptSocket.getInetAddress());
            while (!this.acceptSocket.isClosed()) {
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
                NanoHTTPD.LOG.log(Level.SEVERE, "Communication with the client broken, or an bug in the handler code", e);
            }
        } finally {
        	NanoHTTPD.safeClose(outputStream);
        	NanoHTTPD.safeClose(this.inputStream);
        	NanoHTTPD.safeClose(this.acceptSocket);
            httpd.asyncRunner.closed(this);
        }
    }
}