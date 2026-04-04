package edu.cnu.mdi.mdi3D.view3D.aizawaDemo;

import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

/**
 * Implements the six-parameter Aizawa attractor as a system of three
 * first-order ordinary differential equations.
 * <p>
 * The state vector is:
 *
 * <pre>
 * y[0] = x
 * y[1] = y
 * y[2] = z
 * </pre>
 *
 * and the governing equations are:
 *
 * <pre>
 * dx/dt = (z - b) x - d y
 * dy/dt = d x + (z - b) y
 * dz/dt = c + a z - z^3 / 3 - (x^2 + y^2)(1 + e z) + f z x^3
 * </pre>
 *
 * This is the more commonly used Aizawa system for the familiar strange-attractor
 * visualizations.
 * <p>
 * The class is immutable and thread-safe after construction.
 */
public class AizawaEquations implements FirstOrderDifferentialEquations {

	/** Default value of parameter a. */
	public static final double DEFAULT_A = 0.95;

	/** Default value of parameter b. */
	public static final double DEFAULT_B = 0.70;

	/** Default value of parameter c. */
	public static final double DEFAULT_C = 0.60;

	/** Default value of parameter d. */
	public static final double DEFAULT_D = 3.50;

	/** Default value of parameter e. */
	public static final double DEFAULT_E = 0.25;

	/** Default value of parameter f. */
	public static final double DEFAULT_F = 0.10;

	/**
	 * The dimension of the Aizawa system.
	 * <p>
	 * The state contains the three coordinates {@code x}, {@code y}, and
	 * {@code z}.
	 */
	public static final int DIMENSION = 3;

	/** System parameter a. */
	private final double a;

	/** System parameter b. */
	private final double b;

	/** System parameter c. */
	private final double c;

	/** System parameter d. */
	private final double d;

	/** System parameter e. */
	private final double e;

	/** System parameter f. */
	private final double f;

	/**
	 * Creates an Aizawa system using standard demo parameters.
	 */
	public AizawaEquations() {
		this(DEFAULT_A, DEFAULT_B, DEFAULT_C, DEFAULT_D, DEFAULT_E, DEFAULT_F);
	}

	/**
	 * Creates an Aizawa system with the specified parameter values.
	 *
	 * @param a the linear coefficient multiplying {@code z} in {@code dz/dt}
	 * @param b the shift applied to {@code z} in the {@code x} and {@code y}
	 *          equations
	 * @param c the constant driving term in {@code dz/dt}
	 * @param d the rotation-like coupling between {@code x} and {@code y}
	 * @param e the coefficient in the nonlinear factor {@code (1 + e z)}
	 * @param f the coefficient of the nonlinear term {@code z x^3} in
	 *          {@code dz/dt}
	 */
	public AizawaEquations(double a, double b, double c, double d, double e, double f) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
	}

	/**
	 * Returns a fresh default initial state for integrations of this system.
	 * <p>
	 * A new array is returned on each call so that client code can modify the
	 * returned state safely without changing shared static data.
	 *
	 * @return a new three-element array containing a standard initial condition
	 */
	public static double[] defaultInitialState() {
		return new double[] { 0.1, 0.0, 0.0 };
	}

	/**
	 * Returns the number of coupled first-order equations in the system.
	 *
	 * @return always {@value #DIMENSION}
	 */
	@Override
	public int getDimension() {
		return DIMENSION;
	}

	/**
	 * Computes the time derivatives of the Aizawa state vector.
	 * <p>
	 * The input state array is interpreted as:
	 *
	 * <pre>
	 * state[0] = x
	 * state[1] = y
	 * state[2] = z
	 * </pre>
	 *
	 * and the output derivative array is filled as:
	 *
	 * <pre>
	 * yDot[0] = dx/dt
	 * yDot[1] = dy/dt
	 * yDot[2] = dz/dt
	 * </pre>
	 *
	 * @param t the independent variable, usually time; unused here because the
	 *          system is autonomous
	 * @param y the current state vector
	 * @param yDot the array to receive the computed derivatives
	 * @throws MaxCountExceededException if the Commons Math integrator signals an
	 *         evaluation limit problem
	 */
	@Override
	public void computeDerivatives(double t, double[] y, double[] yDot) throws MaxCountExceededException {
		final double x = y[0];
		final double yy = y[1];
		final double z = y[2];

		final double zMinusB = z - b;
		final double rSquared = x * x + yy * yy;
		final double zCubeOver3 = (z * z * z) / 3.0;

		yDot[0] = zMinusB * x - d * yy;
		yDot[1] = d * x + zMinusB * yy;
		yDot[2] = c + a * z - zCubeOver3 - rSquared * (1.0 + e * z) + f * z * x * x * x;
	}

	/**
	 * Gets parameter a.
	 *
	 * @return parameter a
	 */
	public double getA() {
		return a;
	}

	/**
	 * Gets parameter b.
	 *
	 * @return parameter b
	 */
	public double getB() {
		return b;
	}

	/**
	 * Gets parameter c.
	 *
	 * @return parameter c
	 */
	public double getC() {
		return c;
	}

	/**
	 * Gets parameter d.
	 *
	 * @return parameter d
	 */
	public double getD() {
		return d;
	}

	/**
	 * Gets parameter e.
	 *
	 * @return parameter e
	 */
	public double getE() {
		return e;
	}

	/**
	 * Gets parameter f.
	 *
	 * @return parameter f
	 */
	public double getF() {
		return f;
	}
}