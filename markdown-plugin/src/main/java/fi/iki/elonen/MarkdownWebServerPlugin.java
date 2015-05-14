package fi.iki.elonen;

/*
 * #%L
 * NanoHttpd-Webserver-Markdown-Plugin
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

import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pegdown.PegDownProcessor;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com) On: 9/13/13 at 4:03 AM
 */
public class MarkdownWebServerPlugin implements WebServerPlugin {

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(MarkdownWebServerPlugin.class.getName());

    private final PegDownProcessor processor;

    public MarkdownWebServerPlugin() {
        this.processor = new PegDownProcessor();
    }

    @Override
    public boolean canServeUri(String uri, File rootDir) {
        File f = new File(rootDir, uri);
        return f.exists();
    }

    @Override
    public void initialize(Map<String, String> commandLineOptions) {
    }

    private String readSource(File file) {
        FileReader fileReader = null;
        BufferedReader reader = null;
        try {
            fileReader = new FileReader(file);
            reader = new BufferedReader(fileReader);
            String line = null;
            StringBuilder sb = new StringBuilder();
            do {
                line = reader.readLine();
                if (line != null) {
                    sb.append(line).append("\n");
                }
            } while (line != null);
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            MarkdownWebServerPlugin.LOG.log(Level.SEVERE, "could not read source", e);
            return null;
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ignored) {
                MarkdownWebServerPlugin.LOG.log(Level.FINEST, "close failed", ignored);
            }
        }
    }

    @Override
    public NanoHTTPD.Response serveFile(String uri, Map<String, String> headers, NanoHTTPD.IHTTPSession session, File file, String mimeType) {
        String markdownSource = readSource(file);
        byte[] bytes;
        try {
            bytes = this.processor.markdownToHtml(markdownSource).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            MarkdownWebServerPlugin.LOG.log(Level.SEVERE, "encoding problem, responding nothing", e);
            bytes = new byte[0];
        }
        return markdownSource == null ? null : new NanoHTTPD.Response(OK, NanoHTTPD.MIME_HTML, new ByteArrayInputStream(bytes), bytes.length);
    }
}
