package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import edu.cnu.mdi.sim.ISimulationHost;
import edu.cnu.mdi.sim.ProgressInfo;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationListener;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.ui.ISimulationControlPanel;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;
import edu.cnu.mdi.sim.ui.StandardSimIcons;

/**
 * Control panel for the kinetics demo.
 * <p>
 * This wraps the standard icon-based simulation controls and adds a Reset button
 * that requests an engine reset from the hosting {@link KineticsDemoView}.
 * </p>
 */
@SuppressWarnings("serial")
public class ControlPanel extends JPanel implements ISimulationControlPanel, SimulationListener {

	// Base panel with buttons + progress
	private final IconSimulationControlPanel basePanel;

	// Reset button resets the demo
	private final JButton resetButton;

	// Host (typically a SimulationView3D subclass)
	private ISimulationHost host;

	/**
	 * Construct a new kinetics demo control panel with standard icons.
	 */
	public ControlPanel() {
		this(new StandardSimIcons());
	}

	/**
	 * Construct a new kinetics demo control panel using the provided icon set.
	 *
	 * @param icons simulation icons (non-null)
	 */
	public ControlPanel(StandardSimIcons icons) {
		super(new BorderLayout(8, 0));
		Objects.requireNonNull(icons, "icons");

		// Base panel (left) has the standard media icons
		basePanel = new IconSimulationControlPanel(icons, true);
		add(basePanel, BorderLayout.WEST);

		// Reset button (right)
		resetButton = new JButton("Reset");
		resetButton.setEnabled(false);
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
		btnPanel.add(resetButton);
		btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		resetButton.addActionListener(e -> requestResetFromHost());
		add(btnPanel, BorderLayout.EAST);

		setBorder(BorderFactory.createEtchedBorder());
	}

	@Override
	public void bind(ISimulationHost host) {
		this.host = Objects.requireNonNull(host, "host");

		// Bind the base panel (buttons + progress)
		basePanel.bind(host);

		// Listen for state changes so we can enable/disable Reset
		host.getSimulationEngine().addListener(this);

		// Apply current state immediately
		applyState(host.getSimulationState());
	}

	@Override
	public void unbind() {
		if (host != null) {
			try {
				host.getSimulationEngine().removeListener(this);
			} catch (Throwable ignored) {
				// Defensive: engine may already be stopping.
			}
		}
		basePanel.unbind();
		host = null;
	}

	@Override
	public void onStateChange(SimulationContext ctx, SimulationState from, SimulationState to, String reason) {
		applyState(to);
	}

	@Override
	public void onReady(SimulationContext ctx) {
		applyState(SimulationState.READY);
	}

	@Override
	public void onDone(SimulationContext ctx) {
		applyState(SimulationState.TERMINATED);
	}

	@Override
	public void onFail(SimulationContext ctx, Throwable error) {
		applyState(SimulationState.FAILED);
	}

	@Override
	public void onProgress(SimulationContext ctx, ProgressInfo progress) {
		// no-op (base panel handles progress UI)
	}

	// Based on simulation state, enable/disable Reset
	private void applyState(SimulationState state) {
		boolean editable = (state == SimulationState.READY || state == SimulationState.PAUSED
				|| state == SimulationState.TERMINATED || state == SimulationState.FAILED);
		resetButton.setEnabled(editable);
	}

	private void requestResetFromHost() {
		if (host instanceof KineticsDemoView kd) {
			kd.requestReset();
		}
	}
}
