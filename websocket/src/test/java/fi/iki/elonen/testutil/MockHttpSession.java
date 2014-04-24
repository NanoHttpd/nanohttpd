package fi.iki.elonen.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.ResponseException;

public class MockHttpSession implements IHTTPSession {

    private Map<String, String> params = new HashMap<String, String>();
    private Map<String, String> headers = new HashMap<String, String>();
    
    @Override
    public void execute() throws IOException {
        
    }

    @Override
    public Map<String, String> getParms() {
        return params;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public String getUri() {
        return null;
    }

    @Override
    public String getQueryParameterString() {
        return null;
    }

    @Override
    public Method getMethod() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public CookieHandler getCookies() {
        return null;
    }

    @Override
    public void parseBody(Map<String, String> files) throws IOException,
            ResponseException {
        
    }
    
}
