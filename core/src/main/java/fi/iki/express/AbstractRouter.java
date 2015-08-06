package fi.iki.express;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by James on 6/8/2015.
 */
public class AbstractRouter implements Router {

    private final String urlPath;

    public AbstractRouter(String urlPath){
        this.urlPath = urlPath;
    }

    @Override
    public NanoHTTPD.Response doGet(NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response doPost(NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response doPut(NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response doDelete(NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response doHead(NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response doOptions(NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response doTrace(NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response doConnect(NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public NanoHTTPD.Response doPatch(NanoHTTPD.IHTTPSession session) {
        return null;
    }

    @Override
    public String getDefaultURIPath() {
        return this.urlPath;
    }
}
