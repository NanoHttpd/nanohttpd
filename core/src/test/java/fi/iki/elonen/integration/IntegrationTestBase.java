package fi.iki.elonen.integration;

import fi.iki.elonen.NanoHTTPD;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com)
 *         On: 9/2/13 at 10:02 PM
 */
public abstract class IntegrationTestBase<T extends NanoHTTPD> {
    protected DefaultHttpClient httpclient;
    protected T testServer;

    @Before
    public void setUp() {
        testServer = createTestServer();
        httpclient = new DefaultHttpClient();
        try {
            testServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        httpclient.getConnectionManager().shutdown();
        testServer.stop();
    }

    public abstract T createTestServer();
}
