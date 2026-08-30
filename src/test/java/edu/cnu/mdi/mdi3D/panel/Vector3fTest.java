package edu.cnu.mdi.mdi3D.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Vector3fTest {

	private static final float DELTA = 1.0e-6f;

	@Test
	void nullConstructorIsOrigin() {
		Vector3f v = new Vector3f();
		assertEquals(0f, v.x, DELTA);
		assertEquals(0f, v.y, DELTA);
		assertEquals(0f, v.z, DELTA);
	}

	@Test
	void indexedConstructorReadsTheCorrectTriple() {
		float[] coords = { 1f, 2f, 3f, 4f, 5f, 6f };
		Vector3f v0 = new Vector3f(coords, 0);
		Vector3f v1 = new Vector3f(coords, 1);

		assertEquals(1f, v0.x, DELTA);
		assertEquals(2f, v0.y, DELTA);
		assertEquals(3f, v0.z, DELTA);

		assertEquals(4f, v1.x, DELTA);
		assertEquals(5f, v1.y, DELTA);
		assertEquals(6f, v1.z, DELTA);
	}

	@Test
	void lengthIsEuclideanNorm() {
		Vector3f v = new Vector3f(3f, 4f, 0f);
		assertEquals(5f, v.length(), DELTA);
	}

	@Test
	void dotOfOrthogonalUnitVectorsIsZero() {
		Vector3f x = new Vector3f(1f, 0f, 0f);
		Vector3f y = new Vector3f(0f, 1f, 0f);
		assertEquals(0f, x.dot(y), DELTA);
	}

	@Test
	void dotMatchesHandComputedValue() {
		Vector3f a = new Vector3f(1f, 2f, 3f);
		Vector3f b = new Vector3f(4f, 5f, 6f);
		// 1*4 + 2*5 + 3*6 = 32
		assertEquals(32f, a.dot(b), DELTA);
	}

	@Test
	void crossOfXAndYIsZ() {
		Vector3f x = new Vector3f(1f, 0f, 0f);
		Vector3f y = new Vector3f(0f, 1f, 0f);
		Vector3f z = x.cross(y);

		assertEquals(0f, z.x, DELTA);
		assertEquals(0f, z.y, DELTA);
		assertEquals(1f, z.z, DELTA);
	}

	@Test
	void crossIsPerpendicularToBothInputs() {
		Vector3f a = new Vector3f(2f, -1f, 0.5f);
		Vector3f b = new Vector3f(-3f, 4f, 1f);
		Vector3f cross = a.cross(b);

		assertEquals(0f, a.dot(cross), 1.0e-4f);
		assertEquals(0f, b.dot(cross), 1.0e-4f);
	}

	@Test
	void normalizeScalesToUnitLength() {
		Vector3f v = new Vector3f(3f, 4f, 0f);
		v.normalize();

		assertEquals(1f, v.length(), DELTA);
		assertEquals(0.6f, v.x, DELTA);
		assertEquals(0.8f, v.y, DELTA);
	}

	@Test
	void normalizeOfZeroVectorLeavesItUnchanged() {
		Vector3f v = new Vector3f(0f, 0f, 0f);
		v.normalize();

		assertEquals(0f, v.x, DELTA);
		assertEquals(0f, v.y, DELTA);
		assertEquals(0f, v.z, DELTA);
		assertTrue(Float.isFinite(v.x), "normalize() of a zero vector must not produce NaN");
	}

	@Test
	void midpointIsTheAverageOfBothVectors() {
		Vector3f a = new Vector3f(0f, 0f, 0f);
		Vector3f b = new Vector3f(2f, 4f, 6f);
		Vector3f mid = Vector3f.midpoint(a, b);

		assertEquals(1f, mid.x, DELTA);
		assertEquals(2f, mid.y, DELTA);
		assertEquals(3f, mid.z, DELTA);
	}
}
