package edu.cnu.mdi.mdi3D.item3D;

import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.jogamp.opengl.GLAutoDrawable;

import edu.cnu.mdi.mdi3D.panel.Bad3DPropertyException;
import edu.cnu.mdi.mdi3D.panel.Panel3D;

/**
 * Base class for every drawable object hosted on a {@link Panel3D}.
 * <p>
 * {@code Item3D} is the 3D analogue of MDI's 2D {@code AItem}: a self-contained
 * drawable that owns its own appearance properties and can be organized into a
 * parent/child tree so a group of items (e.g. the axes of a coordinate frame,
 * or the components of a composite shape) can be shown, hidden, or disposed as
 * a unit.
 * </p>
 *
 * <h2>Properties</h2>
 * <p>
 * Appearance is stored as a small {@link Properties}-backed key/value bag
 * ({@link #LINE_WIDTH}, {@link #FILLCOLOR}, {@link #FILLALPHA}, {@link #LINECOLOR},
 * {@link #LINEALPHA}, {@link #TEXT_COLOR}, {@link #FONT}), accessed through the
 * typed {@code get*}/{@code set*} convenience methods. Any property that has not
 * been explicitly set falls back to a class-wide default.
 * </p>
 *
 * <h2>Drawing and disposal</h2>
 * <p>
 * Subclasses implement {@link #draw(GLAutoDrawable)} for their own custom
 * rendering, and only that GL-thread-bound method should touch the GL context.
 * {@link #drawItem(GLAutoDrawable)} and {@link #disposeItem(GLAutoDrawable)} are
 * called by {@link Panel3D} and are {@code final}: they invoke {@link
 * #draw(GLAutoDrawable)}/{@link #dispose(GLAutoDrawable)} on this item and then
 * recurse into every visible child, so a subtree draws or releases its GL
 * resources with a single call from the panel.
 * </p>
 *
 * <h2>Thread safety</h2>
 * <p>
 * {@link #isVisible()}/{@link #setVisible(boolean)} and the child list are safe
 * to touch from threads other than the GL rendering thread: visibility is
 * {@code volatile}, and {@link #addChild(Item3D)}/{@link #removeChild(Item3D)}
 * synchronize on {@code this}, the same monitor used to snapshot the child list
 * before iterating it during draw/dispose. Actual GL calls inside {@link
 * #draw(GLAutoDrawable)} still must only happen on the GL thread.
 * </p>
 */
public abstract class Item3D {

	/** property for line width. Default is 1f */
	public static final String LINE_WIDTH = "LINE_WIDTH";

	/** property for fill color. Default is defined below */
	public static final String FILLCOLOR = "FILLCOLOR";

	/** property for fill alpha. Default is defined below */
	public static final String FILLALPHA = "FILLALPHA";

	/** property for line or frame color. Default is defined below */
	public static final String LINECOLOR = "LINECOLOR";

	/** property for line alpha. Default is defined below */
	public static final String LINEALPHA = "LINEALPHA";

	/** property for text color. Default is defined below */
	public static final String TEXT_COLOR = "TEXT_COLOR";

	/** property string for font. Default is defined below */
	public static final String FONT = "FONT";

	// this item's properties
	private Properties _properties;

	// controls whether the item is visible
	// only in the sense do we "want" to draw it
	// volatile: setVisible()/isVisible() have no other synchronization, so a
	// caller on a non-EDT thread still publishes the change safely to the
	// EDT-based renderer.
	private volatile boolean _visible = true;

	// this item's child items
	private Vector<Item3D> _children;

	// this item's parent
	private Item3D _parent;

	// the owner panel
	protected Panel3D _panel3D;

	// defaults
	private static final float _defaultLineWidth = 1f;
	private static final int _defaultFillAlpha = 255;
	private static final int _defaultLineAlpha = 255;
	private static final Color _defaultFillColor = Color.yellow;
	private static final Color _defaultLineColor = Color.gray;
	private static final Color _defaultTextColor = Color.white;
	private static final Font _defaultFont = new Font("SansSerif", Font.PLAIN, 12);

	/**
	 * Create a 3D item for use on a Panel3D.
	 *
	 * @param panel3D the owner panel 3D object
	 */
	public Item3D(Panel3D panel3D) {
		_panel3D = panel3D;
		defaultProperties();
	}

