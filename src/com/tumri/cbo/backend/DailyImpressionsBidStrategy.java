package com.tumri.cbo.backend;

import java.text.MessageFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.tumri.cbo.utils.CBOConfigurator;
import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;

import com.tumri.af.exceptions.DataAccessException;
import com.tumri.af.utils.DateUtils;

/** A bid strategy that adjusts the bid price daily to achieve a daily impression target.
 * This strategy assumes that all calls to the suggestBid() method are for currently
 * running campaigns with valid start dates, end dates, daily impression limits, 
 * and target impression counts, and that the campaign does NOT have day-part targeting.
 * It assumes we are in a region of linear impression response to bid price and just uses
 * the most recent similar day's data to correct the bid price.
 * <p>
 * The strategy can be run at any time of the day.  It evaluates and updates the
 * bid price once per day after the previous day's response is known.  If the number
 * of impressions in a day exceeds the maximum daily impression limit, this might run
 * early by running in the evening of the day in which the bid cap was met so long
 * as all hourly data is available for that day.
 * <p>
 * Note that AppNexus appears to handle changes to daylight savings time by reporting 
 * all values in the time zone of the starting date of the report when reporting in ET.  
 * It is not clear how the per-day impression limits are enforced on these days.
 * <p>
 */
public class DailyImpressionsBidStrategy extends AbstractBidStrategy implements BidStrategy {

    private static final String NAME = "Daily impressions";
    // This provided so that we don't fall over, now that we've disabled the
    // ImpressionTargetBidStrategy!
    private static final String ALT_NAME = "Impression target";

    public static final DailyImpressionsBidStrategy STRATEGY =
            new DailyImpressionsBidStrategy(NAME, ALT_NAME);

    // Ratio of impression limit/cap to impression target.
    private static double IMPRESSION_CAP_TO_TARGET_RATIO = 1.0;
   
    private final static int MORNING_END_HOUR = 7;     // The morning ends at 7 AM.
    private final static int EVENING_START_HOUR = 18;  // Evening starts at 6 PM.
    
    // The minimum number of mS between bids when morning bidding.
    // This is the time between the start of the evening of the previous day and the end of the morning.
    private static long MIN_MORNING_BID_INTERVAL = (MORNING_END_HOUR + 24 - EVENING_START_HOUR)*DateUtils.ONE_HOUR_MS + 1L;
 
    // The minimum number of mS between bids when evening bidding.
    // This is the time between the start of the evening and the end of the morning the current day.
    private static long MIN_EVENING_BID_INTERVAL = (EVENING_START_HOUR - MORNING_END_HOUR)*DateUtils.ONE_HOUR_MS + 1L;
    
    private final static TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("America/New_York");
    
    //private final static long ONE_HOUR_MS = 3600000L;                   // 1 Hour in mS
   // private final static long STALE_BID_DATA_TIME = 90*24*ONE_HOUR_MS;  // 90 days in mS
    
    // A constant used to ensure that hours are completely contained in time regions.
    //private final static long OVERLAP_TIME_MS = 10*60*1000;   // 10 mins
    
    // The fraction of the daily impression target within which there is no need to change the bid.
    // If the actual impressions are within this fraction of the target the bid can remain the same.
    private final static double CONTROL_REGION = 0.02;
    
    // The impression-weighted fraction of bid prices that must be within +/- the
    // bid price deviation of the average bid price in order to consider that 
    // the bid prices during the day were stable enough to use to predict future values.
    private final static double MIN_BID_PRICE_FRACTION_WITHIN_DEVIATION = 0.7;
    private final static double BID_PRICE_DEVIATION = 0.2;
    
    // A small monitary value below which the bid price is considered to be zero.
    private final static double ESSENTIALLY_ZERO_PRICE = 1.0E-6;

