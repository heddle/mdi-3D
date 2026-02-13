package edu.cnu.mdi.mdi3D.physics;

import edu.cnu.mdi.sim.Simulation;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationState;

/**
 * Convenience interface for any object that *hosts* a {@link PhysicsEngine}.
 * <p>
 * This interface is intentionally minimal and MDI-agnostic. It provides:
 * <ul>
 * <li>a single accessor for the underlying {@link PhysicsEngine}</li>
 * <li>default lifecycle control methods (start, run, pause, resume, stop,
 * cancel)</li>
 * </ul>
 * </p>
 *
 * <h2>Typical Usage</h2>
 *
 * <p>
 * By implementing this interface, UI code (toolbars, menus, control panels) can
 * manipulate simulations in a uniform way without knowing implementation
 * details.
 * </p>
 */
public interface IPhysicsSimHost {

	/**
	 * Return the {@link PhysicsEngine} hosted by this object.
	 *
	 * @return the simulation engine (never null)
	 */
	PhysicsEngine<?> getPhysicsEngine();

	// ------------------------------------------------------------------------
	// Convenience lifecycle control methods
	// ------------------------------------------------------------------------

	/**
	 * Start the simulation engine.
	 * <p>
	 * This is safe to call multiple times; only the first call has an effect.
	 * </p>
	 */
	default void startSimulation() {
		getPhysicsEngine().start();
	}

	/**
	 * Request the simulation to begin or continue running.
	 * <p>
	 * If the engine is paused, this resumes execution. If the engine is READY and
	 * {@code autoRun == false}, this begins execution.
	 * </p>
	 */
	default void runSimulation() {
		getPhysicsEngine().requestRun();
	}

	/**
	 * Request that the simulation pause.
	 * <p>
	 * The engine will transition to {@link SimulationState#PAUSED} at the next safe
	 * point.
	 * </p>
	 */
	default void pauseSimulation() {
		getPhysicsEngine().requestPause();
	}

	/**
	 * Resume a paused simulation.
	 * <p>
	 * This is equivalent to {@link #runSimulation()}.
	 * </p>
	 */
	default void resumeSimulation() {
		getPhysicsEngine().requestRun();
	}

	/**
	 * Request a normal stop.
	 * <p>
	 * The simulation will terminate and invoke
	 * {@link Simulation#shutdown(SimulationContext)} on the simulation thread.
	 * </p>
	 */
	default void stopSimulation() {
		getPhysicsEngine().requestStop();
	}

	/**
	 * Request cancellation.
	 * <p>
	 * Cancellation is cooperative. The simulation should observe
	 * {@link SimulationContext#isCancelRequested()} and exit promptly.
	 * </p>
	 */
	default void cancelSimulation() {
		//TODO implement
	}

	/**
	 * Convenience method to query the current simulation state.
	 *
	 * @return current {@link SimulationState}
	 */
	default SimulationState getSimulationState() {
		//TODO implement
		return null;
	}

	/**
	 * Convenience method to query the simulation context.
	 *
	 * @return simulation context
	 */
	default SimulationContext getSimulationContext() {
		return null; //TODO implement
	}
}