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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
    public void doSomeBasicTest() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://localhost:9090/user/blabla");
        CloseableHttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        String string = new String(readContents(entity), "UTF-8");
        Assert.assertEquals(
                "<html><body>User handler. Method: GET<br><h1>Uri parameters:</h1><div> Param: id&nbsp;Value: blabla</div><h1>Query parameters:</h1></body></html>", string);
        response.close();
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
