package edu.cnu.mdi.mdi3D.view3D.aizawaDemo;

/**
 * Immutable snapshot of the Aizawa attractor state for rendering and diagnostics.
 * <p>
 * The simulation thread produces snapshots, and the EDT consumes them.
 * The packed coordinate array contains the currently visible trajectory in the form:
 *
 * <pre>
 * [x1, y1, z1, x2, y2, z2, ..., xn, yn, zn]
 * </pre>
 *
 * where each consecutive triple is one point on the trajectory.
 *
 * @param coords the packed trajectory coordinates; never modified by the receiver
 * @param time the current simulation time
 * @param x the current x coordinate
 * @param y the current y coordinate
 * @param z the current z coordinate
 */
public record AizawaSnapshot(float[] coords, float time, float x, float y, float z) {
}