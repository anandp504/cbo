package com.tumri.cbo.backend;

import java.util.Calendar;
import java.util.Date;

import com.tumri.af.utils.DateUtils;

/** This class represents bid prices paid over the specified AppNexus day.
 */
public class DailyBidData extends DailyData {

	BidHistoryRow[] m_relevantBidHistory;
	
	// ----------------------------- Constructors -----------------------
	
	/** Constructor that specifies the day this is for and the bid history.
	 * The bid history rows must be ordered in increasing event time.
	 * @param day The day this is for.
	 * @param relevantBidHistory The bid history.
	 */
	DailyBidData(Calendar day, BidHistoryRow[] relevantBidHistory) {
		super(day);
		setRelevantBidHistory(relevantBidHistory);
	}
	
	// ------------------------------- Public methods ---------------------

	/** Determines there were any specific bid prices set during the day.
	 * @return True if there were any specific bid prices during the day or false if not.
	 */
	public boolean hasAnyBidPrices() {
		BidHistoryRow[] rows = getRelevantBidHistory();
		return (rows != null) && (rows.length > 0);
	}
	
	/** Gets the bid price paid for the specified hour of the day.
	 * Assume that a bid price applies at the start of the next hour.
	 * @param hr The zero-based hour of the day.
	 * @return The bid price CPM paid for the hour or null if none.
	 */
	public Double getBidPrice(int hr) {
		Double p = null;
		// This assumes the bid history rows are ordered by increasing event time.
		if((hr >= 0) && (hr < getNumberOfHoursInDay())) {
			Date d = new Date(this.getStartOfDay().getTimeInMillis() + hr*DateUtils.ONE_HOUR_MS);
			BidHistoryRow row = findLastRowBeforeOrAt(d);
			if(row != null) {
				p = row.getBid();
			}
		}
		return p;
	}
	
	/** Gets a string representation of this for debugging.
	 * @return A string representation of this for debugging.
	 */
    public String toString() {
    	StringBuilder buf = new StringBuilder("DailyBidData[day=");
    	buf.append(getStartOfDay());
    	buf.append(",bidHistoryRows=[");
    	BidHistoryRow[] rows = getRelevantBidHistory();
    	if(rows != null) {
    		for(int i = 0; i < rows.length; i++) {
    			if(i > 0) {
    				buf.append(", ");
    			}
    			buf.append(rows[i]);
    		}
    	}	
        return buf.toString();
    }
    
    // ---------------------- Private methods --------------

	/** Gets the last row in the bid history that is at or before the specified date.
	 * @param d The date-time (assumed not null).
	 * @return The last row whose event time is <= d.
	 */
	private BidHistoryRow findLastRowBeforeOrAt(Date d) {
		BidHistoryRow result = null;
		BidHistoryRow[] rows = getRelevantBidHistory();
		if(rows != null) {
			Date bidDate;
			BidHistoryRow row;
			int n = rows.length;
			while(--n >= 0) {
				row = rows[n];
				if(row != null) {
					bidDate = row.getEventTime();
					if((bidDate != null) && (!d.before(bidDate))) {
						result = row;
						break;
					}
				}
			}
		}
		return result;
	}
	
    /** Sets the relevant rows of the bid history.
     * @param relevantBidHistory The bid history.
     */
    private void setRelevantBidHistory(BidHistoryRow[] relevantBidHistory) {
    	m_relevantBidHistory = relevantBidHistory;
    }
    
    /** Gets the bid history rows that are relevant to the day this represents.
     * @return The relevant bid history rows or null if none.
     */
    private BidHistoryRow[] getRelevantBidHistory() {
    	return m_relevantBidHistory;
    }
}
