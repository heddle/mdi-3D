package edu.cnu.mdi.mdi3D.view3D.kineticsDemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
	void resetWithALargerParticleCountReallocatesTheBuffersInsteadOfOverflowing() {
		KineticsModel model = new KineticsModel(10, 1.0f, 0.25f, 0.01f);

		int largerCount = 64;
		model.reset(largerCount, 1.0f, 0.25f, 0.01f);

		assertEquals(largerCount, model.size());
		assertEquals(3 * largerCount, model.getSnapshot().coords().length);
	}

	@Test
	void resetWithASmallerParticleCountLeavesNoStaleTrailingCoordinates() {
		KineticsModel model = new KineticsModel(64, 1.0f, 0.25f, 0.01f);

		int smallerCount = 10;
		model.reset(smallerCount, 1.0f, 0.25f, 0.01f);

		assertEquals(smallerCount, model.size());
		assertEquals(3 * smallerCount, model.getSnapshot().coords().length,
				"the snapshot buffer must shrink to the new count, not retain the old (larger) size");
	}

	@Test
	void resetWithANewLengthBecomesTheElasticWallBoundingBoxForSubsequentUpdates() {
		KineticsModel model = new KineticsModel(30, 1.0f, 0.25f, 1.0f);

		float newLength = 5.0f;
		model.reset(30, newLength, 1.0f, 1.0f);

		for (int i = 0; i < 500; i++) {
			model.update();
		}

		float[] coords = model.getSnapshot().coords();
		boolean sawCoordinateBeyondTheOldBox = false;
		for (float c : coords) {
			assertTrue(c >= 0f && c <= newLength,
					"coordinate " + c + " escaped the new elastic-wall bounding box [0, " + newLength + "]");
			sawCoordinateBeyondTheOldBox |= c > 1.0f;
		}
		assertTrue(sawCoordinateBeyondTheOldBox,
				"expected at least one particle to have moved beyond the old box's length (1.0) "
						+ "into the new, larger box - otherwise this test can't tell reset() apart from a no-op");
	}
}
