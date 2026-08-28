package edu.cnu.mdi.mdi3D.view3D.scatterDemo;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import edu.cnu.mdi.ui.colors.ScientificColorMap;
import edu.cnu.mdi.ui.fonts.Fonts;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.ViewPropertiesBuilder;

/**
 * Demonstration of {@link ScatterPlot3D}.
 *
 * <p>
 * Starts with a deterministic, surface-like data set colored with the MDI
 * Viridis scientific color map, then optionally feeds random data from a
 * background producer thread. "Trickle" adds 5 points/sec; "Storm" adds
 * approximately 5,000 points/sec to exercise the coalesced repaint throttle.
 * </p>
 *
 * <h2>Correct PlainView3D subclassing pattern</h2>
 * <ul>
 * <li>Implement the one abstract method: {@link #make3DPanel}.</li>
 * <li>Override {@link Panel3D#createInitialItems()} inside the anonymous
 * Panel3D to populate the scene.</li>
 * <li>Build any extra UI, such as a control strip, and add it to
 * {@code this} in the constructor.</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class ScatterPlot3DDemo extends PlainView3D {

    // -----------------------------------------------------------------------
    // Camera / window defaults
    // -----------------------------------------------------------------------

    private static final String TITLE = "ScatterPlot3D Demo";

    private static final float ANGLE_X = -25f;
    private static final float ANGLE_Y = 30f;
    private static final float ANGLE_Z = 0f;

    private static final float DIST_X = 0f;
    private static final float DIST_Y = 0f;
    private static final float DIST_Z = -7f;

    // -----------------------------------------------------------------------
    // Scene and producer state
    // -----------------------------------------------------------------------

    /**
     * Set inside createInitialItems(); safe to read on the EDT after that.
     */
    private ScatterPlot3D _scatter;

    private final ScheduledExecutorService _producer =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ScatterPlot3D-producer");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> _producerTask;

    private final AtomicLong _totalAdded = new AtomicLong(0);

    /**
     * Status label created by {@link #buildControlPanel()}.
     */
    private JLabel _statusLabel;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Default constructor using the canonical camera angles and title.
     */
    public ScatterPlot3DDemo() {
        this(createDefaultProperties());
    }

    /**
     * Constructs the demo from the supplied view properties.
     *
     * @param props the view properties
     */
    public ScatterPlot3DDemo(Properties props) {
        super(props);

        /*
         * Use an explicitly multi-row control panel rather than relying on a
         * wrapping FlowLayout. A standard FlowLayout does not report a
         * preferred height that accounts for wrapping at the current width,
         * which can cause a BorderLayout.SOUTH component to have its lower
         * row clipped.
         */
        add(buildControlPanel(), BorderLayout.SOUTH);
    }

    @Override
    public AbstractViewInfo getViewInfo() {
        return new ScatterPlot3DDemoInfo();
    }

    /**
     * Creates the default view properties.
     *
     * @return the default properties
     */
    private static Properties createDefaultProperties() {
        return new ViewPropertiesBuilder()
                .title(TITLE)
                .put(PropertyUtils.ANGLE_X, ANGLE_X)
                .put(PropertyUtils.ANGLE_Y, ANGLE_Y)
                .put(PropertyUtils.ANGLE_Z, ANGLE_Z)
                .put(PropertyUtils.DIST_X, DIST_X)
                .put(PropertyUtils.DIST_Y, DIST_Y)
                .put(PropertyUtils.DIST_Z, DIST_Z)
                .fraction(0.8)
                .aspect(1.0)
                .useContainer(false)
                .build();
    }

    // -----------------------------------------------------------------------
    // PlainView3D contract
    // -----------------------------------------------------------------------

    /**
     * Creates the 3D panel used by this view.
     *
     * <p>
     * The initial scatter plot is installed by overriding
     * {@link Panel3D#createInitialItems()} in the returned anonymous
     * {@link Panel3D}.
     * </p>
     *
     * @param angleX initial x rotation
     * @param angleY initial y rotation
     * @param angleZ initial z rotation
     * @param xDist initial x translation
     * @param yDist initial y translation
     * @param zDist initial z translation
     *
     * @return the 3D panel
     */
    @Override
    protected Panel3D make3DPanel(float angleX, float angleY, float angleZ,
            float xDist, float yDist, float zDist) {

        return new Panel3D(angleX, angleY, angleZ,
                xDist, yDist, zDist) {

            @Override
            public void createInitialItems() {

                // Initial bounding box: +/-1.5 on all axes.
                _scatter = new ScatterPlot3D(this, 1.5f);

                _scatter.setPointRadius(0.035f);
                _scatter.setPointSize(8f);
                _scatter.setResolution(12, 8);
                _scatter.setThrottleMs(50);
                _scatter.setColorMap(ScientificColorMap.VIRIDIS);
                _scatter.setRenderStyle(RenderStyle.AUTO);
                _scatter.setShowFloorGrid(true);

                addItem(_scatter);

                /*
                 * Let the scatter plot own its plot annotations. This keeps
                 * the floor grid, bounding box, axis titles, and tick labels
                 * synchronized with the scatter bounds.
                 */
                _scatter.setAxisLabels(
                        "Diameter", "Aspect", "Freq");
                _scatter.setAxisTickCount(4);
                _scatter.setShowAxisText(true);

                /*
                 * Panel3D currently calls createInitialItems() from its
                 * constructor. At that point ScatterPlot3DDemo field
                 * initializers may not yet have run, so defer loading the
                 * sample data until construction of the enclosing view has
                 * completed.
                 */
                SwingUtilities.invokeLater(() -> {
                    if (_scatter != null) {
                        loadSurfaceData();
                    }
                });
            }
        };
    }

    // -----------------------------------------------------------------------
    // Small UI helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a button with the specified text, font, and action.
     *
     * @param text button text
     * @param font button font
     * @param action action to run when pressed
     *
     * @return the configured button
     */
    private JButton makeButton(
            String text, Font font, Runnable action) {

        JButton button = new JButton(text);
        button.setFont(font);
        button.addActionListener(e -> action.run());
        return button;
    }

    /**
     * Creates a label using the specified font.
     *
     * @param text label text
     * @param font label font
     *
     * @return the configured label
     */
    private JLabel makeLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        return label;
    }

    // -----------------------------------------------------------------------
    // Control panel
    // -----------------------------------------------------------------------

    /**
     * Builds the controls displayed beneath the 3D panel.
     *
     * <p>
     * The controls are deliberately divided into two fixed rows. This avoids
     * the preferred-size problem associated with allowing a single
     * {@link FlowLayout} to wrap inside a {@link BorderLayout#SOUTH} region.
     * </p>
     *
     * @return the complete control panel
     */
    private JPanel buildControlPanel() {

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(
                new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        JPanel topRow =
                new JPanel(new FlowLayout(
                        FlowLayout.LEFT, 6, 4));

        JPanel bottomRow =
                new JPanel(new FlowLayout(
                        FlowLayout.LEFT, 6, 4));

        Font font = Fonts.plainFontDelta(-2);

        // ---------------------------------------------------------------
        // Top row
        // ---------------------------------------------------------------

        JButton surface = makeButton(
                "Surface", font, () -> {
                    stopProducer();
                    loadSurfaceData();
                });
        topRow.add(surface);

        JButton trickle = makeButton(
                "Trickle (5/s)", font,
                () -> startProducer(200, 1));
        topRow.add(trickle);

        JButton storm = makeButton(
                "Storm (5k/s)", font,
                () -> startProducer(20, 100));
        topRow.add(storm);

        JButton stop =
                makeButton("Stop", font, this::stopProducer);
        topRow.add(stop);

        JButton clear = makeButton(
                "Clear", font, () -> {
                    stopProducer();
                    _totalAdded.set(0);

                    if (_scatter != null) {
                        _scatter.clear(
                                -1.5f, 1.5f,
                                -1.5f, 1.5f,
                                -1.5f, 1.5f);
                    }

                    updateStatus();
                });
        topRow.add(clear);

        topRow.add(makeLabel("  Map:", font));

        JComboBox<ScientificColorMap> maps =
                new JComboBox<>(
                        ScientificColorMap.values());

        maps.setFont(font);
        maps.setSelectedItem(
                ScientificColorMap.VIRIDIS);

        maps.addActionListener(e -> {
            if (_scatter != null) {
                _scatter.setColorMap(
                        (ScientificColorMap)
                                maps.getSelectedItem());
            }
        });

        topRow.add(maps);

        topRow.add(makeLabel("  Style:", font));

        JComboBox<RenderStyle> styles =
                new JComboBox<>(RenderStyle.values());

        styles.setFont(font);
        styles.setSelectedItem(RenderStyle.AUTO);

        styles.addActionListener(e -> {
            if (_scatter != null) {
                _scatter.setRenderStyle(
                        (RenderStyle)
                                styles.getSelectedItem());
            }
        });

        topRow.add(styles);

        // ---------------------------------------------------------------
        // Bottom row
        // ---------------------------------------------------------------

        bottomRow.add(
                makeLabel("  Throttle ms:", font));

        JSlider throttle =
                new JSlider(0, 200, 50);

        throttle.setMajorTickSpacing(50);
        throttle.setPaintTicks(true);
        throttle.setPaintLabels(true);
        throttle.setToolTipText(
                "Min ms between repaints — 0 = unlimited");

        throttle.addChangeListener(e -> {
            if (_scatter != null) {
                _scatter.setThrottleMs(
                        throttle.getValue());
            }
        });

        bottomRow.add(throttle);

        _statusLabel =
                makeLabel("  0 points", font);
        bottomRow.add(_statusLabel);

        controlPanel.add(topRow);
        controlPanel.add(bottomRow);

        /*
         * Update the status display every 500 ms on the EDT.
         */
        new Timer(500, e -> updateStatus()).start();

        return controlPanel;
    }

    // -----------------------------------------------------------------------
    // Sample data
    // -----------------------------------------------------------------------

    /**
     * Loads a deterministic, surface-like sample data set reminiscent of
     * scientific scatter plots used in papers and presentations.
     */
    private void loadSurfaceData() {

        _totalAdded.set(0);

        if (_scatter == null) {
            return;
        }

        _scatter.clear(
                -1.5f, 1.5f,
                -1.5f, 1.5f,
                -1.5f, 1.5f);

        final int nd = 24;
        final int na = 18;

        for (int i = 0; i < nd; i++) {

            double diameter =
                    3.0 + i * (15.0 / (nd - 1));

            for (int j = 0; j < na; j++) {

                double aspect =
                        2.0 + j * (8.0 / (na - 1));

                double ridge =
                        0.58
                        + 0.42 * Math.sin(
                                0.72 * aspect
                                + 0.18 * diameter);

                double decay =
                        Math.exp(
                                -0.070
                                * (diameter - 3.0));

                double lobe =
                        0.25 * Math.exp(
                                -Math.pow(
                                        aspect - 5.4, 2)
                                / 5.5);

                double freq01 =
                        Math.max(
                                0.0,
                                Math.min(
                                        1.0,
                                        ridge * decay
                                        + lobe));

                float x = (float)
                        (-1.5
                        + 3.0
                        * (diameter - 3.0)
                        / 15.0);

                float y = (float)
                        (-1.5
                        + 3.0
                        * (aspect - 2.0)
                        / 8.0);

                float z = (float)
                        (-1.5
                        + 3.0 * freq01);

                _scatter.add(
                        x, y, z,
                        (float) freq01);

                _totalAdded.incrementAndGet();
            }
        }

        updateStatus();
    }

    // -----------------------------------------------------------------------
    // Producer helpers
    // -----------------------------------------------------------------------

    /**
     * Starts the background random-point producer.
     *
     * @param intervalMs producer interval in milliseconds
     * @param batchSize number of points produced per interval
     */
    private void startProducer(
            int intervalMs, int batchSize) {

        stopProducer();

        Random rng = new Random();

        _producerTask =
                _producer.scheduleAtFixedRate(() -> {

                    for (int i = 0;
                            i < batchSize; i++) {

                        /*
                         * Gaussian cluster centered at the origin,
                         * sigma approximately 0.5.
                         */
                        float x = (float)
                                (rng.nextGaussian() * 0.5);

                        float y = (float)
                                (rng.nextGaussian() * 0.5);

                        float z = (float)
                                (rng.nextGaussian() * 0.5);

                        /*
                         * Heat-map value: distance from the origin,
                         * normalized to [0, 1].
                         */
                        double dist =
                                Math.sqrt(
                                        x * x
                                        + y * y
                                        + z * z);

                        float value =
                                (float)
                                Math.min(
                                        1.0,
                                        dist / 1.5);

                        if (_scatter != null) {
                            _scatter.add(
                                    x, y, z, value);
                        }

                        _totalAdded.incrementAndGet();
                    }

                }, 0, intervalMs,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the active background producer, if any.
     */
    private void stopProducer() {

        if (_producerTask != null) {
            _producerTask.cancel(false);
            _producerTask = null;
        }
    }

    /**
     * Updates the point-count status label.
     */
    private void updateStatus() {

        if (_statusLabel == null) {
            return;
        }

        int rendered =
                (_scatter != null)
                ? _scatter.getPointCount()
                : 0;

        _statusLabel.setText(
                String.format(
                        "  %,d added  |  %,d rendered",
                        _totalAdded.get(),
                        rendered));
    }

}