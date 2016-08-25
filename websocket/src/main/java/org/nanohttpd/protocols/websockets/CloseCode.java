package org.nanohttpd.protocols.websockets;

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
