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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

public abstract class WebSocket {

    private final InputStream in;

    private OutputStream out;

    private OpCode continuousOpCode = null;

    private final List<WebSocketFrame> continuousFrames = new LinkedList<WebSocketFrame>();

    private State state = State.UNCONNECTED;

    protected boolean enforceNoGzip = true;

    private final IHTTPSession handshakeRequest;

    private final Response handshakeResponse = new Response(Status.SWITCH_PROTOCOL, null, (InputStream) null, 0) {

        @Override
        public void send(OutputStream out) {
            WebSocket.this.out = out;
            WebSocket.this.state = State.CONNECTING;
            super.send(out);
            WebSocket.this.state = State.OPEN;
            WebSocket.this.onOpen();
            readWebsocket();
        }
    };

    public WebSocket(IHTTPSession handshakeRequest) {
        this.handshakeRequest = handshakeRequest;
        this.in = handshakeRequest.getInputStream();
        if (enforceNoGzip)
            handshakeResponse.setUseGzip(false);
        this.handshakeResponse.addHeader(NanoWSD.HEADER_UPGRADE, NanoWSD.HEADER_UPGRADE_VALUE);
        this.handshakeResponse.addHeader(NanoWSD.HEADER_CONNECTION, NanoWSD.HEADER_CONNECTION_VALUE);
    }

    public boolean isOpen() {
        return state == State.OPEN;
    }

    protected abstract void onOpen();

    protected abstract void onClose(CloseCode code, String reason, boolean initiatedByRemote);

    protected abstract void onMessage(WebSocketFrame message);

    protected abstract void onPong(WebSocketFrame pong);

    protected abstract void onException(IOException exception);

    /**
     * Debug method. <b>Do not Override unless for debug purposes!</b>
     * 
     * @param frame
     *            The received WebSocket Frame.
     */
    protected void debugFrameReceived(WebSocketFrame frame) {
    }

    /**
     * Debug method. <b>Do not Override unless for debug purposes!</b><br>
     * This method is called before actually sending the frame.
     * 
     * @param frame
     *            The sent WebSocket Frame.
     */
    protected void debugFrameSent(WebSocketFrame frame) {
    }

    public void close(CloseCode code, String reason, boolean initiatedByRemote) throws IOException {
        State oldState = this.state;
        this.state = State.CLOSING;
        if (oldState == State.OPEN) {
            sendFrame(new CloseFrame(code, reason));
        } else {
            doClose(code, reason, initiatedByRemote);
        }
    }

    private void doClose(CloseCode code, String reason, boolean initiatedByRemote) {
        if (this.state == State.CLOSED) {
            return;
        }
        if (this.in != null) {
            try {
                this.in.close();
            } catch (IOException e) {
                NanoWSD.LOG.log(Level.FINE, "close failed", e);
            }
        }
        if (this.out != null) {
            try {
                this.out.close();
            } catch (IOException e) {
                NanoWSD.LOG.log(Level.FINE, "close failed", e);
            }
        }
        this.state = State.CLOSED;
        onClose(code, reason, initiatedByRemote);
    }

    // --------------------------------IO--------------------------------------

    public IHTTPSession getHandshakeRequest() {
        return this.handshakeRequest;
    }

    public Response getHandshakeResponse() {
        return this.handshakeResponse;
    }

    private void handleCloseFrame(WebSocketFrame frame) throws IOException {
        CloseCode code = CloseCode.NormalClosure;
        String reason = "";
        if (frame instanceof CloseFrame) {
            code = ((CloseFrame) frame).getCloseCode();
            reason = ((CloseFrame) frame).getCloseReason();
        }
        if (this.state == State.CLOSING) {
            // Answer for my requested close
            doClose(code, reason, false);
        } else {
            close(code, reason, true);
        }
    }

    private void handleFrameFragment(WebSocketFrame frame) throws IOException {
        if (frame.getOpCode() != OpCode.Continuation) {
            // First
            if (this.continuousOpCode != null) {
                throw new WebSocketException(CloseCode.ProtocolError, "Previous continuous frame sequence not completed.");
            }
            this.continuousOpCode = frame.getOpCode();
            this.continuousFrames.clear();
            this.continuousFrames.add(frame);
        } else if (frame.isFin()) {
            // Last
            if (this.continuousOpCode == null) {
                throw new WebSocketException(CloseCode.ProtocolError, "Continuous frame sequence was not started.");
            }
            this.continuousFrames.add(frame);
            onMessage(new WebSocketFrame(this.continuousOpCode, this.continuousFrames));
            this.continuousOpCode = null;
            this.continuousFrames.clear();
        } else if (this.continuousOpCode == null) {
            // Unexpected
            throw new WebSocketException(CloseCode.ProtocolError, "Continuous frame sequence was not started.");
        } else {
            // Intermediate
            this.continuousFrames.add(frame);
        }
    }

    private void handleWebsocketFrame(WebSocketFrame frame) throws IOException {
        debugFrameReceived(frame);
        if (frame.getOpCode() == OpCode.Close) {
            handleCloseFrame(frame);
        } else if (frame.getOpCode() == OpCode.Ping) {
            sendFrame(new WebSocketFrame(OpCode.Pong, true, frame.getBinaryPayload()));
        } else if (frame.getOpCode() == OpCode.Pong) {
            onPong(frame);
        } else if (!frame.isFin() || frame.getOpCode() == OpCode.Continuation) {
            handleFrameFragment(frame);
        } else if (this.continuousOpCode != null) {
            throw new WebSocketException(CloseCode.ProtocolError, "Continuous frame sequence not completed.");
        } else if (frame.getOpCode() == OpCode.Text || frame.getOpCode() == OpCode.Binary) {
            onMessage(frame);
        } else {
            throw new WebSocketException(CloseCode.ProtocolError, "Non control or continuous frame expected.");
        }
    }

    // --------------------------------Close-----------------------------------

    public void ping(byte[] payload) throws IOException {
        sendFrame(new WebSocketFrame(OpCode.Ping, true, payload));
    }

    // --------------------------------Public
    // Facade---------------------------

    private void readWebsocket() {
        try {
            while (this.state == State.OPEN) {
                handleWebsocketFrame(WebSocketFrame.read(this.in));
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

    public void send(byte[] payload) throws IOException {
        sendFrame(new WebSocketFrame(OpCode.Binary, true, payload));
    }

    public void send(String payload) throws IOException {
        sendFrame(new WebSocketFrame(OpCode.Text, true, payload));
    }

    public synchronized void sendFrame(WebSocketFrame frame) throws IOException {
        debugFrameSent(frame);
        frame.write(this.out);
    }
}
