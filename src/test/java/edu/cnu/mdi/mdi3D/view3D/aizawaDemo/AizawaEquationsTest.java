package edu.cnu.mdi.mdi3D.view3D.aizawaDemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class AizawaEquationsTest {

	private static final double DELTA = 1.0e-12;

	@Test
	void dimensionIsThree() {
		assertEquals(3, new AizawaEquations().getDimension());
	}

	@Test
	void defaultInitialStateMatchesDocumentedValue() {
		double[] s = AizawaEquations.defaultInitialState();
		assertEquals(3, s.length);
		assertEquals(0.1, s[0], DELTA);
		assertEquals(0.0, s[1], DELTA);
		assertEquals(0.0, s[2], DELTA);
	}

	@Test
	void defaultInitialStateReturnsAFreshArrayEachCall() {
		double[] first = AizawaEquations.defaultInitialState();
		double[] second = AizawaEquations.defaultInitialState();
		assertNotSame(first, second, "each call must return an independent array");

		first[0] = 999.0;
		assertEquals(0.1, AizawaEquations.defaultInitialState()[0], DELTA,
				"mutating a previously returned array must not affect later calls");
	}

	@Test
	void accessorsReturnTheConstructorParameters() {
		AizawaEquations eq = new AizawaEquations(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);
		assertEquals(1.0, eq.getA(), DELTA);
		assertEquals(2.0, eq.getB(), DELTA);
		assertEquals(3.0, eq.getC(), DELTA);
		assertEquals(4.0, eq.getD(), DELTA);
		assertEquals(5.0, eq.getE(), DELTA);
		assertEquals(6.0, eq.getF(), DELTA);
	}

	@Test
	void noArgConstructorUsesDocumentedDefaults() {
		AizawaEquations eq = new AizawaEquations();
		assertEquals(AizawaEquations.DEFAULT_A, eq.getA(), DELTA);
		assertEquals(AizawaEquations.DEFAULT_B, eq.getB(), DELTA);
		assertEquals(AizawaEquations.DEFAULT_C, eq.getC(), DELTA);
		assertEquals(AizawaEquations.DEFAULT_D, eq.getD(), DELTA);
		assertEquals(AizawaEquations.DEFAULT_E, eq.getE(), DELTA);
		assertEquals(AizawaEquations.DEFAULT_F, eq.getF(), DELTA);
	}

	@Test
	void computeDerivativesAtTheOriginMatchesTheClosedFormForZeroState() {
		// At (x, y, z) = (0, 0, 0):
		// dx/dt = (0 - b)*0 - d*0 = 0
		// dy/dt = d*0 + (0 - b)*0 = 0
		// dz/dt = c + a*0 - 0 - 0 + f*0 = c
		AizawaEquations eq = new AizawaEquations(0.95, 0.70, 0.60, 3.50, 0.25, 0.10);
		double[] yDot = new double[3];

		eq.computeDerivatives(0.0, new double[] { 0.0, 0.0, 0.0 }, yDot);

		assertEquals(0.0, yDot[0], DELTA);
		assertEquals(0.0, yDot[1], DELTA);
		assertEquals(0.60, yDot[2], DELTA);
	}

	@Test
	void computeDerivativesMatchesHandComputedValueAtAGeneralState() {
		// Simple integer parameters and state chosen so the arithmetic can be
		// verified by hand:
		// a=1, b=1, c=1, d=1, e=0, f=0; state (x, y, z) = (1, 2, 3)
		//
		// zMinusB = z - b = 2
		// dx/dt = zMinusB*x - d*y = 2*1 - 1*2 = 0
		// dy/dt = d*x + zMinusB*y = 1*1 + 2*2 = 5
		// dz/dt = c + a*z - z^3/3 - (x^2+y^2)*(1+e*z) + f*z*x^3
		//       = 1 + 3 - 9 - 5*1 + 0 = -10
		AizawaEquations eq = new AizawaEquations(1.0, 1.0, 1.0, 1.0, 0.0, 0.0);
		double[] yDot = new double[3];

		eq.computeDerivatives(0.0, new double[] { 1.0, 2.0, 3.0 }, yDot);

		assertEquals(0.0, yDot[0], DELTA);
		assertEquals(5.0, yDot[1], DELTA);
		assertEquals(-10.0, yDot[2], DELTA);
	}
}
