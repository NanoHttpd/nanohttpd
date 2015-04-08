package fi.iki.elonen;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoWebSocketServer.WebSocketFrame.CloseCode;
import fi.iki.elonen.NanoWebSocketServer.WebSocketFrame.CloseFrame;
import fi.iki.elonen.NanoWebSocketServer.WebSocketFrame.OpCode;

public abstract class NanoWebSocketServer extends NanoHTTPD {
	public static final String HEADER_UPGRADE = "upgrade";
	public static final String HEADER_UPGRADE_VALUE = "websocket";
	public static final String HEADER_CONNECTION = "connection";
	public static final String HEADER_CONNECTION_VALUE = "Upgrade";
	public static final String HEADER_WEBSOCKET_VERSION = "sec-websocket-version";
	public static final String HEADER_WEBSOCKET_VERSION_VALUE = "13";
	public static final String HEADER_WEBSOCKET_KEY = "sec-websocket-key";
	public static final String HEADER_WEBSOCKET_ACCEPT = "sec-websocket-accept";
	public static final String HEADER_WEBSOCKET_PROTOCOL = "sec-websocket-protocol";
	
	private final static String WEBSOCKET_KEY_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	
    public NanoWebSocketServer(int port) {
        super(port);
    }

    public NanoWebSocketServer(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(final IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        if (isWebsocketRequested(session)) {
            if (!HEADER_WEBSOCKET_VERSION_VALUE.equalsIgnoreCase(headers.get(HEADER_WEBSOCKET_VERSION))) {
                return new Response(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                        "Invalid Websocket-Version " + headers.get(HEADER_WEBSOCKET_VERSION));
            }

            if (!headers.containsKey(HEADER_WEBSOCKET_KEY)) {
                return new Response(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                        "Missing Websocket-Key");
            }

            WebSocket webSocket = openWebSocket(session);
            Response handshakeResponse = webSocket.getHandshakeResponse();
            try {
                handshakeResponse.addHeader(HEADER_WEBSOCKET_ACCEPT, makeAcceptKey(headers.get(HEADER_WEBSOCKET_KEY)));
            } catch (NoSuchAlgorithmException e) {
                return new Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                        "The SHA-1 Algorithm required for websockets is not available on the server.");
            }

            if (headers.containsKey(HEADER_WEBSOCKET_PROTOCOL)) {
                handshakeResponse.addHeader(HEADER_WEBSOCKET_PROTOCOL, headers.get(HEADER_WEBSOCKET_PROTOCOL).split(",")[0]);
            }

            return handshakeResponse;
        } else {
            return super.serve(session);
        }
    }

    protected boolean isWebsocketRequested(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String upgrade = headers.get(HEADER_UPGRADE);
        boolean isCorrectConnection = isWebSocketConnectionHeader(headers);
        boolean isUpgrade = HEADER_UPGRADE_VALUE.equalsIgnoreCase(upgrade);
        return (isUpgrade && isCorrectConnection);
    }

    private boolean isWebSocketConnectionHeader(Map<String, String> headers) {
        String connection = headers.get(HEADER_CONNECTION);
        return (connection != null && connection.toLowerCase().contains(HEADER_CONNECTION_VALUE.toLowerCase()));
    }
    
