package net.ssehub.kernel_haven.incremental.preparation;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.IPreparation;
import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.incremental.common.IncrementalAnalysisSettings;
import net.ssehub.kernel_haven.incremental.util.DiffIntegrationUtil;
import net.ssehub.kernel_haven.util.Logger;

/**
 * The Class IncrementalPreparation.
 */
public class IncrementalPreparation implements IPreparation {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = Logger.get();

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.ssehub.kernel_haven.IPreparation#run(net.ssehub.kernel_haven.config.
	 * Configuration)
	 */
	@Override
	public void run(Configuration config) throws SetUpException {
		IncrementalAnalysisSettings.registerAllSettings(config);

		File inputDiff = (File) config.getValue(IncrementalAnalysisSettings.SOURCE_TREE_DIFF_FILE);
		File inputSourceDir = (File) config.getValue(DefaultSettings.SOURCE_TREE);

		// Merge changes
		DiffIntegrationUtil mergeUtil = new DiffIntegrationUtil(inputSourceDir, inputDiff);
		boolean mergeSuccessful = mergeUtil.mergeChanges();

		// only continue if merge was successful
		if (!mergeSuccessful) {
			LOGGER.logError("Could not merge provided diff with existing input files!\n"
					+ "The diff-file must describe changes that can be applied to the set of input-files that are to be analyzed. \n"
					+ "Stopping execution of KernelHaven.");
			throw new SetUpException("Could not merge provided diff with existing input files!");
		} else {

			//////////////////////////
			// Filter for codemodel //
			//////////////////////////
			Collection<Path> filteredPaths = filterInput(
					config.getValue(IncrementalAnalysisSettings.CODE_MODEL_FILTER_CLASS), inputSourceDir, inputDiff,
					config.getValue(DefaultSettings.CODE_EXTRACTOR_FILE_REGEX));

			if (!filteredPaths.isEmpty()) {
				config.setValue(IncrementalAnalysisSettings.EXTRACT_CODE_MODEL, true);
				ArrayList<String> pathStrings = new ArrayList<String>();
				filteredPaths.forEach(path -> pathStrings.add(path.toString()));
				config.setValue(DefaultSettings.CODE_EXTRACTOR_FILES, pathStrings);
				// If no paths are included after filtering, the extraction does not need to run
			} else {
				config.setValue(IncrementalAnalysisSettings.EXTRACT_CODE_MODEL, false);
			}

			//////////////////////////////////
			// Filter for variability model //
			//////////////////////////////////
			filteredPaths = filterInput(config.getValue(IncrementalAnalysisSettings.VARIABILITY_MODEL_FILTER_CLASS),
					inputSourceDir, inputDiff, config.getValue(DefaultSettings.VARIABILITY_EXTRACTOR_FILE_REGEX));
			config.setValue(IncrementalAnalysisSettings.EXTRACT_VARIABILITY_MODEL, !filteredPaths.isEmpty());

			////////////////////////////
			// Filter for build model //
			////////////////////////////
			filteredPaths = filterInput(config.getValue(IncrementalAnalysisSettings.BUILD_MODEL_FILTER_CLASS),
					inputSourceDir, inputDiff, config.getValue(DefaultSettings.BUILD_EXTRACTOR_FILE_REGEX));
			config.setValue(IncrementalAnalysisSettings.EXTRACT_BUILD_MODEL, !filteredPaths.isEmpty());

		}

		// Finish and let KernelHaven run
	}

	@SuppressWarnings("unchecked")
	protected Collection<Path> filterInput(String filterClassName, File inputSourceDir, File inputDiff, Pattern regex)
			throws SetUpException {
		Collection<Path> paths = null;
		// Call the method getFilteredResult for filterClassName via reflection-api
		try {
			@SuppressWarnings("rawtypes")
			Class filterClass = Class.forName(filterClassName);
			Object filterObject = filterClass.getConstructor(File.class, File.class, Pattern.class)
					.newInstance(inputSourceDir, inputDiff, regex);
			if (filterObject instanceof InputFilter) {
				Method getFilteredResultMethod = filterClass.getMethod("getFilteredResult");
				paths = (Collection<Path>) getFilteredResultMethod.invoke(filterObject);
			} else {
				throw new SetUpException(
						"The class name provided for the filter does not appear to extend the InputFilter class");
			}

		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException
				| InvocationTargetException e) {
			LOGGER.logException("The specified filter class could not be used", e);
			throw new SetUpException("The specified filter could not be used: " + e.getMessage());
		}
		return paths;

	}

}