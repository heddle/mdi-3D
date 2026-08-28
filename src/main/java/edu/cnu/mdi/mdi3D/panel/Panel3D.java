package edu.cnu.mdi.mdi3D.panel;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;

import edu.cnu.mdi.dialog.DialogUtils;
import edu.cnu.mdi.mdi3D.adapter3D.KeyBindings3D;
import edu.cnu.mdi.mdi3D.adapter3D.MouseAdapter3D;
import edu.cnu.mdi.mdi3D.item3D.Item3D;

/**
 * Swing component that hosts a JOGL {@link GLJPanel} and manages a simple
 * interactive 3D scene.
 *
 * <p>
 * {@code Panel3D} is the central drawing panel used by the MDI-3D extension.
 * It owns the OpenGL panel, the list of {@link Item3D} objects to render, the
 * current camera translation, the current scale, and the current orientation.
 * Rotation is stored as a quaternion so mouse-drag arcball rotations compose
 * smoothly without accumulating Euler-angle artifacts.
 * </p>
 *
 * <p>
 * Subclasses normally customize a panel by overriding
 * {@link #createInitialItems()}, {@link #beforeDraw(GLAutoDrawable)}, or
 * {@link #afterDraw(GLAutoDrawable)}. Optional Swing components can be placed
 * around the OpenGL canvas by overriding the directional hook methods
 * {@link #addNorth()}, {@link #addSouth()}, {@link #addEast()}, and
 * {@link #addWest()}.
 * </p>
 *
 * <p>
 * Rendering uses two passes. Opaque items are drawn first with depth writes
 * enabled. Transparent items are then sorted approximately back-to-front and
 * drawn with blending enabled and depth writes disabled. This is not a full
 * order-independent transparency solution, but it is appropriate for the simple
 * educational and diagnostic scenes used by the MDI-3D demos.
 * </p>
 *
 * <p>
 * The panel supports both unit-scale and large-world scenes. Keyboard panning
 * and mouse-wheel navigation use a configurable navigation step; the default
 * preserves the original small-scene behavior, while larger scenes can call
 * {@link #setNavigationStep(float)} or {@link #setNavigationStepFromExtent(float)}.
 * </p>
 */
@SuppressWarnings("serial")
public class Panel3D extends JPanel implements GLEventListener {

	/** Default background component used for red, green, and blue. */
	public static final float BG_DEFAULT = 0.9804f;

	/** Alpha values below this cutoff are treated as transparent. */
	private static final int OPAQUE_ALPHA_CUTOFF = 250;

	// the actual components of the background
	private float _bgRed = BG_DEFAULT;
	private float _bgGreen = BG_DEFAULT;
	private float _bgBlue = BG_DEFAULT;

	private float _xscale = 1.0f;
	private float _yscale = 1.0f;
	private float _zscale = 1.0f;

	protected GLProfile glprofile;
	protected GLCapabilities glcapabilities;

	/**
	 * The hosted OpenGL panel, or {@code null} if OpenGL could not be
	 * initialized on this system (see {@link #isGLAvailable()}). Every method
	 * in this class that touches {@code gljpanel} already null-checks it, so a
	 * {@code null} value here degrades gracefully to a no-op rather than an
	 * NPE.
	 */
	protected final GLJPanel gljpanel;

	/** Set when {@link #gljpanel} is {@code null}: why OpenGL initialization failed. */
	private final Throwable glInitError;

	protected GLU glu; // glu utilities
	
	// Navigation step used by keyboard panning and mouse-wheel zoom.
	// The default preserves the old behavior for unit-scale views.
	private float _navigationStep = 0.1f;

	// distance in front of the screen
	private float _zdist;

	// x and y translation
	private float _xdist;
	private float _ydist;

	// Quaternion orientation (this IS the truth)
	private final Quat _orientation = new Quat(); // identity by default

	// scratch matrix (column-major for OpenGL)
	private final float[] _rotMat = new float[16];

	// the list of 3D items to be drawn
	protected Vector<Item3D> _itemList = new Vector<>();

	// listen for mouse events
	protected MouseAdapter3D _mouseAdapter;

	protected String _rendererStr;

	private boolean _skipLastStage = false;

	// the openGL version and renderer strings
	protected String _versionStr;

