package edu.cnu.mdi.mdi3D.panel;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Utility for rendering crisp, screen-space text associated with 3D world
 * positions in an MDI-3D {@link Panel3D}.
 * <p>
 * The class projects a world-space point into window coordinates and then uses
 * JOGL's {@link TextRenderer} to draw the text in pixel coordinates. This gives
 * the text a constant on-screen size regardless of zoom, which is usually what
 * is wanted for axes, tick labels, point labels, and plot annotations.
 * </p>
 * <p>
 * Important implementation detail: {@code TextRenderer.beginRendering(...)}
 * installs a 2D projection. Therefore this helper snapshots the 3D scene
 * matrices before the first text draw in each pass and uses those saved matrices
 * for all later world-to-screen projections in that pass.
 * </p>
 */
public final class TextRendering3D {

    /** Anchor point used to place text relative to its projected pixel position. */
    public enum Anchor {
        /** Place the left edge of the text at the projected point. */
        LEFT,
        /** Center the text horizontally on the projected point. */
        CENTER,
        /** Place the right edge of the text at the projected point. */
        RIGHT,
        /** Left aligned and vertically centered on the projected point. */
        LEFT_CENTER,
        /** Horizontally and vertically centered on the projected point. */
        CENTER_CENTER,
        /** Right aligned and vertically centered on the projected point. */
        RIGHT_CENTER
    }

    /** Immutable drawing style for one text pass. */
    public static final class Style {
        /** Foreground text color. */
        public final Color foreground;
        /** Shadow/outline color. If {@code null}, no shadow/outline is drawn. */
        public final Color shadow;
        /** Pixel offset used for shadow drawing. */
        public final int shadowOffset;
        /** If true, draw a four-direction outline instead of a single shadow. */
        public final boolean outline;
        /** If true, skip labels projected outside the viewport or behind camera. */
        public final boolean clipToViewport;

        /**
         * Create a style.
         *
         * @param foreground foreground color; if {@code null}, white is used
         * @param shadow shadow or outline color; {@code null} disables it
         * @param shadowOffset shadow/outline offset in pixels, normally 1 or 2
         * @param outline true for four-direction outline, false for one shadow
         * @param clipToViewport true to skip off-screen or behind-camera labels
         */
        public Style(Color foreground, Color shadow, int shadowOffset, boolean outline, boolean clipToViewport) {
            this.foreground = (foreground == null) ? Color.WHITE : foreground;
            this.shadow = shadow;
            this.shadowOffset = Math.max(1, shadowOffset);
            this.outline = outline;
            this.clipToViewport = clipToViewport;
        }

        /** @return a general-purpose label style */
        public static Style label() {
            return new Style(Color.WHITE, new Color(0, 0, 0, 190), 2, false, true);
        }

        /** @return an axis-title style suitable for light and dark data regions */
        public static Style axisTitle() {
            return new Style(Color.WHITE, new Color(0, 0, 0, 230), 1, true, true);
        }

        /** @return a tick-label style suitable for light plot backgrounds */
        public static Style tickLabel() {
            return new Style(new Color(35, 35, 35), new Color(255, 255, 255, 210), 1, true, true);
        }
    }

    /** Font used by the JOGL text renderer. */
    private final Font _font;

    /** JOGL text renderer owned by this helper. */
    private TextRenderer _renderer;

    /** GLU helper for projecting saved 3D matrices. */
    private final GLU _glu = new GLU();

    /** True between begin(...) and end(). */
    private boolean _rendering;

    /** True after TextRenderer.beginRendering(...) has been called. */
    private boolean _textPassStarted;

    /** True once the 3D matrices have been captured for this label pass. */
    private boolean _matricesCaptured;

    /** Saved viewport from the 3D scene pass. */
    private final int[] _viewport = new int[4];

    /** Saved model-view matrix from the 3D scene pass. */
    private final float[] _model = new float[16];

    /** Saved projection matrix from the 3D scene pass. */
    private final float[] _projection = new float[16];

    /** Current surface width during a rendering pass. */
    private int _width;

    /** Current surface height during a rendering pass. */
    private int _height;

    /**
     * Create a text-rendering helper for a font.
     *
     * @param font font to use; if {@code null}, a 12-point sans-serif font is used
     */
    public TextRendering3D(Font font) {
        _font = (font == null) ? new Font("SansSerif", Font.PLAIN, 12) : font;
    }

    /**
     * Begin a batched text rendering pass. Call {@link #end()} when done.
     *
     * @param drawable current drawable
     */
    public void begin(GLAutoDrawable drawable) {
        if (_rendering) {
            return;
        }
        if (_renderer == null) {
            _renderer = new TextRenderer(_font, true, true);
        }
        _width = drawable.getSurfaceWidth();
        _height = drawable.getSurfaceHeight();
        _rendering = true;
        _textPassStarted = false;
        _matricesCaptured = false;
    }

    /** End the current batched text rendering pass. */
    public void end() {
        if (!_rendering) {
            return;
        }
        if (_textPassStarted) {
            _renderer.endRendering();
        }
        _textPassStarted = false;
        _matricesCaptured = false;
        _rendering = false;
    }

    /** Release the underlying JOGL text renderer. */
    public void dispose() {
        if (_textPassStarted && _renderer != null) {
            _renderer.endRendering();
        }
        if (_renderer != null) {
            _renderer.dispose();
            _renderer = null;
        }
        _textPassStarted = false;
        _matricesCaptured = false;
        _rendering = false;
    }

