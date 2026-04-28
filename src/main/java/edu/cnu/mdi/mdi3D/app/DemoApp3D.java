package edu.cnu.mdi.mdi3D.app;


import edu.cnu.mdi.app.BaseMDIApplication;
import edu.cnu.mdi.log.Log;
import edu.cnu.mdi.mdi3D.view3D.aizawaDemo.AizawaDemoView;
import edu.cnu.mdi.mdi3D.view3D.globe.GlobeView3D;
import edu.cnu.mdi.mdi3D.view3D.kineticsDemo.KineticsDemoView;
import edu.cnu.mdi.util.Environment;
import edu.cnu.mdi.util.PropertyUtils;
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
	// Sample views used by the demo. None are meant to be completely realistic.
	// or functional, except for the LogView.
	// -------------------------------------------------------------------------

	private LogView logView;
	private KineticsDemoView kineticsView;
	private AizawaDemoView aizawaView;

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
	}
	
	@Override
	protected int getVirtualDesktopColumns() {
		return 4;
	} // opts in; 0 = disabled


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
		
		// globe has lazy loading
		ViewManager.getInstance().addConfiguration(GlobeView3D.getConfiguration());
		
		aizawaView = AizawaDemoView.createAizawaView();
	}

	@Override
    protected String getApplicationId() {
        return "mdiDemoApp";
    }


	// put the views in the virtual desktop in a reasonable default layout.
	@Override
	protected void defaultViewLayout() {
		VirtualView vv = VirtualView.getInstance(); // framework already owns it
		vv.moveTo(kineticsView, 0, VirtualView.TOPCENTER);
		vv.moveTo(aizawaView, 2, VirtualView.CENTER);
		vv.moveTo(logView, 3, VirtualView.UPPERLEFT);
	}



	/**
	 * Entry point for the demo.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		BaseMDIApplication.launch(DemoApp3D::getInstance); // launch() already exists in base
	}
}