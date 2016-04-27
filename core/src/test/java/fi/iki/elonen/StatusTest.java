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

import org.junit.Assert;
import org.junit.Test;

import fi.iki.elonen.NanoHTTPD.Response.Status;

public class StatusTest {

    @Test
    public void testLookup() throws Exception {
        Assert.assertEquals(Status.SWITCH_PROTOCOL, Status.lookup(101));
        Assert.assertEquals(Status.OK, Status.lookup(200));
        Assert.assertEquals(Status.CREATED, Status.lookup(201));
        Assert.assertEquals(Status.ACCEPTED, Status.lookup(202));
        Assert.assertEquals(Status.NO_CONTENT, Status.lookup(204));
        Assert.assertEquals(Status.PARTIAL_CONTENT, Status.lookup(206));
        Assert.assertEquals(Status.MULTI_STATUS, Status.lookup(207));
        Assert.assertEquals(Status.REDIRECT, Status.lookup(301));
        Assert.assertEquals(Status.REDIRECT_SEE_OTHER, Status.lookup(303));
        Assert.assertEquals(Status.NOT_MODIFIED, Status.lookup(304));
        Assert.assertEquals(Status.BAD_REQUEST, Status.lookup(400));
        Assert.assertEquals(Status.UNAUTHORIZED, Status.lookup(401));
        Assert.assertEquals(Status.FORBIDDEN, Status.lookup(403));
        Assert.assertEquals(Status.NOT_FOUND, Status.lookup(404));
        Assert.assertEquals(Status.METHOD_NOT_ALLOWED, Status.lookup(405));
        Assert.assertEquals(Status.NOT_ACCEPTABLE, Status.lookup(406));
        Assert.assertEquals(Status.REQUEST_TIMEOUT, Status.lookup(408));
        Assert.assertEquals(Status.CONFLICT, Status.lookup(409));
        Assert.assertEquals(Status.RANGE_NOT_SATISFIABLE, Status.lookup(416));
        Assert.assertEquals(Status.INTERNAL_ERROR, Status.lookup(500));
        Assert.assertEquals(Status.NOT_IMPLEMENTED, Status.lookup(501));
        Assert.assertEquals(Status.UNSUPPORTED_HTTP_VERSION, Status.lookup(505));
    }
}
