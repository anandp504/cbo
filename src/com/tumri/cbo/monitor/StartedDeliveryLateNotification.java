package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.zini.*;
import java.util.Date;
import java.util.TimeZone;

public class StartedDeliveryLateNotification extends NonUrgentNotifyMonitor {

    public StartedDeliveryLateNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign
            (Long advertiserId, CampaignData cd, SQLConnector connector,
             QueryContext qctx, SQLContext sctx, Date now, TimeZone localTz)
    {
        Long lifetimeServed = cd.getLifetimeImpressionsServed();
        if(lifetimeServed != null && lifetimeServed > 0)
        {
            if(isWithinDateBounds(cd, now))
            {   // Campaign still nominally alive
                if(servedImpressionsYesterday(cd))
                {
                    if(servedImpressionsDayBeforeYesterday(cd))
                        return null;
                    else return result("", cd, advertiserId);
                }
                else return null;
            }
            else return null;
        }
        else return null;
    }

    public String heading()
    {
        return
            "The following have started delivery, but after the campaign had already started";
    }

    public String shortHeading()
    {
        return "Started delivery late.";
    }
}