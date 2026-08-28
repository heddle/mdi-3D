package edu.cnu.mdi.mdi3D.view2D;

import java.util.List;

import edu.cnu.mdi.view.AbstractViewInfo;
import edu.cnu.mdi.view.DrawingView;

/**
 * View information for the 2D Drawing View included in the MDI-3D demo.
 */
public class DrawingDemoViewInfo extends AbstractViewInfo {

	@Override
	public String getTitle() {
		return "2D Drawing View";
	}

	@Override
	public String getPurpose() {
		return """
				MDI-3D is an optional extension of MDI, not a replacement for
				it. This view makes that concrete: it is an ordinary
				two-dimensional, item-based view — the same DrawingView class
				the base MDI demo application uses — sitting in the same
				virtual desktop as the Kinetics, Aizawa, Logo, Scatter, and
				Globe 3D views.

				A single MDI application can host any mix of 2D and 3D views
				side by side, sharing the same menu bar, view menu, and
				virtual-desktop window management. Nothing about this view is
				3D-specific.
				""";
	}

	@Override
	public List<String> getUsageBullets() {
		return List.of(
				"Use the standard drawing toolbar to add shapes, lines, and text of your own.",
				"Drag an image file onto the canvas to drop it in as an item.",
				"The pre-placed text item, like any other item, can be selected, moved, or deleted."
		);
	}

	@Override
	public String getTechnicalNotes() {
		return """
				This class subclasses %s directly and changes nothing about
				its behavior except the title and one item created at
				construction time — the standard toolbar, background, aspect,
				and image drag-and-drop support are all inherited unchanged.
				""".formatted(DrawingView.class.getSimpleName());
	}

	@Override
	public String getFooter() {
		return "MDI-3D demo: an ordinary 2D view, included to show 2D and 3D coexisting.";
	}

	@Override
	protected String getAccentColorHex() {
		return "#5a6b7a";
	}
}
