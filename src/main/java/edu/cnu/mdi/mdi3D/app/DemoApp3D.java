package edu.cnu.mdi.mdi3D.app;


import java.awt.EventQueue;

import edu.cnu.mdi.app.BaseMDIApplication;
import edu.cnu.mdi.desktop.Desktop;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mdi3D.view3D.globe.GlobeView3D;
import edu.cnu.mdi.mdi3D.view3D.kineticsDemo.KineticsDemoView;
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

	private LogView logView;
	private KineticsDemoView kineticsView;
	private GlobeView3D globeView;

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

		kineticsView = KineticsDemoView.createKineticsView();
		globeView = GlobeView3D.createGlobeView();
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

		// Column 0: kinetics demo centered
		virtualView.moveTo(kineticsView, 0, VirtualView.CENTER);

		// Column 1: globe view centered
		virtualView.moveTo(globeView, 1, VirtualView.CENTER);

		// column 2: log view upper left (is not vis by default)
		virtualView.moveTo(logView, 2, VirtualView.UPPERLEFT);

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