    /* Bid processing events.
     * 
     * BidStrategyEvent: - Emit just 1 event per invocation.
     * DailyImpressionsBidStrategyEvent
     * Starting bid strategy X on campaign Y at (campaign time zone time)
     * Not bidding because wrong time of day.
     * Not bidding because not all data is available for the day.  (hrs, imps, target, limit)
     * Not bidding because current bid is close enough - (hrs, imps, projection, target, ratio).
     * Not bidding because of an error.
     * Bidding 0 because campaign has ended or reached its lifetime impression limit (end date, lifetime imps served, lifetime imp budget)
     * Bidding early because daily limit exceeded (old bid, day, imps, projected imps, target imps, imp limit, ratio, new bid)
     * Bidding normally (old bid (ECP), day, imps, projected imps, target imps, imp limit, ratio, new bid)
     * Bidding limited by max bid - same args as bidding normally + max bid.
     * 
     * 
     * DailyImpressionTarget/LimitResultEvent: - Emit just 1 event per invocation.
     * Daily impression limit adjustment messages:
     *  Checking daily impression limit/target on campaign Y at (campaign time zone time) + UTC time
     *  Cannot compute daily cap because of error: (reason).
     *  Adjusting daily limit/target (imps served to T, time left, lifetime imp budget, old cap, new cap)
     *  Not adjusting daily cap because it is close enough. (show calculations as above) usually happens.
     * 
     *  Observer can send warning - daily cap adjustment is very large
	 *
	 * BidStrategyWarning:
     * Warnings along the way
     *  3/10 - no time zone specified for campaign...using default ET  (INCORRECT CAMPAIGN SETUP)
     * 
     * 
     * 
     * 3/10 STRANGE: Day does not have 24 hours.  Time change? 
     * 1/10 BORING FINAL RESULT: Not bidding because not all data is available.
     * 2/10 FINAL RESULT: Not bidding because current bid is close enough.
     * 6/10 WARNING MEDIUM: No response data for day - continuing, but something might be wrong.
     * 4/10 WARNING LOW: No bid history for the day ECP bid? - continuing.
     * 
     * 1/10 BORING FINAL RESULT: Final result of bid processing, but did not bid because it was not supposed to bid
     * 
     * Final result of bid processing, but did not bid because of missing data or error.
     *   9/10 ERROR: Could not bid because no impression cap / target specified.  (INCORRECT CAMPAIGN SETUP)
     * FINAL RESULT: Final result of bid processing and did compute a bid.
     *   7/10   NOTE: Warning - Bid is limited by max bid (add to final result).
     *       
     * WARNING MEDIUM: FINAL RESULT and possibly send message: Required bid to way too high 
     * above max bid to meet daily impression target 
     *  - either the target is way too high or the campaign has too many constraints.  (POSSIBLE INCORRECT CAMPAIGN SETUP)
     * 
     * 
     * Daily cap/target adjustment messages:
     *  5/10 Warning, but can continue... Negative or zero daily cap - could not extrapolate rate.  (INCORRECT CAMPAIGN SETUP)
     *  5/10 Warning - daily cap adjustment is very large - send a message  (POSSIBLE INCORRECT CAMPAIGN SETUP)
     *  1/10 FINAL RESULT: Adjusting daily cap normally
     *  2/10 FINAL RESULT: Not adjusting daily cap because it is close enough. 
     *   
     *   
     * Importance levels
     * 10 - Page someone
     * 9 - Send email immediately 
     * - Send email immediately if more than 2 in 24 hrs.  
     * - Send email immediately if more than 2 in 1 hour.
     * - High warning
     * - Medium warning
     * - Low warning
     * - Consolidate messages for 24 hours and send a daily report.
     * - Note: - keep for 7 days then delete unless error.
     * - Normal operation - keep for 7 days then delete unless error.
     * - 
     * 1 - Debug - keep for 2 days then delete unless error.
     */
    
    // Messages
    protected final static String MSG_NO_DAILY_CAP = 
    	"No daily impression limit specified for campaign: {0}.";
    
    protected final static String MSG_DAILY_IMPR_TARGET_NEGATIVE = 
    	"Invalid impression target ({1}) for campaign {0}.";

    
    private final static String MSG_NO_CAMPAIGN_TIME_ZONE = 
    	"No time zone specified for campaign: {0}.  Assuming default time zone: {1}.";
    
    private final static String MSG_TOO_SOON_TO_BID = 
    	"Not bidding on campaign {0}.  Only {1} since last bid at {2}.";
    
    private final static String MSG_NO_MIDDAY_BIDDING = 
    	"No bidding on campaign {0} between " +
    	MORNING_END_HOUR + ":00 and " + 
    	EVENING_START_HOUR + ":00 {1}.  " + 
		"Current hour in campaign time zone = {2}:00";
	
    private final static String MSG_NO_DAILY_TARGET = 
    	"No daily impression target specified for campaign: {0}.";

    private final static String MSG_BID_ECP_PART_OF_DAY = 
    	"Bid ECP part of day and fixed bids other parts of the day";
    
    private final static String MSG_NO_IMPRESSIONS_FOUND_FOR_DAY =
    	"No impressions were found for this day";

    private final static String MSG_TOO_MUCH_BID_VARIATION =
		"Less than {0,number,percent} of bids were served within +/-{1,number,percent} " +
		"of the average bid price of {2,number,currency}.";

    private final static String MGS_APPLYING_STRATEGY =
    	"Applying {0} bid strategy to campaign: {1} (id={2,number,integer})";
    
    private final static String MSG_MORNING_BIDDING =
    	"Morning bidding. Current hour in campaign time zone = {0}.";
    
    private final static String MSG_EVENING_BIDDING =
    	"Evening bidding. Current hour in campaign time zone = {0}.";
    
    private final static String MSG_LIMITED_BID_TO_MAX = 
    	"Suggested bid of {0,number,currency} is being limited by max bid {1,number,currency}.";
    
    private final static String MSG_SUGGESTION = 
    	"DailyImpressionsBidStrategy({0}) suggesting: {1}.";
    	
    private final static String MSG_BIDDING_ECP = 
    	"Bidding ECP throughout the day.  Assuming bid price is max hourly CPM paid: {0,number,currency}.";
    
    private final static String MSG_INCOMPATIBLE_DAILY_RESPONSE =
    	"Incompatible bid response values!";

