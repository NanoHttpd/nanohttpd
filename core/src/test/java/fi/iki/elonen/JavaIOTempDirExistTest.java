package fi.iki.elonen;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by Victor Nikiforov on 10/16/15.
 */
public class JavaIOTempDirExistTest {
	@Test
	public void testJavaIoTempDefault() {
		NanoHTTPD.DefaultTempFileManager manager = new NanoHTTPD.DefaultTempFileManager();
		String tmpdir = System.getProperty("java.io.tmpdir");
		File dir = new File(tmpdir);
		Assert.assertEquals(true, dir.exists());
	}

	@Test
	public void testJavaIoTempSpecific() throws IOException {
		String tmpdir = System.getProperty("java.io.tmpdir");
		String tempFileName = UUID.randomUUID().toString();
		String orgDir = System.getProperty("java.io.tmpdir");
		String newDir = orgDir + File.separator + tempFileName;
		File dir = new File(newDir);
		System.setProperty("java.io.tmpdir", newDir);
		Assert.assertEquals(false, dir.exists());

		NanoHTTPD.DefaultTempFileManager manager = new NanoHTTPD.DefaultTempFileManager();
		Assert.assertEquals(true, dir.exists());
		System.setProperty("java.io.tmpdir", tmpdir);

		dir.delete();
		Assert.assertEquals(false, dir.exists());

	}


}
