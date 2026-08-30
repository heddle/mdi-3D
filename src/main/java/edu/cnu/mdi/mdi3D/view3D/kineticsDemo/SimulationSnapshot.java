package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

/**
 * Optimized snapshot containing raw coordinates for OpenGL.
 * <p>
 * Rendering uses only {@link #coords()}; {@link #entities()} is not currently
 * populated by {@link KineticsModel#getSnapshot()} (callers pass {@code null})
 * but is retained as a typed hook for a future renderer that needs per-particle
 * data rather than just packed positions.
 * </p>
 *
 * @param <T>      the entity type, if {@link #entities()} is populated
 * @param entities optional per-entity data corresponding to {@link #coords()};
 *                 may be {@code null}
 * @param coords   packed particle positions as {@code [x1, y1, z1, x2, y2, z2, ...]}
 * @param time     simulation time at which the snapshot was taken
 * @param entropy  entropy value at the time of the snapshot
 */
public record SimulationSnapshot<T>(java.util.List<T> entities,
		float[] coords,
		float time,
		float entropy) {
}