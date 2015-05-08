package fi.iki.elonen;

/*
 * #%L
 * NanoHttpd-Websocket
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.util.LinkedList;
import java.util.List;

import fi.iki.elonen.WebSocketFrame.CloseCode;
import fi.iki.elonen.WebSocketFrame.CloseFrame;
import fi.iki.elonen.WebSocketFrame.OpCode;

public abstract class WebSocket {
    public static enum State {
        UNCONNECTED, CONNECTING, OPEN, CLOSING, CLOSED
    }

    protected InputStream in;

    protected OutputStream out;

    protected WebSocketFrame.OpCode continuousOpCode = null;

    protected List<WebSocketFrame> continuousFrames = new LinkedList<WebSocketFrame>();

    protected State state = State.UNCONNECTED;

    protected final NanoHTTPD.IHTTPSession handshakeRequest;

    protected final NanoHTTPD.Response handshakeResponse = new NanoHTTPD.Response(
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

        handshakeResponse.addHeader(WebSocketResponseHandler.HEADER_UPGRADE,
                WebSocketResponseHandler.HEADER_UPGRADE_VALUE);
        handshakeResponse.addHeader(WebSocketResponseHandler.HEADER_CONNECTION,
                WebSocketResponseHandler.HEADER_CONNECTION_VALUE);
    }

    public NanoHTTPD.IHTTPSession getHandshakeRequest() {
        return handshakeRequest;
    }

    public NanoHTTPD.Response getHandshakeResponse() {
        return handshakeResponse;
    }

    // --------------------------------IO--------------------------------------

    protected void readWebsocket() {
        try {
            while (state == State.OPEN) {
                handleWebsocketFrame(WebSocketFrame.read(in));
            }
        } catch (CharacterCodingException e) {
            onException(e);
            doClose(CloseCode.InvalidFramePayloadData, e.toString(), false);
        } catch (IOException e) {
            onException(e);
            if (e instanceof WebSocketException) {
                doClose(((WebSocketException) e).getCode(), ((WebSocketException) e).getReason(), false);
            }
        } finally {
            doClose(CloseCode.InternalServerError, "Handler terminated without closing the connection.", false);
        }
    }

    protected void handleWebsocketFrame(WebSocketFrame frame) throws IOException {
        if (frame.getOpCode() == OpCode.Close) {
            handleCloseFrame(frame);
        } else if (frame.getOpCode() == OpCode.Ping) {
            sendFrame(new WebSocketFrame(OpCode.Pong, true, frame.getBinaryPayload()));
        } else if (frame.getOpCode() == OpCode.Pong) {
            onPong(frame);
        } else if (!frame.isFin() || frame.getOpCode() == OpCode.Continuation) {
            handleFrameFragment(frame);
        } else if (continuousOpCode != null) {
            throw new WebSocketException(CloseCode.ProtocolError, "Continuous frame sequence not completed.");
        } else if (frame.getOpCode() == OpCode.Text || frame.getOpCode() == OpCode.Binary) {
            onMessage(frame);
        } else {
            throw new WebSocketException(CloseCode.ProtocolError, "Non control or continuous frame expected.");
        }
    }

    protected void handleCloseFrame(WebSocketFrame frame) throws IOException {
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

    protected void handleFrameFragment(WebSocketFrame frame) throws IOException {
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
            onMessage(new WebSocketFrame(continuousOpCode, continuousFrames));
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
        frame.write(out);
    }

    // --------------------------------Close-----------------------------------

    protected void doClose(CloseCode code, String reason, boolean initiatedByRemote) {
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
        onClose(code, reason, initiatedByRemote);
    }

    // --------------------------------Listener--------------------------------

    protected abstract void onPong(WebSocketFrame pongFrame);

    protected abstract void onMessage(WebSocketFrame messageFrame);

    protected abstract void onClose(CloseCode code, String reason, boolean initiatedByRemote);

    protected abstract void onException(IOException e);

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
