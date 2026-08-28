package edu.cnu.mdi.mdi3D.view3D.logoDemo;

import java.util.List;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * View information for the MDI Logo Demo.
 */
public class LogoDemoViewInfo extends AbstractViewInfo {

	@Override
	public String getTitle() {
		return "MDI Logo Demo";
	}

	@Override
	public String getPurpose() {
		return """
				The MDI logo is three overlapping rectangular "window" panes.
				This view recreates that motif directly in 3D as three lit
				Quad3D panels, arranged like stacked cards and rotated together.

				Its purpose is to exercise MDI-3D's optional Phong lighting path
				(also available on Sphere, Cube, and Cylinder). As the group
				turns, each panel's brightness sweeps from lit to grazing to
				shadowed and back — a flat, unlit scene cannot show that, which
				is exactly the point of this demo.
				""";
	}

	@Override
	public List<String> getUsageBullets() {
		return List.of(
				"Press Start, then Run, to begin the rotation (or just watch — auto-run is on by default).",
				"Drag to add your own rotation on top of the automatic spin; it composes naturally.",
				"Watch the panels brighten and dim together as they turn — that is the lighting, not a color change.",
				"Pause/Resume/Stop behave exactly as in any other MDI simulation, even though there is no physics "
						+ "to pause."
		);
	}

	@Override
	public String getTechnicalNotes() {
		return """
				This is the simplest possible SimulationEngine-driven MDI-3D
				view: LogoSimulation has no numerical model at all. Each step
				self-paces with a short sleep (there is no computation to
				naturally rate-limit it) and advances a rotation angle by a fixed
				increment. LogoDemoView applies that increment to the panel with
				a single rotateY call inside its EDT-marshaled onSimulationRefresh
				hook — the same split every other simulation-driven demo in this
				package follows.

				The three panels all share one local surface normal, so they
				shade in perfect sync rather than independently; that is a
				deliberate simplification, not a bug.
				""";
	}

	@Override
	public String getFooter() {
		return "MDI-3D demo: Phong lighting on a recreation of the MDI mark.";
	}

	@Override
	protected String getAccentColorHex() {
		return "#2f74a8";
	}
}
