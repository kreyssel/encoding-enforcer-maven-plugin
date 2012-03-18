package org.kreyssel.maven.plugins.encdetector;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * Mojo to validate the configured project source encoding.
 * 
 * <p>
 * Based on {@link CharsetDetector} from ICU4J - see
 * http://userguide.icu-project.org/conversion/detection
 * 
 * @goal check
 * @phase validate
 * @requiresDependencyResolution test
 * 
 * @author kreyssel
 */
public class EncodingDetectorMojo extends AbstractMojo {

	/**
	 * The type of the project packaging (pom, jar, war, ...).
	 * 
	 * @parameter expression="${project.packaging}" required="true"
	 */
	private String packaging;

	/**
	 * The source directory.
	 * 
	 * @parameter expression="${project.build.sourceDirectory}" required="true"
	 */
	private File sourceDirectory;

	/**
	 * The source encoding.
	 * 
	 * @parameter expression="${project.build.sourceEncoding}" required="true"
	 */
	private String sourceEncoding;

	/**
	 * The confidence level for detection of good results (0-100, 0 = poor
	 * match, 100 = full match)
	 * 
	 * @parameter expression="${confidence}" default-value="70" required="true"
	 */
	private int confidence = 70;

	/**
	 * Skips the encoding check.
	 * 
	 * @parameter expression="${skipEncDet}" default-value="false"
	 *            required="true"
	 */
	private boolean skip = false;

	/**
	 * Should the build fail if any file does not match?
	 * 
	 * @parameter expression="${failEncDet}" default-value="true"
	 *            required="true"
	 */
	private boolean failOnWarn = true;

	/**
	 * a enum for the various matching states
	 */
	private enum Match {

		// no matching charset found with the given confidence
		NONE,

		// no match with the given confidence

		// charsets found, but not with the given confidence
		NO_CONFIDENTIAL_MATCH,

		// confidential charset found
		MATCH;
	}

	public void setSourceDirectory(final File sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	public void setSourceEncoding(final String sourceEncoding) {
		this.sourceEncoding = sourceEncoding;
	}

	public void setSkip(final boolean skip) {
		this.skip = skip;
	}

	public void setFailOnWarn(final boolean failOnWarn) {
		this.failOnWarn = failOnWarn;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {

		if (shouldSkip()) {
			getLog().warn("Skip charset detection of sources ...");
			return;
		}

		// check first if the environment OK
		validateConfiguration();

		try {
			boolean result = verifyRecursive(
					sourceDirectory.getCanonicalFile(), sourceEncoding,
					confidence);

			String message = "We detect encoding errors on various files - see log!";

			if (!result && failOnWarn) {
				throw new MojoExecutionException(message);
			} else if (!result) {
				getLog().warn(message);
			}

		} catch (IOException ex) {
			throw new MojoExecutionException("Error on detect encoding!", ex);
		}
	}

	boolean shouldSkip() {
		if (skip) {
			return true;
		}

		// pom projects have no source files to check
		if (StringUtils.equalsIgnoreCase("pom", packaging)) {
			return true;
		}

		return false;
	}

	void validateConfiguration() throws MojoFailureException {

		// does the source directory exist and is readable?
		if (sourceDirectory == null || !sourceDirectory.isDirectory()
				|| !sourceDirectory.canRead()) {
			throw new MojoFailureException("Please check the source directory "
					+ sourceDirectory);
		}

		// is a source encoding defined?
		if (StringUtils.isBlank(sourceEncoding)) {
			throw new MojoFailureException(
					"Source-Encoding is not defined! "
							+ "Please define as property 'project.build.sourceDirectory'!");

		}

		// can the given source encoding detect by icu4j?
		Set<String> charsets = new HashSet<String>(
				Arrays.asList(CharsetDetector.getAllDetectableCharsets()));
		if (!charsets.contains(sourceEncoding)) {
			throw new MojoFailureException("Unknown encoding " + sourceEncoding);
		}

		// check confidence level value
		if (confidence < 0 || confidence > 100) {
			throw new MojoFailureException(
					"Confidence should be in the range of 0-100 (current define is "
							+ confidence + ")!");
		}
	}

	boolean verifyRecursive(final File root, final String declaredEncoding,
			final int confidenceLevel) throws IOException {

		boolean result = true;

		getLog().info("scan dir " + root);

		Collection<File> files = FileUtils.listFiles(root,
				new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE);

		for (File file : files) {
			getLog().debug(file.getPath());

			if (!verifyFile(file, declaredEncoding, confidenceLevel)) {
				getLog().warn(
						"File " + file.getCanonicalPath()
								+ " does not match encoding "
								+ declaredEncoding);

				result = false;
			}
		}

		return result;
	}

	boolean verifyFile(final File file, final String declaredEncoding,
			final int confidenceLevel) throws IOException {

		FileInputStream in = new FileInputStream(file);

		CharsetMatch[] matches;

		try {
			matches = detect(in);
		} finally {
			IOUtils.closeQuietly(in);
		}

		// no encoding matches
		if (matches == null || matches.length == 0) {
			return false;
		}

		// first try only good matches - min is the given confidence level
		Match matchResult = verifyMatches(matches, declaredEncoding,
				confidenceLevel);

		if (matchResult == Match.MATCH) {
			// we have a match with the given confidence
			return true;
		} else if (matchResult == Match.NO_CONFIDENTIAL_MATCH) {
			// we have a matches with the given confidence level, but not for
			// the given encoding
			return false;
		}

		getLog().debug(
				"for file "
						+ file
						+ " we are unable to detect a good match (confidence > "
						+ confidenceLevel + ")- now try also poor matches");

		// secondly try all matches
		if (verifyMatches(matches, declaredEncoding, 0) == Match.MATCH) {
			return true;
		}

		return false;
	}

	Match verifyMatches(final CharsetMatch[] matches,
			final String lookupEncoding, final int confidence) {

		// true if a CharsetMatch with the given confidence found
		boolean confidentialMatch = false;

		for (CharsetMatch match : matches) {
			if (match.getConfidence() < confidence) {
				// poor match - see
				// http://userguide.icu-project.org/conversion/detection
				continue;
			}

			getLog().debug(
					"Matched encoding: " + match.getName() + " -- confidence: "
							+ match.getConfidence());

			// we found a matching charset
			if (StringUtils.equalsIgnoreCase(lookupEncoding, match.getName())) {
				return Match.MATCH;
			}

			confidentialMatch = true;
		}
		return confidentialMatch ? Match.NO_CONFIDENTIAL_MATCH : Match.NONE;
	}

	CharsetMatch[] detect(final InputStream in) throws IOException {

		CharsetDetector charsetDetector = new CharsetDetector();

		charsetDetector.setText(new BufferedInputStream(in));

		return charsetDetector.detectAll();
	}

}
