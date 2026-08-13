package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.SwingUtilities;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;
import edu.cnu.mdi.mdi3D.panel.TextRendering3D;
import edu.cnu.mdi.mdi3D.panel.TextRendering3D.Anchor;
import edu.cnu.mdi.ui.colors.ScientificColorMap;

/**
 * A thread-safe 3D scatter plot for use on a {@link Panel3D}.
 *
 * <h2>Overview</h2>
 * <p>
 * {@code ScatterPlot3D} renders a collection of colored spheres as a scatter
 * plot inside a dynamic wireframe bounding box.  The bounding box starts at
 * a caller-supplied initial size and grows automatically as new data points
 * are added.
 * </p>
 *
 * <h2>Threading</h2>
 * <p>
 * {@link #add(float, float, float, float)} and
 * {@link #add(float, float, float, Color)} are safe to call from <em>any</em>
 * thread, including high-frequency producer threads.  Each call atomically
 * expands the bounding-box bounds and enqueues the new sphere.  A repaint
 * is then requested on the EDT via a coalescing gate: no matter how many
 * {@code add()} calls arrive between two EDT ticks, at most one
 * {@code repaint()} is scheduled.  This prevents EDT flooding during a
 * data storm while ensuring the display stays live.
 * </p>
 *
 * <h2>Throttling</h2>
 * <p>
 * An optional minimum repaint interval ({@link #setThrottleMs(long)},
 * default 50 ms ≈ 20 fps) further limits refresh rate under extreme load.
 * Set to 0 to repaint as fast as the EDT can process.
 * </p>
 *
 * <h2>Color Mapping</h2>
 * <p>
 * Use {@link #add(float, float, float, float)} with a scalar value in
 * [0, 1] to get automatic scientific coloring.  The default map is
 * {@link ScientificColorMap#VIRIDIS}, but any MDI scientific color map can
 * be selected with {@link #setColorMap(ScientificColorMap)}.  You can also
 * supply an explicit {@link Color} via {@link #add(float, float, float, Color)}.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create the plot and add it to the panel
 * ScatterPlot3D scatter = new ScatterPlot3D(panel,
 *         -1f, 1f,   // X range
 *         -1f, 1f,   // Y range
 *         -1f, 1f);  // Z range
 * panel.addItem(scatter);
 * panel.softRefresh();
 *
 * // From any thread — safe during a data storm
 * scatter.add(x, y, z, normalizedValue);   // 0..1 → heat-map color
 * scatter.add(x, y, z, Color.CYAN);        // explicit color
 *
 * // Tune the refresh rate
 * scatter.setThrottleMs(16);   // ~60 fps cap
 * scatter.setPointRadius(0.02f);
 * }</pre>
 */
public class ScatterPlot3D extends Item3D {

    // -----------------------------------------------------------------------
    // Defaults
    // -----------------------------------------------------------------------

    /** Default minimum milliseconds between scheduled repaints. */
    private static final long DEFAULT_THROTTLE_MS = 50L;

    /** Default sphere radius for each data point. */
    private static final float DEFAULT_POINT_RADIUS = 0.025f;

    /** Default point-sprite size, in screen pixels. */
    private static final float DEFAULT_POINT_SIZE = 7f;

    /** Default number of grid divisions on the floor plane. */
    private static final int DEFAULT_GRID_DIVISIONS = 10;

    /** Default sphere resolution (longitude slices). */
    private static final int DEFAULT_SLICES = 12;

    /** Default sphere resolution (latitude stacks). */
    private static final int DEFAULT_STACKS = 8;

    /** Wireframe box line width. */
    private static final float BOX_LINE_WIDTH = 1.5f;

    /** Default wireframe box color. */
    private static final Color BOX_COLOR = new Color(170, 170, 170, 190);

    /** Default floor-grid color. */
    private static final Color GRID_COLOR = new Color(215, 215, 215, 120);

    /** Rendering styles for the data markers. */
    public enum RenderStyle {
        /** Render every marker as a lit solid sphere.  Best for modest data sets. */
        SPHERES,

        /** Render markers as OpenGL point sprites.  Best for large data sets. */
        POINTS,

        /** Use spheres for small data sets and points after {@link #setAutoPointThreshold(int)}. */
        AUTO
    }

