package com.tumri.cbo.backend;

import java.text.DecimalFormat;
import java.text.NumberFormat;


/** This class represents a measurement that has a standard
 * deviation of the expected value represented as the error.
 */
public class Measurement {

	private double value;
    private float error;
    
	private final static NumberFormat DEFAULT_VALUE_FORMAT = getScientificNumberFormat(6);
	private final static NumberFormat DEFAULT_ERROR_FORMAT = getScientificNumberFormat(1);

	
	/** Default constructor that sets the value to 0 and the error to 0.
	 */
	public Measurement() {
		this(0.0, 0.0);
	}
	
	/** Constructor that sets the value and the error.
	 * @param value The value.
	 * @param error The error.
	 */
	public Measurement(double value, double error) {
		setValue(value);
		setError(error);
	}
	
    /** Sets the value.
     * @param value The value.
     */
	public void setValue(double value) {
		this.value = value;
	}

    /** Gets the value.
     * @return The value.
     */
	public double getValue() {
		return value;
	}

    /** Sets the error.
     * This is the standard deviation of the Gaussian distribution assumed to be around the value.
     * The error must be positive or zero.
     * @param error The error.
     */
	public void setError(double error) {
		this.error = (float)Math.abs(error);
	}

    /** Gets the error.
     * @return The error.
     */
	public double getError() {
		return error;
	}

	/** Gets a string representation of this for debugging.
	 * @return A string representation of this for debugging.
	 */
    public String toString() {
        return DEFAULT_VALUE_FORMAT.format(getValue()) + " +/- " + DEFAULT_ERROR_FORMAT.format(getError());
    }
    
    // ----------- Private methods -----------
    
    /** Gets a scientific number format with the specified precision.
     * @param prec The precision.
     */
    private static NumberFormat getScientificNumberFormat(int prec) {
    	NumberFormat nf = NumberFormat.getInstance();
    	if(nf instanceof DecimalFormat) {
    		DecimalFormat df = (DecimalFormat)nf;
    		StringBuilder buf = new StringBuilder(prec + 5);
    		buf.append("0.");
    		for(int i = 0; i < prec; i++) {
    			buf.append('0');
    		}
    		buf.append("E0");
    		df.applyPattern(buf.toString());
    	}
    	return nf;
    }
}
