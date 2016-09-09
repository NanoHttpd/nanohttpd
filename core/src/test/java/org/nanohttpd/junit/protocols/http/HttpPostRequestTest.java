package org.nanohttpd.junit.protocols.http;

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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class HttpPostRequestTest extends HttpServerTest {

    public static final String CONTENT_LENGTH = "Content-Length: ";

    public static final String FIELD = "caption";

    public static final String VALUE = "Summer vacation";

    public static final String FIELD2 = "location";

    public static final String VALUE2 = "Grand Canyon";

    public static final String POST_RAW_CONTENT_FILE_ENTRY = "postData";

    public static final String VALUE_TEST_SIMPLE_RAW_DATA_WITH_AMPHASIS = "Test raw data & Result value";

    /**
     * contains common preparation steps for testing POST with Multipart Form
     * 
     * @param fileName
     *            Name of file to be uploaded
     * @param fileContent
     *            Content of file to be uploaded
     * @return input String with POST request complete information including
     *         header, length and content
     */
    private String preparePostWithMultipartForm(String fileName, String fileContent) {
        String divider = UUID.randomUUID().toString();
        String header = "POST " + HttpServerTest.URI + " HTTP/1.1\nContent-Type: " + "multipart/form-data, boundary=" + divider + "\r\n";
        String content =
                "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\"" + HttpPostRequestTest.FIELD + "\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: image/jpeg\r\n" + "\r\n" + fileContent + "\r\n" + "--" + divider + "--\r\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + HttpPostRequestTest.CONTENT_LENGTH.length();
        String input = header + HttpPostRequestTest.CONTENT_LENGTH + (contentLength + 5) + "\r\n\r\n" + content;

        return input;
    }

    @Test
    public void testPostWithMultipartFormUpload() throws Exception {
        String filename = "GrandCanyon.txt";
        String fileContent = HttpPostRequestTest.VALUE;
        String input = preparePostWithMultipartForm(filename, fileContent);

        invokeServer(input);

        assertEquals(1, this.testServer.parms.size());
        assertEquals(1, this.testServer.parameters.size());
        BufferedReader reader = new BufferedReader(new FileReader(this.testServer.files.get(HttpPostRequestTest.FIELD)));
        List<String> lines = readLinesFromFile(reader);
        assertLinesOfText(new String[]{
            fileContent
        }, lines);
    }

    @Test
    public void testPostWithMultipartFormUploadFilenameHasSpaces() throws Exception {
        String fileNameWithSpace = "Grand Canyon.txt";
        String fileContent = HttpPostRequestTest.VALUE;
        String input = preparePostWithMultipartForm(fileNameWithSpace, fileContent);

        invokeServer(input);

        String fileNameAfter = new ArrayList<String>(this.testServer.parms.values()).get(0);
        assertEquals(fileNameWithSpace, fileNameAfter);

        fileNameAfter = new ArrayList<String>(this.testServer.parameters.values().iterator().next()).get(0);
        assertEquals(fileNameWithSpace, fileNameAfter);
    }

    @Test
    public void testPostWithMultipleMultipartFormFields() throws Exception {
        String divider = UUID.randomUUID().toString();
        String header = "POST " + HttpServerTest.URI + " HTTP/1.1\nContent-Type: " + "multipart/form-data; boundary=" + divider + "\n";
        String content =
                "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\"" + HttpPostRequestTest.FIELD + "\"\r\n" + "\r\n" + HttpPostRequestTest.VALUE + "\r\n"
                        + "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\"" + HttpPostRequestTest.FIELD2 + "\"\r\n" + "\r\n" + HttpPostRequestTest.VALUE2
                        + "\r\n" + "--" + divider + "--\r\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + HttpPostRequestTest.CONTENT_LENGTH.length();
        String input = header + HttpPostRequestTest.CONTENT_LENGTH + (contentLength + 4) + "\r\n\r\n" + content;
        invokeServer(input);

        assertEquals(2, this.testServer.parms.size());
        assertEquals(HttpPostRequestTest.VALUE, this.testServer.parms.get(HttpPostRequestTest.FIELD));
        assertEquals(HttpPostRequestTest.VALUE2, this.testServer.parms.get(HttpPostRequestTest.FIELD2));

        assertEquals(2, this.testServer.parameters.size());
        assertEquals(HttpPostRequestTest.VALUE, this.testServer.parameters.get(HttpPostRequestTest.FIELD).get(0));
        assertEquals(HttpPostRequestTest.VALUE2, this.testServer.parameters.get(HttpPostRequestTest.FIELD2).get(0));
    }

    @Test
    public void testPostWithMultipleMultipartFormFieldsWhereContentTypeWasSeparatedByComma() throws Exception {
        String divider = UUID.randomUUID().toString();
        String header = "POST " + HttpServerTest.URI + " HTTP/1.1\nContent-Type: " + "multipart/form-data, boundary=" + divider + "\r\n";
        String content =
                "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\"" + HttpPostRequestTest.FIELD + "\"\r\n" + "\r\n" + HttpPostRequestTest.VALUE + "\r\n"
                        + "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\"" + HttpPostRequestTest.FIELD2 + "\"\r\n" + "\r\n" + HttpPostRequestTest.VALUE2
                        + "\r\n" + "--" + divider + "--\r\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + HttpPostRequestTest.CONTENT_LENGTH.length();
        String input = header + HttpPostRequestTest.CONTENT_LENGTH + (contentLength + 4) + "\r\n\r\n" + content;
        invokeServer(input);

        assertEquals(2, this.testServer.parms.size());
        assertEquals(HttpPostRequestTest.VALUE, this.testServer.parms.get(HttpPostRequestTest.FIELD));
        assertEquals(HttpPostRequestTest.VALUE2, this.testServer.parms.get(HttpPostRequestTest.FIELD2));

        assertEquals(2, this.testServer.parameters.size());
        assertEquals(HttpPostRequestTest.VALUE, this.testServer.parameters.get(HttpPostRequestTest.FIELD).get(0));
        assertEquals(HttpPostRequestTest.VALUE2, this.testServer.parameters.get(HttpPostRequestTest.FIELD2).get(0));
    }

    @Test
    public void testSimplePostWithSingleMultipartFormField() throws Exception {
        String divider = UUID.randomUUID().toString();
        String header = "POST " + HttpServerTest.URI + " HTTP/1.1\nContent-Type: " + "multipart/form-data; boundary=" + divider + "\r\n";
        String content =
                "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\"" + HttpPostRequestTest.FIELD + "\"\r\n" + "\r\n" + HttpPostRequestTest.VALUE + "\r\n"
                        + "--" + divider + "--\r\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + HttpPostRequestTest.CONTENT_LENGTH.length();
        String input = header + HttpPostRequestTest.CONTENT_LENGTH + (contentLength + 4) + "\r\n\r\n" + content;
        invokeServer(input);

        assertEquals(1, this.testServer.parms.size());
        assertEquals(HttpPostRequestTest.VALUE, this.testServer.parms.get(HttpPostRequestTest.FIELD));

        assertEquals(1, this.testServer.parameters.size());
        assertEquals(HttpPostRequestTest.VALUE, this.testServer.parameters.get(HttpPostRequestTest.FIELD).get(0));
    }

    @Test
    public void testSimpleRawPostData() throws Exception {
        String header = "POST " + HttpServerTest.URI + " HTTP/1.1\n";
        String content = HttpPostRequestTest.VALUE_TEST_SIMPLE_RAW_DATA_WITH_AMPHASIS + "\r\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + HttpPostRequestTest.CONTENT_LENGTH.length();
        String input = header + HttpPostRequestTest.CONTENT_LENGTH + (contentLength + 4) + "\r\n\r\n" + content;
        invokeServer(input);
        assertEquals(0, this.testServer.parms.size());
        assertEquals(0, this.testServer.parameters.size());
        assertEquals(1, this.testServer.files.size());
        assertEquals(HttpPostRequestTest.VALUE_TEST_SIMPLE_RAW_DATA_WITH_AMPHASIS, this.testServer.files.get(HttpPostRequestTest.POST_RAW_CONTENT_FILE_ENTRY));
    }

    @Test
    public void testPostWithMultipartFormFieldsAndFile() throws IOException {
        String fileName = "GrandCanyon.txt";
        String fileContent = HttpPostRequestTest.VALUE;

        String divider = UUID.randomUUID().toString();
        String header = "POST " + HttpServerTest.URI + " HTTP/1.1\nContent-Type: " + "multipart/form-data; boundary=" + divider + "\n";
        String content =
                "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\"" + HttpPostRequestTest.FIELD + "\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: image/jpeg\r\n" + "\r\n" + fileContent + "\r\n" + "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\""
                        + HttpPostRequestTest.FIELD2 + "\"\r\n" + "\r\n" + HttpPostRequestTest.VALUE2 + "\r\n" + "--" + divider + "--\r\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + HttpPostRequestTest.CONTENT_LENGTH.length();
        String input = header + HttpPostRequestTest.CONTENT_LENGTH + (contentLength + 4) + "\r\n\r\n" + content;
        invokeServer(input);

        assertEquals("Parms count did not match.", 2, this.testServer.parms.size());
        assertEquals("Parameters count did not match.", 2, this.testServer.parameters.size());
        assertEquals("Param value did not match", HttpPostRequestTest.VALUE2, this.testServer.parms.get(HttpPostRequestTest.FIELD2));
        assertEquals("Parameter value did not match", HttpPostRequestTest.VALUE2, this.testServer.parameters.get(HttpPostRequestTest.FIELD2).get(0));
        BufferedReader reader = new BufferedReader(new FileReader(this.testServer.files.get(HttpPostRequestTest.FIELD)));
        List<String> lines = readLinesFromFile(reader);
        assertLinesOfText(new String[]{
            fileContent
        }, lines);
    }

    @Test
    public void testPostWithMultipartFormUploadMultipleFiles() throws IOException {

        String fileName = "GrandCanyon.txt";
        String fileContent = HttpPostRequestTest.VALUE;
        String file2Name = "AnotherPhoto.txt";
        String file2Content = HttpPostRequestTest.VALUE2;
        String divider = UUID.randomUUID().toString();
        String header = "POST " + HttpServerTest.URI + " HTTP/1.1\nContent-Type: " + "multipart/form-data; boundary=" + divider + "\n";
        String content = "--" + divider + "\r\n"//
                + "Content-Disposition: form-data; name=\"" + HttpPostRequestTest.FIELD + "\"; filename=\"" + fileName + "\"\r\n" //
                + "Content-Type: image/jpeg\r\n" + "\r\n" //
                + fileContent + "\r\n" //
                + "--" + divider + "\r\n" //
                + "Content-Disposition: form-data; name=\"" + HttpPostRequestTest.FIELD2 + "\"; filename=\"" + file2Name + "\"\r\n" //
                + "Content-Type: image/jpeg\r\n" + "\r\n" //
                + file2Content + "\r\n" //
                + "\r\n" //
                + "--" + divider + "--\r\n";
        int size = content.length() + header.length();
        int contentLengthHeaderValueSize = String.valueOf(size).length();
        int contentLength = size + contentLengthHeaderValueSize + HttpPostRequestTest.CONTENT_LENGTH.length();
        String input = header + HttpPostRequestTest.CONTENT_LENGTH + (contentLength + 4) + "\r\n\r\n" + content;
        invokeServer(input);

        assertEquals("Parm count did not match.", 2, this.testServer.parms.size());
        assertEquals("Parameter count did not match.", 2, this.testServer.parameters.size());
        BufferedReader reader = new BufferedReader(new FileReader(this.testServer.files.get(HttpPostRequestTest.FIELD)));
        List<String> lines = readLinesFromFile(reader);
        assertLinesOfText(new String[]{
            fileContent
        }, lines);
        String fileName2 = this.testServer.files.get(HttpPostRequestTest.FIELD2);
        int testNumber = 0;
        while (fileName2 == null && testNumber < 5) {
            testNumber++;
            fileName2 = this.testServer.files.get(HttpPostRequestTest.FIELD2 + testNumber);
        }
        reader = new BufferedReader(new FileReader(fileName2));
        lines = readLinesFromFile(reader);
        assertLinesOfText(new String[]{
            file2Content
        }, lines);

    }

    @Test
    public void testPostWithMultipartFormUploadFileWithMultilineContent() throws Exception {
        String filename = "GrandCanyon.txt";
        String lineSeparator = "\n";
        String fileContent = HttpPostRequestTest.VALUE + lineSeparator + HttpPostRequestTest.VALUE + lineSeparator + HttpPostRequestTest.VALUE;
        String input = preparePostWithMultipartForm(filename, fileContent);

        invokeServer(input);

        assertEquals("Parm count did not match.", 1, this.testServer.parms.size());
        assertEquals("Parameter count did not match.", 1, this.testServer.parameters.size());
        BufferedReader reader = new BufferedReader(new FileReader(this.testServer.files.get(HttpPostRequestTest.FIELD)));
        List<String> lines = readLinesFromFile(reader);
        assertLinesOfText(fileContent.split(lineSeparator), lines);
    }

}