    // -----------------------------------------------------------------------
    // Bounding-box bounds — guarded by _boundsLock
    // -----------------------------------------------------------------------

    private final Object _boundsLock = new Object();
    private float _minX, _maxX;
    private float _minY, _maxY;
    private float _minZ, _maxZ;

    // -----------------------------------------------------------------------
    // Point data — guarded by _pointsLock
    // -----------------------------------------------------------------------

    /** Read-write lock protecting _pendingPoints. */
    private final ReentrantReadWriteLock _pointsLock = new ReentrantReadWriteLock();

    /**
     * Points waiting to be moved into _committedPoints on the next EDT tick.
     * Written by producer threads, drained on the EDT.
     */
    private final List<ScatterPoint> _pendingPoints = new ArrayList<>();

    /**
     * The authoritative list of points drawn each frame.
     * Only ever accessed on the EDT (during draw and during drain).
     */
    private final List<ScatterPoint> _committedPoints = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Repaint coalescing
    // -----------------------------------------------------------------------

    /** True when a repaint has been scheduled but not yet executed. */
    private final AtomicBoolean _repaintPending = new AtomicBoolean(false);

    /** Minimum ms between repaints (throttle). 0 = unlimited. */
    private volatile long _throttleMs = DEFAULT_THROTTLE_MS;

    /** Wall-clock time of the last scheduled repaint. */
    private volatile long _lastRepaintScheduledMs = 0L;

    // -----------------------------------------------------------------------
    // Rendering parameters
    // -----------------------------------------------------------------------

    private volatile float _pointRadius = DEFAULT_POINT_RADIUS;
    private volatile int   _slices      = DEFAULT_SLICES;
    private volatile int   _stacks      = DEFAULT_STACKS;
    private volatile Color _boxColor    = BOX_COLOR;
    private volatile Color _gridColor   = GRID_COLOR;
    private volatile boolean _showBox   = true;
    private volatile boolean _showFloorGrid = true;
    private volatile float _pointSize = DEFAULT_POINT_SIZE;
    private volatile int _gridDivisions = DEFAULT_GRID_DIVISIONS;
    private volatile int _autoPointThreshold = 750;
    private volatile RenderStyle _renderStyle = RenderStyle.AUTO;
    private volatile ScientificColorMap _colorMap = ScientificColorMap.VIRIDIS;

    // -----------------------------------------------------------------------
    // Axis text
    // -----------------------------------------------------------------------

    private volatile boolean _showAxisText = true;
    private volatile String _xAxisLabel = "X";
    private volatile String _yAxisLabel = "Y";
    private volatile String _zAxisLabel = "Z";
    private volatile int _axisTickCount = 4;

    private transient TextRendering3D _axisTitleRenderer;
    private transient TextRendering3D _tickLabelRenderer;

    // -----------------------------------------------------------------------
    // Bounds-change notification
    // -----------------------------------------------------------------------

    /**
     * Callback fired on the EDT whenever the bounding box grows.
     * Receives the new bounds as {@code [minX, maxX, minY, maxY, minZ, maxZ]}.
     *
     * <pre>{@code
     * scatter.setBoundsListener(b ->
     *     rebuildAxes(b[0], b[1], b[2], b[3], b[4], b[5]));
     * }</pre>
     */
    @FunctionalInterface
    public interface BoundsListener {
        void boundsChanged(float[] bounds);
    }

    /** The registered listener, or null. Volatile — written once, read often. */
    private volatile BoundsListener _boundsListener = null;

    /**
     * True when a bounds-change notification is already queued on the EDT.
     * Coalesces multiple expansions between EDT ticks into one callback.
     */
    private final AtomicBoolean _boundsNotifyPending = new AtomicBoolean(false);

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Create a scatter plot with an explicit initial bounding box.
     * The box will grow automatically as points outside it are added.
     *
     * @param panel3D  the owner Panel3D
     * @param minX     initial minimum X bound
     * @param maxX     initial maximum X bound
     * @param minY     initial minimum Y bound
     * @param maxY     initial maximum Y bound
     * @param minZ     initial minimum Z bound
     * @param maxZ     initial maximum Z bound
     */
    public ScatterPlot3D(Panel3D panel3D,
                         float minX, float maxX,
                         float minY, float maxY,
                         float minZ, float maxZ) {
        super(panel3D);
        _minX = minX; _maxX = maxX;
        _minY = minY; _maxY = maxY;
        _minZ = minZ; _maxZ = maxZ;
    }

