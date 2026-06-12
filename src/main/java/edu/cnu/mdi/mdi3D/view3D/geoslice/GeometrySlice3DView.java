package edu.cnu.mdi.mdi3D.view3D.geoslice;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

import edu.cnu.mdi.geometry.Line;
import edu.cnu.mdi.mdi3D.item3D.Axes3D;
import edu.cnu.mdi.mdi3D.item3D.Line3D;
import edu.cnu.mdi.mdi3D.item3D.Quad3D;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.ViewConfiguration;
import edu.cnu.mdi.view.ViewPropertiesBuilder;
import edu.cnu.mdi.view.VirtualView;
import edu.cnu.mdi.view.demo.geoslice.GeometrySliceModel;
import edu.cnu.mdi.view.demo.geoslice.Shell3D;
import edu.cnu.mdi.view.demo.geoslice.SliceProjection;
import edu.cnu.mdi.view.demo.geoslice.Wire3D;

/**
 * Companion MDI-3D view for the 2D geometry slice demo.
 *
 * <p>
 * The corresponding 2D view shows a constant-phi slice through a synthetic 3D
 * detector-like geometry. This view renders the same source model directly in
 * 3D: shell wireframes, wire line segments, fake-hit wires, axes, and the
 * current constant-phi slice plane. Its purpose is explanatory. It helps the
 * reader see what the 2D slice view is cutting through.
 * </p>
 *
 * <p>
 * The view deliberately reuses {@link GeometrySliceModel}, {@link Shell3D},
 * and {@link Wire3D} from the 2D demo package. That is the important design
 * point: the model is shared, while the 2D and 3D views render different
 * representations of that same model.
 * </p>
 */
@SuppressWarnings("serial")
public class GeometrySlice3DView extends PlainView3D {
	
	/** Approximate x/y half extent of the scene. */
	private static final float SCENE_XY_EXTENT = 600f;

	/** Approximate z extent of the scene. */
	private static final float SCENE_Z_EXTENT = 700f;

	/** Characteristic size used for keyboard and mouse-wheel navigation. */
	private static final float NAVIGATION_EXTENT = SCENE_Z_EXTENT;

    /** Default frame title. */
    private static final String TITLE = "Geometry Slice 3D View";

    /** Minimum slider phi angle, in degrees. */
    private static final int PHI_MIN = -25;

    /** Maximum slider phi angle, in degrees. */
    private static final int PHI_MAX = 25;

    /** Initial slider phi angle, in degrees. */
    private static final int PHI_INITIAL = 0;

    /** Initial camera rotation about the x axis. */
    private static final float DEFAULT_ANGLE_X = -28f;

    /** Initial camera rotation about the y axis. */
    private static final float DEFAULT_ANGLE_Y = 18f;

    /** Initial camera rotation about the z axis. */
    private static final float DEFAULT_ANGLE_Z = -18f;

    /** Initial scene translation in x. */
    private static final float DEFAULT_DIST_X = 0f;

    /** Initial scene translation in y. */
    private static final float DEFAULT_DIST_Y = -170f;

    /** Initial scene translation in z. */
    private static final float DEFAULT_DIST_Z = -1250f;

    /** Shared synthetic model used by the 2D and 3D demos. */
    private final GeometrySliceModel model = GeometrySliceModel.createDefault();

    /** Current slice angle, in degrees. */
    private double phiDeg = PHI_INITIAL;

    /** The hosted 3D panel, captured so slider events can rebuild the scene. */
    private Panel3D scenePanel;

    /** Label showing the current phi value. */
    private JLabel phiLabel;

    /**
     * Construct the view with its default properties.
     */
    public GeometrySlice3DView() {
        this(createDefaultProperties());
    }

    /**
     * Construct the view with explicit properties.
     *
     * @param props view construction properties
     */
    public GeometrySlice3DView(Properties props) {
        super(props);
        add(createControlPanel(), BorderLayout.SOUTH);

        // The Panel3D constructor calls createInitialItems() before this view's
        // fields have finished initializing. The panel schedules the first
        // rebuild for later, but this second call is harmless and makes the
        // startup path explicit.
        SwingUtilities.invokeLater(this::rebuildScene);
    }

    /**
     * Legacy compatibility constructor using alternating key/value pairs.
     *
     * @param keyVals alternating property key/value pairs
     */
    public GeometrySlice3DView(Object... keyVals) {
        this(PropertyUtils.fromKeyValues(keyVals));
    }
    
