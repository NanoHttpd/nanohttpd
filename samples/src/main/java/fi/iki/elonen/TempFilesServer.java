package fi.iki.elonen;

/*
 * #%L
 * NanoHttpd-Samples
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fi.iki.elonen.debug.DebugServer;
import fi.iki.elonen.util.ServerRunner;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com) On: 3/9/13 at 12:47 AM
 */
public class TempFilesServer extends DebugServer {

    private static class ExampleManager implements TempFileManager {

        private final File tmpdir;

        private final List<TempFile> tempFiles;

        private ExampleManager() {
            this.tmpdir = new File(System.getProperty("java.io.tmpdir"));
            this.tempFiles = new ArrayList<TempFile>();
        }

        @Override
        public void clear() {
            if (!this.tempFiles.isEmpty()) {
                System.out.println("Cleaning up:");
            }
            for (TempFile file : this.tempFiles) {
                try {
                    System.out.println("   " + file.getName());
                    file.delete();
                } catch (Exception ignored) {
                }
            }
            this.tempFiles.clear();
        }

        @Override
        public TempFile createTempFile(String filename_hint) throws Exception {
            DefaultTempFile tempFile = new DefaultTempFile(this.tmpdir);
            this.tempFiles.add(tempFile);
            System.out.println("Created tempFile: " + tempFile.getName());
            return tempFile;
        }
    }

    private static class ExampleManagerFactory implements TempFileManagerFactory {

        @Override
        public TempFileManager create() {
            return new ExampleManager();
        }
    }

    public static void main(String[] args) {
        TempFilesServer server = new TempFilesServer();
        server.setTempFileManagerFactory(new ExampleManagerFactory());
        ServerRunner.executeInstance(server);
    }
}