    /**
     * Create a scatter plot with a symmetric cubic initial bounding box
     * centred at the origin.
     *
     * @param panel3D    the owner Panel3D
     * @param halfExtent half-length of each side (box runs from -halfExtent
     *                   to +halfExtent on every axis)
     */
    public ScatterPlot3D(Panel3D panel3D, float halfExtent) {
        this(panel3D,
             -halfExtent, halfExtent,
             -halfExtent, halfExtent,
             -halfExtent, halfExtent);
    }

    // -----------------------------------------------------------------------
    // Public add API — call from any thread
    // -----------------------------------------------------------------------

    /**
     * Add a data point using a scalar value mapped through the current
     * {@link ScientificColorMap}.
     *
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param z     Z coordinate
     * @param value scalar in [0, 1]; values outside the range are clamped
     */
    public void add(float x, float y, float z, float value) {
        addMapped(x, y, z, Math.max(0f, Math.min(1f, value)));
    }

    /** Add a scalar-mapped point, preserving the scalar for later recoloring. */
    private void addMapped(float x, float y, float z, float value01) {
        boolean expanded = expandBounds(x, y, z);

        _pointsLock.writeLock().lock();
        try {
            _pendingPoints.add(new ScatterPoint(x, y, z, value01, _colorMap.colorAt(value01)));
        } finally {
            _pointsLock.writeLock().unlock();
        }

        scheduleRepaintIfNeeded();

        if (expanded && _boundsListener != null) {
            scheduleBoundsNotifyIfNeeded();
        }
    }

    /**
     * Add a data point with an explicit color.
     *
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param z     Z coordinate
     * @param color the sphere color
     */
    public void add(float x, float y, float z, Color color) {
        // 1. Expand bounds atomically; track whether they actually changed
        boolean expanded = expandBounds(x, y, z);

        // 2. Enqueue the point for the next EDT drain
        _pointsLock.writeLock().lock();
        try {
            _pendingPoints.add(new ScatterPoint(x, y, z, color));
        } finally {
            _pointsLock.writeLock().unlock();
        }

        // 3. Coalesced, throttled repaint request
        scheduleRepaintIfNeeded();

        // 4. If bounds grew and a listener is registered, notify it (coalesced)
        if (expanded && _boundsListener != null) {
            scheduleBoundsNotifyIfNeeded();
        }
    }

    /**
     * Remove all data points and reset the bounding box to its initial size.
     * Safe to call from any thread.
     */
    public void clear(float minX, float maxX,
                      float minY, float maxY,
                      float minZ, float maxZ) {
        synchronized (_boundsLock) {
            _minX = minX; _maxX = maxX;
            _minY = minY; _maxY = maxY;
            _minZ = minZ; _maxZ = maxZ;
        }
        _pointsLock.writeLock().lock();
        try {
            _pendingPoints.clear();
        } finally {
            _pointsLock.writeLock().unlock();
        }

        Runnable finishOnEdt = () -> {
            _committedPoints.clear();
            BoundsListener listener = _boundsListener;
            if (listener != null) {
                listener.boundsChanged(new float[]{ minX, maxX, minY, maxY, minZ, maxZ });
            }
            _panel3D.softRefresh();
        };

        // _committedPoints is EDT-only (see field doc). Unconditionally
        // deferring this via invokeLater — even when clear() is itself called
        // on the EDT — opened a race: a caller that clears and then
        // immediately re-adds points on the EDT (as loadSurfaceData() does)
        // could have an intervening draw() drain the fresh points into
        // _committedPoints before this deferred callback ran, and the stale
        // clear would then wipe out that brand-new data. Run it inline when
        // we're already on the EDT; only hop for off-EDT callers, which
        // can't touch _committedPoints directly.
        if (SwingUtilities.isEventDispatchThread()) {
            finishOnEdt.run();
        } else {
            SwingUtilities.invokeLater(finishOnEdt);
        }
    }

    // -----------------------------------------------------------------------
    // Configuration — may be called from any thread
    // -----------------------------------------------------------------------

