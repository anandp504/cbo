package com.tumri.cbo.backend;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;

import com.tumri.af.exceptions.DataAccessException;
import com.tumri.af.utils.DateUtils;

/** An implementation of CampaignHistory that uses 
 * in-memory maps of historical data rows and bid history rows
 * for a single campaign.
 */
public class InMemoryCampaignHistory implements HistoricalData {
	
	private Map<Date, HistoricalDataRow> histData;
	private BidHistoryRow[] bidHistory;
	private Date lastHistoryTime;
	private Date lastImpressionLimitChange;
	private long campaignId;
	
	// ----------------------------- Constructor --------------------
	
	/** Constructor
	 * @param campaignId The campaign id.
	 * @param histData The historical response data.
	 * @param bidHistory The historical bids that were applied.
	 * @param lastHistoryTime The start of the last hour that AppNexus processed.
	 */
	InMemoryCampaignHistory(long campaignId, Map<Date, HistoricalDataRow> histData,
							BidHistoryRow[] bidHistory, Date lastHistoryTime) {
		this.campaignId = campaignId;
		this.histData = histData;
		this.bidHistory = bidHistory;
		this.lastHistoryTime = lastHistoryTime;
	}
    
	// --------------------------- Public methods ----------------------
    
    /** Gets the start of the last hour for which response data has been processed.
     * @return The start of the last hour for which response data has been processed or null if no processing has occurred.
     * @throws DataAccessException If error accessing the historical data.
     */
    public Date getLastHourProcessed() throws DataAccessException {
    	return lastHistoryTime;
    }
    
	/** Gets the last time that a bid was placed on the campaign prior to the specified date.
	 * If the date is null the last bid date is returned.
	 * Note that the date must have had a non-null bid.  Do not return the last time the daily
	 * impression limit/target was changed if the bid was not computed.
	 * @param campaignId The campaign id.
	 * @param d The date.
	 * @return The last time a bid was placed on this campaign prior to d or null if no bid was placed prior to d.
	 * @throws DataAccessException If error accessing the historical data.
	 */
    public Date getLastBidTimeBefore(long campaignId, final Date d) throws DataAccessException {
    	assertCorrectCampaignId(campaignId);
    	Date lastBidTime = null;
    	BidHistoryRow lastBid = getLastBidPriorTo(bidHistory, d);
    	if(lastBid != null) {
    		lastBidTime = lastBid.getEventTime();
    	}
    	return lastBidTime;
    }
    
    /** Determines if we have all response data for the campaign
     * on the specified day in the time zone of that day.
     * @param campaignId The campaign id.
     * @param day The start of the day.
     * @return True if this contains response values for all hours of the day.
     * @throws DataAccessException If error accessing the historical data.
     */
    public boolean hasAllDataForDay(long campaignId, final Calendar day) throws DataAccessException {
    	boolean result = false;
    	assertCorrectCampaignId(campaignId);
    	Calendar lastHourOfDay = DateUtils.setToStartOfHour(DateUtils.copyCalendar(day), 23);
    	Date startOfLastHourOfDay = lastHourOfDay.getTime();
    	
    	Date lastHourProcessed = getLastHourProcessed();
    	if((lastHourProcessed != null) && (!lastHourProcessed.before(startOfLastHourOfDay))) {
    		result = true;
    		Utils.logThisPoint
                    (Level.DEBUG,
                     "Last hour processed = " + lastHourProcessed.getTime() +
   				     ", last hour of the day = " + startOfLastHourOfDay +
				     ", hasAllDataForDay(" + DateUtils.toDateTimeString(day) +
				     ") returns " + result);
    	} else {
    		Date d = getLatestHistoricalDataDate(histData);
    		result = (d != null) && (!d.before(startOfLastHourOfDay));
    		Utils.logThisPoint
                    (Level.DEBUG,
                     "Latest historical data at " + d +
    				 ", last hour of the day = " + startOfLastHourOfDay +
    				 ", hasAllDataForDay(" + DateUtils.toDateTimeString(day) +
                             ") returns " + result);
    	}
    	return result;
    }
    
