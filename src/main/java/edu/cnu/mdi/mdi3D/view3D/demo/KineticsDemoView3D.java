package edu.cnu.mdi.mdi3D.view3D.demo;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.Timer;

import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Cube;
import edu.cnu.mdi.mdi3D.item3D.PointSet3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.physics.IPhysicsSimHost;
import edu.cnu.mdi.mdi3D.physics.PhysicsEngine;
import edu.cnu.mdi.mdi3D.physics.SimulationSnapshot;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.properties.PropertyUtils;

@SuppressWarnings("serial")
public class KineticsDemoView3D extends PlainView3D implements IPhysicsSimHost {

	static final float dmax = 1f;

	// viewing distances
	static final float xdist = 0f;
	static final float ydist = -.5f * dmax;
	static final float zdist = -2.5f * dmax;

	// initial rotations
	static final float thetax = -45f;
	static final float thetay = 45f;
	static final float thetaz = 45f;

	// cube color (with alpha for transparency)
	private static Color cubeColor = new Color(0, 0, 0, 10);
	
	//Live entropy v. time plot panel
	private EntropyPlotPanel _entropyPanel;
	
	//the simulation engine
	private PhysicsEngine<Particle> engine;
	
	//the timer that updates the GUI
	private Timer renderTimer;

	// private constructor
	private KineticsDemoView3D() {
		super(PropertyUtils.TITLE, "Sample 3D View", PropertyUtils.ANGLE_X, thetax, PropertyUtils.ANGLE_Y, thetay,
				PropertyUtils.ANGLE_Z, thetaz, PropertyUtils.DIST_X, xdist, PropertyUtils.DIST_Y, ydist,
				PropertyUtils.DIST_Z, zdist, PropertyUtils.LEFT, 0, PropertyUtils.TOP, 0, PropertyUtils.FRACTION, 0.85,
				PropertyUtils.ASPECT, 1.6);
		
		//add the control panel on the right side
		addControlPanel();
	}
	
	// Add the control panel on the right side
	private void addControlPanel() {
		JPanel panel = new JPanel();
		// give a vertical layout to the control panel
		panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
		
		//the plot panel
		_entropyPanel = new EntropyPlotPanel(450);
		panel.add(_entropyPanel);
		
		add(panel, java.awt.BorderLayout.EAST);
	}
	
	// Clean up on exit
	@Override
    public void prepareForExit() {
		if (engine != null) {
			engine.stop();
			System.out.println("Physics engine stopped in prepareForExit.");
		}
		if (renderTimer != null) {
			renderTimer.stop();
			System.out.println("Render timer stopped in prepareForExit.");
		}
		super.prepareForExit();
	}
	
	// make the 3d panel. This is where the 3D items are created
	@Override
	protected Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		return new Panel3D(thetax, thetay, thetaz, xdist, ydist, zdist) {

			@Override
			public void createInitialItems() {

				// Inside make3DPanel -> createInitialItems()
				int particleCount = 50000;
				double initialVolumeFraction = 0.25; // Start particles in 1/4th of the box

				KineticsModel model = new KineticsModel(particleCount, initialVolumeFraction, .01f);
				engine = new PhysicsEngine<>(model); // Ensure engine constructor is generic
				//create Axes
				addItem(new Axes3D(this, 0, dmax, 0, dmax, 0, dmax, null, Color.darkGray, 1f, 7, 7, 8, Color.black,
						Color.blue, new Font("SansSerif", Font.PLAIN, 11), 1));

				//create boundary cube (the container volume)
				addItem(new Cube(this, dmax / 2f, dmax / 2f, dmax / 2f, dmax, cubeColor, true));

				// set up the initially empty point set for the particles (we will update the
				// coordinates in the timer)
				final PointSet3D particlePoints = new PointSet3D(this, null, Color.red, 2f, true);
				addItem(particlePoints);

				//Start the physics thread
				engine.start();			

				//Create the GUI Update Timer
				renderTimer = new Timer(16, e -> {
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
	
	//--- IPhysicsSimHost implementation

	@Override
	public PhysicsEngine<Particle> getPhysicsEngine() {
		return engine;
	}

}
