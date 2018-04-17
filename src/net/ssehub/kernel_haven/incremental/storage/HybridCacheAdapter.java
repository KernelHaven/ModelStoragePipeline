package net.ssehub.kernel_haven.incremental.storage;

import java.io.IOException;
import java.util.Collection;

import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

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

	/** The config. */
	private @NonNull Configuration config;

	/** The {@HybridCache} instance used as input component. */
	private @NonNull AnalysisComponent<HybridCache> inputComponent;

	/** The bm component. */
	private OutputComponent<BuildModel> bmComponent = new OutputComponent<BuildModel>(config,
			"HybridCacheAdapter-bmComponent");

	/** The vm component. */
	private OutputComponent<VariabilityModel> vmComponent = new OutputComponent<VariabilityModel>(config,
			"HybridCacheAdapter-vmComponent");;

	/** The cm component. */
	private OutputComponent<SourceFile> cmComponent = new OutputComponent<SourceFile>(config,
			"HybridCacheAdapter-cmComponent");;

	/** The change set only for cm. */
	private boolean changeSetOnlyForCm = false;

	/**
	 * Creates this double analysis component with the given input component.
	 *
	 * @param config
	 *            The global configuration.
	 * @param inputComponent
	 *            The component to get the results to pass to both other components.
	 * @param changeSetOnlyForCm
	 *            defines whether only the newly extracted files of the codemodel
	 *            should be used.
	 */
	public HybridCacheAdapter(@NonNull Configuration config, @NonNull AnalysisComponent<HybridCache> inputComponent,
			boolean changeSetOnlyForCm) {
		super(config);
		this.config = config;
		this.inputComponent = inputComponent;
		this.changeSetOnlyForCm = changeSetOnlyForCm;
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
		super(config);
		this.config = config;
		this.inputComponent = inputComponent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.ssehub.kernel_haven.analysis.AnalysisComponent#execute()
	 */
	@Override
	protected void execute() {
		HybridCache data;

		if ((data = inputComponent.getNextResult()) != null) {
			try {
				// Get models
				Collection<SourceFile> codeModel;
				if (changeSetOnlyForCm) {
					codeModel = data.readChangedCm();
				} else {
					codeModel = data.readAllCm();
				}
				BuildModel buildModel = data.readBm();
				VariabilityModel varModel = data.readVm();

				// add Models to components
				for (SourceFile srcFile : codeModel) {
					cmComponent.myAddResult(srcFile);
				}
				if (buildModel != null) {
					bmComponent.myAddResult(buildModel);
				}

				if (varModel != null) {
					vmComponent.myAddResult(varModel);
				}
			} catch (IOException | FormatException e) {
				LOGGER.logException("Could not get code model from HybridCache", e);
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