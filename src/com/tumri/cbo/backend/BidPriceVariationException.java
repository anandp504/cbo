package com.tumri.cbo.backend;

public class BidPriceVariationException extends Exception {

	private static final long serialVersionUID = -1906303440074797067L;

	public BidPriceVariationException() {
		this(null);
	}
	
	public BidPriceVariationException(String msg) {
		super(msg);
	}
}