	private final static String MSG_BID_PRICE_VARIATION =
		"{0,number,percent} of the impresions were served within +/-{1,number,percent} " + 
		"of the average bid price of {2,number,currency}.";
	
	private final static String MSG_DAY_NOT_DONE = 
		"Not bidding on campaign {0}.  Full day of data is not available, and not over pacing.";
	
	private final static String MSG_NO_IMPRESSIONS_FOR_DAY = 
		"Not bidding on campaign {0} because there is no impression data for day: {1}.";
	
	private final static String MSG_NO_RESPONSE_DATA_FOR_DAY =
		"Not bidding on campaign {0} because there is no response data for day: {1}.";
	
	private final static String MSG_NO_BID_HISTORY_FOR_DAY =
		"Not bidding on campaign {0} because there is no bid history for day: {1}.";
	
	private final static String MSG_ESTIMATED_TO_DESIRED_IMPS = 
		"Ave bid price: {0,number,currency}, estimated daily imps = {1,number,integer}, " + 
		" impr target = {2,number}, imps/target = {3,number}.";
	
	private final static String MSG_ON_TARGET = 
		"Not bidding on campaign {0} because the ratio of observed to desired impressions, {1,number}, " +
		"is within the control range of +/-{2,number,percent}.";
	
	private final static String MSG_BID_VARIATION_EXCEPTION =
		"Not bidding on campaign {0} because of {1}";
    
	// ------------------------- Constructors ---------------------
	
    protected DailyImpressionsBidStrategy(String... names) {
        super(names);
    }
    
    // -------------------------- Public methods --------------------
    
    /** Suggests the bid and or changes to the daily impression limit.
     * @param camp The campaign (assumed not null).
     * @param hist The historical data.
     * @return A new bid suggestion, which may be null if no changes are needed.
     * @throws DataAccessException If error accessing historical data.
     */
    public BidSuggestion suggestBid(CampaignInfo camp, HistoricalData hist) throws DataAccessException {
    	    	
    	boolean debug = false;
    	BidSuggestion result = null;

    	long campaignId = camp.getCampaignId();
    	String campaignName = camp.getCampaignName();
    	Long dailyImprLimit = camp.getDailyImpressionLimit();
		Double maxBid = camp.getMaximumBid();

		logInfo(MessageFormat.format(MGS_APPLYING_STRATEGY, getPrimaryName(), campaignName, campaignId));
		
		// Default the time zone if it is not specified.
		TimeZone timezone = camp.getTimeZone();
    	if(timezone == null) {
    		logWarning(MessageFormat.format(MSG_NO_CAMPAIGN_TIME_ZONE, campaignName, DEFAULT_TIME_ZONE.getDisplayName()));
    		timezone = DEFAULT_TIME_ZONE;
    	}
    	
		// Represents the current time in the campaign's time zone.
		Calendar currentCampaignTime = Calendar.getInstance(timezone);
		Calendar today = DateUtils.setToStartOfDay(DateUtils.copyCalendar(currentCampaignTime));
		Date now = currentCampaignTime.getTime();
		int hourOfDay = currentCampaignTime.get(Calendar.HOUR_OF_DAY);
		logDebug("Current time in campaign time zone = " + DateUtils.toDateTimeString(currentCampaignTime));
		
		Long dailyImprTarget = null;
		double limitToTargetRatio = getDailyImpressionLimitToTargetRatio();
		
		// Adjust the daily impression target and limit if needed.
		Long newDailyImpressionTarget = computeNewDailyImpressionTarget(camp, hist, timezone, now);
		if(newDailyImpressionTarget != null) {
			dailyImprTarget = newDailyImpressionTarget;
			dailyImprLimit = new Long(Math.round(dailyImprTarget.doubleValue()*limitToTargetRatio));
		} else if(dailyImprLimit != null) {
	    	dailyImprTarget = new Long(Math.round(dailyImprLimit.doubleValue()/limitToTargetRatio));
		}
    	
    	if(dailyImprLimit == null) {
    		logError(MessageFormat.format(MSG_NO_DAILY_CAP, campaignName));
    	} else if(dailyImprLimit.longValue() <= 0L) {
    		logError(MessageFormat.format(MSG_DAILY_IMPR_TARGET_NEGATIVE, campaignName, dailyImprLimit.longValue()));
    	} else {
    		long dailyCap = dailyImprLimit.longValue();
    		if(dailyImprTarget == null) {
    			logError(MessageFormat.format(MSG_NO_DAILY_TARGET, campaignName));
    		} else {
    			long dailyTarget = dailyImprTarget.longValue();
    			Date lastBidTime = hist.getLastBidTimeBefore(campaignId, now);
    			if(debug || (hourOfDay < MORNING_END_HOUR)) {				// Do morning processing on previous day
    				logDebug(MessageFormat.format(MSG_MORNING_BIDDING, hourOfDay));
    				Calendar yesterday = DateUtils.copyCalendar(today);
    				yesterday.add(Calendar.DATE, -1);
    				if((lastBidTime != null) && (now.getTime() - lastBidTime.getTime() < MIN_MORNING_BID_INTERVAL)) {
    					logResult(MessageFormat.format(MSG_TOO_SOON_TO_BID, campaignName, formatTimeInterval(now, lastBidTime), lastBidTime));
    				} else {
    					result = suggestBidIfDataAvaiable(camp, yesterday, dailyCap, dailyTarget, hist);
    				}
    			} else if(hourOfDay >= EVENING_START_HOUR) {	// Do evening processing on current day.
    				logDebug(MessageFormat.format(MSG_EVENING_BIDDING, hourOfDay));
    				if((lastBidTime != null) && (now.getTime() - lastBidTime.getTime() < MIN_EVENING_BID_INTERVAL)) {
    					logResult(MessageFormat.format(MSG_TOO_SOON_TO_BID, campaignName, formatTimeInterval(now, lastBidTime), lastBidTime));
    				} else {
    					result = suggestBidIfDataAvaiable(camp, today, dailyCap, dailyTarget, hist);
    				}
    			} else {
    				// Do nothing during the middle of the day.
    				logResult(MessageFormat.format(MSG_NO_MIDDAY_BIDDING, 
    						  campaignName, timezone.getDisplayName(), hourOfDay));
    			}
    		}
    	}
    	
    	// Set the new daily impression limit if it needs to be changed.
		if((newDailyImpressionTarget != null) && (dailyImprLimit != null)) {
			if(result == null) {
				result = new BidSuggestion();
			}
			result.setNewDailyImpressionLimit(dailyImprLimit.longValue());
		}
		
		// Limit the maximum bid.
    	if(result != null) {
    		Double bid = result.getSuggestedBid();
    		if((bid != null) && (maxBid != null) && (bid.doubleValue() > maxBid.doubleValue())) {
    			// This should probably change the bid reason.
    			logInfo(MessageFormat.format(MSG_LIMITED_BID_TO_MAX, bid, maxBid));
    			result.setSuggestedBid(maxBid);
    		}
    		logResult(MessageFormat.format(MSG_SUGGESTION, campaignName, result.toString()));
    	}
    	return result;
    }
    
