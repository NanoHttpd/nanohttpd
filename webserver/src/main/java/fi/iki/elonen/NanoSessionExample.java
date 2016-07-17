package fi.iki.elonen;

import fi.ike.elonen.NanoSession.ISession;

import fi.iki.elonen.NanoHTTPD;

public class NanoSessionExample extends NanoHTTPD {

	private NanoSession nanoSession;
	public NanoSessionExample(int port) {
		super(port);
		nanoSession = new NanoSession();
	}

	@Override
	public Response serve(IHTTPSession http) {
		return sessionServe(http, nanoSession.serve(http));
	}
	
	//new method for serve http
	public Response sessionServe(IHTTPSession http,ISession session){
		if(http.getUri().equals("/")){
			session.set("test", "test-value");
			session.clear();
		}else{
			return newFixedLengthResponse(session.get("test"));	
		}
		return super.serve(http);
	}

}
