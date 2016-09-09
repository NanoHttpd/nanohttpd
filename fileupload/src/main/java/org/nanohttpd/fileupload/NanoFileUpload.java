package org.nanohttpd.fileupload;

/*
 * #%L
 * NanoHttpd-apache file upload integration
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.UploadContext;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.request.Method;

/**
 * @author victor & ritchieGitHub
 */
public class NanoFileUpload extends FileUpload {

    public static class NanoHttpdContext implements UploadContext {

        private IHTTPSession session;

        public NanoHttpdContext(IHTTPSession session) {
            this.session = session;
        }

        @Override
        public long contentLength() {
            long size;
            try {
                String cl1 = session.getHeaders().get("content-length");
                size = Long.parseLong(cl1);
            } catch (NumberFormatException var4) {
                size = -1L;
            }

            return size;
        }

        @Override
        public String getCharacterEncoding() {
            return "UTF-8";
        }

        @Override
        public String getContentType() {
            return this.session.getHeaders().get("content-type");
        }

        @Override
        public int getContentLength() {
            return (int) contentLength();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return session.getInputStream();
        }
    }

    public static final boolean isMultipartContent(IHTTPSession session) {
        return session.getMethod() == Method.POST && FileUploadBase.isMultipartContent(new NanoHttpdContext(session));
    }

    public NanoFileUpload(FileItemFactory fileItemFactory) {
        super(fileItemFactory);
    }

    public List<FileItem> parseRequest(IHTTPSession session) throws FileUploadException {
        return this.parseRequest(new NanoHttpdContext(session));
    }

    public Map<String, List<FileItem>> parseParameterMap(IHTTPSession session) throws FileUploadException {
        return this.parseParameterMap(new NanoHttpdContext(session));
    }

    public FileItemIterator getItemIterator(IHTTPSession session) throws FileUploadException, IOException {
        return super.getItemIterator(new NanoHttpdContext(session));
    }

}
