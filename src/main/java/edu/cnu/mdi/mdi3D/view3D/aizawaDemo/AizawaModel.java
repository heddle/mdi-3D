package edu.cnu.mdi.mdi3D.view3D.aizawaDemo;

import java.util.Arrays;

import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;

/**
 * Numerical model for the five-parameter Aizawa attractor.
 * <p>
 * This class owns the complete evolving state of the attractor and performs
 * numerical integration using Apache Commons Math.
 * </p>
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>This class contains no UI code.</li>
 *   <li>The simulation thread is expected to call {@link #update()}.</li>
 *   <li>The EDT is expected to call {@link #getSnapshot()}.</li>
 *   <li>For simplicity, snapshot publication uses a copied packed coordinate array.</li>
 * </ul>
 *
 * <h2>Trajectory storage</h2>
 * <p>
 * The trajectory is stored internally in a packed float array:
 * </p>
 *
 * <pre>
 * [x1, y1, z1, x2, y2, z2, ..., xn, yn, zn]
 * </pre>
 *
 * <p>
 * If a maximum point count is enforced and the trajectory exceeds that size,
 * the oldest point is discarded by shifting the packed array left by one point.
 * This is simple and adequate for demo-scale simulations.
 * </p>
 */
public class AizawaModel {

	/** Default RK4 time step. */
	public static final double DEFAULT_DT = 0.01;

	/** Default number of integration substeps per simulation update. */
	public static final int DEFAULT_SUBSTEPS_PER_UPDATE = 8;

	/** Default maximum number of trajectory points to retain. */
	public static final int DEFAULT_MAX_TRAIL_POINTS = 10_000;

	/** Default initial trajectory buffer capacity, in points. */
	public static final int DEFAULT_INITIAL_CAPACITY = 512;

	/** The differential equation system. */
	private final AizawaEquations equations;

	/** Numerical integrator. */
	private final FirstOrderIntegrator integrator;

	/** Time step used for one RK4 advance. */
	private final double dt;

	/** Number of RK4 substeps taken during each {@link #update()}. */
	private final int substepsPerUpdate;

	/**
	 * Maximum number of trajectory points to retain.
	 * A value less than or equal to zero means unbounded growth.
	 */
	private final int maxTrailPoints;

	/** Current simulation time. */
	private double time;

	/** Current state vector [x, y, z]. */
	private final double[] state = new double[3];

	/** Packed coordinate buffer used internally. */
	private float[] trailBuffer;

	/** Number of stored trajectory points. */
	private int pointCount;

	/**
	 * Published coordinate array visible to the EDT.
	 * This always contains only the active portion of the trail.
	 */
	private volatile float[] publishedCoords = new float[0];

	/** Last published time. */
	private volatile float publishedTime;

	/** Last published x. */
	private volatile float publishedX;

	/** Last published y. */
	private volatile float publishedY;

	/** Last published z. */
	private volatile float publishedZ;

	/**
	 * Construct a model using default equations, step size, and history length.
	 */
	public AizawaModel() {
		this(new AizawaEquations(),
				AizawaEquations.defaultInitialState(),
				DEFAULT_DT,
				DEFAULT_SUBSTEPS_PER_UPDATE,
				DEFAULT_MAX_TRAIL_POINTS,
				DEFAULT_INITIAL_CAPACITY);
	}
	/**
	 * Construct a model with the supplied configuration.
	 *
	 * @param equations the Aizawa equations object; must not be null
	 * @param initialState initial state as {@code [x, y, z]}; must contain at least three values
	 * @param dt RK4 step size; values less than or equal to zero are replaced by {@link #DEFAULT_DT}
	 * @param substepsPerUpdate number of RK4 substeps per update; values less than 1 are promoted to 1
	 * @param maxTrailPoints maximum retained history length in points; values less than or equal to zero mean unbounded
	 * @param initialCapacity initial internal buffer capacity in points; values less than 2 are promoted to 2
	 */
	public AizawaModel(AizawaEquations equations, double[] initialState, double dt, int substepsPerUpdate,
			int maxTrailPoints, int initialCapacity) {

		if (equations == null) {
			throw new IllegalArgumentException("equations must not be null");
		}
		if (initialState == null || initialState.length < 3) {
			throw new IllegalArgumentException("initialState must contain at least three values");
		}

		this.equations = equations;
		this.dt = (dt > 0.0) ? dt : DEFAULT_DT;
		this.substepsPerUpdate = Math.max(1, substepsPerUpdate);
		this.maxTrailPoints = maxTrailPoints;
		this.integrator = new ClassicalRungeKuttaIntegrator(this.dt);
		this.trailBuffer = new float[3 * Math.max(2, initialCapacity)];

		reset(initialState);
	}

