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

public enum CloseCode {
    NormalClosure(1000),
    GoingAway(1001),
    ProtocolError(1002),
    UnsupportedData(1003),
    NoStatusRcvd(1005),
    AbnormalClosure(1006),
    InvalidFramePayloadData(1007),
    PolicyViolation(1008),
    MessageTooBig(1009),
    MandatoryExt(1010),
    InternalServerError(1011),
    TLSHandshake(1015);

    public static CloseCode find(int value) {
        for (CloseCode code : values()) {
            if (code.getValue() == value) {
                return code;
            }
        }
        return null;
    }

    private final int code;

    private CloseCode(int code) {
        this.code = code;
    }

    public int getValue() {
        return this.code;
    }
}
