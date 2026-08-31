package edu.cnu.mdi.mdi3D.panel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Support3D}'s pure-math recursive triangle-subdivision
 * helpers ({@code triangulateTriangle}, {@code oneToFourTriangle}). These
 * involve no OpenGL context, unlike almost everything else in {@code Support3D}.
 */
class Support3DTriangulationTest {

	private static final float DELTA = 1.0e-5f;

	// A right triangle in the z=0 plane: (0,0,0), (2,0,0), (0,2,0). Area = 2.
	private static final float[] RIGHT_TRIANGLE = { 0, 0, 0, 2, 0, 0, 0, 2, 0 };
	private static final float RIGHT_TRIANGLE_AREA = 2.0f;

	private static float triangleArea(float[] coords, int triIndex) {
		int i = 9 * triIndex;
		Vector3f a = new Vector3f(coords[i], coords[i + 1], coords[i + 2]);
		Vector3f b = new Vector3f(coords[i + 3], coords[i + 4], coords[i + 5]);
		Vector3f c = new Vector3f(coords[i + 6], coords[i + 7], coords[i + 8]);
		Vector3f ab = new Vector3f(b.x - a.x, b.y - a.y, b.z - a.z);
		Vector3f ac = new Vector3f(c.x - a.x, c.y - a.y, c.z - a.z);
		return 0.5f * ab.cross(ac).length();
	}

	private static float totalArea(float[] coords) {
		int numTri = coords.length / 9;
		float sum = 0f;
		for (int i = 0; i < numTri; i++) {
			sum += triangleArea(coords, i);
		}
		return sum;
	}

	@Test
	void levelZeroReturnsInputUnchanged() {
		assertSame(RIGHT_TRIANGLE, Support3D.triangulateTriangle(RIGHT_TRIANGLE, 0));
	}

	@Test
	void negativeLevelReturnsInputUnchanged() {
		assertSame(RIGHT_TRIANGLE, Support3D.triangulateTriangle(RIGHT_TRIANGLE, -3));
	}

	@Test
	void levelOneProducesFourTrianglesByEdgeMidpoints() {
		// p0=(0,0,0) p1=(2,0,0) p2=(0,2,0)
		// midpoints: p3=mid(p0,p1)=(1,0,0) p4=mid(p1,p2)=(1,1,0) p5=mid(p2,p0)=(0,1,0)
		float[] expected = {
				0, 0, 0, 1, 0, 0, 0, 1, 0, // p0,p3,p5
				2, 0, 0, 1, 0, 0, 1, 1, 0, // p1,p3,p4
				1, 0, 0, 1, 1, 0, 0, 1, 0, // p3,p4,p5
				0, 2, 0, 1, 1, 0, 0, 1, 0, // p2,p4,p5
		};
		assertArrayEquals(expected, Support3D.triangulateTriangle(RIGHT_TRIANGLE, 1), DELTA);
	}

	@Test
	void triangleCountQuadruplesPerLevel() {
		assertEquals(9 * 4, Support3D.triangulateTriangle(RIGHT_TRIANGLE, 1).length);
		assertEquals(9 * 16, Support3D.triangulateTriangle(RIGHT_TRIANGLE, 2).length);
		assertEquals(9 * 64, Support3D.triangulateTriangle(RIGHT_TRIANGLE, 3).length);
	}

	@Test
	void subdivisionConservesTotalAreaAcrossLevels() {
		for (int level = 1; level <= 4; level++) {
			float[] result = Support3D.triangulateTriangle(RIGHT_TRIANGLE, level);
			assertEquals(RIGHT_TRIANGLE_AREA, totalArea(result), DELTA,
					"total area should be conserved at level " + level);
		}
	}

	@Test
	void oneToFourTriangleSingleArgDefaultsToIndexZero() {
		assertArrayEquals(Support3D.oneToFourTriangle(RIGHT_TRIANGLE, 0), Support3D.oneToFourTriangle(RIGHT_TRIANGLE),
				0f);
	}

	@Test
	void oneToFourTriangleHonorsNonZeroIndexIntoAPackedArray() {
		// Two packed triangles: index 0 is a degenerate all-zero triangle, index 1 is RIGHT_TRIANGLE.
		float[] packed = new float[18];
		System.arraycopy(RIGHT_TRIANGLE, 0, packed, 9, 9);

		float[] fromIndex1 = Support3D.oneToFourTriangle(packed, 1);
		float[] fromRightTriangleAlone = Support3D.oneToFourTriangle(RIGHT_TRIANGLE, 0);
		assertArrayEquals(fromRightTriangleAlone, fromIndex1, DELTA);
	}
}
