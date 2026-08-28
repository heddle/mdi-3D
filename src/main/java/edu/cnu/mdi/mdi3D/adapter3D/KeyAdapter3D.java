package edu.cnu.mdi.mdi3D.adapter3D;

import java.awt.event.KeyEvent;

import edu.cnu.mdi.mdi3D.panel.Panel3D;

/**
 * Shared static logic for MDI-3D's fixed keyboard/legend commands: pan
 * (L/R/U/D), dolly (J/K), incremental rotation (X/Y/Z, tripled when shifted),
 * and four canonical axis-aligned views (1-4).
 * <p>
 * This class holds only {@link #handleVK}. Both real keyboard input (via
 * {@link KeyBindings3D}, which registers Swing {@code InputMap}/
 * {@code ActionMap} bindings) and the on-screen {@link KeyboardLabel} legend
 * — whose buttons let a user click a key's effect instead of pressing it —
 * route through this one method, so a keystroke and its corresponding legend
 * button can never disagree about what they do.
 * </p>
 */
public final class KeyAdapter3D {

	// steps in rotation angle
	private static final float DTHETA = 2f; // degrees

	private KeyAdapter3D() {
		// static utility; not instantiable
	}

	/**
	 * Respond to a key stroke, or a {@link KeyboardLabel} button mimicking one.
	 *
	 * @param panel3D the owner panel
	 * @param keyCode the key code
	 * @param shifted whether it was shifted (e.g., capitalized)
	 */
	public static void handleVK(Panel3D panel3D, int keyCode, boolean shifted) {

		int factor = (shifted ? 3 : 1);
		float step = panel3D.getZStep();
		float ang = factor * DTHETA;

		if (keyCode == KeyEvent.VK_L) {
			panel3D.deltaX(-step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_R) {
			panel3D.deltaX(step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_U) {
			panel3D.deltaY(step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_D) {
			panel3D.deltaY(-step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_J) {
			panel3D.deltaZ(step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_K) {
			panel3D.deltaZ(-step);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_X) {
			panel3D.rotateX(ang);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_Y) {
			panel3D.rotateY(ang);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_Z) {
			panel3D.rotateZ(ang);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_1) { // x out
			panel3D.loadIdentityMatrix();
			panel3D.rotateX(180f);
			panel3D.rotateY(90f);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_2) { // y out3
			panel3D.loadIdentityMatrix();
			panel3D.rotateZ(-90f);
			panel3D.rotateY(-90f);
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_3) { // z out
			panel3D.loadIdentityMatrix();
			panel3D.refresh();
		} else if (keyCode == KeyEvent.VK_4) { // z in
			panel3D.loadIdentityMatrix();
			panel3D.rotateY(180f);
			panel3D.refresh();
		}

	}

}
