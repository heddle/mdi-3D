package edu.cnu.mdi.mdi3D.app;


import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.app.BaseMDIApplication;
import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Item3D;
import edu.cnu.mdi.mdi3D.item3D.Line3D;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.item3D.Sphere;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.mdi3D.view3D.demo.KineticsDemoView3D;
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.view.LogView;
import edu.cnu.mdi.view.ViewManager;
import edu.cnu.mdi.view.VirtualView;

/**
 * Demo application for the MDI framework.
 * <p>
 * This class is intentionally "example-first": it demonstrates how a typical
 * application:
 * <ol>
 * <li>Creates the main application frame ({@link BaseMDIApplication})</li>
 * <li>Creates a few internal views (2D map, 3D, drawing, log)</li>
 * <li>Optionally enables a {@link VirtualView} to simulate a virtual
 * desktop</li>
 * <li>Applies default view placement, then applies any persisted
 * layout/config</li>
 * </ol>
 * <p>
 * The "virtual desktop" logic is driven by {@link BaseMDIApplication}'s virtual
 * desktop lifecycle hooks:
 * <ul>
 * <li>{@link #onVirtualDesktopReady()} runs once after the frame is
 * showing</li>
 * <li>{@link #onVirtualDesktopRelayout()} runs (debounced) after
 * resizes/moves</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class DemoApp3D extends BaseMDIApplication {

	/** Singleton instance of the demo app. */
	private static DemoApp3D INSTANCE;

	// -------------------------------------------------------------------------
	// Optional virtual desktop support
	// -------------------------------------------------------------------------

	/** Virtual desktop view (optional). */
	private VirtualView virtualView;

	/** If true, install the VirtualView and place views into columns. */
	private final boolean enableVirtualDesktop = true;

	/** Number of "columns"/cells in the virtual desktop. */
	private final int virtualDesktopCols = 3;

	// -------------------------------------------------------------------------
	// Sample views used by the demo. None are meant to be completely realistic.
	// or functional, except for the LogView.
	// -------------------------------------------------------------------------

	private PlainView3D view3D;
	private LogView logView;
	private KineticsDemoView3D kineticsView3D;

	/**
	 * Private constructor: use {@link #getInstance()}.
	 *
	 * @param keyVals optional key-value pairs passed to {@link BaseMDIApplication}
	 */
	private DemoApp3D(Object... keyVals) {
		super(keyVals);

		// Enable the framework-managed virtual desktop lifecycle (one-shot ready +
		// debounced relayout).
		prepareForVirtualDesktop();

		// Log environment information early.
		Log.getInstance().info(Environment.getInstance().toString());

		// Create internal views. (Do not depend on the outer frame being visible here.)
		addInitialViews();

		// Optionally create the virtual desktop overview.
		// Note: VirtualView now resolves its parent frame lazily in addNotify().
		if (enableVirtualDesktop) {
			virtualView = VirtualView.createVirtualView(virtualDesktopCols);
			virtualView.toFront();
		}
	}

	/**
	 * Public access to the singleton.
	 *
	 * @return the singleton main application frame
	 */
	public static DemoApp3D getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new DemoApp3D(PropertyUtils.TITLE, "Demo Application of MDI Views",
					PropertyUtils.BACKGROUNDIMAGE,
					Environment.MDI_RESOURCE_PATH + "images/mdilogo.png",
					PropertyUtils.FRACTION, 0.8);
		}
		return INSTANCE;
	}

	/**
	 * Create and register the initial set of views shown in the demo.
	 * <p>
	 * This method only builds views; it should not depend on the outer frame being
	 * shown or on final geometry.
	 */
	private void addInitialViews() {

		// Log view is useful but not always visible.
		logView = new LogView();
		logView.setVisible(false);
		ViewManager.getInstance().getViewMenu().addSeparator();

		view3D = create3DView();
		kineticsView3D = KineticsDemoView3D.createKineticsView3D();
	}

	@Override
    protected String getApplicationId() {
        return "mdiDemoApp";
    }

	/**
	 * Runs once after the outer frame is showing and Swing layout has stabilized.
	 * <p>
	 * This is the correct place to:
	 * <ul>
	 * <li>reconfigure the {@link VirtualView} based on the real frame size</li>
	 * <li>apply the demo's default view placement</li>
	 * <li>then load/apply any persisted layout (which may override defaults)</li>
	 * </ul>
	 */
	@Override
	protected void onVirtualDesktopReady() {

		// If virtual desktop is enabled, apply the demo defaults first.
		if (virtualView != null) {
			virtualView.reconfigure();
			restoreDefaultViewLocations();
		}

		// Apply persisted configuration last, so saved layouts override demo defaults.
		Desktop.getInstance().loadConfigurationFile();
		Desktop.getInstance().configureViews();

		Log.getInstance().info("Application name = " + Environment.getApplicationName());
		Log.getInstance().info("Config file = " + Environment.getInstance().getConfigurationFile());

		Log.getInstance().info("DemoApp is ready.");
	}

	/**
	 * Runs after the outer frame is resized or moved (debounced).
	 * <p>
	 * Keep this lightweight. Reconfiguring the virtual desktop updates its world
	 * sizing and refreshes the thumbnail items.
	 */
	@Override
	protected void onVirtualDesktopRelayout() {
		if (virtualView != null) {
			virtualView.reconfigure();
		}
	}

	/**
	 * Places the demo views into a reasonable "default" arrangement on the virtual
	 * desktop.
	 * <p>
	 * If a user has a saved configuration, {@link Desktop#configureViews()} will
	 * typically override these positions.
	 */
	private void restoreDefaultViewLocations() {
		
		//Column 0`: kinetics view bottom center (overlaps with 3D view, but shows the value of virtual desktop layering)
		virtualView.moveTo(kineticsView3D, 0, VirtualView.BOTTOMCENTER);
		// Column 1: 3D centered
		virtualView.moveTo(view3D, 1, VirtualView.CENTER);

	// column 2: log view upper left (is not vis by default)
		virtualView.moveTo(logView, 2, VirtualView.UPPERLEFT);

	}

	// -------------------------------------------------------------------------
	// Demo view creation helpers
	// -------------------------------------------------------------------------

	/**
	 * Create a sample 3D view with a few items to demonstrate rendering.
	 *
	 * @return a new {@link PlainView3D}
	 */
	private PlainView3D create3DView() {
		final float xymax = 600f;
		final float zmax = 600f;
		final float zmin = -200f;
		final float xdist = 0f;
		final float ydist = 0f;
		final float zdist = -2.75f * xymax;

		final float thetax = 45f;
		final float thetay = 45f;
		final float thetaz = 45f;

		PlainView3D view3D = new PlainView3D(PropertyUtils.TITLE, "Sample 3D View", PropertyUtils.ANGLE_X, thetax,
				PropertyUtils.ANGLE_Y, thetay, PropertyUtils.ANGLE_Z, thetaz, PropertyUtils.DIST_X, xdist,
				PropertyUtils.DIST_Y, ydist, PropertyUtils.DIST_Z, zdist, PropertyUtils.LEFT, 0,
				PropertyUtils.TOP, 0, PropertyUtils.FRACTION, 0.75, PropertyUtils.ASPECT, 1.25) {

			@Override
			protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist,
					float zDist) {

				return new Panel3D(thetax, thetay, thetaz, xdist, ydist, zdist) {

					@Override
					public void createInitialItems() {

						// 1) Axes
						addItem(new Axes3D(this, -xymax, xymax, -xymax, xymax, zmin, zmax, null, Color.darkGray, 1f, 7,
								7, 8, Color.black, Color.blue, new Font("SansSerif", Font.PLAIN, 11), 0));

						// 2) Floor grid (y=0 plane), using many Line3D items
						final float gridHalf = 600f;
						final float step = 100f;
						final float yGrid = 0f;
						final Color gridColor = new Color(0, 0, 0, 40);

						for (float x = -gridHalf; x <= gridHalf; x += step) {
							addItem(new Line3D(this, x, yGrid, -gridHalf, x, yGrid, gridHalf, gridColor, 1f));
						}
						for (float z = -gridHalf; z <= gridHalf; z += step) {
							addItem(new Line3D(this, -gridHalf, yGrid, z, gridHalf, yGrid, z, gridColor, 1f));
						}

						// 3) “Earth” sphere at origin with gridlines
						Sphere earth = new Sphere(this, 0f, 0f, 0f, 140f, new Color(40, 100, 220, 128));
						earth.setFillAlpha(128);
						earth.setResolution(32, 24);

						// latitude + longitude lines
						float[] theta = new float[] { (float) (Math.PI * 0.25), (float) (Math.PI * 0.5),
								(float) (Math.PI * 0.75) };
						float[] phi = new float[] { 0f, (float) (Math.PI / 3), (float) (2 * Math.PI / 3),
								(float) (Math.PI) };
						earth.setGridlines(theta, phi);
						earth.setGridColor(new Color(0, 0, 0, 90));
						addItem(earth);

						// 4) Orbit ring (tilted circle polyline) – anonymous Item3D
						final float moonOrbitalRad = 320f;
						final float satOrbitalRad = 220f;

						// Kepler's 3rd law: (Tm/Ts)^2 = (Rm/Rs)^3
						final double periodFact = Math.pow(moonOrbitalRad / satOrbitalRad, 1.5f);
						final float tilt = (float) Math.toRadians(25);
						final int n = 240;

						final float[] orbitCoords = new float[3 * (n + 1)];
						for (int i = 0; i <= n; i++) {
							float a = (float) (2 * Math.PI * i / n);
							float x = moonOrbitalRad * (float) Math.cos(a);
							float z = moonOrbitalRad * (float) Math.sin(a);

							// tilt about x-axis -> rotates y/z
							float y = z * (float) Math.sin(tilt);
							float zt = z * (float) Math.cos(tilt);

							orbitCoords[3 * i] = x;
							orbitCoords[3 * i + 1] = y;
							orbitCoords[3 * i + 2] = zt;
						}

						addItem(new Item3D(this) {
							@Override
							public void draw(GLAutoDrawable drawable) {
								Support3D.drawPolyLine(drawable, orbitCoords, new Color(0, 0, 0, 110), 2f);
							}
						});

						// “Moon” sphere we will animate by updating its center
						final Sphere moon = new Sphere(this, moonOrbitalRad, 0f, 0f, 45f, new Color(200, 200, 200));
						moon.setResolution(24, 18);
						moon.setGridlines(theta, phi);
						moon.setGridColor(new Color(0, 0, 0, 90));
						addItem(moon);

						// A simple “satellite” point (or small sphere/cylinder)
						final float[] sat = new float[] { moonOrbitalRad, 0f, 0f };
						addItem(new PointSet3D(this, sat, new Color(255, 120, 0), 10f, true));

						final int tstep = 500;
						final int nstep = 28;
						final long startTime = System.currentTimeMillis();
						// 7) Optional animation: orbit the moon + satellite
						javax.swing.Timer timer = new javax.swing.Timer(tstep, e -> {
							double t = System.currentTimeMillis() - startTime;
							t = t * 2 * Math.PI / (nstep * tstep);

							float xmoon = moonOrbitalRad * (float) Math.cos(t);
							float zmoon = moonOrbitalRad * (float) Math.sin(t);

							float ymoon = zmoon * (float) Math.sin(tilt);
							float zt = zmoon * (float) Math.cos(tilt);
							moon.setCenter(xmoon, ymoon, zt);

							t *= periodFact; // faster orbit for satellite
							float xsat = satOrbitalRad * (float) Math.cos(t);
							float zsat = satOrbitalRad * (float)Math.sin(t);
							float ysat = zsat * (float)Math.sin(tilt);
							float zsatTilt = zsat * (float)Math.cos(tilt);

							sat[0] = xsat;
							sat[1] = ysat;
							sat[2] = zsatTilt;

							refresh(); // redraw
						});
						timer.start();
					}

					@Override
					public float getZStep() {
						// Step size for zooming in/out.
						return (zmax - zmin) / 50f;
					}
				};
			}
		};

		return view3D;
	}


	/**
	 * Entry point for the demo.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			DemoApp3D frame = DemoApp3D.getInstance();
			frame.restoreDefaultViewLocations();
			frame.setVisible(true);
		});
	}
}