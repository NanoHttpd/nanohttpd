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

import fi.iki.elonen.NanoHTTPD;

public class StatusTest {

    @Test
    public void testLookup() throws Exception {
        Assert.assertEquals(Response.Status.SWITCH_PROTOCOL, Response.Status.lookup(101));
        Assert.assertEquals(Response.Status.OK, Response.Status.lookup(200));
        Assert.assertEquals(Response.Status.CREATED, Response.Status.lookup(201));
        Assert.assertEquals(Response.Status.ACCEPTED, Response.Status.lookup(202));
        Assert.assertEquals(Response.Status.NO_CONTENT, Response.Status.lookup(204));
        Assert.assertEquals(Response.Status.PARTIAL_CONTENT, Response.Status.lookup(206));
        Assert.assertEquals(Response.Status.MULTI_STATUS, Response.Status.lookup(207));
        Assert.assertEquals(Response.Status.REDIRECT, Response.Status.lookup(301));
        Assert.assertEquals(Response.Status.REDIRECT_SEE_OTHER, Response.Status.lookup(303));
        Assert.assertEquals(Response.Status.NOT_MODIFIED, Response.Status.lookup(304));
        Assert.assertEquals(Response.Status.BAD_REQUEST, Response.Status.lookup(400));
        Assert.assertEquals(Response.Status.UNAUTHORIZED, Response.Status.lookup(401));
        Assert.assertEquals(Response.Status.FORBIDDEN, Response.Status.lookup(403));
        Assert.assertEquals(Response.Status.NOT_FOUND, Response.Status.lookup(404));
        Assert.assertEquals(Response.Status.METHOD_NOT_ALLOWED, Response.Status.lookup(405));
        Assert.assertEquals(Response.Status.NOT_ACCEPTABLE, Response.Status.lookup(406));
        Assert.assertEquals(Response.Status.REQUEST_TIMEOUT, Response.Status.lookup(408));
        Assert.assertEquals(Response.Status.CONFLICT, Response.Status.lookup(409));
        Assert.assertEquals(Response.Status.RANGE_NOT_SATISFIABLE, Response.Status.lookup(416));
        Assert.assertEquals(Response.Status.INTERNAL_ERROR, Response.Status.lookup(500));
        Assert.assertEquals(Response.Status.NOT_IMPLEMENTED, Response.Status.lookup(501));
        Assert.assertEquals(Response.Status.UNSUPPORTED_HTTP_VERSION, Response.Status.lookup(505));
    }
}
