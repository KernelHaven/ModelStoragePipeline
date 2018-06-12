package net.ssehub.kernel_haven.incremental.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

// TODO: Auto-generated Javadoc
/**
 * Special adapter class to enable any pipeline-analysis to run as an
 * incremental analysis.
 * 
 * Usage Example:
 * <p>
 * <blockquote>
 * 
 * <pre>
 * HybridCacheAdapter hca = new HybridCacheAdapter(config,
 * 		new IncrementalPostExtraction(config, getCmComponent(), getBmComponent(), getVmComponent()));
 *
 * DeadCodeFinder dcf = new DeadCodeFinder(config, hca.getVmComponent(), hca.getBmComponent(), hca.getCmComponent());
 * </pre>
 * 
 * </blockquote>
 * <p>
 * 
 * @author Moritz
 */
public final class HybridCacheAdapter extends AnalysisComponent<Void> {

	/**
	 * The Enum CodeModelProcessing.
	 */
	public enum CodeModelProcessing {
		/** Provides the complete codemodel to the next component. */
		COMPLETE,
		/**
		 * Provides a partial codemodel to the next component containing only newly
		 * extracted parts of the model.
		 */
		PARTIAL,
		/**
		 * Provides a partial codemodel to the next component containing only those
		 * parts of the codemodel that did change
		 */
		PARTIAL_OPTIMIZED;
	}

	/** The config. */
	private @NonNull Configuration config;

	/** The {@HybridCache} instance used as input component. */
	private @NonNull AnalysisComponent<HybridCache> inputComponent;

	/** The bm component. */
	private OutputComponent<BuildModel> bmComponent;

	/** The vm component. */
	private OutputComponent<VariabilityModel> vmComponent;

	/** The cm component. */
	private OutputComponent<SourceFile> cmComponent;

	/** The change set only for cm. */
	private CodeModelProcessing cmProcessing;

	/**
	 * Creates this double analysis component with the given input component.
	 *
	 * @param config
	 *            The global configuration.
	 * @param inputComponent
	 *            The component to get the results to pass to both other components.
	 * @param cmProcessing
	 *            the processing strategy for the codemodel
	 */
	public HybridCacheAdapter(@NonNull Configuration config, @NonNull AnalysisComponent<HybridCache> inputComponent,
			CodeModelProcessing cmProcessing) {
		super(config);
		this.config = config;
		bmComponent = new OutputComponent<BuildModel>(config, "HybridCacheAdapter-bmComponent");
		vmComponent = new OutputComponent<VariabilityModel>(config, "HybridCacheAdapter-vmComponent");
		cmComponent = new OutputComponent<SourceFile>(config, "HybridCacheAdapter-cmComponent");
		this.inputComponent = inputComponent;
		this.cmProcessing = cmProcessing;
	}

