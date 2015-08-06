package fi.iki.express;

import fi.iki.elonen.NanoHTTPD;

/**
 * Implementation of the Interface should provide a response when the session.getUri() matches getDefaultURIpath result .
 * Created by James on 6/8/2015.
 */
public interface Router {
    public NanoHTTPD.Response doGet     ( NanoHTTPD.IHTTPSession session);
    public NanoHTTPD.Response doPost    ( NanoHTTPD.IHTTPSession session);
    public NanoHTTPD.Response doPut     ( NanoHTTPD.IHTTPSession session);
    public NanoHTTPD.Response doDelete  ( NanoHTTPD.IHTTPSession session);
    public NanoHTTPD.Response doHead    ( NanoHTTPD.IHTTPSession session);
    public NanoHTTPD.Response doOptions ( NanoHTTPD.IHTTPSession session);
    public NanoHTTPD.Response doTrace   ( NanoHTTPD.IHTTPSession session);
    public NanoHTTPD.Response doConnect ( NanoHTTPD.IHTTPSession session);
    public NanoHTTPD.Response doPatch   ( NanoHTTPD.IHTTPSession session);

    public String getDefaultURIPath();

}
