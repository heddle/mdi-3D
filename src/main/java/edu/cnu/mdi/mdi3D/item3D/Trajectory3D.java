package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;

import edu.cnu.mdi.mdi3D.panel.Panel3D;

/**
 * A dynamic 3D trajectory item that grows by appending points over time.
 * <p>
 * This class extends {@link PolyLine3D} by adding simple trajectory semantics:
 * points can be appended one at a time, the trajectory can be cleared, and an
 * optional maximum point count can be enforced.
 * <p>
 * Internally, the trajectory is stored as a flat coordinate array in the form:
 *
 * <pre>
 * [x1, y1, z1, x2, y2, z2, ..., xn, yn, zn]
 * </pre>
 *
 * Consecutive points are connected by straight line segments when drawn.
 * <p>
 * If a maximum point count is specified and the trajectory exceeds that limit,
 * the oldest point is discarded so that the most recent points remain visible.
 * This implementation uses a simple array shift when discarding old points.
 * That keeps the code straightforward and is usually adequate for demo-scale
 * trajectories.
 */
public class Trajectory3D extends PolyLine3D {

	/** Default initial storage capacity, in points. */
	private static final int DEFAULT_INITIAL_CAPACITY = 256;

	/** The number of currently stored points. */
	private int _pointCount;

	/**
	 * The maximum number of points allowed in the trajectory.
	 * <p>
	 * A value less than or equal to zero means the trajectory is unbounded except
	 * by available memory.
	 */
	private final int _maxPoints;

	/**
	 * Creates an unbounded trajectory with a default initial capacity.
	 *
	 * @param panel3D   the owning 3D panel
	 * @param color     the trajectory color
	 * @param lineWidth the trajectory line width
	 */
	public Trajectory3D(Panel3D panel3D, Color color, float lineWidth) {
		this(panel3D, color, lineWidth, 0, DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * Creates a trajectory with an optional maximum number of points.
	 *
	 * @param panel3D   the owning 3D panel
	 * @param color     the trajectory color
	 * @param lineWidth the trajectory line width
	 * @param maxPoints the maximum number of points to retain; values less than or
	 *                  equal to zero mean no explicit maximum
	 */
	public Trajectory3D(Panel3D panel3D, Color color, float lineWidth, int maxPoints) {
		this(panel3D, color, lineWidth, maxPoints,
				(maxPoints > 0) ? Math.min(Math.max(2, maxPoints), DEFAULT_INITIAL_CAPACITY) : DEFAULT_INITIAL_CAPACITY);
	}

	/**
	 * Creates a trajectory with full control over maximum point count and initial
	 * storage capacity.
	 *
	 * @param panel3D         the owning 3D panel
	 * @param color           the trajectory color
	 * @param lineWidth       the trajectory line width
	 * @param maxPoints       the maximum number of points to retain; values less
	 *                        than or equal to zero mean no explicit maximum
	 * @param initialCapacity the initial storage capacity in points; values less
	 *                        than 2 are promoted to 2
	 */
	public Trajectory3D(Panel3D panel3D, Color color, float lineWidth, int maxPoints, int initialCapacity) {
		super(panel3D, new float[3 * Math.max(2, initialCapacity)], color, lineWidth);
		_maxPoints = maxPoints;
		_pointCount = 0;

		// Start with an empty visible trajectory rather than a zero-filled dummy one.
		setCoords(new float[0]);
	}

	/**
	 * Appends a new point to the end of the trajectory.
	 * <p>
	 * If the trajectory has a fixed maximum size and is already full, the oldest
	 * point is discarded before the new point is appended.
	 *
	 * @param x the x coordinate of the new point
	 * @param y the y coordinate of the new point
	 * @param z the z coordinate of the new point
	 */
	public void append(float x, float y, float z) {
		float[] coords = getCoords();

		// If we are using an empty display array after construction or clear, allocate
		// a real backing array now.
		if (coords == null || coords.length == 0) {
			coords = new float[3 * DEFAULT_INITIAL_CAPACITY];
		}

		// If capped and full, discard the oldest point by shifting left one point.
		if ((_maxPoints > 0) && (_pointCount >= _maxPoints)) {
			System.arraycopy(coords, 3, coords, 0, 3 * (_pointCount - 1));
			_pointCount--;
		}

		// Ensure capacity for one more point.
		if (3 * (_pointCount + 1) > coords.length) {
			int newCapacityPoints = Math.max(2 * Math.max(1, _pointCount), _pointCount + 1);

			if (_maxPoints > 0) {
				newCapacityPoints = Math.min(newCapacityPoints, _maxPoints);
			}

			float[] newCoords = new float[3 * newCapacityPoints];
			System.arraycopy(coords, 0, newCoords, 0, 3 * _pointCount);
			coords = newCoords;
		}

		int index = 3 * _pointCount;
		coords[index] = x;
		coords[index + 1] = y;
		coords[index + 2] = z;
		_pointCount++;

		// Publish only the active portion of the coordinate buffer for drawing.
		float[] active = new float[3 * _pointCount];
		System.arraycopy(coords, 0, active, 0, active.length);
		setCoords(active);
	}

	/**
	 * Appends a new point to the end of the trajectory.
	 *
	 * @param xyz the point coordinates as {@code [x, y, z]}
	 * @throws IllegalArgumentException if the array is {@code null} or has length
	 *                                  less than 3
	 */
	public void append(float[] xyz) {
		if (xyz == null || xyz.length < 3) {
			throw new IllegalArgumentException("Point array must contain at least three values.");
		}
		append(xyz[0], xyz[1], xyz[2]);
	}

	/**
	 * Removes all points from the trajectory.
	 * <p>
	 * After calling this method, nothing will be drawn until at least two points
	 * have been appended.
	 */
	public void clear() {
		_pointCount = 0;
		setCoords(new float[0]);
	}

	/**
	 * Gets the current number of points in the trajectory.
	 *
	 * @return the number of stored points
	 */
	public int getPointCount() {
		return _pointCount;
	}

	/**
	 * Gets the maximum allowed number of points.
	 *
	 * @return the maximum number of points, or a value less than or equal to zero
	 *         if the trajectory is unbounded
	 */
	public int getMaxPoints() {
		return _maxPoints;
	}

	/**
	 * Checks whether the trajectory is empty.
	 *
	 * @return {@code true} if no points are stored, {@code false} otherwise
	 */
	public boolean isEmpty() {
		return _pointCount == 0;
	}
}