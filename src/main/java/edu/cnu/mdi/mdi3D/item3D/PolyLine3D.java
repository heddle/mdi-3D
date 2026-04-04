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

	/** The polyline coordinates as [x1, y1, z1, ..., xn, yn, zn]. */
	private float[] _coords;

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
		_coords = coords;
		setLineColor(color);
		setLineWidth(lineWidth);
	}

	/**
	 * Draws the polyline.
	 * <p>
	 * If there are fewer than two points, nothing is drawn.
	 *
	 * @param drawable the OpenGL drawable
	 */
	@Override
	public void draw(GLAutoDrawable drawable) {
		if ((_coords == null) || (_coords.length < 6)) {
			return;
		}
		Support3D.drawPolyLine(drawable, _coords, getLineColor(), getLineWidth());
	}

	/**
	 * Replaces the coordinates of this polyline.
	 * <p>
	 * This is useful for animated trajectories whose vertex list changes over time.
	 *
	 * @param coords the new coordinates as {@code [x1, y1, z1, ..., xn, yn, zn]}
	 */
	public void setCoords(float[] coords) {
		_coords = coords;
	}

	/**
	 * Gets the current polyline coordinates.
	 *
	 * @return the coordinates as {@code [x1, y1, z1, ..., xn, yn, zn]}
	 */
	public float[] getCoords() {
		return _coords;
	}

	/**
	 * Gets the centroid of the polyline vertices.
	 * <p>
	 * This is used as a representative point for sorting.
	 *
	 * @return the centroid of the vertices, or the origin if there are no points
	 */
	public float[] getCentroid() {
		if ((_coords == null) || (_coords.length < 3)) {
			return new float[] { 0f, 0f, 0f };
		}

		final int n = _coords.length / 3;
		float cx = 0f;
		float cy = 0f;
		float cz = 0f;

		for (int i = 0; i < n; i++) {
			cx += _coords[3 * i];
			cy += _coords[3 * i + 1];
			cz += _coords[3 * i + 2];
		}

		return new float[] { cx / n, cy / n, cz / n };
	}

	/**
	 * Gets a representative point used for item sorting.
	 *
	 * @return the centroid of the polyline vertices
	 */
	@Override
	public float[] getSortPoint() {
		return getCentroid();
	}

}