    /** Gets the ratio of the daily impression limit to the daily impression target.
     * @return The ratio of the daily impression limit to the daily impression target.
     */
    public double getDailyImpressionLimitToTargetRatio() {
    	return IMPRESSION_CAP_TO_TARGET_RATIO;
    }
    
    // -------------------------- Protected methods ----------------------

    /** This method determines if the daily impression target should be
     * changed, and, if so, returns a new daily impression target.
     * The new target is computed based on the number of impressions served
     * by the beginning of the day today, the lifetime impression 
     * target of the campaign, and the number of days left in the campaign.
     * This method only returns a non-null value if the target needs to be changed.
     * It always returns null if the bid strategy is not allowed to 
     * adjust the daily impression target.
     * @param camp The campaign.
     * @param hist The historical data.
     * @param tz The possibly defaulted campaign's time zone.
     * @param now The time when the calculation started.
     * @exception DataAccessException If error accessing historical data.
     */
    protected Long computeNewDailyImpressionTarget(final CampaignInfo camp, HistoricalData hist, TimeZone tz, Date now)
                      throws DataAccessException {
    	
    	// This implementation always returns null
    	// which indicates the daily impression target
    	// should not be changed.  Subclasses may override this.
    	
    	return null;
    }

    /** Logs and/or prints a debug message. 
     * @param msg The message.
     */
    protected void logDebug(String msg) {
        Utils.logThisPoint(Level.DEBUG, msg);
    }
    
    /** Logs and/or prints some noteworthy information
     * that is not a result or an error or warning. 
     * @param msg The message.
     */
    protected void logNote(String msg) {
        Utils.logThisPoint(Level.DEBUG, msg);
    }
    
    /** Logs the result of the bid strategy computation.
     * This is usually done when no bid is made to keep track of the reason.
     * @param msg The message.
     */
    protected void logResult(String msg) {
        Utils.logThisPoint(Level.INFO, msg);
    }
    
    /** Logs and/or prints an informational message.
     * @param msg The message.
     */
    protected void logInfo(String msg) {
        Utils.logThisPoint(Level.INFO, msg);
    }
    
    /** Logs and/or prints a warning message.
     * @param msg The message.
     */
    protected void logWarning(String msg) {
        Utils.logThisPoint(Level.WARN, msg);
    }

    /** Logs and/or prints an error message.
     * @param msg The message.
     */
    protected void logError(String msg) {
        Utils.logThisPoint(Level.ERROR, msg);
    }

    static
    {
        Utils.addLogElideMethod(DailyImpressionsBidStrategy.class, "logDebug");
        Utils.addLogElideMethod(DailyImpressionsBidStrategy.class, "logNote");
        Utils.addLogElideMethod(DailyImpressionsBidStrategy.class, "logResult");
        Utils.addLogElideMethod(DailyImpressionsBidStrategy.class, "logWarning");
        Utils.addLogElideMethod(DailyImpressionsBidStrategy.class, "logError");
        Utils.addLogElideMethod(DailyImpressionsBidStrategy.class, "logInfo");
    }
    
    // --------------------------- Private methods ------------------
    
