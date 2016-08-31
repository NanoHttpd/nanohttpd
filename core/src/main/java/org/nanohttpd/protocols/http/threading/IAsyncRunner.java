package org.nanohttpd.protocols.http.threading;

import org.nanohttpd.protocols.http.ClientHandler;

/**
 * Pluggable strategy for asynchronously executing requests.
 */
public interface IAsyncRunner {

    void closeAll();

    void closed(ClientHandler clientHandler);

    void exec(ClientHandler code);
}