package org.nanohttpd.protocols.http.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nanohttpd.protocols.http.ClientHandler;

/**
 * Default threading strategy for NanoHTTPD.
 * <p/>
 * <p>
 * By default, the server spawns a new Thread for every incoming request.
 * These are set to <i>daemon</i> status, and named according to the request
 * number. The name is useful when profiling the application.
 * </p>
 */
public class DefaultAsyncRunner implements IAsyncRunner {

    private long requestCount;

    private final List<ClientHandler> running = Collections.synchronizedList(new ArrayList<ClientHandler>());

    /**
     * @return a list with currently running clients.
     */
    public List<ClientHandler> getRunning() {
        return running;
    }

    @Override
    public void closeAll() {
        // copy of the list for concurrency
        for (ClientHandler clientHandler : new ArrayList<ClientHandler>(this.running)) {
            clientHandler.close();
        }
    }

    @Override
    public void closed(ClientHandler clientHandler) {
        this.running.remove(clientHandler);
    }

    @Override
    public void exec(ClientHandler clientHandler) {
        ++this.requestCount;
        Thread t = new Thread(clientHandler);
        t.setDaemon(true);
        t.setName("NanoHttpd Request Processor (#" + this.requestCount + ")");
        this.running.add(clientHandler);
        t.start();
    }
}