	/**
	 * Set the default properties
	 */
	protected void defaultProperties() {
		_properties = new Properties();
		_properties.put(LINE_WIDTH, _defaultLineWidth);
		_properties.put(FILLCOLOR, _defaultFillColor);
		_properties.put(FILLALPHA, _defaultFillAlpha);
		_properties.put(LINECOLOR, _defaultLineColor);
		_properties.put(LINEALPHA, _defaultLineAlpha);
		_properties.put(TEXT_COLOR, _defaultTextColor);
		_properties.put(FONT, _defaultFont);
	}

	/**
	 * set a property
	 *
	 * @param key   the key
	 * @param value the value
	 */
	public void put(Object key, Object value) {
		_properties.put(key, value);
	}

	/**
	 * Called by the panel3D. Do not overrwrite.
	 *
	 * @param drawable the OpenGL drawable
	 */
	public final void drawItem(GLAutoDrawable drawable) {
		draw(drawable);

		for (Item3D item : childrenSnapshot()) {
			if (item.isVisible()) {
				item.drawItem(drawable);
			}
		}
	}

	/**
	 * Snapshot the current children under {@code synchronized(this)} — the same
	 * monitor {@link #addChild(Item3D)} and {@link #removeChild(Item3D)} use —
	 * so a concurrent structural change (e.g. a child added from a non-EDT
	 * thread) can never be observed mid-iteration here.
	 *
	 * @return an immutable snapshot list; empty (never {@code null}) if there
	 *         are no children
	 */
	private List<Item3D> childrenSnapshot() {
		synchronized (this) {
			return (_children == null) ? List.of() : List.copyOf(_children);
		}
	}

	/**
	 * The custom drawing method
	 *
	 * @param drawable the OpenGL drawable
	 */
	public abstract void draw(GLAutoDrawable drawable);

	/**
	 * Called by the panel3D when its GL context is being torn down. Do not
	 * overwrite.
	 * <p>
	 * Releases this item's own GL resources via {@link #dispose(GLAutoDrawable)},
	 * then recurses into children so a subtree is fully released with one call.
	 * </p>
	 *
	 * @param drawable the OpenGL drawable being disposed
	 */
	public final void disposeItem(GLAutoDrawable drawable) {
		dispose(drawable);

		for (Item3D item : childrenSnapshot()) {
			item.disposeItem(drawable);
		}
	}

	/**
	 * Override to release any GL resources this item owns (textures, display
	 * lists, text renderers, etc.).
	 *
	 * <p>
	 * The default implementation does nothing. Items that lazily allocate
	 * GL-backed resources — for example a JOGL {@code TextRenderer} — should
	 * override this to release them and null out the field so a subsequent
	 * {@code draw} call (if any) lazily recreates it.
	 * </p>
	 *
	 * @param drawable the OpenGL drawable being disposed
	 */
	protected void dispose(GLAutoDrawable drawable) {
		// no resources to release by default
	}

	/**
	 * Get a 3D property
	 *
	 * @param key the name of the property
	 * @return the object corresponding to that key, or null
	 * @throws Bad3DPropertyException if the property is missing or not of the expected type
	 */
	public Object get(String key) throws Bad3DPropertyException {
		Object obj = _properties.get(key);
		if (obj == null) {
			throw new Bad3DPropertyException("Item has no property named: " + key);
		}
		return obj;
	}

	/**
	 * Get a String 3D property
	 *
	 * @param key the name of the property
	 * @return the String corresponding to that key, or null
	 * @throws Bad3DPropertyException if the property is missing or not of the expected type
	 */
	public String getString(String key) throws Bad3DPropertyException {
		Object obj = get(key);

		if (obj instanceof String) {
			return (String) obj;
		} else {
			throw new Bad3DPropertyException("Property named: " + key + " is not of type String");
		}
	}

	/**
	 * Get an AWT color 3D property
	 *
	 * @param key the name of the property
	 * @return the (AWT) Color corresponding to that key, or null
	 * @throws Bad3DPropertyException if the property is missing or not of the expected type
	 */
	public Color getColor(String key) throws Bad3DPropertyException {
		Object obj = get(key);

		if (obj instanceof Color) {
			return (Color) obj;
		} else {
			throw new Bad3DPropertyException("Property named: " + key + " is not of type Color");
		}
	}