    /**
     * Set the minimum interval between scheduled repaints.
     * Use 0 to remove the throttle entirely.
     *
     * @param ms milliseconds (≥ 0)
     */
    public void setThrottleMs(long ms) {
        _throttleMs = Math.max(0, ms);
    }

    /** Set the radius of each data-point sphere. Default is 0.025. */
    public void setPointRadius(float radius) {
        _pointRadius = radius;
    }

    /**
     * Set the sphere tessellation resolution.
     * Higher values produce smoother spheres at the cost of render time.
     *
     * @param slices longitude divisions (min 3)
     * @param stacks latitude divisions (min 2)
     */
    public void setResolution(int slices, int stacks) {
        _slices = Math.max(3, slices);
        _stacks = Math.max(2, stacks);
    }

    /** Set the wireframe bounding-box color. */
    public void setBoxColor(Color color) {
        _boxColor = color;
    }

    /** Show or hide the wireframe bounding box. */
    public void setShowBox(boolean show) {
        _showBox = show;
    }

    /**
     * Select the scientific color map used by {@link #add(float, float, float, float)}.
     * Passing {@code null} restores {@link ScientificColorMap#VIRIDIS}.
     *
     * @param colorMap the map to use, or {@code null} for Viridis
     */
    public void setColorMap(ScientificColorMap colorMap) {
        _colorMap = (colorMap == null) ? ScientificColorMap.VIRIDIS : colorMap;
        recolorMappedPoints();
        scheduleRepaintIfNeeded();
    }

    /**
     * Select how scatter markers are rendered.  {@link RenderStyle#AUTO} is the
     * default and switches from spheres to fast OpenGL points for large data sets.
     *
     * @param renderStyle the render style, or {@code null} for AUTO
     */
    public void setRenderStyle(RenderStyle renderStyle) {
        _renderStyle = (renderStyle == null) ? RenderStyle.AUTO : renderStyle;
        scheduleRepaintIfNeeded();
    }

    /** Set the OpenGL point size used by {@link RenderStyle#POINTS}. */
    public void setPointSize(float pointSize) {
        _pointSize = Math.max(1f, pointSize);
        scheduleRepaintIfNeeded();
    }

    /** Set the AUTO-mode point/sphere switch threshold. */
    public void setAutoPointThreshold(int threshold) {
        _autoPointThreshold = Math.max(1, threshold);
    }

    /** Show or hide the light floor grid drawn on the lower Z plane. */
    public void setShowFloorGrid(boolean showFloorGrid) {
        _showFloorGrid = showFloorGrid;
        scheduleRepaintIfNeeded();
    }

    /** Set the floor-grid color. */
    public void setGridColor(Color gridColor) {
        _gridColor = (gridColor == null) ? GRID_COLOR : gridColor;
        scheduleRepaintIfNeeded();
    }

    /** Set the number of floor-grid divisions in X and Y. */
    public void setGridDivisions(int gridDivisions) {
        _gridDivisions = Math.max(1, gridDivisions);
        scheduleRepaintIfNeeded();
    }

    /**
     * Set the axis titles drawn by this scatter plot.
     * <p>
     * Axis text is rendered as a screen-space overlay using
     * {@link TextRendering3D}, so it remains crisp and readable as the plot is
     * rotated or zoomed. Passing {@code null} for a label makes that title blank.
     * </p>
     *
     * @param xLabel x-axis title
     * @param yLabel y-axis title
     * @param zLabel z-axis title
     */
    public void setAxisLabels(String xLabel, String yLabel, String zLabel) {
        _xAxisLabel = (xLabel == null) ? "" : xLabel;
        _yAxisLabel = (yLabel == null) ? "" : yLabel;
        _zAxisLabel = (zLabel == null) ? "" : zLabel;
        scheduleRepaintIfNeeded();
    }

    /**
     * Show or hide the scatter plot's built-in axis titles and tick labels.
     *
     * @param showAxisText {@code true} to draw axis text
     */
    public void setShowAxisText(boolean showAxisText) {
        _showAxisText = showAxisText;
        scheduleRepaintIfNeeded();
    }

