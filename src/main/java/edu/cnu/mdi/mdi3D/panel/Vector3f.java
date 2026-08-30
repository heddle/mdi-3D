package edu.cnu.mdi.mdi3D.panel;

/**
 * A minimal, mutable single-precision 3D vector with the handful of
 * operations (length, dot, cross, normalize, midpoint) that the {@code mdi3D}
 * geometry helpers need. Not related to any JOGL/JOML vector type.
 */
public class Vector3f {

	/**
	 * The coordinates
	 */
	public float x, y, z;

	/**
	 * null constructor; all coordinates are 0
	 */
	public Vector3f() {
		this(0, 0, 0);
	}

	/**
	 * Constructor
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 */
	public Vector3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Constructor using a set of coordinates
	 *
	 * @param coords [x, y, z, x, y, z, etc]
	 * @param index  start at 3*index for the x coordinate
	 */
	public Vector3f(float coords[], int index) {
		int j = 3 * index;
		x = coords[j];
		y = coords[j + 1];
		z = coords[j + 2];
	}

	/**
	 * Get the length of the vector
	 *
	 * @return the length
	 */
	public float length() {
		return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
	}

	/**
	 * Compute the dot (scalar) product with another vector.
	 *
	 * @param other the other vector
	 * @return the dot product
	 */
	public float dot(Vector3f other) {
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}

	/**
	 * Compute the cross product with another vector.
	 *
	 * @param other the other vector
	 * @return a new vector perpendicular to both this vector and {@code other}
	 */
	public Vector3f cross(Vector3f other) {
		return new Vector3f(this.y * other.z - this.z * other.y, this.z * other.x - this.x * other.z,
				this.x * other.y - this.y * other.x);
	}

	/**
	 * Scale this vector in place to unit length. A zero-length vector is left
	 * unchanged rather than producing {@code NaN} components.
	 */
	public void normalize() {
		float length = length();
		if (length != 0.0f) {
			this.x /= length;
			this.y /= length;
			this.z /= length;
		}
	}

	/**
	 * Obtain a vector that is the midpoint of two other vectors
	 *
	 * @param v1 one vector
	 * @param v2 the other vector
	 * @return the midpoint
	 */
	public static Vector3f midpoint(Vector3f v1, Vector3f v2) {
		float x = 0.5f * (v1.x + v2.x);
		float y = 0.5f * (v1.y + v2.y);
		float z = 0.5f * (v1.z + v2.z);
		return new Vector3f(x, y, z);
	}

}
