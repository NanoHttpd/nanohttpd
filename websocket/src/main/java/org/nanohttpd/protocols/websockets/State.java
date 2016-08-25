package org.nanohttpd.protocols.websockets;

public enum State {
    UNCONNECTED,
    CONNECTING,
    OPEN,
    CLOSING,
    CLOSED
}