	/**
	 * Construct a 3D panel with the default light-gray background.
	 *
	 * <p>
	 * The initial orientation is established by resetting the quaternion
	 * orientation and then applying rotations about x, y, and z, in that order.
	 * The distance parameters are OpenGL model-view translations applied before
	 * scaling and rotation.
	 * </p>
	 *
	 * @param angleX initial rotation angle about the x axis, in degrees
	 * @param angleY initial rotation angle about the y axis, in degrees
	 * @param angleZ initial rotation angle about the z axis, in degrees
	 * @param xDist initial x translation
	 * @param yDist initial y translation
	 * @param zDist initial z translation
	 */
	public Panel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist) {
		this(angleX, angleY, angleZ, xDist, yDist, zDist, BG_DEFAULT, BG_DEFAULT, BG_DEFAULT, false);
	}

	/**
	 * Construct a 3D panel with an explicit background color.
	 *
	 * <p>
	 * The constructor creates the {@link GLJPanel}, registers this object as the
	 * {@link GLEventListener}, installs the MDI-3D keyboard and mouse adapters,
	 * and creates any optional border components returned by the directional hook
	 * methods. The initial scene contents are then created by calling
	 * {@link #createInitialItems()}.
	 * </p>
	 *
	 * @param angleX initial rotation angle about the x axis, in degrees
	 * @param angleY initial rotation angle about the y axis, in degrees
	 * @param angleZ initial rotation angle about the z axis, in degrees
	 * @param xDist initial x translation
	 * @param yDist initial y translation
	 * @param zDist initial z translation
	 * @param bgRed red component of the background color, in {@code [0, 1]}
	 * @param bgGreen green component of the background color, in {@code [0, 1]}
	 * @param bgBlue blue component of the background color, in {@code [0, 1]}
	 * @param skipLastStage if {@code true}, skip the final
	 *        {@code glLoadIdentity()} in {@link #display(GLAutoDrawable)}
	 */
	public Panel3D(float angleX, float angleY, float angleZ, float xDist, float yDist, float zDist, float bgRed,
			float bgGreen, float bgBlue, boolean skipLastStage) {

		_skipLastStage = skipLastStage;

		_xdist = xDist;
		_ydist = yDist;
		_zdist = zDist;

		_bgRed = bgRed;
		_bgGreen = bgGreen;
		_bgBlue = bgBlue;

		setLayout(new BorderLayout(0, 0));

		// GL initialization can fail on a system with no usable OpenGL (a
		// headless CI runner, a remote desktop session without GPU passthrough,
		// a misconfigured or missing driver). Rather than let that propagate out
		// of a view's constructor as a raw JOGL exception -- which previously
		// crashed the whole view with a stack trace and no indication of what
		// went wrong -- fall back to gljpanel == null and show an explanatory
		// panel in its place. Every other method in this class that touches
		// gljpanel already null-checks it (refresh, softRefresh,
		// reinitGLContext), so this is the only place that needed to change.
		GLJPanel panel;
		Throwable initError;
		try {
			GLProfile profile;
			if (GLProfile.isAvailable(GLProfile.GL2)) {
				profile = GLProfile.get(GLProfile.GL2);
			} else {
				profile = GLProfile.getMaxFixedFunc(true);
			}
			glprofile = profile;

			glcapabilities = new GLCapabilities(profile);
			glcapabilities.setRedBits(8);
			glcapabilities.setBlueBits(8);
			glcapabilities.setGreenBits(8);
			glcapabilities.setAlphaBits(8);
			glcapabilities.setDepthBits(32);

			panel = new GLJPanel(glcapabilities);
			panel.addGLEventListener(this);
			initError = null;
		} catch (Throwable t) {
			// Deliberately broad: JOGL/GlueGen can fail with GLException,
			// UnsatisfiedLinkError, or other platform-specific throwables
			// depending on what's missing, and every one of them should land
			// here rather than abort view construction.
			panel = null;
			initError = t;
		}
		gljpanel = panel;
		glInitError = initError;

		safeAdd(addNorth(), BorderLayout.NORTH);
		safeAdd(addSouth(), BorderLayout.SOUTH);
		safeAdd(addEast(), BorderLayout.EAST);
		safeAdd(addWest(), BorderLayout.WEST);

		if (gljpanel != null) {
			add(gljpanel, BorderLayout.CENTER);

			new KeyBindings3D(this);

			_mouseAdapter = new MouseAdapter3D(this);
			gljpanel.addMouseListener(_mouseAdapter);
			gljpanel.addMouseMotionListener(_mouseAdapter);
			gljpanel.addMouseWheelListener(_mouseAdapter);
		} else {
			add(buildUnavailablePanel(glInitError), BorderLayout.CENTER);
		}

		// Set initial orientation using the same semantics as before:
		// reset then apply rotateX/Y/Z in that order.
		loadIdentityMatrix();
		rotateX(angleX);
		rotateY(angleY);
		rotateZ(angleZ);

		createInitialItems();

	}

	/**
	 * Whether OpenGL was successfully initialized for this panel.
	 *
	 * @return {@code true} if this panel has a working {@link GLJPanel};
	 *         {@code false} if GL initialization failed and {@link #getGLJPanel()}
	 *         returns {@code null}
	 */
	public boolean isGLAvailable() {
		return gljpanel != null;
	}

	/**
	 * The reason OpenGL initialization failed, if it did.
	 *
	 * @return the throwable caught during GL setup, or {@code null} if
	 *         {@link #isGLAvailable()} is {@code true}
	 */
	public Throwable getGLInitError() {
		return glInitError;
	}

	// Build the explanatory panel shown in place of the GLJPanel when OpenGL
	// could not be initialized.
	private static JComponent buildUnavailablePanel(Throwable cause) {
		String reason = (cause == null) ? "unknown error" : cause.getClass().getSimpleName()
				+ (cause.getMessage() != null ? ": " + cause.getMessage() : "");

		JLabel label = new JLabel("<html><div style='text-align:center;'>"
				+ "3D rendering is unavailable on this system.<br>"
				+ "OpenGL could not be initialized.<br>"
				+ "<font size='-1' color='gray'>" + reason + "</font></div></html>",
				SwingConstants.CENTER);

		return DialogUtils.paddedPanel(24, 24, label);
	}

	/**
	 * Create the initial 3D items for this panel.
	 *
	 * <p>
	 * The default implementation is empty. Subclasses normally override this
	 * method to populate the panel with axes, lines, surfaces, point clouds, or
	 * other {@link Item3D} objects. This method is called by the constructor after
	 * the OpenGL panel and input adapters have been created.
	 * </p>
	 */
	public void createInitialItems() {
		// default empty implementation
	}

	/**
	 * Add an optional border component when it is non-null.
	 *
	 * @param c component to add; ignored when {@code null}
	 * @param placement {@link BorderLayout} placement constraint
	 */
	private void safeAdd(JComponent c, String placement) {
		if (c != null) {
			add(c, placement);
		}
	}

	/**
	 * Optional hook for a Swing component on the north side of the panel.
	 *
	 * <p>
	 * The default implementation returns {@code null}. Subclasses can override
	 * this method to add controls, legends, sliders, or other Swing components
	 * outside the OpenGL drawing area.
	 * </p>
	 *
	 * @return component to place on the north side, or {@code null}
	 */
	protected JComponent addNorth() {
		return null;
	}

	/**
	 * Optional hook for a Swing component on the south side of the panel.
	 *
	 * <p>
	 * The default implementation returns {@code null}. Subclasses can override
	 * this method to add controls, legends, sliders, or other Swing components
	 * outside the OpenGL drawing area.
	 * </p>
	 *
	 * @return component to place on the south side, or {@code null}
	 */
	protected JComponent addSouth() {
		return null;
	}

	/**
	 * Optional hook for a Swing component on the east side of the panel.
	 *
	 * <p>
	 * The default implementation returns {@code null}. Subclasses can override
	 * this method to add controls, legends, sliders, or other Swing components
	 * outside the OpenGL drawing area.
	 * </p>
	 *
	 * @return component to place on the east side, or {@code null}
	 */
	protected JComponent addEast() {
		return null;
	}

	/**
	 * Optional hook for a Swing component on the west side of the panel.
	 *
	 * <p>
	 * The default implementation returns {@code null}. Subclasses can override
	 * this method to add controls, legends, sliders, or other Swing components
	 * outside the OpenGL drawing area.
	 * </p>
	 *
	 * @return component to place on the west side, or {@code null}
	 */
	protected JComponent addWest() {
		return null;
	}

	/**
	 * Return the JOGL panel hosted by this Swing panel.
	 *
	 * <p>
	 * Most callers should interact with {@code Panel3D} rather than the raw
	 * {@code GLJPanel}. This accessor is provided for cases that need direct
	 * integration with JOGL or Swing.
	 * </p>
	 *
	 * @return the hosted OpenGL panel
	 */
	public GLJPanel getGLJPanel() {
		return gljpanel;
	}

	/**
	 * Set the scale factors applied to the scene before rotation.
	 *
	 * @param xscale scale factor in x
	 * @param yscale scale factor in y
	 * @param zscale scale factor in z
	 */
	public void setScale(float xscale, float yscale, float zscale) {
		_xscale = xscale;
		_yscale = yscale;
		_zscale = zscale;
	}

	/**
	 * Apply an axis-angle rotation to the current orientation.
	 *
	 * <p>
	 * This method composes the new rotation with the existing quaternion
	 * orientation. The rotation angle is in radians, which matches the values
	 * produced by {@link MouseAdapter3D}. Invalid or nearly zero-length axes are
	 * ignored.
	 * </p>
	 *
	 * @param axis rotation axis
	 * @param angleRadians rotation angle, in radians
	 */
	public void rotate(Vector3f axis, float angleRadians) {
		if (axis == null) {
			return;
		}
		float len = axis.length();
		if (len < 1e-6f) {
			return;
		}

		// normalize axis
		float ax = axis.x / len;
		float ay = axis.y / len;
		float az = axis.z / len;

		Quat dq = Quat.fromAxisAngle(ax, ay, az, angleRadians);

		// Compose: newOrientation = dq * orientation
		// (left-multiply matches typical "rotate object in world" feel for arcball)
		synchronized (_orientation) {
			_orientation.set(dq.mul(_orientation));
			_orientation.normalizeInPlace();
		}

		refresh();
	}

	/**
	 * Rotate the scene about the x axis.
	 *
	 * @param angleDeg rotation angle, in degrees
	 */
	public void rotateX(float angleDeg) {
		float rad = (float) Math.toRadians(angleDeg);
		Quat dq = Quat.fromAxisAngle(1f, 0f, 0f, rad);

		synchronized (_orientation) {
			_orientation.set(dq.mul(_orientation));
			_orientation.normalizeInPlace();
		}

		refresh();
	}

	/**
	 * Rotate the scene about the y axis.
	 *
	 * @param angleDeg rotation angle, in degrees
	 */
	public void rotateY(float angleDeg) {
		float rad = (float) Math.toRadians(angleDeg);
		Quat dq = Quat.fromAxisAngle(0f, 1f, 0f, rad);

		synchronized (_orientation) {
			_orientation.set(dq.mul(_orientation));
			_orientation.normalizeInPlace();
		}

		refresh();
	}

	/**
	 * Rotate the scene about the z axis.
	 *
	 * @param angleDeg rotation angle, in degrees
	 */
	public void rotateZ(float angleDeg) {
		float rad = (float) Math.toRadians(angleDeg);
		Quat dq = Quat.fromAxisAngle(0f, 0f, 1f, rad);

		synchronized (_orientation) {
			_orientation.set(dq.mul(_orientation));
			_orientation.normalizeInPlace();
		}

		refresh();
	}

	/**
	 * Reset the current orientation to the identity rotation.
	 *
	 * <p>
	 * Earlier versions of MDI-3D stored the orientation as a matrix. The current
	 * implementation stores the orientation as a quaternion; this method preserves
	 * the old name while resetting the quaternion to identity.
	 * </p>
	 */
	public void loadIdentityMatrix() {
		synchronized (_orientation) {
			_orientation.setIdentity();
		}
	}

	/**
	 * Render the current OpenGL frame.
	 *
	 * <p>
	 * JOGL calls this method whenever the {@link GLJPanel} needs to draw. The
	 * method clears the buffers, applies translation, scale, and quaternion
	 * orientation, snapshots the current item list, draws opaque items first, and
	 * then draws transparent items in approximate back-to-front order.
	 * </p>
	 *
	 * @param drawable JOGL drawable being rendered
	 */
	@Override
	public void display(GLAutoDrawable drawable) {

		final GL2 gl = drawable.getGL().getGL2();

		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);

		gl.glDisable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glTranslatef(_xdist, _ydist, _zdist);
		gl.glScalef(_xscale, _yscale, _zscale);

		// Apply quaternion orientation as a column-major matrix
		synchronized (_orientation) {
			_orientation.toColumnMajorMatrix(_rotMat);
		}
		gl.glMultMatrixf(_rotMat, 0);

		// Snapshot items
		final java.util.List<Item3D> snapshot;
		synchronized (_itemList) {
			snapshot = new java.util.ArrayList<>(_itemList);
		}

		final java.util.List<Item3D> opaque = new java.util.ArrayList<>(snapshot.size());
		final java.util.List<Item3D> transparent = new java.util.ArrayList<>(snapshot.size());

		for (Item3D item : snapshot) {
			if (item != null && item.isVisible()) {
				(isTransparent(item) ? transparent : opaque).add(item);
			}
		}

		gl.glPushMatrix();
		beforeDraw(drawable);

		// PASS 1: OPAQUE
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDepthMask(true);
		gl.glDisable(GL.GL_BLEND);
		for (Item3D item : opaque) {
			item.drawItem(drawable);
		}

		// PASS 2: TRANSPARENT
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glDepthMask(false);
		gl.glEnable(GL.GL_BLEND);
		// Sort transparent items back-to-front to improve blending correctness
		sortTransparentBackToFront(transparent);

		for (Item3D item : transparent) {
			item.drawItem(drawable);
		}

		gl.glDepthMask(true);
		gl.glDisable(GL.GL_BLEND);

		afterDraw(drawable);
		gl.glPopMatrix();

		if (_skipLastStage) {
			return;
		}

		gl.glLoadIdentity();
	}

	/**
	 * Hook called after the model-view transform has been applied and before any
	 * items are drawn.
	 *
	 * <p>
	 * The default implementation is empty. Subclasses can override this method to
	 * draw custom OpenGL content behind the item list.
	 * </p>
	 *
	 * @param drawable JOGL drawable being rendered
	 */
	public void beforeDraw(GLAutoDrawable drawable) {
	}

	/**
	 * Hook called after all items have been drawn and before the model-view matrix
	 * is restored.
	 *
	 * <p>
	 * The default implementation is empty. Subclasses can override this method to
	 * draw custom OpenGL content on top of the item list.
	 * </p>
	 *
	 * @param drawable JOGL drawable being rendered
	 */
	public void afterDraw(GLAutoDrawable drawable) {
	}

	/**
	 * Initialize the OpenGL state for this panel.
	 *
	 * <p>
	 * JOGL calls this method when the GL context is created. The method records
	 * renderer/version strings and configures depth testing, perspective
	 * correction, blending, point sizing, and the clear color.
	 * </p>
	 *
	 * @param drawable JOGL drawable whose context is being initialized
	 */
	@Override
	public void init(GLAutoDrawable drawable) {

		glu = new GLU();
		GL2 gl = drawable.getGL().getGL2();

		_versionStr = gl.glGetString(GL.GL_VERSION);
		_rendererStr = gl.glGetString(GL.GL_RENDERER);

		float values[] = new float[2];
		gl.glGetFloatv(GL2GL3.GL_LINE_WIDTH_GRANULARITY, values, 0);
		gl.glGetFloatv(GL2GL3.GL_LINE_WIDTH_RANGE, values, 0);

		gl.glClearColor(_bgRed, _bgGreen, _bgBlue, 1f);
		gl.glClearDepth(1.0f);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);

		gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		gl.glShadeModel(GLLightingFunc.GL_FLAT);

		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL2ES3.GL_COLOR);
		gl.glHint(GL2ES1.GL_POINT_SMOOTH_HINT, GL.GL_DONT_CARE);
		gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_DONT_CARE);

		gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
	}

	/**
	 * Update the OpenGL viewport and projection matrix after a resize.
	 *
	 * <p>
	 * The projection uses a 45-degree perspective field of view with a broad
	 * near/far range appropriate for the simple demo scenes used by MDI-3D.
	 * </p>
	 *
	 * @param drawable JOGL drawable being reshaped
	 * @param x viewport x origin
	 * @param y viewport y origin
	 * @param width new viewport width
	 * @param height new viewport height
	 */
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// glu may be null if reshape fires before init completes (can happen
		// when a lazily-created GLJPanel is resized before its first display).
		// In that case, request a repaint so init+reshape will run again cleanly.
		if (glu == null) {
			gljpanel.repaint();
			return;
		}

		GL2 gl = drawable.getGL().getGL2();

		if (height == 0) {
			height = 1;
		}

		float aspect = (float) width / height;

		gl.glViewport(0, 0, width, height);

		gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
		gl.glLoadIdentity();

		glu.gluPerspective(45.0, aspect, 0.1, 10000.0);

		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	/**
	 * Increment the current x translation.
	 *
	 * @param dx change in x translation
	 */
	public void deltaX(float dx) {
		_xdist += dx;
	}

	/**
	 * Increment the current y translation.
	 *
	 * @param dy change in y translation
	 */
	public void deltaY(float dy) {
		_ydist += dy;
	}

	/**
	 * Increment the current z translation.
	 *
	 * @param dz change in z translation
	 */
	public void deltaZ(float dz) {
		_zdist += dz;
	}

	/**
	 * Placeholder for queued refresh support.
	 *
	 * <p>
	 * The current implementation is intentionally empty. It is retained for API
	 * compatibility with code that may distinguish queued refreshes from immediate
	 * or soft refreshes.
	 * </p>
	 */
	public void refreshQueued() {
	}

	/**
	 * Soft refresh: schedules a repaint through AWT's normal paint mechanism. Use
	 * this for layout-driven redraws (resize, move, show). This is safe to call
	 * from the EDT and does not race with Swing painting.
	 */
	public void softRefresh() {
		if (gljpanel != null) {
			gljpanel.repaint();
		}
	}

	/**
	 * Hard refresh: forces an immediate JOGL render. Only call this when you need
	 * synchronous GL output, e.g. after a mouse-drag rotation. Do NOT call from
	 * component/layout events.
	 */
	public void refresh() {
		if (gljpanel == null || !gljpanel.isDisplayable() || gljpanel.getWidth() <= 0 || gljpanel.getHeight() <= 0) {
			return;
		}
		gljpanel.display();
	}

	/**
	 * Force JOGL to tear down and rebuild the GL context on next paint. Call this
	 * once after a lazy view has been made visible with a real native peer, to
	 * ensure the FBO is created against the correct surface.
	 */
	public void reinitGLContext() {
		if (gljpanel != null) {
			gljpanel.repaint();
		}
	}

	/**
	 * Add an item to the panel.
	 *
	 * <p>
	 * If the item is already present, it is moved to the end of the list. Later
	 * items are normally drawn later within their opaque/transparent pass.
	 * </p>
	 *
	 * @param item item to add; ignored when {@code null}
	 */
	public void addItem(Item3D item) {
		if (item != null) {
			// Atomic with respect to display()'s synchronized(_itemList) snapshot:
			// without this, a concurrent snapshot taken between the remove and the
			// add would observe the item as transiently missing.
			synchronized (_itemList) {
				_itemList.remove(item);
				_itemList.add(item);
			}
		}
	}

	/**
	 * Add an item at the specified list index.
	 *
	 * <p>
	 * If the item is already present, the existing occurrence is removed first.
	 * </p>
	 *
	 * @param index insertion index
	 * @param item item to add; ignored when {@code null}
	 */
	public void addItem(int index, Item3D item) {
		if (item != null) {
			synchronized (_itemList) {
				_itemList.remove(item);
				_itemList.add(index, item);
			}
		}
	}

	/**
	 * Remove an item from the panel and refresh the display.
	 *
	 * @param item item to remove; ignored when {@code null}
	 */
	public void removeItem(Item3D item) {
		if (item != null) {
			_itemList.remove(item);
			refresh();
		}
	}

	/**
	 * Remove all items from the panel and refresh the display.
	 */
	public void clearItems() {
		_itemList.clear();
		refresh();
	}

	/**
	 * Returns the current X scale factor.
	 * 
	 * @return the current X scale factor
	 */
	public float getXScale() {
		return _xscale;
	}

	/**
	 * Returns the current Y scale factor.
	 * 
	 * @return the current Y scale factor
	 */
	public float getYScale() {
		return _yscale;
	}

	/**
	 * Returns the current Z scale factor.
	 * 
	 * @return the current Z scale factor
	 */
	public float getZScale() {
		return _zscale;
	}

	/**
	 * Project object coordinates to window coordinates using the current OpenGL
	 * matrices.
	 *
	 * <p>
	 * The result is written into {@code winPos}: {@code winPos[0]} is window x,
	 * {@code winPos[1]} is window y, and {@code winPos[2]} is window z/depth.
	 * The caller must provide an array with length at least three.
	 * </p>
	 *
	 * @param gl active GL2 context
	 * @param objX object x coordinate
	 * @param objY object y coordinate
	 * @param objZ object z coordinate
	 * @param winPos output array for window coordinates
	 */
	public void project(GL2 gl, float objX, float objY, float objZ, float winPos[]) {
		int[] view = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, view, 0);

		float[] model = new float[16];
		gl.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, model, 0);

		float[] proj = new float[16];
		gl.glGetFloatv(GLMatrixFunc.GL_PROJECTION_MATRIX, proj, 0);

		glu.gluProject(objX, objY, objZ, model, 0, proj, 0, view, 0, winPos, 0);
	}

	/**
	 * Set the navigation step used by keyboard panning and mouse-wheel zoom.
	 *
	 * <p>
	 * The original default value is suitable for small unit-scale scenes. Larger
	 * scenes should set this to a value proportional to the scene extent, for
	 * example {@code extent / 50}.
	 * </p>
	 *
	 * @param navigationStep positive navigation step in world units
	 */
	public void setNavigationStep(float navigationStep) {
	    if (Float.isFinite(navigationStep) && navigationStep > 0f) {
	        _navigationStep = navigationStep;
	    }
	}

	/**
	 * Set the navigation step from a characteristic scene extent.
	 *
	 * <p>
	 * This is a convenience method for views whose world coordinates are much
	 * larger than order unity. A scene spanning roughly 700 world units, for
	 * example, gets a step of about 14 units.
	 * </p>
	 *
	 * @param extent characteristic scene extent in world units
	 */
	public void setNavigationStepFromExtent(float extent) {
	    if (Float.isFinite(extent) && extent > 0f) {
	        setNavigationStep(extent / 50f);
	    }
	}

	/**
	 * Return the navigation step used by keyboard panning and mouse-wheel zoom.
	 *
	 * @return navigation step in world units
	 */
	public float getZStep() {
	    return _navigationStep;
	}
	/**
	 * Determine whether an item should be drawn in the transparent pass.
	 *
	 * <p>
	 * An item is treated as transparent when either its fill alpha or line alpha
	 * is non-negative and below {@link #OPAQUE_ALPHA_CUTOFF}. Exceptions from
	 * item alpha accessors are ignored so that legacy item implementations remain
	 * renderable.
	 * </p>
	 *
	 * @param item item to test
	 * @return {@code true} if the item should be drawn in the transparent pass
	 */
	private boolean isTransparent(Item3D item) {
		try {
			int fa = item.getFillAlpha();
			if (fa >= 0 && fa < OPAQUE_ALPHA_CUTOFF) {
				return true;
			}
		} catch (Exception ignored) {
		}

		try {
			int la = item.getLineAlpha();
			if (la >= 0 && la < OPAQUE_ALPHA_CUTOFF) {
				return true;
			}
		} catch (Exception ignored) {
		}

		return false;
	}

	// --------------------------------------------------------------------
	// Minimal quaternion implementation (no dependencies)
	// --------------------------------------------------------------------

	/**
	 * Minimal quaternion implementation used for scene orientation.
	 *
	 * <p>
	 * This nested class is deliberately small and dependency-free. It supports
	 * identity reset, copying, axis-angle construction, multiplication,
	 * normalization, and conversion to the OpenGL column-major matrix format used
	 * by {@code glMultMatrixf}.
	 * </p>
	 */
	private static final class Quat {
		// w + xi + yj + zk
		float w = 1f, x = 0f, y = 0f, z = 0f;

		/**
		 * Reset this quaternion to the identity rotation.
		 */
		void setIdentity() {
			w = 1f;
			x = y = z = 0f;
		}

		/**
		 * Copy another quaternion into this one.
		 *
		 * @param q source quaternion
		 */
		void set(Quat q) {
			this.w = q.w;
			this.x = q.x;
			this.y = q.y;
			this.z = q.z;
		}

		/**
		 * Create a quaternion from a normalized axis and angle.
		 *
		 * @param ax x component of rotation axis
		 * @param ay y component of rotation axis
		 * @param az z component of rotation axis
		 * @param angleRad rotation angle, in radians
		 * @return new quaternion representing the rotation
		 */
		static Quat fromAxisAngle(float ax, float ay, float az, float angleRad) {
			float half = 0.5f * angleRad;
			float s = (float) Math.sin(half);
			Quat q = new Quat();
			q.w = (float) Math.cos(half);
			q.x = ax * s;
			q.y = ay * s;
			q.z = az * s;
			return q;
		}

		/**
		 * Multiply this quaternion by another quaternion.
		 *
		 * @param r right-hand quaternion
		 * @return product {@code this * r}
		 */
		Quat mul(Quat r) {
			// this * r
			Quat q = new Quat();
			q.w = this.w * r.w - this.x * r.x - this.y * r.y - this.z * r.z;
			q.x = this.w * r.x + this.x * r.w + this.y * r.z - this.z * r.y;
			q.y = this.w * r.y - this.x * r.z + this.y * r.w + this.z * r.x;
			q.z = this.w * r.z + this.x * r.y - this.y * r.x + this.z * r.w;
			return q;
		}

		/**
		 * Normalize this quaternion in place.
		 *
		 * <p>
		 * If the norm is too small to normalize safely, the quaternion is reset to
		 * identity.
		 * </p>
		 */
		void normalizeInPlace() {
			float n = (float) Math.sqrt(w * w + x * x + y * y + z * z);
			if (n < 1e-12f) {
				setIdentity();
				return;
			}
			w /= n;
			x /= n;
			y /= n;
			z /= n;
		}

		/**
		 * Convert this quaternion to a 4x4 column-major rotation matrix for OpenGL.
		 *
		 * @param m output array with length at least 16
		 */
		void toColumnMajorMatrix(float[] m) {
			// assumes normalized
			float xx = x * x, yy = y * y, zz = z * z;
			float xy = x * y, xz = x * z, yz = y * z;
			float wx = w * x, wy = w * y, wz = w * z;

			// Column-major (OpenGL)
			m[0] = 1f - 2f * (yy + zz);
			m[1] = 2f * (xy + wz);
			m[2] = 2f * (xz - wy);
			m[3] = 0f;

			m[4] = 2f * (xy - wz);
			m[5] = 1f - 2f * (xx + zz);
			m[6] = 2f * (yz + wx);
			m[7] = 0f;

			m[8] = 2f * (xz + wy);
			m[9] = 2f * (yz - wx);
			m[10] = 1f - 2f * (xx + yy);
			m[11] = 0f;

			m[12] = 0f;
			m[13] = 0f;
			m[14] = 0f;
			m[15] = 1f;
		}
	}

	/**
	 * Sort transparent items in approximate back-to-front order.
	 *
	 * <p>
	 * The sort uses each item's sort point transformed into approximate view
	 * space. This improves ordinary alpha blending for simple scenes, but does
	 * not solve all transparency-ordering cases.
	 * </p>
	 *
	 * @param transparent transparent items to sort in place
	 */
	private void sortTransparentBackToFront(java.util.List<Item3D> transparent) {

		// Copy rotation matrix once (already computed for glMultMatrixf)
		final float[] R = _rotMat;

		// Cache camera translation as well (your view translate happens before
		// rotation)
		final float tx = _xdist;
		final float ty = _ydist;
		final float tz = _zdist;

		// Cache scales
		final float sx = _xscale, sy = _yscale, sz = _zscale;

		java.util.Collections.sort(transparent, (a, b) -> {
			float za = viewZ(a, R, sx, sy, sz, tx, ty, tz);
			float zb = viewZ(b, R, sx, sy, sz, tx, ty, tz);
			int c = Float.compare(za, zb); // ascending: more negative first
			if (c != 0) {
				return c;
			}
			return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
		});
	}

	/**
	 * Compute an approximate view-space z value for an item.
	 *
	 * <p>
	 * The computation uses the item's sort point and a simplified view transform:
	 * {@code v = R * (S * p) + T}. The result is suitable for sorting transparent
	 * items but is not intended as a general picking or projection calculation.
	 * </p>
	 *
	 * @param item item whose sort point is used
	 * @param R column-major rotation matrix
	 * @param sx x scale
	 * @param sy y scale
	 * @param sz z scale
	 * @param tx x translation
	 * @param ty y translation
	 * @param tz z translation
	 * @return approximate view-space z value
	 */
	private float viewZ(Item3D item, float[] R, float sx, float sy, float sz, float tx, float ty, float tz) {
		float[] p = item.getSortPoint();

// Scale first (GL_MODELVIEW applies: Translate, then Scale, then Rotate)
		float x = p[0] * sx;
		float y = p[1] * sy;
		float z = p[2] * sz;

// Apply the Z row of the column-major rotation matrix: R[2], R[6], R[10]
		float zr = R[2] * x + R[6] * y + R[10] * z;

// Then translate
		zr += tz;

		return zr;
	}
	
	/**
	 * Dispose of OpenGL resources associated with this listener.
	 *
	 * <p>
	 * {@code Panel3D} itself holds no explicit GL resources, but items on the
	 * panel may — for example a text-drawing item's lazily-created JOGL
	 * {@code TextRenderer}, which owns a GPU texture atlas. This gives every
	 * item (and its children) a chance to release those via {@link
	 * Item3D#disposeItem(GLAutoDrawable)} before the GL context goes away, so
	 * repeatedly opening and closing 3D views doesn't leak texture memory.
	 * </p>
	 *
	 * @param drawable JOGL drawable being disposed
	 */
	@Override
	public void dispose(GLAutoDrawable drawable) {

		final java.util.List<Item3D> snapshot;
		synchronized (_itemList) {
			snapshot = new java.util.ArrayList<>(_itemList);
		}
		for (Item3D item : snapshot) {
			if (item == null) {
				continue;
			}
			// Defensive: one item's dispose() throwing must not abort disposal of
			// the rest, nor propagate out of this GLEventListener callback and
			// potentially corrupt JOGL's dispose/reinit sequence for the panel.
			try {
				item.disposeItem(drawable);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
	}
}