	/**
	 * Get an a Font 3D property
	 *
	 * @param key the name of the property
	 * @return the Font corresponding to that key, or null
	 * @throws Bad3DPropertyException if the property is missing or not of the expected type
	 */
	public Font getFont(String key) throws Bad3DPropertyException {
		Object obj = get(key);

		if (obj instanceof Font) {
			return (Font) obj;
		} else {
			throw new Bad3DPropertyException("Property named: " + key + " is not of type Font");
		}
	}

	/**
	 * Get a integer 3D property
	 *
	 * @param key the name of the property
	 * @return the Integer corresponding to that key, or null
	 * @throws Bad3DPropertyException if the property is missing or not of the expected type
	 */
	public int getInt(String key) throws Bad3DPropertyException {
		Object obj = get(key);

		if (obj instanceof Integer) {
			return (Integer) obj;
		} else {
			throw new Bad3DPropertyException("Property named: " + key + " is not of type Integer");
		}
	}

	/**
	 * Get a float 3D property
	 *
	 * @param key the name of the property
	 * @return the Float corresponding to that key, or null
	 * @throws Bad3DPropertyException if the property is missing or not of the expected type
	 */
	public float getFloat(String key) throws Bad3DPropertyException {
		Object obj = get(key);

		if (obj instanceof Float) {
			return (Float) obj;
		} else {
			throw new Bad3DPropertyException("Property named: " + key + " is not of type Float");
		}
	}

	/**
	 * Convenience method to get the line width for this item
	 *
	 * @return the line width. (Default is 1f)
	 */
	public float getLineWidth() {
		try {
			return getFloat(LINE_WIDTH);
		} catch (Bad3DPropertyException e) {
			return _defaultLineWidth;
		}
	}

	/**
	 * Convenience method to set the line width
	 *
	 * @param lineWidth the line width.
	 */
	public void setLineWidth(float lineWidth) {
		put(LINE_WIDTH, lineWidth);
	}

	/**
	 * Convenience method to get the fill color for this item
	 *
	 * @return the fill color.
	 */
	public Color getFillColor() {
		try {
			Color color = getColor(FILLCOLOR);
			color = setAlphaOfColor(color, getFillAlpha());
			return color;
		} catch (Bad3DPropertyException e) {
			return _defaultFillColor;
		}
	}

	/**
	 * Convenience method to get the fill alpha for this item
	 *
	 * @return the fill alpha.
	 */
	public int getFillAlpha() {
	    try {
	        return getInt(FILLALPHA);
	    } catch (Bad3DPropertyException e) {
	        try {
	            return getColor(FILLCOLOR).getAlpha();
	        } catch (Bad3DPropertyException ignored) {
	            return _defaultFillAlpha;
	        }
	    }
	}

	/**
	 * Convenience method to get the line (frame) alpha for this item
	 *
	 * @return the line (frame) alpha.
	 */
	public int getLineAlpha() {
	    try {
	        return getInt(LINEALPHA);
	    } catch (Bad3DPropertyException e) {
	        try {
	            return getColor(LINECOLOR).getAlpha();
	        } catch (Bad3DPropertyException ignored) {
	            return _defaultLineAlpha;
	        }
	    }
	}

	/**
	 * Convenience method to get the line (and frame)color for this item
	 *
	 * @return the line (frame) color.
	 */
	public Color getLineColor() {
		try {
			Color color = getColor(LINECOLOR);
			color = setAlphaOfColor(color, getLineAlpha());
			return color;
		} catch (Bad3DPropertyException e) {
			return _defaultLineColor;
		}
	}

	/**
	 * Convenience method to get the text color for this item
	 *
	 * @return the text color.
	 */
	public Color getTextColor() {
		try {
			return getColor(TEXT_COLOR);
		} catch (Bad3DPropertyException e) {
			return _defaultTextColor;
		}
	}

	/**
	 * Convenience method to get the fontr for this item
	 *
	 * @return the font.
	 */
	public Font getFont() {
		try {
			return getFont(FONT);
		} catch (Bad3DPropertyException e) {
			return _defaultFont;
		}
	}

