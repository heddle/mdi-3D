package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;
import java.awt.Font;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.awt.TextRenderer;

import edu.cnu.mdi.mdi3D.panel.Panel3D;

/**
 * Draws screen-space text labels for a set of 3D points.
 * <p>
 * This item projects each 3D point into window coordinates using
 * {@link Panel3D#project(GL2, float, float, float, float[])} and then renders
 * the corresponding label in pixel coordinates via JOGL's {@link TextRenderer}.
 * <p>
 * This keeps label size constant regardless of zoom and avoids any need for 3D
 * text geometry. Label visibility policy (e.g., globe front-hemisphere) is
 * optional and controlled by a pluggable {@link LabelCull}.
 */
public class LabelSet3D extends Item3D {

    /** Functional interface for optional per-label visibility tests. */
    @FunctionalInterface
    public interface LabelCull {
        /**
         * Decide whether to render a label for a point.
         *
         * @param gl       current GL2
         * @param modelView a snapshot of GL_MODELVIEW_MATRIX for the 3D scene (length 16)
         * @param x        point world x
         * @param y        point world y
         * @param z        point world z
         * @param winX     projected window x (pixels, origin at left)
         * @param winY     projected window y (pixels, origin at bottom)
         * @param winZ     projected window depth in [0,1] (gluProject convention)
         * @return true to draw the label, false to skip it
         */
        boolean accept(GL2 gl, float[] modelView, float x, float y, float z, float winX, float winY, float winZ);
    }

    /** Point coordinates as [x1,y1,z1,...]. */
    private float[] _coords;

    /** Labels corresponding to points (length == coords.length/3). */
    private String[] _labels;

    /** Pixel offset applied after projection. */
    private int _dx = 6;
    private int _dy = 2;

    /** If true, require projected coords in viewport and depth in [0,1]. */
    private boolean _clipToViewport = true;

    /** Optional culling strategy (null means no extra culling). */
    private LabelCull _cull;

    /** Lazily-created JOGL text renderer. */
    private TextRenderer _textRenderer;

    /**
     * Create a label set for 3D points.
     *
     * @param panel3D owner panel
     * @param coords  point coords as [x1,y1,z1,...]
     * @param labels  per-point labels (same count as points)
     */
    public LabelSet3D(Panel3D panel3D, float[] coords, String[] labels) {
        super(panel3D);
        _coords = coords;
        _labels = labels;
    }

    /**
     * Install (or clear) an optional culling strategy.
     *
     * @param cull strategy, or null to disable extra culling
     */
    public void setCull(LabelCull cull) {
        _cull = cull;
    }

    /**
     * Set pixel offsets applied to each label after projecting.
     *
     * @param dx pixels right
     * @param dy pixels up
     */
    public void setPixelOffset(int dx, int dy) {
        _dx = dx;
        _dy = dy;
    }

    /**
     * If enabled, labels must project into the viewport and have depth in [0,1].
     *
     * @param clip true to clip labels to viewport
     */
    public void setClipToViewport(boolean clip) {
        _clipToViewport = clip;
    }

    /**
     * Update coordinates (e.g., if points move).
     *
     * @param coords [x1,y1,z1,...]
     */
    public void setCoords(float[] coords) {
        _coords = coords;
    }

    /**
     * Update labels.
     *
     * @param labels per-point labels
     */
    public void setLabels(String[] labels) {
        _labels = labels;
    }

    @Override
    public void draw(GLAutoDrawable drawable) {

        if (_coords == null || _labels == null) {
            return;
        }

        int n = _coords.length / 3;
        if (_labels.length < n) {
            n = _labels.length;
        }
        if (n <= 0) {
            return;
        }

        final GL2 gl = drawable.getGL().getGL2();
        final int w = drawable.getSurfaceWidth();
        final int h = drawable.getSurfaceHeight();

        // Lazily create TextRenderer using this item's font property.
        if (_textRenderer == null) {
            Font font = null;
            try {
                font = getFont(FONT);
            } catch (Exception ignored) { }
            if (font == null) {
                font = new Font("SansSerif", Font.PLAIN, 12);
            }
            _textRenderer = new TextRenderer(font, true, true);
        }

        Color textColor;
        try {
            textColor = getColor(TEXT_COLOR);
        } catch (Exception e) {
            textColor = Color.white;
        }

        // Snapshot current modelview matrix (the one used for the 3D scene)
        final float[] mv = new float[16];
        gl.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, mv, 0);

