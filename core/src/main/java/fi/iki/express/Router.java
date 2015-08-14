package fi.iki.express;

import fi.iki.elonen.NanoHTTPD;

/**
 * Perform you setup routine and any necessary tear down routine in the respective methods.
 * Implementation of the Interface should provide a response when the session.getUri() matches getDefaultURIpath result .
 * Created by James on 6/8/2015.
 */
public interface Router {

    public static final String APPLICATION_JAVASCRIPT_CONTENT_TYPE = "application/javascript";
    public static final String STYLESHEET_CONTENT_TYPE = "text/css";
    public static final String HTML_CONTENT_TYPE = "text/html";
    public static final String TEXT_CONTENT_TYPE = "text/plain";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String JSON_TEXT_CONTENT_TYPE = "text/json";

    /***
     * Implement the various http methods as see needs.
     *
     * @param session
     * @return
     */
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

    public void setup();

    public void tearDown();

}
