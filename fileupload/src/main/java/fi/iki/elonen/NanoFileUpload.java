package fi.iki.elonen;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by victor on 7/30/15.
 */
public class NanoFileUpload extends FileUpload {
	private static final String POST_METHOD = "POST";

	public static final boolean isMultipartContent(NanoHTTPD.IHTTPSession session) {
		return !"POST".equalsIgnoreCase(session.getMethod().toString())?false: FileUploadBase.isMultipartContent(new NanoHttpdContext(session));
	}

	public NanoFileUpload() {
	}

	public NanoFileUpload(FileItemFactory fileItemFactory) {
		super(fileItemFactory);
	}

	public List<FileItem> parseRequest(NanoHTTPD.IHTTPSession session) throws FileUploadException {
		return this.parseRequest(new NanoHttpdContext(session));
	}

	public Map<String, List<FileItem>> parseParameterMap(NanoHTTPD.IHTTPSession session) throws FileUploadException {
		return this.parseParameterMap(new NanoHttpdContext(session));
	}

	public FileItemIterator getItemIterator(NanoHTTPD.IHTTPSession session) throws FileUploadException, IOException {
		return super.getItemIterator(new NanoHttpdContext(session));
	}

}
