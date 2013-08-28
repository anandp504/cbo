package com.tumri.cbo.backend;

public abstract class NoOpBidStrategy extends AbstractBidStrategy
       implements BidStrategy {

    public NoOpBidStrategy(String... names) {
        super(names);
    }
    
    /** Bid strategies that do not optimize do not use fixed bids.
     * @return False since this does not use a fixed bid price.
     */
    public boolean isFixedPriceStrategy() {
    	return false;
    }
}

