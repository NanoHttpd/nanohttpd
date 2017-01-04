package org.nanohttpd.protocols.http.request;

import org.nanohttpd.protocols.http.IHTTPSession;

public class Request {
	public final IHTTPSession session;
	public final Method method;
	
	public Request(IHTTPSession session, Method method){
		this.session = session;
		this.method = method;
	}
}
