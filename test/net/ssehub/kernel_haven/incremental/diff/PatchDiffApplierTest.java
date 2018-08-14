package net.ssehub.kernel_haven.incremental.diff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import net.ssehub.kernel_haven.incremental.diff.applier.PatchDiffApplier;
import net.ssehub.kernel_haven.incremental.util.FolderUtil;
import net.ssehub.kernel_haven.util.Logger;

/**
 * Tests for {@link PatchDiffApplier}.
 * 
 * @author moritz
 */
public class PatchDiffApplierTest {

    /** The Constant ORIGINAL_FOLDER. */
    private static final File ORIGINAL_FOLDER =
        new File("testdata/diff-integration/original");

    /** The Constant MODIFIED_FOLDER. */
    private static final File MODIFIED_FOLDER =
        new File("testdata/diff-integration/modified");

    /** The Constant DIFF_FILE. */
    private static final File DIFF_FILE =
        new File("testdata/diff-integration/git.diff");

    /** The logger. */
    private static final Logger LOGGER = Logger.get();

    // CHECKSTYLE:OFF
    /**
     * Tests whether the call to git apply works.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testMerge_positive() throws IOException {
        Path tempFolder = Files.createTempDirectory("git-diff-apply-test");
        LOGGER.logInfo("Temp-Folder for testMerge: " + tempFolder);
        // Setup
        FolderUtil.copyFolderContent(ORIGINAL_FOLDER, tempFolder.toFile());

        // Check preconditions
        Assert.assertTrue(tempFolder.toFile().exists());
        Assert.assertTrue(DIFF_FILE.exists());
        Assert.assertTrue(!FolderUtil.folderContentEquals(tempFolder.toFile(),
            MODIFIED_FOLDER));

        // Merge action

        PatchDiffApplier diffIntegration =
            new PatchDiffApplier(tempFolder.toFile(), DIFF_FILE);

        boolean success = diffIntegration.mergeChanges();
        Assert.assertTrue(success);
        Assert.assertThat(
            FolderUtil.listRelativeFiles(tempFolder.toFile(), true),
            CoreMatchers
                .equalTo(FolderUtil.listRelativeFiles(MODIFIED_FOLDER, true)));
        Assert.assertTrue(
            "Folder content is not equal to the expected folder contents.",
            FolderUtil.folderContentEquals(tempFolder.toFile(),
                MODIFIED_FOLDER));

    }
    // CHECKSTYLE:OFF

    /**
     * Tests whether the call to git apply fails on a folder where the diff has
     * already been applied. This is effectively the same as trying to "apply"
     * on a folder that does not contain the files assumed by the diff-file.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testMerge_negative_alreadyMerged() throws IOException {
        Path tempFolder = Files.createTempDirectory("git-diff-apply-test");
        LOGGER.logInfo("Temp-Folder for testMerge: " + tempFolder);
        // Setup
        FolderUtil.copyFolderContent(MODIFIED_FOLDER, tempFolder.toFile());

        // Check preconditions
        Assert.assertTrue(tempFolder.toFile().exists());
        Assert.assertTrue(DIFF_FILE.exists());

        // Merge action

        PatchDiffApplier diffIntegration =
            new PatchDiffApplier(tempFolder.toFile(), DIFF_FILE);

        boolean success = diffIntegration.mergeChanges();

        Assert.assertFalse(success);

        Assert.assertThat(
            FolderUtil.listRelativeFiles(tempFolder.toFile(), true),
            CoreMatchers
                .equalTo(FolderUtil.listRelativeFiles(MODIFIED_FOLDER, true)));
        Assert.assertTrue(
            "Folder content is not equal to the expected folder contents.",
            FolderUtil.folderContentEquals(tempFolder.toFile(),
                MODIFIED_FOLDER));

    }
    // CHECKSTYLE:ON

    // CHECKSTYLE:OFF
    /**
     * Tests whether the call to git apply -- reverse works.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testRevert_positive() throws IOException {
        Path tempFolder = Files.createTempDirectory("git-diff-apply-test");
        LOGGER.logInfo("Temp-Folder for testRevert: " + tempFolder);

        // Setup
        FolderUtil.copyFolderContent(MODIFIED_FOLDER, tempFolder.toFile());

        // Check preconditions
        Assert.assertTrue(tempFolder.toFile().exists());
        Assert.assertTrue(DIFF_FILE.exists());

        // Revert action
        PatchDiffApplier diffIntegration =
            new PatchDiffApplier(tempFolder.toFile(), DIFF_FILE);

        boolean success = diffIntegration.revertChanges();
        Assert.assertTrue(success);

        Assert.assertThat(
            FolderUtil.listRelativeFiles(tempFolder.toFile(), true),
            CoreMatchers
                .equalTo(FolderUtil.listRelativeFiles(ORIGINAL_FOLDER, true)));

    }
    // CHECKSTYLE:ON

    // CHECKSTYLE:OFF
    /**
     * Tests whether the call to git apply --revert fails on a folder where the
     * diff was not applied before. This is effectively the same as trying to
     * "apply --revert" on a folder that does not fit the diff-file.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void testRevert_negative_noDiffApplied() throws IOException {
        Path tempFolder = Files.createTempDirectory("git-diff-apply-test");
        LOGGER.logInfo("Temp-Folder for testRevert: " + tempFolder);

        // Setup
        FolderUtil.copyFolderContent(ORIGINAL_FOLDER, tempFolder.toFile());

        // Check preconditions
        Assert.assertTrue(tempFolder.toFile().exists());
        Assert.assertTrue(DIFF_FILE.exists());

        // Revert action
        PatchDiffApplier diffIntegration =
            new PatchDiffApplier(tempFolder.toFile(), DIFF_FILE);

        boolean success = diffIntegration.revertChanges();
        Assert.assertFalse(success);

        Assert.assertThat(
            FolderUtil.listRelativeFiles(tempFolder.toFile(), true),
            CoreMatchers
                .equalTo(FolderUtil.listRelativeFiles(ORIGINAL_FOLDER, true)));

    }
    // CHECKSTYLE:ON
}