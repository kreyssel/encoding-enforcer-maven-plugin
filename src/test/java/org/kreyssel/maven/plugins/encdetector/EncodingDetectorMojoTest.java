package org.kreyssel.maven.plugins.encdetector;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

public class EncodingDetectorMojoTest {

	private final static String PATH = "src/test/resources/encoding";

	@Test
	public void detectUTF16BE() throws Exception {
		String encoding = "UTF-16BE";
		assertEncoding("utf16be/UTF16BEWithGermanMarker.java", encoding);
		assertEncoding("utf16be/UTF16BEWithoutMarker.java", encoding);
	}

	@Test
	public void detectUTF8() throws Exception {
		String encoding = "UTF-8";
		assertEncoding("utf8/UTF8WithGermanMarker.java", encoding);
		assertEncoding("utf8/UTF8WithoutMarker.java", encoding);
	}

	@Test
	public void detectISO_8859_1() throws Exception {
		String encoding = "ISO-8859-1";
		assertEncoding("iso_8859_1/ISO_8859_1WithGermanMarker.java", encoding);
		assertEncoding("iso_8859_1/ISO_8859_1WithoutMarker.java", encoding);
	}

	private void assertEncoding(String file, String encoding) {
		File testFile = new File(PATH, file);
		assertTrue("File not found: " + testFile, testFile.isFile());

		EncodingDetectorMojo mojo = new EncodingDetectorMojo();
		mojo.setSourceDirectory(testFile.getParentFile());
		mojo.setSourceEncoding(encoding);

		try {
			mojo.validateConfiguration();

			assertTrue("File " + file + " does not match encoding " + encoding,
					mojo.verifyFile(testFile, encoding, 50));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
