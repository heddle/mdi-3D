package edu.cnu.mdi.mdi3D.physics;

import java.util.concurrent.atomic.AtomicReference;

import edu.cnu.mdi.sim.SimulationState;

public class PhysicsEngine<T>  {
    // Use CopyOnWriteArrayList or synchronized blocks for thread safety
	private final APhysicsModel<T> model; 
    private final AtomicReference<SimulationSnapshot<T>> latestSnapshot = new AtomicReference<>();
	private volatile boolean pauseRequested = true; // Start paused by default
	private volatile boolean stopRequested = false;
	
	// State is volatile since it's read by the EDT and written by the simulation thread.
	private volatile SimulationState state = SimulationState.NEW;
	
	// The simulation thread that runs the main loop. 
	// Marked volatile to ensure visibility across threads.
	private volatile Thread simThread;

   /**
    * Create a new PhysicsEngine with the given model. The engine will 
    * manage the simulation loop and provide snapshots to clients.
    * @param model
    */
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

    /**
     * The main simulation loop. This runs on the simulation thread and 
     * repeatedly calls {@link APhysicsModel#update()} and updates the 
     * latest snapshot. It also respects the pause and stop flags to 
     * control execution.
     */
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