    /** Suggests a bid to be made for the specified day if the all
     * data for the day is available.
     * Returns a null suggestion if the data is not all available 
     * or if there is an inconsistency in the data that would make it
     * too hard to suggest a new bid price.
     * Assumes the history is all for the campaign that is being optimized.
     * All dates passed into this method, and all history dates are assumed to be in UTC.
     * It is assumed that today's date is within the campaign start and 
     * end dates, and that the campaign is "active", and that the total
     * lifetime impression count has not been reached.
     * @param camp The campaign.
     * @param day A Calendar set to the start of the day in the correct time zone.
     * @param dailyCap The maximum number of impressions to serve in one day.
     * @param dailyTarget The desired number of impressions to serve in one day (assumed <= dailyCap)
     * @param hist The historical metric data including impression counts and cost.
     * @return The suggested bid or null if none can or should be made.
     * @throws DataAccessException If error accessing the historical data.
     */
    private BidSuggestion suggestBidIfDataAvaiable(CampaignInfo camp, Calendar day, long dailyCap, long dailyTarget,
    		                                       HistoricalData hist) throws DataAccessException {
    	BidSuggestion result = null;

    	String campaignName = camp.getCampaignName();
    	long campaignId = camp.getCampaignId();
    	
    	// The day will only be non-null if it is the morning or the evening.
    	// Still, this does not guarantee that it is the appropriate time to update the bid.
    	if(day != null) {
    		logDebug("Computing new bid price based on data from " + DateUtils.toDateString(day));
    		// The day here is the start of the last AppNexus day to be considered.
    		// It may or may not have a full day's worth of data.  
    		// If it does not, then no suggestion will be returned.

    		// Characterize the day's impressions.
    		DailyResponseData dailyResponse = hist.getDailyResponseData(campaignId, day);
    		if(dailyResponse != null) {
    			int hrs = dailyResponse.getNumberOfHoursInDay();
    			if(hrs != 24) {
    				logNote(DateUtils.toDateString(day) + " has " + hrs + " hours - must be time change!");
    			}
    			
    			logDebug(getDailyResponseAsString(dailyResponse));
    			
    			BidReason reason = null;
    			if(hrs > 0) {
    				long totalImpressions = dailyResponse.getTotalImpressions();
					logDebug("Total impressions = " + totalImpressions + 
							 ", daily target = " + dailyTarget + ", daily cap = " + dailyCap);
					if(dailyTarget < 0L) {
						logWarning("Negative daily impressions target: " + dailyTarget);
						logDebug("Setting bid to zero");
						reason = BidReason.OVERPACING;
					} else if(dailyTarget == 0L) {
						logDebug("Daily impression target is zero.");
						reason = BidReason.OVERPACING;
					} else if(totalImpressions >= dailyCap) {
    					logDebug("Total impressions >= daily cap");
    					reason = BidReason.OVERPACING;
    				} else if(dailyResponse.getNumberOfHoursWithImpressions() >= hrs) {
    					logDebug("Has impressions for all hours of the day.");
    					reason = (totalImpressions <= dailyTarget) ? BidReason.UNDERPACING : BidReason.OVERPACING;
    				} else if(dailyResponse.getLastHourWithImpressions() >= (hrs - 1)) {
    					logDebug("Have impressions to end of day, but not for all hours.");
    					reason = (totalImpressions <= dailyTarget) ? BidReason.UNDERPACING : BidReason.OVERPACING;
    				} else {
    					// See if the data has been processed to the end of the day.
    					Calendar c = DateUtils.copyCalendar(day);
    					DateUtils.setToEndOfDay(c);
    					DateUtils.setToStartOfHour(c);
    					Date startOfLastHourOfDay = c.getTime();
    					Date latestKnownHour = hist.getLastHourProcessed();
    					
    					// Note:  Sometimes the last known hour is 1 hour ahead of 
    					// the actual data we get, so this code is conservative and
    					// only declares all data to have been processed after we
    					// think the first hour of the next day has been processed.
    					boolean allDailyDataAvailable = latestKnownHour.after(startOfLastHourOfDay);
    					logDebug("startOfLastHourOfDay = " + startOfLastHourOfDay + 
    							 ", latestKnownHour = " + latestKnownHour +
    							 ", allDailyDataAvailable = " + allDailyDataAvailable);
    					
    					if(allDailyDataAvailable) {
    						logDebug("All daily data available, but no data for last hour of the day.");
    						//logDebug(getDailyResponseAsString(dailyResponse));
    						
    						reason = (totalImpressions <= dailyTarget) ? BidReason.UNDERPACING : BidReason.OVERPACING;
    					} else {
    						logResult(MessageFormat.format(MSG_DAY_NOT_DONE, campaignName));
    					}
    				}
    			} else {
    				logResult(MessageFormat.format(MSG_NO_IMPRESSIONS_FOR_DAY, campaignName, DateUtils.toDateString(day)));
    			}

    			if(reason != null) {
    				if(dailyTarget <= 0L) {
    					result = new BidSuggestion(0.0, reason);  // Bid $0.00 if negative or zero imp target.
    				} else {
    					DailyBidData dailyBids = hist.getDailyBidData(campaignId, day);
    					if((dailyBids == null) || (!dailyBids.hasAnyBidPrices())) {
    						logWarning(MessageFormat.format(MSG_NO_BID_HISTORY_FOR_DAY, campaignName, DateUtils.toDateString(day)));
    					}
    					try {
    						double aveBidPrice = getAverageDailyBidPrice(dailyBids, dailyResponse);
    						if(aveBidPrice <= ESSENTIALLY_ZERO_PRICE) {
    							// We were not bidding the previous day,
    							// but there was a positive daily impression target.
    							// For now, just go back to the max bid.
    							Double maxBid = camp.getMaximumBid();
    							logDebug("Bid price for previous day was zero.  Setting bid to max bid: " + maxBid);
    							result = new BidSuggestion(maxBid, reason);
    						} else {
    							double dailyImprEst = estimateTotalDailyImpressions(dailyResponse, dailyCap);
    							double responseRatio = dailyImprEst/dailyTarget;
    							logDebug(MessageFormat.format(MSG_ESTIMATED_TO_DESIRED_IMPS, aveBidPrice, 
    									dailyImprEst, dailyTarget, responseRatio));
    							if(isOutsideControlRegion(responseRatio)) {
    								double newBid = aveBidPrice/responseRatio;
                                    /*
                                     * Changes related to Youtrack issue DCI-248
                                     * http://youtrack.oggifinogi.com/issue/DCI-248
                                     */
                                    if(newBid < CBOConfigurator.getMinBidForAdjDailyImpressionsPolicy()){
                                        newBid = CBOConfigurator.getMinBidForAdjDailyImpressionsPolicy();
                                    }
    								result = new BidSuggestion(newBid, reason);
    							} else {
    								logResult(MessageFormat.format(MSG_ON_TARGET, campaignName, responseRatio, CONTROL_REGION));
    							}
    						}
    					} catch (BidPriceVariationException e) {
    						logResult(MessageFormat.format(MSG_BID_VARIATION_EXCEPTION, campaignName, e.toString()));
    					}
    				}
    			}
    		} else {
    			logResult(MessageFormat.format(MSG_NO_RESPONSE_DATA_FOR_DAY, campaignName, DateUtils.toDateString(day)));
    		}
    	}
    	return result;
    }
    