	/**
	 * Convenience method to set the fill color
	 *
	 * @param color the fill color.
	 */
	public void setFillColor(Color color) {
		put(FILLALPHA, color.getAlpha());
		put(FILLCOLOR, color);
	}

	/**
	 * Convenience method to set the line (frame) color
	 *
	 * @param color the line (frame) color.
	 */
	public void setLineColor(Color color) {
		put(LINEALPHA, color.getAlpha());
		put(LINECOLOR, color);
	}

	/**
	 * Convenience method to set the fill alpha
	 *
	 * @param alpha the fill alpha [0..255].
	 */
	public void setFillAlpha(int alpha) {
		put(FILLALPHA, alpha);
	}

	/**
	 * Convenience method to set the line (frame) alpha
	 *
	 * @param alpha the line (frame) alpha [0..255].
	 */
	public void setLineAlpha(int alpha) {
		put(LINEALPHA, alpha);
	}

	/**
	 * Convenience method to set the text color
	 *
	 * @param textColor the text color.
	 */
	public void setTextColor(Color textColor) {
		put(TEXT_COLOR, textColor);
	}

	/**
	 * Convenience method to set the font
	 *
	 * @param font the text font.
	 */
	public void setFont(Font font) {
		put(FONT, font);
	}

	/**
	 * Controls whether we want the item to be drawn. That is, drawing is skipped if
	 * this is false. Whether or not the item is actually visible when drawn is not
	 * controlled by this flag. This does NOT initiate a redraw,
	 *
	 * @param visible the visibility flag.
	 */
	public void setVisible(boolean visible) {
		_visible = visible;
	}

	/**
	 * Checks whether we want the item to be drawn. That is, drawing is skipped if
	 * this is false. Whether or not the item is actually visible when drawn is not
	 * controlled by this flag.
	 *
	 * @return the visibility flag.
	 */
	public boolean isVisible() {
		return _visible;
	}

	/**
	 * Get a list of the item's children, can be <code>null</code>
	 *
	 * @return a list of the item's children
	 */
	public List<Item3D> getChildren() {
		return _children;
	}

	/**
	 * Get the parent item (might be null)
	 *
	 * @return the parent
	 */
	public Item3D getParent() {
		return _parent;
	}

	/**
	 * Add a child item.
	 * <p>
	 * The remove-then-add (to move an existing child to the end) is performed
	 * under {@code synchronized(this)} so it is atomic with respect to {@link
	 * #childrenSnapshot()} — a concurrent drawer/disposer can never observe the
	 * child transiently missing.
	 * </p>
	 *
	 * @param item the child item
	 */
	public void addChild(Item3D item) {
		if (item != null) {
			synchronized (this) {
				if (_children == null) {
					_children = new Vector<>();
				}
				_children.remove(item);
				_children.addElement(item);
			}
			item._parent = this;
		}
	}

	/**
	 * Remove a child item
	 *
	 * @param item the chold item
	 */
	public void removeChild(Item3D item) {
		if (item != null) {
			synchronized (this) {
				if (_children != null) {
					_children.remove(item);
				}
			}
		}
	}

	/**
	 * Get the parent Panel3D object
	 *
	 * @return the parent Panel3D object
	 */
	public Panel3D getPanel3D() {
		return _panel3D;
	}

	/**
	 * Set the alpha of a color. If it already has the given alpha it just returns
	 * the color. If not, it makes a new color with the same RGB components and the
	 * new alpha
	 *
	 * @param color the color to change
	 * @param alpha the alpha [0..255] 0 is transparent, 255 is opaque
	 * @return the color
	 */
	protected Color setAlphaOfColor(Color color, int alpha) {

		alpha = Math.max(0, Math.min(255, alpha));
		int a = color.getAlpha();
		if (a == alpha) {
			return color;
		}

		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	/**
	 * A representative point for transparent sorting, in world coordinates.
	 * Default is origin. Override in items with a natural center (Sphere, Cube, Cylinder, etc.).
	 *
	 * @return the sort point as {@code [x, y, z]}
	 */
	public float[] getSortPoint() {
	    return new float[] {0f, 0f, 0f};
	}


}
