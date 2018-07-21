package net.ssehub.kernel_haven.incremental.preparation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import diff.DiffAnalyzer;
import net.ssehub.kernel_haven.IPreparation;
import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.incremental.diff.DiffApplyUtil;
import net.ssehub.kernel_haven.incremental.diff.DiffFile;
import net.ssehub.kernel_haven.incremental.preparation.filter.InputFilter;
import net.ssehub.kernel_haven.incremental.settings.IncrementalAnalysisSettings;
import net.ssehub.kernel_haven.incremental.storage.HybridCache;
import net.ssehub.kernel_haven.util.Logger;

/**
 * Preparation task for incremental analyses. This class is used to integrate a
 * diff on the filebase of the source tree and subsequently select a subset of
 * the resulting files for extraction and analyses.
 * {@link IncrementalPreparation} must be used as preparation when working with
 * an incremental analysis.
 * 
 * @author moritz
 */
public class IncrementalPreparation implements IPreparation {

    /** Logger instance. */
    private static final Logger LOGGER = Logger.get();

    /**
     * Handle rollback.
     *
     * @param gitApplyUtil
     *            the git apply util
     * @param config
     *            the config
     */
    private void handleRollback(DiffApplyUtil gitApplyUtil,
        Configuration config) {

        // Handle rollback
        boolean revertSuccessful = gitApplyUtil.revertChanges();
        HybridCache hybridCache = new HybridCache((File) config
            .getValue(IncrementalAnalysisSettings.HYBRID_CACHE_DIRECTORY));
        try {
            hybridCache.rollback();
        } catch (IOException e) {
            revertSuccessful = false;
            LOGGER.logException("Could not revert changes in HybridCache.", e);
        }

        // Stop execution after rollback
        if (revertSuccessful) {
            LOGGER.logInfo("Rollback successful.");
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.ssehub.kernel_haven.IPreparation#run(net.ssehub.kernel_haven.config.
     * Configuration)
     */

    // CHECKSTYLE:OFF
    @Override
    public void run(Configuration config) throws SetUpException {
        // CHECKSTYLE:ON
        long start = System.nanoTime();

        IncrementalAnalysisSettings.registerAllSettings(config);

        File inputDiff = (File) config
            .getValue(IncrementalAnalysisSettings.SOURCE_TREE_DIFF_FILE);
        File inputSourceDir =
            (File) config.getValue(DefaultSettings.SOURCE_TREE);
        DiffApplyUtil gitApplyUtil =
            new DiffApplyUtil(inputSourceDir, inputDiff);

        if (config.getValue(IncrementalAnalysisSettings.ROLLBACK)) {
            // Execution will stop after rollback is complete
            handleRollback(gitApplyUtil, config);
        } else {
            // Merge changes
            boolean mergeSuccessful = gitApplyUtil.mergeChanges();

            // only continue if merge was successful
            if (!mergeSuccessful) {
                LOGGER.logError(
                    "Could not merge provided diff with existing input files!\n"
                        + "The diff-file must describe changes that can"
                        + " be applied to the set of input-files that are to be analyzed. \n"
                        + "Stopping execution of KernelHaven.");
                throw new SetUpException(
                    "Could not merge provided diff with existing input files!");
            } else {
                DiffFile diffFile = generateDiffFile(
                    config.getValue(
                        IncrementalAnalysisSettings.DIFF_ANALYZER_CLASS_NAME),
                    inputDiff);
                try {
                    diffFile.save(
                        new File(inputDiff.getAbsolutePath() + config.getValue(
                            IncrementalAnalysisSettings.PARSED_DIFF_FILE_SUFFIX)));
                } catch (IOException e) {
                    throw new SetUpException(
                        "Could not save parsed version of diff-file", e);
                }

                //////////////////////////
                // Filter for codemodel //
                //////////////////////////
                Collection<Path> filteredPaths = filterInput(
                    config.getValue(
                        IncrementalAnalysisSettings.CODE_MODEL_FILTER_CLASS),
                    inputSourceDir, diffFile,
                    config.getValue(DefaultSettings.CODE_EXTRACTOR_FILE_REGEX),
                    false);

                boolean extractCm = false;
                if (!filteredPaths.isEmpty()) {
                    extractCm = true;
                    ArrayList<String> pathStrings = new ArrayList<String>();
                    filteredPaths
                        .forEach(path -> pathStrings.add(path.toString()));
                    config.setValue(DefaultSettings.CODE_EXTRACTOR_FILES,
                        pathStrings);
                    // If no paths are included after filtering, the extraction
                    // does not need to run
                }

                config.setValue(IncrementalAnalysisSettings.EXTRACT_CODE_MODEL,
                    extractCm);

                //////////////////////////////////
                // Filter for variability model //
                //////////////////////////////////
                filteredPaths = filterInput(config.getValue(
                    IncrementalAnalysisSettings.VARIABILITY_MODEL_FILTER_CLASS),
                    inputSourceDir, diffFile,
                    config.getValue(
                        DefaultSettings.VARIABILITY_EXTRACTOR_FILE_REGEX),
                    true);
                boolean extractVm = !filteredPaths.isEmpty();
                config.setValue(
                    IncrementalAnalysisSettings.EXTRACT_VARIABILITY_MODEL,
                    extractVm);

                ////////////////////////////
                // Filter for build model //
                ////////////////////////////
                if (extractVm) {
                    // if vm was updated, always extract bm aswell as it depends
                    // on the vm
                    config.setValue(
                        IncrementalAnalysisSettings.EXTRACT_BUILD_MODEL, true);
                } else {
                    filteredPaths = filterInput(config.getValue(
                        IncrementalAnalysisSettings.BUILD_MODEL_FILTER_CLASS),
                        inputSourceDir, diffFile, config.getValue(
                            DefaultSettings.BUILD_EXTRACTOR_FILE_REGEX),
                        true);
                    boolean extractBm = !filteredPaths.isEmpty();
                    config.setValue(
                        IncrementalAnalysisSettings.EXTRACT_BUILD_MODEL,
                        extractBm);
                }

                // only start extractory preemptively if all extractors need to
                // run
                if (!extractCm || !extractCm || !extractVm) {
                    config.setValue(
                        DefaultSettings.ANALYSIS_PIPELINE_START_EXTRACTORS,
                        false);
                }

            }
        }

        long totalTime = System.nanoTime() - start;
        // Finish and let KernelHaven run
        LOGGER.logDebug(this.getClass().getSimpleName() + " duration:"
            + TimeUnit.MILLISECONDS.convert(totalTime, TimeUnit.NANOSECONDS)
            + "ms");
    }

    /**
     * Filters input using the class defined by filterClassName. This should be
     * a class available in the classpath and implementing InputFilter.
     *
     * @param filterClassName
     *            the filter class name
     * @param inputSourceDir
     *            the input source dir
     * @param inputDiff
     *            the input diff file
     * @param regex
     *            the regular expression describing which files to include
     * @param includeDeletions
     *            defines whether deletions are included
     * @return the collection of resulting paths
     * @throws SetUpException
     *             the set up exception
     */
    @SuppressWarnings("unchecked")
    protected Collection<Path> filterInput(String filterClassName,
        File inputSourceDir, DiffFile inputDiff, Pattern regex,
        boolean includeDeletions) throws SetUpException {
        Collection<Path> paths = null;
        // Call the method getFilteredResult for filterClassName via
        // reflection-api
        try {
            @SuppressWarnings("rawtypes")
            Class<InputFilter> filterClass =
                (Class<InputFilter>) Class.forName(filterClassName);
            Object filterObject = filterClass.getConstructor(File.class,
                DiffFile.class, Pattern.class, boolean.class).newInstance(
                    inputSourceDir, inputDiff, regex, includeDeletions);
            if (filterObject instanceof InputFilter) {
                Method getFilteredResultMethod =
                    filterClass.getMethod("getFilteredResult");
                paths = (Collection<Path>) getFilteredResultMethod
                    .invoke(filterObject);
            } else {
                throw new SetUpException(
                    "The class name provided for the filter does not appear to extend the InputFilter class");
            }

        } catch (ClassNotFoundException | IllegalAccessException
            | InstantiationException | NoSuchMethodException
            | InvocationTargetException e) {
            LOGGER.logException("The specified filter class could not be used",
                e);
            throw new SetUpException(
                "The specified filter could not be used: " + e.getMessage());
        }
        return paths;

    }

    /**
     * Filters input using the class defined by filterClassName. This should be
     * a class available in the classpath and implementing InputFilter.
     *
     * @param analyzerClassName
     *            the analyzer class name
     * @param inputGitDiff
     *            the input git diff
     * @return the collection
     * @throws SetUpException
     *             the set up exception
     */
    @SuppressWarnings("unchecked")
    protected DiffFile generateDiffFile(String analyzerClassName,
        File inputGitDiff) throws SetUpException {
        DiffFile diffFile = null;
        // Call the method getFilteredResult for filterClassName via
        // reflection-api
        try {
            @SuppressWarnings("rawtypes")
            Class<DiffAnalyzer> analyzerClass =
                (Class<DiffAnalyzer>) Class.forName(analyzerClassName);
            Object analyzerObject =
                analyzerClass.getConstructor().newInstance();
            Method getFilteredResultMethod =
                analyzerClass.getMethod("generateDiffFile", File.class);
            LOGGER.logInfo(
                "Analyzing git-diff with " + analyzerClass.getSimpleName()
                    + ". This may take a while for large git-diffs.");
            diffFile = (DiffFile) getFilteredResultMethod.invoke(analyzerObject,
                inputGitDiff);

        } catch (ClassNotFoundException | IllegalAccessException
            | InstantiationException | NoSuchMethodException
            | InvocationTargetException e) {
            throw new SetUpException("The specified DiffAnalyzer class \""
                + analyzerClassName + "\" could not be used: "
                + e.getClass().getName() + "\n" + e.getMessage());
        }
        return diffFile;

    }

}
