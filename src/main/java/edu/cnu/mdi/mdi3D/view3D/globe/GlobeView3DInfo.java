package edu.cnu.mdi.mdi3D.view3D.globe;

import java.util.List;

import edu.cnu.mdi.view.AbstractViewInfo;

/**
 * View information for the 3D Globe view.
 */
public class GlobeView3DInfo extends AbstractViewInfo {

	@Override
	public String getTitle() {
		return "3D Globe";
	}

	@Override
	public String getPurpose() {
		return """
				A lightweight, interactive 3D globe: country outlines and city
				labels rendered directly on a unit sphere. Longitude/latitude
				coordinates are mapped straight to points on the sphere — there
				is no map projection here, which is the point: it demonstrates
				the same GeoJSON data MDI's 2D mapping views use, but in a true
				3D context instead of projected onto a flat plane.
				""";
	}

	@Override
	public List<String> getUsageBullets() {
		return List.of(
				"Drag to rotate the globe (arcball); scroll to zoom.",
				"There is no simulation to start here — this view is purely interactive and redraws only in "
						+ "response to input.",
				"Country outlines that cross the antimeridian (the +/-180 degree seam) are drawn as split "
						+ "segments so they don't wrap incorrectly across the globe."
		);
	}

	@Override
	public String getTechnicalNotes() {
		return """
				This view does not maintain its own copy of the world's
				geography. It loads the same GeoJSON country and city resources,
				through the same edu.cnu.mdi.mapping loaders, that the 2D mapping
				package uses. GlobeCountryLines3D converts each ring of
				longitude/latitude points to unit-sphere Cartesian coordinates
				and splits any strip that jumps sharply in longitude, so a
				country that straddles the dateline still renders as two
				sensible pieces rather than one line cutting across the globe.

				Unlike the Kinetics, Aizawa, and Logo demos, this view
				deliberately extends PlainView3D rather than SimulationView3D:
				there is no time-evolving state to advance, so the simulation
				lifecycle (run/pause/resume) would not mean anything here.
				""";
	}

	@Override
	public String getFooter() {
		return "MDI-3D geography demo: shared GeoJSON data, rendered in true 3D.";
	}

	@Override
	protected String getAccentColorHex() {
		return "#2980b9";
	}
}
