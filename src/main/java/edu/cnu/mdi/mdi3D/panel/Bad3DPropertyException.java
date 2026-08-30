package edu.cnu.mdi.mdi3D.panel;

/**
 * Thrown by {@link edu.cnu.mdi.mdi3D.item3D.Item3D}'s typed property accessors ({@code get}, {@code
 * getString}, {@code getColor}, {@code getFont}, {@code getInt}, {@code
 * getFloat}) when a requested property is missing or is not of the requested
 * type.
 */
@SuppressWarnings("serial")
public class Bad3DPropertyException extends Exception {

	/**
	 * Creates the exception.
	 *
	 * @param message a message describing the missing or mistyped property
	 */
	public Bad3DPropertyException(String message) {
		super(message);
	}
}
