package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.appnexus.BidSpec;
import com.tumri.mediabuying.appnexus.services.AdvertiserService;
import com.tumri.mediabuying.appnexus.services.CampaignService;
import com.tumri.mediabuying.zini.*;
import java.util.*;
import java.util.List;

public class DBValueExtractor {
    Bidder bidder;
    @SuppressWarnings("unused")
    static MethodMapper methodMapper =
            new MethodMapper
                    (CampaignData.class,
                     BidderInstruction.bidderInstructionSchema.getSlots());
    Long advertiserId;
    Long campaignId;
    QueryContext qctx;
    Agent agent;
    Map<Long, Sexpression> advertiserCampaigns =
            new HashMap<Long, Sexpression>();
    Map<Long, AdvertiserService> advertiserData =
            new HashMap<Long, AdvertiserService>();
    Map<Long, CampaignService> campaignData =
            new HashMap<Long, CampaignService>();

    AdvertiserService getAdvertiserService()
    {
        if(advertiserData.size() == 0)
        {
            Sexpression s = Bidder.fetchSelectedAdvertisers(qctx, null);
            while(s != Null.nil)
            {
                Sexpression adv = s.car();
                if(adv instanceof AdvertiserService)
                {
                    AdvertiserService as = (AdvertiserService) adv;
                    advertiserData.put(as.getId(), as);
                }
                s = s.cdr();
            }
        }
        return advertiserData.get(advertiserId);
    }

    CampaignService getCampaignService()
    {
        Sexpression campaigns = advertiserCampaigns.get(advertiserId);
        if(campaigns == null)
        {
            campaigns = bidder.getAdvertiserCampaigns
                            (advertiserId, agent, qctx,
                             bidder.selectExpiredCampaigns,
                             bidder.getCurrentTime());
            advertiserCampaigns.put(advertiserId, campaigns);
            Sexpression s = campaigns;
            while(s != Null.nil)
            {
                Sexpression camp = s.car();
                if(camp instanceof CampaignService)
                {
                    CampaignService cs = (CampaignService) camp;
                    campaignData.put(cs.getId(), cs);
                }
                s = s.cdr();
            }
        }
        return campaignData.get(campaignId);
    }

    public void setAdvertiserId(Long to)
    {
        advertiserId = to;
    }

    public void setCampaignId(Long to)
    {
        campaignId = to;
    }

    public DBValueExtractor(Bidder bidder)
    {
        this.bidder = bidder;
    }

    @SuppressWarnings("unused")
    public String getAdvertiserName()
    {
        return getAdvertiserService().getName();
    }

    @SuppressWarnings("unused")
    public void setAdvertiserName(String name)
    {
        getAdvertiserService().setName(name);
    }

    public Long getAdvertiserId()
    {
        return advertiserId;
    }

    static final SentenceTemplate getLineItemQuery =
            new SentenceTemplate(
                    "(ask-one (?id ?name)\n" +
                    "         (and (CBO_DB.HistoricalData ?advertiserId ?id ?campaignId) " +
                    "              (CBO_DB.LineItemNames ?id ?name)))");

    public Sexpression getLineItemData()
    {
        BindingList bl = BindingList.truth();
        bl.bind("?advertiserId", advertiserId);
        bl.bind("?campaignId", campaignId);
        List<Sexpression> instantiated = getLineItemQuery.instantiate(bl);
        Sexpression query = instantiated.get(0);
        return Utils.interpretACL(Integrator.INTEGRATOR, query, qctx);
    }

    @SuppressWarnings("unused")
    public String getLineItemName()
    {
        return getLineItemData().second().unboxString();
    }

    @SuppressWarnings("unused")
    public void setLineItemName(String name)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public Long getLineItemId()
    {
        return getLineItemData().car().unboxLong();
    }

    @SuppressWarnings("unused")
    public void setLineItemId(Long id)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public String getCampaignName()
    {
        return getCampaignService().getName();
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
    public Date getStartDate()
    {
        CampaignService campaign = getCampaignService();
        return AppNexusUtils.asDate
                (campaign.getStart_date(), campaign.getTimezone());
    }

    @SuppressWarnings("unused")
    public void setStartDate(Date date)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public Date getEndDate()
    {
        CampaignService campaign = getCampaignService();
        return AppNexusUtils.asDate
                (campaign.getEnd_date(), campaign.getTimezone());
    }

    @SuppressWarnings("unused")
    public void setEndDate(Date date)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public Long getLifetimeImpressionsTarget()
    {
        CampaignService campaign = getCampaignService();
        return campaign.getLifetime_budget_imps();
    }

    @SuppressWarnings("unused")
    public void setLifetimeImpressionsTarget(Long tgt)
    {
        CampaignService campaign = getCampaignService();
        campaign.setLifetime_budget_imps(tgt);
    }

    @SuppressWarnings("unused")
    public Long getLifetimeImpressionsServed()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public void setLifetimeImpressionsServed(Long imps)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public String getPacing()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public void setPacing(String p)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public Double getDailyPacing()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public void setDailyPacing(Double pacing)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public CellStyleName getDailyPacingCellStyle(QueryContext qctx)
    {
    	return CellStyleName.percentage;
    }

    @SuppressWarnings("unused")
    public Long getDailyImpressionsLimit()
    {
        CampaignService campaign = getCampaignService();
        return campaign.getDaily_budget_imps();
    }

    @SuppressWarnings("unused")
    public void setDailyImpressionsLimit(Long limit)
    {
        CampaignService campaign = getCampaignService();
        campaign.setDaily_budget_imps(limit);
    }

    @SuppressWarnings("unused")
    public Long getDailyImpressionsTarget()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public void setDailyImpressionsTarget(Long tgt)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public Long getDailyImpressionsServed()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public void setDailyImpressionsServed(Long imps)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public Long getYesterdayImpressionsServed()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public void setYesterdayImpressionsServed(Long imps)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public Double getMaxBid()
    {
        CampaignService campaign = getCampaignService();
        return campaign.getMax_bid();
    }

    @SuppressWarnings("unused")
    public void setMaxBid(Double bid)
    {
        CampaignService campaign = getCampaignService();
        campaign.setMax_bid(bid);
    }

    @SuppressWarnings("unused")
    public Double getCurrentBid()
    {
        CampaignService campaign = getCampaignService();
        return campaign.getCpm_bid_type().equals(BidSpec.BASE_BID_MODE)
                ? campaign.getBase_bid()
                : campaign.getMax_bid();
    }

    @SuppressWarnings("unused")
    public void setCurrentBid(Double bid)
    {
        CampaignService campaign = getCampaignService();
        if(campaign.getCpm_bid_type().equals(BidSpec.BASE_BID_MODE))
            campaign.setBase_bid(bid);
        else campaign.setMax_bid(bid);
    }

    @SuppressWarnings("unused")
    public Double getSuggestedBid()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public String getBidReason()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public void setBidReason(String reason)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public String getBiddingPolicy()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public void setBiddingPolicy(String policy)
    {
        throw Utils.barf("Should never call this.", this);
    }

    @SuppressWarnings("unused")
    public String getTargetingSpec()
    {
        return null;
    }

    @SuppressWarnings("unused")
    public String getYesterdayEntropy(QueryContext qctx)
    {
        return null;
    }

    @SuppressWarnings("unused")
    public CellStyleName getYesterdayEntropyStyle(QueryContext qctx)
    {
        return null;
    }
}
