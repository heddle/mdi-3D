package edu.cnu.mdi.mdi3D.physics;

import java.util.concurrent.atomic.AtomicReference;

public class PhysicsEngine<T> implements Runnable {
    // Use CopyOnWriteArrayList or synchronized blocks for thread safety
	private final APhysicsModel<T> model; 
    private final AtomicReference<SimulationSnapshot<T>> latestSnapshot = new AtomicReference<>();
    private volatile boolean running = true;
    private volatile boolean paused = false;
    
    public PhysicsEngine(APhysicsModel<T> model) {
		this.model = model;
	}

    /**
     * Request the simulation thread to stop. 
     * The thread will exit gracefully after the current update cycle.
     */
    public void stop() {
        this.running = false;
    }

    /**
	 * Pause the simulation. The thread will stop updating the model until resumed.
	 */
	public void setPaused(boolean paused) {
		this.paused = paused;
	}
	
	/**
	 * @return true if the simulation is currently paused, false otherwise.
	 */
	public boolean isPaused() {
		return paused;
	}
	
	/**
	 * Convenience method to resume the simulation if it is paused.
	 */
	public void resume() {
		this.paused = false;
	}
    
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 120.0; // Target 120Hz

        while (running) {
            long now = System.nanoTime();
            if (!paused) {
                // Option A: Fixed timestep (call update multiple times if lagging)
                while (now - lastTime > nsPerTick) {
                    model.update();
                    lastTime += nsPerTick;
                }
                latestSnapshot.set(model.getSnapshot());
            } else {
                lastTime = now; // Don't "catch up" after unpausing
            }
            
            // Yield to other threads
            Thread.yield(); 
        }
    }
    
    /**
	 * @return the latest simulation snapshot. This may be called from any thread, 
	 * but the returned snapshot should be treated as immutable.
	 */
    public SimulationSnapshot<T> getLatestSnapshot() {
        return latestSnapshot.get();
    }
}