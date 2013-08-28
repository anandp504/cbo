package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.cbo.backend.DailyImpressionsBidStrategyWithCapChange;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;

import java.util.Date;
import java.util.TimeZone;

public class LifetimeImpressionBudgetChangedNotification
        extends NonUrgentNotifyMonitor {

    public LifetimeImpressionBudgetChangedNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign(Long advertiserId, CampaignData cd,
                                 SQLConnector connector, QueryContext qctx,
                                 SQLContext sctx, Date now, TimeZone localTz)
    {
        Date bod = AppNexusUtils.dayFloor(now);
        Date yesterdayBod = AppNexusUtils.dayFloor(now, null, 1);
        String stratName =
            DailyImpressionsBidStrategyWithCapChange.STRATEGY.getPrimaryName();
        String query =
                "SELECT t1.lifetime_impressions_budget,\n" +
                "       t2.lifetime_impressions_budget\n" +
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
                "AND   t1.lifetime_impressions_budget <>\n" +
                "      t2.lifetime_impressions_budget\n" +
                "LIMIT 1;";
        Sexpression res = connector.runSQLQuery(query, qctx);
        if(res != Null.nil)
        {
            String str = AppNexusUtils.intToThousandsString
                    (res.car().second().unboxLong())
                    + " to " +
                    AppNexusUtils.intToThousandsString
                            (res.car().car().unboxLong());
            return result(str, cd, advertiserId);
        }
        else return null;
    }

    public String heading()
    {
        return "The following had a change to their lifetime impression budget";
    }

    public String shortHeading()
    {
        return "Changed lifetime impression budget.";
    }
}