package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.appnexus.Identity;
import com.tumri.mediabuying.appnexus.services.*;
import com.tumri.mediabuying.zini.*;
import java.util.*;


public class AdvertiserData {
    Long id;
    Sexpression data;
    List<CampaignData> campaignData;
    ReportNetworkAnalyticsService[] networkReportData;
    AdvertiserService service;
    Bidder bidder;
    public static AdvertiserComparator nameComparator =
            new AdvertiserComparator();


    public Long getId()
    {
        return id;
    }

    public String getName()
    {
        return service.getName();
    }

    public String prettyString()
    {
        if(service == null)
            return id.toString();
        else return service.getName() + " (" + id + ")";
    }

    public String toString()
    {
        return "["+ AppNexusUtils.afterDot(this.getClass().getName())
                + ": "
                + (id == null ? "Uninitialised" : id)
                + (service == null ? "" : ", " + service.getName()) + "]";
    }

    private void setStatus(Status theStatus)
    {
        bidder.setStatus(theStatus);
    }

    @SuppressWarnings("unused")
    public List<Long> getCampaignIds()
    {
        List<Long> res = new Vector<Long>();
        if(campaignData != null)
        {
            for(CampaignData cd: campaignData)
            {
                res.add(cd.getCampaignId());
            }
        }
        return res;
    }

    AdvertiserData(Identity ident, Bidder bidder, Long id, Sexpression data,
                   Map<String, BidderInstruction> instructions,
                   ReportNetworkAnalyticsService[] networkReportData,
                   SQLContext sctx, QueryContext qctx,
                   Map<Long, AdvertiserData> currentAdvertiserDataMap,
                   Date instructionsDate)
    {
        this.bidder = bidder;
        this.id = id;
        this.data = data;
        this.networkReportData = networkReportData;
        campaignData = new Vector<CampaignData>();
        setStatus(new Status("Creating advertiser data for " + id));
        AdvertiserData currentAdvertiserData =
                (currentAdvertiserDataMap == null
                     ? null
                     : currentAdvertiserDataMap.get(id));
        while(data != Null.nil)
        {
            campaignData.add(new CampaignData
                                (bidder, ident, this, data.car(), instructions,
                                 sctx, qctx, currentAdvertiserData,
                                 instructionsDate));
            data = data.cdr();
        }
    }

    public Object objectForId(Long id)
    {
        if(campaignData == null) return null;
        else
        {
            for(CampaignData cd: campaignData)
            {
                Object obj = cd.objectForId(id);
                if(obj != null) return obj;
            }
            return null;
        }
    }

    public CampaignData getCampaignData(Long id)
    {
        if(campaignData == null) return null;
        else
        {
            for(CampaignData cd: campaignData)
            {
                if(cd.getCampaignId().equals(id)) return cd;
            }
            return null;
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    void effectuateBids(SQLContext sctx, QueryContext qctx,
                        Date lastReportTime,
                        Map<Long, Set<Long>> readyToForceStrategy)
    {
        for(CampaignData cd: campaignData)
        {
            try
            {
                cd.effectuateBid(sctx, qctx, lastReportTime,
                                 readyToForceStrategy);
            }
            catch(Exception e)
            {
                Utils.barf(e, this, cd);
            }
        }
    }

    public Bidder getBidder()
    {
        return bidder;
    }

    public Identity getIdent()
    {
        return bidder.getAppNexusIdentity();
    }

    SQLConnector getAgent()
    {
        return bidder.getAgent();
    }

    public static final IndividualVariable _X =
            IndividualVariable.getIndVar("X");

    public static Long readHighWaterMark
            (SQLConnector connector, QueryContext qctx,
             Long advertiserId, Long campaignId)
    {
        Sexpression res = connector.runSQLQuery
                ("SELECT get_high_water_mark(" +
                        advertiserId + ", " + campaignId + ");", qctx);
        res = res.car().car();
        if(res == Null.nil) return null;
        else return res.unboxLong();
    }

    public static void recordHighWaterMark
            (SQLConnector connector, QueryContext qctx, Long advertiserId,
             Long campaignId, long highWaterMark)
    {
        connector.runSQLUpdate
                ("CALL record_high_water_mark(" +
                        advertiserId + ", " + campaignId + ", " +
                        highWaterMark + ");", qctx);
    }

    void recordObservedData
            (SQLConnector connector, SQLContext sctx,
             QueryContext qctx, Date now,
             Map<Long, String> lineItemNameMap,
             Map<Long, String> campaignNameMap)
    {
        for(CampaignData cd: campaignData)
        {
            Long highWaterMark = null;
            try
            {
                highWaterMark = readHighWaterMark
                        (connector, qctx, id, cd.getCampaignId());
                if(highWaterMark == null)
                {
                    // Then we are dirty, so we have to recompute.
                    Long cId = cd.getCampaignId();
                    Map<String, Long> map =
                        CampaignChangeClassifier.findMaxSeqNumResults
                            (connector, qctx, id, cId);
                    highWaterMark = map.get(id + "-" + cId);
                }
                highWaterMark = cd.recordObservedData
                        (sctx, qctx, now, lineItemNameMap, campaignNameMap,
                         highWaterMark);
            }
            finally
            {
                recordHighWaterMark(connector, qctx, id, cd.getCampaignId(),
                                    highWaterMark);
            }

        }

    }
}

class AdvertiserComparator implements Comparator<AdvertiserData> {
    public int compare(AdvertiserData a, AdvertiserData b)
    {
        AdvertiserService aService = a.service;
        AdvertiserService bService = b.service;
        Long aId = a.getId();
        Long bId = b.getId();
        if(aService == null)
        {
            if(bService == null)
            {
                if(aId == null)
                {
                    if(bId == null) return 0;
                    else return 1;
                }
                else if(bId == null) return -1;
                else return aId.compareTo(bId);
            }
            else return 1;
        }
        else if(bService == null) return -1;
        else
        {
            String aName = aService.getName();
            String bName = bService.getName();
            if(aName == null && bName == null)
                return aService.getId().compareTo(bService.getId());
            else return (aName == null
                          ? 1
                          : (bName == null
                                ? -1
                                : aName.compareToIgnoreCase(bName)));
        }
    }
}

