package com.tumri.cbo.backend;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.Sexpression;
import com.tumri.mediabuying.zini.Null;

import java.util.*;


public class BidHistoryRow implements Comparable<BidHistoryRow> {
    Date eventTime;
    Double bid;
    Long dailyImpressionBudget;
    Long dailyImpressionTarget;
    String bidStrategy;

    public String toString()
    {
        return "["+ AppNexusUtils.afterDot(this.getClass().getName()) + ": "
                  + eventTime + ", " + bid +"]";
    }

    BidHistoryRow (Sexpression eventTime, Sexpression
                   bidStrategy, Sexpression bid,
                   Sexpression dailyImpressionBudget,
                   Sexpression dailyImpressionTarget)
    {
        this.eventTime = eventTime.unboxDate();
        this.bidStrategy = bidStrategy.unboxString();
	    this.bid = (bid == Null.nil ? null : bid.unboxDouble());
        this.dailyImpressionBudget =
                (dailyImpressionBudget == Null.nil
                        ? null
                        : dailyImpressionBudget.unboxLong());
        this.dailyImpressionTarget =
                (dailyImpressionTarget == Null.nil
                        ? null
                        : dailyImpressionTarget.unboxLong());
    }
    
    /** Implementation of comparable that sorts history rows by time.
     * Rows that have no event time or that are null are sorted at the beginning.
     * @param other The other BidHistoryRow to compare to.
     * @return A negative int, zero, or positive int if this is less than, equal to, or greater than the other.
     */
    public int compareTo(BidHistoryRow other) {
    	if(other == this) {
    		return 0;
    	} else if (other == null) {
    		return 1;
    	} else {
    		Date otherEventTime = other.getEventTime();
    		Date thisEventTime = getEventTime();
    		if(otherEventTime == thisEventTime) {
    			return 0;
    		} else if(otherEventTime == null) {
    			return 1;
    		} else if(thisEventTime == null) {
    			return -1;
    		} else {
    			if(thisEventTime.after(otherEventTime)) {
    				return 1;
    			} else if(thisEventTime.before(otherEventTime)) {
    				return -1;
    			} else {
    				return 0;
    			}
    		}
    	}
    	
    }
    
    public Date getEventTime() {
    	return eventTime;
    }
    
    public Double getBid() {
    	return bid;
    }
}