    /**
     * Set the number of major tick intervals per axis. A value of 4 draws five
     * labels, including both end points.
     *
     * @param axisTickCount number of intervals; values less than zero are treated as zero
     */
    public void setAxisTickCount(int axisTickCount) {
        _axisTickCount = Math.max(0, axisTickCount);
        scheduleRepaintIfNeeded();
    }

    /**
     * Register a listener that is called on the EDT whenever the bounding box
     * expands.  The float array passed to the listener is
     * {@code [minX, maxX, minY, maxY, minZ, maxZ]}.
     * <p>
     * Multiple expansions that arrive between EDT ticks are coalesced into a
     * single callback carrying the latest bounds at that moment.
     * Pass {@code null} to remove the listener.
     * </p>
     *
     * @param listener the bounds listener, or {@code null}
     */
    public void setBoundsListener(BoundsListener listener) {
        _boundsListener = listener;
    }

    /** Return the current committed point count. */
    public int getPointCount() {
        return _committedPoints.size();
    }

    /** Return the committed plus pending point count. */
    public int getTotalPointCount() {
        _pointsLock.readLock().lock();
        try {
            return _committedPoints.size() + _pendingPoints.size();
        } finally {
            _pointsLock.readLock().unlock();
        }
    }

    /** Return a bounds snapshot as {@code [minX, maxX, minY, maxY, minZ, maxZ]}. */
    public float[] getBoundsSnapshot() {
        synchronized (_boundsLock) {
            return new float[]{ _minX, _maxX, _minY, _maxY, _minZ, _maxZ };
        }
    }

    // -----------------------------------------------------------------------
    // Item3D contract — called on the GL render thread / EDT
    // -----------------------------------------------------------------------

    /**
     * Releases the lazily-created axis-title and tick-label {@link
     * TextRendering3D} renderers, which each own a GPU texture atlas via
     * their underlying JOGL {@code TextRenderer}.
     */
    @Override
    protected void dispose(GLAutoDrawable drawable) {
        if (_axisTitleRenderer != null) {
            _axisTitleRenderer.dispose();
            _axisTitleRenderer = null;
        }
        if (_tickLabelRenderer != null) {
            _tickLabelRenderer.dispose();
            _tickLabelRenderer = null;
        }
    }

    @Override
    public void draw(GLAutoDrawable drawable) {
        // Drain any pending points that arrived since the last frame
        drainPending();

        float radius  = _pointRadius;
        int   slices  = _slices;
        int   stacks  = _stacks;

        if (_showFloorGrid) {
            drawFloorGrid(drawable);
        }

        RenderStyle style = effectiveRenderStyle(_committedPoints.size());
        if (style == RenderStyle.POINTS) {
            drawPointSprites(drawable);
        } else {
            // Draw each committed point as a solid sphere.
            for (ScatterPoint pt : _committedPoints) {
                Support3D.solidSphere(drawable, pt.x, pt.y, pt.z, radius, slices, stacks, pt.color);
            }
        }

        // Draw the wireframe bounding box after the data so it remains visible.
        if (_showBox) {
            drawBoundingBox(drawable);
        }

        // Draw screen-space axis text last so labels stay readable.
        if (_showAxisText) {
            drawAxisText(drawable);
        }
    }

