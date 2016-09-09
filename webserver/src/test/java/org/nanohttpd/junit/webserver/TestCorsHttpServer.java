package org.nanohttpd.junit.webserver;

/*
 * #%L
 * NanoHttpd-Webserver
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

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nanohttpd.webserver.SimpleWebServer;

/**
 * @author Matthieu Brouillard [matthieu@brouillard.fr]
 */
public class TestCorsHttpServer extends AbstractTestHttpServer {

    private static PipedOutputStream stdIn;

    private static Thread serverStartThread;

    @BeforeClass
    public static void setUp() throws Exception {
        stdIn = new PipedOutputStream();
        System.setIn(new PipedInputStream(stdIn));
        serverStartThread = new Thread(new Runnable() {

            @Override
            public void run() {
                String[] args = {
                    "--host",
                    "localhost",
                    "--port",
                    "9090",
                    "--dir",
                    "src/test/resources",
                    "--cors"
                };
                SimpleWebServer.main(args);
            }
        });
        serverStartThread.start();
        // give the server some tine to start.
        Thread.sleep(100);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stdIn.write("\n\n".getBytes());
        serverStartThread.join(2000);
        Assert.assertFalse(serverStartThread.isAlive());
    }

    @Test
    public void doTestOption() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpOptions httpOption = new HttpOptions("http://localhost:9090/xxx/yyy.html");
        CloseableHttpResponse response = httpclient.execute(httpOption);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        Assert.assertNotNull("Cors should have added a header: Access-Control-Allow-Origin", response.getLastHeader("Access-Control-Allow-Origin"));
        Assert.assertEquals("Cors should have added a header: Access-Control-Allow-Origin: *", "*", response.getLastHeader("Access-Control-Allow-Origin").getValue());
        response.close();
    }

    @Test
    public void doSomeBasicTest() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://localhost:9090/testdir/test.html");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");

        Assert.assertNotNull("Cors should have added a header: Access-Control-Allow-Origin", response.getLastHeader("Access-Control-Allow-Origin"));
        Assert.assertEquals("Cors should have added a header: Access-Control-Allow-Origin: *", "*", response.getLastHeader("Access-Control-Allow-Origin").getValue());
        Assert.assertEquals("<html>\n<head>\n<title>dummy</title>\n</head>\n<body>\n\t<h1>it works</h1>\n</body>\n</html>", string);
        response.close();
    }

    @Test
    public void testAccessControlAllowHeaderUsesDefaultsWithoutSystemProperty() throws Exception {
        Assert.assertNull("no System " + SimpleWebServer.ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME + " shoudl be set",
                System.getProperty(SimpleWebServer.ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME));

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpOptions httpOption = new HttpOptions("http://localhost:9090/xxx/yyy.html");
        CloseableHttpResponse response = httpclient.execute(httpOption);
        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        Assert.assertEquals("Cors should have added a header: Access-Control-Allow-Headers: " + SimpleWebServer.DEFAULT_ALLOWED_HEADERS,
                SimpleWebServer.DEFAULT_ALLOWED_HEADERS, response.getLastHeader("Access-Control-Allow-Headers").getValue());
        response.close();
    }

    @Test
    public void testAccessControlAllowHeaderUsesSystemPropertyWhenSet() throws Exception {
        Assert.assertNull("no System " + SimpleWebServer.ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME + " shoudl be set",
                System.getProperty(SimpleWebServer.ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME));

        final String expectedValue = "origin";
        System.setProperty(SimpleWebServer.ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME, expectedValue);

        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpOptions httpOption = new HttpOptions("http://localhost:9090/xxx/yyy.html");
            CloseableHttpResponse response = httpclient.execute(httpOption);
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            Assert.assertEquals("Cors should have added a header: Access-Control-Allow-Headers: " + expectedValue, expectedValue,
                    response.getLastHeader("Access-Control-Allow-Headers").getValue());
            response.close();
        } finally {
            System.clearProperty(SimpleWebServer.ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME);
        }
    }
}
