package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

import java.awt.Color;
import java.awt.Font;
import java.util.Properties;

import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Cube;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.view3D.SimulationView3D;
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.SimulationState;
import edu.cnu.mdi.view.ViewConfiguration;
import edu.cnu.mdi.view.ViewPropertiesBuilder;
import edu.cnu.mdi.view.VirtualView;

/**
 * 3D kinetic-gas demo hosted in the MDI simulation framework.
 * <p>
 * Key design point: the simulation thread owns the physics and the model. The
 * view updates OpenGL/Swing state only from engine refresh callbacks on the
 * EDT.
 * </p>
 */
@SuppressWarnings("serial")
public class KineticsDemoView extends SimulationView3D {

	/** Default frame title for the demo. */
	private static final String TITLE = "Kinetics Demo";

	// Default parameters for the kinetics model
	public static final int DEFAULT_PARTICLE_COUNT = 50_000;
	public static final float DEFAULT_VOLUME_FRACTION = 0.25f;
	public static final float DEFAULT_INITIAL_TEMP = 0.01f;

	/** The side length of the bounding cube. */
	private static final float LENGTH = 1.0f;

	// viewing distances
	static final float xdist = 0f;
	static final float ydist = -.5f * LENGTH;
	static final float zdist = -2.5f * LENGTH;

	// initial rotations
	static final float thetax = -45f;
	static final float thetay = 45f;
	static final float thetaz = 45f;

	// Track the time of the last applied snapshot (prevents redundant work)
	private float lastAppliedTime = Float.NEGATIVE_INFINITY;

	// cube color (with alpha for transparency)
	private static final Color cubeColor = new Color(0, 0, 0, 10);

	// the PointSet3D that will display the particles (updated on EDT)
	private PointSet3D particlePoints;

	/**
	 * Construct the kinetics demo view using its canonical default properties.
	 */
	public KineticsDemoView() {
		this(createDefaultProperties());
	}

	/**
	 * Construct the kinetics demo view using the supplied properties.
	 *
	 * @param props the properties used to configure the view
	 */
	public KineticsDemoView(Properties props) {
		super(createSimulation(),
				new SimulationEngineConfig(16, 250, 0, false),
				true,
				(SimulationView3D.ControlPanelFactory) ControlPanel::new,
				true,
				EntropyPlotPanel::new,
				0.70,
				props);

		// Give the simulation a handle to the engine for optional message/progress posts.
		getSimulation().setEngine(getSimulationEngine());
	}

	/**
	 * Legacy compatibility constructor using alternating key/value pairs.
	 *
	 * @param keyVals alternating property key/value pairs
	 */
	public KineticsDemoView(Object... keyVals) {
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
	public static ViewConfiguration<KineticsDemoView> getConfiguration() {
		return ViewConfiguration.lazy(TITLE, KineticsDemoView::new, 0, 0, 0, VirtualView.CENTER);
	}

	// Helper method to create the simulation instance with default parameters.
	private static KineticsSimulation createSimulation() {
		KineticsModel model = new KineticsModel(DEFAULT_PARTICLE_COUNT, LENGTH, DEFAULT_VOLUME_FRACTION,
				DEFAULT_INITIAL_TEMP);
		return new KineticsSimulation(model);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		return new Panel3D(angleX, angleY, angleZ, xDist, yDist, zDist) {

			@Override
			public void createInitialItems() {

				// Create axes
				addItem(new Axes3D(this, 0, LENGTH, 0, LENGTH, 0, LENGTH, null, Color.darkGray, 1f, 7, 7, 8,
						Color.black, Color.blue, new Font("SansSerif", Font.PLAIN, 11), 1));

				// Create boundary cube (container volume)
				addItem(new Cube(this, LENGTH / 2f, LENGTH / 2f, LENGTH / 2f, LENGTH, cubeColor, true));

				// Initially empty point set (coords will be swapped in on refresh events)
				particlePoints = new PointSet3D(this, null, Color.red, 1f, true);
				addItem(particlePoints);
			}
		};
	}

	/**
	 * Compatibility factory to build the view with its standard defaults.
	 * <p>
	 * Newer code should generally prefer {@code new KineticsDemoView()}.
	 * </p>
	 *
	 * @return a new default-configured kinetics view
	 */
	public static KineticsDemoView createKineticsView() {
		return new KineticsDemoView();
	}

	// ---------------------------------------------------------------------
	// Simulation lifecycle hooks (all run on EDT)
	// ---------------------------------------------------------------------

	@Override
	protected void onSimulationReady(SimulationContext ctx) {
		getEntropyPanel().clearData();
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

	// Fetch the latest snapshot and apply it to the 3D point set + entropy plot.
	private void applyLatestSnapshotIfAny() {
		SimulationSnapshot<Particle> snap = getModel().getSnapshot();
		if (snap == null) {
			return;
		}

		float t = snap.time();
		if (t <= lastAppliedTime) {
			return;
		}

		if (particlePoints != null) {
			particlePoints.setCoords(snap.coords());
		}

		EntropyPlotPanel ep = getEntropyPanel();
		if (ep != null) {
			ep.addEntropy(snap.time(), snap.entropy());
		}

		lastAppliedTime = t;
	}

	// ---------------------------------------------------------------------
	// Typed accessors
	// ---------------------------------------------------------------------

	/**
	 * Helper method to get the simulation instance with the correct type.
	 *
	 * @return the kinetics simulation
	 */
	public KineticsSimulation getSimulation() {
		return (KineticsSimulation) getSimulationEngine().getSimulation();
	}

	/**
	 * Helper method to get the model instance with the correct type.
	 *
	 * @return the kinetics model
	 */
	public KineticsModel getModel() {
		return getSimulation().getModel();
	}

	/**
	 * Helper method to get the diagnostics panel with the correct type.
	 *
	 * @return the entropy plot panel
	 */
	public EntropyPlotPanel getEntropyPanel() {
		return (EntropyPlotPanel) getDiagnosticsComponent();
	}

	// ---------------------------------------------------------------------
	// Optional: reset hook used by the demo ControlPanel
	// ---------------------------------------------------------------------

	/**
	 * Reset the demo back to its default parameters.
	 * <p>
	 * This requests a safe engine reset: if running, it will stop and swap the
	 * engine once the current simulation terminates.
	 * </p>
	 */
	public void requestReset() {
		requestEngineReset(KineticsDemoView::createSimulation,
				e -> ((KineticsSimulation) e.getSimulation()).setEngine(e),
				true,
				true);
	}
}