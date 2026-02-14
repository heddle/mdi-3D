package edu.cnu.mdi.mdi3D.view3D.demo;

import edu.cnu.mdi.mdi3D.physics.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KineticsModel extends APhysicsModel<Particle> {
    private final List<PhysicsParticle> internalState = new ArrayList<>();
    private final float dmax = 1.0f; // The side length of the bounding cube
    
 // Double buffers for coordinates
    private float[] bufferA;
    private float[] bufferB;
    private boolean useBufferA = true;
    private float temperature;
    private float time = 0;
     
    /**
     * Initializes the simulation.
     * @param count Number of particles.
     * @param volumeFraction Fraction of the dmax cube to populate (e.g., 0.5 for half volume).
     * @param initialTemp Initial temperature, which determines the velocity distribution of the particles.
     */
    public KineticsModel(int count, double volumeFraction, float initialTemp) {
        this.temperature = initialTemp;
        this.bufferA = new float[count * 3];
        this.bufferB = new float[count * 3];
        
        Random rnd = new Random();
        float subBound = (float) (dmax * volumeFraction);
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
        initializeBuffers();
    }
    
    // Populate the initial coordinate buffers from the internal state
    private void initializeBuffers() {
		for (int i = 0; i < internalState.size(); i++) {
			PhysicsParticle p = internalState.get(i);
			bufferA[3 * i] = p.x;
			bufferA[3 * i + 1] = p.y;
			bufferA[3 * i + 2] = p.z;
		}
	}

    @Override
    public void update() {
        float dt = 0.0083f;
        float[] activeBuffer = useBufferA ? bufferA : bufferB;
        
        for (int i = 0; i < internalState.size(); i++) {
            PhysicsParticle p = internalState.get(i);
            p.move(dt, dmax);

			activeBuffer[3 * i] = p.x;
			activeBuffer[3 * i + 1] = p.y;
			activeBuffer[3 * i + 2] = p.z;
		}
        time += dt;
	}

    @Override
    public SimulationSnapshot<Particle> getSnapshot() {
        // Return the buffer that was just completed in update()
        float[] snapshotCoords = useBufferA ? bufferA : bufferB;
        useBufferA = !useBufferA; // Swap for next update
        
        // Passing null for entities list to optimize for 10k particles
        return new SimulationSnapshot<>(null, snapshotCoords, time, computeEntropy());
    }
    
    public float computeEntropy() {
		// Compute Shannon-Boltzmann entropy
		int [][][] histogram = new int[10][10][10];
		for (PhysicsParticle p : internalState) {
			int hx = Math.min((int) (p.x / (dmax / 10)), 9);
			int hy = Math.min((int) (p.y / (dmax / 10)), 9);
			int hz = Math.min((int) (p.z / (dmax / 10)), 9);
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
	 * Adjusts the temperature of the system, which affects particle velocities.
	 * The velocities are scaled to maintain a Maxwell-Boltzmann distribution consistent with the new temperature.
	 *
	 * @param newTemp the new temperature to set
	 */
    public void setTemperature(float newTemp) {
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
     * Keeps velocity out of the public GUI record.
     */
    private static class PhysicsParticle {
        float x, y, z;
        float vx, vy, vz;

        PhysicsParticle(float x, float y, float z, float vx, float vy, float vz) {
            this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = vy; this.vz = vz;
        }

        void move(float dt, float dmax) {
            // Update position
            x += vx * dt;
            y += vy * dt;
            z += vz * dt;

            // Elastic collisions with walls (reverse velocity component)
            if (x < 0 || x > dmax) vx *= -1;
            if (y < 0 || y > dmax) vy *= -1;
            if (z < 0 || z > dmax) vz *= -1;
        }
    }

}