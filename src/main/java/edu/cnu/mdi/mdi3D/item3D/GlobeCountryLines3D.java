package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mapping.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.mdi3D.panel.Support3D;

/**
 * Draws country outlines on a sphere as 3D polyline strips.
 * <p>
 * Input polygons are provided as rings of {@link Point2D.Double} values where:
 * <ul>
 *   <li>{@code x} is longitude in radians (typically wrapped to {@code [-pi, pi)})</li>
 *   <li>{@code y} is latitude in radians ({@code [-pi/2, pi/2]})</li>
 * </ul>
 * This is the format produced by {@code edu.cnu.mdi.mapping.GeoJsonCountryLoader}.
 *
 * <h2>Dateline seam handling</h2>
 * GeoJSON rings that cross the dateline can contain adjacent points whose longitudes differ by
 * nearly {@code 2*pi}. If rendered as a continuous strip on the sphere, those segments would
 * "cut across" the globe.
 * <p>
 * For demo purposes, this item takes a simple, robust approach:
 * it splits the polyline whenever a segment exhibits {@code |dLon| > pi}.
 * This avoids globe-spanning chords at the cost of a small seam gap at the dateline.
 *
 * <h2>Rendering details</h2>
 * By default, vertices are placed slightly above the sphere surface ({@link #getRadialLift()})
 * to reduce z-fighting with the sphere mesh.
 */
public class GlobeCountryLines3D extends Item3D {

    /** Threshold for splitting segments across the dateline seam. */
    public static final double DEFAULT_DATELINE_SPLIT_THRESHOLD = Math.PI;

    private final List<float[]> strips = new ArrayList<>();
    private final float radius;

    private Color lineColor = new Color(230, 230, 230);
    private float lineWidth = 1.0f;
    private float radialLift = 0.0025f;

    private double datelineSplitThreshold = DEFAULT_DATELINE_SPLIT_THRESHOLD;

    /**
     * Construct a country outline renderer.
     *
     * @param panel3D   the owning 3D panel
     * @param features  country features loaded from GeoJSON
     * @param radius    sphere radius in scene units
     */
    public GlobeCountryLines3D(Panel3D panel3D, List<CountryFeature> features, float radius) {
        super(panel3D);
        this.radius = radius;
        rebuild(Objects.requireNonNull(features, "features"));
    }

    /**
     * Rebuild the internal polyline strips from the provided features.
     *
     * @param features country features
     */
    public final void rebuild(List<CountryFeature> features) {
        strips.clear();
        for (CountryFeature feature : features) {
            if (feature == null) {
                continue;
            }
            List<List<Point2D.Double>> polys = feature.getPolygons();
            if (polys == null) {
                continue;
            }
            for (List<Point2D.Double> ring : polys) {
                addRingAsStrips(ring);
            }
        }
    }

    /**
     * Set the outline color.
     *
     * @param c the new color (non-null)
     */
    @Override
	public void setLineColor(Color c) {
        this.lineColor = Objects.requireNonNull(c, "c");
    }

    /**
     * Get the current outline color.
     *
     * @return line color
     */
    @Override
	public Color getLineColor() {
        return lineColor;
    }

    /**
     * Set the polyline width (OpenGL line width).
     *
     * @param w width in pixels (values &lt;= 0 are clamped to 1)
     */
    @Override
	public void setLineWidth(float w) {
        this.lineWidth = Math.max(1.0f, w);
    }

    /**
     * Get the current line width.
     *
     * @return line width in pixels
     */
    @Override
	public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Set a small radial lift applied to all vertices to reduce z-fighting with the sphere.
     *
     * @param lift radial lift in scene units (negative values treated as 0)
     */
    public void setRadialLift(float lift) {
        this.radialLift = Math.max(0f, lift);
    }

    /**
     * Get the radial lift.
     *
     * @return radial lift in scene units
     */
    public float getRadialLift() {
        return radialLift;
    }

    /**
     * Set the longitude discontinuity threshold (radians) used to split rings at the dateline.
     *
     * @param threshold radians; values &lt;= 0 revert to {@link #DEFAULT_DATELINE_SPLIT_THRESHOLD}
     */
    public void setDatelineSplitThreshold(double threshold) {
        this.datelineSplitThreshold = (threshold > 0) ? threshold : DEFAULT_DATELINE_SPLIT_THRESHOLD;
    }

    /**
     * Get the current dateline split threshold.
     *
     * @return threshold in radians
     */
    public double getDatelineSplitThreshold() {
        return datelineSplitThreshold;
    }

    @Override
    public void draw(GLAutoDrawable drawable) {
        if (strips.isEmpty()) {
            return;
        }
        for (float[] coords : strips) {
            if (coords != null && coords.length >= 6) {
                Support3D.drawPolyLine(drawable, coords, lineColor, lineWidth);
            }
        }
    }

    // ----------------------------- internal helpers -----------------------------

    private void addRingAsStrips(List<Point2D.Double> ring) {
        if (ring == null || ring.size() < 2) {
            return;
        }

        // Collect vertices into a growable float list. We'll flush to a strip on seam splits.
        FloatList verts = new FloatList(ring.size() * 3);

        Point2D.Double prev = null;
        for (Point2D.Double p : ring) {
            if (p == null) {
                continue;
            }
            if (prev != null) {
                double dLon = p.x - prev.x;
                if (Math.abs(dLon) > datelineSplitThreshold) {
                    // Flush current strip and restart.
                    flushStrip(verts);
                }
            }
            addLonLatVertex(verts, p.x, p.y);
            prev = p;
        }

        // Flush remaining vertices.
        flushStrip(verts);
    }

    private void flushStrip(FloatList verts) {
        if (verts.size() >= 6) { // at least 2 points
            strips.add(verts.toArray());
        }
        verts.clear();
    }

    private void addLonLatVertex(FloatList out, double lonRad, double latRad) {
        // Standard sphere mapping:
        // x = r cos(lat) cos(lon)
        // y = r cos(lat) sin(lon)
        // z = r sin(lat)
        double cosLat = Math.cos(latRad);
        float r = radius + radialLift;

        float x = (float) (r * cosLat * Math.cos(lonRad));
        float y = (float) (r * cosLat * Math.sin(lonRad));
        float z = (float) (r * Math.sin(latRad));

        out.add(x);
        out.add(y);
        out.add(z);
    }

    /**
     * Minimal growable float list to avoid boxing.
     */
    private static final class FloatList {
        private float[] a;
        private int n;

        FloatList(int capacity) {
            a = new float[Math.max(16, capacity)];
        }

        void add(float v) {
            if (n >= a.length) {
                float[] b = new float[a.length * 2];
                System.arraycopy(a, 0, b, 0, a.length);
                a = b;
            }
            a[n++] = v;
        }

        int size() {
            return n;
        }

        void clear() {
            n = 0;
        }

        float[] toArray() {
            float[] out = new float[n];
            System.arraycopy(a, 0, out, 0, n);
            return out;
        }
    }
}
