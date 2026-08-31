package edu.cnu.mdi.mdi3D.view3D.logoDemo;

import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngine;

/**
 * Simulation wrapper for the rotating MDI-logo demo.
 * <p>
 * This class contains no UI code. It is executed on the simulation thread by
 * the MDI simulation engine, exactly like {@code AizawaSimulation} and
 * {@code KineticsSimulation}.
 * </p>
 * <p>
 * Unlike those two demos, this simulation has no numerical model to
 * integrate — each step only advances a rotation angle by a fixed increment.
 * Because that has essentially zero computational cost, {@link #step} sleeps
 * briefly before doing its (trivial) work, so the simulation thread
 * self-paces instead of busy-spinning and consuming a full CPU core for no
 * visual benefit. {@link SimulationEngine} applies no throttling of its own
 * between {@code step()} calls, so a step body that is this cheap must pace
 * itself.
 * </p>
 */
public class LogoSimulation implements Simulation {

	/** Default safety cap on the number of steps (about 4.6 hours at the default rate). */
	public static final int DEFAULT_MAX_STEPS = 1_000_000;

	/** Default animation-angle increment, in degrees, advanced per step. */
	public static final float DEFAULT_DEGREES_PER_STEP = 0.6f;

	/** Default self-paced delay between steps, in milliseconds. */
	public static final long DEFAULT_STEP_SLEEP_MS = 16L;

	/** Animation-angle increment, in degrees, advanced on each step. */
	private final float degreesPerStep;

	/** Self-paced delay at the start of each step, in milliseconds. */
	private final long stepSleepMs;

	/** Current step index. */
	private int step;

	/** Optional safety cap on steps. */
	private int maxSteps = DEFAULT_MAX_STEPS;

	/** Engine used to post status and request refreshes. */
	private SimulationEngine engine;

	/** Guards {@link #pendingDeltaDeg} against the step()/takePendingDelta() race. */
	private final Object lock = new Object();

	/** Rotation (degrees) accumulated since the last {@link #takePendingDelta()} call. */
	private float pendingDeltaDeg;

	/**
	 * Construct the logo-spin simulation with the default rate.
	 */
	public LogoSimulation() {
		this(DEFAULT_DEGREES_PER_STEP, DEFAULT_STEP_SLEEP_MS);
	}

	/**
	 * Construct the logo-spin simulation with an explicit rate.
	 *
	 * @param degreesPerStep animation-angle increment, in degrees, per step
	 * @param stepSleepMs    milliseconds to sleep at the start of each step, so
	 *                       the simulation thread self-paces instead of
	 *                       busy-spinning
	 */
	public LogoSimulation(float degreesPerStep, long stepSleepMs) {
		this.degreesPerStep = degreesPerStep;
		this.stepSleepMs = Math.max(0L, stepSleepMs);
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
	 * Set a safety cap on the number of steps.
	 *
	 * @param maxSteps maximum number of steps; values less than 1 are promoted to 1
	 */
	public void setMaxSteps(int maxSteps) {
		this.maxSteps = Math.max(1, maxSteps);
	}

	/**
	 * Take the animation-angle increment accumulated since the last call and reset the
	 * accumulator to zero.
	 * <p>
	 * Intended to be called only from an EDT-marshaled {@code onSimulationRefresh}
	 * hook; the increment side runs on the simulation thread in {@link #step}.
	 * Both sides share {@link #lock} so a refresh can never observe a torn
	 * (partially-added) accumulation.
	 * </p>
	 *
	 * @return the accumulated animation-angle increment, in degrees, since the previous call
	 */
	public float takePendingDelta() {
		synchronized (lock) {
			float d = pendingDeltaDeg;
			pendingDeltaDeg = 0f;
			return d;
		}
	}

	@Override
	public void init(SimulationContext ctx) throws Exception {
		step = 0;
		synchronized (lock) {
			pendingDeltaDeg = 0f;
		}

		if (engine != null) {
			engine.postMessage("MDI logo demo ready.");
			engine.postProgress(ProgressInfo.indeterminate("Spinning"));
			engine.requestRefresh();
		}
	}

	@Override
	public boolean step(SimulationContext ctx) throws Exception {
		if (ctx.isCancelRequested()) {
			return false;
		}

		if (stepSleepMs > 0) {
			try {
				Thread.sleep(stepSleepMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}

		step++;
		synchronized (lock) {
			pendingDeltaDeg += degreesPerStep;
		}

		return step < maxSteps;
	}
}
