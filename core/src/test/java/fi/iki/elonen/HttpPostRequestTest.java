package fi.iki.elonen;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;

public class HttpPostRequestTest extends HttpServerTest {

    public static final String CONTENT_LENGTH = "Content-Length: ";
    public static final String FIELD = "caption";
    public static final String VALUE = "Summer vacation";
    public static final String FIELD2 = "location";
    public static final String VALUE2 = "Grand Canyon";

    @Test
    public void testSimplePostWithSingleMultipartFormField() throws Exception {
        String divider = UUID.randomUUID().toString();
        String header = "POST " + URI + " HTTP/1.1\nContent-Type: " +
                "multipart/form-data; boundary=" + divider + "\n";
        String content = "--" + divider + "\n" +
                "Content-Disposition: form-data; name=\""+FIELD+"\"\n" +
                "\n" +
                VALUE +"\n" +
                "--" + divider + "--\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + CONTENT_LENGTH.length();
        String input = header + CONTENT_LENGTH + (contentLength+4) + "\r\n\r\n" + content;
        invokeServer(input);

        assertEquals(1, testServer.parms.size());
        assertEquals(VALUE, testServer.parms.get(FIELD));
    }

    @Test
    public void testPostWithMultipleMultipartFormFields() throws Exception {
        String divider = UUID.randomUUID().toString();
        String header = "POST " + URI + " HTTP/1.1\nContent-Type: " +
                "multipart/form-data; boundary=" + divider + "\n";
        String content = "--" + divider + "\n" +
                "Content-Disposition: form-data; name=\""+FIELD+"\"\n" +
                "\n" +
                VALUE +"\n" +"--" + divider + "\n" +
                "Content-Disposition: form-data; name=\""+FIELD2+"\"\n" +
                "\n" +
                VALUE2 +"\n" +
                "--" + divider + "--\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + CONTENT_LENGTH.length();
        String input = header + CONTENT_LENGTH + (contentLength+4) + "\r\n\r\n" + content;
        invokeServer(input);

        assertEquals(2, testServer.parms.size());
        assertEquals(VALUE, testServer.parms.get(FIELD));
        assertEquals(VALUE2, testServer.parms.get(FIELD2));
    }

    @Test
    public void testPostWithMultipleMultipartFormFieldsWhereContentTypeWasSeparatedByComma() throws Exception {
        String divider = UUID.randomUUID().toString();
        String header = "POST " + URI + " HTTP/1.1\nContent-Type: " +
                "multipart/form-data, boundary=" + divider + "\n";
        String content = "--" + divider + "\n" +
                "Content-Disposition: form-data; name=\""+FIELD+"\"\n" +
                "\n" +
                VALUE +"\n" +"--" + divider + "\n" +
                "Content-Disposition: form-data; name=\""+FIELD2+"\"\n" +
                "\n" +
                VALUE2 +"\n" +
                "--" + divider + "--\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + CONTENT_LENGTH.length();
        String input = header + CONTENT_LENGTH + (contentLength+4) + "\r\n\r\n" + content;
        invokeServer(input);

        assertEquals(2, testServer.parms.size());
        assertEquals(VALUE, testServer.parms.get(FIELD));
        assertEquals(VALUE2, testServer.parms.get(FIELD2));
    }

    @Test
    public void testPostWithMultipartFormUpload() throws Exception {
        String divider = UUID.randomUUID().toString();
        String filename = "GrandCanyon.txt";
        String fileContent = VALUE;
        String header = "POST " + URI + " HTTP/1.1\nContent-Type: " +
                "multipart/form-data, boundary=" + divider + "\n";
        String content = "--" + divider + "\n" +
                "Content-Disposition: form-data; name=\""+FIELD+"\"; filename=\""+filename+"\"\n" +
                "Content-Type: image/jpeg\r\n"+
                "\r\n" +
                fileContent +"\r\n" +
                "--" + divider + "--\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + CONTENT_LENGTH.length();
        String input = header + CONTENT_LENGTH + (contentLength+5) + "\r\n\r\n" + content;

        invokeServer(input);

        assertEquals(1, testServer.parms.size());
        BufferedReader reader = new BufferedReader(new FileReader(testServer.files.get(FIELD)));
        List<String> lines = readLinesFromFile(reader);
        assertLinesOfText(new String[]{fileContent}, lines);
    }
}
