package fi.iki.elonen;

import fi.iki.elonen.NanoHTTPD;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public abstract class NanoWSD extends NanoHTTPD {
    public static final String HEADER_UPGRADE = "upgrade";
    public static final String HEADER_UPGRADE_VALUE = "websocket";
    public static final String HEADER_CONNECTION = "connection";
    public static final String HEADER_CONNECTION_VALUE = "Upgrade";
    public static final String HEADER_WEBSOCKET_VERSION = "sec-websocket-version";
    public static final String HEADER_WEBSOCKET_VERSION_VALUE = "13";
    public static final String HEADER_WEBSOCKET_KEY = "sec-websocket-key";
    public static final String HEADER_WEBSOCKET_ACCEPT = "sec-websocket-accept";
    public static final String HEADER_WEBSOCKET_PROTOCOL = "sec-websocket-protocol";

    public final static String WEBSOCKET_KEY_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public NanoWSD(int port) {
        super(port);
    }

    public NanoWSD(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(final IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        if (isWebsocketRequested(session)) {
            if (!HEADER_UPGRADE_VALUE.equalsIgnoreCase(headers.get(HEADER_UPGRADE))
                    || !HEADER_CONNECTION_VALUE.equalsIgnoreCase(headers.get(HEADER_CONNECTION))) {
                return new Response(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Invalid Websocket handshake");
            }
            if (!HEADER_WEBSOCKET_VERSION_VALUE.equalsIgnoreCase(headers.get(HEADER_WEBSOCKET_VERSION))) {
                return new Response(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Invalid Websocket-Version " + headers.get(HEADER_WEBSOCKET_VERSION));
            }
            if (!headers.containsKey(HEADER_WEBSOCKET_KEY)) {
                return new Response(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing Websocket-Key");
            }

            WebSocket webSocket = openWebSocket(session);
            try {
                webSocket.getHandshakeResponse().addHeader(HEADER_WEBSOCKET_ACCEPT, makeAcceptKey(headers.get(HEADER_WEBSOCKET_KEY)));
            } catch (NoSuchAlgorithmException e) {
                return new Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "The SHA-1 Algorithm required for websockets is not available on the server.");
            }
            if (headers.containsKey(HEADER_WEBSOCKET_PROTOCOL)) {
                webSocket.getHandshakeResponse().addHeader(HEADER_WEBSOCKET_PROTOCOL, headers.get(HEADER_WEBSOCKET_PROTOCOL).split(",")[0]);
            }
            return webSocket.getHandshakeResponse();
        } else {
            return super.serve(session);
        }
    }

    protected abstract WebSocket openWebSocket(IHTTPSession handshake);

    protected boolean isWebsocketRequested(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        return ((HEADER_UPGRADE_VALUE.equalsIgnoreCase(headers.get(HEADER_UPGRADE)))
                && (HEADER_CONNECTION_VALUE.equalsIgnoreCase(headers.get(HEADER_CONNECTION))));
    }

    public static String makeAcceptKey(String key) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String text = key + WEBSOCKET_KEY_MAGIC;
        md.update(text.getBytes(), 0, text.length());
        byte[] sha1hash = md.digest();
        return encodeBase64(sha1hash);
    }


    private final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    /**
     * Translates the specified byte array into Base64 string.
     * <p>
     * Android has android.util.Base64, sun has sun.misc.Base64Encoder, Java 8 hast java.util.Base64,
     * I have this from stackoverflow: http://stackoverflow.com/a/4265472
     * </p>
     *
     * @param buf the byte array (not null)
     * @return the translated Base64 string (not null)
     */
    private static String encodeBase64(byte[] buf) {
        int size = buf.length;
        char[] ar = new char[((size + 2) / 3) * 4];
        int a = 0;
        int i = 0;
        while (i < size) {
            byte b0 = buf[i++];
            byte b1 = (i < size) ? buf[i++] : 0;
            byte b2 = (i < size) ? buf[i++] : 0;

            int mask = 0x3F;
            ar[a++] = ALPHABET[(b0 >> 2) & mask];
            ar[a++] = ALPHABET[((b0 << 4) | ((b1 & 0xFF) >> 4)) & mask];
            ar[a++] = ALPHABET[((b1 << 2) | ((b2 & 0xFF) >> 6)) & mask];
            ar[a++] = ALPHABET[b2 & mask];
        }
        switch (size % 3) {
            case 1:
                ar[--a] = '=';
            case 2:
                ar[--a] = '=';
        }
        return new String(ar);
    }
}