    /** Gets the bid reason of Underpacing or Overpacing depending on
     * how the total impressions compares to the impression target.
     */
    
    /** Gets a string representation of the daily response that
     * lists the impressions, cost, and cpm for each hour.
     * @param dr The daily response.
     * @return A detailed string representation of the daily response.
     */
    private String getDailyResponseAsString(DailyResponseData dr) {
		StringBuilder buf = new StringBuilder("DailyResponseData = ");
		if(dr == null) {
			buf.append("null");
		} else {
			buf.append(dr.toString());
			int hrs = dr.getNumberOfHoursInDay();
			buf.append("\nHour\tImps\tCPM\tcost\n");
			for(int hr = 0; hr < hrs; hr++) {
				buf.append(String.valueOf(hr));
				buf.append(":00  ");
				buf.append(String.valueOf(dr.getImpressionCount(hr)));
				buf.append('\t');
				buf.append(String.valueOf(dr.getAverageCPM(hr)));
				buf.append('\t');
				buf.append(String.valueOf(dr.getCost(hr)));
				buf.append('\n');
			}
		}
		return buf.toString();
    }
	
    /** Gets the bid price for the day averaged by the number of impressions served each hour.
     * Throws an exception if the average daily bid price was too variable.
     * Returns the maximum average hourly CPM paid if the bid price was ECP for the entire day.
     * @param dailyBids The daily bid data.
     * @param dailyResponse The response for this day.
     * @return The average bid price for the day.
     * @exception BidPriceVariationException If the bid price was too variable to work with over the day.
     */
    private double getAverageDailyBidPrice(DailyBidData dailyBids, DailyResponseData dailyResponse) throws BidPriceVariationException {
    	double aveBidPrice = 0.0;
    	if((dailyBids == null) || (!dailyBids.hasAnyBidPrices())) {
    		double maxHourlyCPMPaid = dailyResponse.getMaxHourlyCPMPaid();
    		logNote(MessageFormat.format(MSG_BIDDING_ECP, maxHourlyCPMPaid));
    		aveBidPrice = maxHourlyCPMPaid;
    	} else {
    		// Get the bid price averaged by the number of impressions.
    		// If this is too variable, give up.
    		// Assume bids take effect the hour after they are applied.

    		long totalImpressions = 0L;
    		long hourlyImpressions = 0L;
    		double p = 0.0;
    		Double bidPrice = null;
    		int firstHour = dailyResponse.getFirstHourWithImpressions();
    		int lastHour = dailyResponse.getLastHourWithImpressions();
            int hoursWithImps = dailyResponse.getNumberOfHoursWithImpressions();
    		logDebug("Computing ave daily bid price");
            if(hoursWithImps > 0 && firstHour < 0) // Sanity check
            {
                Utils.barf(MSG_INCOMPATIBLE_DAILY_RESPONSE, this, dailyResponse);
                throw new BidPriceVariationException(MSG_INCOMPATIBLE_DAILY_RESPONSE);
            }
            else if(hoursWithImps > 0) // In case there were no impressions at all.
            {
                for(int hr = firstHour; hr <= lastHour; hr++) {
                    bidPrice = getEffectiveBidPrice(hr, dailyBids, dailyResponse);
                    logDebug(hr + "\t" + dailyResponse.getImpressionCount(hr) + "\t" + bidPrice);
                    if(bidPrice != null) {
                        hourlyImpressions = dailyResponse.getImpressionCount(hr);
                        totalImpressions += hourlyImpressions;
                        p += bidPrice.doubleValue()*hourlyImpressions;
                    } else {
                        throw new BidPriceVariationException(MSG_BID_ECP_PART_OF_DAY);
                    }
                }
                aveBidPrice = p / (double)(totalImpressions);
                logDebug("p = " + p + " totalImpressions = " + totalImpressions + ", avePrice = " + aveBidPrice);
            }
            else
            {
                logDebug("p = " + p + " No impressions found!");
                throw new BidPriceVariationException(MSG_NO_IMPRESSIONS_FOUND_FOR_DAY);
            }


    		// Check the bid price distribution to see if it is too wild.
    		// A certain percentage of the impression-averaged bid prices
    		// must be within some percent of the average bid price in order
    		// to be considered a stable bid price for the day.
    		double lowPrice = (1.0 - BID_PRICE_DEVIATION)*aveBidPrice;
    		double highPrice = (1.0 + BID_PRICE_DEVIATION)*aveBidPrice;
    		long impressionsPricedNearAveBidPrice = 0L;
    		for(int hr = firstHour; hr <= lastHour; hr++) {
    			bidPrice = getEffectiveBidPrice(hr, dailyBids, dailyResponse);
    			if(bidPrice != null) {
    				p = bidPrice.doubleValue();
    				if((p >= lowPrice) && (p <= highPrice)) {
    					impressionsPricedNearAveBidPrice += dailyResponse.getImpressionCount(hr);
    				}
    			}
    		}
    		double fractionInRange = ((double)impressionsPricedNearAveBidPrice)/((double)totalImpressions);
    		logDebug(MessageFormat.format(MSG_BID_PRICE_VARIATION, fractionInRange, BID_PRICE_DEVIATION, aveBidPrice));
    		if(fractionInRange < MIN_BID_PRICE_FRACTION_WITHIN_DEVIATION) {
    			String msg = MessageFormat.format(MSG_TOO_MUCH_BID_VARIATION, 
    					                          MIN_BID_PRICE_FRACTION_WITHIN_DEVIATION, 
    					                          BID_PRICE_DEVIATION, aveBidPrice);
    			throw new BidPriceVariationException(msg);
    		}
    	}
    	return aveBidPrice;
    }
   
