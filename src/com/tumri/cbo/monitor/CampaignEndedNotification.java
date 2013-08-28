package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.QueryContext;
import com.tumri.mediabuying.zini.SQLConnector;
import com.tumri.mediabuying.zini.SQLContext;
import java.util.Date;
import java.util.TimeZone;

public class CampaignEndedNotification extends NonUrgentNotifyMonitor {

    // 25 hours.
    static final long A_BIT_OVER_A_DAY = 25l * 3600l * 1000l;

    public CampaignEndedNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign(Long advertiserId, CampaignData cd,
                                 SQLConnector connector, QueryContext qctx,
                                 SQLContext sctx, Date now, TimeZone localTz)
    {
        String liED = cd.getLineItem().getEnd_date();
        String caED = cd.getCampaign().getEnd_date();
        Date endDate = AppNexusUtils.asDate(AppNexusUtils.dateMin(liED, caED),
                                            cd.getTimeZoneString());
        if(endDate != null)
        {
            long diff = now.getTime() - endDate.getTime();
            // Only complain if the campaign has ended, but it ended within the
            // last 25 hours.  We use 25 rather than 24 to be safe.
            if(diff > 0 && diff <= A_BIT_OVER_A_DAY)
            {
                String str = "";
                return result(str, cd, advertiserId);
            }
            else return null;
        }
        else return null;
    }

    public String heading()
    {
        return "The following have ended";
    }

    public String shortHeading()
    {
        return "Campaign ended";
    }
}