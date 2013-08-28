package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.CellStyleName;

import java.text.NumberFormat;

public class BidSuggestion {
    private Double m_newBid;
    private BidReason m_bidReason;
    private Long m_newImpressionLimit;
	private String reason;
    private CellStyleName styleName = null;

    // ---------------------- Constructors -----------------------
    
    /** Default constructor that constructs a "null" suggestion
     * with a null bid, bid reason, impression limit and impression limit reason.
     * This indicates that nothing should be changed.
     */
    public BidSuggestion() {
    	this(null, null, null);
    }
    
    /** Bid suggestion that specifies a new bid and a reason for the new bid,
     * and a null new daily impression limit and a null reason for changing
     * the new daily impression limit.
     * If the new bid is null that indicates that the bid should not be changed.
     * The reason can be set to a non-null value even if the bid is set to null
     * to indicate why a new bid should not be placed.
     * @param bid The new bid.
     * @param reason The reason for the bid.
     */
    public BidSuggestion(Double bid, BidReason reason) {
    	this(bid, reason, null);
    }
    
    /** Bid suggestion that specifies a new bid, a reason for the new bid,
     * a new daily impression limit, and the reason for changing the impression limit.
     * If the new bid is null that indicates that the bid should not be changed.
     * The bid reason can be set to a non-null value even if the bid is set to null
     * to indicate why a new bid should not be placed.
     * Similarly, if the new daily impression limit is set to null, that means that
     * the current limit should not be changed, not that the limit should be removed.
     * The impression limit reason can be set even if the impression limit is not changed.
     * @param bid The new bid.
     * @param reason The reason for the bid.
     * @param limit The new daily impression limit or null to not change anything.
     */
    public BidSuggestion(Double bid, BidReason reason, Long limit) {
    	setSuggestedBid(bid);
    	setBidReason(reason);
    	setNewDailyImpressionLimit(limit);
    }
    
    // ---------------- Public methods -------------------------
    
    /** Sets the suggested bid.
     * This may be set to null to indicate the bid should not be changed.
     * Null indicates that the suggestion is to not change the bid whatever it may be.
     * @param bid The suggested bid or null if none.
     */
	public void setSuggestedBid(Double bid) {
		this.m_newBid = bid;
	}

	/** Gets the suggested bid.
	 * @return The suggested bid or null if none.
	 */
    public Double getSuggestedBid() {
		return m_newBid;
	}

    /** Sets the reason for suggesting the bid.
     * This may be non-null even if the suggested bid is null.
     * @param br The reason for suggesting the bid.
     */
	public void setBidReason(BidReason br) {
    	m_bidReason = br;
    	if(br != null) {
    		reason = br.getShortName();
    		styleName = br.getStyleName();
    	} else {
    		reason = "";
    		styleName = null;
    	}
	}
	
    /** Gets the reason for suggesting the bid.
     * This may be non-null even if the suggested bid is null.
     * @return The reason for suggesting the bid or null if none.
     */
	public BidReason getBidReason() {
		return m_bidReason;
	}

    /** Sets the suggested new daily impression limit.
     * If this is set to null it means there are no changes needed,
     * not that the suggestion is to change the daily limit to null.
     * @param limit The suggested new daily impression limit.
     */
    public void setNewDailyImpressionLimit(Long limit) {
    	m_newImpressionLimit = limit;
    }
    
    /** Gets the suggested new daily impression limit.
     * If this is null it means there are no changes needed,
     * not that the suggestion is to change the daily limit to null.
     * @return The suggested new daily impression limit.
     */
    public Long getNewDailyImpressionLimit() {
    	return m_newImpressionLimit;
    }
    
	public String toString() {
		return "BidSuggestion[bid=" + getBidAsString(4) +
		       ",bidReason=" + getBidReasonString() +
               ",styleName=" + getStyleName() + 
               ",newDailyImpLimit=" + getNewDailyImpressionLimit() +
               "]";
	}
	
	// --------------------- Package private methods ---------------
	
	String getBidReasonString() {
		return reason;
	}
	
	CellStyleName getStyleName() {
		return styleName;
	}

	// ---------------------- Private methods ------------------
	
	/** Gets the bid as a string for debugging purposes.
	 * @param decimals The number of digits after the decimal point.
	 * @return the bid as a string for debugging purposes.
	 */
	private String getBidAsString(int decimals) {
		String result = "No Bid";
        Double bid = getSuggestedBid();
        if(bid != null) {
    		NumberFormat nf = NumberFormat.getCurrencyInstance();
    		nf.setMinimumFractionDigits(decimals);
    		nf.setMaximumFractionDigits(decimals);
    		result = nf.format(bid);
        }
        return result;
	}
}

