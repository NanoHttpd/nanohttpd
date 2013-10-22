package fi.iki.elonen;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import static org.junit.Assert.assertEquals;

public class HttpSessionHeadersTest extends HttpServerTest {
    private static final String DUMMY_REQUEST_CONTENT = "dummy request content";
    private static final TestTempFileManager TEST_TEMP_FILE_MANAGER = new TestTempFileManager();

    @Override
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testHeadersRemoteIp() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(DUMMY_REQUEST_CONTENT.getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String[] ipAddresses = { "127.0.0.1", "192.168.1.1", "192.30.252.129" };
        for(String ipAddress : ipAddresses) {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            NanoHTTPD.HTTPSession session = testServer.createSession(TEST_TEMP_FILE_MANAGER, inputStream, outputStream, inetAddress);
            assertEquals(ipAddress, session.getHeaders().get("remote-addr"));
            assertEquals(ipAddress, session.getHeaders().get("http-client-ip"));
        }
    }

}
