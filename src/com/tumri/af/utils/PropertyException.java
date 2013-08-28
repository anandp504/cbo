package com.tumri.af.utils;

/** An exception that indicates that a required property in a 
 * properties file has not been set correctly.
 */
public class PropertyException extends Exception {
	
	private static final long serialVersionUID = 8158060122562534340L;
	
	private String key;
	
	// ------------------------ Constructors ------------------

	/** Default constructor.
	 */
	public PropertyException() {
		this(null, null);
	}
	
	/** Constructor that takes a message and a property key.
	 * @param msg The message.
	 * @param key The key.
	 */
	public PropertyException(String msg, String key) {
		super(msg);
		setKey(key);
	}
	
	// ------------------------ Public methods ------------------
	
	public final String getKey() {
		return key;
	}

	// ----------------------- Private methods ------------------
	
	private void setKey(String key) {
		this.key = key;
	}
}