    /** Gets the start of the first hour (UTC) for which there is response data for the campaign in our tables.
     * @param campaignId The campaign id.
     * @return The start of the first hour for which there is response data in our tables or null if none.
     * @throws DataAccessException If error accessing the historical data.
     */
    public Date getEarliestResponseData(long campaignId) throws DataAccessException {
    	assertCorrectCampaignId(campaignId);
    	return getEarliestHistoricalDataDate(histData);
    }
    
    /** Gets the start of the last hour (UTC) for which there is response data for the campaign in our tables.
     * @param campaignId The campaign id.
     * @return The start of the last hour for which there is response data in our tables or null if none.
     * @throws DataAccessException If error accessing the historical data.
     */
    public Date getLatestResponseData(long campaignId) throws DataAccessException {
    	assertCorrectCampaignId(campaignId);
    	return getLatestHistoricalDataDate(histData);
    }
 
    /** Gets the total impressions served between the start time and end times inclusive for the campaign.
     * @param campaignId The campaign id.
     * @param from The start time (UTC).
     * @param to The end time (UTC)
     * @return The total number of impressions for this campaign between the specified times.
     * @throws DataAccessException If error accessing the historical data.
     */
    public long getTotalImpressionsServed(long campaignId, final Date from, final Date to) throws DataAccessException {
    	assertCorrectCampaignId(campaignId);
    	return getTotalImpressions(histData, from, to);
    }
    
    /** Gets the last time the impression limit was changed for the campaign.
     * @param campaignId The campaign id.
     * @return The last date-time the impression limit was changed on this campaign (in UTC).
     * @throws DataAccessException If error accessing the historical data.
     */
    public Date getLastTimeDailyImpressionLimitChanged(long campaignId) throws DataAccessException {
    	assertCorrectCampaignId(campaignId);
    	// TODO:  Set this
    	return lastImpressionLimitChange;
    }
    
    /** Gets the bid history for the campaign on the specified day.
     * @param campaignId The campaign id.
     * @param day The start of the day.
     * @return The daily bid data or null if the day is null.
     * @throws DataAccessException If error accessing the historical data.
     */
    public DailyBidData getDailyBidData(long campaignId, final Calendar day) throws DataAccessException {
    	assertCorrectCampaignId(campaignId);
    	DailyBidData result = null;
    	BidHistoryRow[] relevantRows = null;
    	if(day != null) {
    		Calendar c = DateUtils.setToStartOfDay(DateUtils.copyCalendar(day));
    		Date startTime = c.getTime();
    		DateUtils.setToEndOfDay(c);
    		Date endTime = c.getTime();
    		List<BidHistoryRow> rows = getRelevantBidHistory(bidHistory, startTime, endTime);
    		if(rows != null) {
    			relevantRows = new BidHistoryRow[rows.size()];
    			relevantRows = rows.toArray(relevantRows);
    		}
    	}
    	result = new DailyBidData(day, relevantRows);
    	return result;
    }

    /** Gets the daily response data for the campaign on the specified day.
     * Note that the response may have 23 hours for the day that represents
     * the start of daylight savings time, or 25 hours for the day that
     * ends daylight savings time.
     * @param campaignId The campaign id.
     * @param day The start of the day.
     * @return The daily response data or null if the day is null.
     * @throws DataAccessException If error accessing the historical data.
     */
    public DailyResponseData getDailyResponseData(long campaignId, final Calendar day) throws DataAccessException {
    	assertCorrectCampaignId(campaignId);
    	DailyResponseData result = null;
    	HistoricalDataRow[] rows = null;
    	if(day != null) {
    		Calendar c = DateUtils.setToStartOfDay(DateUtils.copyCalendar(day));
    		Date startTime = c.getTime();
    		DateUtils.setToEndOfDay(c);
    		Date endTime = c.getTime();

    		long dt = endTime.getTime() - startTime.getTime() + DateUtils.ONE_HOUR_MS/2L;
    		int numHours = (int)(dt/DateUtils.ONE_HOUR_MS);
    		rows = new HistoricalDataRow[numHours];
    		Iterator<HistoricalDataRow> iter = getHisotricalDataBetween(histData, startTime, endTime);
    		HistoricalDataRow row = null;
    		if(iter != null) {
    			while(iter.hasNext()) {
    				row = iter.next();
    				Date d = row.getDate();
    				rows[getHoursBetween(d, startTime)] = row;
    			}
    		} 
    	}
    	if(rows == null) {
    		rows = new HistoricalDataRow[24];  // default to 24 hrs.
    	}
    	result = new DailyResponseData(day, rows);
    	return result;
    }
    
