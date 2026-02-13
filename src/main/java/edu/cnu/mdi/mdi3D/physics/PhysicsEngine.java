package edu.cnu.mdi.mdi3D.physics;

import java.util.concurrent.atomic.AtomicReference;

public class PhysicsEngine<T>  {
    // Use CopyOnWriteArrayList or synchronized blocks for thread safety
	private final APhysicsModel<T> model; 
    private final AtomicReference<SimulationSnapshot<T>> latestSnapshot = new AtomicReference<>();
	private volatile boolean pauseRequested = false;
	private volatile boolean stopRequested = false;
	
	private volatile Thread simThread;

   
    public PhysicsEngine(APhysicsModel<T> model) {
		this.model = model;
	}
    
    
    /**
	 * Start the simulation engine in its own thread.
	 * <p>
	 * This is safe to call multiple times; only the first call has an effect.
	 * Note that this will cause the thread to run indefinitely until {@link #stop()} 
	 * is called. The actual simulation will not run until
	 * {@link #requestPaused(boolean)} is called with false.
	 */
    public synchronized void start() {
		if (simThread != null) {
			return;
		}
		stopRequested = false;
		pauseRequested = false;

		simThread = new Thread(this::runLoop, "SimulationEngine");
		simThread.setDaemon(true);
		simThread.start();
	}
    
	/**
	 * Alias for {@link #requestResume()}.
	 */
	public void requestRun() {
		if (simThread == null) {
			start();
		}
		requestResume();
	}

	/**
	 * Request a pause.
	 */
	public void requestPause() {
		pauseRequested = true;
	}

	/**
	 * Request resume from PAUSED (or start running from READY).
	 */
	public void requestResume() {
		pauseRequested = false;
	}
	

	/**
	 * Request stop (normal termination).
	 */
	public void requestStop() {
		stopRequested = true;
		pauseRequested = false;
	}

	/**
	 * Request cancellation (cooperative).
	 */
	public void requestCancel() {
		pauseRequested = false;
	}

    
    public void runLoop() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 120.0; // Target 120Hz

        while (!stopRequested) {
            long now = System.nanoTime();
            if (!pauseRequested) {
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