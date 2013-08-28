package com.tumri.af.exceptions;

public class BusyException extends Exception {
	
	private static final long serialVersionUID = -7631809862074287792L;

	public BusyException() {
		super();
	}
	
	public BusyException(String msg) {
		super(msg);
	}
	
    public String toString()
    {
        return "BusyException[msg=" + getMessage() + "]";
    }
}
