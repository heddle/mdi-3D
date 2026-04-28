package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * A 3D polyline item represented by an ordered list of vertices.
 * <p>
 * The coordinates are stored in the form:
 *
 * <pre>
 * [x1, y1, z1, x2, y2, z2, ..., xn, yn, zn]
 * </pre>
 *
 * Consecutive points are connected by straight line segments, producing a
 * single open polyline. This item is useful for drawing trajectories, orbit
 * paths, field lines, and other connected 3D curves.
 * <p>
 * At least two points are required for visible output.
 */
public class PolyLine3D extends Item3D {

	/** The polyline coordinate buffer. Only indices {@code [0, 3*_livePoints)} are valid. */
	private float[] _coords;

	/**
	 * Number of live points in {@code _coords}.
	 * <p>
	 * When set via {@link #setCoords(float[])}, this equals {@code coords.length / 3}.
	 * When set via {@link #setCoords(float[], int)}, the buffer may be larger and
	 * only the first {@code livePoints} entries are drawn.
	 */
	private int _livePoints;

	/**
	 * Creates a 3D polyline from an array of coordinates.
	 *
	 * @param panel3D   the owning 3D panel
	 * @param coords    the polyline coordinates as
	 *                  {@code [x1, y1, z1, ..., xn, yn, zn]}
	 * @param color     the line color
	 * @param lineWidth the line width
	 */
	public PolyLine3D(Panel3D panel3D, float[] coords, Color color, float lineWidth) {
		super(panel3D);
		setCoords(coords);
		setLineColor(color);
		setLineWidth(lineWidth);
	}

	/**
	 * Draws the polyline.
	 * <p>
	 * If there are fewer than two live points, nothing is drawn.
	 *
	 * @param drawable the OpenGL drawable
	 */
	@Override
	public void draw(GLAutoDrawable drawable) {
		if (_coords == null || _livePoints < 2) {
			return;
		}
		Support3D.drawPolyLine(drawable, _coords, _livePoints, getLineColor(), getLineWidth());
	}

	/**
	 * Replaces the coordinates of this polyline.
	 * <p>
	 * The entire array is treated as live data; {@code _livePoints} is set to
	 * {@code coords.length / 3}. Pass {@code null} or an empty array to produce
	 * no output.
	 *
	 * @param coords the new coordinates as {@code [x1, y1, z1, ..., xn, yn, zn]},
	 *               or {@code null} to clear
	 */
	public void setCoords(float[] coords) {
		_coords = coords;
		_livePoints = (coords != null) ? coords.length / 3 : 0;
	}

	/**
	 * Points this polyline at a (possibly oversized) buffer, treating only the
	 * first {@code livePoints} entries as valid data.
	 * <p>
	 * No copy is made. The caller must not modify indices at or beyond
	 * {@code 3 * livePoints} while a draw is in progress. {@link Trajectory3D}
	 * satisfies this by writing new data only after advancing {@code _pointCount}.
	 *
	 * @param buf        the coordinate buffer; must have length &ge; {@code 3 * livePoints}
	 * @param livePoints number of valid points in {@code buf}; must be &ge; 0
	 * @throws IllegalArgumentException if {@code livePoints} is negative or the
	 *                                  buffer is too small
	 */
	public void setCoords(float[] buf, int livePoints) {
		if (livePoints < 0) {
			throw new IllegalArgumentException("livePoints must be >= 0, got: " + livePoints);
		}
		if (buf == null && livePoints > 0) {
			throw new IllegalArgumentException("buf is null but livePoints = " + livePoints);
		}
		if (buf != null && buf.length < 3 * livePoints) {
			throw new IllegalArgumentException(
					"buf.length (" + buf.length + ") < 3 * livePoints (" + (3 * livePoints) + ")");
		}
		_coords     = buf;
		_livePoints = livePoints;
	}

	/**
	 * Gets the current coordinate buffer.
	 * <p>
	 * The buffer may be larger than the live data; use {@link #getLivePoints()} to
	 * determine how many points are valid.
	 *
	 * @return the coordinate buffer, or {@code null} if none has been set
	 */
	public float[] getCoords() {
		return _coords;
	}

	/**
	 * Returns the number of live points currently held by this polyline.
	 *
	 * @return number of valid points in the coordinate buffer
	 */
	public int getLivePoints() {
		return _livePoints;
	}

	/**
	 * Gets the centroid of the live polyline vertices.
	 * <p>
	 * This is used as a representative point for sorting.
	 *
	 * @return the centroid of the live vertices, or the origin if there are none
	 */
	public float[] getCentroid() {
		if (_coords == null || _livePoints == 0) {
			return new float[] { 0f, 0f, 0f };
		}

		float cx = 0f;
		float cy = 0f;
		float cz = 0f;

		for (int i = 0; i < _livePoints; i++) {
			cx += _coords[3 * i];
			cy += _coords[3 * i + 1];
			cz += _coords[3 * i + 2];
		}

		return new float[] { cx / _livePoints, cy / _livePoints, cz / _livePoints };
	}

	/**
	 * Gets a representative point used for item sorting.
	 *
	 * @return the centroid of the live polyline vertices
	 */
	@Override
	public float[] getSortPoint() {
		return getCentroid();
	}

}