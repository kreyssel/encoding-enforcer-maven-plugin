package org.kreyssel.maven.plugins.encdetector;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Finds duplicate classes/resources.
 * 
 * @goal check
 * @phase verify
 * @requiresDependencyResolution test
 * 
 * @author kreyssel
 */
public class EncodingDetectorMojo extends AbstractMojo {

	/**
	 * The maven project.
	 * 
	 * @parameter expression="${project}" required="true"
	 */
	private MavenProject project;

	public void execute() throws MojoExecutionException, MojoFailureException {
		File sourceDirectory = new File(project.getBuild().getSourceDirectory());
		verifyRecursive(sourceDirectory);
	}

	private void verifyRecursive(File root) {
		Collection<File> files = FileUtils.listFiles(root,
				DirectoryFileFilter.INSTANCE, new WildcardFileFilter("*.java"));

		for (File file : files) {
			getLog().info(file.getPath());
		}
	}
}
