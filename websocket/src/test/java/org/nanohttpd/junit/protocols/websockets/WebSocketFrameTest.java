package org.nanohttpd.junit.protocols.websockets;

/*
 * #%L
 * NanoHttpd-Websocket
 * %%
 * Copyright (C) 2012 - 2019 nanohttpd
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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.nanohttpd.protocols.websockets.OpCode;
import org.nanohttpd.protocols.websockets.WebSocketException;
import org.nanohttpd.protocols.websockets.WebSocketFrame;

public class WebSocketFrameTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testReadEOFException() throws IOException {

        // Arrange
        final byte[] byteArray = {0};

        final ByteArrayInputStream in = new ByteArrayInputStream(byteArray);

        // Act
        thrown.expect(EOFException.class);
        WebSocketFrame.read(in);

    }

    @Test
    public void testReadWebSocketExceptionMaxFrameLength() throws IOException {

        // Arrange
        final byte[] byteArray = {-128, -1, 0, 0, 0, 1, 47, 120, -16, 16};

        final ByteArrayInputStream in = new ByteArrayInputStream(byteArray);

        // Act
        thrown.expect(WebSocketException.class);
        WebSocketFrame.read(in);

    }

    @Test
    public void testReadWebSocketException4ByteLength() throws Exception {

        // Arrange
        final byte[] byteArray = {-128, -1, -128, 0, 0, 0, 47, 120, -16, 16};

        final ByteArrayInputStream in = new ByteArrayInputStream(byteArray);

        // Act
        thrown.expect(WebSocketException.class);
        WebSocketFrame.read(in);

    }

    @Test
    public void testReadWebSocketException2ByteLength() throws Exception {

        // Arrange
        final byte[] byteArray = {-59, -128, 126, 0, 0, 32, -1, -128, 4, -128};

        final ByteArrayInputStream in = new ByteArrayInputStream(byteArray, 1, 9);

        // Act
        thrown.expect(WebSocketException.class);
        WebSocketFrame.read(in);

    }

    @Test
    public void testReadPayloadInfoReservedBitsNotZero() throws Throwable {

        // Arrange
        final byte[] byteArray = {24, 127, 0, 0, 0, 0, 0, -128, 0, 1};
        final ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
        final ArrayList<WebSocketFrame> fragments = new ArrayList<WebSocketFrame>();

        final WebSocketFrame objectUnderTest = new WebSocketFrame(OpCode.Close, fragments);

        // Act
        thrown.expect(WebSocketException.class);
        objectUnderTest.read(in);

    }

    @Test
    public void testSetTextPayload() throws WebSocketException, CharacterCodingException {

        // Arrange
        final ArrayList<WebSocketFrame> fragments = new ArrayList<WebSocketFrame>();
        final String payload = "foo";

        final WebSocketFrame objectUnderTest = new WebSocketFrame(OpCode.Text, fragments);

        // Act
        objectUnderTest.setTextPayload(payload);

        // Assert act
        Assert.assertEquals(objectUnderTest.getTextPayload(), payload);

    }
}
