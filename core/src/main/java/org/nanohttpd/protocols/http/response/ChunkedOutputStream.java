package org.nanohttpd.protocols.http.response;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream that will automatically send every write to the wrapped
 * OutputStream according to chunked transfer:
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.6.1
 */
public class ChunkedOutputStream extends FilterOutputStream {
	
    public ChunkedOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        byte[] data = {
            (byte) b
        };
        write(data, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0)
            return;
        out.write(String.format("%x\r\n", len).getBytes());
        out.write(b, off, len);
        out.write("\r\n".getBytes());
    }

    public void finish() throws IOException {
        out.write("0\r\n\r\n".getBytes());
    }
}
