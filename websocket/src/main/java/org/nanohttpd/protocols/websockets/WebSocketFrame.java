package org.nanohttpd.protocols.websockets;

/*
 * #%L
 * NanoHttpd-Websocket
 * %%
 * Copyright (C) 2012 - 2016 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class WebSocketFrame {

    public static final Charset TEXT_CHARSET = Charset.forName("UTF-8");

    public static String binary2Text(byte[] payload) throws CharacterCodingException {
        return new String(payload, WebSocketFrame.TEXT_CHARSET);
    }

    public static String binary2Text(byte[] payload, int offset, int length) throws CharacterCodingException {
        return new String(payload, offset, length, WebSocketFrame.TEXT_CHARSET);
    }

    private static int checkedRead(int read) throws IOException {
        if (read < 0) {
            throw new EOFException();
        }
        return read;
    }

    public static WebSocketFrame read(InputStream in) throws IOException {
        byte head = (byte) checkedRead(in.read());
        boolean fin = (head & 0x80) != 0;
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

    public static byte[] text2Binary(String payload) throws CharacterCodingException {
        return payload.getBytes(WebSocketFrame.TEXT_CHARSET);
    }

    private OpCode opCode;

    private boolean fin;

    private byte[] maskingKey;

    private byte[] payload;

    // --------------------------------GETTERS---------------------------------

    private transient int _payloadLength;

    private transient String _payloadString;

    private WebSocketFrame(OpCode opCode, boolean fin) {
        setOpCode(opCode);
        setFin(fin);
    }

    public WebSocketFrame(OpCode opCode, boolean fin, byte[] payload) {
        this(opCode, fin, payload, null);
    }

    public WebSocketFrame(OpCode opCode, boolean fin, byte[] payload, byte[] maskingKey) {
        this(opCode, fin);
        setMaskingKey(maskingKey);
        setBinaryPayload(payload);
    }

    public WebSocketFrame(OpCode opCode, boolean fin, String payload) throws CharacterCodingException {
        this(opCode, fin, payload, null);
    }

    public WebSocketFrame(OpCode opCode, boolean fin, String payload, byte[] maskingKey) throws CharacterCodingException {
        this(opCode, fin);
        setMaskingKey(maskingKey);
        setTextPayload(payload);
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

    public WebSocketFrame(WebSocketFrame clone) {
        setOpCode(clone.getOpCode());
        setFin(clone.isFin());
        setBinaryPayload(clone.getBinaryPayload());
        setMaskingKey(clone.getMaskingKey());
    }

    public byte[] getBinaryPayload() {
        return this.payload;
    }

    public byte[] getMaskingKey() {
        return this.maskingKey;
    }

    public OpCode getOpCode() {
        return this.opCode;
    }

    // --------------------------------SERIALIZATION---------------------------

    public String getTextPayload() {
        if (this._payloadString == null) {
            try {
                this._payloadString = binary2Text(getBinaryPayload());
            } catch (CharacterCodingException e) {
                throw new RuntimeException("Undetected CharacterCodingException", e);
            }
        }
        return this._payloadString;
    }

    public boolean isFin() {
        return this.fin;
    }

    public boolean isMasked() {
        return this.maskingKey != null && this.maskingKey.length == 4;
    }

    private String payloadToString() {
        if (this.payload == null) {
            return "null";
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append('[').append(this.payload.length).append("b] ");
            if (getOpCode() == OpCode.Text) {
                String text = getTextPayload();
                if (text.length() > 100) {
                    sb.append(text.substring(0, 100)).append("...");
                } else {
                    sb.append(text);
                }
            } else {
                sb.append("0x");
                for (int i = 0; i < Math.min(this.payload.length, 50); ++i) {
                    sb.append(Integer.toHexString(this.payload[i] & 0xFF));
                }
                if (this.payload.length > 50) {
                    sb.append("...");
                }
            }
            return sb.toString();
        }
    }

    private void readPayload(InputStream in) throws IOException {
        this.payload = new byte[this._payloadLength];
        int read = 0;
        while (read < this._payloadLength) {
            read += checkedRead(in.read(this.payload, read, this._payloadLength - read));
        }

        if (isMasked()) {
            for (int i = 0; i < this.payload.length; i++) {
                this.payload[i] ^= this.maskingKey[i % 4];
            }
        }

        // Test for Unicode errors
        if (getOpCode() == OpCode.Text) {
            this._payloadString = binary2Text(getBinaryPayload());
        }
    }

    // --------------------------------ENCODING--------------------------------

    private void readPayloadInfo(InputStream in) throws IOException {
        byte b = (byte) checkedRead(in.read());
        boolean masked = (b & 0x80) != 0;

        this._payloadLength = (byte) (0x7F & b);
        if (this._payloadLength == 126) {
            // checkedRead must return int for this to work
            this._payloadLength = (checkedRead(in.read()) << 8 | checkedRead(in.read())) & 0xFFFF;
            if (this._payloadLength < 126) {
                throw new WebSocketException(CloseCode.ProtocolError, "Invalid data frame 2byte length. (not using minimal length encoding)");
            }
        } else if (this._payloadLength == 127) {
            long _payloadLength =
                    (long) checkedRead(in.read()) << 56 | (long) checkedRead(in.read()) << 48 | (long) checkedRead(in.read()) << 40 | (long) checkedRead(in.read()) << 32
                            | checkedRead(in.read()) << 24 | checkedRead(in.read()) << 16 | checkedRead(in.read()) << 8 | checkedRead(in.read());
            if (_payloadLength < 65536) {
                throw new WebSocketException(CloseCode.ProtocolError, "Invalid data frame 4byte length. (not using minimal length encoding)");
            }
            if (_payloadLength < 0 || _payloadLength > Integer.MAX_VALUE) {
                throw new WebSocketException(CloseCode.MessageTooBig, "Max frame length has been exceeded.");
            }
            this._payloadLength = (int) _payloadLength;
        }

        if (this.opCode.isControlFrame()) {
            if (this._payloadLength > 125) {
                throw new WebSocketException(CloseCode.ProtocolError, "Control frame with payload length > 125 bytes.");
            }
            if (this.opCode == OpCode.Close && this._payloadLength == 1) {
                throw new WebSocketException(CloseCode.ProtocolError, "Received close frame with payload len 1.");
            }
        }

        if (masked) {
            this.maskingKey = new byte[4];
            int read = 0;
            while (read < this.maskingKey.length) {
                read += checkedRead(in.read(this.maskingKey, read, this.maskingKey.length - read));
            }
        }
    }

    public void setBinaryPayload(byte[] payload) {
        this.payload = payload;
        this._payloadLength = payload.length;
        this._payloadString = null;
    }

    public void setFin(boolean fin) {
        this.fin = fin;
    }

    public void setMaskingKey(byte[] maskingKey) {
        if (maskingKey != null && maskingKey.length != 4) {
            throw new IllegalArgumentException("MaskingKey " + Arrays.toString(maskingKey) + " hasn't length 4");
        }
        this.maskingKey = maskingKey;
    }

    public void setOpCode(OpCode opcode) {
        this.opCode = opcode;
    }

    public void setTextPayload(String payload) throws CharacterCodingException {
        this.payload = text2Binary(payload);
        this._payloadLength = payload.length();
        this._payloadString = payload;
    }

    // --------------------------------CONSTANTS-------------------------------

    public void setUnmasked() {
        setMaskingKey(null);
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

    // ------------------------------------------------------------------------

    public void write(OutputStream out) throws IOException {
        byte header = 0;
        if (this.fin) {
            header |= 0x80;
        }
        header |= this.opCode.getValue() & 0x0F;
        out.write(header);

        this._payloadLength = getBinaryPayload().length;
        if (this._payloadLength <= 125) {
            out.write(isMasked() ? 0x80 | (byte) this._payloadLength : (byte) this._payloadLength);
        } else if (this._payloadLength <= 0xFFFF) {
            out.write(isMasked() ? 0xFE : 126);
            out.write(this._payloadLength >>> 8);
            out.write(this._payloadLength);
        } else {
            out.write(isMasked() ? 0xFF : 127);
            out.write(this._payloadLength >>> 56 & 0); // integer only
                                                       // contains
            // 31 bit
            out.write(this._payloadLength >>> 48 & 0);
            out.write(this._payloadLength >>> 40 & 0);
            out.write(this._payloadLength >>> 32 & 0);
            out.write(this._payloadLength >>> 24);
            out.write(this._payloadLength >>> 16);
            out.write(this._payloadLength >>> 8);
            out.write(this._payloadLength);
        }

        if (isMasked()) {
            out.write(this.maskingKey);
            for (int i = 0; i < this._payloadLength; i++) {
                out.write(getBinaryPayload()[i] ^ this.maskingKey[i % 4]);
            }
        } else {
            out.write(getBinaryPayload());
        }
        out.flush();
    }
}