    /**
     * Draw text attached to a 3D world coordinate.
     *
     * @param panel3D owning panel, retained for call-site clarity
     * @param gl current GL2 context while the 3D model-view/projection state is active
     * @param text text to draw; blank or {@code null} text is ignored
     * @param x world x coordinate
     * @param y world y coordinate
     * @param z world z coordinate
     * @param dx pixel offset applied after projection, positive right
     * @param dy pixel offset applied after projection, positive up
     * @param anchor text anchor; if {@code null}, {@link Anchor#LEFT} is used
     * @param style drawing style; if {@code null}, {@link Style#label()} is used
     * @return {@code true} if the label was drawn; {@code false} if skipped
     */
    public boolean drawWorld(Panel3D panel3D, GL2 gl, String text, float x, float y, float z, int dx, int dy,
            Anchor anchor, Style style) {
        if (!_rendering || panel3D == null || gl == null || text == null || text.isBlank()) {
            return false;
        }
        captureMatricesIfNeeded(gl);
        final float[] win = new float[3];
        _glu.gluProject(x, y, z, _model, 0, _projection, 0, _viewport, 0, win, 0);
        return drawProjected(text, win[0], win[1], win[2], dx, dy, anchor, style);
    }

    /**
     * Draw text at already-projected window coordinates. Window coordinates use
     * the JOGL convention: origin at lower left.
     */
    public boolean drawProjected(String text, float winX, float winY, float winZ, int dx, int dy, Anchor anchor,
            Style style) {
        if (!_rendering || text == null || text.isBlank()) {
            return false;
        }
        final Style s = (style == null) ? Style.label() : style;
        if (s.clipToViewport) {
            if (winZ < 0f || winZ > 1f || winX < 0f || winX > _width || winY < 0f || winY > _height) {
                return false;
            }
        }
        final Anchor a = (anchor == null) ? Anchor.LEFT : anchor;
        final Rectangle2D bounds = _renderer.getBounds(text);
        int px = Math.round(winX) + dx;
        int py = Math.round(winY) + dy;

        switch (a) {
        case CENTER:
            px -= Math.round((float) bounds.getWidth() / 2f);
            break;
        case RIGHT:
            px -= Math.round((float) bounds.getWidth());
            break;
        case LEFT_CENTER:
            py -= Math.round((float) bounds.getHeight() / 2f);
            break;
        case CENTER_CENTER:
            px -= Math.round((float) bounds.getWidth() / 2f);
            py -= Math.round((float) bounds.getHeight() / 2f);
            break;
        case RIGHT_CENTER:
            px -= Math.round((float) bounds.getWidth());
            py -= Math.round((float) bounds.getHeight() / 2f);
            break;
        case LEFT:
        default:
            break;
        }
        drawScreen(text, px, py, s);
        return true;
    }

    /**
     * Draw text directly in screen space. The helper must be inside a
     * {@link #begin(GLAutoDrawable)} / {@link #end()} pair.
     */
    public void drawScreen(String text, int x, int y, Style style) {
        if (!_rendering || text == null || text.isBlank()) {
            return;
        }
        ensureTextPassStarted();
        final Style s = (style == null) ? Style.label() : style;
        if (s.shadow != null) {
            _renderer.setColor(s.shadow);
            final int d = s.shadowOffset;
            if (s.outline) {
                _renderer.draw(text, x - d, y);
                _renderer.draw(text, x + d, y);
                _renderer.draw(text, x, y - d);
                _renderer.draw(text, x, y + d);
            } else {
                _renderer.draw(text, x + d, y - d);
            }
        }
        _renderer.setColor(s.foreground);
        _renderer.draw(text, x, y);
    }

    /** Convenience one-shot method for drawing a single world-attached label. */
    public boolean drawWorldOneShot(Panel3D panel3D, GLAutoDrawable drawable, GL2 gl, String text, float x, float y,
            float z, int dx, int dy, Anchor anchor, Style style) {
        begin(drawable);
        try {
            return drawWorld(panel3D, gl, text, x, y, z, dx, dy, anchor, style);
        } finally {
            end();
        }
    }

    /** Capture the current 3D viewport and matrices once per label pass. */
    private void captureMatricesIfNeeded(GL2 gl) {
        if (_matricesCaptured) {
            return;
        }
        gl.glGetIntegerv(GL.GL_VIEWPORT, _viewport, 0);
        gl.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, _model, 0);
        gl.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, _projection, 0);
        _matricesCaptured = true;
    }

    /** Lazily start JOGL text rendering after projection state has been saved. */
    private void ensureTextPassStarted() {
        if (!_textPassStarted) {
            _renderer.beginRendering(_width, _height);
            _textPassStarted = true;
        }
    }

    /** Push an OpenGL state suitable for 2D overlay drawing without depth testing. */
    public static void pushOverlayState(GL2 gl, GLAutoDrawable drawable) {
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(0, drawable.getSurfaceWidth(), 0, drawable.getSurfaceHeight(), -1, 1);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glDisable(GL.GL_DEPTH_TEST);
    }

    /** Restore state pushed by {@link #pushOverlayState(GL2, GLAutoDrawable)}. */
    public static void popOverlayState(GL2 gl) {
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glPopMatrix();
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
    }
}
