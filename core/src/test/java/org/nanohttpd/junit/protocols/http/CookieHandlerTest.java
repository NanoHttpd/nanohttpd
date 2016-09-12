package org.nanohttpd.junit.protocols.http;

/*
 * #%L
 * NanoHttpd-Core
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.junit.Test;
import org.nanohttpd.protocols.http.HTTPSession;
import org.nanohttpd.protocols.http.content.CookieHandler;
import org.nanohttpd.protocols.http.response.Response;

public class CookieHandlerTest extends HttpServerTest {

    @Test
    public void testCookieHeaderCorrectlyParsed() throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("GET " + HttpServerTest.URI + " HTTP/1.1").append(System.getProperty("line.separator")).append("Cookie: theme=light; sessionToken=abc123");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBuilder.toString().getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HTTPSession session = this.testServer.createSession(this.tempFileManager, inputStream, outputStream);
        session.execute();
        Set<String> allCookies = new HashSet<String>();
        CookieHandler cookieHandler = session.getCookies();
        for (String cookie : cookieHandler) {
            allCookies.add(cookie);
        }
        assertTrue("cookie specified in header not correctly parsed", allCookies.contains("theme"));
        assertTrue("cookie specified in header not correctly parsed", allCookies.contains("sessionToken"));
        assertEquals("cookie value not correctly parsed", "light", cookieHandler.read("theme"));
        assertEquals("cookie value not correctly parsed", "abc123", cookieHandler.read("sessionToken"));

    }

    @Test
    public void testCookieHeaderWithSpecialCharactersCorrectlyParsed() throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        // not including ; = and ,
        requestBuilder.append("GET " + HttpServerTest.URI + " HTTP/1.1").append(System.getProperty("line.separator"))
                .append("Cookie: theme=light; sessionToken=abc123!@#$%^&*()-_+{}[]\\|:\"'<>.?/");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBuilder.toString().getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HTTPSession session = this.testServer.createSession(this.tempFileManager, inputStream, outputStream);
        session.execute();
        Set<String> allCookies = new HashSet<String>();
        CookieHandler cookieHandler = session.getCookies();
        for (String cookie : cookieHandler) {
            allCookies.add(cookie);
        }
        assertTrue("cookie specified in header not correctly parsed", allCookies.contains("theme"));
        assertTrue("cookie specified in header not correctly parsed", allCookies.contains("sessionToken"));
        assertEquals("cookie value not correctly parsed", "light", cookieHandler.read("theme"));
        assertEquals("cookie value not correctly parsed", "abc123!@#$%^&*()-_+{}[]\\|:\"'<>.?/", cookieHandler.read("sessionToken"));

    }

    @Test
    public void testUnloadQueue() throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("GET " + HttpServerTest.URI + " HTTP/1.1").append(System.getProperty("line.separator")).append("Cookie: theme=light; sessionToken=abc123");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBuilder.toString().getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HTTPSession session = this.testServer.createSession(this.tempFileManager, inputStream, outputStream);
        session.execute();
        CookieHandler cookieHandler = session.getCookies();
        Response response = Response.newFixedLengthResponse("");
        cookieHandler.set("name", "value", 30);
        cookieHandler.unloadQueue(response);
        String setCookieHeader = response.getCookieHeaders().get(0);
        assertTrue("unloadQueue did not set the cookies correctly", setCookieHeader.startsWith("name=value; expires="));
    }

    @Test
    public void testDelete() throws IOException, ParseException {
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("GET " + HttpServerTest.URI + " HTTP/1.1").append(System.getProperty("line.separator")).append("Cookie: theme=light; sessionToken=abc123");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBuilder.toString().getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HTTPSession session = this.testServer.createSession(this.tempFileManager, inputStream, outputStream);
        session.execute();
        CookieHandler cookieHandler = session.getCookies();

        Response response = Response.newFixedLengthResponse("");
        cookieHandler.delete("name");
        cookieHandler.unloadQueue(response);

        String setCookieHeader = response.getCookieHeaders().get(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String dateString = setCookieHeader.split(";")[1].split("=")[1].trim();
        Date date = dateFormat.parse(dateString);
        assertTrue("Deleted cookie's expiry time should be a time in the past", date.compareTo(new Date()) < 0);
    }
}