    /** Gets the "effective" bid price for an hour of data.
     * If we were bidding ECP then the average CPM price paid for the hour is returned.
     * @param hr The zero-based hour of the day.
     * @param dailyBids The bids as a function of hour.
     * @param dailyResponse The response vs hour.
     * @return The effective bid price for the specified hour or null if none.
     */
    private Double getEffectiveBidPrice(int hr, DailyBidData dailyBids, DailyResponseData dailyResponse) {
    	Double bidPrice = null;
    	if((dailyBids != null) && (dailyResponse != null)) {
    		bidPrice = dailyBids.getBidPrice(hr);
    		if(bidPrice == null) {
    			bidPrice = dailyResponse.getAverageCPM(hr);
    		}
    	}
		return bidPrice;
    }
    
    /** Determines if the response ratio is outside of the the "dead zone" near optimal.
     * @param responseRatio the ratio of observed impressions to target impressions.
     * @return True if the response ratio warrants a change in bid price or false if not.
     */
    private boolean isOutsideControlRegion(double responseRatio) {
		return (responseRatio > 0) && ((responseRatio < 1.0 - CONTROL_REGION) || (responseRatio > 1.0 + CONTROL_REGION));
    }
			 
    /** Estimates what the total daily impressions would have been if impressions were served
     * for all hours of the day.  If the total number of impressions is at least the daily cap,
     * then this method is being called when the campaign is over-pacing.
     * In that case it ignores the last hour of impression data because that data was 
     * probably affected by hitting the impression cap.
     * For now a straight averaging over all hours is done.  
     * This could be improved using a model of the percentage of impressions versus hour of day.
     * @param dailyResponse The daily impression response (assumed not null).
     * @return An estimate of the total daily impressions if all hours had been served impressions.
     */
    private double estimateTotalDailyImpressions(DailyResponseData dailyResponse, long dailyCap) {
    	
    	// For now this just does the dumb extrapolation based on
    	// the number of hours of data that are available.
    	// It should throw some exception if the number of hours are too small
    	// or are in a bad place to make an accurate estimate.
    	
    	// For the purposes of extrapolation, any hours between the first
    	// and last hour of the day that do not have impressions will now 
    	// be treated as if they had 0 impressions.  Days without any
    	// impressions will be still classified as having 0 hours with impressions.
    	
    	long totalImpr = dailyResponse.getTotalImpressions();
    	int hoursWithImpr = 0;
    	if(dailyResponse.getNumberOfHoursWithImpressions() > 0) {
    		hoursWithImpr =  1 + dailyResponse.getLastHourWithImpressions() - dailyResponse.getFirstHourWithImpressions();
    	}
    	int hoursInDay = dailyResponse.getNumberOfHoursInDay();
    	
    	double result = linearlyExtrapolateImpressions(totalImpr, hoursWithImpr, hoursInDay);
    	
    	// If the daily cap has been hit the last hour may not be a reliable value.
    	// However, in one case we have seen the impression count to shoot up significantly
    	// in the last hour.  Therefore, estimate the daily impression count with and
    	// without including the last hour and choose the higher of the two.
    	
    	if((totalImpr >= dailyCap) && (hoursWithImpr > 0)) {	 // May need to exclude the last hour if the cap has been hit.
    		long lastHourImpr = dailyResponse.getImpressionCount(dailyResponse.getLastHourWithImpressions());
    		if(lastHourImpr > 0L) {
    			double resultWithoutLastHour = linearlyExtrapolateImpressions(totalImpr - lastHourImpr, hoursWithImpr - 1, hoursInDay);
    			if(resultWithoutLastHour > result) {
    				result = resultWithoutLastHour;
    			}
    		}
		}
    	return result;
    }
    
