package edu.cnu.mdi.mdi3D.view3D;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.util.PropertyUtils;
import edu.cnu.mdi.view.BaseView;

/**
 * Base class for simple 3D MDI views backed by a {@link Panel3D}.
 * <p>
 * This class supports both the legacy {@code Object... keyVals}
 * construction pattern and the cleaner {@link Properties}-based pattern used by
 * {@link BaseView}. The legacy constructor is retained as a compatibility shim.
 * </p>
 */
@SuppressWarnings("serial")
public abstract class PlainView3D extends BaseView implements ActionListener {

	/** The hosted 3D panel. */
	protected final Panel3D _panel3D;

	/**
	 * Construct a 3D view from legacy alternating key/value pairs.
	 * <p>
	 * This constructor is retained for backward compatibility and simply converts
	 * the key/value pairs into {@link Properties} before delegating to
	 * {@link #PlainView3D(Properties)}.
	 * </p>
	 *
	 * @param keyVals alternating property key/value pairs
	 */
	public PlainView3D(Object... keyVals) {
		this(PropertyUtils.fromKeyValues(keyVals));
	}

	/**
	 * Construct a 3D view from a properties object.
	 *
	 * @param props the properties used to configure the view; may be null
	 */
	public PlainView3D(Properties props) {
		super(props);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		addMenus();

		float angleX = PropertyUtils.getFloat(properties, PropertyUtils.ANGLE_X);
		float angleY = PropertyUtils.getFloat(properties, PropertyUtils.ANGLE_Y);
		float angleZ = PropertyUtils.getFloat(properties, PropertyUtils.ANGLE_Z);
		float xDist = PropertyUtils.getFloat(properties, PropertyUtils.DIST_X);
		float yDist = PropertyUtils.getFloat(properties, PropertyUtils.DIST_Y);
		float zDist = PropertyUtils.getFloat(properties, PropertyUtils.DIST_Z);

		setLayout(new BorderLayout(1, 1));

		_panel3D = make3DPanel(angleX, angleY, angleZ, xDist, yDist, zDist);
		add(_panel3D, BorderLayout.CENTER);
		add(Box.createHorizontalStrut(1), BorderLayout.WEST);

	}
	


	/**
	 * Create the 3D panel for this view.
	 *
	 * @param angleX initial x rotation angle
	 * @param angleY initial y rotation angle
	 * @param angleZ initial z rotation angle
	 * @param xDist  initial camera x offset
	 * @param yDist  initial camera y offset
	 * @param zDist  initial camera z offset
	 * @return the created 3D panel
	 */
	protected abstract Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist,
			float zDist);

	/**
	 * Hook for subclasses to add menus to the view's menu bar.
	 */
	protected void addMenus() {
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	}

	@Override
	public void focusGained(FocusEvent e) {
		if (_panel3D != null) {
			_panel3D.requestFocus();
		}
	}

	@Override
	public void refresh() {
		if (_panel3D != null) {
			_panel3D.refresh();
		}
	}

	
	/**
	 * When a 3D internal frame becomes visible, schedule a refresh.
	 * This is especially important for lazily created views.
	 */
	@Override
	public void componentShown(ComponentEvent e) {
		if (_panel3D != null) _panel3D.softRefresh();
	}

	/**
	 * When a 3D internal frame is resized, schedule a refresh.
	 */
	@Override
	public void componentResized(ComponentEvent e) {
		if (_panel3D != null) _panel3D.softRefresh();
	}

	/**
	 * When a 3D internal frame is moved, schedule a refresh.
	 * This is a likely trigger in virtual-desktop activation.
	 */
	@Override
	public void componentMoved(ComponentEvent e) {
		if (_panel3D != null) _panel3D.softRefresh();
	}
	
	@Override
	protected IContainer resolveContainer(java.awt.geom.Rectangle2D.Double worldSystem) {
	    // 3D views render via GLJPanel; a BaseContainer overlay would sit on
	    // top of gljpanel and consume all mouse events. IContainers are
		// for 2D views, so we return null here to indicate that this view does not
		// support containers.
	    return null;
	}

	/**
	 * Schedule a best-effort refresh of the 3D panel after Swing has processed the
	 * current event.
	 */
	protected void request3DRefresh() {
		// GUARD CLAUSE: Prevent FBO context crashes by ensuring the panel has real
		// dimensions.
		// If Swing hasn't finished sizing the lazy view yet, ignore the render request.
		if (_panel3D == null || _panel3D.getWidth() <= 0 || _panel3D.getHeight() <= 0) {
			return;
		}

		// Queue the refresh to happen AFTER Swing finishes its current layout task
		SwingUtilities.invokeLater(() -> {
			refresh(); // This will call Panel3D.refresh()
		});
	}}