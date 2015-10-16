package fi.iki.elonen;

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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import fi.iki.elonen.NanoHTTPD.DefaultTempFile;

/**
 * Created by Victor Nikiforov on 10/16/15.
 */
public class JavaIOTempDirExistTest {

    @Test
    public void testJavaIoTempDefault() throws Exception {
        String tmpdir = System.getProperty("java.io.tmpdir");
        NanoHTTPD.DefaultTempFileManager manager = new NanoHTTPD.DefaultTempFileManager();
        DefaultTempFile tempFile = (DefaultTempFile) manager.createTempFile("xx");
        File tempFileBackRef = new File(tempFile.getName());
        Assert.assertEquals(tempFileBackRef.getParentFile(), new File(tmpdir));

        // force an exception
        tempFileBackRef.delete();
        Exception e = null;
        try {
            tempFile.delete();
        } catch (Exception ex) {
            e = ex;
        }
        Assert.assertNotNull(e);
        manager.clear();
    }

    @Test
    public void testJavaIoTempSpecific() throws IOException {
        final String tmpdir = System.getProperty("java.io.tmpdir");
        try {
            String tempFileName = UUID.randomUUID().toString();
            File newDir = new File("target", tempFileName);
            System.setProperty("java.io.tmpdir", newDir.getAbsolutePath());
            Assert.assertEquals(false, newDir.exists());
            new NanoHTTPD.DefaultTempFileManager();
            Assert.assertEquals(true, newDir.exists());
            newDir.delete();
        } finally {
            System.setProperty("java.io.tmpdir", tmpdir);
        }

    }

}
