package edu.cnu.mdi.mdi3D.view3D.aizawaDemo;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;

/**
 * Simulation wrapper for the Aizawa attractor demo.
 * <p>
 * This class contains no UI code. It is executed on the simulation thread by
 * the MDI simulation engine.
 * </p>
 *
 * <p>
 * The model owns the numerical state and update logic. This wrapper integrates
 * that model into the simulation engine lifecycle.
 * </p>
 */
public class AizawaSimulation implements Simulation {

	/** Default safety cap on the number of simulation updates. */
	public static final int DEFAULT_MAX_STEPS = 200_000;

	/** The numerical model. */
	private final AizawaModel model;

	/** Current simulation step index. */
	private int step;

	/** Optional safety cap on steps. */
	private int maxSteps = DEFAULT_MAX_STEPS;

	/** Engine used to post status and request refreshes. */
	private SimulationEngine engine;

	/**
	 * Construct a simulation for the given model.
	 *
	 * @param model the Aizawa model; must not be null
	 */
	public AizawaSimulation(AizawaModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null");
		}
		this.model = model;
	}

	/**
	 * Get the numerical model.
	 *
	 * @return the model
	 */
	public AizawaModel getModel() {
		return model;
	}

	/**
	 * Attach the engine so this simulation can post messages and request refreshes.
	 *
	 * @param engine the simulation engine, possibly null
	 */
	public void setEngine(SimulationEngine engine) {
		this.engine = engine;
	}

	/**
	 * Set a safety cap on the number of simulation updates.
	 *
	 * @param maxSteps maximum number of updates; values less than 1 are promoted to 1
	 */
	public void setMaxSteps(int maxSteps) {
		this.maxSteps = Math.max(1, maxSteps);
	}

	@Override
	public void init(SimulationContext ctx) throws Exception {
		step = 0;

		if (engine != null) {
			engine.postMessage("Initialized Aizawa attractor model. Ready.");
			engine.postProgress(ProgressInfo.indeterminate("Ready"));
			engine.requestRefresh();
		}
	}

	@Override
	public boolean step(SimulationContext ctx) throws Exception {
		if (ctx.isCancelRequested()) {
			return false;
		}

		step++;
		model.update();

		return step < maxSteps;
	}
}