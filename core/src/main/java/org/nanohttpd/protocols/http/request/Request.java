package org.nanohttpd.protocols.http.request;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nanohttpd.protocols.http.IConnection;

public class Request implements IRequest{
	private final IConnection connection;
	
	private final Map<String, List<String>> parameters;
	private final Map<String, String> headers;
	private CookieHandler cookies;
	
	public Request(IConnection connection){
		this.connection = connection;
		this.parameters = new HashMap<>();
		this.headers = new HashMap<>();
	}
	
	public void recycle(){
		parameters.clear();
		headers.clear();
	}

	@Override
	public IConnection getClientConnection() {
		return connection;
	}

	@Override
	public Method getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URL getURL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getResource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getQueryString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFirstParameter(String param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getParameter(String param, int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<String>> getAllParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CookieHandler getCookies() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