    public WebSocket openWebSocket(IHTTPSession handshake) {
        return new WebSocket(handshake);
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
    
    public static enum State {
    	UNCONNECTED, CONNECTING, OPEN, CLOSING, CLOSED
    }

    public class WebSocket {
        private InputStream in;
        private OutputStream out;
        private WebSocketFrame.OpCode continuousOpCode = null;
        private List<WebSocketFrame> continuousFrames = new LinkedList<WebSocketFrame>();
        private State state = State.UNCONNECTED;
        private final NanoHTTPD.IHTTPSession handshakeRequest;

        private final NanoHTTPD.Response handshakeResponse = new NanoHTTPD.Response(
                NanoHTTPD.Response.Status.SWITCH_PROTOCOL, null, (InputStream) null) {
            @Override
            protected void send(OutputStream out) {
                WebSocket.this.out = out;
                state = State.CONNECTING;
                super.send(out);
                state = State.OPEN;
                readWebsocket();
            }
        };

        public WebSocket(NanoHTTPD.IHTTPSession handshakeRequest) {
            this.handshakeRequest = handshakeRequest;
            this.in = handshakeRequest.getInputStream();

			handshakeResponse.addHeader(HEADER_UPGRADE, HEADER_UPGRADE_VALUE);
			handshakeResponse.addHeader(HEADER_CONNECTION, HEADER_CONNECTION_VALUE);
        }

        public NanoHTTPD.IHTTPSession getHandshakeRequest() {
            return handshakeRequest;
        }

        public NanoHTTPD.Response getHandshakeResponse() {
            return handshakeResponse;
        }

        // --------------------------------IO--------------------------------------

        private void readWebsocket() {
            try {
                while (state == State.OPEN) {
                    handleWebsocketFrame(WebSocketFrame.read(in));
                }
            } catch (CharacterCodingException e) {
                onException(this, e);
                doClose(CloseCode.InvalidFramePayloadData, e.toString(), false);
            } catch (IOException e) {
                onException(this, e);
                if (e instanceof WebSocketException) {
                    doClose(((WebSocketException) e).getCode(), ((WebSocketException) e).getReason(), false);
                }
            } finally {
                doClose(CloseCode.InternalServerError, "Handler terminated without closing the connection.", false);
            }
        }

        private void handleWebsocketFrame(WebSocketFrame frame) throws IOException {
        	onFrameReceived(frame);
            if (frame.getOpCode() == OpCode.Close) {
                handleCloseFrame(frame);
            } else if (frame.getOpCode() == OpCode.Ping) {
                sendFrame(new WebSocketFrame(OpCode.Pong, true, frame.getBinaryPayload()));
            } else if (frame.getOpCode() == OpCode.Pong) {
                onPong(this, frame);
            } else if (!frame.isFin() || frame.getOpCode() == OpCode.Continuation) {
                handleFrameFragment(frame);
            } else if (continuousOpCode != null) {
                throw new WebSocketException(CloseCode.ProtocolError, "Continuous frame sequence not completed.");
            } else if (frame.getOpCode() == OpCode.Text || frame.getOpCode() == OpCode.Binary) {
                onMessage(this, frame);
            } else {
                throw new WebSocketException(CloseCode.ProtocolError, "Non control or continuous frame expected.");
            }
        }

        private void handleCloseFrame(WebSocketFrame frame) throws IOException {
            CloseCode code = CloseCode.NormalClosure;
            String reason = "";
            if (frame instanceof CloseFrame) {
                code = ((CloseFrame) frame).getCloseCode();
                reason = ((CloseFrame) frame).getCloseReason();
            }
            if (state == State.CLOSING) {
                //Answer for my requested close
                doClose(code, reason, false);
            } else {
                //Answer close request from other endpoint and close self
                State oldState = state;
                state = State.CLOSING;
                if (oldState == State.OPEN) {
                    sendFrame(new CloseFrame(code, reason));
                }
                doClose(code, reason, true);
            }
        }

        private void handleFrameFragment(WebSocketFrame frame) throws IOException {
            if (frame.getOpCode() != OpCode.Continuation) {
                //First
                if (continuousOpCode != null) {
                    throw new WebSocketException(CloseCode.ProtocolError, "Previous continuous frame sequence not completed.");
                }
                continuousOpCode = frame.getOpCode();
                continuousFrames.clear();
                continuousFrames.add(frame);
            } else if (frame.isFin()) {
                //Last
                if (continuousOpCode == null) {
                    throw new WebSocketException(CloseCode.ProtocolError, "Continuous frame sequence was not started.");
                }
                onMessage(this, new WebSocketFrame(continuousOpCode, continuousFrames));
                continuousOpCode = null;
                continuousFrames.clear();
            } else if (continuousOpCode == null) {
                //Unexpected
                throw new WebSocketException(CloseCode.ProtocolError, "Continuous frame sequence was not started.");
            } else {
                //Intermediate
                continuousFrames.add(frame);
            }
        }

        public synchronized void sendFrame(WebSocketFrame frame) throws IOException {
        	onSendFrame(frame);
            frame.write(out);
        }

        // --------------------------------Close-----------------------------------

        private void doClose(CloseCode code, String reason, boolean initiatedByRemote) {
            if (state == State.CLOSED) {
                return;
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            state = State.CLOSED;
            onClose(this, code, reason, initiatedByRemote);
        }

        // --------------------------------Public Facade---------------------------
        
        public void ping(byte[] payload) throws IOException {
        	sendFrame(new WebSocketFrame(OpCode.Ping, true, payload));
        }
        
        public void send(byte[] payload) throws IOException {
        	sendFrame(new WebSocketFrame(OpCode.Binary, true, payload));
        }
        
        public void send(String payload) throws IOException {
        	sendFrame(new WebSocketFrame(OpCode.Text, true, payload));
        }
        
        public void close(CloseCode code, String reason) throws IOException {
        	State oldState = state;
        	state = State.CLOSING;
        	if (oldState == State.OPEN) {
        		sendFrame(new CloseFrame(code, reason));
        	} else {
        		doClose(code, reason, false);
        	}
        }
    }
    
    public static class WebSocketFrame {
        private OpCode opCode;
        private boolean fin;
        private byte[] maskingKey;

        private byte[] payload;

        private transient int _payloadLength;
        private transient String _payloadString;

        private WebSocketFrame(OpCode opCode, boolean fin) {
            setOpCode(opCode);
            setFin(fin);
        }

        public WebSocketFrame(OpCode opCode, boolean fin, byte[] payload, byte[] maskingKey) {
            this(opCode, fin);
            setMaskingKey(maskingKey);
            setBinaryPayload(payload);
        }

        public WebSocketFrame(OpCode opCode, boolean fin, byte[] payload) {
            this(opCode, fin, payload, null);
        }

        public WebSocketFrame(OpCode opCode, boolean fin, String payload, byte[] maskingKey) throws CharacterCodingException {
            this(opCode, fin);
            setMaskingKey(maskingKey);
            setTextPayload(payload);
        }

        public WebSocketFrame(OpCode opCode, boolean fin, String payload) throws CharacterCodingException {
            this(opCode, fin, payload, null);
        }

        public WebSocketFrame(WebSocketFrame clone) {
            setOpCode(clone.getOpCode());
            setFin(clone.isFin());
            setBinaryPayload(clone.getBinaryPayload());
            setMaskingKey(clone.getMaskingKey());
        }

        public WebSocketFrame(OpCode opCode, List<WebSocketFrame> fragments) throws WebSocketException {
            setOpCode(opCode);
            setFin(true);

            long _payloadLength = 0;
            for (WebSocketFrame inter : fragments) {
                _payloadLength += inter.getBinaryPayload().length;
            }
            if (_payloadLength < 0 || _payloadLength > Integer.MAX_VALUE) {
                throw new WebSocketException(CloseCode.MessageTooBig, "Max frame length has been exceeded.");
            }
            this._payloadLength = (int) _payloadLength;
            byte[] payload = new byte[this._payloadLength];
            int offset = 0;
            for (WebSocketFrame inter : fragments) {
                System.arraycopy(inter.getBinaryPayload(), 0, payload, offset, inter.getBinaryPayload().length);
                offset += inter.getBinaryPayload().length;
            }
            setBinaryPayload(payload);
        }

        // --------------------------------GETTERS---------------------------------

        public OpCode getOpCode() {
            return opCode;
        }

        public void setOpCode(OpCode opcode) {
            this.opCode = opcode;
        }

        public boolean isFin() {
            return fin;
        }

        public void setFin(boolean fin) {
            this.fin = fin;
        }

        public boolean isMasked() {
            return maskingKey != null && maskingKey.length == 4;
        }

        public byte[] getMaskingKey() {
            return maskingKey;
        }

        public void setMaskingKey(byte[] maskingKey) {
            if (maskingKey != null && maskingKey.length != 4) {
                throw new IllegalArgumentException("MaskingKey " + Arrays.toString(maskingKey) + " hasn't length 4");
            }
            this.maskingKey = maskingKey;
        }

        public void setUnmasked() {
            setMaskingKey(null);
        }

        public byte[] getBinaryPayload() {
            return payload;
        }

        public void setBinaryPayload(byte[] payload) {
            this.payload = payload;
            this._payloadLength = payload.length;
            this._payloadString = null;
        }

        public String getTextPayload() {
            if (_payloadString == null) {
                try {
                    _payloadString = binary2Text(getBinaryPayload());
                } catch (CharacterCodingException e) {
                    throw new RuntimeException("Undetected CharacterCodingException", e);
                }
            }
            return _payloadString;
        }

        public void setTextPayload(String payload) throws CharacterCodingException {
            this.payload = text2Binary(payload);
            this._payloadLength = payload.length();
            this._payloadString = payload;
        }

        // --------------------------------SERIALIZATION---------------------------

        public static WebSocketFrame read(InputStream in) throws IOException {
            byte head = (byte) checkedRead(in.read());
            boolean fin = ((head & 0x80) != 0);
            OpCode opCode = OpCode.find((byte) (head & 0x0F));
            if ((head & 0x70) != 0) {
                throw new WebSocketException(CloseCode.ProtocolError, "The reserved bits (" + Integer.toBinaryString(head & 0x70) + ") must be 0.");
            }
            if (opCode == null) {
                throw new WebSocketException(CloseCode.ProtocolError, "Received frame with reserved/unknown opcode " + (head & 0x0F) + ".");
            } else if (opCode.isControlFrame() && !fin) {
                throw new WebSocketException(CloseCode.ProtocolError, "Fragmented control frame.");
            }

            WebSocketFrame frame = new WebSocketFrame(opCode, fin);
            frame.readPayloadInfo(in);
            frame.readPayload(in);
            if (frame.getOpCode() == OpCode.Close) {
                return new CloseFrame(frame);
            } else {
                return frame;
            }
        }

        private static int checkedRead(int read) throws IOException {
            if (read < 0) {
                throw new EOFException();
            }
            return read;
        }

        private void readPayloadInfo(InputStream in) throws IOException {
            byte b = (byte) checkedRead(in.read());
            boolean masked = ((b & 0x80) != 0);

            _payloadLength = (byte) (0x7F & b);
            if (_payloadLength == 126) {
                // checkedRead must return int for this to work
                _payloadLength = (checkedRead(in.read()) << 8 | checkedRead(in.read())) & 0xFFFF;
                if (_payloadLength < 126) {
                    throw new WebSocketException(CloseCode.ProtocolError, "Invalid data frame 2byte length. (not using minimal length encoding)");
                }
            } else if (_payloadLength == 127) {
                long _payloadLength = ((long) checkedRead(in.read())) << 56 |
                        ((long) checkedRead(in.read())) << 48 |
                        ((long) checkedRead(in.read())) << 40 |
                        ((long) checkedRead(in.read())) << 32 |
                        checkedRead(in.read()) << 24 | checkedRead(in.read()) << 16 | checkedRead(in.read()) << 8 | checkedRead(in.read());
                if (_payloadLength < 65536) {
                    throw new WebSocketException(CloseCode.ProtocolError, "Invalid data frame 4byte length. (not using minimal length encoding)");
                }
                if (_payloadLength < 0 || _payloadLength > Integer.MAX_VALUE) {
                    throw new WebSocketException(CloseCode.MessageTooBig, "Max frame length has been exceeded.");
                }
                this._payloadLength = (int) _payloadLength;
            }

            if (opCode.isControlFrame()) {
                if (_payloadLength > 125) {
                    throw new WebSocketException(CloseCode.ProtocolError, "Control frame with payload length > 125 bytes.");
                }
                if (opCode == OpCode.Close && _payloadLength == 1) {
                    throw new WebSocketException(CloseCode.ProtocolError, "Received close frame with payload len 1.");
                }
            }

            if (masked) {
                maskingKey = new byte[4];
                int read = 0;
                while (read < maskingKey.length) {
                    read += checkedRead(in.read(maskingKey, read, maskingKey.length - read));
                }
            }
        }

        private void readPayload(InputStream in) throws IOException {
            payload = new byte[_payloadLength];
            int read = 0;
            while (read < _payloadLength) {
                read += checkedRead(in.read(payload, read, _payloadLength - read));
            }

            if (isMasked()) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= maskingKey[i % 4];
                }
            }

            //Test for Unicode errors
            if (getOpCode() == OpCode.Text) {
                _payloadString = binary2Text(getBinaryPayload());
            }
        }

        public void write(OutputStream out) throws IOException {
            byte header = 0;
            if (fin) {
                header |= 0x80;
            }
            header |= opCode.getValue() & 0x0F;
            out.write(header);

            _payloadLength = getBinaryPayload().length;
            if (_payloadLength <= 125) {
                out.write(isMasked() ? 0x80 | (byte) _payloadLength : (byte) _payloadLength);
            } else if (_payloadLength <= 0xFFFF) {
                out.write(isMasked() ? 0xFE : 126);
                out.write(_payloadLength >>> 8);
                out.write(_payloadLength);
            } else {
                out.write(isMasked() ? 0xFF : 127);
                out.write(_payloadLength >>> 56 & 0); //integer only contains 31 bit
                out.write(_payloadLength >>> 48 & 0);
                out.write(_payloadLength >>> 40 & 0);
                out.write(_payloadLength >>> 32 & 0);
                out.write(_payloadLength >>> 24);
                out.write(_payloadLength >>> 16);
                out.write(_payloadLength >>> 8);
                out.write(_payloadLength);
            }


            if (isMasked()) {
                out.write(maskingKey);
                for (int i = 0; i < _payloadLength; i++) {
                    out.write(getBinaryPayload()[i] ^ maskingKey[i % 4]);
                }
            } else {
                out.write(getBinaryPayload());
            }
            out.flush();
        }

        // --------------------------------ENCODING--------------------------------

        public static final Charset TEXT_CHARSET = Charset.forName("UTF-8");

        public static String binary2Text(byte[] payload) throws CharacterCodingException {
        	return new String(payload, TEXT_CHARSET);
        }

        public static String binary2Text(byte[] payload, int offset, int length) throws CharacterCodingException {
        	return new String(payload, offset, length, TEXT_CHARSET);
        }

        public static byte[] text2Binary(String payload) throws CharacterCodingException {
        	return payload.getBytes(TEXT_CHARSET);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("WS[");
            sb.append(getOpCode());
            sb.append(", ").append(isFin() ? "fin" : "inter");
            sb.append(", ").append(isMasked() ? "masked" : "unmasked");
            sb.append(", ").append(payloadToString());
            sb.append(']');
            return sb.toString();
        }

        private String payloadToString() {
            if (payload == null) return "null";
            else {
                final StringBuilder sb = new StringBuilder();
                sb.append('[').append(payload.length).append("b] ");
                if (getOpCode() == OpCode.Text) {
                    String text = getTextPayload();
                    if (text.length() > 100)
                        sb.append(text.substring(0, 100)).append("...");
                    else
                        sb.append(text);
                } else {
                    sb.append("0x");
                    for (int i = 0; i < Math.min(payload.length, 50); ++i)
                        sb.append(Integer.toHexString((int) payload[i] & 0xFF));
                    if (payload.length > 50)
                        sb.append("...");
                }
                return sb.toString();
            }
        }

        // --------------------------------CONSTANTS-------------------------------

        public static enum OpCode {
            Continuation(0), Text(1), Binary(2), Close(8), Ping(9), Pong(10);

            private final byte code;

            private OpCode(int code) {
                this.code = (byte) code;
            }

            public byte getValue() {
                return code;
            }

            public boolean isControlFrame() {
                return this == Close || this == Ping || this == Pong;
            }

            public static OpCode find(byte value) {
                for (OpCode opcode : values()) {
                    if (opcode.getValue() == value) {
                        return opcode;
                    }
                }
                return null;
            }
        }

        public static enum CloseCode {
            NormalClosure(1000), GoingAway(1001), ProtocolError(1002), UnsupportedData(1003), NoStatusRcvd(1005),
            AbnormalClosure(1006), InvalidFramePayloadData(1007), PolicyViolation(1008), MessageTooBig(1009),
            MandatoryExt(1010), InternalServerError(1011), TLSHandshake(1015);

            private final int code;

            private CloseCode(int code) {
                this.code = code;
            }

            public int getValue() {
                return code;
            }

            public static CloseCode find(int value) {
                for (CloseCode code : values()) {
                    if (code.getValue() == value) {
                        return code;
                    }
                }
                return null;
            }
        }

        // ------------------------------------------------------------------------

        public static class CloseFrame extends WebSocketFrame {
            private CloseCode _closeCode;
            private String _closeReason;

            private CloseFrame(WebSocketFrame wrap) throws CharacterCodingException {
                super(wrap);
                assert wrap.getOpCode() == OpCode.Close;
                if (wrap.getBinaryPayload().length >= 2) {
                    _closeCode = CloseCode.find((wrap.getBinaryPayload()[0] & 0xFF) << 8 |
                            (wrap.getBinaryPayload()[1] & 0xFF));
                    _closeReason = binary2Text(getBinaryPayload(), 2, getBinaryPayload().length - 2);
                }
            }

            public CloseFrame(CloseCode code, String closeReason) throws CharacterCodingException {
                super(OpCode.Close, true, generatePayload(code, closeReason));
            }

            private static byte[] generatePayload(CloseCode code, String closeReason) throws CharacterCodingException {
                if (code != null) {
                    byte[] reasonBytes = text2Binary(closeReason);
                    byte[] payload = new byte[reasonBytes.length + 2];
                    payload[0] = (byte) ((code.getValue() >> 8) & 0xFF);
                    payload[1] = (byte) ((code.getValue()) & 0xFF);
                    System.arraycopy(reasonBytes, 0, payload, 2, reasonBytes.length);
                    return payload;
                } else {
                    return new byte[0];
                }
            }

            public CloseCode getCloseCode() {
                return _closeCode;
            }

            public String getCloseReason() {
                return _closeReason;
            }
        }
    }

    public static class WebSocketException extends IOException {
        private static final long serialVersionUID = 1L;
        
    	private CloseCode code;
        private String reason;

        public WebSocketException(Exception cause) {
            this(CloseCode.InternalServerError, cause.toString(), cause);
        }

        public WebSocketException(CloseCode code, String reason) {
            this(code, reason, null);
        }

        public WebSocketException(CloseCode code, String reason, Exception cause) {
            super(code + ": " + reason, cause);
            this.code = code;
            this.reason = reason;
        }

        public CloseCode getCode() {
            return code;
        }

        public String getReason() {
            return reason;
        }
    }
    
    // --------------------------------Listener--------------------------------

    protected abstract void onPong(WebSocket webSocket, WebSocketFrame pongFrame);

    protected abstract void onMessage(WebSocket webSocket, WebSocketFrame messageFrame);

    protected abstract void onClose(WebSocket webSocket, CloseCode code, String reason, boolean initiatedByRemote);

    protected abstract void onException(WebSocket webSocket, IOException e);
    
    protected void onFrameReceived(WebSocketFrame webSocket) {
    	// only for debugging
    }

    public void onSendFrame(WebSocketFrame webSocket) {
    	// only for debugging
    }
}

