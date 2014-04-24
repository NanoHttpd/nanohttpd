package fi.iki.elonen;

import fi.iki.elonen.WebSocketFrame.CloseCode;

import java.io.IOException;

public class WebSocketException extends IOException {
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
