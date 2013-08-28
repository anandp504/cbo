package com.tumri.af.exceptions;

public class DataAccessException extends Exception {

	private static final long serialVersionUID = -8282589307272568728L;

	public DataAccessException() {
		super();
	}
	
	public DataAccessException(String msg) {
		super(msg);
	}
	
	public DataAccessException(Exception cause) {
		super(cause);
	}
	
    public String toString()
    {
        return "DataAccessException[msg=" + getMessage() + "]";
    }
}
