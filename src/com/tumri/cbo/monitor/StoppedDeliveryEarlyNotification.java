package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.zini.*;
import java.util.Date;
import java.util.TimeZone;

public class StoppedDeliveryEarlyNotification extends NonUrgentNotifyMonitor {

    public StoppedDeliveryEarlyNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign
            (Long advertiserId, CampaignData cd, SQLConnector connector,
             QueryContext qctx, SQLContext sctx, Date now, TimeZone localTz)
    {
        if(isWithinDateBounds(cd, now))
        {   // Campaign still nominally alive
            Long lifetimeServed = cd.getLifetimeImpressionsServed();
            if(lifetimeServed == null || lifetimeServed <= 0)
                return null;
            else if(servedImpressionsDayBeforeYesterday(cd))
            {
                Long lifetimeBudget = cd.getLifetimeImpressionsTarget();
                if(lifetimeBudget == null|| servedImpressionsYesterday(cd))
                    return null;
                else if(lifetimeServed >= lifetimeBudget)
                    return result("Lifetime impression budget met early.",
                                  cd, advertiserId);
                else return result("Lifetime impression budget NOT met.",
                                   cd, advertiserId);
            }
            else return null;
        }
        else return null;
    }

    public String heading()
    {
        return
            "The following have stopped delivery before the campaign end date";
    }

    public String shortHeading()
    {
        return "Stopped delivery early.";
    }
}