package edu.cnu.mdi.mdi3D.item3D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.mapping.loader.GeoJsonCountryLoader.CountryFeature;

/**
 * Tests for {@link GlobeCountryLines3D}'s pure-logic ring processing: dateline
 * splitting and the lon/lat-to-xyz sphere mapping. None of this touches
 * OpenGL (only {@code draw(GLAutoDrawable)} does), so a {@code null} owner
 * panel is fine for construction.
 */
class GlobeCountryLines3DTest {

	private static final float DELTA = 1.0e-5f;
	private static final float RADIUS = 1.0f;

	private static Point2D.Double pt(double lonRad, double latRad) {
		return new Point2D.Double(lonRad, latRad);
	}

	private static CountryFeature feature(List<List<Point2D.Double>> polygons) {
		return new CountryFeature("Testland", "TST", polygons);
	}

	private static GlobeCountryLines3D build(List<Point2D.Double> ring) {
		GlobeCountryLines3D item = new GlobeCountryLines3D(null,
				List.of(feature(List.of(ring))), RADIUS);
		item.setRadialLift(0f); // simplify expected-value math
		item.rebuild(List.of(feature(List.of(ring)))); // re-run with lift already 0
		return item;
	}

	@Test
	void constructorRejectsNullFeatureList() {
		assertThrows(NullPointerException.class, () -> new GlobeCountryLines3D(null, null, RADIUS));
	}

	@Test
	void ringWithoutDatelineCrossingProducesOneStrip() {
		List<Point2D.Double> ring = List.of(pt(0.0, 0.0), pt(0.1, 0.0), pt(0.2, 0.0));
		GlobeCountryLines3D item = build(ring);

		assertEquals(1, item.stripCount());
		assertEquals(9, item.strip(0).length); // 3 vertices * 3 floats
	}

	@Test
	void lonLatIsMappedToSphereSurfaceUsingStandardConvention() {
		// lon=0, lat=0 -> (r, 0, 0); lon=pi/2, lat=0 -> (0, r, 0)
		List<Point2D.Double> ring = List.of(pt(0.0, 0.0), pt(Math.PI / 2, 0.0));
		GlobeCountryLines3D item = build(ring);

		float[] strip = item.strip(0);
		assertEquals(RADIUS, strip[0], DELTA);
		assertEquals(0f, strip[1], DELTA);
		assertEquals(0f, strip[2], DELTA);

		assertEquals(0f, strip[3], DELTA);
		assertEquals(RADIUS, strip[4], DELTA);
		assertEquals(0f, strip[5], DELTA);
	}

	@Test
	void radialLiftIncreasesEffectiveRadius() {
		List<Point2D.Double> ring = List.of(pt(0.0, 0.0), pt(0.1, 0.0));
		GlobeCountryLines3D item = new GlobeCountryLines3D(null, List.of(feature(List.of(ring))), RADIUS);
		item.setRadialLift(0.5f);
		item.rebuild(List.of(feature(List.of(ring))));

		float[] strip = item.strip(0);
		assertEquals(RADIUS + 0.5f, strip[0], DELTA); // lon=0,lat=0 -> (r+lift, 0, 0)
	}

	@Test
	void segmentCrossingTheDatelineSplitsIntoTwoStrips() {
		// -3.0 -> -2.9 (dLon=0.1, no split) -> 3.0 (dLon=5.9 > pi, split) -> 2.9 (dLon=-0.1, no split)
		List<Point2D.Double> ring = List.of(pt(-3.0, 0.0), pt(-2.9, 0.0), pt(3.0, 0.0), pt(2.9, 0.0));
		GlobeCountryLines3D item = build(ring);

		assertEquals(2, item.stripCount());
		assertEquals(6, item.strip(0).length); // 2 vertices before the seam
		assertEquals(6, item.strip(1).length); // 2 vertices after the seam
	}

	@Test
	void customDatelineThresholdChangesWhereSplitsOccur() {
		// dLon = 0.5; with the default threshold (pi) this would NOT split, but a
		// tight custom threshold of 0.1 forces a split.
		List<Point2D.Double> ring = List.of(pt(0.0, 0.0), pt(0.5, 0.0));
		GlobeCountryLines3D item = new GlobeCountryLines3D(null, List.of(feature(List.of(ring))), RADIUS);
		item.setDatelineSplitThreshold(0.1);
		item.rebuild(List.of(feature(List.of(ring))));

		// each point ends up alone in its own (too-short, <2 vertex) strip, so nothing is kept
		assertEquals(0, item.stripCount());
	}

	@Test
	void nonPositiveDatelineThresholdRevertsToDefault() {
		GlobeCountryLines3D item = build(List.of(pt(0.0, 0.0), pt(0.1, 0.0)));
		item.setDatelineSplitThreshold(-1.0);
		assertEquals(GlobeCountryLines3D.DEFAULT_DATELINE_SPLIT_THRESHOLD, item.getDatelineSplitThreshold(), DELTA);
	}

	@Test
	void ringsShorterThanTwoPointsProduceNoStrip() {
		GlobeCountryLines3D item = build(List.of(pt(0.0, 0.0)));
		assertEquals(0, item.stripCount());
	}

	@Test
	void nullPointsWithinARingAreSkipped() {
		List<Point2D.Double> ring = Arrays.asList(pt(0.0, 0.0), null, pt(0.1, 0.0));
		GlobeCountryLines3D item = build(ring);

		assertEquals(1, item.stripCount());
		assertEquals(6, item.strip(0).length); // the two non-null points
	}

	@Test
	void nullFeaturesInTheListAreSkippedDuringRebuild() {
		List<CountryFeature> features = new ArrayList<>();
		features.add(null);
		features.add(feature(List.of(List.of(pt(0.0, 0.0), pt(0.1, 0.0)))));

		GlobeCountryLines3D item = new GlobeCountryLines3D(null, Collections.unmodifiableList(features), RADIUS);
		item.setRadialLift(0f);
		item.rebuild(features);

		assertEquals(1, item.stripCount());
	}

	@Test
	void lineWidthIsClampedToAtLeastOne() {
		GlobeCountryLines3D item = build(List.of(pt(0.0, 0.0), pt(0.1, 0.0)));
		item.setLineWidth(0.2f);
		assertEquals(1.0f, item.getLineWidth(), DELTA);

		item.setLineWidth(3.5f);
		assertEquals(3.5f, item.getLineWidth(), DELTA);
	}
}
