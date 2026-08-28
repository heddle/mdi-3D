package edu.cnu.mdi.mdi3D.view3D.aizawaDemo;

import java.awt.Color;
import java.awt.Font;
import java.util.Properties;

import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Sphere;
import edu.cnu.mdi.mdi3D.item3D.Trajectory3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.view3D.SimulationView3D;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.sim.ui.IconSimulationControlPanel;
import edu.cnu.mdi.sim.ui.StandardSimIcons;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.ViewConfiguration;
import edu.cnu.mdi.view.ViewPropertiesBuilder;
import edu.cnu.mdi.view.VirtualView;

/**
 * 3D MDI demo view for the Aizawa attractor.
 * <p>
 * The simulation thread owns the model and numerical integration.
 * The EDT applies published snapshots to the 3D trajectory item and the phase
 * plot.
 * </p>
 */
@SuppressWarnings("serial")
public class AizawaDemoView extends SimulationView3D {

	/** Default frame title for the demo. */
	private static final String TITLE = "Aizawa Attractor Demo";

	/** Default visible world scale for axes. */
	private static final float AXIS_MIN = -2.0f;

	/** Default visible world scale for axes. */
	private static final float AXIS_MAX = 2.0f;

	/** Initial camera distance x. */
	static final float xdist = 0f;

	/** Initial camera distance y. */
	static final float ydist = 0f;

	/** Initial camera distance z. */
	static final float zdist = -6.0f;

	/** Initial x rotation. */
	static final float thetax = -25f;

	/** Initial y rotation. */
	static final float thetay = 35f;

	/** Initial z rotation. */
	static final float thetaz = 0f;

	/** Last applied snapshot time, used to avoid redundant redraw work. */
	private float lastAppliedTime = Float.NEGATIVE_INFINITY;

	/** The 3D trajectory item updated on the EDT. */
	private Trajectory3D trajectoryItem;

	/** Marker sphere showing the current point. */
	private Sphere headMarker;

	/**
	 * Construct the Aizawa demo view using its canonical default properties.
	 */
	public AizawaDemoView() {
		this(createDefaultProperties());
	}

	/**
	 * Construct the Aizawa demo view using the supplied properties.
	 *
	 * @param props the properties used to configure the view
	 */
	public AizawaDemoView(Properties props) {
		super(createSimulation(),
				new SimulationEngineConfig(16, 250, 0, false),
				true,
				() -> new IconSimulationControlPanel(new StandardSimIcons(), true),
				true,
				AizawaPhasePlotPanel::new,
				0.70,
				props);

		getSimulation().setEngine(getSimulationEngine());
	}

	/**
	 * Legacy compatibility constructor using alternating key/value pairs.
	 * <p>
	 * Newer code should prefer {@link #AizawaDemoView()} for the default demo view
	 * or {@link #AizawaDemoView(Properties)} for customized construction.
	 * </p>
	 *
	 * @param keyVals alternating property key/value pairs
	 */
	public AizawaDemoView(Object... keyVals) {
		this(PropertyUtils.fromKeyValues(keyVals));
	}

	/**
	 * Create the default properties for this view.
	 *
	 * @return the default view properties
	 */
	private static Properties createDefaultProperties() {
		return new ViewPropertiesBuilder()
				.title(TITLE)
				.put(PropertyUtils.ANGLE_X, thetax)
				.put(PropertyUtils.ANGLE_Y, thetay)
				.put(PropertyUtils.ANGLE_Z, thetaz)
				.put(PropertyUtils.DIST_X, xdist)
				.put(PropertyUtils.DIST_Y, ydist)
				.put(PropertyUtils.DIST_Z, zdist)
				.fraction(0.85)
				.aspect(1.6)
				.useContainer(false)
				.build();
	}

	/**
	 * Get the lazy-creation configuration for this view.
	 *
	 * @return the view configuration for lazy creation
	 */
	public static ViewConfiguration<AizawaDemoView> getConfiguration() {
		return ViewConfiguration.lazy(TITLE, AizawaDemoView::new, 2, 0, 0, VirtualView.CENTER);
	}

	@Override
	public AbstractViewInfo getViewInfo() {
		return new AizawaDemoViewInfo();
	}

