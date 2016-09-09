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

import java.nio.charset.CharacterCodingException;

public class CloseFrame extends WebSocketFrame {

    private static byte[] generatePayload(CloseCode code, String closeReason) throws CharacterCodingException {
        if (code != null) {
            byte[] reasonBytes = text2Binary(closeReason);
            byte[] payload = new byte[reasonBytes.length + 2];
            payload[0] = (byte) (code.getValue() >> 8 & 0xFF);
            payload[1] = (byte) (code.getValue() & 0xFF);
            System.arraycopy(reasonBytes, 0, payload, 2, reasonBytes.length);
            return payload;
        } else {
            return new byte[0];
        }
    }

    private CloseCode _closeCode;

    private String _closeReason;

    public CloseFrame(CloseCode code, String closeReason) throws CharacterCodingException {
        super(OpCode.Close, true, generatePayload(code, closeReason));
    }

    public CloseFrame(WebSocketFrame wrap) throws CharacterCodingException {
        super(wrap);
        assert wrap.getOpCode() == OpCode.Close;
        if (wrap.getBinaryPayload().length >= 2) {
            this._closeCode = CloseCode.find((wrap.getBinaryPayload()[0] & 0xFF) << 8 | wrap.getBinaryPayload()[1] & 0xFF);
            this._closeReason = binary2Text(getBinaryPayload(), 2, getBinaryPayload().length - 2);
        }
    }

    public CloseCode getCloseCode() {
        return this._closeCode;
    }

    public String getCloseReason() {
        return this._closeReason;
    }
}
