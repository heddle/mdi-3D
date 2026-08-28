package edu.cnu.mdi.mdi3D.view3D.logoDemo;

import java.awt.Color;
import java.awt.Font;
import java.util.Properties;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.item3D.Quad3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.TextRendering3D;
import edu.cnu.mdi.mdi3D.view3D.SimulationView3D;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;
import edu.cnu.mdi.sim.ui.StandardSimIcons;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.ViewConfiguration;
import edu.cnu.mdi.view.ViewPropertiesBuilder;
import edu.cnu.mdi.view.VirtualView;

/**
 * 3D MDI demo view: a lit, continuously rotating recreation of the MDI logo.
 * <p>
 * The logo itself is three overlapping rectangular "window" panes — this
 * view recreates that motif directly in 3D as three lit {@link Quad3D}
 * panels, arranged like stacked cards and rotated as a rigid group. As the
 * group turns, each panel's diffuse and specular shading sweeps visibly,
 * which is exactly what a flat, unlit scene cannot show — this view exists
 * primarily to exercise the Phong lighting path added to {@code Quad3D} (and,
 * via the same mechanism, {@code Sphere}, {@code Cube}, and {@code Cylinder}).
 * </p>
 * <p>
 * Architecturally this is the simplest possible {@link SimulationView3D}: the
 * simulation thread has no numerical model to integrate, so {@link
 * LogoSimulation#step} does nothing but advance a rotation angle at a
 * self-paced rate. All scene mutation — the single {@code rotateY} call per
 * frame — happens in {@link #onSimulationRefresh}, on the EDT, exactly like
 * the Aizawa and Kinetics demos.
 * </p>
 */
@SuppressWarnings("serial")
public class LogoDemoView extends SimulationView3D {

	/** Default frame title for the demo. */
	private static final String TITLE = "MDI Logo Demo";

	/** Initial camera distance x. */
	private static final float XDIST = 0f;

	/** Initial camera distance y. */
	private static final float YDIST = 0f;

	/** Initial camera distance z. */
	private static final float ZDIST = -650f;

	/** Initial x rotation. */
	private static final float THETAX = 18f;

	/** Initial y rotation. */
	private static final float THETAY = -25f;

	/** Initial z rotation. */
	private static final float THETAZ = 0f;

	/** Half-extent used to size mouse-wheel/keyboard navigation steps. */
	private static final float SCENE_EXTENT = 350f;

	// Panel colors, chosen to evoke the actual MDI logo's stacked blue "windows"
	// with a light, screen-like front pane.
	private static final Color PANEL_BACK  = new Color(70, 122, 168);
	private static final Color PANEL_MID   = new Color(34, 88, 140);
	private static final Color PANEL_FRONT = new Color(205, 214, 224);

	/** Style used to draw the fixed "MDI" screen-space caption. */
	private static final TextRendering3D.Style LABEL_STYLE =
			new TextRendering3D.Style(new Color(20, 45, 80), new Color(255, 255, 255, 220), 2, true, false);

	/**
	 * Construct the logo demo view using its canonical default properties.
	 */
	public LogoDemoView() {
		this(createDefaultProperties());
	}

	/**
	 * Construct the logo demo view using the supplied properties.
	 *
	 * @param props the properties used to configure the view
	 */
	public LogoDemoView(Properties props) {
		super(createSimulation(),
				new SimulationEngineConfig(33, 1000, 0, true),
				true,
				() -> new IconSimulationControlPanel(new StandardSimIcons(), true),
				props);

		getSimulation().setEngine(getSimulationEngine());
	}

	/**
	 * Legacy compatibility constructor using alternating key/value pairs.
	 *
	 * @param keyVals alternating property key/value pairs
	 */
	public LogoDemoView(Object... keyVals) {
		this(PropertyUtils.fromKeyValues(keyVals));
	}

