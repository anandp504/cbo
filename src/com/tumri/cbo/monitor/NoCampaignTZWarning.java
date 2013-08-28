package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.appnexus.services.CampaignService;
import com.tumri.mediabuying.zini.QueryContext;
import com.tumri.mediabuying.zini.SQLConnector;
import com.tumri.mediabuying.zini.SQLContext;
import java.util.Date;
import java.util.TimeZone;

public class NoCampaignTZWarning extends NonUrgentWarningMonitor {

    public NoCampaignTZWarning
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign(Long advertiserId, CampaignData cd,
                                 SQLConnector connector, QueryContext qctx,
                                 SQLContext sctx, Date now, TimeZone localTz)
    {
        CampaignService campaign = cd.getCampaign();
        String s = campaign.getTimezone();
        if(s == null || "".equals(s))
        {
            String str = "";
            return result(str, cd, advertiserId);
        }
        else return null;
    }

    public String heading()
    {
        return "The following have no declared timezone";
    }

    public String shortHeading()
    {
        return "No declared timezone.";
    }
}