package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

import java.awt.Color;
import java.awt.Font;

import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Cube;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.view3D.SimulationView3D;
import edu.cnu.mdi.properties.PropertyUtils;
import edu.cnu.mdi.sim.SimulationContext;
import edu.cnu.mdi.sim.SimulationEngineConfig;
import edu.cnu.mdi.sim.SimulationState;

/**
 * 3D kinetic-gas demo hosted in the MDI simulation framework.
 * <p>
 * Key design point: the simulation thread owns the physics and the model. The view
 * updates OpenGL/Swing state only from engine refresh callbacks on the EDT.
 * </p>
 */
@SuppressWarnings("serial")
public class KineticsDemoView extends SimulationView3D {

	// Default parameters for the kinetics model
	public static final int DEFAULT_PARTICLE_COUNT = 50_000;
	public static final float DEFAULT_VOLUME_FRACTION = 0.25f;
	public static final float DEFAULT_INITIAL_TEMP = 0.01f;

	private static final float LENGTH = 1.0f; // The side length of the bounding cube

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
	 * Create a kinetics demo view with default parameters.
	 *
	 * @param keyVals Optional key-value pairs for additional configuration.
	 */
	public KineticsDemoView(Object... keyVals) {
		// Build simulation + engine config first.
		super(createSimulation(),
				new SimulationEngineConfig(16, 250, 0, false), // ~60 Hz refresh, no cooperative yield, start in READY
				true,
				(SimulationView3D.ControlPanelFactory) ControlPanel::new,
				true,
				EntropyPlotPanel::new,
				0.70,
				keyVals);

		// Give the simulation a handle to the engine for optional message/progress posts.
		getSimulation().setEngine(getSimulationEngine());
	}

	// Helper method to create the simulation instance with default parameters.
	private static KineticsSimulation createSimulation() {
		KineticsModel model = new KineticsModel(DEFAULT_PARTICLE_COUNT, LENGTH, DEFAULT_VOLUME_FRACTION,
				DEFAULT_INITIAL_TEMP);
		// For this demo, entropy every update is fine. If you want faster throughput,
		// try model.setEntropyEvery(2) or (3).
		return new KineticsSimulation(model);
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		return new Panel3D(thetax, thetay, thetaz, xdist, ydist, zdist) {

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
	 * Static factory to build the view with reasonable defaults.
	 *
	 * @return a new instance of {@link KineticsDemoView}
	 */
	public static KineticsDemoView createKineticsView() {
		return new KineticsDemoView(PropertyUtils.TITLE, "Kinetics Demo", PropertyUtils.ANGLE_X, thetax,
				PropertyUtils.ANGLE_Y, thetay, PropertyUtils.ANGLE_Z, thetaz, PropertyUtils.DIST_X, xdist,
				PropertyUtils.DIST_Y, ydist, PropertyUtils.DIST_Z, zdist, PropertyUtils.FRACTION, 0.85,
				PropertyUtils.ASPECT, 1.6);
	}

	// ---------------------------------------------------------------------
	// Simulation lifecycle hooks (all run on EDT)
	// ---------------------------------------------------------------------

	@Override
	protected void onSimulationReady(SimulationContext ctx) {
		// Seed initial drawing once the sim has initialized.
		System.out.println("Kinetics simulation READY, seeding initial drawing...");
		getEntropyPanel().clearData();
		applyLatestSnapshotIfAny();
		System.out.println("Kinetics simulation READY with " + getModel().size() + " particles.");
	}

	@Override
	protected void onSimulationRun(SimulationContext ctx) {
		// Optional: ensure we paint at least once right as we enter RUNNING.
		applyLatestSnapshotIfAny();
	}

	@Override
	protected void onSimulationRefresh(SimulationContext ctx) {
		// Engine refresh is already rate-limited and guaranteed to arrive on EDT.
		applyLatestSnapshotIfAny();
	}

	@Override
	protected void onSimulationStateChange(SimulationContext ctx, SimulationState from, SimulationState to,
			String reason) {
		if (to == SimulationState.TERMINATED || to == SimulationState.FAILED) {
			// allow a restart to repaint from t=0
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
			// Direct buffer hand-off to JOGL PointSet3D (no per-particle loop on EDT)
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
	 */
	public KineticsSimulation getSimulation() {
		return (KineticsSimulation) getSimulationEngine().getSimulation();
	}

	/**
	 * Helper method to get the model instance with the correct type.
	 */
	public KineticsModel getModel() {
		return getSimulation().getModel();
	}

	/**
	 * Helper method to get the diagnostics panel with the correct type.
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
	 * This requests a safe engine reset: if running, it will stop and swap the engine
	 * once the current simulation terminates.
	 * </p>
	 */
	public void requestReset() {
		requestEngineReset(KineticsDemoView::createSimulation,
				e -> ((KineticsSimulation) e.getSimulation()).setEngine(e),
				true,
				true);
	}
}
