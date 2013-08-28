package com.tumri.cbo.backend;

import com.tumri.af.context.TimeScale;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.ExcelColSchema;
import com.tumri.mediabuying.zini.ExcelDumpable;
import com.tumri.mediabuying.zini.Utils;

import java.util.*;

/** Represents a row of the performance history of the campaign.
 */
public class PerformanceHistoryRow implements ExcelDumpable {
	private BidParameters m_params;
	private BidResponse m_response;
	private Collection<BidderEvent> m_events;
    private String m_timeString;
    private Date m_time;


    private static String[] excelSchemaDef =
        {
           // "Name=Time, getMethod=getTime, styleName=dateAndTime",
           "Name=Time, getMethod=getTimeString",
           "Name=Fixed bid price?, getMethod=isFixedBidPrice",
           "Name=Min bid, getMethod=getMinBidPrice, styleName=dollarCurrency",
           "Name=Max bid, getMethod=getMaxBidPrice, styleName=dollarCurrency",
           "Name=Bid price, getMethod=getBidPrice, styleName=dollarCurrency",
           "Name=Impressions served, getMethod=getImpressionsServed, styleName=numberWithCommas",
           "Name=Average CPM, getMethod=getAverageCPMPaid, styleName=dollarCurrency",
           "Name=Total cost, getMethod=getTotalCost, styleName=dollarCurrency",
           "Name=Entropy, getMethod=getEntropy",
           "Name=Events, getMethod=getEventsAsString",
           "Name=Changes, getMethod=getChangesSummary"
        };

    private static List<ExcelColSchema> excelSchema = null;

    public static List<ExcelColSchema> getExcelSchema()
    {
        if(excelSchema == null)
            excelSchema = ExcelColSchema.getSchema
                    (PerformanceHistoryRow.class, excelSchemaDef);
        return excelSchema;
    }

    public List<ExcelColSchema> getSchema()
    {
        return getExcelSchema();
    }

    public Object getValue(ExcelColSchema colSchema)
    {
        return colSchema.getValue(this);
    }

	// ---------------------- Constructors ---------------

	/** Constructor that takes a reference to a bid parameters and bid response object.
	 * @param params The bid parameters.
	 * @param resp The bid response.
	 * @param events The bidder events.
     * @param timeString The time of this point in the curve as a String.
     * @param time The time of this point in the curve.
	 */
	public PerformanceHistoryRow(BidParameters params, BidResponse resp,
                                 Collection<BidderEvent> events,
                                 String timeString, Date time) {
		m_params = params;
		m_response = resp;
		m_events = Collections.unmodifiableCollection(events);
        m_timeString = timeString;
        m_time = time;
	}
	
	/** Constructor that takes a reference to an existing row (or null)
     * to initialise from.
	 * @param prototype An instance to copy from.
     * @param params Incoming BidderParameters, if known, otherwise use the prototype.
     * @param timeString The time of this point in the curve as a String.
	 * @param time The time of this point in the curve.
	 */
	public PerformanceHistoryRow(PerformanceHistoryRow prototype,
                                 BidParameters params,
                                 String timeString, Date time){
		m_params = (params == null
                    ? (prototype == null ||
                       prototype.getBidParameters() == null
                        ? new BidParameters()
                        : new BidParameters(prototype.getBidParameters()))
                    : params);
		if(prototype == null || prototype.getBidResponse() == null)
            m_response = new TrivialBidResponse(0l, 0d, null, null, null);
        else
        {
            BidResponse r = prototype.getBidResponse();
            m_response = r.cloneSelf();
        }
		m_events = Collections.unmodifiableCollection(new Vector<BidderEvent>());
        m_timeString = timeString;
        m_time = time;
	}

	// ------------------------ Public methods ---------------------------

    public Date getTime() {
        return m_time;
    }

    public String getTimeString() {
        return m_timeString;
    }

	public boolean isFixedBidPrice() {
		boolean fixed = false;
		BidParameters bp = getBidParameters();
		if(bp != null) {
			fixed = bp.isFixedBid();
		}
		return fixed;
	}
	
