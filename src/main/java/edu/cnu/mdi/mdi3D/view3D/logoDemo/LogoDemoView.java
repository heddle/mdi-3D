package edu.cnu.mdi.mdi3D.view3D.logoDemo;

import java.awt.Color;
import java.awt.Font;
import java.util.Properties;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.item3D.Quad3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.TextRendering3D;
import edu.cnu.mdi.mdi3D.panel.Vector3f;
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
 * LogoSimulation#step} does nothing but advance an animation angle at a
 * self-paced rate. All scene mutation happens in {@link #onSimulationRefresh},
 * on the EDT, exactly like the Aizawa and Kinetics demos. The view uses that
 * angle to rotate about a slowly precessing axis, giving the lit pane normals
 * a much richer set of orientations than a simple turntable spin.
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

	/** X amplitude of the slowly precessing rotation axis. */
	private static final float AXIS_X_AMPLITUDE = 0.45f;

	/** Z amplitude of the slowly precessing rotation axis. */
	private static final float AXIS_Z_AMPLITUDE = 0.32f;

	/** Relative phase rate for the x component of the rotation axis. */
	private static final float AXIS_X_PHASE_RATE = 0.61f;

	/** Relative phase rate for the z component of the rotation axis. */
	private static final float AXIS_Z_PHASE_RATE = 0.37f;

	/** Animation phase, in degrees, used to precess the automatic rotation axis. */
	private float rotationPhaseDeg;

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

				/*
				 * Give the three panes slightly different surface normals. The offsets
				 * are deliberately small: the group should still read immediately as
				 * the MDI stacked-window mark, while the different normals make the
				 * lighting response visibly independent on each pane.
				 */
				addItem(panel(this, -70f,  70f, -70f,  85f, -5f,  4f, PANEL_BACK));
				addItem(panel(this, -15f,  15f, -25f,  95f,  4f, -3f, PANEL_MID));
				addItem(panel(this,  45f, -45f,  25f, 110f, -3f,  5f, PANEL_FRONT));
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

	/**
	 * Build one lit, framed square panel centered at {@code (cx, cy, cz)}.
	 *
	 * <p>
	 * The square is constructed in its local xy plane and then given small
	 * rotations about its local x and y axes. This lets different panes have
	 * slightly different surface normals while preserving the stacked-window
	 * appearance of the MDI mark.
	 * </p>
	 *
	 * @param p owner panel
	 * @param cx center x coordinate
	 * @param cy center y coordinate
	 * @param cz center z coordinate
	 * @param half half-width and half-height of the square
	 * @param tiltXDeg local x-axis tilt, in degrees
	 * @param tiltYDeg local y-axis tilt, in degrees
	 * @param color panel color
	 * @return the configured lit quad
	 */
	private static Quad3D panel(Panel3D p,
			float cx, float cy, float cz,
			float half,
			float tiltXDeg, float tiltYDeg,
			Color color) {

		float[] coords = {
				-half, -half, 0f,
				 half, -half, 0f,
				 half,  half, 0f,
				-half,  half, 0f
		};

		rotateAndTranslate(coords, cx, cy, cz, tiltXDeg, tiltYDeg);

		Quad3D q = new Quad3D(p, coords, color, 2.5f, true);
		q.setLighted(true);
		return q;
	}

	/**
	 * Rotate local quad coordinates first about x and then about y, and finally
	 * translate them to their scene position.
	 *
	 * @param coords xyz triples to transform in place
	 * @param cx destination center x
	 * @param cy destination center y
	 * @param cz destination center z
	 * @param tiltXDeg x-axis rotation in degrees
	 * @param tiltYDeg y-axis rotation in degrees
	 */
	private static void rotateAndTranslate(float[] coords,
			float cx, float cy, float cz,
			float tiltXDeg, float tiltYDeg) {

		double ax = Math.toRadians(tiltXDeg);
		double ay = Math.toRadians(tiltYDeg);

		float cosX = (float) Math.cos(ax);
		float sinX = (float) Math.sin(ax);
		float cosY = (float) Math.cos(ay);
		float sinY = (float) Math.sin(ay);

		for (int i = 0; i < coords.length; i += 3) {
			float x = coords[i];
			float y = coords[i + 1];
			float z = coords[i + 2];

			// Rotate about local x.
			float yx = y * cosX - z * sinX;
			float zx = y * sinX + z * cosX;

			// Then rotate about local y.
			float xy = x * cosY + zx * sinY;
			float zy = -x * sinY + zx * cosY;

			coords[i] = xy + cx;
			coords[i + 1] = yx + cy;
			coords[i + 2] = zy + cz;
		}
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
		if (delta == 0f) {
			return;
		}

		/*
		 * A fixed combination of X/Y/Z increments is still, to a very good
		 * approximation, just rotation about one fixed compound axis. For a
		 * lighting demo that can leave the panes in long stretches of rather
		 * similar illumination. Instead, slowly precess the rotation axis itself.
		 * Y remains dominant so the motion still reads as a graceful spin, while
		 * the smaller X and Z components continually change the pane normals
		 * presented to the fixed light.
		 */
		rotationPhaseDeg += delta;
		float phase = (float) Math.toRadians(rotationPhaseDeg);

		float ax = AXIS_X_AMPLITUDE * (float) Math.sin(AXIS_X_PHASE_RATE * phase);
		float ay = 1f;
		float az = AXIS_Z_AMPLITUDE * (float) Math.cos(AXIS_Z_PHASE_RATE * phase);

		Vector3f axis = new Vector3f(ax, ay, az);
		_panel3D.rotate(axis, (float) Math.toRadians(delta));
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
