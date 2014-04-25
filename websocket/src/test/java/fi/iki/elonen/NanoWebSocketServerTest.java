package fi.iki.elonen;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NanoWebSocketServerTest {
    @Mock
    private NanoHTTPD.IHTTPSession session;

    private NanoWebSocketServer server;

    @Before
    public void setUp() {
        server = new NanoWebSocketServer(9090);
    }

    @Test(expected = Error.class)
    public void testMissingResponseFactoryThrowsErrorOnServe() {
        server.openWebSocket(session);
    }

    @Test
    public void testMissingResponseFactoryThrowsErrorWithCorrectMessageOnServe() {
        NanoWebSocketServer server = new NanoWebSocketServer(9090);
        try {
            server.openWebSocket(session);
        } catch (Error e) {
            assertEquals(NanoWebSocketServer.MISSING_FACTORY_MESSAGE, e.getMessage());
        }
    }
}