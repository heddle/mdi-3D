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
				Quad3D panels arranged like stacked cards and rotated together.

				Its purpose is to demonstrate MDI-3D's optional lighting path,
				which is also available for Sphere, Cube, and Cylinder. The
				panes have slightly different surface normals, so their
				brightness and highlights change independently as the mark
				rotates. That makes the effect of surface orientation on
				lighting especially easy to see.
				""";
	}

	@Override
	public List<String> getUsageBullets() {
		return List.of(
				"Use the simulation controls to run, pause, resume, or stop the automatic rotation.",
				"Drag with the mouse to add your own rotation on top of the automatic spin.",
				"Watch the three panes brighten and dim somewhat differently as their surface normals turn relative to the light.",
				"Use Image > Save/Copy Image... to save the rendered view or copy it directly to the clipboard."
		);
	}

	@Override
	public String getTechnicalNotes() {
		return """
				This is the simplest SimulationEngine-driven MDI-3D view:
				LogoSimulation has no numerical model to integrate. Each step
				self-paces with a short sleep and accumulates a small rotation
				increment. LogoDemoView applies that increment with a rotateY
				call inside its EDT-marshaled onSimulationRefresh hook.

				Lighting is opt-in on the Quad3D items. Each pane is given a
				small fixed local tilt, producing a slightly different surface
				normal while preserving the overall stacked-window appearance.
				The light remains fixed while the scene rotates, so the changing
				shading comes from the orientation of each surface rather than
				from changing its base color.
				""";
	}

	@Override
	public String getFooter() {
		return "MDI-3D demo: lighting on a recreation of the MDI mark.";
	}

	@Override
	protected String getAccentColorHex() {
		return "#2f74a8";
	}
}