    @SuppressWarnings("unused")
	public Double getNormalizedBidPrice(TimeScale timeScale) {
        return getBidPrice();
	}
	
	public Double getBidPrice() {
		Double p = null;
		BidParameters bp = getBidParameters();
		if(bp != null) {
			p = bp.getBid();
		}
		return p;
	}

    @SuppressWarnings("unused")
	public Double getMinBidPrice() {
		Double p = null;
		BidParameters bp = getBidParameters();
		if(bp != null) {
			p = bp.getMinimumBid();
		}
		return (p == null ? -1.0d : p);
	}
	
    @SuppressWarnings("unused")
	public Double getMaxBidPrice() {
		Double p = null;
		BidParameters bp = getBidParameters();
		if(bp != null) {
			p = bp.getMaximumBid();
		}
		return (p == null ? -1.0d : p);
	}
	
	/** Gets the timeScale-normalized impression target.
     * @param timeScale The timeScale against which to normalise.
	 * @return The impression target.
	 */
    @SuppressWarnings("unused")
	public long getNormalizedImpressionTarget(TimeScale timeScale) {
        Long n = 0L;
        BidResponse br = getBidResponse();
        if(br != null) {
            n = br.getImpressionTarget();
            if(n == null)
                return 0L;
            else
            {
                // Impression target has daily granularity.
                // We impute the daily value to hours.
                // All coarser-grained values have been summed up, so we have
                // to divide up the granularity.
                switch(timeScale)
                {
                    case HOURLY: n = n / 24; break;
                    case DAILY: break;
                    case WEEKLY: n = n * 7; break;
                    case MONTHLY: n = n * 30; break;
                    case QUARTERLY: n = n * 91; break;
                    case YEARLY: n = n * 365; break;
                    default: throw Utils.barf
                            ("Unhandled timeScale", this, timeScale);
                }
            }
        }
        return n;
	}
	
	/** Gets the timeScale-normalized impression budget.
     * @param timeScale The timeScale against which to normalise.
	 * @return The impression target.
	 */
    @SuppressWarnings("unused")
	public long getNormalizedImpressionBudget(TimeScale timeScale) {
		long n = 0L;
        BidParameters bp = getBidParameters();
	    if(bp != null) {
		    n = bp.getDailyImpressionBudget();
            // Daily values have to be normalised by TimeScale.
            switch(timeScale)
            {
                case HOURLY: n = n / 24; break;
                case DAILY: break;
                case WEEKLY: n = n * 7; break;
                case MONTHLY: n = n * 30; break;
                case QUARTERLY: n = n * 91; break;
                case YEARLY: n = n * 365; break;
                default: throw Utils.barf
                        ("Unhandled timeScale", this, timeScale);
            }
		}
		return n;
	}

	/** Gets the timeScale-normalized site distribution entropy.
	 * @return The entropy.
	 */
    @SuppressWarnings("unused")
	public double getEntropy() {
		Double e = 0d;
        BidResponse br = getBidResponse();
	    if(br != null) {
		    e = br.getEntropy();
            if(e == null)
                return 0d;
            else return e;
		}
		return e;
	}

	/** Gets the timeScale-normalized site distribution entropy.
     * @param timeScale The timeScale against which to normalise.
	 * @return The entropy.
	 */
    @SuppressWarnings("unused")
	public double getNormalizedEntropy(TimeScale timeScale) {
		Double e = 0d;
        BidResponse br = getBidResponse();
	    if(br != null) {
		    e = br.getEntropy();
            if(e == null)
                return 0d;
            else
            {
                // Entropy has daily granularity.
                // We impute the daily value to hours.
                // All coarser-grained values have been summed up, so we have
                // to divide up the granularity.
                switch(timeScale)
                {
                    case HOURLY: break;
                    case DAILY:  break;
                    case WEEKLY: e = e / 7; break;
                    case MONTHLY: e = e / 30; break;
                    case QUARTERLY: e = e / 91; break;
                    case YEARLY: e = e / 365; break;
                    default: throw Utils.barf
                            ("Unhandled timeScale", this, timeScale);
                }
            }
		}
		return e;
	}