    @Override
    public float[] getSortPoint() {
        float cx, cy, cz;
        synchronized (_boundsLock) {
            cx = (_minX + _maxX) / 2f;
            cy = (_minY + _maxY) / 2f;
            cz = (_minZ + _maxZ) / 2f;
        }
        return new float[]{ cx, cy, cz };
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Drain _pendingPoints into _committedPoints.
     * MUST be called on the EDT or GL render thread (whichever owns Panel3D).
     */
    private void drainPending() {
        // Fast check without locking
        if (_pendingPoints.isEmpty()) {
            return;
        }

        List<ScatterPoint> snapshot;
        _pointsLock.writeLock().lock();
        try {
            if (_pendingPoints.isEmpty()) return;
            snapshot = new ArrayList<>(_pendingPoints);
            _pendingPoints.clear();
        } finally {
            _pointsLock.writeLock().unlock();
        }

        _committedPoints.addAll(snapshot);
    }

    /**
     * Expand the bounding box to include (x, y, z).
     * Called from producer threads; uses a tiny synchronized block.
     *
     * @return {@code true} if any bound actually changed
     */
    private boolean expandBounds(float x, float y, float z) {
        synchronized (_boundsLock) {
            boolean changed = false;
            if (x < _minX) { _minX = x; changed = true; }
            if (x > _maxX) { _maxX = x; changed = true; }
            if (y < _minY) { _minY = y; changed = true; }
            if (y > _maxY) { _maxY = y; changed = true; }
            if (z < _minZ) { _minZ = z; changed = true; }
            if (z > _maxZ) { _maxZ = z; changed = true; }
            return changed;
        }
    }


    /** Recompute colors for scalar-mapped points after the color map changes. */
    private void recolorMappedPoints() {
        ScientificColorMap map = _colorMap;

        _pointsLock.writeLock().lock();
        try {
            for (ScatterPoint pt : _pendingPoints) {
                if (pt.mapped) {
                    pt.color = map.colorAt(pt.value01);
                }
            }
        } finally {
            _pointsLock.writeLock().unlock();
        }

        SwingUtilities.invokeLater(() -> {
            for (ScatterPoint pt : _committedPoints) {
                if (pt.mapped) {
                    pt.color = map.colorAt(pt.value01);
                }
            }
        });
    }

    /**
     * Coalesced bounds-change notification.  At most one EDT task is queued
     * at a time; it reads the latest bounds when it fires, so rapid expansions
     * are folded into a single callback.
     */
    private void scheduleBoundsNotifyIfNeeded() {
        if (_boundsNotifyPending.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                _boundsNotifyPending.set(false);
                BoundsListener listener = _boundsListener;
                if (listener == null) return;
                float[] b;
                synchronized (_boundsLock) {
                    b = new float[]{ _minX, _maxX, _minY, _maxY, _minZ, _maxZ };
                }
                listener.boundsChanged(b);
            });
        }
    }

    /**
     * Schedule a repaint on the EDT if:
     *   (a) no repaint is already pending, AND
     *   (b) the throttle interval has elapsed since the last scheduled repaint.
     *
     * Uses compare-and-set as the gate so only one Runnable is ever queued
     * at a time.
     */
    private void scheduleRepaintIfNeeded() {
        long now      = System.currentTimeMillis();
        long throttle = _throttleMs;

        // Throttle check — if we fired too recently, skip this cycle.
        // The next add() call will get through once the interval has passed.
        if (throttle > 0 && (now - _lastRepaintScheduledMs) < throttle) {
            // Still within the throttle window — but make sure a repaint will
            // eventually happen even if no more adds come in.
            if (_repaintPending.compareAndSet(false, true)) {
                long delay = throttle - (now - _lastRepaintScheduledMs);
                // Schedule a delayed repaint so the last batch isn't lost
                scheduleDelayedRepaint(delay);
            }
            return;
        }

        // Gate: only one pending repaint at a time
        if (_repaintPending.compareAndSet(false, true)) {
            _lastRepaintScheduledMs = now;
            SwingUtilities.invokeLater(this::doRepaint);
        }
    }

    /**
     * Schedule a repaint after {@code delayMs} milliseconds using a daemon
     * thread so we don't leave abandoned work items if the user disposes
     * the window.
     */
    private void scheduleDelayedRepaint(long delayMs) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            _lastRepaintScheduledMs = System.currentTimeMillis();
            SwingUtilities.invokeLater(this::doRepaint);
        }, "ScatterPlot3D-delayed-repaint");
        t.setDaemon(true);
        t.start();
    }

    /** The actual repaint action — always runs on the EDT. */
    private void doRepaint() {
        // Clear the gate first so new adds during this repaint are captured
        _repaintPending.set(false);
        if (_panel3D != null) {
            _panel3D.softRefresh();
        }
    }


    /** Return the concrete render style for the current data size. */
    private RenderStyle effectiveRenderStyle(int npts) {
        RenderStyle style = _renderStyle;
        if (style == RenderStyle.AUTO) {
            return (npts > _autoPointThreshold) ? RenderStyle.POINTS : RenderStyle.SPHERES;
        }
        return style;
    }

    /** Draw axis titles and tick labels as a crisp screen-space overlay. */
    private void drawAxisText(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        if (_axisTitleRenderer == null) {
            _axisTitleRenderer = new TextRendering3D(new Font("SansSerif", Font.BOLD, 13));
        }

        if (_tickLabelRenderer == null) {
            _tickLabelRenderer = new TextRendering3D(new Font("SansSerif", Font.PLAIN, 11));
        }

        float x0, x1, y0, y1, z0, z1;
        synchronized (_boundsLock) {
            x0 = _minX; x1 = _maxX;
            y0 = _minY; y1 = _maxY;
            z0 = _minZ; z1 = _maxZ;
        }

        final float xm = 0.5f * (x0 + x1);
        final float ym = 0.5f * (y0 + y1);
        final float zm = 0.5f * (z0 + z1);

        _axisTitleRenderer.begin(drawable);
        try {
            _axisTitleRenderer.drawWorld(_panel3D, gl,
                    _xAxisLabel, xm, y0, z0,
                    0, -30,
                    Anchor.CENTER,
                    TextRendering3D.Style.axisTitle());

            _axisTitleRenderer.drawWorld(_panel3D, gl,
                    _yAxisLabel, x0, ym, z0,
                    -18, 4,
                    Anchor.RIGHT_CENTER,
                    TextRendering3D.Style.axisTitle());

            _axisTitleRenderer.drawWorld(_panel3D, gl,
                    _zAxisLabel, x0, y0, zm,
                    10, 2,
                    Anchor.LEFT_CENTER,
                    TextRendering3D.Style.axisTitle());
        } finally {
            _axisTitleRenderer.end();
        }

        int n = _axisTickCount;
        if (n <= 0) {
            return;
        }

        _tickLabelRenderer.begin(drawable);
        try {
            for (int i = 0; i <= n; i++) {
                float f = i / (float) n;

                float x = lerp(x0, x1, f);
                float y = lerp(y0, y1, f);
                float z = lerp(z0, z1, f);

                _tickLabelRenderer.drawWorld(_panel3D, gl,
                        formatTick(x), x, y0, z0,
                        0, -15,
                        Anchor.CENTER,
                        TextRendering3D.Style.tickLabel());

                _tickLabelRenderer.drawWorld(_panel3D, gl,
                        formatTick(y), x0, y, z0,
                        -8, 0,
                        Anchor.RIGHT_CENTER,
                        TextRendering3D.Style.tickLabel());

                _tickLabelRenderer.drawWorld(_panel3D, gl,
                        formatTick(z), x0, y0, z,
                        8, 0,
                        Anchor.LEFT_CENTER,
                        TextRendering3D.Style.tickLabel());
            }
        } finally {
            _tickLabelRenderer.end();
        }
    }

    /** Format a tick value compactly for axis labels. */
    private static String formatTick(float value) {
        if (Math.abs(value) < 1.0e-6f) {
            return "0";
        }
        if (Math.abs(value) >= 1000f || Math.abs(value) < 0.01f) {
            return String.format("%.1e", value);
        }
        return String.format("%.2f", value);
    }

    /** Draw all committed points as fast, per-vertex-colored OpenGL points. */
    private void drawPointSprites(GLAutoDrawable drawable) {
        if (_committedPoints.isEmpty()) {
            return;
        }

        GL2 gl = drawable.getGL().getGL2();
        gl.glPointSize(_pointSize);
        gl.glEnable(GL2ES1.GL_POINT_SMOOTH);

        gl.glBegin(GL.GL_POINTS);
        for (ScatterPoint pt : _committedPoints) {
            Support3D.setColor(gl, pt.color);
            gl.glVertex3f(pt.x, pt.y, pt.z);
        }
        gl.glEnd();
    }

    /** Draw a light reference grid on the lower-Z face of the bounding box. */
    private void drawFloorGrid(GLAutoDrawable drawable) {
        float x0, x1, y0, y1, z0;
        synchronized (_boundsLock) {
            x0 = _minX; x1 = _maxX;
            y0 = _minY; y1 = _maxY;
            z0 = _minZ;
        }

        int n = _gridDivisions;
        Color gc = _gridColor;
        for (int i = 0; i <= n; i++) {
            float f = i / (float) n;
            float x = lerp(x0, x1, f);
            float y = lerp(y0, y1, f);
            Support3D.drawLine(drawable, new float[]{ x, y0, z0 }, new float[]{ x, y1, z0 }, gc, 0.75f);
            Support3D.drawLine(drawable, new float[]{ x0, y, z0 }, new float[]{ x1, y, z0 }, gc, 0.75f);
        }
    }

    /**
     * Draw the 12-edge wireframe bounding box using the current min/max bounds.
     * Uses Line3D-style drawing via Support3D so the box integrates cleanly
     * with the Panel3D depth-sort pipeline.
     */
    private void drawBoundingBox(GLAutoDrawable drawable) {
        float x0, x1, y0, y1, z0, z1;
        synchronized (_boundsLock) {
            x0 = _minX; x1 = _maxX;
            y0 = _minY; y1 = _maxY;
            z0 = _minZ; z1 = _maxZ;
        }

        Color bc = _boxColor;

        // Bottom face (z = z0)
        drawEdge(drawable, x0, y0, z0,  x1, y0, z0, bc);
        drawEdge(drawable, x1, y0, z0,  x1, y1, z0, bc);
        drawEdge(drawable, x1, y1, z0,  x0, y1, z0, bc);
        drawEdge(drawable, x0, y1, z0,  x0, y0, z0, bc);

        // Top face (z = z1)
        drawEdge(drawable, x0, y0, z1,  x1, y0, z1, bc);
        drawEdge(drawable, x1, y0, z1,  x1, y1, z1, bc);
        drawEdge(drawable, x1, y1, z1,  x0, y1, z1, bc);
        drawEdge(drawable, x0, y1, z1,  x0, y0, z1, bc);

        // Vertical pillars
        drawEdge(drawable, x0, y0, z0,  x0, y0, z1, bc);
        drawEdge(drawable, x1, y0, z0,  x1, y0, z1, bc);
        drawEdge(drawable, x1, y1, z0,  x1, y1, z1, bc);
        drawEdge(drawable, x0, y1, z0,  x0, y1, z1, bc);
    }

    private static void drawEdge(GLAutoDrawable drawable,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 Color color) {
        Support3D.drawLine(drawable,
                new float[]{ x0, y0, z0 },
                new float[]{ x1, y1, z1 },
                color, BOX_LINE_WIDTH);
    }

    // -----------------------------------------------------------------------
    // Legacy convenience heat-map color gradient: blue → cyan → green → yellow → red
    // -----------------------------------------------------------------------

    /**
     * Legacy convenience method. New code should normally use ScientificColorMap.
     * Maps a scalar value in [0, 1] to a five-stop heat-map color:
     * blue (0) → cyan (0.25) → green (0.5) → yellow (0.75) → red (1).
     *
     * @param t value in [0, 1] (clamped)
     * @return interpolated AWT Color
     */
    public static Color heatMapColor(float t) {
        t = Math.max(0f, Math.min(1f, t));

        // Five stops at 0, 0.25, 0.5, 0.75, 1.0
        final float[][] stops = {
            { 0f,   0f,   1f  },  // blue
            { 0f,   1f,   1f  },  // cyan
            { 0f,   1f,   0f  },  // green
            { 1f,   1f,   0f  },  // yellow
            { 1f,   0f,   0f  },  // red
        };

        float scaled = t * (stops.length - 1);
        int   lo     = Math.min((int) scaled, stops.length - 2);
        float frac   = scaled - lo;

        float r = lerp(stops[lo][0], stops[lo + 1][0], frac);
        float g = lerp(stops[lo][1], stops[lo + 1][1], frac);
        float b = lerp(stops[lo][2], stops[lo + 1][2], frac);

        return new Color(r, g, b);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // -----------------------------------------------------------------------
    // Inner data record
    // -----------------------------------------------------------------------

    /** Mutable color record for one scatter-plot point. */
    private static final class ScatterPoint {
        final float x, y, z;
        final boolean mapped;
        final float value01;
        Color color;

        ScatterPoint(float x, float y, float z, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.mapped = false;
            this.value01 = 0f;
            this.color = (color == null) ? Color.WHITE : color;
        }

        ScatterPoint(float x, float y, float z, float value01, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.mapped = true;
            this.value01 = value01;
            this.color = (color == null) ? Color.WHITE : color;
        }
    }
}
