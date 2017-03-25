package org.nanohttpd.junit.protocols.websockets;

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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.NanoWSD;
import org.nanohttpd.protocols.websockets.OpCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;
import org.nanohttpd.util.IHandler;

@RunWith(MockitoJUnitRunner.class)
public class WebSocketResponseHandlerTest {

    @Mock
    private IHTTPSession session;

    private MockedWSD nanoWebSocketServer;

    private Map<String, String> headers;

    private static class MockedWSD extends NanoWSD {

        public MockedWSD(int port) {
            super(port);
        }

        public MockedWSD(String hostname, int port) {
            super(hostname, port);
        }

        // This is to work around Mockito being a little bitch.
        public void initialize() {
            interceptors = new ArrayList<IHandler<IHTTPSession, Response>>();
            addHTTPInterceptor(new Interceptor());

            setHTTPHandler(new IHandler<IHTTPSession, Response>() {

                @Override
                public Response handle(IHTTPSession input) {
                    return serve(input);
                }
            });
        }

        @Override
        protected WebSocket openWebSocket(IHTTPSession handshake) {
            return new WebSocket(handshake) { // Dummy websocket inner class.

                @Override
                protected void onPong(WebSocketFrame pong) {
                }

                @Override
                protected void onOpen() {
                }

                @Override
                protected void onMessage(WebSocketFrame message) {
                }

                @Override
                protected void onException(IOException exception) {
                }

                @Override
                protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
                }
            };
        }
    }

    @Before
    public void setUp() {
        // Be careful.
        // This does NOT call any constructors, instead, directly creates
        // the object in memory. I wasted 3 fucking hours attempting to
        // debug this. ~ LordFokas
        this.nanoWebSocketServer = Mockito.mock(MockedWSD.class, Mockito.CALLS_REAL_METHODS);
        // this could have been avoided if Mockito had a way to call fucking
        // constructors!!
        this.nanoWebSocketServer.initialize();

        this.headers = new HashMap<String, String>();
        this.headers.put("upgrade", "websocket");
        this.headers.put("connection", "Upgrade");
        this.headers.put("sec-websocket-key", "x3JJHMbDL1EzLkh9GBhXDw==");
        this.headers.put("sec-websocket-protocol", "chat, superchat");
        this.headers.put("sec-websocket-version", "13");

        when(this.session.getHeaders()).thenReturn(this.headers);
    }

    @Test
    public void testConnectionHeaderHandlesKeepAlive_FixingFirefoxConnectIssue() {
        this.headers.put("connection", "keep-alive, Upgrade");
        Response handshakeResponse = this.nanoWebSocketServer.handle(this.session);

        assertNotNull(handshakeResponse);
    }

    @Test
    public void testHandshakeReturnsResponseWithExpectedHeaders() {
        Response handshakeResponse = this.nanoWebSocketServer.handle(this.session);

        assertNotNull(handshakeResponse);

        assertEquals(handshakeResponse.getHeader(NanoWSD.HEADER_WEBSOCKET_ACCEPT), "HSmrc0sMlYUkAGmm5OPpG2HaGWk=");
        assertEquals(handshakeResponse.getHeader(NanoWSD.HEADER_WEBSOCKET_PROTOCOL), "chat");
    }

    @Test
    public void testMissingKeyReturnsErrorResponse() {
        this.headers.remove("sec-websocket-key");

        Response handshakeResponse = this.nanoWebSocketServer.handle(this.session);

        assertNotNull(handshakeResponse);
        assertEquals(Status.BAD_REQUEST, handshakeResponse.getStatus());
    }

    @Test
    public void testWrongConnectionHeaderReturnsNullResponse() {
        this.headers.put("connection", "Junk");
        Response handshakeResponse = this.nanoWebSocketServer.handle(this.session);
        assertNull(handshakeResponse.getHeader(NanoWSD.HEADER_UPGRADE));
    }

    @Test
    public void testWrongUpgradeHeaderReturnsNullResponse() {
        this.headers.put("upgrade", "not a websocket");
        Response handshakeResponse = this.nanoWebSocketServer.handle(this.session);
        assertNull(handshakeResponse.getHeader(NanoWSD.HEADER_UPGRADE));
    }

    @Test
    public void testWrongWebsocketVersionReturnsErrorResponse() {
        this.headers.put("sec-websocket-version", "12");

        Response handshakeResponse = this.nanoWebSocketServer.handle(this.session);

        assertNotNull(handshakeResponse);
        assertEquals(Status.BAD_REQUEST, handshakeResponse.getStatus());
    }

    @Test
    public void testSetMaskingKeyThrowsExceptionMaskingKeyLengthIsNotFour() {
        WebSocketFrame webSocketFrame = new WebSocketFrame(OpCode.Text, true, new byte[0]);
        for (int maskingKeyLength = 0; maskingKeyLength < 10; maskingKeyLength++) {
            if (maskingKeyLength == 4)
                continue;
            try {
                webSocketFrame.setMaskingKey(new byte[maskingKeyLength]);
                Assert.fail("IllegalArgumentException expected but not thrown");
            } catch (IllegalArgumentException e) {

            }
        }
    }

    @Test
    public void testIsMasked() {
        WebSocketFrame webSocketFrame = new WebSocketFrame(OpCode.Text, true, new byte[0]);
        Assert.assertFalse("isMasked should return true if masking key is not set.", webSocketFrame.isMasked());

        webSocketFrame.setMaskingKey(new byte[4]);
        Assert.assertTrue("isMasked should return true if correct masking key is set.", webSocketFrame.isMasked());

    }

    @Test
    public void testWriteWhenNotMasked() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        WebSocketFrame webSocketFrame = new WebSocketFrame(OpCode.Text, true, "payload".getBytes());
        webSocketFrame.write(byteArrayOutputStream);
        byte[] writtenBytes = byteArrayOutputStream.toByteArray();
        Assert.assertEquals(9, writtenBytes.length);
        Assert.assertEquals("Header byte incorrect.", -127, writtenBytes[0]);
        Assert.assertEquals("Payload length byte incorrect.", 7, writtenBytes[1]);
        Assert.assertArrayEquals(new byte[]{
            -127,
            7,
            112,
            97,
            121,
            108,
            111,
            97,
            100
        }, writtenBytes);
    }

    @Test
    public void testWriteWhenMasked() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        WebSocketFrame webSocketFrame = new WebSocketFrame(OpCode.Binary, true, "payload".getBytes());
        webSocketFrame.setMaskingKey(new byte[]{
            12,
            45,
            33,
            32
        });
        webSocketFrame.write(byteArrayOutputStream);
        byte[] writtenBytes = byteArrayOutputStream.toByteArray();
        Assert.assertEquals(13, writtenBytes.length);
        Assert.assertEquals("Header byte incorrect.", -126, writtenBytes[0]);
        Assert.assertEquals("Payload length byte incorrect.", -121, writtenBytes[1]);
        Assert.assertArrayEquals(new byte[]{
            -126,
            -121,
            12,
            45,
            33,
            32,
            124,
            76,
            88,
            76,
            99,
            76,
            69
        }, writtenBytes);
    }

    @Test
    public void testWriteWhenNotMaskedPayloadLengthGreaterThan125() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        WebSocketFrame webSocketFrame = new WebSocketFrame(OpCode.Ping, true, new byte[257]);
        webSocketFrame.write(byteArrayOutputStream);
        byte[] writtenBytes = byteArrayOutputStream.toByteArray();
        Assert.assertEquals(261, writtenBytes.length);
        Assert.assertEquals("Header byte incorrect.", -119, writtenBytes[0]);
        Assert.assertArrayEquals("Payload length bytes incorrect.", new byte[]{
            126,
            1,
            1
        }, new byte[]{
            writtenBytes[1],
            writtenBytes[2],
            writtenBytes[3]
        });
    }

    @Test
    public void testWriteWhenMaskedPayloadLengthGreaterThan125() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        WebSocketFrame webSocketFrame = new WebSocketFrame(OpCode.Ping, false, new byte[257]);
        webSocketFrame.setMaskingKey(new byte[]{
            19,
            25,
            79,
            11
        });
        webSocketFrame.write(byteArrayOutputStream);
        byte[] writtenBytes = byteArrayOutputStream.toByteArray();
        Assert.assertEquals(265, writtenBytes.length);
        Assert.assertEquals("Header byte incorrect.", 9, writtenBytes[0]);
        Assert.assertArrayEquals("Payload length bytes incorrect.", new byte[]{
            -2,
            1,
            1
        }, new byte[]{
            writtenBytes[1],
            writtenBytes[2],
            writtenBytes[3]
        });
    }

    @Test
    public void testWriteWhenNotMaskedPayloadLengthGreaterThan65535() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        WebSocketFrame webSocketFrame = new WebSocketFrame(OpCode.Ping, true, new byte[65536]);
        webSocketFrame.write(byteArrayOutputStream);
        byte[] writtenBytes = byteArrayOutputStream.toByteArray();
        Assert.assertEquals(65546, writtenBytes.length);
        Assert.assertEquals("Header byte incorrect.", -119, writtenBytes[0]);
        Assert.assertArrayEquals("Payload length bytes incorrect.", new byte[]{
            127,
            0,
            0,
            0,
            0,
            0,
            1,
            0,
            0
        }, Arrays.copyOfRange(writtenBytes, 1, 10));
    }
}
