package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.Null;
import com.tumri.mediabuying.zini.QueryContext;
import com.tumri.mediabuying.zini.SQLConnector;
import com.tumri.mediabuying.zini.Sexpression;
import java.util.Date;


public abstract class NotifyMonitor extends AbstractMonitor {

    public NotifyMonitor(MessageReport[] applicableReports,
                         ReportApplicabilityColumn... includedColumns)
    {
        super(applicableReports, includedColumns);
    }

    public AbstractProblem result
            (String message, CampaignData cd, Object... arguments)
    {
        return recordProblem(new Notification(this, message, cd, arguments));
    }

    public AbstractProblem result
            (Messages messages, CampaignData cd, Object... arguments)
    {
        return recordProblem(new Notification(this, messages, cd, arguments));
    }

    @SuppressWarnings("unused")
    public static boolean stillServingImpressions
            (SQLConnector connector, QueryContext qctx,
             CampaignData cd, Date startTime, Date endTime)
    {
        // Served at least some impressions since the beginning of yesterday.
        String query =
        "SELECT (SELECT imps\n" +
        "        FROM historicaldata t2\n" +
        "        WHERE t1.advertiser_id = t2.advertiser_id\n" +
        "        AND   t1.line_item_id = t2.line_item_id\n" +
        "        AND   t1.campaign_id = t2.campaign_id\n" +
        "        AND   date_format(t2.hour, '%Y-%m-%d %k')\n" +
        "            = date_format(t1.observation_time, '%Y-%m-%d %k'))\n" +
        "FROM observeddata t1\n" +
        "WHERE advertiser_id = " + cd.getAdvertiserId() + "\n" +
        "AND   campaign_id = " + cd.getCampaignId() + "\n" +
        (startTime != null
            ? "AND   observation_time >= '" +
                     connector.dateToSQL(startTime) + "'\n"
            : "") +
        (endTime != null
            ? "AND   observation_time <= '" +
                     connector.dateToSQL(endTime) + "'\n"
            : "") +
        "LIMIT 1;";
        Sexpression res = connector.runSQLQuery(query, qctx);
        return res != Null.nil && res.car().car() != Null.nil &&
               res.car().car().unboxLong() != 0;
    }

    public static boolean servedImpressionsYesterday
            (CampaignData cd)
    {
        Long imps = cd.getYesterdayImpressionsServed();
        return imps != null && imps > 0;
    }

    public static boolean servedImpressionsDayBeforeYesterday
            (CampaignData cd)
    {
        Long imps = cd.getDayBeforeYesterdayImpressionsServed();
        return imps != null && imps > 0;
    }

    public static boolean isWithinDateBounds(CampaignData cd, Date now)
    {
        String liSD = cd.getLineItem().getStart_date();
        String caSD = cd.getCampaign().getStart_date();
        String tzstr = cd.getTimeZoneString();
        Date startDate = AppNexusUtils.asDate
                (AppNexusUtils.dateMin(liSD, caSD), tzstr);
        String liED = cd.getLineItem().getEnd_date();
        String caED = cd.getCampaign().getEnd_date();
        Date endDate = AppNexusUtils.asDate
                (AppNexusUtils.dateMin(liED, caED), tzstr);
        boolean res;
        res = (startDate == null || startDate.getTime() < now.getTime()) &&
              (endDate == null || endDate.getTime() > now.getTime());
        return res;
    }

}
