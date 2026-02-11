package edu.cnu.mdi.mdi3D.view3D.demo;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.Timer;

import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Cube;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.physics.PhysicsEngine;
import edu.cnu.mdi.mdi3D.physics.SimulationSnapshot;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.properties.PropertyUtils;

public class KineticsDemoView3D extends PlainView3D {

	static final float dmax = 1f;

	// viewing distances
	static final float xdist = 0f;
	static final float ydist = -.5f * dmax;
	static final float zdist = -2.5f * dmax;

	// initial rotations
	static final float thetax = -45f;
	static final float thetay = 45f;
	static final float thetaz = 45f;

	private static Color cubeColor = new Color(0, 0, 0, 10);
	
	private final EntropyPlotPanel _entropyPanel;

	// private constructor
	private KineticsDemoView3D() {
		super(PropertyUtils.TITLE, "Sample 3D View", PropertyUtils.ANGLE_X, thetax, PropertyUtils.ANGLE_Y, thetay,
				PropertyUtils.ANGLE_Z, thetaz, PropertyUtils.DIST_X, xdist, PropertyUtils.DIST_Y, ydist,
				PropertyUtils.DIST_Z, zdist, PropertyUtils.LEFT, 0, PropertyUtils.TOP, 0, PropertyUtils.FRACTION, 0.75,
				PropertyUtils.ASPECT, 1.25);
		
		_entropyPanel = addControlPanel();
	}
	
	private EntropyPlotPanel addControlPanel() {
		JPanel panel = new JPanel();
		// give a vertical layout to the control panel
		panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
		
		EntropyPlotPanel entropyPanel = new EntropyPlotPanel(500);
		panel.add(entropyPanel);
		
		this.add(panel, java.awt.BorderLayout.EAST);
		return entropyPanel;
	}

	//
	private void setupAncestorCleanup(PhysicsEngine<?> engine, Timer timer) {
		// We use a HierarchyListener to wait until the frame is actually added to a
		// window
		this.addHierarchyListener(e -> {
			if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
				java.awt.Window parentWindow = javax.swing.SwingUtilities.getWindowAncestor(this);
				if (parentWindow != null) {
					parentWindow.addWindowListener(new java.awt.event.WindowAdapter() {
						@Override
						public void windowClosing(java.awt.event.WindowEvent e) {
							engine.stop();
							timer.stop();
						}
					});
				}
			}
		});
	}

	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		return new Panel3D(thetax, thetay, thetaz, xdist, ydist, zdist) {

			@Override
			public void createInitialItems() {

				// Inside make3DPanel -> createInitialItems()
				int particleCount = 50000;
				double initialVolumeFraction = 0.25; // Start particles in 1/4th of the box

				KineticsModel model = new KineticsModel(particleCount, initialVolumeFraction, .01f);
				PhysicsEngine<Particle> engine = new PhysicsEngine<>(model); // Ensure engine constructor is generic
				// 1) Axes
				addItem(new Axes3D(this, 0, dmax, 0, dmax, 0, dmax, null, Color.darkGray, 1f, 7, 7, 8, Color.black,
						Color.blue, new Font("SansSerif", Font.PLAIN, 11), 1));

				// boundary cube (the container volume)
				addItem(new Cube(this, dmax / 2f, dmax / 2f, dmax / 2f, dmax, cubeColor, true));

				// set up the initially empty point set for the particles (we will update the
				// coordinates in the timer)
				final PointSet3D particlePoints = new PointSet3D(this, null, Color.red, 2f, true);
				addItem(particlePoints);

				// 2. Start the physics thread
				Thread thread = new Thread(engine, "Physics-Thread");
				thread.start();
				

				// 3. Create the GUI Update Timer
				Timer renderTimer = new Timer(16, e -> {
				    SimulationSnapshot<Particle> snap = engine.getLatestSnapshot();

				    if (snap != null) {
				        // Direct reference hand-off to the JOGL PointSet3D
				        // No loop, no new float[] allocation here!
				        particlePoints.setCoords(snap.coords()); 
				        _entropyPanel.addEntropy(snap.time(), snap.entropy());
				        refresh();
				    }
				});
				renderTimer.start();
				// 4. Register the cleanup (accessing the outer class 'this')
				KineticsDemoView3D.this.setupAncestorCleanup(engine, renderTimer);
			} // create initial items

		};

	}

	/**
	 * Static method to build the view
	 * 
	 * @return
	 */
	public static KineticsDemoView3D createKineticsView3D() {
		return new KineticsDemoView3D();
	}

}
