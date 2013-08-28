package com.tumri.cbo.backend;

import com.tumri.af.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/** Represents the set of parameters that are used to control bidding.
 */
public class BidParameters {

	private boolean m_fixedBid;
	private Map<Long, Long> m_dailyImpressionBudgets = new HashMap<Long, Long>();
	private Boolean m_evenPacing;
	private Map<Long, Double> m_bids = new HashMap<Long, Double>();
	private Map<Long, Double> m_minBids = new HashMap<Long, Double>();
	private Map<Long, Double> m_maxBids = new HashMap<Long, Double>();
    private CampaignChangeCount m_changes = null;
	
	// ------------------------- Constructors ------------------------------

    /** Default constructor that makes an empty instance for curve points that have no
     * basis on a previous point.
     * It sets the bid to a fixed bid with even pacing but with no bid price
     * or minimum or maximum bids and without any target or maximum daily impressions.
     */
    public BidParameters() {
    	this(true, true, null, null, null, null, null);
    }

    /** Constructor that makes a mostly empty instance for curve points.
     * Values get plugged in later.
     * @param evenPacing True to enable even pacing or false not to.  May be set to null if this is not specified.
     */
    public BidParameters(Boolean evenPacing) {
    	this(true, evenPacing, null, null, null, null, null);
    }

    /** Constructor that makes an instance based on a previous point
     * on the curve.
     * @param prototype The instance from the previous point in time.
     */
    public BidParameters(BidParameters prototype) {
    	this(prototype.isFixedBid(), 
    		 prototype.getEvenPacing(), 
    		 prototype.getBids(),
    		 prototype.getMinimumBids(),
    		 prototype.getMaximumBids(),
    		 prototype.getDailyImpressionBudgets(),
             prototype.getChanges());
    }
    
    /** Constructor that constructs a fixed bid with an even pacing flag and an optional daily impression limit.
     * @param bid The bid price CPM or null if none specified.
     * @param evenPacing True to enable even pacing or false not to.  May be set to null if this is not specified.
     * @param dailyImpressionBudget The daily impression limit or null if none.
     * @param changes The CampaignChangeCount instance.
     */
    public BidParameters(Double bid, Boolean evenPacing, Long dailyImpressionBudget, CampaignChangeCount changes) {
    	this(true, evenPacing, primeMap(bid), null, null, primeMap(dailyImpressionBudget), changes);
    }
    
    /** Constructor that constructs a variable bid with minimum and maximum bid prices,
     * an even pacing flag and an optional daily impression limit.
     * @param minBid The minimum bid CPM or null if no minimum.
     * @param maxBid The maximum bid CPM or null if no maximum.
     * @param evenPacing True to enable even pacing or false not to.  May be set to null if this is not specified.
     * @param dailyImpressionBudget The daily impression limit or null if none.
     * @param changes The CampaignChangeCount instance.
     */
    public BidParameters(Double minBid, Double maxBid, Boolean evenPacing, Long dailyImpressionBudget, CampaignChangeCount changes) {
    	this(false, evenPacing, null, primeMap(minBid), primeMap(maxBid), primeMap(dailyImpressionBudget), changes);
    }
	
    /** Constructor that takes all of the arguments.
     * @param fixedBid True to use a fixed bid or false to use a variable bid.
     * @param evenPacing Flag to indicate the campaign should be paced evenly.  May be null if not specified.
     * @param bids The bid price CPM or null if not using a fixed bid.
     * @param minBids The minimum bid CPM or null if none or if using a fixed bid.
     * @param maxBids The maximum bid CPM or null if none or if using a fixed bid.
     * @param dailyImpressionBudgets The maximum daily impressions or null if none.
     * @param changes The CampaignChangeCount instance.
     */
    private BidParameters(boolean fixedBid, Boolean evenPacing, Map<Long, Double> bids, Map<Long, Double> minBids, Map<Long, Double> maxBids, Map<Long, Long> dailyImpressionBudgets, CampaignChangeCount changes) {
    	setFixedBid(fixedBid);
    	setEvenPacing(evenPacing);
    	if(bids != null) setBids(bids);
    	if(minBids != null) setMinimumBids(minBids);
    	if(maxBids != null) setMaximumBids(maxBids);
    	if(dailyImpressionBudgets != null)
            setDailyImpressionBudgets(dailyImpressionBudgets);
        setChanges(changes);
    }
    
    // -------------------------- Public methods -------------------------
	
    /** Sets the campaign change count structure.
     * @return The CampaignChangeCount instance.
     */
    public CampaignChangeCount getChanges() {
        return m_changes;
    }

	/** Gets the maximum number of impressions to be served during a single day.
	 * The start of the day is defined by the time zone of the campaign that this is associated with.
	 * @return The maximum number of impressions to be served during a single day or null to indicate there is no maximum.
	 */
	public Long getDailyImpressionBudget() {
		return getAvg(m_dailyImpressionBudgets);
	}

