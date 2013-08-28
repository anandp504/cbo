package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.cbo.backend.DailyImpressionsBidStrategy;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;

import java.util.Date;
import java.util.TimeZone;

public class DailyImpressionBudgetChangedNotification
        extends NonUrgentNotifyMonitor {

    public DailyImpressionBudgetChangedNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    /*
    public AbstractProblem checkCampaign(Long advertiserId, CampaignData cd,
                                 SQLConnector connector, QueryContext qctx,
                                 SQLContext sctx, Date now, TimeZone localTz)
    {
        Date yesterdayBod = AppNexusUtils.dayFloor(now, null, 1);
        Date  tomorrowBod = AppNexusUtils.dayFloor(now, null, -1);
        String stratName =
                DailyImpressionsBidStrategy.STRATEGY.getPrimaryName();
        String query =
                "SELECT daily_impressions_budget\n" +
                "FROM observeddata t1\n" +
                "WHERE t1.advertiser_id = " + advertiserId + "\n" +
                "AND   t1.campaign_id =  " + cd.getCampaignId() + "\n" +
                "AND   t1.control_bid_strategy =  '" + stratName + "'\n" +
                "AND   t1.observation_time <  '" +
                       connector.dateToSQL(tomorrowBod) + "'\n" +
                "AND   t1.observation_time >= '" +
                       connector.dateToSQL(yesterdayBod) + "'\n" +
                "ORDER BY t1.observation_time;";
        Sexpression res =
                Utils.uniquify(connector.runSQLQuery(query, qctx), false);
        if(res.cdr() != Null.nil)
        {
            String str = AppNexusUtils.intToThousandsString
                    (res.car().car().unboxLong())
                    + " to " +
                    AppNexusUtils.intToThousandsString
                            (res.second().car().unboxLong());
            return result(str, cd, advertiserId);
        }
        else return null;
    }
    */

    public AbstractProblem checkCampaign(Long advertiserId, CampaignData cd,
                                 SQLConnector connector, QueryContext qctx,
                                 SQLContext sctx, Date now, TimeZone localTz)
    {
        Date bod = AppNexusUtils.dayFloor(now);
        Date yesterdayBod = AppNexusUtils.dayFloor(now, null, 1);
        String stratName =
                DailyImpressionsBidStrategy.STRATEGY.getPrimaryName();
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
            String str = AppNexusUtils.intToThousandsString(from)
                    + " to " + AppNexusUtils.intToThousandsString(to);
            return result(str, cd, advertiserId);
        }
        else return null;
    }

    public String heading()
    {
        return "The following had a change to their daily impression budget";
    }

    public String shortHeading()
    {
        return "Changed daily impression budget.";
    }
}