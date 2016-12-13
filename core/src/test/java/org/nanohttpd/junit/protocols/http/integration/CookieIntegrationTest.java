package org.nanohttpd.junit.protocols.http.integration;

/*
 * #%L
 * NanoHttpd-Core
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.Test;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.content.Cookie;
import org.nanohttpd.protocols.http.content.CookieHandler;
import org.nanohttpd.protocols.http.response.Response;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com) On: 9/2/13 at 10:10 PM
 */
public class CookieIntegrationTest extends IntegrationTestBase<CookieIntegrationTest.CookieTestServer> {

    public static class CookieTestServer extends NanoHTTPD {

        List<Cookie> cookiesReceived = new ArrayList<Cookie>();

        List<Cookie> cookiesToSend = new ArrayList<Cookie>();

        public CookieTestServer() {
            super(8192);
        }

        @Override
        public Response serve(IHTTPSession session) {
            CookieHandler cookies = session.getCookies();
            for (String cookieName : cookies) {
                this.cookiesReceived.add(new Cookie(cookieName, cookies.read(cookieName)));
            }
            for (Cookie c : this.cookiesToSend) {
                cookies.set(c);
            }
            return Response.newFixedLengthResponse("Cookies!");
        }
    }

    @Override
    public CookieTestServer createTestServer() {
        return new CookieTestServer();
    }

    @Test
    public void testCookieSentBackToClient() throws Exception {
        this.testServer.cookiesToSend.add(new Cookie("name", "value", 30));
        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        this.httpclient.execute(httpget, responseHandler);

        CookieStore cookies = this.httpclient.getCookieStore();
        assertEquals(1, cookies.getCookies().size());
        assertEquals("name", cookies.getCookies().get(0).getName());
        assertEquals("value", cookies.getCookies().get(0).getValue());
    }

    @Test
    public void testMultipleCookieSentBackToClient() throws Exception {
        this.testServer.cookiesToSend.add(new Cookie("name0", "value0", 30));
        this.testServer.cookiesToSend.add(new Cookie("name1", "value1", 30));
        this.testServer.cookiesToSend.add(new Cookie("name2", "value2", 30));
        this.testServer.cookiesToSend.add(new Cookie("name3", "value3", 30));
        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        this.httpclient.execute(httpget, responseHandler);

        assertEquals(4, this.httpclient.getCookieStore().getCookies().size());
    }

    @Test
    public void testNoCookies() throws Exception {
        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        this.httpclient.execute(httpget, responseHandler);

        CookieStore cookies = this.httpclient.getCookieStore();
        assertEquals(0, cookies.getCookies().size());
    }

    @Test
    public void testServerReceivesCookiesSentFromClient() throws Exception {
        BasicClientCookie clientCookie = new BasicClientCookie("name", "value");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 100);
        clientCookie.setExpiryDate(calendar.getTime());
        clientCookie.setDomain("localhost");
        this.httpclient.getCookieStore().addCookie(clientCookie);
        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        this.httpclient.execute(httpget, responseHandler);

        assertEquals(1, this.testServer.cookiesReceived.size());
        assertTrue(this.testServer.cookiesReceived.get(0).getHTTPHeader().contains("name=value"));
    }

    @Test
    public void testServerReceivesMultipleCookiesSentFromClient() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 100);
        Date date = calendar.getTime();
        BasicClientCookie clientCookie0 = new BasicClientCookie("name0", "value0");
        BasicClientCookie clientCookie1 = new BasicClientCookie("name1", "value1");
        BasicClientCookie clientCookie2 = new BasicClientCookie("name2", "value2");
        BasicClientCookie clientCookie3 = new BasicClientCookie("name3", "value3");
        clientCookie0.setExpiryDate(date);
        clientCookie0.setDomain("localhost");
        clientCookie1.setExpiryDate(date);
        clientCookie1.setDomain("localhost");
        clientCookie2.setExpiryDate(date);
        clientCookie2.setDomain("localhost");
        clientCookie3.setExpiryDate(date);
        clientCookie3.setDomain("localhost");
        this.httpclient.getCookieStore().addCookie(clientCookie0);
        this.httpclient.getCookieStore().addCookie(clientCookie1);
        this.httpclient.getCookieStore().addCookie(clientCookie2);
        this.httpclient.getCookieStore().addCookie(clientCookie3);
        HttpGet httpget = new HttpGet("http://localhost:8192/");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        this.httpclient.execute(httpget, responseHandler);

        assertEquals(4, this.testServer.cookiesReceived.size());
    }
}
