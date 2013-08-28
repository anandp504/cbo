package com.tumri.cbo.backend;

import java.util.Calendar;
import java.util.Date;

import com.tumri.af.exceptions.DataAccessException;

/** The information required from a campaign to do bid optimization.
 * Note that all Date objects represent an absolute time based on the
 * milliseconds since the start of the era which started January 1, 1970.
 * When these date objects are printed using the default Date.toString() method
 * they are represented in the default time zone of this JVM.
 */
public interface HistoricalData {
	
    /** Gets the start of the last hour for which response data has been processed by AppNexus.
     * @return The start of the last hour for which data has been processed by AppNexus.
     * @throws DataAccessException If error accessing the historical data.
     */
    public Date getLastHourProcessed() throws DataAccessException;
	
	/** Gets the last time that a bid was placed on the specified campaign prior to the specified date.
	 * If the date is null this gets the last date that any bid was placed on the campaign.
	 * Note that the date must have had a non-null bid.  Do not return the last time the daily
	 * impression limit/target was changed if the bid was not computed.
	 * @param campaignId The campaign id.
	 * @param d The date.
	 * @return The last time a bid was placed on this campaign prior to d or null if no bid was placed prior to d.
	 * @throws DataAccessException If error accessing the historical data.
	 */
    public Date getLastBidTimeBefore(long campaignId, Date d) throws DataAccessException;
    
    /** Determines if we have all response data for campaign for the specified day in the time zone of that day.
     * @param campaignId The campaign id.
     * @param day The start of the day.
     * @return True if this contains response values for all hours of the day.
     * @throws DataAccessException If error accessing the historical data.
     */
    public boolean hasAllDataForDay(long campaignId, Calendar day) throws DataAccessException;
    
    /** Gets the start of the first hour (UTC) for which there is response data for the campaign in our tables.
     * @param campaignId The campaign id.
     * @return The start of the first hour for which there is response data in our tables or null if none.
     * @throws DataAccessException If error accessing the historical data.
     */
    public Date getEarliestResponseData(long campaignId) throws DataAccessException;
    
    /** Gets the start of the last hour (UTC) for which there is response data for the campaign in our tables.
     * @param campaignId The campaign id.
     * @return The start of the last hour for which there is response data in our tables or null if none.
     * @throws DataAccessException If error accessing the historical data.
     */
    public Date getLatestResponseData(long campaignId) throws DataAccessException;
 
    /** Gets the total impressions served for the campaign between the start time and end times inclusive.
     * @param campaignId The campaign id.
     * @param from The start time (UTC).
     * @param to The end time (UTC)
     * @return The total number of impressions for this campaign between the specified times.
     * @throws DataAccessException If error accessing the historical data.
     */
    public long getTotalImpressionsServed(long campaignId, Date from, Date to) throws DataAccessException;
    
    /** Gets the last time the impression limit was changed for the campaign.
     * @param campaignId The campaign id.
     * @return The last date-time the impression limit was changed on this campaign (in UTC).
     * @throws DataAccessException If error accessing the historical data.
     */
    public Date getLastTimeDailyImpressionLimitChanged(long campaignId) throws DataAccessException;
    
    /** Gets the bid history for the campaign on the specified day.
     * @param campaignId The campaign id.
     * @param day The start of the day.
     * @return The daily bid data or null if the day is null.
     * @throws DataAccessException If error accessing the historical data.
     */
    public DailyBidData getDailyBidData(long campaignId, Calendar day) throws DataAccessException;

    /** Gets the daily response data for the campaign on the specified day.
     * Note that the response may have 23 hours for the day that represents
     * the start of daylight savings time, or 25 hours for the day that
     * ends daylight savings time.
     * @param campaignId The campaign id.
     * @param day The start of the day.
     * @return The daily response data or null if the day is null.
     * @throws DataAccessException If error accessing the historical data.
     */
    public DailyResponseData getDailyResponseData(long campaignId, Calendar day) throws DataAccessException;
}
