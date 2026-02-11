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

    public void stop() {
        this.running = false;
    }

	public void setPaused(boolean paused) {
		this.paused = paused;
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
    
    public SimulationSnapshot<T> getLatestSnapshot() {
        return latestSnapshot.get();
    }
}