	/** Determines if the ad network will try to serve impressions evenly over time.
	 * This may return null if the even pacing flag has not been specified.
	 * @return True if even pacing is enabled or false if not.
	 */
	public Boolean getEvenPacing() {
		return m_evenPacing;
	}
	
	/** Determines if we are always bidding the bid price or if we allow the bid price to be
	 * optimized for any reason by the ad network on which we are bidding.
	 * If this returns false, then the maximum and minimum bid prices will be used
	 * to enforce limits on the optimization algorithm of the ad network.
	 * @return True if we are setting a fixed bid price or false if not.
	 */
	public boolean isFixedBid() {
		return m_fixedBid;
	}
	
	/** Gets the bid price to be paid (in US dollars) for impressions on a CPM basis.
	 * This price will be the only price paid if the bidding is on a fixed basis.
	 * @return The bid price as a price per thousand impressions or null to indicate there is no fixed bid price.
	 */
	public Double getBid() {
		return getAvg(m_bids);
	}
	
    /** Adds a bid price to be paid (in US dollars) for impressions on a CPM basis.
     * This price will be the only price paid if the bidding is on a fixed basis.
     * @param time The time at which the bid was observed.
     * @param bid The bid price as a price per thousand impressions or null to indicate there is no fixed bid price, by time.
     */
    public void addBid(Long time, Double bid) {
        m_bids.put(time, bid);
    }

	/** Gets the minimum bid price (in US dollars) for impressions on a CPM basis.
	 * @return The minimum bid price in USD for 1000 impressions or null if there is none.
	 */
	public Double getMinimumBid() {
		return getAvg(m_minBids);
	}


    /** Adds a minimum bid price to be paid (in US dollars) for impressions on a CPM basis.
     * This price will be the only price paid if the bidding is on a fixed basis.
     * @param time The time at which the bid was observed.
     * @param bid The bid price as a price per thousand impressions or null to indicate there is no fixed bid price, by time.
     */
    public void addMinBid(Long time, Double bid) {
        m_minBids.put(time, bid);
    }

    /** Adds a maximum bid price to be paid (in US dollars) for impressions on a CPM basis.
     * This price will be the only price paid if the bidding is on a fixed basis.
     * @param time The time at which the bid was observed.
     * @param bid The bid price as a price per thousand impressions or null to indicate there is no fixed bid price, by time.
     */
    public void addMaxBid(Long time, Double bid) {
        m_maxBids.put(time, bid);
    }

    /** Adds a daily impression limit value.
     * @param time The time at which the bid was observed.
     * @param limit the impression limit at the specified time.
     */
    public void addDailyImpressionBudget(Long time, Long limit) {
        m_dailyImpressionBudgets.put(time, limit);
    }

    /** Adds a set of campaign change counts to the current counts.
     * @param changes The changes to add.
     */
    public void accumulateChanges(CampaignChangeCount changes) {
        m_changes = CampaignChangeCount.accumulate(m_changes, changes);
    }

	/** Gets the maximum amount that can be paid (in US dollars) for impressions on a CPM basis.
	 * @return The maximum bid price for 1000 impressions or null if there is none.
	 */
	public Double getMaximumBid() {
		return getAvg(m_maxBids);
	}
	
