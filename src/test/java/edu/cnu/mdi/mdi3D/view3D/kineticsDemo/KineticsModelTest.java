package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KineticsModelTest {

	@Test
	void sizeMatchesTheRequestedParticleCount() {
		KineticsModel model = new KineticsModel(37, 1.0f, 0.25f, 0.01f);
		assertEquals(37, model.size());
	}

	@Test
	void snapshotCoordinateBufferHasThreeFloatsPerParticle() {
		int count = 20;
		KineticsModel model = new KineticsModel(count, 1.0f, 0.25f, 0.01f);
		assertEquals(3 * count, model.getSnapshot().coords().length);
	}

	@Test
	void initialParticlesAreConfinedToTheSubVolume() {
		float length = 2.0f;
		float volumeFraction = 0.25f;
		float subBound = length * volumeFraction;

		KineticsModel model = new KineticsModel(200, length, volumeFraction, 0.01f);
		float[] coords = model.getSnapshot().coords();

		for (float c : coords) {
			assertTrue(c >= 0f && c <= subBound,
					"initial coordinate " + c + " must lie within [0, " + subBound + "]");
		}
	}

	@Test
	void particlesStayInsideTheBoundingBoxAfterManyUpdates() {
		float length = 1.0f;
		KineticsModel model = new KineticsModel(50, length, 0.25f, 1.0f);

		for (int i = 0; i < 500; i++) {
			model.update();
		}

		float[] coords = model.getSnapshot().coords();
		for (float c : coords) {
			assertTrue(c >= 0f && c <= length,
					"coordinate " + c + " escaped the elastic-wall bounding box [0, " + length + "]");
		}
	}

	@Test
	void updateAdvancesSimulationTimeByTheConfiguredStep() {
		KineticsModel model = new KineticsModel(5, 1.0f, 0.25f, 0.01f);
		model.setTimeStep(0.01f);

		model.update();
		model.update();

		assertEquals(0.02f, model.getSnapshot().time(), 1.0e-6f);
	}

	@Test
	void resetWithTheSameParticleCountConfinesToTheNewSubVolume() {
		// Use the same particle count and length as the constructor: reset()'s
		// coordinate buffers are allocated once, at the *constructor's* count,
		// and are final (see resetWithALargerParticleCountThrows below), and
		// reset()'s length parameter only sizes the initial confinement
		// sub-region, not the elastic-wall bounding box used by update() (see
		// the "Known limitation" note on reset()'s javadoc).
		float length = 1.0f;
		int count = 10;
		KineticsModel model = new KineticsModel(count, length, 0.25f, 0.01f);

		float newVolumeFraction = 0.5f;
		float subBound = length * newVolumeFraction;

		model.reset(count, length, newVolumeFraction, 0.02f);

		float[] coords = model.getSnapshot().coords();
		assertEquals(3 * count, coords.length);
		for (float c : coords) {
			assertTrue(c >= 0f && c <= subBound,
					"coordinate " + c + " must lie within the new sub-volume [0, " + subBound + "]");
		}
	}

	@Test
	void resetWithALargerParticleCountThrows() {
		// KNOWN BUG, pinned here rather than silently worked around: the
		// double-buffered coordinate arrays are allocated once in the
		// constructor at `count * 3` and are final. reset(count, ...) accepts
		// a new count and repopulates internalState with that many particles,
		// but can never resize the buffers. A reset() to a larger count
		// overflows them. If this starts passing, the buffer-resize was fixed
		// — replace this test with one asserting the new count is honored.
		KineticsModel model = new KineticsModel(10, 1.0f, 0.25f, 0.01f);
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> model.reset(64, 1.0f, 0.25f, 0.01f));
	}

	@Test
	void resetLengthParameterDoesNotResizeTheElasticWallBoundingBox() {
		// Documents the known limitation on KineticsModel.reset(): the wall
		// bounding box is fixed at construction time and is NOT updated by a
		// later reset() call, even though reset() also takes a "length"
		// parameter. If this ever starts failing, either the limitation was
		// fixed (great — update the javadoc and this test) or a regression
		// reintroduced box/seed inconsistency the other way.
		float constructedLength = 1.0f;
		KineticsModel model = new KineticsModel(30, constructedLength, 0.25f, 1.0f);

		float largerResetLength = 5.0f;
		model.reset(30, largerResetLength, 1.0f, 1.0f);

		for (int i = 0; i < 500; i++) {
			model.update();
		}

		float[] coords = model.getSnapshot().coords();
		for (float c : coords) {
			assertTrue(c >= 0f && c <= constructedLength,
					"particles remain bounded by the constructor's length ("
							+ constructedLength + "), not reset()'s length (" + largerResetLength + ")");
		}
	}
}
