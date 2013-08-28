package com.tumri.cbo.backend;

import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** The information required from a campaign to do bid optimization.
 * The data is read-only and is used only for bid strategies.
 * It represents the fully-defaulted values that will be used
 * for the campaign even if those values are specified at a
 * higher level.
 */
public final class CampaignInfo {
	
	private long m_campaignId;
	private String m_campaignName;
	private Date m_startDate;
	private Date m_endDate;
	private TimeZone m_timeZone;
	private Double m_currentBid;
	private Double m_maxBid;
	private Currency m_currency;
	private Long m_dailyImpressionTarget;
	private Long m_dailyImpressionLimit;
	private Long m_lifetimeImpressionTarget;
	private Long m_lifetimeImpressionLimit;
	private long m_lifetimeImpressionsServed;
	
	public final static Currency USD = Currency.getInstance(Locale.US);
	
	// --------------------- Constructors --------------------
	
	/** Constructor that specifies all arguments.
	 * The values should not be changed once the constructor has been called.
	 * @param id The campaign id.
	 * @param name The campaign name.
	 * @param startDate The campaign start date.
	 * @param endDate The campaign end date.
	 * @param tz The effective time zone in which the campaign is run.
	 * @param currentBid The current bid price or null if not bidding a fixed bid.
	 * @param maxBid The maximum bid allowed on this campaign ever.
	 * @param currency The currency for all monetary amounts this campaign.
	 * @param dailyImpressionTarget The target number of impressions to serve in a day or null if unspecified.
	 * @param dailyImpressionLimit The maximum number of impressions to serve in a day or null if none.
	 * @param lifetimeImpressionTarget The target number of impressions to serve throughout the campaign or null if none.
	 * @param lifetimeImpressionLimit The maximum number of impressions to serve throughout the campaign or null if none.
	 * @param lifetimeImpressionsServed The total number of impressions served to date (estimated) or 0 if none.
	 */
	public CampaignInfo(long id, String name, Date startDate, Date endDate, TimeZone tz,
						Double currentBid, Double maxBid, Currency currency,
						Long dailyImpressionTarget, Long dailyImpressionLimit,
						Long lifetimeImpressionTarget, Long lifetimeImpressionLimit,
						long lifetimeImpressionsServed) {
		m_campaignId = id;
		m_campaignName = name;
		m_startDate = startDate;
		m_endDate = endDate;
		m_timeZone = tz;
		m_currentBid = currentBid;
		m_maxBid = maxBid;
		m_currency = currency;
		m_dailyImpressionTarget = dailyImpressionTarget;
		m_dailyImpressionLimit = dailyImpressionLimit;
		m_lifetimeImpressionTarget = lifetimeImpressionTarget;
		m_lifetimeImpressionLimit = lifetimeImpressionLimit;
		m_lifetimeImpressionsServed = lifetimeImpressionsServed;
	}
							
	// -------------------- Public methods -------------------
	
	/** Gets the id of this campaign.
	 * @return The id of this campaign.
	 */
	public long getCampaignId() {
		return m_campaignId;
	}
	
	/** Gets the display name of this campaign.
	 * @return The display name of this campaign.
	 */
	public String getCampaignName() {
		return m_campaignName;
	}

	/** Gets the date/time when this campaign is supposed to start.
	 * Dates always represent times in UTC although they may be
	 * printed in the default time zone in Date.toString().
	 * @return The date/time at which this campaign is supposed to start.
	 */
	public Date getStartDate() {
		return  m_startDate;
	}
	
	/** Gets the date/time at which this campaign is supposed to end.
	 * Dates always represent times in UTC although they may be
	 * printed in the default time zone in Date.toString().
	 * @return The date/time at which this campaign is supposed to end.
	 */
	public Date getEndDate() {
		return m_endDate;
	}
	
	/** Gets the time zone in which this campaign is operating.
	 * Returns the time zone specified at the line item level or at the advertiser level
	 * if the time zone is not explicitly set at the campaign level.
	 * @return The time zone in which this campaign is operating (never null).
	 */
	public TimeZone getTimeZone() {
		return m_timeZone;
	}
	
	/** Determines if the bid price is a fixed bid.
	 * @return True if the bid is a fixed bid or false if not.
	 */
	public boolean isFixedBid() {
		return (getCurrentBid() != null);
	}
	
	/** Gets the current bid price.
	 * @return The current bid price or null if not using a fixed bid.
	 */
	public Double getCurrentBid() {
		return m_currentBid;
	}
	
	/** Gets the maximum bid price as a CPM specified for this campaign or null if none has been specified.
	 * @return The maximum bid specified for this campaign or null if none has been specified.
	 */
	public Double getMaximumBid() {
		return m_maxBid;
	}

	/** Gets the currency in which all prices for this campaign are specified (never null).
	 * @return The currency in which all prices for this campaign are specified (never null).
	 */
	public Currency getCampaignCurrency() {
		return m_currency;
	}
	
	/** Gets the current daily impression target.
	 * @return The current daily impression target or none if there is no target.
	 */
	public Long getDailyImpressionTarget() {
		return m_dailyImpressionTarget;
	}
	
	/** Gets the current daily impression limit.
	 * @return The current daily impression limit or none if there is no limit.
	 */
	public Long getDailyImpressionLimit() {
		return m_dailyImpressionLimit;
	}
	
	/** Gets the current lifetime impression target for this campaign.
	 * @return The lifetime impression target for this campaign or null if none set.
	 */
	public Long getLifetimeImpressionTarget() {
		return m_lifetimeImpressionTarget;
	}
	
	/** Gets the current lifetime impression limit for this campaign.
	 * @return The lifetime impression limit for this campaign or null if none set.
	 */
	public Long getLifetimeImpressionLimit() {
		return m_lifetimeImpressionLimit;
	}
	
	/** Gets an approximation of the total number of impressions served to date.
	 * This comes from the AppNexus quick-stats and may not always be up to date.
	 * @return The total number of impressions served to date or 0L none served.
	 */
	public long getLifetimeImpressionsServed() {
		return m_lifetimeImpressionsServed;
	}
}