	/** Gets the impression target.
	 * @return The impression target.
	 */
    @SuppressWarnings("unused")
	public long getDailyImpressionBudget() {
		long n = 0L;
        BidParameters bp = getBidParameters();
	    if(bp != null) {
		    n = bp.getDailyImpressionBudget();
		}
		return n;
	}

	/** Gets the timeScale-normalized number of impressions served.
     * @param timeScale The timeScale against which to normalise.
	 * @return The number of impressions served.
	 */
    @SuppressWarnings("unused")
	public long getNormalizedImpressionsServed(TimeScale timeScale) {
        return getImpressionsServed();
	}

	/** Gets the number of impressions served.
	 * @return The number of impressions served.
	 */
	public long getImpressionsServed() {
		long n = 0L;
		BidResponse br = getBidResponse();
		if(br != null) {
			n = br.getImpressionsServed();
		}
		return n;
	}

	/** Gets the total cost of the impressions served (in USD).
	 * @return The total cost of the impressions served (in USD).
	 */
	public double getTotalCost() {
		double cost = 0.0;
		BidResponse br = getBidResponse();
		if(br != null) {
			cost = br.getTotalCost();
		}
		return cost;
	}
	
	/** Gets the timeScale-normalized count of non-trivial campaign changes.
     * @param timeScale The timeScale against which to normalise.
	 * @return The count of non-trivial changes.
	 */
    @SuppressWarnings("unused")
	public long getNormalizedChangeCount(TimeScale timeScale) {
        return getChangeCount();
	}
	
	/** Gets the count of non-trivial campaign changes.
	 * @return The count of non-trivial changes.
	 */
	public long getChangeCount() {
        if(m_params == null) return 0L;
        else
        {
            CampaignChangeCount changes = m_params.getChanges();
            if(changes == null) return 0L;
            else return changes.getCountOfChanges();
        }
	}

	/** Gets the timeScale-normalized average CPM for impressions served (in USD).
     * @param timeScale The timeScale against which to normalise.
	 * @return The average CPM for impressions served (in USD).
	 */
    @SuppressWarnings("unused")
	public double getNormalizedAverageCPMPaid(TimeScale timeScale) {
        return getAverageCPMPaid();
	}

	/** Gets the average CPM for impressions served (in USD).
	 * @return The average CPM for impressions served (in USD).
	 */
	public double getAverageCPMPaid() {
		double aveCPM = 0.0;
		BidResponse br = getBidResponse();
		if(br != null) {
			aveCPM = br.getAverageCPM();
		}
		return aveCPM;
	}

	/** Gets the bidder events that happened within the time period represented by this response.
	 * @return The bidder events that happened within the time period represented by this response.
	 */
    @SuppressWarnings("unused")
	public Collection<BidderEvent> getBidderEvents() {
		return m_events;
	}

    @SuppressWarnings("unused")
    public String getEventsAsString()
    {
        if(m_events == null) return "";
        else
        {
            StringBuffer b = new StringBuffer();
            boolean first = true;
            for(BidderEvent e: m_events)
            {
                if(first) first = false;
                else b.append(", ");
                b.append(e.getBriefString());
            }
            return b.toString();
        }
    }
	
    public String getChangesSummary()
    {
        if(m_params == null) return "";
        else
        {
            CampaignChangeCount changes = m_params.getChanges();
            if(changes == null) return "";
            else return changes.summarise();
        }
    }

    public String toString()
    {
        return "#<" + AppNexusUtils.afterDot(getClass().getName()) + " " +
                m_time + ">";
    }

	// ---------------------- Package private -------------------
	
	/** Gets the bid parameters that were in effect during the time interval this row represents.
	 * @return The bid parameters.
	 */
	BidParameters getBidParameters() {
		return m_params;
	}
	
	void setBidParameters(BidParameters params) {
		m_params = params;
	}

	BidResponse getBidResponse() {
		return m_response;
	}

	void setBidResponse(BidResponse response) {
		m_response = response;
	}

}
