package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.zini.*;
import java.util.Date;
import java.util.TimeZone;

public class UnderOrOverPacingWarning extends NonUrgentWarningMonitor {

    public UnderOrOverPacingWarning
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    static double overpacingThreshold = 1.2;
    static double underpacingThreshold = 0.8;
    static double underpacingAtMaxBidThreshold = 0.9;

    public AbstractProblem checkCampaign
            (Long advertiserId, CampaignData cd, SQLConnector connector,
             QueryContext qctx, SQLContext sctx, Date now, TimeZone localTz)
    {
        Long targetImpressions = cd.getLifetimeImpressionsTarget();
        Long projected = cd.getLifetimePacingInternal(qctx);
        if(projected == null || targetImpressions == null)
        {
            return null; // Can't say!
        }
        else
        {
            double frac = (1.0d * projected) / targetImpressions;
            String pct = asPercent(frac);
            String str = null;
            if(cd.getCurrentBid() == null || cd.getMaxBid() == null) {}
            else if(frac > overpacingThreshold)
                str = "Severely overpacing (" + pct + ").";
            else if(frac < underpacingAtMaxBidThreshold &&
                   cd.getCurrentBid() >= cd.getMaxBid())
                str = "Underpacing at the maximum bid (" + pct + ").";
            else if(frac < underpacingThreshold)
                str = "Severely underpacing (" + pct + ").";
            else {}
            if(str != null)
                return result(str, cd, advertiserId);
            else return null;
        }
    }

    public String heading()
    {
        return "The following are under- or over-pacing";
    }

    public String shortHeading()
    {
        return ""; // "Pacing problem.";
    }
}