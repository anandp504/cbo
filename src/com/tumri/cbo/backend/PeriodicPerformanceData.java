package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.util.*;
import java.util.List;

public class PeriodicPerformanceData {

    String advertiserName;
    Long advertiserId;
    String campaignName;
    Long campaignId;
    Long impressions;
    Long clicks;
    Double mediaCost;
    Double unoptCPM = null;
    Bidder bidder;
    QueryContext qctx;

    String estimateUnoptimisedCPMQuery()
    {
        String query;
        query = "SELECT stats_imps, stats_media_cost, sequence_number\n" +
                "FROM observeddata o1 use index (ix10)\n" +
                "WHERE o1.advertiser_id = " + advertiserId + "\n" +
                "AND   o1.campaign_id = " + campaignId + "\n" +
                "AND   o1.bidding_policy = 'clearing'\n" +
                "ORDER BY o1.advertiser_id, o1.campaign_id,\n" +
                "         o1.observation_time DESC\n" +
                "LIMIT 1000;";

        return query;
    }

    Double estimateUnoptimisedCPM()
    {
        String query = estimateUnoptimisedCPMQuery();
        Sexpression rows;
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        long imps = 0l;
        double cost = 0.0d;
        long lastSeqNum = 0L;
        rows = connector.runSQLQuery(query, qctx, true);
        for(Sexpression row: rows)
        {
            long seqNum = row.third().unboxLong();
            if(lastSeqNum == seqNum + 1)
            {
                imps = imps + row.first().unboxLong();
                cost = cost + row.second().unboxDouble();
            }
            lastSeqNum = seqNum;
        }
        if(imps > 0)
            return (cost / (imps / 1000.0d));
        else return 0.0d;
    }

    @SuppressWarnings("unused")
    public String getAdvertiserName()
    {
        return advertiserName;
    }
    @SuppressWarnings("unused")
    public void setAdvertiserName(String name)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public Long getAdvertiserId()
    {
        return advertiserId;
    }
    @SuppressWarnings("unused")
    public void setAdvertiserId(Long Id)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public String getCampaignName()
    {
        return campaignName;
    }
    @SuppressWarnings("unused")
    public void setCampaignName(String name)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public Long getCampaignId()
    {
        return campaignId;
    }
    @SuppressWarnings("unused")
    public void setCampaignId(Long Id)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public Long getImpressions()
    {
        return impressions;
    }
    @SuppressWarnings("unused")
    public void setImpressions(Long imps)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public Long getClicks()
    {
        return clicks;
    }
    @SuppressWarnings("unused")
    public void setClicks(Long cl)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public Double getMediaCost()
    {
        return mediaCost;
    }
    @SuppressWarnings("unused")
    public void setMediaCost(Double cost)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public String getOptCPMFormula()
    {
        return "=1000*{(- x 1),y}/{(- x 2),y}";
    }
    @SuppressWarnings("unused")
    public void setOptCPMFormula(String formula)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public Double getUnoptCPM()
    {
        if(unoptCPM == null)
            unoptCPM = estimateUnoptimisedCPM();
        return unoptCPM;
    }
    @SuppressWarnings("unused")
    public void setUnoptCPMFormula(Double cpm)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public String getEstUnoptCostFormula()
    {
        return "={(- x 4),y}*{(- x 1),y}/1000";
    }
    @SuppressWarnings("unused")
    public void setEstUnoptCostFormula(String formula)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public String getEstSavingsFormula()
    {
        return "=if({(- x 1),y} = 0, \"No CPM\", {(- x 1),y}-{(- x 4),y})";
    }
    @SuppressWarnings("unused")
    public void setEstSavingsFormula(String formula)
    {
        throw Utils.barf("Should never call this.", this);
    }
    @SuppressWarnings("unused")
    public String getEstPctSavingsFormula()
    {
        return "=if({(- x 2),y} = 0, \"No CPM\", {(- x 1),y}/{(- x 2),y})";
    }
    @SuppressWarnings("unused")
    public void setEstPctSavingsFormula(String formula)
    {
        throw Utils.barf("Should never call this.", this);
    }

