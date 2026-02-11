package edu.cnu.mdi.mdi3D.physics;

/**
 * Optimized snapshot containing raw coordinates for OpenGL.
 */
public record SimulationSnapshot<T>(java.util.List<T> entities, 
		float[] coords,
		float time,
		float entropy) {
}