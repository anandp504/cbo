package com.tumri.cbo.backend;

import java.util.Currency;

/** Represents an amount of money with its currency.
 */
public class MonitaryValue {

	private double amount;
	private Currency currency;
	
	/** The currency code for US dollars. */
	public final static String CURRENCY_CODE_US_DOLLARS = "USD";
	
	/** The currency for US dollars. */
	public final static Currency USD = Currency.getInstance(CURRENCY_CODE_US_DOLLARS);
	
	/** The default currency. */
	public final static Currency DEFAULT_CURRENCY = USD;
	
	// ------------------------ Constructors ---------------
	
	/** Default constructor.
	 * Defaults to USD as the currency.
	 */
	public MonitaryValue() {
		this(0.0, DEFAULT_CURRENCY);
	}
	
	/** Constructor that specifies a value in US dollars.
	 * @param amt The number of dollars.
	 */
	public MonitaryValue(double amt) {
		this(amt, USD);
	}
	
	/** Constructor that specifies a value and a currency.
	 * @param amt The numeric value.
	 * @param c The currency.
	 */
	public MonitaryValue(double amt, Currency c) {
		setAmount(amt);
		setCurrency(c);
	}
	
	// ---------------------- Public methods ----------------
	
	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}
}
