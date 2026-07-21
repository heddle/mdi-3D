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
 *
 * <h2>Allocation strategy</h2>
 * <p>
 * A single backing buffer is owned by this class and never exposed to
 * {@link PolyLine3D}'s drawing path as a trimmed copy. Instead,
 * {@link PolyLine3D#setCoords(float[], int)} is used so the draw loop reads
 * only the live slice of the buffer, eliminating the per-append allocation that
 * the naive {@code Arrays.copyOf} approach would incur.
 * </p>
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
	 * The backing coordinate buffer. Only indices {@code [0, 3 * _pointCount)} are
	 * valid. This buffer is handed directly to {@link PolyLine3D} via
	 * {@link PolyLine3D#setCoords(float[], int)} to avoid copying on every append.
	 */
	private float[] _buf;

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
		super(panel3D, null, color, lineWidth);
		_maxPoints  = maxPoints;
		_pointCount = 0;
		_buf        = new float[3 * Math.max(2, initialCapacity)];
		// Tell the parent about the buffer now so getCoords() is never null.
		setCoords(_buf, 0);
	}

	/**
	 * Appends a new point to the end of the trajectory.
	 * <p>
	 * If the trajectory has a fixed maximum size and is already full, the oldest
	 * point is discarded before the new point is appended. No allocation occurs
	 * unless the backing buffer needs to grow.
	 *
	 * @param x the x coordinate of the new point
	 * @param y the y coordinate of the new point
	 * @param z the z coordinate of the new point
	 */
	public void append(float x, float y, float z) {

		// If capped and full, evict the oldest point by shifting left one slot.
		if (_maxPoints > 0 && _pointCount >= _maxPoints) {
			System.arraycopy(_buf, 3, _buf, 0, 3 * (_pointCount - 1));
			_pointCount--;
		}

		// Grow the backing buffer if needed (amortised O(log n) allocations total).
		if (3 * (_pointCount + 1) > _buf.length) {
			int newCapPoints = Math.max(2 * Math.max(1, _pointCount), _pointCount + 1);
			if (_maxPoints > 0) {
				newCapPoints = Math.min(newCapPoints, _maxPoints);
			}
			float[] newBuf = new float[3 * newCapPoints];
			System.arraycopy(_buf, 0, newBuf, 0, 3 * _pointCount);
			_buf = newBuf;
		}

		// Write the new point.
		int i = 3 * _pointCount;
		_buf[i]     = x;
		_buf[i + 1] = y;
		_buf[i + 2] = z;
		_pointCount++;

		// Publish the live window — no copy, just update the live-point count.
		setCoords(_buf, _pointCount);
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
	 * have been appended. The backing buffer is retained so that a subsequent
	 * fill does not re-allocate.
	 */
	public void clear() {
		_pointCount = 0;
		setCoords(_buf, 0);
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