    // ------------------------------- Private methods ------------------------------
    
    /** Gets the earliest date (UTC) that there is history data for.
     * @param histData The history data.
     * @return The earliest date that there is history data for or null if none.
     */
    private Date getEarliestHistoricalDataDate(Map<Date, HistoricalDataRow> histData) {
    	Date d = null;
    	if(histData != null) {
    		d = Collections.min(histData.keySet());
    	}
    	return d;
    }
    
    /** Gets the last date (UTC) that there is history data for.
     * @param histData The history data.
     * @return The latest date that there is history data for or null if none.
     */
    private Date getLatestHistoricalDataDate(Map<Date, HistoricalDataRow> histData) {
    	Date d = null;
    	if(histData != null) {
    		d = Collections.max(histData.keySet());
    	}
    	return d;
    }
    
    /** Gets the last bid history row that is strictly prior to the specified time.
     * If the last time is null this just gets the latest bid time.
     * Only returns those times where the bid is non-null since now we might have
     * sent a "bid" to AppNexus in order to just change the impression limit.
     * May return null if there is not any bid history prior to the time.
     * This should be part of the BidHistory API.
     * @param bidHistory The bid history.
     * @param lastDate The date in UTC prior to which to get the last history row.
     * @return The last bid history row strictly prior to t or null if none.
     */
    private BidHistoryRow getLastBidPriorTo(BidHistoryRow[] bidHistory, Date lastDate) {
    	BidHistoryRow row = null;
    	if(bidHistory != null) {
    		BidHistoryRow testRow = null;
    		Date testDate = null;
    		int n = bidHistory.length;
    		while(--n >= 0) {
    			testRow = bidHistory[n];
    			if(testRow != null) {
    				testDate = testRow.getEventTime();
    				if((testDate != null) && ((lastDate == null) || testDate.before(lastDate))) {
    					if(((row == null) || testDate.after(row.getEventTime())) && (testRow.getBid() != null)) {
    						row = testRow;
    					}
    				}
    			}
    		}
    	}
    	return row;
    }
    
    /** Gets the bid history rows that are relevant to 
     * determining what the bid prices were within the specified time range.
     * This includes the last change before or at the start time, and all changes
     * up to and including those at the end time.
     * @param bidHistory The bid history.
     * @param startTime The start time in UTC or null to specify the beginning of time.
     * @param endTime The end time in UTC or null to specify the end of time.
     * @return The bid price changes that are relevant to the time range (never null).
     */
    private List<BidHistoryRow> getRelevantBidHistory(BidHistoryRow[] bidHistory, Date startTime, Date endTime) {
    	Date earliestEventTime = null;
    	List<BidHistoryRow> result = getSortedBidHistory(bidHistory, startTime, endTime);
    	if((startTime != null) && (result != null)) {
    		if(result.size() > 0) {
    			BidHistoryRow row = result.get(0);
    			if(row != null) {
    				earliestEventTime = row.getEventTime();
    			}
    		}
    		if((earliestEventTime == null) || earliestEventTime.after(startTime)) {
    			BidHistoryRow priorHistory = getLastBidPriorTo(bidHistory, startTime);
    			if(priorHistory != null) {
    				result.add(0, priorHistory);   // Add the bid history from the start of the time.
    			}
    		}
    	}
    	return result;
    }

