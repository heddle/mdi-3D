package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

public class Cube extends Item3D {

	// x coordinate of center of cube
	public final float xc;

	// y coordinate of center of cube
	public final float yc;

	// z coordinate of center of cube
	public final float zc;

	//
	public final float length;

	// frame?
	protected boolean _frame;

	// Phong lighting off by default (preserves prior visuals)
	private boolean _lighted = false;

	/**
	 * Create a cube
	 * @param panel3D
	 * @param xc
	 * @param yc
	 * @param zc
	 * @param length
	 * @param color
	 */
	public Cube(Panel3D panel3D, float xc, float yc, float zc, float length, Color color) {
		this(panel3D, xc, yc, zc, length, color, false);
	}

	public Cube(Panel3D panel3D, float xc, float yc, float zc, float length, Color color, boolean frame) {
		super(panel3D);
		this.xc = xc;
		this.yc = yc;
		this.zc = zc;
		this.length = length;
		_frame = frame;
		setFillColor(color);
	}

	/**
	 * Enable or disable Phong-style lighting for this cube.
	 *
	 * <p>When enabled, each of the six faces is drawn with its own face normal
	 * via {@link Support3D#drawShadedRectangularSolid}, so shading visibly
	 * changes as the cube (or the scene around it) rotates. Disabled by default,
	 * matching the flat-colour appearance existing callers already expect.
	 *
	 * @param lighted {@code true} to enable lighting; {@code false} for flat colour
	 */
	public void setLighted(boolean lighted) {
		_lighted = lighted;
	}

	/**
	 * Whether Phong-style lighting is enabled for this cube.
	 *
	 * @return {@code true} if lighting is enabled
	 */
	public boolean isLighted() {
		return _lighted;
	}

	@Override
	public void draw(GLAutoDrawable drawable) {
		if (_lighted) {
			Support3D.drawShadedRectangularSolid(drawable, xc, yc, zc, length, length, length, getFillColor(), null,
					1, _frame, true);
		} else {
			Support3D.drawRectangularSolid(drawable, xc, yc, zc, length, length, length, getFillColor(), 1, _frame);
		}
	}

	@Override
	public float[] getSortPoint() {
		return new float[] { xc, yc, zc };
	}


}