        // Project FIRST (while 3D matrices are active).
        final float[] win = new float[3];
        final int[] sx = new int[n];
        final int[] sy = new int[n];
        final boolean[] ok = new boolean[n];

        for (int i = 0; i < n; i++) {

            final String label = _labels[i];
            if (label == null || label.isBlank()) {
                ok[i] = false;
                continue;
            }

            final float x = _coords[3 * i];
            final float y = _coords[3 * i + 1];
            final float z = _coords[3 * i + 2];

            _panel3D.project(gl, x, y, z, win);

            final float wx = win[0];
            final float wy = win[1];
            final float wz = win[2];

            if (_clipToViewport) {
                if (wz < 0f || wz > 1f) {
                    ok[i] = false;
                    continue;
                }
                if (wx < 0f || wx > w || wy < 0f || wy > h) {
                    ok[i] = false;
                    continue;
                }
            }

            if (_cull != null) {
                if (!_cull.accept(gl, mv, x, y, z, wx, wy, wz)) {
                    ok[i] = false;
                    continue;
                }
            }

            sx[i] = (int) (wx + _dx);
            sy[i] = (int) (wy + _dy);
            ok[i] = true;
        }

        // Now render in screen space.
        _textRenderer.beginRendering(w, h);
        _textRenderer.setColor(textColor);

        for (int i = 0; i < n; i++) {
            if (!ok[i]) {
				continue;
			}
            _textRenderer.draw(_labels[i], sx[i], sy[i]);
        }

        _textRenderer.endRendering();
    }

    // --------------------------------------------------------------------
    // Useful built-in culling policies
    // --------------------------------------------------------------------

    /**
     * Returns a culling policy that keeps labels on the visible hemisphere of a sphere.
     * <p>
     * This is a geometric hemisphere test for a sphere whose center is given in world coordinates.
     * It does <b>not</b> require the sphere to be centered at (0,0,0).
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Transforms the sphere center C and the point P into eye space using the provided modelview.</li>
     *   <li>Computes N = P - C (outward normal direction).</li>
     *   <li>Computes V = -C (vector from center to camera at origin in eye space).</li>
     *   <li>Accepts if N · V &gt; 0.</li>
     * </ul>
     *
     * @param cx sphere center world x
     * @param cy sphere center world y
     * @param cz sphere center world z
     * @return a culling strategy for the front hemisphere of that sphere
     */
    public static LabelCull frontHemisphereCull(final float cx, final float cy, final float cz) {
        return (gl, mv, x, y, z, winX, winY, winZ) -> {

            // Eye-space center C = M * [cx cy cz 1]^T
            final float Cx = mv[0] * cx + mv[4] * cy + mv[8]  * cz + mv[12];
            final float Cy = mv[1] * cx + mv[5] * cy + mv[9]  * cz + mv[13];
            final float Cz = mv[2] * cx + mv[6] * cy + mv[10] * cz + mv[14];

            // Eye-space point P = M * [x y z 1]^T
            final float Px = mv[0] * x + mv[4] * y + mv[8]  * z + mv[12];
            final float Py = mv[1] * x + mv[5] * y + mv[9]  * z + mv[13];
            final float Pz = mv[2] * x + mv[6] * y + mv[10] * z + mv[14];

            // N = P - C
            final float Nx = Px - Cx;
            final float Ny = Py - Cy;
            final float Nz = Pz - Cz;

            // V = -C (center -> camera)
            final float Vx = -Cx;
            final float Vy = -Cy;
            final float Vz = -Cz;

            return (Nx * Vx + Ny * Vy + Nz * Vz) > 0f;
        };
    }
}
