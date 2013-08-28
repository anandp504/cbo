package com.tumri.cbo.backend;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.tumri.af.exceptions.DataAccessException;
import com.tumri.af.utils.DateUtils;

/** The daily impressions bid strategy with a daily impression cap set to
 * 25% above the impression target and with an adjustable daily impression limit.
 */
public class DailyImpressionsBidStrategyWithCapChange extends DailyImpressionsBidStrategy {

    private static final String NAME = "Adjustable daily impressions";

    public static final DailyImpressionsBidStrategyWithCapChange STRATEGY =
            new DailyImpressionsBidStrategyWithCapChange(NAME);

    // Definitional for this class.
    public boolean isVariableDailyImpressionBudgetStrategy() {
    	return true;
    }

    private final static Long ZERO_LONG = new Long(0);
    
    // Ratio of impression limit/cap to impression target.
    private static double IMPRESSION_CAP_TO_TARGET_RATIO = 1.25;
    
    // The fraction of the daily impression target within which there is no need to change the 
    // daily impression target.  If a newly computed daily impression target is within this
    // fraction of the old daily impression target then don't bother to change the target.
    private final static double DAILY_IMPR_TARGET_ACCURACY = 0.02;
    
    // The fraction of the daily impression target change that warrants a warning.
    private final static double DAILY_IMPR_TARGET_WARNING_THREASHOLD = 0.50;
    
    
    private final static String MSG_NO_END_DATE = 
    	"No end date specified for campaign {0}.  " +
    	"Reverting to daily impressions strategy.";
    
    private final static String MSG_NO_LIFETIME_IMPRESSIONS = 
    	"No lifetime impression target specified for campaign {0}.  " +
    	"Reverting to daily impressions strategy.";
    
    private final static String MSG_DAILY_IMPR_TARGET_UNCHANGED =
    	"The daily impressions target for campaign {0} " +
		 "is the same as its current value: {1}.  No need to change it.";
    
    private final static String MSG_DAILY_IMPR_TARGET_OK =
    	"The new daily impressions target for campaign {0}, {2}" +
		 ", is within {3,number,percent} of the current value, {1}.  No need to change it.";
		 
    private final static String MSG_DAILY_IMPR_TARGET_CHANGE = 
    	"The daily impression target for campaign {0} " +
		"should be changed from {1} to {2}.";
    
    private final static String MSG_DAILY_IMPR_TARGET_TOO_LOW = 
    	"The daily impression target of campaign {0} is far too low.  " +
    	"It needs to be increased by {3,number,percent} from {1} to {2}.";
 
    private final static String MSG_DAILY_IMPR_TARGET_TOO_HIGH = 
    	"The daily impression target of campaign {0} is far too high.  " +
    	"It needs to be reduced by {3,number,percent} from {1} to {2}.";

    private final static String MSG_DAILY_IMPR_TARGET_RESULT = 
    	"{0,number} impressions served prior to {1}.  " + 
    	"Campaign ends {2}.  {3,number,integer} hours left.  " +
    	"Lifetime impr budget = {4,number,integer}, {5,number,integer} impressions left.  " +
    	"New daily target = {6,number,integer}.";

    private final static String MSG_FUTURE_RESPONSE_DATA = 
    	"There appears to be historical data for campaign {0} " +
    	"that is in the future.  The latest historical data is at {1} " +
    	", but it is only {2}.";
    
    private final static String MSG_ADJUSTING_LAST_RESPONSE_TIME =
    	"The last non-zero impression data for campaign {0} is at {1}, " +
    	"but we should have data up to at least {2}, so we are " +
    	"using {2} as the last time for which there is data.";
    
    // ------------------------------ Constructors ----------------------
    
    protected DailyImpressionsBidStrategyWithCapChange(String name) {
        super(name);
    }
    
    // --------------------------- Public methods ----------------------
    
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
     * by the end of the last full day that data is available, the lifetime impression 
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