    /** Linearly estimates the total number of impressions from the number observed and the number of hours.
     * Returns 0 if the number of hours with impressions is zero or if the total number
     * of impressions is zero or negative.
     * Returns the total impression count if the number of hours with impressions is
     * equal to the number of hours in the day (and they are not zero).
     * @param totalImpr The total number of impressions observed.
     * @param hoursWithImpr The number of hours with impressions.
     * @param hoursInDay The number of hours in the day.
     * @return The linearly extrapolated daily impression count.
     */
    private double linearlyExtrapolateImpressions(long totalImpr, int hoursWithImpr, int hoursInDay) {
    	double estimate = 0.0;
    	if((hoursWithImpr > 0) && (totalImpr > 0L)) {
    		estimate = (double)totalImpr;
    		if(hoursWithImpr < hoursInDay) {
    			estimate *= ((double)hoursInDay)/((double)(hoursWithImpr));
    		}
    	}
    	return estimate;
    }
    
    /** Determines if we have all data for the specified day in the time zone of that day.
     * First checks the historical data.  If it is all there then this returns true.
     * If not all data is there, we check the last hour processed
     * Because the last hour processed may be 1 hour ahead of the actual data
     * only consider all data to be available if we are 1 hour into the next date.
     * @param camp The campaign (assumed not null).
     * @param hist The historical data.
     * @param day The day (assumed not null).
     * @return True if the historical data contains values for all hours of the day.
     * @exception DataAccessException If error getting the historical data.
    private boolean hasAllDataForDay(CampaignInfo camp, HistoricalData hist, Calendar day) throws DataAccessException {
    	
    	boolean result = false;
    	
    	Calendar lastHourOfDay = DateUtils.setToStartOfHour(DateUtils.copyCalendar(day), 23);
		Date d = hist.getLatestResponseData(camp.getCampaignId());
		if(d != null) {
			result = (!d.before(lastHourOfDay.getTime()));
		}
		
		// It is possible that the last hour of the day has no data, 
		// but AppNexus has processed the data anyway.  Check this.
		// Only override the previous result if there AppNexus
		// claims to have data for the first hour of the next day
		// because sometimes AppNexus says it has processed the hour,
		// but the data is not there.
		if(result == false) {
			Date lastHourProcessed = hist.getLastHourProcessed();
			if((lastHourProcessed != null) && lastHourProcessed.after(lastHourOfDay.getTime())) {
				result = true;
			}
		}
    	return result;
    }
	*/
    
    /** Formats the difference between two times in days, hours, and mins.
     * @param later The later time (assumed not null and later than earlier).
     * @param earlier The earlier time (assumed not null).
     */
    private String formatTimeInterval(Date later, Date earlier) {
    	StringBuilder buf = new StringBuilder();
    	long secs = (later.getTime() - earlier.getTime())/ 1000L;
    	long mins = secs / 60L;
    	long hrs = mins / 60L;
    	long days = hrs/24L;
    	
    	hrs = hrs % 24L;
    	mins = mins % 60L;
    	secs = secs % 60L;
    	
    	if(days > 0L) {
    		buf.append(String.valueOf(days));
    		buf.append(" days");
    	}
    	if(hrs > 0L) {
    		if(buf.length() > 0) {
    			buf.append(", ");
    		}
    		buf.append(String.valueOf(hrs));
    		buf.append(" hours");
    	}
    	if(mins >= 0L) {
    		if(buf.length() > 0) {
    			buf.append(", ");
    		}
    		buf.append(String.valueOf(mins));
    		buf.append(" minutes");
    	}
    	return buf.toString();
    }
}

