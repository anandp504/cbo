package com.tumri.af.utils;

/** An exception that indicates that a required property in a 
 * properties file has not been set.
 */
public class PropertyNotFoundException extends PropertyException {

	private static final long serialVersionUID = 2047590595985146362L;
	
	private final static String DEFAULT_MESSAGE = "Property not found: ";
	
	// ------------------------ Constructors ------------------

	/** Default constructor.
	 */
	public PropertyNotFoundException() {
		this(null, null);
	}
	
	/** Constructor that takes a key.
	 * The message is defaulted to "Property not found: <key>"
	 * @param key The key.
	 */
	public PropertyNotFoundException(String key) {
		this(DEFAULT_MESSAGE + key, key);
	}
	
	/** Constructor that takes a message and a key.
	 * @param msg The message.
	 * @param key The key.
	 */
	public PropertyNotFoundException(String msg, String key) {
		super(msg, key);
	}
}