    @Override
    public AbstractViewInfo getViewInfo() {
        return new GeometrySlice3DViewInfo();
    }

    /**
     * Create the default construction properties for this 3D view.
     *
     * @return default view properties
     */
    private static Properties createDefaultProperties() {
        return new ViewPropertiesBuilder()
                .title(TITLE)
                .put(PropertyUtils.ANGLE_X, DEFAULT_ANGLE_X)
                .put(PropertyUtils.ANGLE_Y, DEFAULT_ANGLE_Y)
                .put(PropertyUtils.ANGLE_Z, DEFAULT_ANGLE_Z)
                .put(PropertyUtils.DIST_X, DEFAULT_DIST_X)
                .put(PropertyUtils.DIST_Y, DEFAULT_DIST_Y)
                .put(PropertyUtils.DIST_Z, DEFAULT_DIST_Z)
                .fraction(0.80)
                .aspect(1.6)
                .useContainer(false)
                .build();
    }

    /**
     * Return the lazy-creation configuration for this view.
     *
     * @return view configuration for lazy creation
     */
    public static ViewConfiguration<GeometrySlice3DView> getConfiguration() {
        return ViewConfiguration.lazy(
                TITLE,
                GeometrySlice3DView::new,
                1,
                0,
                0,
                VirtualView.CENTER);
    }

    /**
     * Compatibility factory method.
     *
     * @return a new default-configured 3D slice view
     */
    public static GeometrySlice3DView createGeometrySlice3DView() {
        return new GeometrySlice3DView();
    }

    /**
     * Create the hosted 3D panel.
     *
     * <p>
     * {@link Panel3D} invokes {@code createInitialItems()} from its own
     * constructor. Because this occurs during the superclass constructor call,
     * the outer {@code GeometrySlice3DView} has not finished field
     * initialization yet. For that reason the initial scene rebuild is deferred
     * with {@link SwingUtilities#invokeLater(Runnable)}.
     * </p>
     */
    @Override
    protected Panel3D make3DPanel(float angleX, float angleY, float angleZ,
            float xDist, float yDist, float zDist) {

        Panel3D panel = new Panel3D(
                angleX, angleY, angleZ,
                xDist, yDist, zDist,
                0.98f, 0.98f, 0.96f,
                false) {

            @Override
            public void createInitialItems() {
                SwingUtilities.invokeLater(GeometrySlice3DView.this::rebuildScene);
            }
        };
        
        panel.setNavigationStepFromExtent(NAVIGATION_EXTENT);

        scenePanel = panel;
        return panel;
    }

    /**
     * Create the bottom control strip containing the phi slider.
     *
     * @return control panel
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setBorder(BorderFactory.createEtchedBorder());

        phiLabel = new JLabel(labelText());
        phiLabel.setPreferredSize(new Dimension(70, 22));
        phiLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        phiLabel.setFont(Fonts.smallFont);

        JSlider slider = new JSlider(PHI_MIN, PHI_MAX, PHI_INITIAL);
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(360, 48));

        slider.addChangeListener((ChangeEvent e) -> {
            phiDeg = slider.getValue();
            phiLabel.setText(labelText());
            rebuildScene();
        });

        JLabel note = new JLabel("yellow plane is the current constant-\u03c6 slice");
        note.setFont(Fonts.smallFont);

        panel.add(new JLabel("Slice angle:"));
        panel.add(phiLabel);
        panel.add(slider);
        panel.add(note);

        return panel;
    }

    /**
     * Return the text for the current phi label.
     *
     * @return phi label text
     */
    private String labelText() {
        return String.format("\u03c6 = %.0f\u00b0", phiDeg);
    }

    /**
     * Rebuild the complete 3D scene for the current phi value.
     *
     * <p>
     * The scene is small enough that clearing and rebuilding is simpler and less
     * error-prone than trying to update just the slice-plane item.
     * </p>
     */
    private void rebuildScene() {
        if (scenePanel == null) {
            return;
        }

        scenePanel.clearItems();

        addAxes(scenePanel);
        addShellWireframes(scenePanel);
        addWires(scenePanel);
        addSlicePlane(scenePanel, phiDeg);

        scenePanel.softRefresh();
    }

