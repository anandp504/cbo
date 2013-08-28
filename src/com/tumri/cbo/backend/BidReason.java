package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.CellStyleName;

/** A bid reason.
 */
public enum BidReason {

	OVERPACING ("Overpacing", CellStyleName.orangeBackground),
	UNDERPACING("Underpacing", CellStyleName.greenBackground),
	UNDERPACING_AT_MAX_BID("Underpacing at max bid", CellStyleName.orangeBackground),
	DAILY_IMPR_TARGET_TOO_LOW("Daily impression target much too low", CellStyleName.redBackground),
	DAILY_IMPR_TARGET_TOO_HIGH("Daily impression target much too high", CellStyleName.redBackground),
	COASTING("Coasting", CellStyleName.greenBackground),
	NO_POLICY("No policy selected", CellStyleName.orangeBackground),
	OPTIMIZATION_DISABLED("Optimization disabled", CellStyleName.orangeBackground),
	INSUFFICIENT_DATA("Insufficient data", CellStyleName.redBackground),	
    CAMPAIGN_ENDED("Campaign has finished", CellStyleName.redBackground),
    CAMPAIGN_NOT_STARTED("Campaign has not started", CellStyleName.redBackground),
    INVALID_CAMPAIGN_DATES("Flight dates not fully specified", CellStyleName.redBackground),
    NO_STATISTICS("No statistics available", CellStyleName.redBackground),
    NO_LIFETIME_IMPRESSION_TARGET("Lifetime Impression target not specified", CellStyleName.redBackground),
    LIFETIME_IMPRESSIONS_REACHED("Lifetime impression target reached", CellStyleName.orangeBackground),
    NO_DAILY_IMPRESSION_TARGET("Daily Impression target not specified", CellStyleName.redBackground),
    NO_DAILY_IMPRESSION_LIMIT("Daily impression limit not specified", CellStyleName.redBackground);
	

	private String m_shortName;
	private CellStyleName m_styleName;
	
	private BidReason(String shortName, CellStyleName styleName) {
		m_shortName = shortName;
		m_styleName = styleName;
	}
	
	public String getShortName() {
		return m_shortName;
	}
	
	public CellStyleName getStyleName() {
		return m_styleName;
	}

}
