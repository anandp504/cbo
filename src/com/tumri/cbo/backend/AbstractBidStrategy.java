package com.tumri.cbo.backend;

public abstract class AbstractBidStrategy implements BidStrategy {

    private String[] names;

    public AbstractBidStrategy(String... names) {
    	this.names = names;
    }

    public String getPrimaryName() {
    	return names[0];
    }

    public String[] getNames() {
    	return names;
    }

    /** Determines if this bid strategy uses a fixed bid price.
     * By default all but the NoOp bid strategies are fixed price at this point.
     * @return True.  Subclasses that are not fixed bid price should override this.
     */
    public boolean isFixedPriceStrategy() {
    	return true;
    }

    /** Determines if this bid strategy uses a variable daily budget.
     * By default, all strategies have a fixed daily budget.
     * @return False.  Subclasses that adjust the daily impression budget should override this.
     */
    public boolean isVariableDailyImpressionBudgetStrategy() {
    	return false;
    }
}

