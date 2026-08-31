package edu.cnu.mdi.mdi3D.adapter3D;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import edu.cnu.mdi.mdi3D.panel.Panel3D;

/**
 * Installs the standard MDI-3D keyboard bindings on a {@link Panel3D} via
 * Swing's {@code InputMap}/{@code ActionMap}, so they work regardless of
 * which child component currently has keyboard focus within the panel's
 * focused window (unlike a plain {@code KeyListener}, which only fires when
 * the panel itself has focus).
 * <p>
 * Every binding — including the shifted variants (larger pan/dolly step,
 * reversed rotation) — routes through the single shared {@link
 * KeyAdapter3D#handleVK} method, the same one {@link KeyboardLabel}'s
 * on-screen legend buttons call. Key handling logic therefore lives in
 * exactly one place; this class only registers key strokes.
 * </p>
 */
public class KeyBindings3D {

	// The fixed set of virtual key codes this class binds, unshifted and shifted.
	private static final int[] VK_CODES = {
			KeyEvent.VK_L, KeyEvent.VK_R, KeyEvent.VK_U, KeyEvent.VK_D,
			KeyEvent.VK_J, KeyEvent.VK_K,
			KeyEvent.VK_X, KeyEvent.VK_Y, KeyEvent.VK_Z,
			KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4
	};

	/**
	 * Install the standard key bindings on the given panel.
	 *
	 * @param panel the panel to receive the bindings
	 */
	public KeyBindings3D(Panel3D panel) {

		InputMap inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = panel.getActionMap();

		for (int vk : VK_CODES) {
			bind(inputMap, actionMap, panel, vk, 0, false);
			bind(inputMap, actionMap, panel, vk, InputEvent.SHIFT_DOWN_MASK, true);
		}

		// F5: force an immediate redraw, independent of the handleVK command set.
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
		actionMap.put("refresh", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				panel.refresh();
			}
		});
	}

	// Register one key stroke (with the given modifiers) and route it through
	// KeyAdapter3D.handleVK via a shared Action keyed by (vkCode, shifted).
	private static void bind(InputMap inputMap, ActionMap actionMap, Panel3D panel, int vk, int modifiers,
			boolean shifted) {
		String actionKey = vk + (shifted ? "-shift" : "");
		inputMap.put(KeyStroke.getKeyStroke(vk, modifiers), actionKey);
		actionMap.put(actionKey, new KeyAction(panel, vk, shifted));
	}

	// Routes one bound key stroke to KeyAdapter3D.handleVK, ignoring key events
	// while a text field has focus so typing "x", "1", etc. into a control
	// doesn't also rotate or reposition the scene.
	@SuppressWarnings("serial")
	private static final class KeyAction extends AbstractAction {

		private final Panel3D panel3D;
		private final int vkCode;
		private final boolean shifted;

		KeyAction(Panel3D panel3D, int vkCode, boolean shifted) {
			this.panel3D = panel3D;
			this.vkCode = vkCode;
			this.shifted = shifted;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if (focusOwner instanceof JTextComponent) {
				return;
			}
			KeyAdapter3D.handleVK(panel3D, vkCode, shifted);
		}
	}
}
