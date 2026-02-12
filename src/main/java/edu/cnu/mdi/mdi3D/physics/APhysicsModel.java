package edu.cnu.mdi.mdi3D.physics;


/**
 * @param <T> The type of data representing an entity state (e.g., ParticleState)
 */
public abstract class APhysicsModel<T> {
	
	/**
	 * Updates the physics model by one time step.
	 */
    public abstract void update();
    
    /**
	 * Retrieves a snapshot of the current state of the simulation.
	 * 
	 * @return A SimulationSnapshot containing the current state.
	 */
    public abstract SimulationSnapshot<T> getSnapshot(); 
}

