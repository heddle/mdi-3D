package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

import java.util.List;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * View information for the Kinetics Demo.
 */
public class KineticsDemoViewInfo extends AbstractViewInfo {

	@Override
	public String getTitle() {
		return "Kinetics Demo";
	}

	@Override
	public String getPurpose() {
		return """
				This view simulates a kinetic theory of gases: thousands of hard
				spheres, confined to a cube, colliding elastically with each other
				and with the cube's walls. It is a physics simulation, not a
				decorative animation — the particle positions come from a real
				elastic-collision model advanced on a dedicated simulation thread.

				The default configuration runs 50,000 particles at a modest volume
				fraction and a low initial temperature, so you can watch the system
				explore its available phase space over time.
				""";
	}

	@Override
	public List<String> getUsageBullets() {
		return List.of(
				"Press Start, then Run, to begin the simulation; Pause/Resume/Stop control it from there.",
				"Reset (bottom right) discards the current run and starts a fresh model with the same parameters.",
				"The diagnostic panel on the right plots an entropy-like quantity against time — watch it climb "
						+ "toward equilibrium as the run progresses.",
				"Drag to rotate, scroll to zoom, and use the standard keyboard shortcuts (see the panel's own "
						+ "Info) to navigate the cube."
		);
	}

	@Override
	public String getTechnicalNotes() {
		return """
				KineticsModel owns the physics: positions and velocities for every
				particle, advanced with elastic wall and pairwise collisions. It
				publishes results through a double-buffered snapshot (front/back
				coordinate arrays), so the simulation thread can keep writing the
				next frame while the EDT safely reads the last completed one.

				As with every simulation-driven MDI-3D view, the simulation thread
				never touches the 3D scene directly. KineticsDemoView reads the
				latest snapshot and updates the PointSet3D item only inside its
				EDT-marshaled onSimulationRefresh hook.
				""";
	}

	@Override
	public String getFooter() {
		return "MDI-3D physics demo: kinetic theory of gases.";
	}

	@Override
	protected String getAccentColorHex() {
		return "#d35400";
	}
}