    /**
     * Add labeled Cartesian axes to the scene.
     *
     * @param panel 3D panel receiving the axis item
     */
    private void addAxes(Panel3D panel) {
    	float[] limits = new float[] {
    	        -SCENE_XY_EXTENT, SCENE_XY_EXTENT,
    	        -SCENE_XY_EXTENT, SCENE_XY_EXTENT,
    	        0f, SCENE_Z_EXTENT
    	};
    	
        String[] labels = { "x", "y", "z" };

        Axes3D axes = new Axes3D(
                panel,
                limits,
                labels,
                new Color(90, 90, 90),
                1.0f,
                4,
                4,
                4,
                new Color(110, 110, 110),
                Color.DARK_GRAY,
                Fonts.smallFont,
                0);

        panel.addItem(axes);
    }

    /**
     * Add the wireframe for each synthetic 3D shell.
     *
     * @param panel 3D panel receiving the shell edges
     */
    private void addShellWireframes(Panel3D panel) {
        for (Shell3D shell : model.getShells()) {
            Color color = shellColor(shell.getRegionIndex());

            for (Line edge : shell.createWireframeEdges()) {
                panel.addItem(lineItem(panel, edge, color, 1.6f));
            }
        }
    }

    /**
     * Add all synthetic wires, emphasizing fake-hit wires in red.
     *
     * @param panel 3D panel receiving the wire items
     */
    private void addWires(Panel3D panel) {
        Color normalWire = new Color(40, 120, 220, 135);
        Color hitWire = new Color(220, 30, 30, 255);

        for (Wire3D wire : model.getWires()) {
            Color color = wire.isFakeHit() ? hitWire : normalWire;
            float width = wire.isFakeHit() ? 2.6f : 0.8f;
            panel.addItem(lineItem(panel, wire.getLine(), color, width));
        }
    }

    /**
     * Add a translucent constant-phi slice plane.
     *
     * <p>
     * The plane is drawn as a rectangular quad covering the visible detector
     * envelope. It is not meant to be an infinite mathematical plane; it is a
     * visual cue that corresponds to the current slice shown by the 2D view.
     * </p>
     *
     * @param panel 3D panel receiving the slice plane
     * @param phiDeg current phi angle in degrees
     */
    private void addSlicePlane(Panel3D panel, double phiDeg) {
    	double rMin = 35.0;
    	double rMax = SCENE_XY_EXTENT - 10.0;
    	double zMin = 35.0;
    	double zMax = SCENE_Z_EXTENT - 30.0;
    	
        edu.cnu.mdi.geometry.Point p1 = SliceProjection.cylindrical(rMin, phiDeg, zMin);
        edu.cnu.mdi.geometry.Point p2 = SliceProjection.cylindrical(rMax, phiDeg, zMin);
        edu.cnu.mdi.geometry.Point p3 = SliceProjection.cylindrical(rMax, phiDeg, zMax);
        edu.cnu.mdi.geometry.Point p4 = SliceProjection.cylindrical(rMin, phiDeg, zMax);

        float[] coords = new float[] {
                f(p1.x), f(p1.y), f(p1.z),
                f(p2.x), f(p2.y), f(p2.z),
                f(p3.x), f(p3.y), f(p3.z),
                f(p4.x), f(p4.y), f(p4.z)
        };

        Quad3D plane = new Quad3D(
                panel,
                coords,
                new Color(255, 210, 60, 70),
                1.4f,
                true);

        panel.addItem(plane);
    }

    /**
     * Create a {@link Line3D} from an MDI geometry {@link Line}.
     *
     * @param panel owner panel
     * @param line source geometry line
     * @param color line color
     * @param width line width
     * @return 3D line item
     */
    private static Line3D lineItem(
            Panel3D panel, Line line, Color color, float width) {

        edu.cnu.mdi.geometry.Point p0 = line.getP0();
        edu.cnu.mdi.geometry.Point p1 = line.getP1();

        return new Line3D(
                panel,
                f(p0.x), f(p0.y), f(p0.z),
                f(p1.x), f(p1.y), f(p1.z),
                color,
                width);
    }

    /**
     * Return a distinct but subdued shell color for a region index.
     *
     * @param regionIndex zero-based region index
     * @return shell wireframe color
     */
    private static Color shellColor(int regionIndex) {
        return switch (regionIndex) {
        case 0 -> new Color(40, 40, 40, 210);
        case 1 -> new Color(70, 70, 70, 210);
        default -> new Color(100, 100, 100, 210);
        };
    }

    /**
     * Convert a double coordinate to float for the 3D item classes.
     *
     * @param v double value
     * @return float value
     */
    private static float f(double v) {
        return (float) v;
    }
}