	/** Determines if this is equal to another object.
	 * @param obj The other object to compare this to.
	 * @return True if the other object is a BidParameters with the same values.
	 */
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if((obj != null) && (obj.getClass() == getClass())) {
			BidParameters other = (BidParameters)obj;
			return(Utils.equals(getBid(), other.getBid()) &&
				   (isFixedBid() == other.isFixedBid()) &&
				   Utils.equals(getDailyImpressionBudget(), other.getDailyImpressionBudget()) &&
				   Utils.equals(getEvenPacing(), other.getEvenPacing()) && 
				   Utils.equals(getMaximumBid(), other.getMaximumBid()) &&
				   Utils.equals(getMinimumBid(), other.getMinimumBid()));			
		}
		return false;
	}
	
	/** Gets a hash code consistent with equals.
	 * @return A hash code consistent with equals.
	 */
	public int hashCode() {		
		int hash = getClass().hashCode();
		hash = Utils.addToHash(hash, getBid());
		hash = Utils.addToHash(hash, getMinimumBid());
		hash = Utils.addToHash(hash, getMaximumBid());
		hash = Utils.addToHash(hash, getDailyImpressionBudget());
		hash = Utils.addToHash(hash, getEvenPacing());
		hash = Utils.addToHash(hash, isFixedBid());
		return hash;
	}

	/** Gets a string representation of this for debugging.
	 * @return A string representation of this for debugging.
	 */
	public String toString() {
		return "BidParameters[fixed=" + isFixedBid() + ", bid=" + getBid() +
			   ", minBid=" + getMinimumBid() + ", maxBid=" + getMaximumBid() +
			   ", dailyImpLimit=" + getDailyImpressionBudget() +
			   ", evenPacing=" + getEvenPacing() +
			   (getChanges() != null && getChanges().isNonTrivial()
                  ? ", nonTrivialChanges!" : "") + 
               "]";
	}

	// ---------------------------- Private methods ---------------------------

    static Map<Long, Long> primeMap(Long l)
    {
        Map<Long, Long> res = new HashMap<Long, Long>();
        res.put(0l, l);
        return res;
    }

    static Map<Long, Double> primeMap(Double d)
    {
        Map<Long, Double> res = new HashMap<Long, Double>();
        res.put(0l, d);
        return res;
    }

    static Long getAvg(Map<Long, Long> table)
    {
        Long sum = 0l;
        Long count = 0l;
        for(Long v: table.values())
        {
            if(v != null)
            {
                count = count + 1;
                sum = sum + v;
            }
        }
        if(count == 0l) return 0l;
        else return sum / count;
    }

    static Double getAvg(Map<Long, Double> table)
    {
        Double sum = 0d;
        Long count = 0l;
        for(Double v: table.values())
        {
            if(v != null)
            {
                count = count + 1;
                sum = sum + v;
            }
        }
        if(count == 0l) return 0d;
        else return sum / count;
    }

	/** Sets a flag to indicate that the bid price should always be fixed.
	 * @param fixedBid True if for a fixed bid pricing or false for variable bid pricing.
	 */
	private void setFixedBid(boolean fixedBid) {
		m_fixedBid = fixedBid;
	}
	
	/** Sets a flag that indicates that impressions should be shown at an even pace.
	 * This may be set to null if the even pacing flag is not specified.
	 * @param evenPacing True if even pacing should be applied or false if not.
	 */
	private void setEvenPacing(Boolean evenPacing) {
		m_evenPacing = evenPacing;
	}
	
	/** Gets the bid price to be paid (in US dollars) for impressions on a CPM basis.
	 * This price will be the only price paid if the bidding is on a fixed basis.
	 * @param bids The bid price as a price per thousand impressions or null to indicate there is no fixed bid price, by time.
	 */
	private void setBids(Map<Long, Double> bids) {
		m_bids = bids;
	}

	/** Gets the bid price to be paid (in US dollars) for impressions on a CPM basis.
	 * This price will be the only price paid if the bidding is on a fixed basis.
	 * @return The bid price as a price per thousand impressions or null to indicate there is no fixed bid price, by time.
	 */
	private Map<Long, Double> getBids() {
		return m_bids;
	}

	/** Sets the minimum bid price (in US dollars) for impressions on a CPM basis.
	 * @return The minimum bid price for 1000 impressions or null if there is no minimum, by time, by time.
	 */
	private Map<Long, Double> getMinimumBids() {
		return m_minBids;
	}
	
	/** Sets the minimum bid price (in US dollars) for impressions on a CPM basis.
	 * @param minBids The minimum bid price for 1000 impressions or null if there is no minimum, by time.
	 */
	private void setMinimumBids(Map<Long, Double> minBids) {
		m_minBids = minBids;
	}

	/** Sets the maximum bid price (in US dollars) for impressions on a CPM basis.
	 * @return The maximum bid price for 1000 impressions or null if there is no maximum, by time.
	 */
	private Map<Long, Double> getMaximumBids() {
		return m_maxBids;
	}
	
	/** Sets the maximum bid price (in US dollars) for impressions on a CPM basis.
	 * @param maxBids The maximum bid price for 1000 impressions or null if there is no maximum, by time.
	 */
	private void setMaximumBids(Map<Long, Double> maxBids) {
		m_maxBids = maxBids;
	}

	/** Sets the maximum number of impressions to be served during a single day.
	 * The start of the day is defined by the time zone of the campaign that this is associated with.
	 * @param dailyImpressionBudgets The maximum number of impressions to be served during a single day or null if no maximum, by time.
	 */
	private void setDailyImpressionBudgets(Map<Long, Long> dailyImpressionBudgets) {
		m_dailyImpressionBudgets = dailyImpressionBudgets;
	}
	
	/** Gets the maximum number of impressions to be served during a single day.
	 * The start of the day is defined by the time zone of the campaign that this is associated with.
	 * @return The maximum number of impressions to be served during a single day or null if no maximum, by time.
	 */
	private Map<Long, Long> getDailyImpressionBudgets() {
		return m_dailyImpressionBudgets;
	}

	/** Sets the campaign change count structure.
	 * @param changes The CampaignChangeCount instance.
	 */
	private void setChanges(CampaignChangeCount changes) {
		m_changes = changes;
	}
}
