package com.tumri.cbo.backend;

import com.tumri.af.exceptions.DataAccessException;

public interface BidStrategy {

	/** Gets the name of this bid strategy.
	 * @return The primary name of this bid strategy.
	 */
    public String getPrimaryName();
    
	/** Gets the name of this bid strategy.
	 * @return The names of this bid strategy.
	 */
    public String[] getNames();

    /** Suggests a change to the current bid price and/or to the daily impressions limit
     * for the campaign.
     * @param camp The campaign.
     * @param histData The historical data for the specified campaign.
     * @return A bid suggestion or null if the current bid and daily impression limit should remain the same.
     * @exception DataAccessException If error accessing historical data.
     */
    public BidSuggestion suggestBid(CampaignInfo camp, HistoricalData histData) throws DataAccessException;

    /** Determines if this bid strategy uses a fixed bid price.
     * If so, the bid controller fixes the bid and the ad network will not change the bid.
     * @return True if this bid strategy uses a fixed bid price or false if not.
     */
    public boolean isFixedPriceStrategy();

    /** Determines if this bid strategy uses a variable daily budget.
     * By default, all strategies have a fixed daily budget.
     * @return True if the strategy adjusts the daily impression budget.
     */
    public boolean isVariableDailyImpressionBudgetStrategy();
}

