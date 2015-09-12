package fi.iki.elonen.router;

/*
 * #%L
 * NanoHttpd nano application server
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

public class TestNanolets {

    private static PipedOutputStream stdIn;

    private static Thread serverStartThread;

    @BeforeClass
    public static void setUp() throws Exception {
        stdIn = new PipedOutputStream();
        System.setIn(new PipedInputStream(stdIn));
        serverStartThread = new Thread(new Runnable() {

            @Override
            public void run() {
                String[] args = {};
                AppNanolets.main(args);
            }
        });
        serverStartThread.start();
        // give the server some tine to start.
        Thread.sleep(100);
    }

    @Test
    public void doSomeBasicMethodTest() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet("http://localhost:9090/user/blabla");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals(
                "<html><body>User handler. Method: GET<br><h1>Uri parameters:</h1><div> Param: id&nbsp;Value: blabla</div><h1>Query parameters:</h1></body></html>", string);
        response.close();

        HttpPost httppost = new HttpPost("http://localhost:9090/user/blabla");
        response = httpclient.execute(httppost);
        entity = response.getEntity();
        string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals(
                "<html><body>User handler. Method: POST<br><h1>Uri parameters:</h1><div> Param: id&nbsp;Value: blabla</div><h1>Query parameters:</h1></body></html>", string);
        response.close();

        HttpPut httpgput = new HttpPut("http://localhost:9090/user/blabla");
        response = httpclient.execute(httpgput);
        entity = response.getEntity();
        string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals(
                "<html><body>User handler. Method: PUT<br><h1>Uri parameters:</h1><div> Param: id&nbsp;Value: blabla</div><h1>Query parameters:</h1></body></html>", string);
        response.close();

        HttpDelete httpdelete = new HttpDelete("http://localhost:9090/user/blabla");
        response = httpclient.execute(httpdelete);
        entity = response.getEntity();
        string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals(
                "<html><body>User handler. Method: DELETE<br><h1>Uri parameters:</h1><div> Param: id&nbsp;Value: blabla</div><h1>Query parameters:</h1></body></html>", string);
        response.close();
    }

    @Test
    public void doNonRouterRequest() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet("http://localhost:9090/test");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("Return: java.lang.String.toString() -> ", string);
        response.close();
    }

    @Test
    public void doExceptionRequest() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet("http://localhost:9090/interface");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("Error: java.lang.InstantiationException : fi.iki.elonen.router.RouterNanoHTTPD$UriResponder", string);
        response.close();
    }

    @Test
    public void doDeletedRoute() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet("http://localhost:9090/toBeDeleted");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h3>Error 404: the requested page doesn't exist.</h3></body></html>", string);
        response.close();
    }

    @Test
    public void doUriSelection1() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet("http://localhost:9090/user/help");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h1>Url: /user/help</h1><br><p>no params in url</p><br>", string);
        response.close();
    }

    @Test
    public void doStreamOfData() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet("http://localhost:9090/stream");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("a stream of data ;-)", string);
        response.close();
    }

    @Test(expected = IllegalStateException.class)
    public void illegalMethod1() throws Exception {
        new AppNanolets.UserHandler().getData();
    }

    @Test(expected = IllegalStateException.class)
    public void illegalMethod2() throws Exception {
        new RouterNanoHTTPD.GeneralHandler().getText();
    }

    @Test
    public void doGeneralParams() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet("http://localhost:9090/general/value1/value2?param3=value3&param4=value4");

        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h1>Url: /general/value1/value2</h1><br><p>Param 'param3' = value3</p><p>Param 'param4' = value4</p>", string);
        response.close();
    }

    @Test
    public void doIndexHandler() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet("http://localhost:9090/index.html");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h2>Hello world!</h3></body></html>", string);
        response.close();
    }

    @Test
    public void doMissingHandler() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet("http://localhost:9090/photos/abc/def");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h2>The uri is mapped in the router, but no handler is specified. <br> Status: Not implemented!</h3></body></html>", string);
        response.close();
    }

    @Test
    public void uriToString() throws Exception {
        Assert.assertEquals(//
                "UrlResource{hasParameters=true, uriParamsCount=2, uri='photos/:customer_id/:photo_id', urlParts=[UriPart{name='photos', isParam=false}, UriPart{name='customer_id', isParam=true}, UriPart{name='photo_id', isParam=true}]}",//
                new UriResource("/photos/:customer_id/:photo_id", GeneralHandler.class).toString());
    }

    @Test
    public void doOtherMethod() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpTrace httphead = new HttpTrace("http://localhost:9090/index.html");
        CloseableHttpResponse response = httpclient.execute(httphead);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h2>Hello world!</h3></body></html>", string);
        response.close();
    }

    @Test
    public void normalize() throws Exception {
        Assert.assertNull(RouterNanoHTTPD.normalizeUri(null));
        Assert.assertEquals("", RouterNanoHTTPD.normalizeUri("/"));
        Assert.assertEquals("xxx/yyy", RouterNanoHTTPD.normalizeUri("/xxx/yyy"));
        Assert.assertEquals("xxx/yyy", RouterNanoHTTPD.normalizeUri("/xxx/yyy/"));
    }

    private byte[] readContents(HttpEntity entity) throws IOException {
        InputStream instream = entity.getContent();
        return readContents(instream);
    }

    private byte[] readContents(InputStream instream) throws IOException {
        byte[] bytes;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = instream.read(buffer)) >= 0) {
                out.write(buffer, 0, count);
            }
            bytes = out.toByteArray();
        } finally {
            instream.close();
        }
        return bytes;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stdIn.write("\n\n".getBytes());
        serverStartThread.join(2000);
        Assert.assertFalse(serverStartThread.isAlive());
    }

}
