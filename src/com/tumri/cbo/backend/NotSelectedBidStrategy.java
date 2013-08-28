package com.tumri.cbo.backend;

import com.tumri.af.exceptions.DataAccessException;

public class NotSelectedBidStrategy extends NoOpBidStrategy
        implements BidStrategy {

    public static final String NOT_SELECTED = "Not selected yet";

    private NotSelectedBidStrategy(String name) {
        super(name);
    }

    public static final NotSelectedBidStrategy STRATEGY =
            new NotSelectedBidStrategy(NOT_SELECTED);

    /** Suggests a change to the current bid price and/or to the daily impressions limit
     * for the campaign.
     * @param camp The campaign.
     * @param histData The historical data for the specified campaign.
     * @return A bid suggestion or null if the current bid and daily impression limit should remain the same.
     * @exception DataAccessException If error accessing historical data.
     */
    public BidSuggestion suggestBid(CampaignInfo camp, HistoricalData histData) throws DataAccessException {
        return new BidSuggestion(0.0, BidReason.NO_POLICY);
    }
}