	// Create the simulation with default model parameters.
	private static AizawaSimulation createSimulation() {
		AizawaModel model = new AizawaModel();
		return new AizawaSimulation(model);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		return new Panel3D(angleX, angleY, angleZ, xDist, yDist, zDist) {

			@Override
			public void createInitialItems() {

				addItem(new Axes3D(this,
						AXIS_MIN, AXIS_MAX,
						AXIS_MIN, AXIS_MAX,
						AXIS_MIN, AXIS_MAX,
						null,
						Color.darkGray,
						1f,
						5, 5, 5,
						Color.black,
						Color.blue,
						new Font("SansSerif", Font.PLAIN, 11),
						1));

				trajectoryItem = new Trajectory3D(this, new Color(0, 220, 255, 180), 2.0f,
						AizawaModel.DEFAULT_MAX_TRAIL_POINTS);
				addItem(trajectoryItem);

				headMarker = new Sphere(this, 0f, 0f, 0f, 0.05f, new Color(255, 120, 40, 220));
				headMarker.setResolution(20, 16);
				addItem(headMarker);
			}
		};
	}

	/**
	 * Compatibility factory method to create the demo view with its standard title
	 * and layout.
	 * <p>
	 * Newer code should generally prefer {@code new AizawaDemoView()}.
	 * </p>
	 *
	 * @return a new default-configured Aizawa demo view
	 */
	public static AizawaDemoView createAizawaView() {
		return new AizawaDemoView();
	}

	// ---------------------------------------------------------------------
	// Simulation lifecycle hooks (all run on EDT)
	// ---------------------------------------------------------------------

	@Override
	protected void onSimulationReady(SimulationContext ctx) {
		getPhasePanel().clearData();
		applyLatestSnapshotIfAny();
	}

	@Override
	protected void onSimulationRun(SimulationContext ctx) {
		applyLatestSnapshotIfAny();
	}

	@Override
	protected void onSimulationRefresh(SimulationContext ctx) {
		applyLatestSnapshotIfAny();
	}

	@Override
	protected void onSimulationStateChange(SimulationContext ctx, SimulationState from, SimulationState to,
			String reason) {
		if (to == SimulationState.TERMINATED || to == SimulationState.FAILED) {
			lastAppliedTime = Float.NEGATIVE_INFINITY;
		}
	}

	// Pull the latest snapshot from the model and apply it to the 3D view and phase plot.
	private void applyLatestSnapshotIfAny() {
		AizawaSnapshot snap = getModel().getSnapshot();
		if (snap == null) {
			return;
		}

		float t = snap.time();
		if (t <= lastAppliedTime) {
			return;
		}

		if (trajectoryItem != null) {
			trajectoryItem.setCoords(snap.coords());
		}

		if (headMarker != null) {
			headMarker.setCenter(snap.x(), snap.y(), snap.z());
		}

		AizawaPhasePlotPanel pp = getPhasePanel();
		if (pp != null) {
			pp.setPhaseData(snap.coords());
		}

		lastAppliedTime = t;
	}

	// ---------------------------------------------------------------------
	// Typed accessors
	// ---------------------------------------------------------------------

	/**
	 * Get the simulation instance with the correct type.
	 *
	 * @return the Aizawa simulation
	 */
	public AizawaSimulation getSimulation() {
		return (AizawaSimulation) getSimulationEngine().getSimulation();
	}

	/**
	 * Get the model instance with the correct type.
	 *
	 * @return the Aizawa model
	 */
	public AizawaModel getModel() {
		return getSimulation().getModel();
	}

	/**
	 * Get the diagnostics phase plot panel with the correct type.
	 *
	 * @return the phase plot panel
	 */
	public AizawaPhasePlotPanel getPhasePanel() {
		return (AizawaPhasePlotPanel) getDiagnosticsComponent();
	}

	/**
	 * Request a reset of the demo back to its default state.
	 * <p>
	 * This swaps in a newly created simulation and model using the standard engine
	 * reset mechanism.
	 * </p>
	 */
	public void requestReset() {
		requestEngineReset(AizawaDemoView::createSimulation,
				e -> ((AizawaSimulation) e.getSimulation()).setEngine(e),
				true,
				true);
	}
}