	// Create the default properties for this view.
	private static Properties createDefaultProperties() {
		return new ViewPropertiesBuilder()
				.title(TITLE)
				.put(PropertyUtils.ANGLE_X, THETAX)
				.put(PropertyUtils.ANGLE_Y, THETAY)
				.put(PropertyUtils.ANGLE_Z, THETAZ)
				.put(PropertyUtils.DIST_X, XDIST)
				.put(PropertyUtils.DIST_Y, YDIST)
				.put(PropertyUtils.DIST_Z, ZDIST)
				.fraction(0.6)
				.aspect(1.2)
				.useContainer(false)
				.build();
	}

	/**
	 * Get the lazy-creation configuration for this view.
	 *
	 * @return the view configuration for lazy creation
	 */
	public static ViewConfiguration<LogoDemoView> getConfiguration() {
		return ViewConfiguration.lazy(TITLE, LogoDemoView::new, 1, 0, 0, VirtualView.CENTER);
	}

	@Override
	public AbstractViewInfo getViewInfo() {
		return new LogoDemoViewInfo();
	}

	// Create the simulation with default parameters.
	private static LogoSimulation createSimulation() {
		return new LogoSimulation();
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		return new Panel3D(angleX, angleY, angleZ, xDist, yDist, zDist) {

			// Lazily created on the GL thread; released in dispose(), matching the
			// Item3D lazy-resource pattern (see Axis3D, LabelSet3D, ScatterPlot3D).
			private TextRendering3D mdiLabel;

			@Override
			public void createInitialItems() {
				setNavigationStepFromExtent(SCENE_EXTENT);

				addItem(panel(this, -70f, 70f, -70f, 85f, PANEL_BACK));
				addItem(panel(this, -15f, 15f, -25f, 95f, PANEL_MID));
				addItem(panel(this, 45f, -45f, 25f, 110f, PANEL_FRONT));
			}

			@Override
			public void afterDraw(GLAutoDrawable drawable) {
				if (mdiLabel == null) {
					mdiLabel = new TextRendering3D(new Font("SansSerif", Font.BOLD, 28));
				}
				float w = drawable.getSurfaceWidth();

				// Fixed screen-space caption -- always legible regardless of the
				// current rotation, the way the real logo's "MDI" wordmark sits
				// below the window-stack mark rather than rotating with it.
				mdiLabel.begin(drawable);
				mdiLabel.drawProjected("MDI", w / 2f, 28f, 0f, 0, 0,
						TextRendering3D.Anchor.CENTER_CENTER, LABEL_STYLE);
				mdiLabel.end();
			}

			@Override
			public void dispose(GLAutoDrawable drawable) {
				super.dispose(drawable);
				if (mdiLabel != null) {
					mdiLabel.dispose();
					mdiLabel = null;
				}
			}
		};
	}

	// Build one lit, framed, square Quad3D panel centered at (cx, cy, cz).
	private static Quad3D panel(Panel3D p, float cx, float cy, float cz, float half, Color color) {
		Quad3D q = new Quad3D(p,
				cx - half, cy - half, cz,
				cx + half, cy - half, cz,
				cx + half, cy + half, cz,
				cx - half, cy + half, cz,
				color, 2.5f, true);
		q.setLighted(true);
		return q;
	}

	/**
	 * Compatibility factory method to create the demo view with its standard
	 * title and layout.
	 *
	 * @return a new default-configured logo demo view
	 */
	public static LogoDemoView createLogoView() {
		return new LogoDemoView();
	}

	// ---------------------------------------------------------------------
	// Simulation lifecycle hooks (all run on EDT)
	// ---------------------------------------------------------------------

	@Override
	protected void onSimulationRefresh(SimulationContext ctx) {
		float delta = getSimulation().takePendingDelta();
		if (delta != 0f) {
			_panel3D.rotateY(delta);
		}
	}

	// ---------------------------------------------------------------------
	// Typed accessors
	// ---------------------------------------------------------------------

	/**
	 * Get the simulation instance with the correct type.
	 *
	 * @return the logo simulation
	 */
	public LogoSimulation getSimulation() {
		return (LogoSimulation) getSimulationEngine().getSimulation();
	}
}
