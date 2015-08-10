package fi.iki.elonen;

import org.apache.commons.fileupload.UploadContext;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by victor on 7/30/15.
 */
public class NanoHttpdContext implements UploadContext {

	private NanoHTTPD.IHTTPSession session;

	public NanoHttpdContext(NanoHTTPD.IHTTPSession session) {
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
		return (int)contentLength();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return session.getInputStream();
	}
}
