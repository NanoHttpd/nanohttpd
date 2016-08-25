package org.nanohttpd.protocols.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;

/**
 * The runnable that will be used for the main listening thread.
 */
public class ServerRunnable implements Runnable {
    private NanoHTTPD httpd;

    private final int timeout;

    private IOException bindException;

    private boolean hasBinded = false;

    public ServerRunnable(NanoHTTPD httpd, int timeout) {
    	this.httpd = httpd;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        try {
            httpd.getMyServerSocket().bind(httpd.hostname != null ? new InetSocketAddress(httpd.hostname, httpd.myPort) : new InetSocketAddress(httpd.myPort));
            hasBinded = true;
        } catch (IOException e) {
            this.bindException = e;
            return;
        }
        do {
            try {
                final Socket finalAccept = httpd.getMyServerSocket().accept();
                if (this.timeout > 0) {
                    finalAccept.setSoTimeout(this.timeout);
                }
                final InputStream inputStream = finalAccept.getInputStream();
                httpd.asyncRunner.exec(httpd.createClientHandler(finalAccept, inputStream));
            } catch (IOException e) {
                NanoHTTPD.LOG.log(Level.FINE, "Communication with the client broken", e);
            }
        } while (!httpd.getMyServerSocket().isClosed());
    }

	public IOException getBindException() {
		return bindException;
	}

	public boolean hasBinded() {
		return hasBinded;
	}
}