    /** Gets all the bid history rows whose event times are at or after the specified
     * start time and before or at the specified end time.
     * The times are specified in UTC.
     * Sorts the results by the event time of the result.
     * @param bidHistory The bid history.
     * @param startTime The start time in UTC or null to specify the beginning of time.
     * @param endTime The end time in UTC or null to specify the end of time.
     * @return The sorted bid history rows within the specified date/time range (inclusive) (never null).
     */
    private List<BidHistoryRow> getSortedBidHistory(BidHistoryRow[] bidHistory, Date startTime, Date endTime) {
    	// This should be part of the BidHistory API.
    	List<BidHistoryRow> result = new ArrayList<BidHistoryRow>();
    	if(bidHistory != null) {
        	BidHistoryRow row = null;
        	Date eventTime = null;
    		int len = bidHistory.length;
    		for(int i = 0; i < len; i++) {
    			row = bidHistory[i];
    			if(row != null) {
    				eventTime = row.getEventTime();
    				if(eventTime != null) {
    					if(((startTime == null) || (!eventTime.before(startTime))) && 
    					   ((endTime == null) || (!eventTime.after(endTime)))) {
    						result.add(row);
    					}
    				}
    			}
    		}
    	}
    	Collections.sort(result);
    	return result;
    }

    
    /** Gets the total impressions served between the start time and end times inclusive.
     * @param histData The historical data for the campaign.
     * @param from The start time (UTC).
     * @param to The end time (UTC)
     * @return
     */
    private long getTotalImpressions(Map<Date, HistoricalDataRow> histData, Date from, Date to) {
    	long total = 0L;
    	Iterator<HistoricalDataRow> rows = getHisotricalDataBetween(histData, from, to);
		if(rows != null) {
			HistoricalDataRow row = null;
			while(rows.hasNext()) {
				row = rows.next();
				if(row != null) {
					total += row.getImpressionCount();
				}
			}
		}
		return total;
    }
    

    /** Gets an iterator containing the historical data rows with UTC dates between the dates passed in.
     * If the start or end date passed in is null there is no minimum or maximum of the dates in the returned iterator.
     * @param start The first date that is acceptable or null if there is no limit.
     * @param end The last date that is acceptable or null if there is no limit.
     * @param histData The historical data.
     * @return An iterator over the data rows or null if there are none.
     */
    private Iterator<HistoricalDataRow> getHisotricalDataBetween(final Map<Date, HistoricalDataRow> histData,
    															 final Date start, final Date end) {
    	Iterator<HistoricalDataRow> result = null;
    	if(histData != null) {
    		Date d = null;
    		List<HistoricalDataRow> returnedList = new ArrayList<HistoricalDataRow>();
    		for(HistoricalDataRow row : histData.values()) {
    			if(row != null) {
    				d = row.getDate();
    				if(d != null) {
    					if(((start == null) || (!start.after(d))) && 
    						((end == null) || end.after(d))) {
    						returnedList.add(row);
    					}
    				}
    			}
    		}
    		result = returnedList.iterator();
    	}
    	return result;
    }
    
    /** Gets the number of hours between two dates.
     * Rounds to the nearest 1/4 hour.
     * @param d2 The later date (assumed not null).
     * @param d1 The earlier date (assumed not null).
     * @return The number of hours between d2 and d1.
     */
    private int getHoursBetween(Date d2, Date d1) {
    	long n = (DateUtils.ONE_HOUR_MS/4 + d2.getTime() - d1.getTime())/DateUtils.ONE_HOUR_MS;
    	return (int)n;
    }
    
    /** Asserts that the campaign id passed in matches the id of the campaign for which we have data.
     * @param campaignId The campaign ID.
     * @throws DataAccessException If the campaign id is for the wrong campaign.
     */
    private void assertCorrectCampaignId(long campaignId) throws DataAccessException {
    	if(campaignId != this.campaignId) {
    		throw new DataAccessException("No data found for campaign id " + campaignId);
    	}
    }
}
