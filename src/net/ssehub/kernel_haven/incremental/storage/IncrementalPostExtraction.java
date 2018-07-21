package net.ssehub.kernel_haven.incremental.storage;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.incremental.diff.DiffFile;
import net.ssehub.kernel_haven.incremental.diff.FileEntry;
import net.ssehub.kernel_haven.incremental.diff.analyzer.SimpleDiffAnalyzer;
import net.ssehub.kernel_haven.incremental.settings.IncrementalAnalysisSettings;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * This class is an {@link AnalysisComponent} which handles the extraction of
 * models within an incremental analysis pipeline. It should be used in
 * conjunction with the preparation task {@link IncrementalPreparation} The
 * result is given as {@link HybridCache}.
 * 
 * @author moritz
 */
public class IncrementalPostExtraction extends AnalysisComponent<HybridCache> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.get();

    /** The cm component. */
    private AnalysisComponent<SourceFile> cmComponent;

    /** The config. */
    private Configuration config;

    /** The bm component. */
    private AnalysisComponent<BuildModel> bmComponent;

    /** The vm component. */
    private AnalysisComponent<VariabilityModel> vmComponent;

    /**
     * Instantiates a new IncremenmtalPostExtraction.
     *
     * @param config
     *            the config
     * @param cmComponent
     *            the cm component
     * @param bmComponent
     *            the bm component
     * @param vmComponent
     *            the vm component
     * @throws SetUpException
     *             thrown if required parameters were not configured correctly.
     */
    public IncrementalPostExtraction(Configuration config,
        AnalysisComponent<SourceFile> cmComponent,
        AnalysisComponent<BuildModel> bmComponent,
        AnalysisComponent<VariabilityModel> vmComponent) throws SetUpException {
        super(config);
        this.config = config;
        IncrementalAnalysisSettings.registerAllSettings(config);
        this.cmComponent = cmComponent;
        this.bmComponent = bmComponent;
        this.vmComponent = vmComponent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.ssehub.kernel_haven.analysis.AnalysisComponent#execute()
     */
    @Override
    protected void execute() {
        HybridCache hybridCache = new HybridCache(config
            .getValue(IncrementalAnalysisSettings.HYBRID_CACHE_DIRECTORY));
        hybridCache.clearChangeHistory();

        // start threads for each model-type so they can run parallel
        Thread cmThread = null;
        if (config.getValue(IncrementalAnalysisSettings.EXTRACT_CODE_MODEL)) {
            cmThread = new Thread() {
                public void run() {
                    codeModelExtraction(hybridCache);
                }
            };
            cmThread.start();
        }
        
        Thread vmThread = null;
        if (config
            .getValue(IncrementalAnalysisSettings.EXTRACT_VARIABILITY_MODEL)) {
            vmThread = new Thread() {
                public void run() {
                    variabilityModelExtraction(hybridCache);
                }
            };
            vmThread.start();
        }

        Thread bmThread = null;
        if (config.getValue(IncrementalAnalysisSettings.EXTRACT_BUILD_MODEL)) {
            bmThread = new Thread() {
                public void run() {
                    buildModelExtraction(hybridCache);
                }
            };
            bmThread.start();
        }

        // wait for all model-threads to finish
        if (cmThread != null) {
            try {
                cmThread.join();
            } catch (InterruptedException e) {
                LOGGER.logException("Thread interrupted", e);
            }
        }

        if (vmThread != null) {
            try {
                vmThread.join();
            } catch (InterruptedException e) {
                LOGGER.logException("Thread interrupted", e);
            }
        }

        if (bmThread != null) {
            try {
                bmThread.join();
            } catch (InterruptedException e) {
                LOGGER.logException("Thread interrupted", e);
            }
        }
        // add results
        this.addResult(hybridCache);
    }

    /**
     * Variability model extraction.
     *
     * @param hybridCache
     *            the hybrid cache to write the extracated results to.
     */
    private void variabilityModelExtraction(HybridCache hybridCache) {
        VariabilityModel variabilityModel;
        // CHECKSTYLE:OFF
        if ((variabilityModel = vmComponent.getNextResult()) != null) {
            // CHECKSTYLE:ON
            try {
                hybridCache.write(variabilityModel);
            } catch (IOException e) {
                LOGGER.logException(
                    "Could not write variability-model to HybridCache", e);
            }
        } else {
            try {
                hybridCache.deleteVariabilityModel();
            } catch (IOException e) {
                LOGGER.logException(
                    "Could not delete variability-model from HybridCache", e);
            }
        }
    }

    /**
     * Build model extraction.
     *
     * @param hybridCache
     *            the hybrid cache to write the extracated results to.
     */
    private void buildModelExtraction(HybridCache hybridCache) {
        BuildModel buildModel;
        // CHECKSTYLE:OFF
        if ((buildModel = bmComponent.getNextResult()) != null) {
            // CHECKSTYLE:ON
            try {
                hybridCache.write(buildModel);
            } catch (IOException e) {
                LOGGER.logException(
                    "Could not write build-model to HybridCache", e);
            }
        } else {
            try {
                hybridCache.deleteBuildModel();
            } catch (IOException e) {
                LOGGER.logException(
                    "Could not delete build-model from HybridCache", e);
            }
        }
    }

    /**
     * Code model extraction.
     *
     * @param hybridCache
     *            the hybrid cache to write the extracted results to.
     */
    private void codeModelExtraction(HybridCache hybridCache) {
        SourceFile file;
        // We need access to the diff-file because we need to know which files
        // were removed through the diff
        DiffFile diffFile = null;
        File originalDiffFile =
            config.getValue(IncrementalAnalysisSettings.SOURCE_TREE_DIFF_FILE);
        File parsedDiffFile =
            new File(originalDiffFile.getAbsolutePath() + config
                .getValue(IncrementalAnalysisSettings.PARSED_DIFF_FILE_SUFFIX));
        ///////////////////////////////////////////////////////////
        // Deletion of models for files removed in the diff-file //
        ///////////////////////////////////////////////////////////
        if (parsedDiffFile.exists()) {
            LOGGER.logInfo("Reusing parsed diff-file: "
                + parsedDiffFile.getAbsolutePath());
            try {
                diffFile = DiffFile.load(parsedDiffFile);
            } catch (IOException | ParseException e) {
                LOGGER.logException("Could not reuse parsed diff-file: "
                    + parsedDiffFile.getAbsolutePath(), e);
            }
        }
        if (diffFile == null) {
            // Try to reuse existing parsed diff if available from preparati
            // If no parsed diff was available for reuse, generate a new
            // DiffFile-Object
            if (diffFile == null) {
                LOGGER.logInfo("Parsing original diff-file: "
                    + originalDiffFile.getAbsolutePath());
                try {
                    diffFile = new SimpleDiffAnalyzer()
                        .generateDiffFile(originalDiffFile);
                } catch (IOException e) {
                    String error = "This is a major problem as it might"
                        + " result in an inconsistent state of your"
                        + " HybridCache-directory.";
                    LOGGER.logError("Could not parse diff-file: "
                        + originalDiffFile.getAbsolutePath(), error);
                }
            }
        }
        // with the help of the diffFile, remove all models corresponding to
        // deleted files
        for (FileEntry entry : diffFile.getEntries()) {
            if (entry.getType().equals(FileEntry.Type.DELETION)) {
                try {
                    LOGGER.logDebug(
                        "Deleting model because of DiffEntry: " + entry);
                    hybridCache.deleteCodeModel(entry.getPath().toFile());
                } catch (IOException exception) {
                    LOGGER.logException("Could not delete CodeModel-File. "
                        + "This may result in an inconsistent state of Hybridcache. "
                        + "To fix an inconsistent state you can either do a rollback "
                        + "or extract all models from scratch.", exception);
                }
            }
        }
        ///////////////////////////////////
        // Add new models to hybridCache //
        ///////////////////////////////////
        while ((file = cmComponent.getNextResult()) != null) {
            try {
                hybridCache.write(file);
            } catch (IOException e) {
                LOGGER.logException("Could not write sourcefile to HybridCache",
                    e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.ssehub.kernel_haven.analysis.AnalysisComponent#getResultName()
     */
    @Override
    public String getResultName() {
        return "HybridCache";
    }

}