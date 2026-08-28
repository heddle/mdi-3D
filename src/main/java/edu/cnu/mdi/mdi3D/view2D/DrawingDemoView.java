package edu.cnu.mdi.mdi3D.view2D;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;

import edu.cnu.mdi.container.CreationSupport;
import edu.cnu.mdi.item.TextItem;
import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.DrawingView;

/**
 * An ordinary 2D drawing view, included in the MDI-3D demo application
 * specifically to demonstrate that 2D and 3D MDI views coexist in the same
 * application.
 * <p>
 * This is otherwise identical to {@link DrawingView} — the same view the
 * base MDI demo application uses — with two differences: the title, and one
 * explanatory {@link TextItem} already placed on the annotation layer so the
 * point is visible the moment the view opens, without requiring any
 * interaction.
 * </p>
 */
@SuppressWarnings("serial")
public class DrawingDemoView extends DrawingView {

	/** Frame title for this view. */
	private static final String TITLE = "2D Drawing View";

	/** Text of the pre-placed explanatory item. */
	private static final String EXPLANATION =
			  "This 2D view is included in the MDI-3D demo to emphasize that\n"
			+ "2D and 3D views can coexist within the same MDI application.";

	/** Guards against adding the explanation item more than once. */
	private boolean explanationAdded;

	/**
	 * Construct the view using {@link DrawingView}'s own defaults (toolbar,
	 * background, aspect, etc.), just with a different title.
	 * <p>
	 * The explanatory {@link TextItem} is <em>not</em> created here. A
	 * {@code TextItem}'s world-space bounds are derived from the container's
	 * current local-to-world mapping ({@code IContainer.worldToLocal}/
	 * {@code localToWorld}), which depends on the container's canvas having a
	 * real, realized pixel size. Immediately after construction — before this
	 * view has been laid out inside the desktop — that size can still be
	 * zero, which would give the item degenerate (invisible) bounds. See
	 * {@link #componentShown(ComponentEvent)}.
	 * </p>
	 */
	public DrawingDemoView() {
		super((Object[]) null);
		setTitle(TITLE);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Adds the explanatory text item the first time this view is actually
	 * shown with a real size, exactly the same "lazily created view" pattern
	 * {@code PlainView3D} uses in MDI-3D for the same reason. Guarded by
	 * {@link #explanationAdded} so a later hide/show cycle does not add it
	 * twice.
	 * </p>
	 */
	@Override
	public void componentShown(ComponentEvent e) {
		super.componentShown(e);
		if (explanationAdded) {
			return;
		}
		explanationAdded = true;

		Font font = new Font("SansSerif", Font.PLAIN, 14);
		TextItem item = new TextItem(getAnnotationLayer(), new Point2D.Double(0.5, 0.55), font,
				EXPLANATION, Color.darkGray, null, Color.black);
		CreationSupport.defaultConfigureItem(item);
		refresh();
	}

	/**
	 * Compatibility factory, matching the {@code create...View()} pattern
	 * used by the other MDI-3D demos.
	 *
	 * @return a new default-configured drawing demo view
	 */
	public static DrawingDemoView createDrawingDemoView() {
		return new DrawingDemoView();
	}

	@Override
	public AbstractViewInfo getViewInfo() {
		return new DrawingDemoViewInfo();
	}
}
