package edu.cnu.mdi.mdi3D.view3D.aizawaDemo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AizawaModelTest {

	private static final double DELTA = 1.0e-9;

	@Test
	void constructorRejectsNullEquations() {
		assertThrows(IllegalArgumentException.class,
				() -> new AizawaModel(null, AizawaEquations.defaultInitialState(), 0.01, 1, 100, 4));
	}

	@Test
	void constructorRejectsShortInitialState() {
		assertThrows(IllegalArgumentException.class,
				() -> new AizawaModel(new AizawaEquations(), new double[] { 1.0, 2.0 }, 0.01, 1, 100, 4));
	}

	@Test
	void constructorSeedsOnePointAtTheInitialState() {
		double[] initial = { 1.0, 2.0, 3.0 };
		AizawaModel model = new AizawaModel(new AizawaEquations(), initial, 0.01, 1, 0, 4);

		assertEquals(1, model.getPointCount());
		assertArrayEquals(initial, model.getStateCopy(), DELTA);

		AizawaSnapshot snap = model.getSnapshot();
		assertEquals(3, snap.coords().length);
		assertEquals((float) initial[0], snap.coords()[0]);
		assertEquals((float) initial[1], snap.coords()[1]);
		assertEquals((float) initial[2], snap.coords()[2]);
	}

	@Test
	void nonPositiveDtFallsBackToTheDefault() {
		AizawaModel model = new AizawaModel(new AizawaEquations(), AizawaEquations.defaultInitialState(), -1.0, 1, 0, 4);
		assertEquals(AizawaModel.DEFAULT_DT, model.getDt(), DELTA);
	}

	@Test
	void nonPositiveSubstepsIsPromotedToOne() {
		AizawaModel model = new AizawaModel(new AizawaEquations(), AizawaEquations.defaultInitialState(), 0.01, 0, 0, 4);
		assertEquals(1, model.getSubstepsPerUpdate());
	}

	@Test
	void updateAdvancesTimeBySubstepsTimesDt() {
		AizawaModel model = new AizawaModel(new AizawaEquations(), AizawaEquations.defaultInitialState(), 0.01, 4, 0, 4);

		model.update();

		assertEquals(0.04, model.getTime(), 1.0e-9);
		// one seed point + 4 substep points
		assertEquals(5, model.getPointCount());
	}

	@Test
	void resetReturnsToOnePointAtTheNewState() {
		AizawaModel model = new AizawaModel(new AizawaEquations(), AizawaEquations.defaultInitialState(), 0.01, 4, 0, 4);
		model.update();
		model.update();
		assertTrue(model.getPointCount() > 1);

		double[] newState = { 5.0, 6.0, 7.0 };
		model.reset(newState);

		assertEquals(1, model.getPointCount());
		assertEquals(0.0, model.getTime(), DELTA);
		assertArrayEquals(newState, model.getStateCopy(), DELTA);
	}

	@Test
	void trailIsCappedAtMaxTrailPointsAndKeepsTheMostRecentPoints() {
		// substepsPerUpdate = 1 so each update() appends exactly one point.
		int maxTrailPoints = 5;
		AizawaModel model = new AizawaModel(new AizawaEquations(), AizawaEquations.defaultInitialState(), 0.01, 1,
				maxTrailPoints, 4);

		// Seed point counts as point 1; 10 more updates would give 11 total
		// without capping.
		for (int i = 0; i < 10; i++) {
			model.update();
		}

		assertEquals(maxTrailPoints, model.getPointCount(),
				"point count must never exceed the configured maximum");

		float[] coords = model.getSnapshot().coords();
		assertEquals(3 * maxTrailPoints, coords.length);

		// The most recent point in the trail must equal the current state
		// (the oldest points, not the newest, are the ones discarded).
		double[] state = model.getStateCopy();
		int lastIndex = coords.length - 3;
		assertEquals((float) state[0], coords[lastIndex]);
		assertEquals((float) state[1], coords[lastIndex + 1]);
		assertEquals((float) state[2], coords[lastIndex + 2]);
	}

	@Test
	void getStateCopyIsIndependentOfInternalState() {
		AizawaModel model = new AizawaModel(new AizawaEquations(), AizawaEquations.defaultInitialState(), 0.01, 1, 0, 4);

		double[] copy = model.getStateCopy();
		copy[0] = 12345.0;

		assertEquals(AizawaEquations.defaultInitialState()[0], model.getStateCopy()[0], DELTA,
				"mutating a returned copy must not affect the model's internal state");
	}
}
