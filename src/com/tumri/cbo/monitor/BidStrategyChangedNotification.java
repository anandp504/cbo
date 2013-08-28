package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;
import java.io.StringWriter;
import java.util.Date;
import java.util.TimeZone;

public class BidStrategyChangedNotification extends NonUrgentNotifyMonitor {

    public BidStrategyChangedNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign
            (Long advertiserId, CampaignData cd, SQLConnector connector,
             QueryContext qctx, SQLContext sctx, Date now, TimeZone localTz)
    {
        TimeZone campaignTz = cd.getTimeZone();
        Date yesterdayBod = AppNexusUtils.dayFloor(now, campaignTz, 1);
        String query =
        "SELECT DISTINCT control_bid_strategy\n" +
        "FROM observeddata t1\n" +
        "WHERE advertiser_id = " + cd.getAdvertiserId() + "\n" +
        "AND   campaign_id = " + cd.getCampaignId() + "\n" +
        "AND   observation_time >= '" +
                connector.dateToSQL(yesterdayBod) + "'\n" +
        "ORDER BY observation_time;\n";
        Sexpression res = connector.runSQLQuery(query, qctx);
        if(res.cdr() != Null.nil)
        {
            Messages messages;
            String text = null;
            String html = null;
            for(int i = 0; i < AbstractMonitor.MESSAGES_SIZE; i++)
            {
                boolean htmlify = i > AbstractMonitor.TEXT_INDEX;
                StringWriter sb = new StringWriter();
                boolean firstP = true;
                Sexpression l = res;
                while(l != Null.nil)
                {
                    if(firstP) firstP = false;
                    else sb.append((htmlify ? " -&gt; " : " -> "));
                    sb.append(AbstractProblem._html
                                    (l.car().car().unboxString(),
                                     htmlify));
                    l = l.cdr();
                }
                if(htmlify) html = sb.toString();
                else text = sb.toString();
            }
            messages = new Messages(text, html);
            return result(messages, cd, advertiserId);
        }
        else return null;
    }

    public String heading()
    {
        return
            "The following have had a change of bidding strategy";
    }

    public String shortHeading()
    {
        return "Bidding strategy changed.";
    }
}