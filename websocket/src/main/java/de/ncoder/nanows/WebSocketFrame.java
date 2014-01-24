package de.ncoder.nanows;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class WebSocketFrame {
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

    public WebSocketFrame(OpCode opCode, boolean fin, String payload, byte[] maskingKey) {
        this(opCode, fin);
        setMaskingKey(maskingKey);
        setTextPayload(payload);
    }

    public WebSocketFrame(OpCode opCode, boolean fin, String payload) {
        this(opCode, fin, payload, null);
    }

    public WebSocketFrame(WebSocketFrame clone) {
        setOpCode(clone.getOpCode());
        setFin(clone.isFin());
        setBinaryPayload(clone.getBinaryPayload());
        setMaskingKey(clone.getMaskingKey());
    }

    public WebSocketFrame(OpCode opCode, List<WebSocketFrame> fragments) {
        setOpCode(opCode);
        setFin(true);

        int length = 0;
        for (WebSocketFrame inter : fragments) {
            length += inter.getBinaryPayload().length;
        }
        byte[] payload = new byte[length];
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
            _payloadString = binary2Text(getBinaryPayload());
        }
        return _payloadString;
    }

    public void setTextPayload(String payload) {
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
        System.out.println(read + "/" + Integer.toBinaryString(read));
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
                throw new WebSocketException(CloseCode.ProtocolError, "Max frame length has been exceeded.");
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
            checkedRead(in.read(maskingKey, 0, 4));
        }
    }

    private void readPayload(InputStream in) throws IOException {
        payload = new byte[_payloadLength];
        checkedRead(in.read(payload, 0, _payloadLength));

        if (isMasked()) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskingKey[i % 4];
            }
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
    }

    // --------------------------------ENCODING--------------------------------

    public static final String TEXT_ENCODING = "UTF-8";

    public static String binary2Text(byte[] payload) {
        try {
            return new String(payload, TEXT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return new String(payload);
        }
    }

    public static String binary2Text(byte[] payload, int offset, int length) {
        try {
            return new String(payload, offset, length, TEXT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return new String(payload, offset, length);
        }
    }

    public static byte[] text2Binary(String payload) {
        try {
            return payload.getBytes(TEXT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return payload.getBytes();
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

        private CloseFrame(WebSocketFrame wrap) {
            super(wrap);
            if (wrap.getBinaryPayload().length >= 2) {
                _closeCode = CloseCode.find((wrap.getBinaryPayload()[0] & 0xFF) << 8 |
                        (wrap.getBinaryPayload()[1] & 0xFF));
                _closeReason = binary2Text(getBinaryPayload(), 2, getBinaryPayload().length - 2);
            }
        }

        public CloseFrame(CloseCode code, String closeReason) {
            super(OpCode.Close, true, generatePayload(code, closeReason));
        }

        private static byte[] generatePayload(CloseCode code, String closeReason) {
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
