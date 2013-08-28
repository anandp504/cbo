package com.tumri.af.utils;

/** An exception that indicates that a required property in a 
 * properties file has been set to an invalid value.
 */
public class InvalidPropertyException extends PropertyException {
	
	private static final long serialVersionUID = -1144171626570451190L;
	
	private final static String DEFAULT_MESSAGE = "Invalid property value: ";
	
	// ------------------------ Constructors ------------------

	/** Default constructor.
	 */
	public InvalidPropertyException() {
		this(null, null);
	}
	
	/** Constructor that takes a key.
	 * The message is defaulted to "Invalid property value: <key>"
	 * @param key The key.
	 */
	public InvalidPropertyException(String key) {
		this(DEFAULT_MESSAGE + key, key);
	}
	
	/** Constructor that takes a message and a key.
	 * @param msg The message.
	 * @param key The key.
	 */
	public InvalidPropertyException(String msg, String key) {
		super(msg, key);
	}
}
