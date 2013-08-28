package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AppNexusUtils;

import java.util.Date;

/** This class represents an event that was logged by the bidder.
 * It can also be used as a run-time event to be dispatched to listeners.
 */
public class BidderEvent {

	public enum EventType {
		BID_TYPE_CHANGED,
		BID_PLACED,
		DAILY_CAP_CHANGED,
		TARGETING_PARAMETERS_CHANGED;
	}

	private long advertiserId;
	private long campaignId;
	private Date eventTime;
	private EventType eventType;
	private String description;

    public String toString()
    {
        return "#<" + AppNexusUtils.afterDot(getClass().getName()) + " " +
                eventTime + ">";
    }

    public String getBriefString()
    {
        return eventType + ": " + description;
    }

	public long getAdvertiserId() {
		return advertiserId;
	}
	
	public void setAdvertiserId(long advertiserId) {
		this.advertiserId = advertiserId;
	}
	
	public long getCampaignId() {
		return campaignId;
	}
	
	public void setCampaignId(long campaignId) {
		this.campaignId = campaignId;
	}
	
	public Date getEventTime() {
		return eventTime;
	}
	
	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}
	
	public EventType getEventType() {
		return eventType;
	}
	
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
}
