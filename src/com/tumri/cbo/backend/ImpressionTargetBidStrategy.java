package com.tumri.cbo.backend;

import java.util.Date;

import com.tumri.af.exceptions.DataAccessException;

public class ImpressionTargetBidStrategy extends AbstractBidStrategy
       implements BidStrategy {

    public static final String IMPRESSION_TARGET = "Impression target";

    public static final ImpressionTargetBidStrategy STRATEGY =
            new ImpressionTargetBidStrategy(IMPRESSION_TARGET);

    private ImpressionTargetBidStrategy(String name) {
        super(name);
    }
    
    /** Gets the only instance of this class.
     * @return The only instance of this class.
     */
    public static ImpressionTargetBidStrategy getInstance() {
    	return STRATEGY;
    }
    
    /** Suggests the bid and or changes to the daily impression limit.
     * @param camp The campaign (assumed not null).
     * @param hist The historical data.
     * @return A new bid suggestion, which may be null if no changes are needed.
     * @throws DataAccessException If error accessing historical data.
     */
    public BidSuggestion suggestBid(CampaignInfo camp, HistoricalData hist) throws DataAccessException {
    	return suggestBid(camp);
    }
    
    // --------------------- Package private methods -----------------
    
    /** Suggests the bid and or changes to the daily impression limit.
     * @param camp The campaign (assumed not null).
     * @return A new bid suggestion, which may be null if no changes are needed.
     */
    BidSuggestion suggestBid(CampaignInfo camp) {
    	
    	BidSuggestion result = new BidSuggestion(null, BidReason.INSUFFICIENT_DATA);
    	
    	if(camp != null) {
    		Date start = camp.getStartDate();
    		Date end = camp.getEndDate();
    		Double cb = camp.getCurrentBid();
    		Long lifetimeImpressionsTarget = camp.getLifetimeImpressionTarget();
    		long lifetimeImpressionsServed = camp.getLifetimeImpressionsServed();
    		
    		if((start != null) && (end != null) && (lifetimeImpressionsTarget != null) &&
    				(lifetimeImpressionsTarget > 0L) && (cb !=  null)) {
    			
    			Date now = new Date();
    			double campaignDuration = (double)(end.getTime() - start.getTime());
        		double durationSoFar = (double)(now.getTime() - start.getTime());
        		double timeFraction = durationSoFar/campaignDuration;
        		double impressionsFraction = ((double)lifetimeImpressionsServed) / ((double)lifetimeImpressionsTarget);
        		double spendRatio = impressionsFraction / timeFraction;
        		
        		Double suggested = cb / Math.min(1.3, Math.max(0.7,spendRatio));
    			BidReason reason = (spendRatio > 1.0) ? BidReason.OVERPACING : BidReason.UNDERPACING;
    			result = new BidSuggestion(suggested, reason);
    		}
    	}
    	return result;
    }
}

