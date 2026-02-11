package edu.cnu.mdi.mdi3D.physics;


/**
 * @param <T> The type of data representing an entity state (e.g., ParticleState)
 */
public abstract class APhysicsModel<T> {
    public abstract void update();
    
    // Returns a snapshot containing a list of generic type T
    public abstract SimulationSnapshot<T> getSnapshot(); 
}

