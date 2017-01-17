package org.nanohttpd.protocols.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.nanohttpd.protocols.http.request.IRequest;

public interface IConnection {
	/**
	 * Blocking I/O: suspends the thread until a new request arrives.
	 * The received request will then be handled.
	 * This method is meant to be called <b>exclusively</b> by the
	 * thread listening on the socket for requests packets.
	 */
	public void handleNextRequest() throws IOException;
	
	/** @return an input stream to read from this client. */
	public InputStream getInputStream();
	
	/** @return an output stream to write to this client. */
	public OutputStream getOutputStream();
	
	/** @return this socket's remote IP address */
	public String getRemoteIPAddress();
	
	/**
	 * Caution: Calling this method may take some time as the
	 * hostname resolution is a potentially lengthy process.
	 * @return this client's hostname.
	 */
	public String getRemoteHostname();
}
