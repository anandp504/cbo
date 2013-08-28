package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.BidderDashboardHTTPHandler;
import com.tumri.cbo.backend.CampaignData;
import com.tumri.cbo.backend.DailyImpressionsBidStrategyWithCapChange;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;

import java.text.NumberFormat;
import java.util.Date;
import java.util.TimeZone;

public class DailyImpressionTargetLargePctChangeNotification
        extends NonUrgentNotifyMonitor {

    public DailyImpressionTargetLargePctChangeNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    static final double LARGE_CHANGE_LOWER_BOUND = -0.2;
    static final double LARGE_CHANGE_UPPER_BOUND =  0.2;
    static final NumberFormat percentFormat =
            BidderDashboardHTTPHandler.makePercentageFormat();

    public AbstractProblem checkCampaign(Long advertiserId, CampaignData cd,
                                 SQLConnector connector, QueryContext qctx,
                                 SQLContext sctx, Date now, TimeZone localTz)
    {
        Date bod = AppNexusUtils.dayFloor(now);
        Date yesterdayBod = AppNexusUtils.dayFloor(now, null, 1);
        String stratName =
            DailyImpressionsBidStrategyWithCapChange.STRATEGY.getPrimaryName();
        String query =
                "SELECT t1.daily_impressions_budget,\n" +
                "       t2.daily_impressions_budget\n" +
                "FROM observeddata t1, observeddata t2\n" +
                "WHERE 1 = 1\n" +
                "AND   t1.advertiser_id = " + advertiserId + "\n" +
                "AND   t1.campaign_id =  " + cd.getCampaignId() + "\n" +
                "AND   t2.advertiser_id =  " + advertiserId + "\n" +
                "AND   t2.campaign_id =  " + cd.getCampaignId() + "\n" +
                "AND   t1.control_bid_strategy =  '" + stratName + "'\n" +
                "AND   t2.control_bid_strategy =  '" + stratName + "'\n" +
                "AND   t1.observation_day = '" +
                       connector.dateToSQL(bod) + "'\n" +
                "AND   t2.observation_day = '" +
                       connector.dateToSQL(yesterdayBod) + "'\n" +
                "AND   t1.daily_impressions_budget <>\n" +
                "      t2.daily_impressions_budget\n" +
                "LIMIT 1;";
        Sexpression res = connector.runSQLQuery(query, qctx);
        if(res != Null.nil)
        {
            long from = res.car().second().unboxLong();
            long to   = res.car().car().unboxLong();
            if(from > 0)
            {
                double frac = (1.0d * (to - from)) / (1.0 * from);
                boolean largePctChange = (frac <= LARGE_CHANGE_LOWER_BOUND ||
                                          frac >= LARGE_CHANGE_UPPER_BOUND);
                if(largePctChange)
                {
                    String str = AppNexusUtils.intToThousandsString(from)
                            + " to " + AppNexusUtils.intToThousandsString(to)
                            + " (" + percentFormat.format(frac) + ")";
                    return result(str, cd, advertiserId);
                }
                else return null;
            }
            else return null;
        }
        else return null;
    }

    public String heading()
    {
        return "The following had a large %age change to their daily " +
               "impression budget";
    }

    public String shortHeading()
    {
        return "Significantly changed daily impression budget.";
    }
}