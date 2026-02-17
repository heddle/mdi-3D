package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A physics model simulating particles in a 3D box with elastic collisions.
 * <p>
 * The model maintains an internal mutable state (positions + velocities) and
 * exposes an optimized, UI-friendly {@link SimulationSnapshot} containing only
 * a reference to a packed {@code float[]} coordinate buffer.
 * </p>
 *
 * <h2>Double buffering (front/back)</h2>
 * <ul>
 *   <li>The simulation thread writes new positions into the <em>back</em> buffer.</li>
 *   <li>After the update, buffers are swapped so the <em>front</em> buffer is always
 *       the most recently completed frame.</li>
 *   <li>{@link #getSnapshot()} is <b>side-effect free</b> and simply returns the
 *       current front buffer.</li>
 * </ul>
 */
public class KineticsModel {
	// This is the complete internal state of the simulation, including velocities.
	// The public snapshot only exposes positions (raw coords), which is all the GUI needs.

	private final List<PhysicsParticle> internalState = new ArrayList<>();
	private final float length; // side length of bounding cube

	// Double buffers for coordinates (packed x,y,z)
	private final float[] bufferA;
	private final float[] bufferB;
	private volatile float[] frontBuffer;
	private volatile float[] backBuffer;

	private float temperature;
	private float time = 0f;
	private float timeStep = 0.0083f; // ~120 updates per second

	// Cached entropy computed on the simulation thread.
	private volatile float lastEntropy = 0f;

	// Optional rate-limit for entropy computation (compute every N updates)
	private int entropyEvery = 1;
	private int entropyCounter = 0;

	/**
	 * Initializes the simulation.
	 *
	 * @param count          number of particles
	 * @param length         side length of the bounding cube
	 * @param volumeFraction fraction of the cube to populate initially (e.g. 0.25)
	 * @param initialTemp    initial "temperature" (sets RMS speed; sigma = sqrt(T))
	 */
	public KineticsModel(int count, float length, float volumeFraction, float initialTemp) {
		this.temperature = initialTemp;
		this.length = length;
		this.bufferA = new float[count * 3];
		this.bufferB = new float[count * 3];
		this.frontBuffer = bufferA;
		this.backBuffer = bufferB;

		Random rnd = new Random();

		// Initialize particles in a sub-cube to create a low-entropy starting state.
		float subBound = length * volumeFraction;

		// In sim units, sigma = sqrt(T)
		float sigma = (float) Math.sqrt(temperature);

		for (int i = 0; i < count; i++) {
			float px = rnd.nextFloat() * subBound;
			float py = rnd.nextFloat() * subBound;
			float pz = rnd.nextFloat() * subBound;

			// Gaussian velocities create a Maxwell-Boltzmann distribution
			float vx = (float) (rnd.nextGaussian() * sigma);
			float vy = (float) (rnd.nextGaussian() * sigma);
			float vz = (float) (rnd.nextGaussian() * sigma);

			internalState.add(new PhysicsParticle(px, py, pz, vx, vy, vz));
		}

		// Seed the front buffer with the initial positions
		initializeFrontBuffer();
		lastEntropy = computeEntropy();
	}

	/**
	 * Returns the number of particles.
	 *
	 * @return particle count
	 */
	public int size() {
		return internalState.size();
	}

	/**
	 * Set the simulation time step.
	 *
	 * @param dt time step (seconds in simulation units)
	 */
	public void setTimeStep(float dt) {
		if (dt > 0) {
			this.timeStep = dt;
		}
	}

	/**
	 * Optional rate-limit for entropy computation.
	 *
	 * @param every compute entropy every N updates (>=1)
	 */
	public void setEntropyEvery(int every) {
		this.entropyEvery = Math.max(1, every);
	}

	// Populate the initial coordinate buffer from the internal state.
	private void initializeFrontBuffer() {
		for (int i = 0; i < internalState.size(); i++) {
			PhysicsParticle p = internalState.get(i);
			frontBuffer[3 * i] = p.x;
			frontBuffer[3 * i + 1] = p.y;
			frontBuffer[3 * i + 2] = p.z;
		}
	}

	/**
	 * Advance the simulation by one step.
	 * <p>
	 * This method is intended to run on the simulation thread.
	 * </p>
	 */
	public void update() {
		// Write into backBuffer
		final float[] out = backBuffer;

		for (int i = 0; i < internalState.size(); i++) {
			PhysicsParticle p = internalState.get(i);
			p.move(timeStep, length);

			out[3 * i] = p.x;
			out[3 * i + 1] = p.y;
			out[3 * i + 2] = p.z;
		}

		time += timeStep;

		// Entropy (optionally rate-limited)
		entropyCounter++;
		if (entropyCounter >= entropyEvery) {
			lastEntropy = computeEntropy();
			entropyCounter = 0;
		}

		// Swap front/back so the finished frame becomes visible to the EDT.
		swapBuffers();
	}

	// Single point of truth for swapping buffers.
	private void swapBuffers() {
		final float[] oldFront = frontBuffer;
		frontBuffer = backBuffer;
		backBuffer = oldFront;
	}

	/**
	 * Get an optimized snapshot of the most recently completed simulation state.
	 * <p>
	 * This method is side-effect free and safe to call from the EDT.
	 * </p>
	 *
	 * @return snapshot containing a reference to the current front buffer
	 */
	public SimulationSnapshot<Particle> getSnapshot() {
		return new SimulationSnapshot<>(null, frontBuffer, time, lastEntropy);
	}

	/**
	 * Computes the Shannon entropy of the system based on a coarse 3D occupancy histogram.
	 * <p>
	 * This is O(N) in the number of particles and is intended to run on the simulation thread.
	 * </p>
	 *
	 * @return entropy in nats
	 */
	public float computeEntropy() {
		int[][][] histogram = new int[10][10][10];
		for (PhysicsParticle p : internalState) {
			int hx = Math.min((int) (p.x / (length / 10f)), 9);
			int hy = Math.min((int) (p.y / (length / 10f)), 9);
			int hz = Math.min((int) (p.z / (length / 10f)), 9);
			histogram[hx][hy][hz]++;
		}

		double entropy = 0.0;
		int totalParticles = internalState.size();
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 10; y++) {
				for (int z = 0; z < 10; z++) {
					int count = histogram[x][y][z];
					if (count > 0) {
						double p = (double) count / totalParticles;
						entropy -= p * Math.log(p);
					}
				}
			}
		}
		return (float) entropy;
	}

	/**
	 * Adjust the temperature (scales particle velocities).
	 *
	 * @param newTemp new temperature (>0)
	 */
	public void setTemperature(float newTemp) {
		if (newTemp <= 0) {
			return;
		}
		float oldSigma = (float) Math.sqrt(this.temperature);
		this.temperature = newTemp;
		float newSigma = (float) Math.sqrt(this.temperature);
		float ratio = newSigma / oldSigma;

		for (PhysicsParticle p : internalState) {
			p.vx *= ratio;
			p.vy *= ratio;
			p.vz *= ratio;
		}
	}

	/**
	 * Internal mutable class for physics calculations.
	 * Keeps velocity out of the public snapshot.
	 */
	private static class PhysicsParticle {
		float x, y, z;
		float vx, vy, vz;

		PhysicsParticle(float x, float y, float z, float vx, float vy, float vz) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.vx = vx;
			this.vy = vy;
			this.vz = vz;
		}

		void move(float dt, float dmax) {
			// Update position
			x += vx * dt;
			y += vy * dt;
			z += vz * dt;

			// Elastic collisions with walls (reverse velocity component)
			if (x < 0 || x > dmax) {
				vx *= -1;
				x = Math.max(0, Math.min(dmax, x));
			}
			if (y < 0 || y > dmax) {
				vy *= -1;
				y = Math.max(0, Math.min(dmax, y));
			}
			if (z < 0 || z > dmax) {
				vz *= -1;
				z = Math.max(0, Math.min(dmax, z));
			}
		}
	}
	
	// Optional reset method to reinitialize the simulation with new parameters.
	public void reset(int count, float length, float volumeFraction, float initialTemp) {
		internalState.clear();
		this.temperature = initialTemp;
		this.time = 0f;

		Random rnd = new Random();
		float subBound = length * volumeFraction;
		float sigma = (float) Math.sqrt(temperature);

		for (int i = 0; i < count; i++) {
			float px = rnd.nextFloat() * subBound;
			float py = rnd.nextFloat() * subBound;
			float pz = rnd.nextFloat() * subBound;

			float vx = (float) (rnd.nextGaussian() * sigma);
			float vy = (float) (rnd.nextGaussian() * sigma);
			float vz = (float) (rnd.nextGaussian() * sigma);

			internalState.add(new PhysicsParticle(px, py, pz, vx, vy, vz));
		}

		initializeFrontBuffer();
		lastEntropy = computeEntropy();
	}
	
}
