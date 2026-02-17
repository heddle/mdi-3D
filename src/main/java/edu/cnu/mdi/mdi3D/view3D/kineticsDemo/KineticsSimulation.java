package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;

/**
 * Simulation wrapper for the kinetic-gas demo.
 * <p>
 * This class contains <b>no UI code</b>. It is executed on the simulation thread by
 * {@link SimulationEngine}. Any UI updates must be requested through the engine
 * ({@link SimulationEngine#requestRefresh()}, {@link SimulationEngine#postProgress(ProgressInfo)}, etc.).
 * </p>
 */
public class KineticsSimulation implements Simulation {

	/** The physics model that defines the state and update logic. */
	private final KineticsModel model;

	/** Current step index. */
	private int step;

	/** Hard stop after this many steps (safety / demo). */
	private int maxSteps = 2000;

	/** Simulation engine used for posting progress/messages/refresh. */
	private SimulationEngine engine;

	/**
	 * Constructs a KineticsSimulation with the given model.
	 *
	 * @param model The {@link KineticsModel} to use. Must not be null.
	 */
	public KineticsSimulation(KineticsModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null");
		}
		this.model = model;
	}

	/**
	 * Returns the physics model.
	 *
	 * @return model (never null)
	 */
	public KineticsModel getModel() {
		return model;
	}

	/**
	 * Attach the engine so this simulation can post messages/progress/refresh.
	 * <p>
	 * Called by the hosting view after it constructs the {@link SimulationEngine}.
	 * </p>
	 *
	 * @param engine engine executing this simulation (may be null)
	 */
	public void setEngine(SimulationEngine engine) {
		this.engine = engine;
	}

	/**
	 * Set a safety cap on the number of steps.
	 *
	 * @param maxSteps max steps (>= 1)
	 */
	public void setMaxSteps(int maxSteps) {
		this.maxSteps = Math.max(1, maxSteps);
	}

	@Override
	public void init(SimulationContext ctx) throws Exception {
		step = 0;

		// First refresh so the view can paint the initial snapshot.
		if (engine != null) {
			engine.postMessage("Initialized kinetics model (" + model.size() + " particles). Ready.");
			engine.postProgress(ProgressInfo.indeterminate("Ready"));
			engine.requestRefresh();
		}
	}

	@Override
	public boolean step(SimulationContext ctx) throws Exception {
		// terminate if stop/cancel requested
		if (ctx.isCancelRequested()) {
			return false;
		}

		step++;
		model.update();

		// Let the engine handle refresh rate limiting via its config.
		return step < maxSteps;
	}
}