	/**
	 * Creates this double analysis component with the given input component. This
	 * will include the entire current model from the {@link HybridCache}
	 * inputComponent.
	 *
	 * @param config
	 *            The global configuration.
	 * @param inputComponent
	 *            The component to get the results to pass to both other components.
	 */
	public HybridCacheAdapter(@NonNull Configuration config, @NonNull AnalysisComponent<HybridCache> inputComponent) {
		this(config, inputComponent, CodeModelProcessing.COMPLETE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.ssehub.kernel_haven.analysis.AnalysisComponent#execute()
	 */
	@Override
	protected void execute() {
		long start = System.nanoTime();
		HybridCache data;

		if ((data = inputComponent.getNextResult()) != null) {
			try {
				// Get models
				Collection<SourceFile> codeModel;
				if (this.cmProcessing.equals(CodeModelProcessing.COMPLETE)) {
					codeModel = data.readCm();
				} else {
					codeModel = data.readCmNewlyWrittenParts();
				}

				BuildModel buildModel = data.readBm();
				VariabilityModel varModel = data.readVm();

				if (codeModel.isEmpty()) {
					LOGGER.logWarning(
							HybridCacheAdapter.class.getSimpleName() + " contains empty code model after execute()");
				}

				if (buildModel == null || buildModel.getSize() == 0) {
					LOGGER.logWarning(HybridCacheAdapter.class.getSimpleName()
							+ " contains none or an empty build model after execute()");
				}

				if (varModel == null || varModel.getVariables().size() == 0) {
					LOGGER.logWarning(HybridCacheAdapter.class.getSimpleName()
							+ " contains none or empty variability model after execute()");
				}

				// add Models to components
				for (SourceFile srcFile : codeModel) {
					if (srcFile == null) {
						throw new NullPointerException("SourceFile was null - this should never happen");
					} else {

						if (cmProcessing.equals(CodeModelProcessing.PARTIAL_OPTIMIZED)
								&& !srcFile.equals(data.readPreviousCm(srcFile.getPath()))) {
							cmComponent.myAddResult(srcFile);
						} else {
							cmComponent.myAddResult(srcFile);
						}

					}
				}
				
				// do not prevent null from being added
				// this is because some analysis implementations expect the null result
				// for failed extractions
				bmComponent.myAddResult(buildModel);
				vmComponent.myAddResult(varModel);

			} catch (IOException | FormatException e) {
				LOGGER.logException("Could not get models from HybridCache", e);
			}
		}

		bmComponent.done = true;
		synchronized (bmComponent) {
			bmComponent.notifyAll();
		}

		vmComponent.done = true;
		synchronized (bmComponent) {
			bmComponent.notifyAll();
		}

		cmComponent.done = true;
		synchronized (bmComponent) {
			bmComponent.notifyAll();
		}

		long totalTime = System.nanoTime() - start;
		LOGGER.logDebug(this.getClass().getSimpleName() + " duration:"
				+ TimeUnit.MILLISECONDS.convert(totalTime, TimeUnit.NANOSECONDS) + "ms");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.ssehub.kernel_haven.analysis.AnalysisComponent#getResultName()
	 */
	@Override
	public @NonNull String getResultName() {
		return "HybridSplitComponent";
	}

	/**
	 * The pseudo component that the next components will get as the input.
	 *
	 * @param <T>
	 *            the generic type
	 */
	private class OutputComponent<T> extends AnalysisComponent<T> {

		/** The done. */
		private volatile boolean done;

		/** The name. */
		private String name;

		/**
		 * Creates this output component.
		 *
		 * @param config
		 *            The global configuration.
		 * @param name
		 *            the name
		 */
		public OutputComponent(@NonNull Configuration config, String name) {
			super(config);
			this.name = name;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see net.ssehub.kernel_haven.analysis.AnalysisComponent#execute()
		 */
		@Override
		protected void execute() {
			// make sure that SplitComponent is started; multiple calls to start() will do
			// no harm
			HybridCacheAdapter.this.start();

			while (!done) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}

		/**
		 * Method to allow for access to {@link AnalysisComponent#addResult} from within
		 * {@link HybridCacheAdapter}.
		 *
		 * @param result
		 *            the result
		 */
		public void myAddResult(T result) {
			this.addResult(result);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see net.ssehub.kernel_haven.analysis.AnalysisComponent#getResultName()
		 */
		@Override
		public @NonNull String getResultName() {
			return this.name;
		}

	}

	/**
	 * Gets the vm component.
	 *
	 * @return the vm component
	 */
	public @NonNull AnalysisComponent<VariabilityModel> getVmComponent() {
		return this.vmComponent;
	}

	/**
	 * Gets the bm component.
	 *
	 * @return the bm component
	 */
	public @NonNull AnalysisComponent<BuildModel> getBmComponent() {
		return this.bmComponent;
	}

	/**
	 * Gets the cm component.
	 *
	 * @return the cm component
	 */
	public @NonNull AnalysisComponent<SourceFile> getCmComponent() {
		return this.cmComponent;
	}

}
