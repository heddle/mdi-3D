package edu.cnu.mdi.mdi3D.view3D.scatterDemo;

import java.util.List;
import java.util.Map;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * View information for the ScatterPlot3D Demo.
 */
public class ScatterPlot3DDemoInfo extends AbstractViewInfo {

	@Override
	public String getTitle() {
		return "ScatterPlot3D Demo";
	}

	@Override
	public String getPurpose() {
		return """
				This view demonstrates ScatterPlot3D, MDI-3D's item for rendering
				large three-dimensional point clouds with a scientific color map,
				floor grid, bounding box, and axis annotations that stay
				synchronized with the data's own bounds.

				It starts with a deterministic, surface-like data set colored with
				the Viridis scientific color map. You can then feed it random
				data from a background producer thread at two very different
				rates, to see how the throttled repaint mechanism holds up under
				load.
				""";
	}

	@Override
	public List<String> getUsageBullets() {
		return List.of(
				"Surface: reload the deterministic starting data set.",
				"Trickle (5/s): add points slowly from a background thread — easy to watch individually.",
				"Storm (5k/s): add points as fast as possible, to exercise the coalesced-repaint throttle.",
				"Stop / Clear: stop the background producer, or stop it and remove all points.",
				"The color-map and render-style menus, and the repaint-throttle slider, take effect immediately.",
				"Render style AUTO uses lit solid spheres for small point counts and switches to point sprites "
						+ "once the count crosses a threshold, for performance."
		);
	}

	@Override
	public Map<String, String> getKeyboardShortcuts() {
		return Map.of(
				"Drag", "Rotate the scene (arcball)",
				"Wheel", "Zoom in/out"
		);
	}

	@Override
	public String getTechnicalNotes() {
		return """
				Unlike the simulation-driven demos in this package, ScatterPlot3D
				is designed for genuinely concurrent producers: the background
				feed here uses a ScheduledExecutorService adding points from a
				worker thread while the EDT keeps rendering. Pending points are
				protected by a ReentrantReadWriteLock; the committed point list
				the renderer actually draws is touched only on the EDT. The
				repaint-throttle slider controls how often a burst of adds is
				allowed to trigger an actual redraw.

				The RenderStyle.SPHERES option renders each point as a lit solid
				sphere via the same Phong lighting path used elsewhere in MDI-3D
				(see Support3D and the MDI Logo Demo). Because that mode is only
				reachable below the auto-point threshold (default 750), the
				per-sphere lighting state changes stay cheap; larger point counts
				fall through to the batched, unlit point-sprite render style
				instead.
				""";
	}

	@Override
	public String getFooter() {
		return "MDI-3D data-visualization demo: large point clouds under a throttled repaint.";
	}

	@Override
	protected String getAccentColorHex() {
		return "#16a085";
	}
}