    	Long result = null;
    	try {
    		if((camp != null) && (hist != null)) {
    			String campaignName = camp.getCampaignName();
    			long newTarget = computeDailyImpressionTarget(camp, hist, tz, now);
    			Long dailyImprLimit = camp.getDailyImpressionLimit();

    			if(dailyImprLimit == null) {
    				logWarning(MessageFormat.format(MSG_NO_DAILY_CAP, campaignName));
    				result = new Long(newTarget);
    			} else if(dailyImprLimit.longValue() <= 0L) {
    				logWarning(MessageFormat.format(MSG_DAILY_IMPR_TARGET_NEGATIVE, campaignName, dailyImprLimit.longValue()));
    				result = new Long(newTarget);
    			} else {
    				long oldTarget = Math.round(dailyImprLimit.doubleValue()/getDailyImpressionLimitToTargetRatio());
    				double relativeChange = ((double)(newTarget-oldTarget))/((double)oldTarget);

    				// Change the target if the new target is larger than the old target,
    				// or if the new target is more than some percentage less than the old target.
    				if((newTarget > oldTarget) || (Math.abs(relativeChange) > DAILY_IMPR_TARGET_ACCURACY)) {
    					if(relativeChange > DAILY_IMPR_TARGET_WARNING_THREASHOLD) {
    						logWarning(MessageFormat.format(MSG_DAILY_IMPR_TARGET_TOO_LOW, 
    								campaignName, oldTarget, newTarget, relativeChange));
    					} else if(relativeChange < -DAILY_IMPR_TARGET_WARNING_THREASHOLD) {
    						logWarning(MessageFormat.format(MSG_DAILY_IMPR_TARGET_TOO_HIGH, 
    								campaignName, oldTarget, newTarget, -relativeChange));
    					} else {
    						logDebug(MessageFormat.format(MSG_DAILY_IMPR_TARGET_CHANGE, 
    								campaignName, oldTarget, newTarget));
    					}
    					result = new Long(newTarget);
    				} else if(newTarget == oldTarget) {
    					logDebug(MessageFormat.format(MSG_DAILY_IMPR_TARGET_UNCHANGED, 
    							campaignName, newTarget));
    				} else {
    					logDebug(MessageFormat.format(MSG_DAILY_IMPR_TARGET_OK, 
    							campaignName, oldTarget, newTarget, DAILY_IMPR_TARGET_ACCURACY));    							
    				}
    			}
    		}
    	} catch(CampaignConfigurationException cce) {
    		logWarning(cce.getMessage());
    		result = null;    // Causes the daily impressions bid strategy to apply.
    	}
    	return result;
    }
    
    // ------------------------------- Private methods ------------------------
    
    /** Computes the daily impression target required to hit the lifetime impressions target starting 
     * at the beginning of the specified day.
     * Tries to compute the total lifetime impressions from historical data because the
     * lifetime impressions served value is approximate and is to a strange time called now.
     * @param camp The campaign (assumed not null).
     * @param hist The historical data (assumed not null).
     * @param now The current time (assumed not null).
     * @param tz The time zone of the campaign (assumed not null).
     * @return The daily impression target required to hit the lifetime impression target.
     * @throws DataAccessException If error accessing the historical data
     */
    private long computeDailyImpressionTarget(CampaignInfo camp, HistoricalData hist, TimeZone tz, Date now) 
             throws DataAccessException, CampaignConfigurationException {
    	
    	long t1 = 0L;
    	long dailyTarget = 0L;
    	long impressionsServed = 0L;
    	
    	// The estimate will be best if we have all of the historical data for the campaign.
    	// If not, we can estimate it from the lifetime impressions, but we don't know for
    	// sure the time for which the lifetime impressions were computed to.
    	
    	long campaignId = camp.getCampaignId();
    	String campaignName = camp.getCampaignName();
    	Date lastHistoryTime = hist.getLastHourProcessed();
    	Date lastHourWithData = hist.getLatestResponseData(campaignId);
    	
    	// Ensure that the last response data is not in the future.
		if((lastHourWithData != null) && lastHourWithData.after(now)) {
			logError(MessageFormat.format(MSG_FUTURE_RESPONSE_DATA, campaignName, lastHourWithData, now));
			lastHourWithData = null;
		}
		
		// Get the total number of impressions up to 
		// and including the last full day for which we
		// have impressions.  
		
    	if((lastHourWithData != null) && hasAllHistoricalData(camp, hist)) {
    		logDebug("Estimating daily impr target from historical data.");
    			
    		// If the data provider claims to have processed data to a time that
    		// is more than 1 hour after the last recorded impression data,
    		// then use the data provider's last processing time minus 1 hour
    		// as the minimum time for which we must have data.
    		// This can be needed if there are no impressions for the last few hours of a day.
    		if(lastHistoryTime != null) {
    			long t = lastHistoryTime.getTime() - DateUtils.ONE_HOUR_MS;  // Assume we must have data to here.
    			if(t > lastHourWithData.getTime()) {
    				Date d = new Date(t);
    				logDebug(MessageFormat.format(MSG_ADJUSTING_LAST_RESPONSE_TIME, campaignName, lastHourWithData, d));
    				lastHourWithData = d;
    			}
    		}
    		
    		// Find the last day in the campaign's time zone for which we have all of the data.
    		// Remember that d is the start of the last hour for which we have data.
    		
    		Calendar c = Calendar.getInstance(tz);	// Use the possibly defaulted time zone.
    		c.setTime(lastHourWithData);
    		//logDebug("Start of last hour with response data is " + DateUtils.toDateTimeString(c));
    		
    		c.add(Calendar.HOUR, 1);				// The start of the first hour without data.
    		c.add(Calendar.DATE, -1);
    		DateUtils.setToEndOfDay(c);				// End of last day with all data.
    		//logDebug("End of last full day with response data is " + DateUtils.toDateTimeString(c));
    		
        	Date campaignStarts = camp.getStartDate();
    		impressionsServed = hist.getTotalImpressionsServed(campaignId, campaignStarts, c.getTime());
    		c.add(Calendar.HOUR, 1);
    		DateUtils.setToStartOfDay(c);			// Start of first day without all hours of data.
    		//logDebug("Start of first day without all hours of data = " + DateUtils.toDateTimeString(c));
    		t1 = c.getTime().getTime();
    	} else {
    		Long lifetimeImpressionsServed = camp.getLifetimeImpressionsServed();
    		if(lifetimeImpressionsServed == null) {
    			lifetimeImpressionsServed = ZERO_LONG;
    		}
    		logDebug("Estimating daily impr target from lifetime impressions served (" + 
  				     lifetimeImpressionsServed + ").  Last history time = " + lastHistoryTime);
    		impressionsServed = lifetimeImpressionsServed;
    		t1 = (lastHistoryTime.getTime() + DateUtils.ONE_HOUR_MS);
    	}
 
    	
    	Date campaignEnds = camp.getEndDate();
    	if(campaignEnds == null) {
    		throw new CampaignConfigurationException(MessageFormat.format(MSG_NO_END_DATE, campaignName));
    	}
		Long lifetimeImpressionTarget = camp.getLifetimeImpressionTarget();
		if(lifetimeImpressionTarget == null) {
			throw new CampaignConfigurationException(MessageFormat.format(MSG_NO_LIFETIME_IMPRESSIONS, campaignName));
		}
    	
    	long timeLeft = campaignEnds.getTime() - t1;
    	long impsLeft = lifetimeImpressionTarget - impressionsServed;
    	Calendar extrapolationStart = Calendar.getInstance(tz);
    	extrapolationStart.setTime(new Date(t1));
    	

    	if(impsLeft > 0) {
    		dailyTarget = Math.round(DateUtils.ONE_DAY_MS*((double)impsLeft)/((double)timeLeft) + 0.5);
    	}
    	
        Calendar ends = Calendar.getInstance(tz);
        ends.setTime(campaignEnds);
    	logDebug(MessageFormat.format(MSG_DAILY_IMPR_TARGET_RESULT, impressionsServed,
   			 	                      DateUtils.toDateTimeString(extrapolationStart), 
   			 	                      DateUtils.toDateTimeString(ends),  
   			 	                      (((double)timeLeft)/((double)DateUtils.ONE_HOUR_MS)),
   			 	                      lifetimeImpressionTarget, impsLeft, dailyTarget));
			 								  
    	return dailyTarget;
    }

	
    /** Determines if we have the historical data 
     * from the start of the campaign.
     * For now just assume we have all data.
     * @param camp The campaign (assumed not null).
     * @param hist The historical data (assumed not null).
     * @return True if we have all of the historical data or false if not or if we don't know.
     * @exception DataAccessException If error getting the historical data.
     */
    private boolean hasAllHistoricalData(CampaignInfo camp, HistoricalData hist) throws DataAccessException {
    	boolean result = true;
    	
    	/*
    	boolean result = false;
    	long campaignId = camp.getCampaignId();
    	Date startDate = camp.getStartDate();
    	Date first = hist.getEarliestResponseData(campaignId);	
    	if((startDate != null) && (first != null) && (first.getTime() <= startDate.getTime() + DateUtils.ONE_DAY_MS)) {
    		result = true;
    	}
    	
		// TODO:  Need to get the time the first impressions were served from the campaign.
    	//camp.getTimeFirstImpressionsServed();
    	// This implementation assumes data must start within 24 hours of the specified start date.
*/
    	return result;
    }   
 }

