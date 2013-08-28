package com.tumri.cbo.backend;

/** This class represents the state of the bidder.
 */
public enum BidderState {
	IDLE(false, "Idle"),
	STARTING(true, "Starting"),
	READING_SPREADSHEET(true, "Reading spreadsheet"),
	WRITING_SPREADSHEET(true, "Writing spreadsheet"),
	FETCHING_ADVERTISERS(true, "Fetching advertisers"),
	FETCHING_ADVERTISER_DATA(true, "Fetching advertiser data"),
    @SuppressWarnings("unused")
	FETCHING_CAMPAIGNS(true, "Fetching campaigns"),
    @SuppressWarnings("unused")
	FETCHING_CAMPAIGN_DATA(true, "Fetching campaign data"),
    @SuppressWarnings("unused")
	FETCHING_REPORT_DATA(true, "Fetching report data"),
	EFFECTUATING_BIDS(true, "Effectuating bids"),
	IDENTIFYING_CAMPAIGN_DIFFERENCES(true, "Identifying material campaign differences"),
	RECORDING_CAMPAIGN_DIFFERENCES(true, "Recording missing differences");

	/*
	 * "Creating campaign data"
	 * "Creating advertiser data" advertiser =
	 * "Handling bid policy change for " + campaign.getId()
	 * "Saving bid policy for " + campaign.getId()
	 * "Effectuating campaign bid for " + campaign.getId()
	 */
	private boolean m_running;
	private String m_displayName;
	
	private BidderState(boolean running, String displayName) {
		m_running = running;
		m_displayName = displayName;
	}

    @SuppressWarnings("unused")
	public boolean isRunning() {
		return m_running;
	}
	
	public String getDisplayName() {
		return m_displayName;
	}
	
}
