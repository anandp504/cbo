package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.QueryContext;
import com.tumri.mediabuying.zini.SQLConnector;
import com.tumri.mediabuying.zini.SQLContext;
import com.tumri.mediabuying.zini.Sexpression;
import com.tumri.mediabuying.zini.Null;
import java.util.Date;
import java.util.TimeZone;

public class MaxBidChangedNotification extends NonUrgentWarningMonitor {

    public MaxBidChangedNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign(Long advertiserId, CampaignData cd,
                                 SQLConnector connector, QueryContext qctx,
                                 SQLContext sctx, Date now, TimeZone localTz)
    {
        TimeZone campaignTz = cd.getTimeZone();
        Date bod = AppNexusUtils.dayFloor(now, campaignTz, 0);
        // Note: Open question is the actual time window to use for this.
        // Here, we currently start at the beginning of yesterday
        // relative to the campaign's TZ.
        Date yesterdayBod = AppNexusUtils.dayFloor(now, campaignTz, 1);
        String query =
                "SELECT t1.max_bid, t2.max_bid\n" +
                "FROM observeddata t1, observeddata t2\n" +
                "WHERE 1 = 1\n" +
                "AND   t1.advertiser_id = " + advertiserId + "\n" +
                "AND   t1.campaign_id =  " + cd.getCampaignId() + "\n" +
                "AND   t2.advertiser_id =  " + advertiserId + "\n" +
                "AND   t2.campaign_id =  " + cd.getCampaignId() + "\n" +
                "AND   t1.observation_day = '" +
                        connector.dateToSQL(bod) + "'\n" +
                "AND   t2.observation_day = '" +
                        connector.dateToSQL(yesterdayBod) + "'\n" +
                "AND   t1.max_bid <> t2.max_bid\n" +
                "LIMIT 1;";
        Sexpression res = connector.runSQLQuery(query, qctx);
        if(res != Null.nil)
        {
            String str = asTwoDecimals(res.car().second().unboxDouble())
                         + " to " +
                         asTwoDecimals(res.car().car().unboxDouble());
            return result(str, cd, advertiserId);
        }
        else return null;
    }

    public String heading()
    {
        return "The following have had a change of max bid since yesterday";
    }

    public String shortHeading()
    {
        return "Change of max bid since yesterday";
    }
}