package edu.cnu.mdi.mdi3D.basic;

import java.awt.Color;

import com.jogamp.opengl.GL2;

/**
 * Static GL2 color-state helper used by the {@code basic} package's minimal,
 * standalone JOGL example classes ({@link BasicPanel3D}, {@link
 * BasicLineDrawing}) — a bare-JOGL counterpart kept separate from the
 * {@link edu.cnu.mdi.mdi3D.panel.Panel3D}/{@link edu.cnu.mdi.mdi3D.item3D.Item3D}
 * framework used by the rest of {@code mdi3D}.
 */
public class BasicColorSupport3D {

	/**
	 * Set the color
	 *
	 * @param gl2   the context
	 * @param color the color
	 */
	public static void setColor(GL2 gl2, Color color) {
		float rf = color.getRed() / 255f;
		float gf = color.getGreen() / 255f;
		float bf = color.getBlue() / 255f;
		float af = color.getAlpha() / 255f;
		gl2.glColor4f(rf, gf, bf, af);
	}
}