	/**
	 * Reset the model to the provided initial state.
	 *
	 * @param initialState initial state as {@code [x, y, z]}
	 */
	public synchronized void reset(double[] initialState) {
		if (initialState == null || initialState.length < 3) {
			throw new IllegalArgumentException("initialState must contain at least three values");
		}

		time = 0.0;
		state[0] = initialState[0];
		state[1] = initialState[1];
		state[2] = initialState[2];
		pointCount = 0;

		appendPoint((float) state[0], (float) state[1], (float) state[2]);
		publishSnapshotData();
	}

	/**
	 * Reset the model to the standard default initial state.
	 */
	public synchronized void reset() {
		reset(AizawaEquations.defaultInitialState());
	}

	/**
	 * Advance the model by one simulation update.
	 * <p>
	 * Each call performs {@link #substepsPerUpdate} RK4 substeps of size {@link #dt}.
	 * After each substep, the new point is appended to the trajectory.
	 * </p>
	 */
	public synchronized void update() {
		for (int i = 0; i < substepsPerUpdate; i++) {
			integrator.integrate(equations, time, state, time + dt, state);
			time += dt;
			appendPoint((float) state[0], (float) state[1], (float) state[2]);
		}
		publishSnapshotData();
	}

	/**
	 * Get the most recently published snapshot.
	 * <p>
	 * This method is side-effect free and intended for use by the EDT.
	 * </p>
	 *
	 * @return the latest snapshot
	 */
	public AizawaSnapshot getSnapshot() {
		return new AizawaSnapshot(publishedCoords, publishedTime, publishedX, publishedY, publishedZ);
	}

	/**
	 * Get the equations object used by this model.
	 *
	 * @return the equations object
	 */
	public AizawaEquations getEquations() {
		return equations;
	}

	/**
	 * Get the RK4 time step.
	 *
	 * @return the RK4 time step
	 */
	public double getDt() {
		return dt;
	}

	/**
	 * Get the number of RK4 substeps taken during each simulation update.
	 *
	 * @return substeps per update
	 */
	public int getSubstepsPerUpdate() {
		return substepsPerUpdate;
	}

	/**
	 * Get the maximum number of retained trail points.
	 *
	 * @return maximum trail point count, or a value less than or equal to zero if unbounded
	 */
	public int getMaxTrailPoints() {
		return maxTrailPoints;
	}

	/**
	 * Get the current number of points in the trajectory.
	 *
	 * @return current point count
	 */
	public synchronized int getPointCount() {
		return pointCount;
	}

	/**
	 * Get the current simulation time.
	 *
	 * @return current time
	 */
	public synchronized double getTime() {
		return time;
	}

	/**
	 * Get a copy of the current state vector.
	 *
	 * @return current state as {@code [x, y, z]}
	 */
	public synchronized double[] getStateCopy() {
		return Arrays.copyOf(state, state.length);
	}

	// Append one point to the internal packed coordinate buffer.
	private void appendPoint(float x, float y, float z) {

		// If capped and full, shift left one point.
		if ((maxTrailPoints > 0) && (pointCount >= maxTrailPoints)) {
			System.arraycopy(trailBuffer, 3, trailBuffer, 0, 3 * (pointCount - 1));
			pointCount--;
		}

		// Ensure capacity for one more point.
		if (3 * (pointCount + 1) > trailBuffer.length) {
			int newCapacityPoints = Math.max(2 * Math.max(1, pointCount), pointCount + 1);
			if (maxTrailPoints > 0) {
				newCapacityPoints = Math.min(newCapacityPoints, maxTrailPoints);
			}

			float[] newBuffer = new float[3 * newCapacityPoints];
			System.arraycopy(trailBuffer, 0, newBuffer, 0, 3 * pointCount);
			trailBuffer = newBuffer;
		}

		int index = 3 * pointCount;
		trailBuffer[index] = x;
		trailBuffer[index + 1] = y;
		trailBuffer[index + 2] = z;
		pointCount++;
	}

	// Publish a stable copy of the active trajectory and state for EDT consumption.
	private void publishSnapshotData() {
		float[] active = new float[3 * pointCount];
		System.arraycopy(trailBuffer, 0, active, 0, active.length);

		publishedCoords = active;
		publishedTime = (float) time;
		publishedX = (float) state[0];
		publishedY = (float) state[1];
		publishedZ = (float) state[2];
	}
}