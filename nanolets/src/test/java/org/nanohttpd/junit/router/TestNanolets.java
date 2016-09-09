package org.nanohttpd.junit.router;

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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.router.RouterNanoHTTPD;
import org.nanohttpd.router.RouterNanoHTTPD.DefaultRoutePrioritizer;
import org.nanohttpd.router.RouterNanoHTTPD.Error404UriHandler;
import org.nanohttpd.router.RouterNanoHTTPD.GeneralHandler;
import org.nanohttpd.router.RouterNanoHTTPD.IndexHandler;
import org.nanohttpd.router.RouterNanoHTTPD.InsertionOrderRoutePrioritizer;
import org.nanohttpd.router.RouterNanoHTTPD.NotImplementedHandler;
import org.nanohttpd.router.RouterNanoHTTPD.ProvidedPriorityRoutePrioritizer;
import org.nanohttpd.router.RouterNanoHTTPD.StaticPageHandler;
import org.nanohttpd.router.RouterNanoHTTPD.UriResource;
import org.nanohttpd.router.RouterNanoHTTPD.UriResponder;
import org.nanohttpd.router.RouterNanoHTTPD.UriRouter;

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
        Thread.sleep(200);
    }

    public static void main(String[] args) {
        {
            String uri = "def";
            Pattern.compile("([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=]+)");
            Pattern URI_PATTERN = Pattern.compile("([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=]+)");
            System.out.println(URI_PATTERN.matcher(uri).matches());
        }

        String uri = "photos/abc/def";
        Pattern URI_PATTERN = Pattern.compile("photos/([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=]+)/([A-Za-z0-9\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=]+)");
        Matcher matcher = URI_PATTERN.matcher(uri);
        System.out.println("--------------->" + "/" + uri);
        while (matcher.matches()) {

            System.out.println(matcher.group());
        }
        // for (int index = 0; index < matcher.groupCount(); index++) {
        // System.out.println(matcher.group());
        // }
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
    public void doEncodedRequest() throws ClientProtocolException, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://localhost:9090/general/param%201/param%202");
        CloseableHttpResponse response = httpclient.execute(httpget);
        Assert.assertEquals(Status.OK.getRequestStatus(), response.getStatusLine().getStatusCode());
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
        Assert.assertEquals("Error: java.lang.InstantiationException : org.nanohttpd.router.RouterNanoHTTPD$UriResponder", string);
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

    @Test(expected = IllegalStateException.class)
    public void illegalMethod3() throws Exception {
        new RouterNanoHTTPD.StaticPageHandler().getText();
    }

    @Test(expected = IllegalStateException.class)
    public void illegalMethod4() throws Exception {
        new RouterNanoHTTPD.StaticPageHandler().getMimeType();
    }

    @Test(expected = ClassCastException.class)
    public void checkIniParameter1() throws Exception {
        new RouterNanoHTTPD.UriResource("browse", 100, null, "init").initParameter(String.class);
        new RouterNanoHTTPD.UriResource("browse", 100, null, "init").initParameter(Integer.class);
    }

    @Test
    public void checkIniParameter2() throws Exception {
        Assert.assertEquals("init", new RouterNanoHTTPD.UriResource("browse", 100, null, "init").initParameter(String.class));
        Assert.assertNull(new RouterNanoHTTPD.UriResource("browse", 100, null).initParameter(String.class));
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
                "UrlResource{uri='photos/:customer_id/:photo_id', urlParts=[customer_id, photo_id]}",//
                new UriResource("/photos/:customer_id/:photo_id", 100, GeneralHandler.class).toString());
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

    @Test
    public void staticFiles() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpTrace httphead = new HttpTrace("http://localhost:9090/browse/blabla.html");
        CloseableHttpResponse response = httpclient.execute(httphead);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h3>just a page</h3></body></html>", string);
        response.close();

        httphead = new HttpTrace("http://localhost:9090/browse/dir/blabla.html");
        response = httpclient.execute(httphead);
        entity = response.getEntity();
        string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h3>just an other page</h3></body></html>", string);
        response.close();

        httphead = new HttpTrace("http://localhost:9090/browse/dir/nanohttpd_logo.png");
        response = httpclient.execute(httphead);
        entity = response.getEntity();
        Assert.assertEquals("image/png", entity.getContentType().getValue());
        response.close();

        httphead = new HttpTrace("http://localhost:9090/browse/dir/xxx.html");
        response = httpclient.execute(httphead);
        entity = response.getEntity();
        string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h3>Error 404: the requested page doesn't exist.</h3></body></html>", string);
        response.close();

        httphead = new HttpTrace("http://localhost:9090/browse/dir/");
        response = httpclient.execute(httphead);
        entity = response.getEntity();
        string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("<html><body><h3>just an index page</h3></body></html>", string);
        response.close();

        httphead = new HttpTrace("http://localhost:9090/browse/exception.html");
        response = httpclient.execute(httphead);
        Assert.assertEquals(Status.REQUEST_TIMEOUT.getRequestStatus(), response.getStatusLine().getStatusCode());
        entity = response.getEntity();
        string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals("", string);
        response.close();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stdIn.write("\n\n".getBytes());
        serverStartThread.join(2000);
        Assert.assertFalse(serverStartThread.isAlive());
    }

    @Test
    public void testGeneralHandlerGetStatus() {
        Assert.assertEquals("GeneralHandler#getStatus should return OK status", Status.OK, new RouterNanoHTTPD.GeneralHandler().getStatus());
    }

    @Test
    public void testStaticPageHandlerGetStatus() {
        Assert.assertEquals("StaticPageHandler#getStatus should return OK status", Status.OK, new RouterNanoHTTPD.StaticPageHandler().getStatus());
    }

    @Test
    public void testError404UriHandlerGetStatus() {
        Assert.assertEquals("Error404UriHandler#getStatus should return NOT_FOUND status", Status.NOT_FOUND, new RouterNanoHTTPD.Error404UriHandler().getStatus());
    }

    @Test
    public void testError404UriHandlerGetMimeType() {
        Assert.assertEquals("Error404UriHandler mime type should be text/html", "text/html", new RouterNanoHTTPD.Error404UriHandler().getMimeType());
    }

    @Test
    public void testNotImplementedHandlerGetStatus() {
        Assert.assertEquals("NotImplementedHandler#getStatus should return OK status", Status.OK, new RouterNanoHTTPD.NotImplementedHandler().getStatus());
    }

    @Test
    public void testIndexHandlerGetStatus() {
        Assert.assertEquals("IndexHandler#getStatus should return OK status", Status.OK, new RouterNanoHTTPD.IndexHandler().getStatus());
    }

    @Test
    public void testIndexHandlerGetMimeType() {
        Assert.assertEquals("IndexHandler mime type should be text/html", "text/html", new RouterNanoHTTPD.IndexHandler().getMimeType());
    }

    @Test
    public void testNotImplementedHandlerGetMimeType() {
        Assert.assertEquals("NotImplementedHandler mime type should be text/html", "text/html", new RouterNanoHTTPD.NotImplementedHandler().getMimeType());
    }

    @Test
    public void testBaseRoutePrioritizerAddNullRoute() {
        DefaultRoutePrioritizer routePrioritizer = new DefaultRoutePrioritizer();
        routePrioritizer.addRoute(null, 100, null);
        Assert.assertEquals(0, routePrioritizer.getPrioritizedRoutes().size());
    }

    @Test
    public void testInsertionOrderRoutePrioritizer() throws IOException {
        InsertionOrderRoutePrioritizer routePrioritizer = new InsertionOrderRoutePrioritizer();

        Class<?> handler1 = String.class;
        Class<?> handler2 = Boolean.class;
        Class<?> handler3 = Long.class;

        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(handler1);
        classes.add(handler2);
        classes.add(handler3);

        routePrioritizer.addRoute("/user", 100, handler1);
        routePrioritizer.addRoute("/user", 100, handler2);
        routePrioritizer.addRoute("/user", 100, handler3);
        List<UriResource> prioritizedResources = new ArrayList<UriResource>();
        prioritizedResources.addAll(routePrioritizer.getPrioritizedRoutes());

        for (int i = 0; i < classes.size(); i++) {
            Class<?> handler = classes.get(i);
            UriResource resource = prioritizedResources.get(i);

            InputStream inputStream = resource.process(null, null).getData();
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            String message = new String(bytes);

            Assert.assertTrue(message.contains(handler.getCanonicalName()));

        }
    }

    @Test
    public void testProvidedPriorityRoutePrioritizerNullUri() {
        ProvidedPriorityRoutePrioritizer routePrioritizer = new ProvidedPriorityRoutePrioritizer();
        Assert.assertEquals(0, routePrioritizer.getPrioritizedRoutes().size());
        routePrioritizer.addRoute(null, 100, null);
        Assert.assertEquals(0, routePrioritizer.getPrioritizedRoutes().size());
    }

    @Test
    public void testProvidedPriorityRoutePrioritizerNullHandler() {
        ProvidedPriorityRoutePrioritizer routePrioritizer = new ProvidedPriorityRoutePrioritizer();
        Assert.assertEquals(0, routePrioritizer.getPrioritizedRoutes().size());
        routePrioritizer.addRoute("/help", 100, null);
        Assert.assertEquals(1, routePrioritizer.getPrioritizedRoutes().size());
    }

    @Test
    public void testProvidedPriorityRoutePrioritizer() throws IOException {
        ProvidedPriorityRoutePrioritizer routePrioritizer = new ProvidedPriorityRoutePrioritizer();

        Class<?> handler1 = String.class;
        Class<?> handler2 = Boolean.class;
        Class<?> handler3 = Long.class;

        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(handler2);
        classes.add(handler1);
        classes.add(handler3);

        routePrioritizer.addRoute("/user", 101, handler1);
        routePrioritizer.addRoute("/user", 100, handler2);
        routePrioritizer.addRoute("/user", 102, handler3);
        List<UriResource> prioritizedResources = new ArrayList<UriResource>();
        prioritizedResources.addAll(routePrioritizer.getPrioritizedRoutes());

        for (int i = 0; i < classes.size(); i++) {
            Class<?> handler = classes.get(i);
            UriResource resource = prioritizedResources.get(i);

            InputStream inputStream = resource.process(null, null).getData();
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            String message = new String(bytes);

            Assert.assertTrue(message.contains(handler.getCanonicalName()));
        }
    }

    @Test
    public void testUriResourceComparator() {
        UriResource r1 = new UriResource("uri", null);
        r1.setPriority(100);
        Assert.assertTrue(r1.compareTo(null) >= 1);

        UriResource r2 = new UriResource("uri", null);
        r2.setPriority(100);
        Assert.assertEquals(0, r1.compareTo(r2));

        r2.setPriority(99);
        Assert.assertTrue(r1.compareTo(r2) >= 1);

        r2.setPriority(101);
        Assert.assertTrue(r1.compareTo(r2) <= 1);
    }

    @Test
    public void testUriResourceMatch() {
        UriResource resource = new RouterNanoHTTPD.UriResource("browse", 100, null, "init");
        Assert.assertNull("UriResource should not match incorrect URL, and thus, should not return a URI parameter map", resource.match("/xyz/pqr/"));
        Assert.assertNotNull("UriResource should match the correct URL, and thus, should return a URI parameter map", resource.match("browse"));
    }

    @Test
    public void testRoutePrioritizerRemoveRouteNoRouteMatches() {
        DefaultRoutePrioritizer prioritizer = new DefaultRoutePrioritizer();
        prioritizer.addRoute("/world", 100, NotImplementedHandler.class);
        prioritizer.removeRoute("/hello");

        Assert.assertEquals(1, prioritizer.getPrioritizedRoutes().size());
    }

    @Test
    public void testHandlerSetters() throws Exception {
        final UriResponder notFoundHandler = new GeneralHandler() {
        };
        final UriResponder notImplementedHandler = new GeneralHandler() {
        };

        TestRouter router = new TestRouter();

        RouterNanoHTTPD routerNanoHttpd = new RouterNanoHTTPD(9999);

        Field routerField = RouterNanoHTTPD.class.getDeclaredField("router");
        routerField.setAccessible(true);
        routerField.set(routerNanoHttpd, router);

        routerNanoHttpd.setNotFoundHandler(notFoundHandler.getClass());
        routerNanoHttpd.setNotImplementedHandler(notImplementedHandler.getClass());

        Assert.assertEquals(notFoundHandler.getClass(), router.notFoundHandlerClass);
        Assert.assertEquals(notImplementedHandler.getClass(), router.notImplementedHandlerClass);
    }

    private static final class TestRouter extends UriRouter {

        private Class<?> notFoundHandlerClass;

        private Class<?> notImplementedHandlerClass;

        @Override
        public void setNotFoundHandler(Class<?> handler) {
            notFoundHandlerClass = handler;
        }

        @Override
        public void setNotImplemented(Class<?> handler) {
            notImplementedHandlerClass = handler;
        }
    }
}
