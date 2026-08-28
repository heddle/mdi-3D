package edu.cnu.mdi.mdi3D.view2D;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;

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
			"This 2D view is included in the MDI-3D demo to emphasize\n"
			+ "that 2D and 3D views can coexist in the same MDI\n"
			+ "application.";

	/**
	 * Construct the view using {@link DrawingView}'s own defaults (toolbar,
	 * background, aspect, etc.), just with a different title and one
	 * pre-placed text item.
	 */
	public DrawingDemoView() {
		super((Object[]) null);
		setTitle(TITLE);

		Font font = new Font("SansSerif", Font.PLAIN, 14);
		new TextItem(getAnnotationLayer(), new Point2D.Double(0.06, 0.55), font,
				EXPLANATION, Color.darkGray, null, Color.black);

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
