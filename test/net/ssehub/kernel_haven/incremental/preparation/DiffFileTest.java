package net.ssehub.kernel_haven.incremental.preparation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.kernel_haven.incremental.util.diff.DiffFile;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.Logger.Level;

public class DiffFileTest {
	private static Logger LOGGER = null;
	private static final File DIFF_FILE = new File("testdata/changed-only/git.diff");

	/**
	 * Inits the logger.
	 */
	@BeforeClass
	public static void initLogger() {
		LOGGER = Logger.get();
		LOGGER.setLevel(Level.DEBUG);
	}
	
	/**
	 * Tests whether the doFilter method works.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDoFilter() throws IOException {
		DiffFile diffFile = new DiffFile(DIFF_FILE);
		Collection<Path> paths = diffFile.getModified();
		Assert.assertThat(paths, CoreMatchers.hasItem(Paths.get("modify/Kbuild")));
		Assert.assertThat(paths, CoreMatchers.hasItem(Paths.get("modify/Kconfig")));
		Assert.assertThat(paths, CoreMatchers.hasItem(Paths.get("modify/a-code-file.c")));

	}
	
	

}