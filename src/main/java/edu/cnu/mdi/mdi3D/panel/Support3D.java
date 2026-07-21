package edu.cnu.mdi.mdi3D.panel;

import java.awt.Color;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A stateless library of OpenGL drawing primitives for the MDI 3D framework.
 *
 * <p>All methods are {@code static} and accept a {@link GLAutoDrawable} (or a
 * raw {@link GL2} context for lower-level helpers) plus the geometry and colour
 * parameters needed to produce output. No instance is required.
 *
 * <h2>Coordinate conventions</h2>
 * <p>All geometry is expressed in the caller's current model-view coordinate
 * system. Packed coordinate arrays always follow the layout:
 * <pre>
 *   [x&#8320;, y&#8320;, z&#8320;,  x&#8321;, y&#8321;, z&#8321;,  ...]
 * </pre>
 * so every three consecutive floats describe one point and
 * {@code coords.length / 3} gives the point count.
 *
 * <h2>Colour and alpha</h2>
 * <p>Every drawing method accepts {@link java.awt.Color} values whose alpha
 * component is honoured: a fully-opaque colour has alpha&nbsp;255 and a fully
 * transparent colour has alpha&nbsp;0. Pass {@code null} where a colour is
 * documented as optional to suppress that drawing pass.
 *
 * <h2>Line width</h2>
 * <p>Methods that accept a {@code lineWidth} parameter set {@code glLineWidth}
 * before drawing and restore it to {@code 1f} afterwards. The caller's current
 * line width is therefore clobbered; this is consistent with the rest of the
 * framework.
 *
 * <h2>Thread safety</h2>
 * <p>All methods must be called from the JOGL GL thread (typically inside a
 * {@link com.jogamp.opengl.GLEventListener} callback). The shared
 * {@link #glut} field and the internal {@link GLU} / {@link GLUquadric}
 * singletons are not thread-safe; do not call these methods concurrently from
 * multiple GL contexts.
 */
public class Support3D {

	/**
	 * Shared GLUT utility instance used for stroke fonts, solid/wire primitives,
	 * and other GLUT geometry helpers.
	 *
	 * <p>This field is {@code public} so that callers can invoke GLUT methods not
	 * directly wrapped by this class (e.g. {@code glut.glutBitmapCharacter(...)}).
	 * It must only be used from the GL thread.
	 */
	public static GLUT glut = new GLUT();

	/**
	 * Shared GLU quadric used by {@link #drawTube}. Lazily initialised on first
	 * use. {@code private} because callers should go through the typed drawing
	 * methods rather than manipulating the quadric directly.
	 */
	private static GLUquadric _quad;

	/**
	 * Shared GLU instance. Lazily initialised via {@link #getGLU()}. GLU is
	 * stateless for the methods used here, so a single instance is safe for the
	 * lifetime of the application provided all calls happen on the same GL thread.
	 */
	private static GLU _glu;

	/**
	 * Returns the shared {@link GLU} instance, creating it on first call.
	 *
	 * @return the shared GLU instance (never {@code null})
	 */
	private static GLU getGLU() {
	    if (_glu == null) {
	        _glu = new GLU();
	    }
	    return _glu;
	}

	// -------------------------------------------------------------------------
	// Points
	// -------------------------------------------------------------------------

	/**
	 * Draws a set of points from a packed coordinate array.
	 *
	 * <p>All points are drawn with the same colour and pixel size. Pass
	 * {@code circular = true} to enable {@code GL_POINT_SMOOTH}, which asks the
	 * driver to rasterise each point as a circle rather than a square; support
	 * is driver-dependent.
	 *
	 * @param drawable the OpenGL drawable
	 * @param coords   packed point coordinates as {@code [x, y, z, x, y, z, ...]};
	 *                 {@code null} or empty arrays are silently ignored
	 * @param color    the point colour
	 * @param size     the point diameter in pixels
	 * @param circular {@code true} to request round (smooth) points;
	 *                 {@code false} for square points
	 */
	public static void drawPoints(GLAutoDrawable drawable, float coords[], Color color, float size, boolean circular) {
		if (coords == null || coords.length == 0) {
			return;
		}

		GL2 gl = drawable.getGL().getGL2();
		gl.glPointSize(size);

		int np = coords.length / 3;

		if (circular) {
			gl.glEnable(GL2ES1.GL_POINT_SMOOTH);
		} else {
			gl.glDisable(GL2ES1.GL_POINT_SMOOTH);
		}
		gl.glBegin(GL.GL_POINTS);
		setColor(gl, color);

		for (int i = 0; i < np; i++) {
			int j = i * 3;
			gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
		}

		gl.glEnd();
	}

	/**
	 * Draws a set of points with an optional contrasting frame ring.
	 *
	 * <p>When {@code frame} is non-{@code null}, each point is drawn twice: first
	 * at full {@code size} in the frame colour to create an outline, then at
	 * {@code size - 2} pixels in the fill colour on top. When {@code frame} is
	 * {@code null} this is equivalent to
	 * {@link #drawPoints(GLAutoDrawable, float[], Color, float, boolean)}.
	 *
	 * @param drawable the OpenGL drawable
	 * @param coords   packed point coordinates as {@code [x, y, z, x, y, z, ...]};
	 *                 {@code null} or empty arrays are silently ignored
	 * @param fill     the inner fill colour
	 * @param frame    the outer frame colour, or {@code null} for no frame
	 * @param size     the outer point diameter in pixels; the inner fill is drawn
	 *                 at {@code size - 2}
	 * @param circular {@code true} to request round (smooth) points
	 */
	public static void drawPoints(GLAutoDrawable drawable, float coords[], Color fill, Color frame, float size,
			boolean circular) {
		if (frame == null) {
			drawPoints(drawable, coords, fill, size, circular);
		} else {
			drawPoints(drawable, coords, frame, size, circular);
			drawPoints(drawable, coords, fill, size - 2, circular);
		}
	}

	/**
	 * Draws a single point at double-precision coordinates.
	 *
	 * <p>The coordinates are cast to {@code float} before submission to OpenGL.
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        the x coordinate
	 * @param y        the y coordinate
	 * @param z        the z coordinate
	 * @param color    the point colour
	 * @param size     the point diameter in pixels
	 * @param circular {@code true} to request round (smooth) points
	 */
	public static void drawPoint(GLAutoDrawable drawable, double x, double y, double z, Color color, float size,
			boolean circular) {
		drawPoint(drawable, (float) x, (float) y, (float) z, color, size, circular);
	}

	/**
	 * Draws a single point at float coordinates.
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        the x coordinate
	 * @param y        the y coordinate
	 * @param z        the z coordinate
	 * @param color    the point colour
	 * @param size     the point diameter in pixels
	 * @param circular {@code true} to request round (smooth) points
	 */
	public static void drawPoint(GLAutoDrawable drawable, float x, float y, float z, Color color, float size,
			boolean circular) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glPointSize(size);

		setColor(gl, color);
		if (circular) {
			gl.glEnable(GL2ES1.GL_POINT_SMOOTH);
		} else {
			gl.glDisable(GL2ES1.GL_POINT_SMOOTH);
		}

		gl.glBegin(GL.GL_POINTS);
		gl.glVertex3f(x, y, z);
		gl.glEnd();
	}

	/**
	 * Draws a labelled marker: a point accompanied by a text label rendered in
	 * screen space below it.
	 *
	 * <p>The label is drawn using GLUT's {@code STROKE_ROMAN} vector font,
	 * projected into a temporary 2D orthographic overlay so its pixel size remains
	 * constant regardless of zoom. The text is centred horizontally on the marker's
	 * screen x and positioned {@code markerSize/2 + 3} pixels below its screen y.
	 *
	 * <p><strong>Note:</strong> depth testing is temporarily disabled while the
	 * label is drawn so that it is never occluded by scene geometry.
	 *
	 * @param drawable    the OpenGL drawable
	 * @param x           the x coordinate of the marker in world space
	 * @param y           the y coordinate of the marker in world space
	 * @param z           the z coordinate of the marker in world space
	 * @param markerColor the colour of the point marker (the point itself is not
	 *                    drawn by this method; pass to
	 *                    {@link #drawPoint(GLAutoDrawable, float, float, float, Color, float, boolean)}
	 *                    separately if required)
	 * @param markerSize  the pixel diameter of the marker, used only for
	 *                    computing the label offset
	 * @param circular    {@code true} to request round (smooth) point rendering
	 * @param label       the text string to display; must not be {@code null}
	 * @param fontSize    scaling factor applied to the GLUT stroke font; values
	 *                    around {@code 0.07f–0.15f} produce readable results in
	 *                    typical scenes
	 * @param fontColor   the colour of the rendered text
	 */
	public static void drawMarker(GLAutoDrawable drawable, float x, float y, float z, Color markerColor,
			float markerSize, boolean circular, String label, float fontSize, Color fontColor) {
		GL2 gl = drawable.getGL().getGL2();

		GLU glu = getGLU();

		// Retrieve the current matrices and viewport to project the marker position.
		int[] viewport = new int[4];
		double[] modelview = new double[16];
		double[] projection = new double[16];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		gl.glGetDoublev(GLMatrixFunc.GL_MODELVIEW_MATRIX, modelview, 0);
		gl.glGetDoublev(GLMatrixFunc.GL_PROJECTION_MATRIX, projection, 0);

		// Map the marker's 3D position to window (screen) coordinates.
		double[] winCoords = new double[3];
		glu.gluProject(x, y, z, modelview, 0, projection, 0, viewport, 0, winCoords, 0);

		// Compute the text width in pixels using GLUT stroke font metrics.
		// glutStrokeWidth returns widths in the font's native coordinate system,
		// so we scale by fontSize to match the rendered size.
		float textWidth = 0;
		for (int i = 0; i < label.length(); i++) {
			textWidth += glut.glutStrokeWidth(GLUT.STROKE_ROMAN, label.charAt(i)) * fontSize;
		}

		// Centre the text horizontally on the marker and place it just below.
		double textWinX = winCoords[0] - textWidth / 2.0;
		double textWinY = winCoords[1] - markerSize / 2.0 - 3.0;

		// Switch to a 2D orthographic projection covering the entire viewport.
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(0, viewport[2], 0, viewport[3], -1, 1);

		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		// Disable depth testing so the label is never hidden by scene geometry.
		gl.glDisable(GL.GL_DEPTH_TEST);

		setColor(gl, fontColor);

		gl.glPushMatrix();
		gl.glTranslated(textWinX, textWinY, 0);
		gl.glScalef(fontSize, fontSize, fontSize);

		for (int i = 0; i < label.length(); i++) {
			glut.glutStrokeCharacter(GLUT.STROKE_ROMAN, label.charAt(i));
		}
		gl.glPopMatrix();

		// Restore depth testing and the saved matrices.
		gl.glEnable(GL.GL_DEPTH_TEST);

		gl.glPopMatrix();
		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
	}

	/**
	 * Draws a single point rendered as a point sprite.
	 *
	 * <p>Point sprites ({@code GL_POINT_SPRITE}) allow a texture to be mapped onto
	 * a point primitive. This overload enables the sprite state, draws the point,
	 * then leaves sprite state enabled for the caller to manage. If no texture is
	 * bound, the point will appear as a solid-coloured square.
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        the x coordinate
	 * @param y        the y coordinate
	 * @param z        the z coordinate
	 * @param color    the point colour
	 * @param size     the point sprite size in pixels
	 */
	public static void drawPoint(GLAutoDrawable drawable, float x, float y, float z, Color color, float size) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glPointSize(size);

		setColor(gl, color);
		gl.glEnable(GL2ES1.GL_POINT_SPRITE);
		gl.glBegin(GL.GL_POINTS);
		gl.glVertex3f(x, y, z);
		gl.glEnd();
	}

	// -------------------------------------------------------------------------
	// Spheres
	// -------------------------------------------------------------------------

	/**
	 * Draws a wireframe sphere centred at {@code (x, y, z)}.
	 *
	 * <p>The sphere is rendered using GLUT's {@code glutWireSphere}, which
	 * approximates the surface with longitude/latitude line strips.
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        x coordinate of the centre
	 * @param y        y coordinate of the centre
	 * @param z        z coordinate of the centre
	 * @param radius   radius in model-space units
	 * @param slices   number of subdivisions around the Z axis (longitude lines);
	 *                 higher values produce a smoother appearance
	 * @param stacks   number of subdivisions along the Z axis (latitude bands);
	 *                 higher values produce a smoother appearance
	 * @param color    the wire colour
	 */
	public static void wireSphere(GLAutoDrawable drawable, float x, float y, float z, float radius, int slices,
			int stacks, Color color) {
		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);
		gl.glPushMatrix();
		gl.glTranslatef(x, y, z);
		glut.glutWireSphere(radius, slices, stacks);
		gl.glPopMatrix();
	}

	/**
	 * Draws a solid (filled) sphere centred at {@code (x, y, z)}.
	 *
	 * <p>The sphere is rendered using GLUT's {@code glutSolidSphere} with flat
	 * shading and no lighting. For a shaded appearance see
	 * {@link #solidShadedSphere}.
	 *
	 * @param drawable the OpenGL drawable
	 * @param x        x coordinate of the centre
	 * @param y        y coordinate of the centre
	 * @param z        z coordinate of the centre
	 * @param radius   radius in model-space units
	 * @param slices   number of subdivisions around the Z axis
	 * @param stacks   number of subdivisions along the Z axis
	 * @param color    the fill colour
	 */
	public static void solidSphere(GLAutoDrawable drawable, float x, float y, float z, float radius, int slices,
			int stacks, Color color) {
		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);
		gl.glPushMatrix();
		gl.glTranslatef(x, y, z);
		glut.glutSolidSphere(radius, slices, stacks);
		gl.glPopMatrix();
	}

	/**
	 * Draws a solid sphere with optional Phong-style lighting.
	 *
	 * <p>When {@code enableLighting} is {@code true}, {@code GL_LIGHT0} is
	 * configured as a directional light at {@code (1, 1, 1)} with white diffuse
	 * and specular components, and a material with shininess 50 is applied. The
	 * lighting and material state are restored (lighting disabled) after drawing.
	 * When {@code enableLighting} is {@code false} the sphere is drawn with flat
	 * colour, identical to {@link #solidSphere}.
	 *
	 * @param drawable      the OpenGL drawable
	 * @param x             x coordinate of the centre
	 * @param y             y coordinate of the centre
	 * @param z             z coordinate of the centre
	 * @param radius        radius in model-space units
	 * @param slices        number of subdivisions around the Z axis
	 * @param stacks        number of subdivisions along the Z axis
	 * @param color         the sphere colour; also used as the material diffuse colour
	 *                      when lighting is enabled
	 * @param enableLighting {@code true} to enable a directional GL_LIGHT0 and
	 *                       Phong materials; {@code false} for flat colour
	 */
	public static void solidShadedSphere(GLAutoDrawable drawable, float x, float y, float z, float radius, int slices,
			int stacks, Color color, boolean enableLighting) {
		GL2 gl = drawable.getGL().getGL2();

		setColor(gl, color);

		if (enableLighting) {
			gl.glEnable(GLLightingFunc.GL_LIGHTING);
			gl.glEnable(GLLightingFunc.GL_LIGHT0);

			float[] lightPosition = { 1.0f, 1.0f, 1.0f, 0.0f }; // directional
			float[] lightDiffuse  = { 1.0f, 1.0f, 1.0f, 1.0f };
			float[] lightSpecular = { 1.0f, 1.0f, 1.0f, 1.0f };

			gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);
			gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE,  lightDiffuse,  0);
			gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_SPECULAR, lightSpecular, 0);

			float[] matAmbient   = { 0.2f, 0.2f, 0.2f, 1.0f };
			float[] matDiffuse   = { color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1.0f };
			float[] matSpecular  = { 1.0f, 1.0f, 1.0f, 1.0f };
			float[] matShininess = { 50.0f };

			gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT,   matAmbient,   0);
			gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_DIFFUSE,   matDiffuse,   0);
			gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SPECULAR,  matSpecular,  0);
			gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SHININESS, matShininess, 0);
		}

		gl.glPushMatrix();
		gl.glTranslatef(x, y, z);
		glut.glutSolidSphere(radius, slices, stacks);
		gl.glPopMatrix();

		if (enableLighting) {
			gl.glDisable(GLLightingFunc.GL_LIGHTING);
		}
	}

	/**
	 * Draws a solid spherical shell — a sphere with a hollow interior — by
	 * rendering an outer surface, an inner surface with inverted normals, and a
	 * connecting band of quads along each horizontal latitude strip.
	 *
	 * @param drawable    the OpenGL drawable
	 * @param cx          x coordinate of the centre
	 * @param cy          y coordinate of the centre
	 * @param cz          z coordinate of the centre
	 * @param innerRadius the radius of the hollow interior; must be less than
	 *                    {@code outerRadius}
	 * @param outerRadius the outer surface radius
	 * @param slices      number of subdivisions around the Z axis (longitude);
	 *                    higher values give a smoother silhouette
	 * @param stacks      number of subdivisions along the Z axis (latitude);
	 *                    higher values give a smoother silhouette
	 * @param color       the shell colour
	 */
	public static void solidSphereShell(GLAutoDrawable drawable, float cx, float cy, float cz, float innerRadius,
			float outerRadius, int slices, int stacks, Color color) {
		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);
		gl.glPushMatrix();
		gl.glTranslatef(cx, cy, cz);

		// Outer surface with outward-facing normals.
		drawSphereSurface(gl, outerRadius, slices, stacks, false);

		// Inner surface with inward-facing normals.
		drawSphereSurface(gl, innerRadius, slices, stacks, true);

		// Connect the outer and inner surfaces with a quad strip per latitude band.
		for (int i = 0; i < stacks; i++) {
			float theta1 = (float) (i * Math.PI / stacks);
			float theta2 = (float) ((i + 1) * Math.PI / stacks);
			gl.glBegin(GL2.GL_QUAD_STRIP);
			for (int j = 0; j <= slices; j++) {
				float phi      = (float) (j * 2 * Math.PI / slices);
				float sinTheta1 = (float) Math.sin(theta1);
				float cosTheta1 = (float) Math.cos(theta1);
				float sinTheta2 = (float) Math.sin(theta2);
				float cosTheta2 = (float) Math.cos(theta2);
				float sinPhi    = (float) Math.sin(phi);
				float cosPhi    = (float) Math.cos(phi);

				float xOuter1 = outerRadius * sinTheta1 * cosPhi;
				float yOuter1 = outerRadius * cosTheta1;
				float zOuter1 = outerRadius * sinTheta1 * sinPhi;

				float xOuter2 = outerRadius * sinTheta2 * cosPhi;
				float yOuter2 = outerRadius * cosTheta2;
				float zOuter2 = outerRadius * sinTheta2 * sinPhi;

				float xInner1 = innerRadius * sinTheta1 * cosPhi;
				float yInner1 = innerRadius * cosTheta1;
				float zInner1 = innerRadius * sinTheta1 * sinPhi;

				float xInner2 = innerRadius * sinTheta2 * cosPhi;
				float yInner2 = innerRadius * cosTheta2;
				float zInner2 = innerRadius * sinTheta2 * sinPhi;

				gl.glVertex3f(xOuter1, yOuter1, zOuter1);
				gl.glVertex3f(xInner1, yInner1, zInner1);
				gl.glVertex3f(xOuter2, yOuter2, zOuter2);
				gl.glVertex3f(xInner2, yInner2, zInner2);
			}
			gl.glEnd();
		}

		gl.glPopMatrix();
	}

	/**
	 * Draws one hemisphere-band surface of a sphere as a series of quad strips.
	 *
	 * <p>Used internally by {@link #solidSphereShell} to draw both the outer and
	 * inner surfaces of the shell.
	 *
	 * @param gl            the GL2 context
	 * @param radius        the sphere radius
	 * @param slices        number of subdivisions around the Z axis
	 * @param stacks        number of subdivisions along the Z axis
	 * @param invertNormals {@code true} to flip normals inward, used for the
	 *                      inner surface of a shell
	 */
	private static void drawSphereSurface(GL2 gl, float radius, int slices, int stacks, boolean invertNormals) {
		for (int i = 0; i < stacks; i++) {
			float theta1 = (float) (i * Math.PI / stacks);
			float theta2 = (float) ((i + 1) * Math.PI / stacks);
			gl.glBegin(GL2.GL_QUAD_STRIP);
			for (int j = 0; j <= slices; j++) {
				float phi       = (float) (j * 2 * Math.PI / slices);
				float sinTheta1 = (float) Math.sin(theta1);
				float cosTheta1 = (float) Math.cos(theta1);
				float sinTheta2 = (float) Math.sin(theta2);
				float cosTheta2 = (float) Math.cos(theta2);
				float sinPhi    = (float) Math.sin(phi);
				float cosPhi    = (float) Math.cos(phi);

				float x1 = radius * sinTheta1 * cosPhi;
				float y1 = radius * cosTheta1;
				float z1 = radius * sinTheta1 * sinPhi;
				float x2 = radius * sinTheta2 * cosPhi;
				float y2 = radius * cosTheta2;
				float z2 = radius * sinTheta2 * sinPhi;

				if (invertNormals) {
					gl.glNormal3f(-x1 / radius, -y1 / radius, -z1 / radius);
					gl.glVertex3f(x1, y1, z1);
					gl.glNormal3f(-x2 / radius, -y2 / radius, -z2 / radius);
					gl.glVertex3f(x2, y2, z2);
				} else {
					gl.glNormal3f(x1 / radius, y1 / radius, z1 / radius);
					gl.glVertex3f(x1, y1, z1);
					gl.glNormal3f(x2 / radius, y2 / radius, z2 / radius);
					gl.glVertex3f(x2, y2, z2);
				}
			}
			gl.glEnd();
		}
	}

	// -------------------------------------------------------------------------
	// Boxes and quads
	// -------------------------------------------------------------------------

	/**
	 * Draws a solid axis-aligned rectangular box centred at
	 * {@code (xc, yc, zc)} with a frame in a darker shade of the fill colour.
	 *
	 * <p>Equivalent to calling
	 * {@link #drawRectangularSolid(GLAutoDrawable, float, float, float, float, float, float, Color, Color, float, boolean)}
	 * with {@code lc = null} (the frame colour defaults to {@code fc.darker()}).
	 *
	 * @param drawable  the OpenGL drawable
	 * @param xc        x coordinate of the box centre
	 * @param yc        y coordinate of the box centre
	 * @param zc        z coordinate of the box centre
	 * @param xw        total width along the X axis
	 * @param yw        total width along the Y axis
	 * @param zw        total width along the Z axis
	 * @param fc        the fill colour for all six faces
	 * @param lineWidth the frame line width in pixels
	 * @param frame     {@code true} to draw a wireframe outline over the filled faces
	 */
	public static void drawRectangularSolid(GLAutoDrawable drawable, float xc, float yc, float zc, float xw, float yw,
			float zw, Color fc, float lineWidth, boolean frame) {
		drawRectangularSolid(drawable, xc, yc, zc, xw, yw, zw, fc, null, lineWidth, frame);
	}

	/**
	 * Draws a solid axis-aligned rectangular box centred at
	 * {@code (xc, yc, zc)} with an independent frame colour.
	 *
	 * <p>All six faces are filled with {@code fc}. When {@code frame} is
	 * {@code true} a wireframe outline is superimposed using {@code lc}; if
	 * {@code lc} is {@code null} the frame is drawn in {@code fc.darker()}.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param xc        x coordinate of the box centre
	 * @param yc        y coordinate of the box centre
	 * @param zc        z coordinate of the box centre
	 * @param xw        total width along the X axis
	 * @param yw        total width along the Y axis
	 * @param zw        total width along the Z axis
	 * @param fc        the fill colour for all six faces
	 * @param lc        the frame (outline) colour, or {@code null} to use
	 *                  {@code fc.darker()}
	 * @param lineWidth the frame line width in pixels
	 * @param frame     {@code true} to draw a wireframe outline over the filled faces
	 */
	public static void drawRectangularSolid(GLAutoDrawable drawable, float xc, float yc, float zc, float xw, float yw,
			float zw, Color fc, Color lc, float lineWidth, boolean frame) {
		GL2 gl = drawable.getGL().getGL2();

		float xm = xc - xw / 2;
		float xp = xc + xw / 2;
		float ym = yc - yw / 2;
		float yp = yc + yw / 2;
		float zm = zc - zw / 2;
		float zp = zc + zw / 2;

		Support3D.setColor(gl, fc);
		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, ym, zp);
		gl.glVertex3f(xm, yp, zp);
		gl.glVertex3f(xp, yp, zp);
		gl.glVertex3f(xp, ym, zp);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, ym, zm);
		gl.glVertex3f(xm, yp, zm);
		gl.glVertex3f(xp, yp, zm);
		gl.glVertex3f(xp, ym, zm);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, yp, zm);
		gl.glVertex3f(xm, yp, zp);
		gl.glVertex3f(xp, yp, zp);
		gl.glVertex3f(xp, yp, zm);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, ym, zm);
		gl.glVertex3f(xm, ym, zp);
		gl.glVertex3f(xp, ym, zp);
		gl.glVertex3f(xp, ym, zm);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xp, yp, zm);
		gl.glVertex3f(xp, yp, zp);
		gl.glVertex3f(xp, ym, zp);
		gl.glVertex3f(xp, ym, zm);
		gl.glEnd();

		gl.glBegin(GL2ES3.GL_QUADS);
		gl.glVertex3f(xm, yp, zm);
		gl.glVertex3f(xm, yp, zp);
		gl.glVertex3f(xm, ym, zp);
		gl.glVertex3f(xm, ym, zm);
		gl.glEnd();

		if (frame) {
			if (lc == null) {
				lc = fc.darker();
			}
			Support3D.setColor(gl, lc);

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, yp, zm);
			gl.glVertex3f(xm, yp, zp);
			gl.glVertex3f(xm, ym, zp);
			gl.glVertex3f(xm, ym, zm);
			gl.glVertex3f(xm, yp, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, ym, zm);
			gl.glVertex3f(xm, yp, zm);
			gl.glVertex3f(xp, yp, zm);
			gl.glVertex3f(xp, ym, zm);
			gl.glVertex3f(xm, ym, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, yp, zm);
			gl.glVertex3f(xm, yp, zp);
			gl.glVertex3f(xp, yp, zp);
			gl.glVertex3f(xp, yp, zm);
			gl.glVertex3f(xm, yp, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, ym, zm);
			gl.glVertex3f(xm, ym, zp);
			gl.glVertex3f(xp, ym, zp);
			gl.glVertex3f(xp, ym, zm);
			gl.glVertex3f(xm, ym, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xp, yp, zm);
			gl.glVertex3f(xp, yp, zp);
			gl.glVertex3f(xp, ym, zp);
			gl.glVertex3f(xp, ym, zm);
			gl.glVertex3f(xp, yp, zm);
			gl.glEnd();

			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(xm, yp, zm);
			gl.glVertex3f(xm, yp, zp);
			gl.glVertex3f(xm, ym, zp);
			gl.glVertex3f(xm, ym, zm);
			gl.glVertex3f(xm, yp, zm);
			gl.glEnd();
		}
		gl.glLineWidth(1f);
	}

	/**
	 * Draws a batch of quads from a packed coordinate array, with an optional
	 * frame drawn in a darker shade of the fill colour.
	 *
	 * <p>Equivalent to
	 * {@link #drawQuads(GLAutoDrawable, float[], Color, Color, float)} with
	 * {@code lineColor = frame ? color.darker() : null}.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    packed quad vertices as {@code [x, y, z, ...]};
	 *                  every 12 floats (4 vertices × 3 coordinates) describe one quad
	 * @param color     the fill colour
	 * @param lineWidth the frame line width in pixels
	 * @param frame     {@code true} to draw a wireframe outline around each quad
	 */
	public static void drawQuads(GLAutoDrawable drawable, float coords[], Color color, float lineWidth, boolean frame) {
		drawQuads(drawable, coords, color, (frame ? color.darker() : null), lineWidth);
	}

	/**
	 * Draws a batch of quads from a packed coordinate array with an independent
	 * frame colour.
	 *
	 * <p>All vertices in {@code coords} are submitted as a single
	 * {@code GL_QUADS} primitive. When {@code lineColor} is non-{@code null},
	 * each group of four vertices is additionally drawn as a closed
	 * {@code GL_LINE_STRIP} to produce a per-quad outline.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    packed quad vertices as {@code [x, y, z, ...]};
	 *                  every 12 floats describe one quad
	 * @param color     the fill colour
	 * @param lineColor the frame colour, or {@code null} for no frame
	 * @param lineWidth the frame line width in pixels
	 */
	public static void drawQuads(GLAutoDrawable drawable, float coords[], Color color, Color lineColor,
			float lineWidth) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		gl.glBegin(GL2ES3.GL_QUADS);
		setColor(gl, color);

		int numPoints = coords.length / 3;
		for (int i = 0; i < numPoints; i++) {
			int j = 3 * i;
			gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
		}
		gl.glEnd();

		if (lineColor != null) {
			// Each quad has 4 vertices = 12 floats.
			int numQuad = coords.length / 12;
			for (int i = 0; i < numQuad; i++) {
				gl.glBegin(GL.GL_LINE_STRIP);
				setColor(gl, lineColor);

				int j = i * 12;
				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);
				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);
				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);
				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);

				// Close the loop back to the first vertex of this quad.
				j = i * 12;
				gl.glVertex3f(coords[j++], coords[j++], coords[j++]);

				gl.glEnd();
			}
		}

		gl.glLineWidth(1f);
	}

	/**
	 * Draws a single quad addressed by four vertex indices into a shared
	 * coordinate array, with an optional frame in a darker shade.
	 *
	 * <p>The indices are "triple indices": index {@code k} refers to the point
	 * at {@code coords[3k], coords[3k+1], coords[3k+2]}.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    the shared coordinate pool as {@code [x, y, z, ...]}
	 * @param index1    triple index of the first vertex
	 * @param index2    triple index of the second vertex
	 * @param index3    triple index of the third vertex
	 * @param index4    triple index of the fourth vertex
	 * @param color     the fill colour
	 * @param lineWidth the frame line width in pixels
	 * @param frame     {@code true} to outline the quad in {@code color.darker()}
	 */
	public static void drawQuad(GLAutoDrawable drawable, float coords[], int index1, int index2, int index3, int index4,
			Color color, float lineWidth, boolean frame) {
		int i1 = 3 * index1;
		int i2 = 3 * index2;
		int i3 = 3 * index3;
		int i4 = 3 * index4;

		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		gl.glBegin(GL2ES3.GL_QUADS);
		setColor(gl, color);
		gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
		gl.glVertex3f(coords[i2], coords[i2 + 1], coords[i2 + 2]);
		gl.glVertex3f(coords[i3], coords[i3 + 1], coords[i3 + 2]);
		gl.glVertex3f(coords[i4], coords[i4 + 1], coords[i4 + 2]);
		gl.glEnd();

		if (frame) {
			gl.glBegin(GL.GL_LINE_STRIP);
			setColor(gl, color.darker());
			gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
			gl.glVertex3f(coords[i2], coords[i2 + 1], coords[i2 + 2]);
			gl.glVertex3f(coords[i3], coords[i3 + 1], coords[i3 + 2]);
			gl.glVertex3f(coords[i4], coords[i4 + 1], coords[i4 + 2]);
			gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
			gl.glEnd();
		}

		gl.glLineWidth(1f);
	}

	// -------------------------------------------------------------------------
	// Triangles
	// -------------------------------------------------------------------------

	/**
	 * Draws a batch of triangles from a packed coordinate array.
	 *
	 * <p>Every 9 floats in {@code coords} describe one triangle
	 * ({@code [x1,y1,z1, x2,y2,z2, x3,y3,z3]}). Each triangle is drawn by
	 * delegating to
	 * {@link #drawTriangle(GLAutoDrawable, float[], int, int, int, Color, float, boolean)}.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    packed triangle vertices as {@code [x, y, z, ...]};
	 *                  length must be a multiple of 9
	 * @param color     the fill colour
	 * @param lineWidth the frame line width in pixels
	 * @param frame     {@code true} to outline each triangle in {@code color.darker()}
	 */
	public static void drawTriangles(GLAutoDrawable drawable, float coords[], Color color, float lineWidth,
			boolean frame) {
		int numTriangle = coords.length / 9;
		for (int i = 0; i < numTriangle; i++) {
			int j = 3 * i;
			drawTriangle(drawable, coords, j, j + 1, j + 2, color, lineWidth, frame);
		}
	}

	/**
	 * Draws a single triangle addressed by three vertex indices into a shared
	 * coordinate array, with an optional frame in a darker shade.
	 *
	 * <p>The indices are "triple indices": index {@code k} refers to the point
	 * at {@code coords[3k], coords[3k+1], coords[3k+2]}.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    the shared coordinate pool as {@code [x, y, z, ...]}
	 * @param index1    triple index of the first vertex
	 * @param index2    triple index of the second vertex
	 * @param index3    triple index of the third vertex
	 * @param color     the fill colour
	 * @param lineWidth the frame line width in pixels
	 * @param frame     {@code true} to outline the triangle in {@code color.darker()}
	 */
	public static void drawTriangle(GLAutoDrawable drawable, float coords[], int index1, int index2, int index3,
			Color color, float lineWidth, boolean frame) {
		int i1 = 3 * index1;
		int i2 = 3 * index2;
		int i3 = 3 * index3;

		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		gl.glBegin(GL.GL_TRIANGLES);
		setColor(gl, color);
		gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
		gl.glVertex3f(coords[i2], coords[i2 + 1], coords[i2 + 2]);
		gl.glVertex3f(coords[i3], coords[i3 + 1], coords[i3 + 2]);
		gl.glEnd();

		if (frame) {
			gl.glBegin(GL.GL_LINE_STRIP);
			setColor(gl, color.darker());
			gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
			gl.glVertex3f(coords[i2], coords[i2 + 1], coords[i2 + 2]);
			gl.glVertex3f(coords[i3], coords[i3 + 1], coords[i3 + 2]);
			gl.glVertex3f(coords[i1], coords[i1 + 1], coords[i1 + 2]);
			gl.glEnd();
		}
		gl.glLineWidth(1f);
	}

	/**
	 * Subdivides a triangle into {@code 4^level} smaller triangles by recursively
	 * connecting edge midpoints.
	 *
	 * <p>At {@code level = 1} the input triangle is split into 4. At
	 * {@code level = 2} each of those 4 is split again, yielding 16, and so on.
	 * The resulting triangles lie in the same plane as the original (no projection
	 * onto a sphere is applied); for spherical subdivision, normalise the vertices
	 * after calling this method.
	 *
	 * @param coords the input triangle as {@code [x1, y1, z1, x2, y2, z2, x3, y3, z3]}
	 * @param level  subdivision depth; values less than 1 return {@code coords}
	 *               unchanged; each level multiplies the triangle count by 4
	 * @return a packed coordinate array containing all output triangles in the
	 *         same {@code [x, y, z, ...]} layout; length is
	 *         {@code 9 * 4^max(level,0)}
	 */
	public static float[] triangulateTriangle(float coords[], int level) {
		if (level < 1) {
			return coords;
		}
		float tricoords[] = oneToFourTriangle(coords);

		for (int lev = 2; lev <= level; lev++) {
			int numtri    = tricoords.length / 9;
			int numNewTri = 4 * numtri;
			float tri[][]  = new float[numtri][];
			float allTris[] = new float[9 * numNewTri];
			for (int i = 0; i < numtri; i++) {
				tri[i] = oneToFourTriangle(tricoords, i);
				System.arraycopy(tri[i], 0, allTris, 36 * i, 36);
			}
			tricoords = allTris;
		}
		return tricoords;
	}

	/**
	 * Splits one triangle — addressed by a triple index into a packed array —
	 * into four by connecting its three edge midpoints.
	 *
	 * <p>The result is returned as a new packed array of 36 floats
	 * (4 triangles × 3 vertices × 3 coordinates). The winding order of the
	 * four output triangles matches the winding order of the input.
	 *
	 * @param coords the packed coordinate array containing at least
	 *               {@code 3 * (index + 3)} floats
	 * @param index  triple index of the first vertex of the triangle to split;
	 *               vertices are at triple indices {@code index}, {@code index+1},
	 *               {@code index+2}
	 * @return a 36-element array containing the four output triangles
	 */
	public static float[] oneToFourTriangle(float coords[], int index) {
		Vector3f p[] = new Vector3f[6];

		int j = 3 * index;

		p[0] = new Vector3f(coords, j + 0);
		p[1] = new Vector3f(coords, j + 1);
		p[2] = new Vector3f(coords, j + 2);

		p[3] = Vector3f.midpoint(p[0], p[1]);
		p[4] = Vector3f.midpoint(p[1], p[2]);
		p[5] = Vector3f.midpoint(p[2], p[0]);

		float coords4[] = new float[36];

		fillCoords(coords4, 0, p[0], p[3], p[5]);
		fillCoords(coords4, 1, p[1], p[3], p[4]);
		fillCoords(coords4, 2, p[3], p[4], p[5]);
		fillCoords(coords4, 3, p[2], p[4], p[5]);
		return coords4;
	}

	/**
	 * Splits a triangle given as a self-contained 9-element array into four
	 * smaller triangles by connecting its three edge midpoints.
	 *
	 * <p>Equivalent to {@link #oneToFourTriangle(float[], int)} with
	 * {@code index = 0}.
	 *
	 * @param coords the triangle as {@code [x1, y1, z1, x2, y2, z2, x3, y3, z3]};
	 *               must have length &ge; 9
	 * @return a 36-element array containing the four output triangles
	 */
	public static float[] oneToFourTriangle(float coords[]) {
		return oneToFourTriangle(coords, 0);
	}

	/**
	 * Writes a sequence of {@link Vector3f} points into a packed coordinate array
	 * starting at the position corresponding to triple index {@code index}.
	 *
	 * @param coords the destination array; must be large enough to hold the data
	 * @param index  triple index of the first point to write
	 * @param p      the points to write, in order
	 */
	private static void fillCoords(float coords[], int index, Vector3f... p) {
		int size = 3 * p.length;
		int i    = size * index;
		for (Vector3f v3f : p) {
			coords[i++] = v3f.x;
			coords[i++] = v3f.y;
			coords[i++] = v3f.z;
		}
	}

	// -------------------------------------------------------------------------
	// Lines and polylines
	// -------------------------------------------------------------------------

	/**
	 * Draws a line segment from a start point along a unit-vector direction for
	 * a given length.
	 *
	 * <p>The endpoint is computed as
	 * {@code (x1 + length*ux, y1 + length*uy, z1 + length*uz)} and the call
	 * delegates to
	 * {@link #drawLine(GLAutoDrawable, float, float, float, float, float, float, Color, float)}.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param x1        x coordinate of the start point
	 * @param y1        y coordinate of the start point
	 * @param z1        z coordinate of the start point
	 * @param ux        x component of the unit direction vector
	 * @param uy        y component of the unit direction vector
	 * @param uz        z component of the unit direction vector
	 * @param length    the length of the line segment in model-space units
	 * @param color     the line colour
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, float x1, float y1, float z1, float ux, float uy, float uz,
			float length, Color color, float lineWidth) {
		float x2 = x1 + length * ux;
		float y2 = y1 + length * uy;
		float z2 = z1 + length * uz;
		drawLine(drawable, x1, y1, z1, x2, y2, z2, color, lineWidth);
	}

	/**
	 * Draws a line segment between two explicit float endpoints.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param x1        x coordinate of the first endpoint
	 * @param y1        y coordinate of the first endpoint
	 * @param z1        z coordinate of the first endpoint
	 * @param x2        x coordinate of the second endpoint
	 * @param y2        y coordinate of the second endpoint
	 * @param z2        z coordinate of the second endpoint
	 * @param color     the line colour
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, float x1, float y1, float z1, float x2, float y2, float z2,
			Color color, float lineWidth) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		gl.glBegin(GL.GL_LINES);
		setColor(gl, color);
		gl.glVertex3f(x1, y1, z1);
		gl.glVertex3f(x2, y2, z2);
		gl.glEnd();
		gl.glLineWidth(1f);
	}

	/**
	 * Draws a line segment between two explicit double endpoints.
	 *
	 * <p>The coordinates are cast to {@code float} before submission to OpenGL.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param x1        x coordinate of the first endpoint
	 * @param y1        y coordinate of the first endpoint
	 * @param z1        z coordinate of the first endpoint
	 * @param x2        x coordinate of the second endpoint
	 * @param y2        y coordinate of the second endpoint
	 * @param z2        z coordinate of the second endpoint
	 * @param color     the line colour
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, double x1, double y1, double z1, double x2, double y2,
			double z2, Color color, float lineWidth) {
		drawLine(drawable, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, color, lineWidth);
	}

	/**
	 * Draws a line segment between two endpoints supplied as {@code float[3]}
	 * arrays.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param p0        first endpoint as {@code [x, y, z]}; must have length &ge; 3
	 * @param p1        second endpoint as {@code [x, y, z]}; must have length &ge; 3
	 * @param color     the line colour
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, float[] p0, float[] p1, Color color, float lineWidth) {
		drawLine(drawable, p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], color, lineWidth);
	}

	/**
	 * Draws a line segment from a packed six-element coordinate array.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    the line as {@code [x1, y1, z1, x2, y2, z2]};
	 *                  must have length &ge; 6
	 * @param color     the line colour
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, float[] coords, Color color, float lineWidth) {
		drawLine(drawable, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5], color, lineWidth);
	}

	/**
	 * Draws a line segment rendered in two alternating stipple colours.
	 *
	 * <p>The segment is drawn twice using {@code GL_LINE_STIPPLE}: once with
	 * pattern {@code 0x00FF} (first half solid) in {@code color1}, and once with
	 * pattern {@code 0xFF00} (second half solid) in {@code color2}. Either colour
	 * may be {@code null} to suppress that pass. The combined effect produces a
	 * two-colour dashed appearance.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param x1        x coordinate of the first endpoint
	 * @param y1        y coordinate of the first endpoint
	 * @param z1        z coordinate of the first endpoint
	 * @param x2        x coordinate of the second endpoint
	 * @param y2        y coordinate of the second endpoint
	 * @param z2        z coordinate of the second endpoint
	 * @param color1    the first stipple colour, or {@code null} to skip
	 * @param color2    the second stipple colour, or {@code null} to skip
	 * @param lineWidth the line width in pixels
	 */
	public static void drawLine(GLAutoDrawable drawable, float x1, float y1, float z1, float x2, float y2, float z2,
			Color color1, Color color2, float lineWidth) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glEnable(GL2.GL_LINE_STIPPLE);
		gl.glLineWidth(lineWidth);

		if (color1 != null) {
			gl.glLineStipple(1, (short) 0x00FF);
			gl.glBegin(GL.GL_LINES);
			setColor(gl, color1);
			gl.glVertex3f(x1, y1, z1);
			gl.glVertex3f(x2, y2, z2);
			gl.glEnd();
		}
		if (color2 != null) {
			gl.glLineStipple(1, (short) 0xFF00);
			gl.glBegin(GL.GL_LINES);
			setColor(gl, color2);
			gl.glVertex3f(x1, y1, z1);
			gl.glVertex3f(x2, y2, z2);
			gl.glEnd();
		}

		gl.glDisable(GL2.GL_LINE_STIPPLE);
		gl.glLineWidth(1f);
	}

	/**
	 * Draws an open polyline connecting all vertices in a packed coordinate array.
	 *
	 * <p>Every three consecutive floats describe one vertex. At least two vertices
	 * are required for visible output; the method does not guard against fewer.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    packed vertex coordinates as {@code [x, y, z, ...]}
	 * @param color     the line colour
	 * @param lineWidth the line width in pixels
	 */
	public static void drawPolyLine(GLAutoDrawable drawable, float[] coords, Color color, float lineWidth) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);

		int np = coords.length / 3;

		gl.glBegin(GL.GL_LINE_STRIP);
		setColor(gl, color);

		for (int i = 0; i < np; i++) {
			int j = i * 3;
			gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
		}
		gl.glEnd();
		gl.glLineWidth(1f);
	}

	/**
	 * Draws an open polyline using only the first {@code livePoints} vertices of a
	 * (possibly oversized) coordinate buffer.
	 *
	 * <p>This overload exists to avoid trimming a backing buffer to its live
	 * length before drawing — the caller supplies the count explicitly. It is used
	 * by {@link edu.cnu.mdi.mdi3D.item3D.Trajectory3D} to achieve zero-copy
	 * append semantics. If {@code livePoints} is less than 2, nothing is drawn.
	 *
	 * @param drawable   the OpenGL drawable
	 * @param coords     the coordinate buffer as {@code [x, y, z, ...]}; may be
	 *                   larger than {@code 3 * livePoints}; only indices
	 *                   {@code [0, 3 * livePoints)} are read
	 * @param livePoints the number of valid vertices to draw; values less than 2
	 *                   produce no output
	 * @param color      the line colour
	 * @param lineWidth  the line width in pixels
	 */
	public static void drawPolyLine(GLAutoDrawable drawable, float[] coords, int livePoints,
	        Color color, float lineWidth) {
	    if (livePoints < 2) {
	    	return;
	    }
	    GL2 gl = drawable.getGL().getGL2();
	    gl.glLineWidth(lineWidth);
	    gl.glBegin(GL.GL_LINE_STRIP);
	    setColor(gl, color);
	    for (int i = 0; i < livePoints; i++) {
	        int j = i * 3;
	        gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
	    }
	    gl.glEnd();
	    gl.glLineWidth(1f);
	}

	/**
	 * Draws an open polyline rendered in two alternating stipple colours.
	 *
	 * <p>The polyline is drawn twice using {@code GL_LINE_STIPPLE}: once with
	 * pattern {@code 0x00FF} in {@code color1} and once with pattern {@code 0xFF00}
	 * in {@code color2}. Either colour may be {@code null} to suppress that pass.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param coords    packed vertex coordinates as {@code [x, y, z, ...]}
	 * @param color1    the first stipple colour, or {@code null} to skip
	 * @param color2    the second stipple colour, or {@code null} to skip
	 * @param lineWidth the line width in pixels
	 */
	public static void drawPolyLine(GLAutoDrawable drawable, float[] coords, Color color1, Color color2,
			float lineWidth) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glLineWidth(lineWidth);
		gl.glEnable(GL2.GL_LINE_STIPPLE);

		int np = coords.length / 3;

		if (color1 != null) {
			gl.glLineStipple(1, (short) 0x00FF);
			gl.glBegin(GL.GL_LINE_STRIP);
			setColor(gl, color1);
			for (int i = 0; i < np; i++) {
				int j = i * 3;
				gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
			}
			gl.glEnd();
		}
		if (color2 != null) {
			gl.glLineStipple(1, (short) 0xFF00);
			gl.glBegin(GL.GL_LINE_STRIP);
			setColor(gl, color2);
			for (int i = 0; i < np; i++) {
				int j = i * 3;
				gl.glVertex3f(coords[j], coords[j + 1], coords[j + 2]);
			}
			gl.glEnd();
		}

		gl.glDisable(GL2.GL_LINE_STIPPLE);
		gl.glLineWidth(1f);
	}

	// -------------------------------------------------------------------------
	// Cones and tubes
	// -------------------------------------------------------------------------

	/**
	 * Draws a solid cone with its base centred at {@code (x1, y1, z1)} and its
	 * tip at {@code (x2, y2, z2)}.
	 *
	 * <p>The cone is oriented along the vector from base to tip using a single
	 * {@code glRotatef} call. The rotation axis degenerates when the direction
	 * vector is exactly parallel to the Z axis; a small epsilon ({@code 1e-5}) is
	 * applied to the Z component to avoid this in practice.
	 *
	 * @param drawable the OpenGL drawable
	 * @param x1       x coordinate of the base centre
	 * @param y1       y coordinate of the base centre
	 * @param z1       z coordinate of the base centre
	 * @param x2       x coordinate of the tip
	 * @param y2       y coordinate of the tip
	 * @param z2       z coordinate of the tip
	 * @param radius   radius of the base circle in model-space units
	 * @param color    the cone colour
	 */
	public static void drawCone(GLAutoDrawable drawable, float x1, float y1, float z1, float x2, float y2, float z2,
			float radius, Color color) {
		float vx = x2 - x1;
		float vy = y2 - y1;
		float vz = z2 - z1;
		if (Math.abs(vz) < 1.0e-5) {
			vz = 0.0001f;
		}

		float v  = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
		float ax = (float) (57.2957795 * Math.acos(vz / v));
		if (vz < 0.0) {
			ax = -ax;
		}
		float rx = -vy * vz;
		float ry = vx * vz;

		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);

		gl.glPushMatrix();
		gl.glTranslatef(x1, y1, z1);
		gl.glRotatef(ax, rx, ry, 0f);
		glut.glutSolidCone(radius, v, 20, 20);
		gl.glPopMatrix();
	}

	/**
	 * Draws a solid cylindrical tube between two endpoints.
	 *
	 * <p>The tube is rendered as a GLU cylinder oriented along the vector from
	 * {@code (x1,y1,z1)} to {@code (x2,y2,z2)} with 50 radial subdivisions and
	 * no end caps. The same axis-alignment approach as {@link #drawCone} is used;
	 * the same Z-axis epsilon applies.
	 *
	 * @param drawable the OpenGL drawable
	 * @param x1       x coordinate of the first end
	 * @param y1       y coordinate of the first end
	 * @param z1       z coordinate of the first end
	 * @param x2       x coordinate of the second end
	 * @param y2       y coordinate of the second end
	 * @param z2       z coordinate of the second end
	 * @param radius   the tube radius in model-space units
	 * @param color    the tube colour
	 */
	public static void drawTube(GLAutoDrawable drawable, float x1, float y1, float z1, float x2, float y2, float z2,
			float radius, Color color) {
		GLU glu = getGLU();

		if (_quad == null) {
			_quad = glu.gluNewQuadric();
		}

		float vx = x2 - x1;
		float vy = y2 - y1;
		float vz = z2 - z1;
		if (Math.abs(vz) < 1.0e-5) {
			vz = 0.0001f;
		}

		float v  = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
		float ax = (float) (57.2957795 * Math.acos(vz / v));
		if (vz < 0.0) {
			ax = -ax;
		}
		float rx = -vy * vz;
		float ry = vx * vz;

		GL2 gl = drawable.getGL().getGL2();
		setColor(gl, color);

		gl.glPushMatrix();
		gl.glTranslatef(x1, y1, z1);
		gl.glRotatef(ax, rx, ry, 0f);
		glu.gluCylinder(_quad, radius, radius, v, 50, 1);
		gl.glPopMatrix();
	}

	// -------------------------------------------------------------------------
	// Spherical geometry
	// -------------------------------------------------------------------------

	/**
	 * Draws a filled and/or outlined spherical polygon defined by vertices in
	 * spherical coordinates.
	 *
	 * <p>The polygon vertices are supplied in spherical coordinates as alternating
	 * {@code (theta, phi)} pairs in radians, where {@code theta} is the polar
	 * angle from the positive Z axis ({@code [0, π]}) and {@code phi} is the
	 * azimuthal angle in the XY plane ({@code [-π, π]}). They are converted
	 * internally to Cartesian coordinates on the surface of a sphere of the given
	 * {@code radius}.
	 *
	 * <p>The fill is drawn as a {@code GL_POLYGON} primitive; the outline is drawn
	 * as a {@code GL_LINE_LOOP}. Either pass may be suppressed by passing
	 * {@code null} for the corresponding colour.
	 *
	 * @param drawable  the OpenGL drawable
	 * @param radius    the sphere radius on whose surface the polygon is drawn
	 * @param coords    spherical coordinate pairs as
	 *                  {@code [theta0, phi0, theta1, phi1, ...]};
	 *                  length must be even
	 * @param lineColor the outline colour, or {@code null} for no outline
	 * @param fillColor the fill colour, or {@code null} for no fill
	 * @param lineWidth the outline line width in pixels
	 */
	public static void drawSphericalPolygon(GLAutoDrawable drawable, float radius, float[] coords, Color lineColor,
			Color fillColor, float lineWidth) {
		GL2 gl = drawable.getGL().getGL2();

		int numPoints = coords.length / 2;
		float[] cartesianCoords = new float[numPoints * 3];

		for (int i = 0; i < numPoints; i++) {
			float theta = coords[2 * i];
			float phi   = coords[2 * i + 1];

			cartesianCoords[3 * i]     = radius * (float) (Math.sin(theta) * Math.cos(phi));
			cartesianCoords[3 * i + 1] = radius * (float) (Math.sin(theta) * Math.sin(phi));
			cartesianCoords[3 * i + 2] = radius * (float) Math.cos(theta);
		}

		if (fillColor != null) {
			gl.glBegin(GL2.GL_POLYGON);
			Support3D.setColor(gl, fillColor);
			for (int i = 0; i < numPoints; i++) {
				gl.glVertex3f(cartesianCoords[3 * i], cartesianCoords[3 * i + 1], cartesianCoords[3 * i + 2]);
			}
			gl.glEnd();
		}

		if (lineColor != null) {
			gl.glLineWidth(lineWidth);
			gl.glBegin(GL.GL_LINE_LOOP);
			Support3D.setColor(gl, lineColor);
			for (int i = 0; i < numPoints; i++) {
				gl.glVertex3f(cartesianCoords[3 * i], cartesianCoords[3 * i + 1], cartesianCoords[3 * i + 2]);
			}
			gl.glEnd();
		}

		gl.glLineWidth(1.0f);
	}

	// -------------------------------------------------------------------------
	// Colour and utility
	// -------------------------------------------------------------------------

	/**
	 * Sets the current OpenGL drawing colour from an AWT {@link Color}.
	 *
	 * <p>All four components (red, green, blue, alpha) are normalised from the
	 * AWT range {@code [0, 255]} to the OpenGL range {@code [0.0, 1.0]} and
	 * submitted via {@code glColor4f}.
	 *
	 * @param gl    the GL2 context
	 * @param color the AWT colour to apply; must not be {@code null}
	 */
	public static void setColor(GL2 gl, Color color) {
		float r = color.getRed()   / 255f;
		float g = color.getGreen() / 255f;
		float b = color.getBlue()  / 255f;
		float a = color.getAlpha() / 255f;
		gl.glColor4f(r, g, b, a);
	}

	/**
	 * Convenience factory that converts a varargs list of {@code float} values
	 * into a {@code float[]}.
	 *
	 * <p>This is syntactic sugar for building inline coordinate arrays:
	 * <pre>
	 *   Support3D.drawPolyLine(drawable,
	 *       Support3D.toArray(0f, 0f, 0f,  1f, 0f, 0f,  1f, 1f, 0f),
	 *       Color.WHITE, 1f);
	 * </pre>
	 *
	 * @param v the float values; may be empty
	 * @return the same values as a {@code float[]}; the array is the varargs
	 *         array itself, so no copy is made
	 */
	public static float[] toArray(float... v) {
		return v;
	}

}