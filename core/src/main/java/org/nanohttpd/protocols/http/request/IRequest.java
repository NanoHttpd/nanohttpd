package org.nanohttpd.protocols.http.request;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.nanohttpd.protocols.http.IConnection;

public interface IRequest {
	/** @return the connection this request originated from */
	public IConnection getClientConnection();
	
	/** @return the request method (GET / POST / etc) */
	public Method getMethod();
	
	/** @return the full URL of this request: protocol, address, port, resource + query string */
	public URL getURL();
	
	/** @return the path of the requested resource (file) */
	public String getResource();
	
	/** @return the raw query string */
	public String getQueryString();
	
	/**
	 * Equivalent to calling getParameter(param, 0);
	 * @return the first parameter with this name
	 */
	public String getFirstParameter(String param);
	
	/** @return the parameter at this index, from the set of parameters with this name */
	public String getParameter(String param, int index);
	
	/** @return the full data structure with all received parameters */
	public Map<String, List<String>> getAllParameters();
	
	/** @return the map with the received http headers */
	public Map<String, String> getHeaders();
	
	/** @return the CookieHandler containing all the cookies received in this request */
	public CookieHandler getCookies();
}
