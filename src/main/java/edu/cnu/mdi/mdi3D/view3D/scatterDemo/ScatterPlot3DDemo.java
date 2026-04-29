package edu.cnu.mdi.mdi3D.view3D.scatterDemo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import edu.cnu.mdi.mdi3D.item3D.ScatterPlot3D;
import edu.cnu.mdi.mdi3D.item3D.ScatterPlot3D.RenderStyle;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.view3D.PlainView3D;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.ViewPropertiesBuilder;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * Demonstration of {@link ScatterPlot3D}.
 *
 * <p>Starts with a deterministic, surface-like data set colored with the MDI
 * Viridis scientific color map, then optionally feeds random data from a
 * background producer thread. "Trickle" adds 5 points/sec; "Storm" adds
 * ~5,000 points/sec to exercise the coalesced repaint throttle.</p>
 *
 * <h2>Correct PlainView3D subclassing pattern</h2>
 * <ul>
 *   <li>Implement the one abstract method: {@link #make3DPanel}.</li>
 *   <li>Override {@link Panel3D#createInitialItems()} inside the anonymous
 *       Panel3D to populate the scene.</li>
 *   <li>Build any extra UI (e.g. a control strip) and add it to {@code this}
 *       (the view is a JPanel) in the constructor.</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class ScatterPlot3DDemo extends PlainView3D {

    // -----------------------------------------------------------------------
    // Camera / window defaults
    // -----------------------------------------------------------------------

    private static final String TITLE   = "ScatterPlot3D Demo";
    private static final float  ANGLE_X = -25f;
    private static final float  ANGLE_Y =  30f;
    private static final float  ANGLE_Z =   0f;
    private static final float  DIST_X  =   0f;
    private static final float  DIST_Y  =   0f;
    private static final float  DIST_Z  =  -5f;

    // -----------------------------------------------------------------------
    // Scene and producer state
    // -----------------------------------------------------------------------

    /** Set inside createInitialItems(); safe to read on EDT after that. */
    private ScatterPlot3D _scatter;

    private final ScheduledExecutorService _producer =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ScatterPlot3D-producer");
            t.setDaemon(true);
            return t;
        });

    private ScheduledFuture<?> _producerTask;
    private final AtomicLong   _totalAdded   = new AtomicLong(0);
    private JLabel             _statusLabel;   // set in buildControlPanel()

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /** Default constructor — uses the canonical camera angles and title. */
    public ScatterPlot3DDemo() {
        this(createDefaultProperties());
    }

    /**
     * Properties constructor.  The super call builds the 3D panel via
     * {@link #make3DPanel}; we then add our control strip to SOUTH.
     */
    public ScatterPlot3DDemo(Properties props) {
        super(props);
        add(buildControlPanel(), BorderLayout.SOUTH);
    }

    private static Properties createDefaultProperties() {
        return new ViewPropertiesBuilder()
                .title(TITLE)
                .put(PropertyUtils.ANGLE_X, ANGLE_X)
                .put(PropertyUtils.ANGLE_Y, ANGLE_Y)
                .put(PropertyUtils.ANGLE_Z, ANGLE_Z)
                .put(PropertyUtils.DIST_X,  DIST_X)
                .put(PropertyUtils.DIST_Y,  DIST_Y)
                .put(PropertyUtils.DIST_Z,  DIST_Z)
                .fraction(0.85)
                .aspect(1.0)
                .useContainer(false)
                .build();
    }

    // -----------------------------------------------------------------------
    // PlainView3D contract — the ONE abstract method we must implement
    // -----------------------------------------------------------------------

    /**
     * Called by {@link PlainView3D}'s constructor to create the GL panel.
     * Items are added by overriding {@link Panel3D#createInitialItems()}.
     */
    @Override
    protected Panel3D make3DPanel(float angleX, float angleY, float angleZ,
                                   float xDist,  float yDist,  float zDist) {
        return new Panel3D(angleX, angleY, angleZ, xDist, yDist, zDist) {
            @Override
            public void createInitialItems() {
                // Initial bounding box: ±1.5 on all axes
                _scatter = new ScatterPlot3D(this, 1.5f);
                _scatter.setPointRadius(0.035f);
                _scatter.setPointSize(8f);
                _scatter.setResolution(12, 8);
                _scatter.setThrottleMs(50);     // cap repaints at ~20 fps
                _scatter.setColorMap(ScientificColorMap.VIRIDIS);
                _scatter.setRenderStyle(RenderStyle.AUTO);
                _scatter.setShowFloorGrid(true);
                addItem(_scatter);

                // Let the scatter plot own its plot annotations. This avoids
                // duplicate text and keeps the floor grid, bounding box, axis
                // titles, and tick labels synchronized with the scatter bounds.
                _scatter.setAxisLabels("Diameter", "Aspect", "Freq");
                _scatter.setAxisTickCount(4);
                _scatter.setShowAxisText(true);

                // Panel3D currently calls createInitialItems() from its constructor.
                // At that point ScatterPlot3DDemo field initializers, such as
                // _totalAdded, have not run yet. Defer data loading until the
                // enclosing view is fully constructed.
                SwingUtilities.invokeLater(() -> {
                    if (_scatter != null) {
                        loadSurfaceData();
                    }
                });
            }
        };
    }

    // -----------------------------------------------------------------------
    // Control strip
    // -----------------------------------------------------------------------

    private JPanel buildControlPanel() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JButton surface = new JButton("Surface Demo");
        surface.addActionListener(e -> {
            stopProducer();
            loadSurfaceData();
        });
        bar.add(surface);

        // --- Trickle: 1 pt every 200 ms ---
        JButton trickle = new JButton("Trickle (5/s)");
        trickle.addActionListener(e -> startProducer(200, 1));
        bar.add(trickle);

        // --- Storm: 100 pts every 20 ms ≈ 5,000/s ---
        JButton storm = new JButton("Storm (5000/s)");
        storm.addActionListener(e -> startProducer(20, 100));
        bar.add(storm);

        JButton stop = new JButton("Stop");
        stop.addActionListener(e -> stopProducer());
        bar.add(stop);

        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> {
            stopProducer();
            _totalAdded.set(0);
            if (_scatter != null) {
                _scatter.clear(-1.5f, 1.5f, -1.5f, 1.5f, -1.5f, 1.5f);
            }
            updateStatus();
        });
        bar.add(clear);

        bar.add(new JLabel("  Map:"));
        JComboBox<ScientificColorMap> maps = new JComboBox<>(ScientificColorMap.values());
        maps.setSelectedItem(ScientificColorMap.VIRIDIS);
        maps.addActionListener(e -> {
            if (_scatter != null) {
                _scatter.setColorMap((ScientificColorMap) maps.getSelectedItem());
            }
        });
        bar.add(maps);

        bar.add(new JLabel("  Style:"));
        JComboBox<RenderStyle> styles = new JComboBox<>(RenderStyle.values());
        styles.setSelectedItem(RenderStyle.AUTO);
        styles.addActionListener(e -> {
            if (_scatter != null) {
                _scatter.setRenderStyle((RenderStyle) styles.getSelectedItem());
            }
        });
        bar.add(styles);

        bar.add(new JLabel("  Throttle ms:"));
        JSlider throttle = new JSlider(0, 200, 50);
        throttle.setMajorTickSpacing(50);
        throttle.setPaintTicks(true);
        throttle.setPaintLabels(true);
        throttle.setToolTipText("Min ms between repaints — 0 = unlimited");
        throttle.addChangeListener(e -> {
            if (_scatter != null) _scatter.setThrottleMs(throttle.getValue());
        });
        bar.add(throttle);

        _statusLabel = new JLabel("  0 points");
        bar.add(_statusLabel);

        // Tick the status label every 500 ms on the EDT
        new Timer(500, e -> updateStatus()).start();

        return bar;
    }

    /**
     * Load a deterministic, surface-like sample data set reminiscent of the
     * scientific scatter plots used in papers and presentations.
     */
    private void loadSurfaceData() {
        _totalAdded.set(0);
        if (_scatter == null) {
            return;
        }
        _scatter.clear(-1.5f, 1.5f, -1.5f, 1.5f, -1.5f, 1.5f);

        final int nd = 24;
        final int na = 18;
        for (int i = 0; i < nd; i++) {
            double diameter = 3.0 + i * (15.0 / (nd - 1));
            for (int j = 0; j < na; j++) {
                double aspect = 2.0 + j * (8.0 / (na - 1));

                double ridge = 0.58 + 0.42 * Math.sin(0.72 * aspect + 0.18 * diameter);
                double decay = Math.exp(-0.070 * (diameter - 3.0));
                double lobe  = 0.25 * Math.exp(-Math.pow(aspect - 5.4, 2) / 5.5);
                double freq01 = Math.max(0.0, Math.min(1.0, ridge * decay + lobe));

                float x = (float) (-1.5 + 3.0 * (diameter - 3.0) / 15.0);
                float y = (float) (-1.5 + 3.0 * (aspect - 2.0) / 8.0);
                float z = (float) (-1.5 + 3.0 * freq01);

                _scatter.add(x, y, z, (float) freq01);
                _totalAdded.incrementAndGet();
            }
        }
        updateStatus();
    }

    // -----------------------------------------------------------------------
    // Producer helpers
    // -----------------------------------------------------------------------

    private void startProducer(int intervalMs, int batchSize) {
        stopProducer();
        Random rng = new Random();
        _producerTask = _producer.scheduleAtFixedRate(() -> {
            for (int i = 0; i < batchSize; i++) {
                // Gaussian cluster centred at origin, sigma ≈ 0.5
                float x = (float) (rng.nextGaussian() * 0.5);
                float y = (float) (rng.nextGaussian() * 0.5);
                float z = (float) (rng.nextGaussian() * 0.5);

                // Heat-map value: distance from origin, normalised to [0, 1]
                double dist  = Math.sqrt(x * x + y * y + z * z);
                float  value = (float) Math.min(1.0, dist / 1.5);

                if (_scatter != null) {
                    _scatter.add(x, y, z, value);
                }
                _totalAdded.incrementAndGet();
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void stopProducer() {
        if (_producerTask != null) {
            _producerTask.cancel(false);
            _producerTask = null;
        }
    }

    private void updateStatus() {
        if (_statusLabel == null) return;
        int rendered = (_scatter != null) ? _scatter.getPointCount() : 0;
        _statusLabel.setText(String.format(
            "  %,d added  |  %,d rendered",
            _totalAdded.get(), rendered));
    }

    // -----------------------------------------------------------------------
    // Standalone entry point (for quick testing outside MDI)
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(TITLE);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new ScatterPlot3DDemo());
            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
