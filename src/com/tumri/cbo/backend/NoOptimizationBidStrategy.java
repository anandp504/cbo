package com.tumri.cbo.backend;

import com.tumri.af.exceptions.DataAccessException;

public class NoOptimizationBidStrategy extends NoOpBidStrategy
       implements BidStrategy {

    public static final String NO_OPTIMIZATION   = "ECP";
    public static final String NO_OPTIMIZATION_SECONDARY = "Do not optimize";

    private NoOptimizationBidStrategy(String... names)
    {
        super(names);
    }

    public static final NoOptimizationBidStrategy STRATEGY =
            new NoOptimizationBidStrategy
                    (NO_OPTIMIZATION, NO_OPTIMIZATION_SECONDARY);

    /** Suggests a change to the current bid price and/or to the daily impressions limit
     * for the campaign.
     * @param camp The campaign.
     * @param histData The historical data for the specified campaign.
     * @return A bid suggestion or null if the current bid and daily impression limit should remain the same.
     * @exception DataAccessException If error accessing historical data.
     */
    public BidSuggestion suggestBid(CampaignInfo camp, HistoricalData histData) throws DataAccessException {
    	return new BidSuggestion(0.0, BidReason.OPTIMIZATION_DISABLED);
    }
}

