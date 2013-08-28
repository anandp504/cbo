package com.tumri.cbo.monitor;

import com.tumri.af.context.TimeScale;
import com.tumri.cbo.backend.CampaignData;
import com.tumri.cbo.backend.PerformanceHistoryDAO;
import com.tumri.cbo.backend.PerformanceHistoryRow;
import com.tumri.cbo.backend.TimeScaleIterator;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ImpressionsOscillationWarning extends NonUrgentWarningMonitor {

    public ImpressionsOscillationWarning
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    static int windowWidthInDays = 4;
    static double greatlyExceedsThreshold = 10.0d;

    static boolean exceedsGreatly(long x, long y)
    {
        return y != 0l && ((1.0d * x) / (1.0d * y)) > greatlyExceedsThreshold;
    }

    static boolean exceedsGreatly(double x, double y)
    {
        return y != 0.0d && (x / y) > greatlyExceedsThreshold;
    }

    public AbstractProblem checkCampaign
            (Long advertiserId, CampaignData cd, SQLConnector connector,
             QueryContext qctx, SQLContext sctx, Date now, TimeZone localTz)
    {
        PerformanceHistoryDAO impl = cd.bidder().getPerformanceHistoryDAO();
        TimeScale ts = TimeScale.DAILY;
        TimeZone wrtTimeZone = cd.getTimeZone();
        Date startDay =
                TimeScaleIterator.timeCeiling
                  (AppNexusUtils.dayFloor(now, wrtTimeZone, windowWidthInDays),
                   ts, wrtTimeZone);
        try
        {
            List<PerformanceHistoryRow> points =
                    impl.getCampaignPerformanceHistory
                          (advertiserId, cd.getCampaignId(), ts, startDay,
                           TimeScaleIterator.timeCeiling(now, ts, wrtTimeZone),
                           wrtTimeZone);
            AbstractProblem res = null;
            if(points.size() >= windowWidthInDays + 1)
            {
                long[] imps = new long[windowWidthInDays + 1];
                for(int i = 0; i <= windowWidthInDays; i++)
                {
                    imps[i] = points.get
                            (points.size() + i - (windowWidthInDays + 1)).
                                    getImpressionsServed();
                }
                for(int i = 0; i < imps.length - 2; i++)
                {
                    if(res == null &&
                       exceedsGreatly(imps[i], imps[i + 1]) &&
                       exceedsGreatly(imps[i + 2], imps[i + 1]))
                        res = result("", cd, advertiserId);
                    else if(res == null &&
                            exceedsGreatly(imps[i + 1], imps[i]) &&
                            exceedsGreatly(imps[i + 1], imps[i + 2]))
                        res = result("", cd, advertiserId);
                    else {}
                }
                return res;
            }
            else return null;
        }
        catch (Exception e)
        {
            throw Utils.barf(e, this, advertiserId, cd, connector,
                             qctx, sctx, now);
        }
    }

    public String heading()
    {
        return "The following are showing day-by-day impressions oscillations";
    }

    public String shortHeading()
    {
        return "Day-by-day impressions oscillations.";
    }
}