    static String periodicPerformanceQuery
            (Date startTime, Date endTime, Long advertiserId, Long campaignId)
    {
        SynchDateFormat format = SQLConnector.grindFormat;
        String query;
        query =
        "SELECT an.name, t.advertiser_id, cam.name, t.campaign_id,\n" +
        "       Imps, Clicks, Cost\n" +
        "FROM (SELECT h.advertiser_id, h.campaign_id,\n" +
        "             SUM(imps) as Imps,\n" +
        "             SUM(clicks) as Clicks,\n" +
        "             SUM(cost) as Cost\n" +
        "      FROM (SELECT advertiser_id, campaign_id,\n" +
        "                   MAX(event_time) AS MaxECPTime\n" +
        "            FROM bidhistory\n" +
        "            WHERE bid_strategy = 'ECP'\n" +
        (advertiserId == null ? "" :
        "            AND advertiser_id = " + advertiserId +"\n") +
        (campaignId == null ? "" :
        "            AND campaign_id = " + campaignId +"\n") +
        "            GROUP BY advertiser_id, campaign_id\n" +
        "            ORDER by advertiser_id, campaign_id) bh\n" +
        "      RIGHT OUTER JOIN historicaldata h\n" +
        "      ON  bh.advertiser_id = h.advertiser_id\n" +
        "      AND bh.campaign_id = h.campaign_id\n" +
        "      WHERE 1 = 1\n" +
        "      AND (bh.MaxECPTime IS NULL OR hour > bh.MaxECPTime)\n" +
        (startTime == null ? "" :
        "      AND hour >= '" + format.format(startTime) + "'\n") +
        (endTime == null ? "" :
        "      AND hour <= '" + format.format(endTime) + "'\n") +
        (advertiserId == null ? "" :
        "      AND h.advertiser_id = " + advertiserId +"\n") +
        (campaignId == null ? "" :
        "      AND h.campaign_id = " + campaignId +"\n") +
        "      GROUP BY advertiser_id, campaign_id) t,\n" +
        "     advertisernames an, campaignnames cam\n" +
        "WHERE an.id = t.advertiser_id\n" +
        "AND   cam.id = t.campaign_id\n" +
        "ORDER BY an.name, cam.name";
        return query;
    }

    @SuppressWarnings("unused")
    static String periodicPerformanceQueryOld
            (Date startTime, Date endTime, Long advertiserId, Long campaignId)
    {
        SynchDateFormat format = SQLConnector.grindFormat;
        String query;
        query =
        "SELECT an.name, t.advertiser_id, cam.name, t.campaign_id,\n" +
        "       Imps, Clicks, Cost\n" +
        "FROM (SELECT advertiser_id, campaign_id,\n" +
        "             SUM(imps) as Imps,\n" +
        "             SUM(clicks) as Clicks,\n" +
        "             SUM(cost) as Cost\n" +
        "      FROM historicaldata\n" +
        "      WHERE 1 = 1\n" +
        (startTime == null ? "" :
        "      AND hour >= '" + format.format(startTime) + "'\n") +
        (endTime == null ? "" :
        "      AND hour <= '" + format.format(endTime) + "'\n") +
        (advertiserId == null ? "" :
        "      AND advertiser_id = " + advertiserId +"\n") +
        (campaignId == null ? "" :
        "      AND campaign_id = " + campaignId +"\n") +
        "      GROUP BY advertiser_id, campaign_id) t,\n" +
        "     advertisernames an, campaignnames cam\n" +
        "WHERE an.id = t.advertiser_id\n" +
        "AND   cam.id = t.campaign_id\n" +
        "ORDER BY an.name, cam.name";
        return query;
    }

    public static List<PeriodicPerformanceData> getPeriodicPerformanceData
            (Date startTime, Date endTime, Long advertiserId, Long campaignId,
             Bidder bidder, QueryContext qctx)
    {
        Sexpression rows;
        List<PeriodicPerformanceData> resList =
                new Vector<PeriodicPerformanceData>();
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        String query = periodicPerformanceQuery
                                (startTime, endTime, advertiserId, campaignId);
        rows = connector.runSQLQuery(query, qctx);
        for(Sexpression row: rows)
        {
            resList.add(new PeriodicPerformanceData
                            (bidder, qctx,
                             Cons.nth(0, row).unboxString(),
                             Cons.nth(1, row).unboxLong(),
                             Cons.nth(2, row).unboxString(),
                             Cons.nth(3, row).unboxLong(),
                             Cons.nth(4, row).unboxLong(),
                             Cons.nth(5, row).unboxLong(),
                             Cons.nth(6, row).unboxDouble()));
        }
        return resList;
    }

    PeriodicPerformanceData
            (Bidder bidder,
             QueryContext qctx,
             String advertiserName,
             Long advertiserId,
             String campaignName,
             Long campaignId,
             Long impressions,
             Long clicks,
             Double mediaCost)
    {
        this.bidder = bidder;
        this.qctx = qctx;
        this.advertiserName = advertiserName;
        this.advertiserId = advertiserId;
        this.campaignName = campaignName;
        this.campaignId = campaignId;
        this.impressions = impressions;
        this.clicks = clicks;
        this.mediaCost = mediaCost;
    }
}
