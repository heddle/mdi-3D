package edu.cnu.mdi.mdi3D.view3D.aizawaDemo;

import java.util.List;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * View information for the Aizawa Attractor Demo.
 */
public class AizawaDemoViewInfo extends AbstractViewInfo {

	@Override
	public String getTitle() {
		return "Aizawa Attractor Demo";
	}

	@Override
	public String getPurpose() {
		return """
				This view integrates the Aizawa system, a chaotic ordinary
				differential equation whose trajectory settles onto a strange
				attractor: a bounded, non-repeating, fractal-structured path
				through three-dimensional space.

				A small marker sphere shows the current point on the trajectory;
				a fading trail behind it traces the path the system has already
				followed. Because the system is chaotic, the shape that emerges
				never exactly repeats, no matter how long the simulation runs.
				""";
	}

	@Override
	public List<String> getUsageBullets() {
		return List.of(
				"Press Start, then Run, to begin integrating the equations.",
				"The diagnostic panel on the right plots phase-space projections of the trajectory as it accumulates.",
				"Rotate the 3D view (drag) to see the attractor's structure from different angles — it is not "
						+ "obviously three-dimensional from every viewpoint.",
				"Pause/Resume/Stop behave exactly as in any other MDI simulation."
		);
	}

	@Override
	public String getTechnicalNotes() {
		return """
				AizawaModel integrates the Aizawa equations on the simulation
				thread and publishes an immutable AizawaSnapshot (position,
				accumulated trail coordinates, elapsed time) after each step.
				AizawaDemoView reads the latest snapshot only inside its
				EDT-marshaled onSimulationRefresh hook, and applies it to both the
				Trajectory3D trail item and the phase-plot diagnostic panel — the
				simulation thread itself never touches the 3D scene.
				""";
	}

	@Override
	public String getFooter() {
		return "MDI-3D chaos demo: a continuously integrated strange attractor.";
	}

	@Override
	protected String getAccentColorHex() {
		return "#8e44ad";
	}
}
