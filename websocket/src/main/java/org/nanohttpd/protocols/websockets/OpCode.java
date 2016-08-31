package org.nanohttpd.protocols.websockets;

public enum OpCode {
    Continuation(0),
    Text(1),
    Binary(2),
    Close(8),
    Ping(9),
    Pong(10);

    public static OpCode find(byte value) {
        for (OpCode opcode : values()) {
            if (opcode.getValue() == value) {
                return opcode;
            }
        }
        return null;
    }

    private final byte code;

    private OpCode(int code) {
        this.code = (byte) code;
    }

    public byte getValue() {
        return this.code;
    }

    public boolean isControlFrame() {
        return this == Close || this == Ping || this == Pong;
    }
}
