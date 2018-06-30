package net.ssehub.kernel_haven.incremental.preparation.filter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Pattern;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.incremental.diff.DiffFile;
import net.ssehub.kernel_haven.incremental.diff.analyzer.VariabilityDiffAnalyzer;

/**
 * Tests for {@link VariabilityChangeFilter}.
 * 
 * @author moritz
 */
public class VariabilityChangeFilterTest {

    // CHECKSTYLE:OFF
    /**
     * Tests whether the doFilter method works if a file was completely changed.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testDoFilter_variability_change() throws IOException {
        DiffFile diffFile =
            new VariabilityDiffAnalyzer().generateDiffFile(new File(
                "testdata/variability-changes/some-variability-changes.diff"));
        VariabilityChangeFilter filter = new VariabilityChangeFilter(null,
            diffFile, Pattern.compile(".*"), false);
        Collection<Path> paths = filter.getFilteredResult();
        Assert.assertThat(paths,
            CoreMatchers.hasItem(Paths.get("drivers/crypto/caam/ctrl.c")));

    }

    // CHECKSTYLE:ON
}
