package edu.cnu.mdi.mdi3D.physics;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.lang.reflect.Constructor;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;

import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.ui.ISimulationControlPanel;
import edu.cnu.mdi.sim.ui.StandardSimIcons;
import edu.cnu.mdi.ui.fonts.Fonts;

/**
 * A more graphical variant of {@link SimulationControlPanel} that prefers icons and
 * supports either an indeterminate "busy" graphic (when available) or an indeterminate
 * progress bar fallback.
 *
 * <p>MDI-agnostic: icons are supplied by an {@link IconProvider}.</p>
 */
@SuppressWarnings("serial")
public class PhysicsSimControlPanel extends JPanel  {

	public interface IconProvider {
		Icon start();
		Icon run();
		Icon pause();
		Icon resume();
		Icon stop();
		Icon cancel();
	}

	// the host is typically a subclass of SimulationView
	private IPhysicsSimHost host;


	private final JButton runBtn;
	private final JButton pauseBtn;
	private final JButton resumeBtn;
	private final JButton stopBtn;
	private final JButton cancelBtn;

	public PhysicsSimControlPanel(IPhysicsSimHost host) {
		Objects.requireNonNull(host, "host");
		
		this.host = host;
		StandardSimIcons icons = new StandardSimIcons();

		setLayout(new BorderLayout(6, 6));


		// Bottom: toolbar with icons
		JToolBar tb = new JToolBar();
		tb.setFloatable(false);

		runBtn    = toolButton(icons.run(),    "Run");
		pauseBtn = toolButton(icons.pause(), "Pause");
		resumeBtn = toolButton(icons.resume(), "Resume");
		stopBtn = toolButton(icons.stop(), "Stop");
		cancelBtn = toolButton(icons.cancel(), "Cancel");

		tb.add(runBtn);
		tb.addSeparator();
		tb.add(pauseBtn);
		tb.add(resumeBtn);
		tb.addSeparator();
		tb.add(stopBtn);
		tb.add(cancelBtn);

		// Optional: put spinner to the right of the toolbar if available
		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(tb, BorderLayout.WEST);

		add(bottom, BorderLayout.SOUTH);

		runBtn.addActionListener(e -> { if (host != null) {
			host.runSimulation();
		} });
		pauseBtn.addActionListener(e -> { if (host != null) {
			host.pauseSimulation();
		} });
		resumeBtn.addActionListener(e -> { if (host != null) {
			host.resumeSimulation();
		} });
		stopBtn.addActionListener(e -> { if (host != null) {
			host.stopSimulation();
		} });
		cancelBtn.addActionListener(e -> { if (host != null) {
			host.cancelSimulation();
		} });

		applyState(SimulationState.NEW, "unbound");

		Dimension size = getPreferredSize();
		size.width = 300;
		setPreferredSize(size);
	}

	// ------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------

	private JButton toolButton(Icon icon, String tooltip) {
		JButton b = new JButton(icon);
		b.setToolTipText(tooltip);
		b.setFocusable(false);
		return b;
	}

	private void applyState(SimulationState state, String reason) {
//
//		runBtn.setEnabled(bound && (state == SimulationState.READY || state == SimulationState.PAUSED));
//		pauseBtn.setEnabled(bound && state == SimulationState.RUNNING);
//		resumeBtn.setEnabled(bound && state == SimulationState.PAUSED);
//
//		boolean canStopOrCancel = bound && (state == SimulationState.INITIALIZING || state == SimulationState.READY
//				|| state == SimulationState.RUNNING || state == SimulationState.PAUSED
//				|| state == SimulationState.SWITCHING);
//
//		stopBtn.setEnabled(canStopOrCancel);
//		cancelBtn.setEnabled(canStopOrCancel);
	}


	/**
	 * If FlatLaf Extras is on the classpath, use FlatBusyLabel as a spinner/busy indicator.
	 * Otherwise return null and we fall back to JProgressBar indeterminate.
	 */
	private static JComponent tryCreateFlatBusyLabel() {
		try {
			Class<?> c = Class.forName("com.formdev.flatlaf.extras.components.FlatBusyLabel");
			Constructor<?> ctor = c.getConstructor();
			Object inst = ctor.newInstance();
			if (inst instanceof JComponent jc) {
				jc.setName("busyIndicator");
				jc.setAlignmentY(Component.CENTER_ALIGNMENT);
				return jc;
			}
		} catch (Throwable ignored) {
			// FlatLaf extras not present (or any other failure) => no spinner
		}
		return null;
	}
	

}