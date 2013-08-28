package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.QueryContext;
import com.tumri.mediabuying.zini.SQLConnector;
import com.tumri.mediabuying.zini.SQLContext;
import java.util.Date;
import java.util.TimeZone;

public class CampaignEndingSoonNotification extends NonUrgentNotifyMonitor {

    public CampaignEndingSoonNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    static double coolThreshold = 0.95;
    static double hotThreshold = 1.05;
    static long soonnessThreshold = 3 * MILLISECONDS_PER_DAY;

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
            long endTime = endDate.getTime();
            long nowTime = now.getTime();
            if(endTime > nowTime && nowTime + soonnessThreshold >= endTime)
            // Campaigns ending soon, ....
            {
                Long projected = cd.getLifetimeProjectedImpressions(qctx);
                Long budget = cd.getLifetimeImpressionsTarget();
                if(projected != null && budget != null)
                {
                    double diff = (1.0d * projected) / budget;
                    if(diff >= hotThreshold)
                        return result("Will exceed the lifetime budget (" +
                                      asPercent(diff) + "%)",
                                      cd, advertiserId);
                    else if(diff <= coolThreshold)
                        return result("Will not meet the lifetime budget (" +
                                      asPercent(diff) + "%)",
                                      cd, advertiserId);
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
        return "The following are ending soon";
    }

    public String shortHeading()
    {
        return "Campaign ending soon.";
    }
}