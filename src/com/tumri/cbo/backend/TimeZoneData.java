package com.tumri.cbo.backend;

import java.util.Calendar;
import java.util.TimeZone;

public class TimeZoneData {
    private String selectedTimeZoneName;
    private TimeZone campaignTimeZone;
    private TimeZone wrtTimeZone;
    private TimeZone localTimeZone;

    public TimeZoneData(String selectedTimeZoneName, TimeZone campaignTimeZone,
                        TimeZone wrtTimeZone)
    {
        this.selectedTimeZoneName = selectedTimeZoneName;
        this.campaignTimeZone = campaignTimeZone;
        this.wrtTimeZone = wrtTimeZone;
        localTimeZone = Calendar.getInstance().getTimeZone();
    }

    @SuppressWarnings("unused")
    public String getSelectedTimeZoneName() { return selectedTimeZoneName; }
    @SuppressWarnings("unused")
    public TimeZone getCampaignTimeZone() { return campaignTimeZone; }
    public TimeZone getWrtTimeZone() { return wrtTimeZone; }
    public TimeZone getLocalTimeZone() { return localTimeZone; }

}
