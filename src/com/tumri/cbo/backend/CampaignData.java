package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.appnexus.services.*;
import com.tumri.mediabuying.zini.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.apache.poi.ss.util.CellReference;

import static com.tumri.mediabuying.zini.Sexpression.*;

@SuppressWarnings("unused")
public class CampaignData {
    static final int MAX_EXCEL_CELL_TEXT_SIZE = 32767;
    private static Logger log = Logger.getLogger(CampaignData.class);
    private final static String NO_SUGGESTION = "Continuing with current bid";
    private final static String UNKNOWN_PACING = "Unknown";
    
    public final static String ENTROPY_QUERY_EXPRESSION = "log2(sum(imps)) - sum(imps*log2(imps))/sum(imps)";
    
    public static CampaignComparator nameComparator = new CampaignComparator();
    Bidder bidder = null;
    Identity ident = null;
    private Date lastWriteDate = null;
    List<StashedUpdate> stashedUpdates = new Vector<StashedUpdate>();
    List<String> notifications = new Vector<String>();

    public List<String> getNotifications()
    {
        return notifications;
    }

    public void addNotification(String s)
    {
        notifications.add(s);
    }

    public void resetNotifications()
    {
        notifications.clear();
    }

    public String prettyString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(parent == null ? "???" : parent.prettyString());
        if(campaign == null)
            sb.append(campaignId.toString());
        else
        {
            sb.append(campaign.getName());
            sb.append(" (");
            sb.append(campaignId);
            sb.append(")");
        }
        return sb.toString();
    }

    public synchronized void stashUpdate
            (MethodMapper methodMapper, String slotName,
             QueryContext qctx, Object value)
    {
        stashedUpdates.add(new StashedUpdate(methodMapper, slotName,
                                             qctx, value));
        setLastWriteDate(new Date());
    }

    synchronized void executeUpdates(CampaignData onTarget)
    {
        for(StashedUpdate u: stashedUpdates)
        {
            u.execute(onTarget);
        }
        stashedUpdates.clear();
    }

    public void setLastWriteDate(Date d)
    {
        lastWriteDate = d;
    }

    public Date getLastWriteDate()
    {
        return lastWriteDate;
    }

    // This marker is used to denote the bid suggestion has been calculated
    // and it is actually null.  
    private final static Object NULL_BID_SUGGESTION = new Object();

    Long advertiserId;
    String advertiserName;
    Long lineItemId;
    Long lineItemProfileId;
    Long campaignId;
    Long campaignProfileId;
    Sexpression data;
    AdvertiserService advertiser;
    LineItemService lineItem;
    ProfileService lineItemProfile;
    CampaignService campaign;
    ProfileService campaignProfile;
    AdvertiserData parent;
    String biddingPolicy;
    Double maxBid;
    Map<?,?> stats;
    Long lifetimeImpressionsServed;
    Long dailyImpressionsServed;
    Long yesterdayImpressionsServed;
    Long dayBeforeYesterdayImpressionsServed;
    Long dailyImpressionsTarget = 0L;
    Long dailyImpressionsLimit = 0L;
    Double yesterdayEntropy = null;

    public Long   getAdvertiserId()      { return advertiserId; }
    public String getAdvertiserName()
    {
        if(advertiser == null)
            return advertiserName;
        else return advertiser.getName();
    }
    public Long   getLineItemId()        { return lineItem.getId(); }
    public String getLineItemName()      { return lineItem.getName(); }
    public Long   getCampaignId()        { return campaign.getId(); }
    public String getCampaignName()      { return campaign.getName(); }
    public CampaignService getCampaign() { return campaign; }
    public LineItemService getLineItem() { return lineItem; }
    public String getBiddingPolicy()
    {
        return biddingPolicy;
    }

    public BidStrategy getBiddingPolicyObj()
    {
        return BidderInstruction.getStrategy(getBiddingPolicy());
    }

    static boolean ecp_type_strategy(String strat)
    {
        return NoOptimizationBidStrategy.NO_OPTIMIZATION.equals(strat) ||
               NoOptimizationBidStrategy.NO_OPTIMIZATION_SECONDARY.equals(strat) ||
               NotSelectedBidStrategy.NOT_SELECTED.equals(strat);
    }

    public Double getCurrentOrMaxBid()
    {
        if(ecp_type_strategy(getBiddingPolicy()))
            return (campaign.getMax_bid() == null
                        ? maxBid
                        : campaign.getMax_bid());
        else return maxBid;
    }
    public void setCurrentOrMaxBid(Double bid)
    {
        if(bid == null && ecp_type_strategy(getBiddingPolicy()))
            setMaxBid(campaign.getMax_bid());
        else 
            setMaxBid(bid); // Is this the right thing to do?  I think so.
    }
    public Double getMaxBid()
    {
        return maxBid;
    }
    public String getIdPair()
    {
        return "" + getAdvertiserId() + "/" + getCampaignId();
    }
    public void setBiddingPolicy(String policy)
    {
        biddingPolicy = policy;
    }
    public void setMaxBid(Double bid)
    {
        maxBid = bid;
    }

    public String getTargetingSpec()
    {
        return getTargetingSpecPrecis(campaignProfile);
    }
    
    public Double getSuggestedBid(QueryContext qctx)
    {
    	BidSuggestion bs =
                suggestedBid(qctx, getNetworkAnalyticsReportTime(qctx));
    	return (bs == null) ? null : bs.getSuggestedBid();
    }
    public String getBidReason(QueryContext qctx)
    {
    	BidSuggestion bs =
                suggestedBid(qctx, getNetworkAnalyticsReportTime(qctx));
    	return (bs == null) ? NO_SUGGESTION : bs.getBidReasonString();
    }

    public Date getLastModified()
    {
        return AppNexusUtils.asDate
                (campaign.getLast_modified(), campaign.getTimezone());
    }
    public Date getStartDate()
    {
        return AppNexusUtils.asDate
                (campaign.getStart_date(), campaign.getTimezone());
    }

    public Date getEndDate()
    {
        return AppNexusUtils.asDate
                (campaign.getEnd_date(), campaign.getTimezone());
    }
    public Long getDailyImpressionsServed()     { return dailyImpressionsServed; }
    public Long getYesterdayImpressionsServed() { return yesterdayImpressionsServed; }
    public Long getDayBeforeYesterdayImpressionsServed()
    {
        return dayBeforeYesterdayImpressionsServed; 
    }
    public Long getLifetimeImpressionsServed()  { return lifetimeImpressionsServed; }
    public Long getLifetimeImpressionsTarget()
    {
        return campaign.getLifetime_budget_imps();
    }
    public Long    getDailyImpressionsLimit()   { return campaign.getDaily_budget_imps(); }
    public void    setDailyImpressionsLimit(Long limit) { campaign.setDaily_budget_imps(limit); }

    public Long getDailyImpressionsTarget()
    {
        Long limit = getDailyImpressionsLimit();
        if(limit == null) return null;
        else
        {
            // Get bid strategy.
            BidStrategy bs = BidderInstruction.getStrategy(biddingPolicy);
            // If bs is a DailyImpressionsBidStrategy then divide the Limit
            // by the ratio, otherwise return the Limit.
            if(bs instanceof DailyImpressionsBidStrategy)
            {
                DailyImpressionsBidStrategy dibs =
                        (DailyImpressionsBidStrategy) bs;
                Double temp =
                        limit / dibs.getDailyImpressionLimitToTargetRatio();
                return temp.longValue();
            }
            else return limit;
        }
    }

    public Double getCurrentBid()
    {
        return campaign.getCpm_bid_type().equals(BidSpec.BASE_BID_MODE)
                ? campaign.getBase_bid()
                : campaign.getMax_bid();
    }

    // This does not ever seem to be called...
    // It has been modified from the original and is untested.
    public Sexpression getEntropyCurve(Long advertiserId, Long campaignId,
                                       Date fromDay, Date toDay,
                                       QueryContext qctx)
    {
        Sexpression res;
        SQLConnector connector = bidder().ensureBidderSQLConnector();
        TimeZone tz = TimeZone.getTimeZone(Utils.UTC_TIMEZONE);
        String query =
            "SELECT day, " + ENTROPY_QUERY_EXPRESSION + "\n" +
            "  FROM network_site_domain_performance\n" +
            " WHERE advertiser_id = "+ advertiserId + "\n" +
            "   AND   campaign_id = " + campaignId + "\n" +
            (fromDay == null
                    ? ""
                    :
            "   AND   day >= '" + connector.dateToSQL
                        (AppNexusUtils.dayFloor(fromDay, tz, 0)) + "'\n") +
            (toDay == null 
                    ? ""
                    :
            "   AND   day <= '" + connector.dateToSQL
                        (AppNexusUtils.dayFloor(  toDay, tz, 0)) + "'\n") +
            "GROUP BY day\n" +
            "ORDER BY day ASC";
        res = connector.runSQLQuery(query, qctx);
        return res;
    }
    
    public Double getYesterdayEntropy(QueryContext qctx)
    {
        if(yesterdayEntropy == null)
        {
            SQLConnector connector = bidder().ensureBidderSQLConnector();
            TimeZone tz = TimeZone.getTimeZone(Utils.UTC_TIMEZONE);
            Date yesterdayStartDate =
                    AppNexusUtils.dayFloor(new Date(), tz, 1);
            String query = "SELECT " + ENTROPY_QUERY_EXPRESSION + "\n" +
                           "  FROM network_site_domain_performance\n" +
                           " WHERE advertiser_id = " + advertiserId + "\n" +
                           "   AND campaign_id = " + campaignId + "\n" +
                           "   AND day = '" + connector.dateToSQL(yesterdayStartDate) + "'";
            Sexpression res = connector.runSQLQuery(query, qctx);
            yesterdayEntropy = (res.car().car() == Null.nil
                                    ? null
                                    :res.car().car().unboxDouble());
            if((yesterdayEntropy != null) && (yesterdayEntropy < 0.0)) {	// Fix round-off errors.
            	yesterdayEntropy = 0.0;
            }
            return yesterdayEntropy;
        }
        return yesterdayEntropy;
    }

    public Sexpression getYesterdayTopNSites(QueryContext qctx, int n)
    {
            SQLConnector connector = bidder().ensureBidderSQLConnector();
            TimeZone tz = TimeZone.getTimeZone(Utils.UTC_TIMEZONE);
            Date yesterdayStartDate =
                    AppNexusUtils.dayFloor(new Date(), tz, 1);
            String query =
            "SELECT site_domain, sum(imps) AS ImpsSum\n" +
            "FROM network_site_domain_performance n\n" +
            "WHERE advertiser_id = " + advertiserId + "\n" +
            "AND campaign_id = " + campaignId + "\n" +
            "AND day >= '" + connector.dateToSQL(yesterdayStartDate) + "'\n" +
            "GROUP BY site_domain\n" +
            "ORDER BY ImpsSum DESC\n" +
            "LIMIT " + Integer.toString(n) + ";";
            Sexpression res;
            res = connector.runSQLQuery(query, qctx);
            return res;
    }

    // This could be SELECT distinct(site_domain) FROM network_site_domain_performance;
    // only we don't want to have to compute it.  I've observed 112k in this
    // table, so we could guess (say) 200k.  However, this represents some sort
    // of theoretical maximum.  If we got a high enntropy distribution over 4k
    // sites, I reckon we'd be pretty happy, so I'm picking this number
    // (10 bits) as the hoped for entropy upper bound.  This number of bits
    // will map to bright green.
    static double MAX_OBSERVED_DOMAINS_DENOMINATOR =
            Math.log(1024.0d)/ Math.log(2);

    // Bits below this count as bright red.
    static double ENTROPY_OFFSET = 1.0;

    public Object getYesterdayEntropyStyle(QueryContext qctx)
    {
        String colour;
        Double entropy = getYesterdayEntropy(qctx);
        if(entropy == null) return null;
        else
        {
            Double fraction =
               Math.max(0.0d, Math.min(1.0d, (entropy - ENTROPY_OFFSET)/
                         (MAX_OBSERVED_DOMAINS_DENOMINATOR - ENTROPY_OFFSET)));
            // We now have the fraction of the possible entropy we'd get if
            // we had a uniform distribution over the observed total number
            // of sites.
            // We now scale this over the range green -> red.
            // A fraction of 0.5 maps to white.
            Double   redPart = (fraction > 0.5d
                    ? (1.0d - fraction) * 512.0d
                    : 255.0d);
            Double greenPart = (fraction > 0.5d
                    ? 255.0d
                    : fraction * 512.0d);
            Double  bluePart = (fraction > 0.5d
                    ? (1.0d - fraction) * 512.0d
                    : fraction * 512.0d);
            int   iRedPart = Math.max(0, Math.min(255,   redPart.intValue()));
            int iGreenPart = Math.max(0, Math.min(255, greenPart.intValue()));
            int  iBluePart = Math.max(0, Math.min(255,  bluePart.intValue()));
            colour = String.format("%02x%02x%02x",
                                   iRedPart, iGreenPart, iBluePart);
            return colour;
        }
    }

    public CellStyleName getBidReasonCellStyle(QueryContext qctx)
    {
    	CellStyleName styleName = Bidder.DEFAULT_STYLE_NAME;
        BidSuggestion sb =
                suggestedBid(qctx, getNetworkAnalyticsReportTime(qctx));
        if(sb != null) {
        	styleName = sb.getStyleName();
        }
        return styleName;
    }
    public String getPacing(QueryContext qctx)
    {
    	BidSuggestion bs = ImpressionTargetBidStrategy.getInstance().suggestBid(createCampaignInfo());
    	return (bs == null) ? UNKNOWN_PACING : bs.getBidReasonString();
    }
    
    public CellStyleName getPacingCellStyle(QueryContext qctx)
    {
    	CellStyleName styleName = Bidder.DEFAULT_STYLE_NAME;
    	BidSuggestion bs = ImpressionTargetBidStrategy.getInstance().suggestBid(createCampaignInfo());
        if(bs != null) {
        	styleName = bs.getStyleName();
        }
        return styleName;
    }

    public Double getDailyPacing(QueryContext qctx)
    {
        Long dailyImpTarget = getDailyImpressionsTarget();
    	if(dailyImpTarget != null && dailyImpTarget > 0)
            return (1.0d * getYesterdayImpressionsServed()) / dailyImpTarget;
        else return 1.0d;
    }

    public CellStyleName getDailyPacingCellStyle(QueryContext qctx)
    {
    	return CellStyleName.percentage;
    }

    static final int[][] spectrum =
        { { 254,   0,   0 },
          { 254,   0,   0 },
          { 254,   0,   0 },
          { 255,   1,   1 },
          { 254,   1,   3 },
          { 254,   1,   3 },
          { 255,   2,   4 },
          { 255,   2,   4 },
          { 253,   3,   4 },
          { 254,   4,   5 },
          { 254,   6,   6 },
          { 254,   6,   6 },
          { 254,   6,   6 },
          { 253,   7,   8 },
          { 253,   8,   7 },
          { 254,   9,   8 },
          { 254,   8,   9 },
          { 254,  10,  10 },
          { 254,  12,  11 },
          { 254,  12,  11 },
          { 255,  13,  12 },
          { 253,  13,  12 },
          { 254,  14,  15 },
          { 255,  15,  16 },
          { 254,  16,  16 },
          { 254,  16,  16 },
          { 255,  17,  17 },
          { 253,  17,  17 },
          { 254,  18,  18 },
          { 254,  20,  19 },
          { 254,  20,  19 },
          { 255,  21,  20 },
          { 254,  22,  20 },
          { 254,  22,  20 },
          { 254,  22,  22 },
          { 253,  23,  23 },
          { 254,  24,  24 },
          { 254,  26,  25 },
          { 254,  26,  27 },
          { 252,  26,  27 },
          { 253,  27,  28 },
          { 253,  27,  28 },
          { 253,  29,  29 },
          { 253,  29,  29 },
          { 254,  30,  30 },
          { 253,  31,  30 },
          { 253,  31,  30 },
          { 254,  31,  32 },
          { 254,  31,  32 },
          { 253,  33,  33 },
          { 253,  35,  34 },
          { 253,  35,  34 },
          { 254,  36,  35 },
          { 252,  37,  35 },
          { 252,  37,  35 },
          { 253,  38,  36 },
          { 253,  39,  37 },
          { 253,  39,  37 },
          { 254,  40,  40 },
          { 253,  41,  40 },
          { 252,  42,  41 },
          { 252,  42,  41 },
          { 251,  43,  41 },
          { 252,  44,  42 },
          { 251,  46,  43 },
          { 252,  47,  44 },
          { 252,  46,  46 },
          { 251,  47,  46 },
          { 252,  48,  47 },
          { 252,  48,  47 },
          { 252,  50,  48 },
          { 252,  53,  50 },
          { 252,  53,  50 },
          { 251,  53,  50 },
          { 251,  53,  50 },
          { 251,  53,  52 },
          { 249,  54,  52 },
          { 249,  55,  53 },
          { 250,  56,  54 },
          { 250,  56,  54 },
          { 250,  56,  54 },
          { 250,  56,  54 },
          { 251,  59,  56 },
          { 250,  61,  57 },
          { 250,  61,  59 },
          { 249,  61,  59 },
          { 250,  62,  60 },
          { 248,  63,  60 },
          { 249,  64,  62 },
          { 249,  64,  61 },
          { 249,  66,  62 },
          { 249,  67,  63 },
          { 250,  68,  64 },
          { 248,  69,  64 },
          { 248,  69,  65 },
          { 248,  69,  65 },
          { 248,  70,  66 },
          { 249,  71,  67 },
          { 248,  73,  68 },
          { 248,  73,  68 },
          { 247,  74,  68 },
          { 247,  76,  69 },
          { 248,  76,  72 },
          { 248,  76,  72 },
          { 248,  76,  72 },
          { 248,  79,  74 },
          { 247,  80,  74 },
          { 247,  80,  74 },
          { 247,  79,  76 },
          { 246,  81,  77 },
          { 246,  83,  78 },
          { 246,  83,  78 },
          { 246,  83,  76 },
          { 247,  84,  79 },
          { 246,  84,  79 },
          { 245,  86,  80 },
          { 245,  88,  81 },
          { 245,  88,  81 },
          { 245,  88,  81 },
          { 243,  89,  81 },
          { 243,  90,  82 },
          { 243,  90,  82 },
          { 243,  90,  82 },
          { 243,  92,  85 },
          { 242,  93,  86 },
          { 242,  93,  86 },
          { 242,  95,  87 },
          { 242,  95,  87 },
          { 243,  96,  88 },
          { 241,  97,  88 },
          { 241,  97,  88 },
          { 241,  99,  89 },
          { 241,  99,  89 },
          { 241, 100,  90 },
          { 240, 102,  91 },
          { 241, 103,  92 },
          { 241, 103,  92 },
          { 240, 103,  93 },
          { 239, 105,  94 },
          { 239, 105,  94 },
          { 239, 107,  95 },
          { 239, 107,  95 },
          { 239, 109,  96 },
          { 237, 109,  96 },
          { 237, 109,  96 },
          { 237, 109,  96 },
          { 238, 110,  97 },
          { 237, 111,  97 },
          { 236, 113,  98 },
          { 236, 113,  98 },
          { 236, 112, 100 },
          { 236, 114, 101 },
          { 235, 115, 101 },
          { 235, 115, 101 },
          { 234, 116, 102 },
          { 234, 118, 103 },
          { 234, 118, 103 },
          { 234, 118, 103 },
          { 233, 120, 104 },
          { 233, 122, 105 },
          { 233, 122, 105 },
          { 233, 122, 105 },
          { 233, 123, 106 },
          { 233, 123, 106 },
          { 231, 124, 106 },
          { 231, 126, 107 },
          { 231, 126, 107 },
          { 231, 126, 107 },
          { 230, 127, 108 },
          { 229, 128, 108 },
          { 229, 128, 108 },
          { 230, 129, 109 },
          { 229, 130, 109 },
          { 228, 132, 110 },
          { 228, 132, 110 },
          { 227, 132, 110 },
          { 228, 133, 111 },
          { 228, 133, 111 },
          { 226, 134, 111 },
          { 226, 136, 112 },
          { 225, 137, 115 },
          { 225, 137, 115 },
          { 224, 136, 114 },
          { 224, 138, 115 },
          { 223, 139, 115 },
          { 223, 139, 115 },
          { 222, 141, 114 },
          { 222, 140, 116 },
          { 221, 141, 116 },
          { 222, 142, 117 },
          { 220, 143, 117 },
          { 221, 144, 116 },
          { 220, 144, 118 },
          { 220, 144, 118 },
          { 220, 144, 118 },
          { 220, 146, 119 },
          { 219, 148, 118 },
          { 218, 149, 118 },
          { 218, 149, 120 },
          { 216, 149, 120 },
          { 216, 149, 120 },
          { 215, 150, 120 },
          { 215, 150, 120 },
          { 215, 152, 121 },
          { 215, 152, 119 },
          { 214, 154, 120 },
          { 213, 154, 120 },
          { 213, 154, 122 },
          { 213, 154, 122 },
          { 212, 156, 121 },
          { 211, 157, 121 },
          { 211, 159, 122 },
          { 211, 158, 124 },
          { 210, 157, 123 },
          { 209, 159, 124 },
          { 209, 159, 122 },
          { 207, 162, 123 },
          { 209, 161, 123 },
          { 207, 162, 123 },
          { 206, 162, 123 },
          { 206, 162, 123 },
          { 207, 163, 124 },
          { 206, 164, 124 },
          { 204, 165, 124 },
          { 203, 166, 124 },
          { 203, 166, 124 },
          { 203, 166, 124 },
          { 202, 167, 125 },
          { 202, 167, 125 },
          { 202, 169, 126 },
          { 202, 169, 126 },
          { 200, 169, 125 },
          { 199, 170, 126 },
          { 199, 172, 127 },
          { 199, 172, 125 },
          { 199, 172, 125 },
          { 197, 173, 125 },
          { 197, 173, 125 },
          { 196, 174, 125 },
          { 195, 175, 125 },
          { 195, 175, 125 },
          { 196, 176, 126 },
          { 194, 176, 126 },
          { 193, 177, 126 },
          { 191, 178, 126 },
          { 191, 178, 126 },
          { 191, 178, 126 },
          { 190, 178, 126 },
          { 190, 181, 126 },
          { 190, 181, 126 },
          { 190, 181, 126 },
          { 189, 182, 127 },
          { 188, 181, 126 },
          { 188, 183, 127 },
          { 188, 183, 127 },
          { 185, 183, 126 },
          { 185, 184, 127 },
          { 184, 185, 127 },
          { 184, 185, 125 },
          { 184, 185, 125 },
          { 184, 185, 125 },
          { 181, 187, 125 },
          { 182, 188, 126 },
          { 182, 188, 126 },
          { 179, 188, 125 },
          { 180, 189, 126 },
          { 178, 188, 125 },
          { 177, 189, 125 },
          { 178, 190, 126 },
          { 176, 191, 124 },
          { 175, 192, 124 },
          { 175, 192, 124 },
          { 175, 192, 124 },
          { 173, 193, 124 },
          { 173, 193, 124 },
          { 171, 194, 124 },
          { 171, 194, 124 },
          { 171, 194, 124 },
          { 172, 195, 123 },
          { 170, 196, 123 },
          { 168, 196, 122 },
          { 169, 197, 123 },
          { 166, 196, 122 },
          { 166, 197, 121 },
          { 166, 198, 122 },
          { 166, 198, 122 },
          { 165, 199, 122 },
          { 165, 199, 122 },
          { 163, 200, 120 },
          { 162, 201, 120 },
          { 162, 201, 120 },
          { 160, 202, 120 },
          { 160, 202, 120 },
          { 159, 202, 120 },
          { 158, 201, 119 },
          { 158, 203, 120 },
          { 156, 204, 120 },
          { 156, 204, 120 },
          { 156, 204, 118 },
          { 154, 204, 117 },
          { 155, 205, 118 },
          { 153, 206, 118 },
          { 152, 206, 118 },
          { 152, 206, 118 },
          { 151, 207, 116 },
          { 151, 207, 116 },
          { 149, 208, 116 },
          { 149, 208, 116 },
          { 148, 209, 116 },
          { 148, 209, 116 },
          { 145, 209, 113 },
          { 145, 211, 114 },
          { 145, 211, 114 },
          { 144, 211, 114 },
          { 143, 210, 113 },
          { 141, 211, 113 },
          { 141, 211, 112 },
          { 140, 212, 112 },
          { 140, 212, 112 },
          { 140, 212, 112 },
          { 139, 214, 111 },
          { 137, 214, 110 },
          { 137, 214, 110 },
          { 137, 214, 110 },
          { 136, 215, 110 },
          { 134, 215, 110 },
          { 133, 216, 108 },
          { 133, 216, 108 },
          { 133, 216, 108 },
          { 131, 217, 108 },
          { 129, 217, 107 },
          { 129, 217, 107 },
          { 129, 217, 105 },
          { 128, 218, 105 },
          { 127, 220, 105 },
          { 126, 219, 104 },
          { 126, 219, 104 },
          { 125, 220, 104 },
          { 125, 220, 104 },
          { 121, 220, 103 },
          { 122, 221, 102 },
          { 122, 221, 102 },
          { 122, 221, 102 },
          { 120, 221, 101 },
          { 118, 222,  99 },
          { 119, 223, 100 },
          { 117, 223,  99 },
          { 117, 223,  99 },
          { 115, 224,  97 },
          { 114, 225,  97 },
          { 114, 225,  97 },
          { 113, 225,  97 },
          { 112, 224,  96 },
          { 112, 224,  96 },
          { 112, 225,  94 },
          { 110, 225,  94 },
          { 109, 226,  94 },
          { 109, 226,  94 },
          { 106, 226,  92 },
          { 106, 226,  92 },
          { 106, 226,  92 },
          { 105, 227,  92 },
          { 105, 227,  92 },
          { 103, 228,  90 },
          { 103, 228,  90 },
          { 102, 229,  90 },
          { 101, 228,  87 },
          { 100, 229,  87 },
          {  98, 229,  87 },
          {  98, 229,  87 },
          {  98, 230,  85 },
          {  97, 230,  85 },
          {  94, 230,  84 },
          {  94, 230,  84 },
          {  93, 229,  83 },
          {  93, 231,  83 },
          {  93, 231,  83 },
          {  91, 231,  82 },
          {  92, 232,  83 },
          {  89, 232,  80 },
          {  89, 232,  80 },
          {  88, 233,  80 },
          {  88, 233,  78 },
          {  88, 233,  78 },
          {  85, 232,  79 },
          {  85, 235,  78 },
          {  85, 235,  76 },
          {  84, 234,  77 },
          {  83, 235,  75 },
          {  81, 236,  74 },
          {  81, 236,  74 },
          {  81, 235,  75 },
          {  80, 235,  73 },
          {  79, 235,  73 },
          {  76, 235,  70 },
          {  77, 236,  71 },
          {  77, 236,  71 },
          {  75, 236,  70 },
          {  74, 237,  68 },
          {  74, 237,  68 },
          {  74, 237,  68 },
          {  73, 236,  67 },
          {  71, 237,  67 },
          {  70, 238,  65 },
          {  70, 238,  65 },
          {  70, 238,  65 },
          {  67, 238,  62 },
          {  67, 240,  63 },
          {  66, 239,  62 },
          {  66, 239,  62 },
          {  66, 239,  61 },
          {  64, 239,  60 },
          {  62, 239,  60 },
          {  62, 239,  60 },
          {  62, 240,  58 },
          {  62, 240,  58 },
          {  60, 240,  57 },
          {  61, 241,  56 },
          {  58, 241,  55 },
          {  58, 241,  55 },
          {  57, 241,  55 },
          {  56, 241,  52 },
          {  55, 241,  52 },
          {  55, 241,  52 },
          {  53, 242,  52 },
          {  53, 242,  51 },
          {  52, 241,  50 },
          {  51, 242,  50 },
          {  51, 242,  50 },
          {  49, 243,  48 },
          {  49, 243,  48 },
          {  47, 243,  47 },
          {  48, 242,  47 },
          {  47, 243,  47 },
          {  46, 244,  45 },
          {  46, 244,  45 },
          {  46, 244,  45 },
          {  45, 243,  44 },
          {  43, 244,  42 },
          {  42, 245,  41 },
          {  42, 245,  41 },
          {  42, 245,  41 },
          {  42, 245,  41 },
          {  39, 245,  38 },
          {  39, 245,  38 },
          {  37, 245,  39 },
          {  38, 246,  38 },
          {  38, 246,  38 },
          {  36, 246,  35 },
          {  36, 246,  35 },
          {  34, 247,  35 },
          {  34, 247,  35 },
          {  34, 247,  33 },
          {  33, 248,  33 },
          {  32, 247,  32 },
          {  32, 247,  32 },
          {  32, 247,  30 },
          {  29, 247,  29 },
          {  29, 248,  30 },
          {  29, 248,  30 },
          {  28, 248,  28 },
          {  28, 248,  28 },
          {  27, 249,  28 },
          {  26, 248,  25 },
          {  26, 248,  27 },
          {  24, 249,  25 },
          {  24, 249,  25 },
          {  23, 249,  25 },
          {  22, 249,  22 },
          {  22, 249,  22 },
          {  22, 249,  22 },
          {  20, 250,  20 },
          {  20, 250,  20 },
          {  19, 250,  20 },
          {  19, 250,  20 },
          {  18, 249,  19 },
          {  18, 249,  19 },
          {  18, 250,  18 },
          {  17, 250,  18 },
          {  16, 249,  17 },
          {  17, 251,  16 },
          {  14, 251,  15 },
          {  14, 251,  13 },
          {  14, 251,  13 },
          {  13, 252,  13 },
          {  12, 250,  14 },
          {  12, 251,  12 },
          {  12, 251,  12 },
          {  10, 251,  12 },
          {  10, 252,  10 },
          {  10, 252,  10 },
          {   9, 252,  10 },
          {   9, 252,  10 },
          {   8, 251,   9 },
          {   8, 252,   7 },
          {   7, 252,   7 },
          {   7, 252,   7 },
          {   7, 253,   6 },
          {   5, 253,   6 },
          {   4, 252,   5 },
          {   4, 252,   5 },
          {   4, 252,   5 },
          {   3, 253,   5 },
          {   3, 253,   5 },
          {   2, 253,   2 },
          {   2, 253,   2 },
          {   2, 253,   2 },
          {   1, 254,   3 },
          {   1, 255,   1 },
          {   0, 254,   0 },
          {   0, 254,   0 },
          {   0, 254,   0 },
          {   0, 254,   0 },
          {   0, 254,   0 },
          {   0, 254,   0 },
          {   0, 254,   0 },
          {   1, 255,   1 },
          {   1, 254,   3 },
          {   2, 253,   2 },
          {   2, 253,   2 },
          {   2, 253,   2 },
          {   3, 253,   5 },
          {   3, 253,   5 },
          {   4, 252,   5 },
          {   5, 253,   6 },
          {   4, 252,   5 },
          {   6, 252,   5 },
          {   7, 252,   7 },
          {   7, 253,   6 },
          {   7, 252,   7 },
          {   8, 252,   7 },
          {   8, 252,   7 },
          {   9, 253,   8 },
          {   9, 253,   8 },
          {  10, 252,  10 },
          {  10, 251,  12 },
          {  12, 251,  12 },
          {  12, 251,  12 },
          {  12, 251,  12 },
          {  12, 251,  12 },
          {  12, 250,  14 },
          {  13, 251,  15 },
          {  13, 251,  15 },
          {  14, 251,  15 },
          {  15, 251,  18 },
          {  17, 250,  18 },
          {  17, 250,  19 },
          {  17, 250,  19 },
          {  17, 250,  19 },
          {  17, 250,  19 },
          {  17, 248,  18 },
          {  18, 249,  21 },
          {  18, 249,  21 },
          {  19, 248,  21 },
          {  20, 249,  22 },
          {  20, 249,  24 },
          {  20, 249,  24 },
          {  22, 248,  24 },
          {  23, 247,  26 },
          {  23, 249,  28 },
          {  24, 248,  28 },
          {  24, 248,  28 },
          {  24, 248,  28 },
          {  27, 248,  29 },
          {  26, 247,  30 },
          {  26, 247,  30 },
          {  28, 247,  31 },
          {  28, 247,  31 },
          {  28, 247,  33 },
          {  30, 247,  34 },
          {  29, 246,  33 },
          {  30, 247,  36 },
          {  30, 247,  36 },
          {  31, 245,  37 },
          {  32, 246,  38 },
          {  33, 245,  39 },
          {  33, 245,  39 },
          {  33, 245,  39 },
          {  33, 245,  39 },
          {  34, 246,  40 },
          {  36, 245,  42 },
          {  36, 245,  42 },
          {  37, 244,  42 },
          {  37, 243,  44 },
          {  37, 243,  44 },
          {  38, 244,  45 },
          {  38, 242,  46 },
          {  39, 243,  47 },
          {  39, 243,  47 },
          {  41, 242,  48 },
          {  41, 242,  48 },
          {  42, 241,  50 },
          {  42, 241,  50 },
          {  43, 242,  51 },
          {  43, 242,  53 },
          {  45, 241,  55 },
          {  45, 241,  55 },
          {  45, 241,  55 },
          {  47, 241,  56 },
          {  46, 240,  56 },
          {  46, 240,  56 },
          {  47, 241,  57 },
          {  48, 240,  59 },
          {  48, 240,  59 },
          {  50, 239,  59 },
          {  50, 239,  61 },
          {  50, 239,  61 },
          {  51, 240,  62 },
          {  52, 239,  64 },
          {  52, 238,  66 },
          {  52, 238,  66 },
          {  54, 237,  67 },
          {  54, 237,  67 },
          {  55, 237,  67 },
          {  56, 238,  68 },
          {  56, 237,  70 },
          {  55, 236,  69 },
          {  57, 236,  72 },
          {  57, 236,  72 },
          {  57, 236,  72 },
          {  57, 236,  74 },
          {  59, 235,  74 },
          {  60, 234,  75 },
          {  61, 235,  76 },
          {  61, 235,  76 },
          {  60, 234,  77 },
          {  63, 234,  78 },
          {  63, 234,  80 },
          {  64, 233,  80 },
          {  64, 233,  80 },
          {  63, 232,  81 },
          {  65, 232,  82 },
          {  65, 232,  82 },
          {  66, 233,  84 },
          {  67, 230,  85 },
          {  67, 230,  85 },
          {  68, 231,  86 },
          {  69, 230,  88 },
          {  69, 230,  90 },
          {  69, 230,  88 },
          {  69, 230,  90 },
          {  71, 229,  90 },
          {  71, 229,  92 },
          {  71, 229,  92 },
          {  72, 228,  93 },
          {  72, 228,  93 },
          {  73, 229,  96 },
          {  73, 229,  96 },
          {  74, 228,  96 },
          {  74, 228,  98 },
          {  75, 226,  97 },
          {  75, 225,  99 },
          {  76, 226, 100 },
          {  76, 224, 100 },
          {  77, 225, 101 },
          {  77, 225, 103 },
          {  79, 224, 103 },
          {  79, 224, 103 },
          {  80, 223, 105 },
          {  80, 223, 107 },
          {  80, 223, 107 },
          {  80, 223, 107 },
          {  81, 222, 109 },
          {  81, 222, 110 },
          {  82, 223, 111 },
          {  82, 223, 111 },
          {  83, 221, 110 },
          {  83, 220, 114 },
          {  84, 219, 114 },
          {  85, 220, 115 },
          {  85, 220, 115 },
          {  84, 219, 116 },
          {  87, 219, 117 },
          {  86, 218, 117 },
          {  86, 218, 117 },
          {  86, 218, 119 },
          {  87, 217, 119 },
          {  88, 216, 121 },
          {  88, 216, 123 },
          {  88, 216, 121 },
          {  90, 215, 123 },
          {  90, 215, 124 },
          {  90, 215, 124 },
          {  90, 214, 126 },
          {  90, 214, 126 },
          {  91, 213, 128 },
          {  91, 213, 128 },
          {  93, 212, 130 },
          {  93, 212, 130 },
          {  93, 212, 132 },
          {  93, 212, 132 },
          {  93, 211, 133 },
          {  93, 209, 134 },
          {  94, 210, 135 },
          {  95, 210, 133 },
          {  95, 209, 137 },
          {  95, 209, 137 },
          {  95, 209, 137 },
          {  97, 208, 139 },
          {  97, 208, 139 },
          {  97, 208, 140 },
          {  98, 207, 142 },
          {  98, 207, 142 },
          {  98, 207, 142 },
          {  97, 205, 143 },
          {  99, 205, 143 },
          {  99, 204, 145 },
          {  99, 204, 145 },
          {  99, 204, 147 },
          { 100, 203, 147 },
          {  99, 202, 147 },
          { 101, 201, 147 },
          {  99, 202, 149 },
          { 101, 201, 151 },
          { 101, 201, 151 },
          { 102, 200, 151 },
          { 102, 200, 151 },
          { 102, 199, 154 },
          { 101, 198, 153 },
          { 103, 198, 156 },
          { 102, 197, 155 },
          { 102, 197, 155 },
          { 104, 196, 157 },
          { 104, 196, 159 },
          { 104, 196, 159 },
          { 104, 196, 159 },
          { 104, 195, 160 },
          { 104, 193, 161 },
          { 105, 194, 162 },
          { 105, 194, 162 },
          { 104, 193, 163 },
          { 106, 192, 165 },
          { 106, 192, 165 },
          { 106, 192, 165 },
          { 105, 191, 164 },
          { 106, 190, 166 },
          { 106, 190, 167 },
          { 106, 189, 169 },
          { 105, 188, 170 },
          { 107, 188, 171 },
          { 106, 187, 170 },
          { 106, 187, 172 },
          { 108, 186, 172 },
          { 108, 186, 172 },
          { 107, 185, 172 },
          { 108, 185, 175 },
          { 108, 183, 176 },
          { 108, 183, 176 },
          { 108, 183, 176 },
          { 108, 183, 176 },
          { 107, 182, 177 },
          { 109, 181, 178 },
          { 109, 181, 178 },
          { 109, 181, 180 },
          { 109, 181, 180 },
          { 108, 179, 181 },
          { 108, 179, 181 },
          { 109, 179, 181 },
          { 109, 178, 183 },
          { 110, 177, 185 },
          { 109, 176, 185 },
          { 108, 175, 184 },
          { 108, 175, 184 },
          { 109, 175, 187 },
          { 108, 174, 186 },
          { 110, 173, 188 },
          { 109, 172, 187 },
          { 109, 172, 189 },
          { 109, 172, 189 },
          { 109, 172, 190 },
          { 109, 172, 190 },
          { 109, 170, 191 },
          { 109, 170, 191 },
          { 108, 168, 192 },
          { 108, 168, 192 },
          { 108, 168, 192 },
          { 110, 167, 194 },
          { 109, 166, 195 },
          { 109, 166, 195 },
          { 109, 166, 196 },
          { 109, 166, 196 },
          { 109, 163, 197 },
          { 109, 163, 197 },
          { 108, 162, 198 },
          { 108, 162, 198 },
          { 107, 161, 199 },
          { 108, 161, 201 },
          { 108, 160, 200 },
          { 108, 159, 202 },
          { 108, 159, 202 },
          { 107, 158, 203 },
          { 108, 156, 202 },
          { 108, 156, 204 },
          { 108, 156, 204 },
          { 108, 156, 205 },
          { 107, 155, 204 },
          { 107, 154, 206 },
          { 107, 152, 207 },
          { 107, 152, 207 },
          { 107, 152, 207 },
          { 106, 151, 208 },
          { 107, 152, 209 },
          { 106, 151, 208 },
          { 106, 151, 210 },
          { 107, 148, 210 },
          { 106, 147, 209 },
          { 106, 147, 211 },
          { 106, 147, 211 },
          { 105, 146, 212 },
          { 105, 146, 212 },
          { 105, 145, 214 },
          { 106, 145, 214 },
          { 105, 144, 213 },
          { 105, 143, 214 },
          { 104, 142, 215 },
          { 103, 141, 216 },
          { 104, 140, 214 },
          { 104, 140, 216 },
          { 102, 139, 217 },
          { 103, 139, 217 },
          { 103, 138, 219 },
          { 103, 138, 219 },
          { 102, 137, 219 },
          { 101, 136, 218 },
          { 103, 135, 220 },
          { 103, 135, 220 },
          { 102, 134, 219 },
          { 102, 134, 221 },
          { 101, 132, 222 },
          { 101, 132, 222 },
          { 100, 131, 222 },
          { 100, 131, 222 },
          { 100, 131, 224 },
          {  99, 130, 223 },
          {  99, 130, 223 },
          {  98, 128, 224 },
          {  99, 127, 224 },
          {  98, 126, 225 },
          {  97, 125, 225 },
          {  97, 125, 225 },
          {  97, 125, 225 },
          {  96, 123, 226 },
          {  97, 121, 227 },
          {  97, 121, 227 },
          {  97, 121, 227 },
          {  96, 120, 228 },
          {  95, 119, 229 },
          {  95, 119, 229 },
          {  95, 119, 229 },
          {  94, 117, 229 },
          {  94, 117, 231 },
          {  93, 116, 230 },
          {  92, 115, 231 },
          {  93, 116, 232 },
          {  92, 114, 233 },
          {  91, 113, 232 },
          {  91, 111, 232 },
          {  92, 112, 233 },
          {  91, 111, 232 },
          {  90, 110, 233 },
          {  89, 109, 234 },
          {  89, 109, 234 },
          {  89, 108, 236 },
          {  88, 107, 235 },
          {  88, 107, 235 },
          {  87, 106, 235 },
          {  87, 106, 235 },
          {  87, 106, 235 },
          {  86, 105, 236 },
          {  85, 103, 237 },
          {  84, 102, 236 },
          {  84, 102, 236 },
          {  84, 100, 237 },
          {  84, 100, 239 },
          {  83,  99, 238 },
          {  83,  99, 238 },
          {  83,  99, 238 },
          {  82,  97, 238 },
          {  81,  96, 239 },
          {  80,  95, 240 },
          {  80,  95, 240 },
          {  82,  94, 240 },
          {  81,  93, 241 },
          {  79,  93, 241 },
          {  79,  93, 241 },
          {  78,  92, 241 },
          {  77,  91, 240 },
          {  76,  90, 241 },
          {  75,  88, 242 },
          {  76,  89, 243 },
          {  75,  88, 242 },
          {  76,  86, 243 },
          {  75,  85, 242 },
          {  75,  85, 244 },
          {  75,  85, 244 },
          {  74,  84, 243 },
          {  73,  83, 243 },
          {  73,  82, 245 },
          {  73,  82, 245 },
          {  72,  81, 244 },
          {  71,  80, 243 },
          {  70,  79, 246 },
          {  69,  78, 245 },
          {  69,  78, 245 },
          {  69,  78, 245 },
          {  68,  76, 245 },
          {  67,  75, 246 },
          {  66,  74, 245 },
          {  66,  74, 245 },
          {  66,  74, 247 },
          {  65,  73, 246 },
          {  65,  73, 246 },
          {  64,  71, 247 },
          {  64,  71, 247 },
          {  63,  70, 247 },
          {  62,  69, 246 },
          {  61,  68, 245 },
          {  61,  68, 247 },
          {  60,  66, 248 },
          {  60,  66, 248 },
          {  59,  65, 249 },
          {  57,  66, 249 },
          {  58,  64, 248 },
          {  58,  64, 248 },
          {  57,  63, 249 },
          {  57,  63, 249 },
          {  57,  60, 249 },
          {  55,  60, 248 },
          {  54,  59, 249 },
          {  53,  58, 248 },
          {  53,  58, 248 },
          {  54,  57, 250 },
          {  52,  56, 249 },
          {  52,  56, 251 },
          {  49,  56, 250 },
          {  49,  55, 251 },
          {  50,  54, 250 },
          {  49,  53, 249 },
          {  49,  52, 251 },
          {  48,  51, 250 },
          {  47,  50, 249 },
          {  47,  50, 251 },
          {  46,  49, 250 },
          {  45,  48, 251 },
          {  45,  48, 251 },
          {  44,  47, 250 },
          {  44,  46, 252 },
          {  43,  45, 251 },
          {  43,  45, 251 },
          {  43,  45, 252 },
          {  42,  44, 251 },
          {  41,  43, 250 },
          {  41,  43, 252 },
          {  41,  43, 252 },
          {  41,  42, 254 },
          {  40,  41, 253 },
          {  39,  40, 252 },
          {  38,  39, 253 },
          {  38,  39, 253 },
          {  37,  38, 252 },
          {  36,  37, 252 },
          {  36,  37, 252 },
          {  36,  37, 252 },
          {  33,  36, 251 },
          {  32,  35, 252 },
          {  31,  34, 253 },
          {  31,  34, 253 },
          {  31,  34, 253 },
          {  32,  32, 254 },
          {  31,  31, 253 },
          {  28,  30, 253 },
          {  27,  29, 252 },
          {  27,  29, 252 },
          {  27,  29, 252 },
          {  25,  27, 252 },
          {  25,  27, 252 },
          {  25,  27, 252 },
          {  24,  25, 253 },
          {  24,  25, 253 },
          {  23,  24, 254 },
          {  25,  23, 254 },
          {  22,  23, 253 },
          {  21,  22, 252 },
          {  21,  22, 254 },
          {  20,  21, 253 },
          {  20,  21, 253 },
          {  20,  21, 253 },
          {  19,  19, 253 },
          {  17,  19, 252 },
          {  17,  19, 254 },
          {  16,  18, 253 },
          {  16,  18, 253 },
          {  16,  17, 255 },
          {  15,  16, 254 },
          {  15,  16, 254 },
          {  14,  15, 255 },
          {  13,  14, 254 },
          {  13,  12, 253 },
          {  13,  14, 254 },
          {  12,  13, 253 },
          {  12,  13, 254 },
          {  11,  12, 253 },
          {  10,  10, 254 },
          {  10,  10, 254 },
          {  10,  10, 254 },
          {   9,   9, 253 },
          {   8,   8, 254 },
          {   7,   7, 253 },
          {   8,   8, 254 },
          {   7,   7, 255 },
          {   6,   6, 254 },
          {   6,   6, 254 },
          {   5,   5, 253 },
          {   5,   4, 255 },
          {   5,   4, 255 },
          {   3,   2, 253 },
          {   4,   3, 255 },
          {   4,   3, 254 },
          {   3,   2, 254 },
          {   2,   1, 255 },
          {   1,   1, 255 },
          {   1,   2, 253 },
          {   0,   0, 254 },
          {   0,   0, 255 } };

    public String get3ColourPercentageColourStyle
            (Double value, Double allLowValue, Double allHighValue,
             boolean lowIsRed)
    {
        String colour;
        if(value == null) return null;
        else
        {
            Double fraction =
               Math.max(0.0d, Math.min(1.0d, (value - allLowValue)/
                         (allHighValue - allLowValue)));
            int index = (int)(Math.max(0, Math.min(spectrum.length - 1,
                                                   Math.round(spectrum.length *
                                                              fraction))));
            if(!lowIsRed)
                index = (spectrum.length - 1) - index;
            int[] rgb = spectrum[index];
            colour = String.format("%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
            return colour;
        }
    }

    public String get2ColourPercentageColourStyle
            (Double value, Double allLowValue, Double allHighValue,
             boolean  lowIsRed, boolean  lowIsGreen, boolean  lowIsBlue,
             boolean highIsRed, boolean highIsGreen, boolean highIsBlue)
    {
        String colour;
        if(value == null) return null;
        else
        {
            Double fraction =
               Math.max(0.0d, Math.min(1.0d, (value - allLowValue)/
                         (allHighValue - allLowValue)));
            Double  lowPart = (fraction > 0.5d
                    ? (1.0d - fraction) * 512.0d
                    : 255.0d);
            Double highPart = (fraction > 0.5d
                    ? 255.0d
                    : fraction * 512.0d);
            Double unusedPart = (fraction > 0.5d
                    ? (1.0d - fraction) * 512.0d
                    : fraction * 512.0d);
            int   iHighPart = Math.max(0, Math.min(255,  highPart.intValue()));
            int iUnusedPart = Math.max(0, Math.min(255,unusedPart.intValue()));
            int    iLowPart = Math.max(0, Math.min(255,   lowPart.intValue()));
            colour = String.format("%02x%02x%02x",
                    (lowIsRed   ? iLowPart
                                : (highIsRed   ? iHighPart : iUnusedPart)),
                    (lowIsGreen ? iLowPart
                                : (highIsGreen ? iHighPart : iUnusedPart)),
                    (lowIsBlue  ? iLowPart
                                : (highIsBlue  ? iHighPart : iUnusedPart)));
            /*
            System.out.println("get2ColourPercentageColourStyle: " + value +
                    ", Colour: " + colour +
                    ", Fraction: " + fraction +
                    ", LowPart: " + lowPart +
                    ", HighPart: " + highPart +
                    ", iLowPart: " + iLowPart +
                    ", iHighPart: " + iHighPart);
                    */
            return colour;
        }
    }

    public Object getLifetimePacingCellStyleWithColour(QueryContext qctx)
    {
        // Underpacing is cool, overpacing is not.
    	String colourString =
                get3ColourPercentageColourStyle
                    (getLifetimePacingPercentage(qctx), 0.5d, 1.5d, false);
        /*
                get2ColourPercentageColourStyle
                    (getLifetimePacingPercentage(qctx), 0.5d, 1.5d,
                     false, false,  true,
                      true, false, false);
                      */
        List<Object> res = new Vector<Object>();
        res.add(colourString);
        res.add(CellStyleName.percentage);
        return res;
    }

    public Object getLifetimePacingLookbackCellStyle(QueryContext qctx)
    {
        return CellStyleName.numberWithCommas;
    }

    public Object getDailyPacingCellStyleWithColour(QueryContext qctx)
    {
    	String colourString =
                // Underpacing is cool, overpacing is not.
                get3ColourPercentageColourStyle
                    (getDailyPacing(qctx), 0.5d, 1.5d, false);
        /*
                get2ColourPercentageColourStyle
                    (getDailyPacing(qctx), 0.5d, 1.5d,
                     false, false,  true,
                      true, false, false);
                      */
        List<Object> res = new Vector<Object>();
        res.add(colourString);
        res.add(CellStyleName.percentage);
        return res;
    }

    public String getCanonicalName()
    {
        return AppNexusCampaignSplitter.getCanonicalName(campaign);
    }

    public int getSplitIndex()
    {
        return AppNexusCampaignSplitter.getSplitIndex(campaign);
    }

    static String[] boringSuffices = new String[] { "_targets" };
    static String shortenKey(String key)
    {
        for(String b: boringSuffices)
        {
            if(key.endsWith(b))
                return key.substring(0, key.length() - b.length());
        }
        return key;
    }

    public ProfileService getCampaignProfile()
    {
    	return campaignProfile;
    }

    public ProfileService getLineItemProfile()
    {
    	return lineItemProfile;
    }

    static final String NetworkAnalyticsReportTime =
            "NetworkAnalyticsReportTime";

    public static Date getNetworkAnalyticsReportTime(QueryContext qctx)
    {
        return getNetworkAnalyticsReportTime(qctx, false);
    }

    public static Date getNetworkAnalyticsReportTime
            (QueryContext qctx, boolean missingOk)
    {
        Object date = qctx.simpleCacheGet(NetworkAnalyticsReportTime);
        if(date instanceof Date) return (Date) date;
        else if(missingOk) return null;
        else throw Utils.barf
                ("Failed to find the NetworkAnalyticsReportTime", null);
    }

    public static Date recordNetworkAnalyticsReportTime
            (QueryContext qctx, Identity ident)
    {
        Date date = AppNexusInterface.getNetworkAnalyticsReportTime(ident);
        qctx.simpleCachePut(NetworkAnalyticsReportTime, date);
        return date;
    }

    static String[] boringProfileKeys =
            new String[]
                    {
                            "id", "last_modified", "max_day_imps", "code",
                            "description", "is_template", "max_session_imps",
                            "min_minutes_per_imp", "min_session_imps",
                            "require_cookie_for_freq_cap", "session_freq_type"
                    };

    static boolean interestingKeyP(String key)
    {
        for(String k: boringProfileKeys)
        {
            if(k.equals(key)) return false;
        }
        return true;
    }

    static String[][] arrayOfObjSlots =
            new String[][]
                    {
                            { "segment_targets", "id" },
                            { "domain_list_targets", "id" },
                            { "inv_class_targets", "inv_class" },
                            { "country_targets", "country" }
                    };


    public Object objectForId(Long id)
    {
        if(campaign == null) return null;
        else if(campaign.getId().equals(id)) return this;
        else if(campaignProfile == null) return null;
        else if(campaignProfile.getId().equals(id)) return campaignProfile;
        else if(lineItem == null) return null;
        else if(lineItem.getId().equals(id)) return lineItem;
        else if(lineItemProfile == null) return null;
        else if(lineItemProfile.getId().equals(id)) return lineItemProfile;
        else return null;
    }

    static String simplifyProfileValue(String key, Object v)
    {
        for(String[] pair: arrayOfObjSlots)
        {
            if(key.equals(pair[0]))
            {
                if(v instanceof JSONArray)
                {
                    StringBuffer sb = new StringBuffer();
                    JSONArray a = (JSONArray)v;
                    boolean firstp = true;
                    for(Object o: (JSONArray)v)
                    {
                        if(firstp) firstp = false;
                        else sb.append(", ");
                        if(o instanceof JSONObject)
                            sb.append(((JSONObject)o).get(pair[1]));
                        else sb.append(o.toString());
                    }
                    return sb.toString();
                }
                return v.toString();
            }
        }
        return v.toString();
    }

    String getTargetingSpecPrecis(ProfileService profile)
    {
        if(profile != null)
        {
            JSONObject obj = profile.serviceToJSONUnwrapped();
            StringBuffer res = new StringBuffer();
            boolean firstp = true;
            for(Object keyO: obj.keySet())
            {
                if(keyO instanceof String)
                {
                    String key = (String) keyO;
                    Object value = obj.get(key);
                    if(value != null && interestingKeyP(key))
                    {
                        if(firstp) firstp = false;
                        else res.append(", ");
                        res.append(shortenKey(key));
                        res.append(":");
                        res.append(simplifyProfileValue(key, value));
                    }
                }
            }
            return res.toString();
        }
        else return "";
    }

    static List<Object> makeHistDataKey(Identity ident, Date start, Date end,
                                        Long advertiserId, Long lineItemId,
                                        Long campaignId)
    {
        List<Object> key = new Vector<Object>();
        key.add("HistDataFor:");
        key.add(ident);
        key.add(start);
        key.add(end);
        key.add(advertiserId);
        key.add(lineItemId);
        key.add(campaignId);
        return key;
    }

    public HistoricalDataRow getLatestHistoricalData(QueryContext qctx)
    {
        Map<Date, HistoricalDataRow> curve = getHistoricalData(qctx);
        Date latest = null;
        long latestTime = -1;
        HistoricalDataRow latestRow = null;
        for(Date key: curve.keySet())
        {
            HistoricalDataRow row = curve.get(key);
            long thisTime = key.getTime();
            if(latest == null || latestTime < thisTime)
            {
                latest = key;
                latestTime = thisTime;
                latestRow = row;
            }
        }
        return latestRow;
    }

    @SuppressWarnings("unchecked")
    public Map<Date, HistoricalDataRow> getHistoricalData(QueryContext qctx)
    {
        Date start = getStartDate();
        Date end = getEndDate();
        Date now = bidder().getCurrentTime();
        List<Object> key = makeHistDataKey(ident, start, end, advertiserId,
                                           lineItemId, campaignId);
        Object histData = qctx.simpleCacheGet(key);
        if(histData != null)
            return (Map<Date, HistoricalDataRow>) histData;
        else
        {
            Map<Date, HistoricalDataRow> res =
                    bidder().getHistoricalDataFor
                            (ident, start, end, advertiserId,
                             lineItemId, campaignId, qctx);
            qctx.simpleCachePut(key, res);
            // Gets too big these days!
            // if(bidder().getDebugMode())
            //     InspectHTTPHandler.noteInterestingObject
            //          ("Bidder context data", qctx.debugGetSimpleCacheTable());
            return res;
        }
    }

    static final String BID_SUGGESTION = "BID_SUGGESTION";

    BidSuggestion suggestedBid(QueryContext qctx, Date lastReportTime)
    {
        List<Object> key = new Vector<Object>();
        key.add(BID_SUGGESTION);
        key.add(this);
        Object res = qctx.simpleCacheGet(key);
        if(res == null)		// No cached result.
        {
            res = suggestedBidInternal(qctx, lastReportTime);
            if(res == null) {
            	qctx.simpleCachePut(key, NULL_BID_SUGGESTION);
            } else {
            	qctx.simpleCachePut(key, res);
            }
        } else if (res == NULL_BID_SUGGESTION) {
        	res = null;
        }
        return (BidSuggestion)res;
    }

    /* Estimates the number of impressions expected to
     * be delivered by the end of the campaign by using the
     * average hourly impression rate since the "pacing_start_time"
     * and extrapolating the number of impressions that should be
     * delivered if it continues to deliver impressions at that
     * average hourly rate through the end of the campaign.
     * This technique does not account for different hourly
     * rates throughout the day.
     * <p>
     * In principle, it would be best to group the impressions
     * by day and extrapolate from the average daily rate not
     * the average hourly rate.  However, these two are the same
     * if the total number of hours is large.
     * Therefore, the pacing data on the dashboard will be
     * more accurate if the number of hours of data it used
     * is larger.
     * <p>
     * The "pacing_start_time is the maximum of:                                                        
     *  a) The first hour that the campaign had historical data.                    
     *  b) The last time we observed a material difference.                         
     *  c) 30 days ago.                                                             
	 * <p>
	 * This is implemented as a very complex query with subqueries.
	 * <p>
	 * The first inner-most query 
	 * (which starts with "SELECT hd.advertiser_id...")
	 * gets the first hour of data within the last 30 days.
	 * This query uses a new index historicaldata.ix1
	 * which needs to be created by the update script from
	 * database version 10 to 11.
	 * <p>
	 * The second inner-most query
	 * (which starts with SELECT od.advertiser_id...)
	 * gets the time of the last material difference
	 * within the last 30 days.
	 * <p>
     * The maximum time (t) is selected from the union
     * of these two queries to determine the pacing start time
     * for each advertiser and campaign.
     * <p>
     * Finally the number of hours and total impressions since
     * the pacing start time are selected and returned.
     */
    public static Sexpression getPacingData(Bidder bidder, QueryContext qctx)
    {
        Sexpression res;
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        TimeZone tz = TimeZone.getTimeZone(Utils.UTC_TIMEZONE);
        String query =
            "SELECT T2.advertiser_id, T2.campaign_id,\n" +
            "       COUNT(*) AS Hours,\n" +
            "       T1.pacing_start_time AS Since,\n" +
            "       MAX(T2.hour) AS EndHour,\n" +
            "       SUM(T2.imps) AS TotalImps\n" +
            "FROM (" +
            "      SELECT advertiser_id, line_item_id, campaign_id,\n" +
            "             max(t) as pacing_start_time\n" +
            "        FROM (\n" +
            "              SELECT hd.advertiser_id as advertiser_id,\n" +
            "                     hd.line_item_id as line_item_id,\n" +
            "                     hd.campaign_id as campaign_id,\n" + 
            "                     min(hd.hour) as t\n" +
            "                FROM historicaldata hd\n" +
            "                WHERE hd.hour > date_sub(now(), INTERVAL 30 DAY)\n" +
            "              GROUP BY hd.advertiser_id, hd.campaign_id\n" +
            "              UNION\n" +
            "              SELECT od.advertiser_id as advertiser_id,\n" + 
            "                     od.line_item_id as line_item_id,\n" +
            "                     od.campaign_id as campaign_id,\n" +
            "                     max(od.observation_time) as t\n" +
            "                FROM observeddata od\n" +
            "               WHERE od.observation_time > date_sub(now(), INTERVAL 30 DAY)\n" +
            "                 AND od.materially_different = true\n" +
            "                 AND od.has_material_differences = true\n" +
            "            GROUP BY od.advertiser_id, od.campaign_id\n" +
            "      ) as tmp\n" + 
            "      GROUP BY advertiser_id, line_item_id, campaign_id) as T1,\n"+
            "     historicaldata T2\n" +
            "WHERE T1.advertiser_id = T2.advertiser_id\n" +
            "AND   T1.line_item_id = T2.line_item_id\n" +
            "AND   T1.campaign_id = T2.campaign_id\n" +
            "AND   T2.hour >= T1.pacing_start_time\n" +
            "GROUP BY T2.advertiser_id, T2.campaign_id\n" +
            "ORDER BY T2.advertiser_id, T2.campaign_id";
        res = connector.runSQLQuery(query, qctx);
        return res;
    }

    static final String PACING_DATA = "PacingData";

    @SuppressWarnings("unchecked")
    public static Map<Long, Map<Long, PacingData>> ensurePacingData
            (Bidder bidder, QueryContext qctx)
    {
        Map<Long, Map<Long, PacingData>> data =
                (Map<Long, Map<Long, PacingData>>)
                        qctx.simpleCacheGet(PACING_DATA);
        if(data == null)
        {
            Sexpression sexp = getPacingData(bidder, qctx);
            data = new HashMap<Long, Map<Long, PacingData>>();
            while(sexp != Null.nil)
            {
                Sexpression row = sexp.car();
                Long advertiserId = row.car().unboxLong();
                Long campaignId = row.second().unboxLong();
                PacingData pd = new PacingData(row);
                Map<Long, PacingData> campaigns = data.get(advertiserId);
                if(campaigns == null)
                {
                    campaigns = new HashMap<Long, PacingData>();
                    data.put(advertiserId, campaigns);
                }
                campaigns.put(campaignId, pd);
                sexp = sexp.cdr();
            }
            qctx.simpleCachePut(PACING_DATA, data) ;
        }
        return data;
    }

    static Bidder getGlobalBidder()
    {
        return Bidder.getInstance();
    }

    public Long getLifetimePacingInternal
            (QueryContext qctx, Long advertiserId, Long campaignId)
    {
        Map<Long, Map<Long, PacingData>> pacingData =
                ensurePacingData(getGlobalBidder(), qctx);
        Map<Long, PacingData> campaigns = pacingData.get(advertiserId);
        if(campaigns == null)
            return null;
        else
        {
            PacingData pd = campaigns.get(campaignId);
            if(pd == null)
                return null;
            else return pd.getLifetimePacing(this);
        }
    }

    public Long getLifetimePacingInternal(QueryContext qctx)
    {
        return getLifetimePacingInternal(qctx, advertiserId, campaignId);
    }

    public Long getLifetimeProjectedImpressions(QueryContext qctx)
    {
        Long projected;
        projected = getLifetimePacingInternal(qctx);
        return projected;
    }

    public Double getLifetimePacingPercentage(QueryContext qctx)
    {
        Long targetImpressions = getLifetimeImpressionsTarget();
        Long projected = getLifetimePacingInternal(qctx);
        if(projected == null || targetImpressions == null)
            return null;
        else return (1.0d * projected) / targetImpressions;
    }

    public Long getLifetimePacingLookback(QueryContext qctx)
    {
        Map<Long, Map<Long, PacingData>> pacingData =
                ensurePacingData(getGlobalBidder(), qctx);
        Map<Long, PacingData> campaigns = pacingData.get(advertiserId);
        if(campaigns == null)
            return null;
        else
        {
            PacingData pd = campaigns.get(campaignId);
            if(pd == null)
                return null;
            else return pd.getHours();
        }
    }

    static final String DEFAULT_TIMEZONE = "EST5EDT";

    public String getTimeZoneString()
    {
        String appNexusTimeZone = campaign.getTimezone();
        if(appNexusTimeZone == null)
            appNexusTimeZone = lineItem.getTimezone();
        if(appNexusTimeZone == null)
            appNexusTimeZone = advertiser.getTimezone();
        if(appNexusTimeZone == null)
            appNexusTimeZone = DEFAULT_TIMEZONE;
        return appNexusTimeZone;
    }

    public TimeZone getTimeZone()
    {
        return TimeZone.getTimeZone(getTimeZoneString());
    }

    BidSuggestion suggestedBidInternal(QueryContext qctx, Date lastReportTime)
    {
        Date start = getStartDate();
        Date end = getEndDate();
        Date now = bidder().getCurrentTime();
        Long lifetimeImpressionsTarget = getLifetimeImpressionsTarget();
        Long dailyImpressionsTarget = campaign.getDaily_budget_imps();
        Map<Date, HistoricalDataRow> histData = null;
        BidHistoryRow[] bidHistory = null;
        //------------------
        if(bidder().getDebugMode())
        {
            // Do these up here in debug mode before all of the sanity
            // checks so as to force promiscuous prefetching of report data.
            histData = parent.getBidder().getHistoricalDataFor
                            (parent.getIdent(), start, end, advertiserId,
                             lineItemId, campaignId, qctx);
            bidHistory = getBidHistory(qctx);
        }
        //------------------
        // No stats, so no basis on which to suggest.
        if(end != null && end.getTime() < now.getTime())
            return new BidSuggestion(0.0, BidReason.CAMPAIGN_ENDED);
        else if(start != null && start.getTime() > now.getTime())
            return new BidSuggestion(0.0, BidReason.CAMPAIGN_NOT_STARTED);
        else if(start == null || end == null)
            return new BidSuggestion(getCurrentBid(), BidReason.INVALID_CAMPAIGN_DATES);
        else if(lifetimeImpressionsServed == null ||
                dailyImpressionsServed == null ||
                lifetimeImpressionsServed.equals(0L))
            return new BidSuggestion(getCurrentBid(), BidReason.NO_STATISTICS);
        else
        {
            if((lifetimeImpressionsTarget != null) && (lifetimeImpressionsServed >= lifetimeImpressionsTarget))
                return new BidSuggestion(0.0, BidReason.LIFETIME_IMPRESSIONS_REACHED);
            else
            {
                if(histData == null)
                    histData = parent.getBidder().getHistoricalDataFor
                                (parent.getIdent(), start, end, advertiserId,
                                 lineItemId, campaignId, qctx);
                if(bidHistory == null) bidHistory = getBidHistory(qctx);
                BidStrategy bs = BidderInstruction.getStrategy(biddingPolicy);
                
                CampaignInfo camp = createCampaignInfo(start, end);
                HistoricalData hist = 
                	    new InMemoryCampaignHistory(getCampaignId(), histData, bidHistory, lastReportTime); 
                BidSuggestion suggested = null;
            	try { 		
            		suggested = bs.suggestBid(camp, hist);
            	} catch(Exception e) {
            		Utils.barf("Exception caught while suggesting bid:", this,
                               e, qctx, lastReportTime, start, end, now,
                               lifetimeImpressionsTarget,
                               dailyImpressionsTarget, histData, bidHistory,
                               bs, camp, hist, suggested);
            	}
                 
                if((maxBid != null) && (suggested != null)) {     // Never exceed the max bid!
                	Double bid = suggested.getSuggestedBid();
                	if((bid != null) && (bid > maxBid))
                		suggested = new BidSuggestion(maxBid, BidReason.UNDERPACING_AT_MAX_BID);
                }
                return suggested;
            }
        }
    }
    
    private CampaignInfo createCampaignInfo() {
    	return createCampaignInfo(getStartDate(), getEndDate());
    }
    
    private CampaignInfo createCampaignInfo(Date start, Date end) {
    	return new CampaignInfo(getCampaignId(), getCampaignName(), start, end, getTimeZone(),
    			getCurrentBid(), getMaxBid(), Currency.getInstance(Locale.US),
    			getDailyImpressionsTarget(), getDailyImpressionsTarget(),
    			getLifetimeImpressionsTarget(), getLifetimeImpressionsTarget(),
    			getLifetimeImpressionsServed());
    }
    

    SQLConnector getAgent()
    {
        return parent.getAgent();
    }

    public String toString()
    {
        return "["+ AppNexusUtils.afterDot(this.getClass().getName()) + ": "
                + (advertiser == null ? "?" :advertiser.getId())
                + "/"
                + (lineItem == null ? "?" : lineItem.getId())
                + "/"
                + (campaign == null ? "?" : campaign.getId())
                + " -/- "
                + (lineItemProfileId == null ? "?" : lineItemProfileId)
                + "/"
                + (campaignProfileId == null ? "?" : campaignProfileId)
                + (lifetimeImpressionsServed != null
                    ? " -> " + lifetimeImpressionsServed
                    : "")
                + ", "
                + (advertiser == null ? "Uninitialised" :advertiser.getName())
                + "/"
                + (lineItem == null ? "Uninitialised" : lineItem.getName())
                + "/"
                + (campaign == null ? "Uninitialised" : campaign.getName())
                + "]";
    }

    public String getKey()
    {
        return advertiserId.toString() + "_" + lineItemId.toString() + "_" +
                campaignId.toString();
    }

    boolean matches(BidderInstruction bi)
    {
        return bi.advertiserId.equals(advertiser.getId()) &&
                bi.lineItemId.equals(lineItem.getId()) &&
                bi.campaignId.equals(campaign.getId());
    }

    static int emitColumnHeaders (HSSFSheet worksheet, int rowIndex,
                            Map<CellStyleName, CellStyle> styleMap,
                            ExcelFileSchema schema)
    {
        Row row = worksheet.createRow(rowIndex);
        int colIndex = 0;
        Cell cell;
        for(ExcelSlotSchema spec: schema.getSlots())
        {
            if(spec != null)
            {
                cell = row.createCell(colIndex);
                cell.setCellValue(spec.getLabel());
                cell.setCellStyle(styleMap.get(CellStyleName.boldText));
                colIndex = colIndex + 1;
            }
        }
        CellRangeAddressList addressList =
                new CellRangeAddressList(rowIndex, rowIndex, 0, colIndex);
        HSSFDataValidation dataValidation =
                new HSSFDataValidation
                        (addressList, ExcelColSchema.readOnly_Constraint);
        dataValidation.setSuppressDropDownArrow(false);
        try { worksheet.addValidationData(dataValidation); }
        catch (IllegalArgumentException e)
        {
            throw Utils.barf
                  ("Couldn't assert validation data for column headers", null);
        }
        return ++rowIndex;
    }

    static String processParam(int x, int y, String p)
    {
        String[] split = p.split(",");
        if(split.length != 2)
            throw Utils.barf("Cell reference expression not of the form x,y",
                             null, x, y, p);
        EvaluatorEnvironment env = new EvaluatorEnvironment("X", x, "Y", y); 
        int col = Evaluator.eval(Sexpression.readFromString
                                    (split[0]), env).unboxInteger();
        int row = Evaluator.eval(Sexpression.readFromString
                                    (split[1]), env).unboxInteger();
        String res;
        res = new CellReference(row, col).formatAsString();
        return res;
    }

    public static String groundOutFormula
            (int x, int y, String formula, int start)
    {
        int paramIndex = formula.indexOf("{", start);
        if(paramIndex >= start)
        {
            String prefix = formula.substring(start, paramIndex);
            int endParamIndex = formula.indexOf("}", paramIndex);
            if(endParamIndex >= start)
            {
                String processedParam =
                        processParam
                            (x, y, formula.substring
                                    (paramIndex + 1, endParamIndex));
                String tailResult = groundOutFormula
                                        (x, y, formula, endParamIndex + 1);
                return prefix + processedParam + tailResult;
            }
            else throw Utils.barf("Illegal formula: ", null, formula, start,
                                  paramIndex);
        }
        else return formula.substring(start);
    }

    public static String groundOutFormula(Cell cell, String formula)
    {
        int x = cell.getColumnIndex();
        int y = cell.getRowIndex();
        return groundOutFormula(x, y, formula, 0);
    }

    public static void assertCellValue
            (Cell cell, Object value, boolean blanksForNulls, Object dataRow)
    {
        if(value != null)
        {
            if(value instanceof Boolean)
                cell.setCellValue((Boolean)value);
            else if(value instanceof Calendar)
                cell.setCellValue((Calendar)value);
            else if(value instanceof Date)
                cell.setCellValue((Date)value);
            else if(value instanceof Double)
                cell.setCellValue((Double)value);
            else if(value instanceof Long)
                cell.setCellValue((Long)value);
            else if(value instanceof RichTextString)
                cell.setCellValue((RichTextString)value);
            else if(value instanceof String && ((String)value).startsWith("="))
            {
                String s = (String) value;
                if(s.length() > MAX_EXCEL_CELL_TEXT_SIZE)
                    Utils.logThisPoint
                            (Level.WARN,
                             "Excel cell too wide for value: " + s);
                else
                {
                    String grounded = groundOutFormula(cell, s.substring(1));
                    // System.out.println("Grounding out " + s + " -> " + grounded);
                    if(grounded != null)
                        cell.setCellFormula(grounded);
                }
            }
            else if(value instanceof String)
            {
                String s = (String) value;
                if(s.length() > MAX_EXCEL_CELL_TEXT_SIZE)
                {
                    Utils.logThisPoint
                            (Level.WARN, "Excel cell too wide for row = " +
                                         dataRow + ", value: " + s);
                    s = s.substring(0, MAX_EXCEL_CELL_TEXT_SIZE - 1);
                }
                cell.setCellValue(s);
            }
            else throw Utils.barf("Cannot put value: " + value +
                                 " into a spreadsheet.", null, cell, value);
        }
        else if(blanksForNulls) cell.setCellType(Cell.CELL_TYPE_BLANK);
        else {} // Do nothing.
    }

    static Map<String, Method> methodMap(MethodMapper methodMapper)
    {
        return methodMapper.getGetMethodMap();
    }

    static Map<String, Method> qctxMethodMap(MethodMapper methodMapper)
    {
        return methodMapper.getQctxGetMethodMap();
    }

    static CellStyleName coerceToCellStyleName(Object o)
    {
        if(o == null) return null;
        else if(o instanceof CellStyleName) return (CellStyleName) o;
        else if(o instanceof List)
        {
            for(Object elt: (List) o)
            {
                CellStyleName res = coerceToCellStyleName(elt);
                if(res != null) return res;
            }
            return null;
        }
        else return null;
    }

    public static void dumpCampaignData
            (HSSFSheet worksheet, int rowIndex, Map<CellStyleName,
             CellStyle> styleMap, QueryContext qctx, ExcelFileSchema schema,
             MethodMapper methodMapper, Object dataRow)
    {
        Row row = worksheet.createRow(rowIndex);
        int colIndex = 0;
        Cell cell;
        Object[] simpleGetArgs = new Object[] { };
        Object[] qctxGetArgs = new Object[] { qctx };
        Object[] simpleStyleArgs = new Object[] { };
        Object[] qctxStyleArgs = new Object[] { qctx };
        for(ExcelSlotSchema spec: schema.getSlots())
        {
            if(spec != null)
            {
                try
                {
                    cell = row.createCell(colIndex);
                    Method simpleM = methodMapper.getGetMethodMap().get
                            (spec.getSlotName());
                    Method qctxM = methodMapper.getQctxGetMethodMap().get
                            (spec.getSlotName());
                    if(simpleM != null)
                        assertCellValue(cell, simpleM.invoke
                                (dataRow, simpleGetArgs), true, dataRow);
                    else assertCellValue(cell, qctxM.invoke
                            (dataRow, qctxGetArgs), true, dataRow);
                    CellStyleName styleName = spec.getStyleName();
                    String styleNameMethod = spec.getStyleNameMethod();
                    Method simpleStyleNameMethod =
                            (styleNameMethod == null
                                ? null
                                : methodMap(methodMapper).get
                                    (spec.getStyleNameMethod()));
                    Method qctxStyleNameMethod =
                            (styleNameMethod == null
                             ? null
                             : qctxMethodMap(methodMapper).get
                                    (spec.getStyleNameMethod()));
                    if(simpleStyleNameMethod != null)
                        styleName = coerceToCellStyleName
                                (simpleStyleNameMethod.invoke
                                 (dataRow, simpleStyleArgs));
                    else if(qctxStyleNameMethod != null)
                        styleName = coerceToCellStyleName
                                (qctxStyleNameMethod.invoke
                                 (dataRow, qctxStyleArgs));
                    else {}
                    if(styleName != null)
                    {
                        CellStyle style = styleMap.get(styleName);
                        if(style == null)
                            throw Utils.barf
                                    ("Missing Cell Style for: " + styleName,
                                     dataRow, rowIndex, spec);
                        else cell.setCellStyle(style);
                    }
                    colIndex = colIndex + 1;
                }
                catch (IllegalAccessException e)
                { throw Utils.barf(e, dataRow, rowIndex, spec); }
                catch (InvocationTargetException e)
                { throw Utils.barf(e, dataRow, rowIndex, spec); }
            }
        }
    }

    private Status createStatus(SQLContext sctx, QueryContext qctx,
                           Long advertiserId, Long campaignId,
                           String eventType, String theStatus)
    {
        bidder().recordEvent(sctx, qctx, advertiserId, campaignId,
                             eventType, theStatus);
        Status status = new Status(theStatus);
        bidder().setStatus(status);
        return status;
    }

    private void setStatus(Status theStatus)
    {
        bidder().setStatus(theStatus);
    }

    static BidderInstruction arbitraryElement
            (Map<String, BidderInstruction> instructions)
    {
        for(BidderInstruction bi: instructions.values())
        {
            if(bi != null) return bi;
        }
        return null;
    }

    public String advertiserNameFromId(Long advertiserId, Bidder bidder,
                                       SQLContext sctx, QueryContext qctx)
    {
        String query =
           "SELECT name FROM advertisernames WHERE id = " + advertiserId + ";";
        Sexpression res =
                bidder.ensureBidderSQLConnector().runSQLQuery(query, qctx);
        String name = null;
        if(res != Null.nil) name = res.car().car().unboxString();
        return name;
    }

    BidStrategy imposeDefaultBidStrategy()
    {
        BidStrategy bs = BidderInstruction.knownBidStrategies[0];
        String name = bs.getPrimaryName();
        setBiddingPolicy(name);
        String noteString =
                "A new campaign, " + prettyString() +
                ", has been found and assigned the" +
                " \"" + name + "\" bidding strategy.";
        addNotification(noteString);
        Utils.logIt(Level.WARN, noteString);
        return bs;
    }

    public CampaignData(Bidder theBidder,Identity ident, AdvertiserData parent,
                        Sexpression data,
                        Map<String, BidderInstruction> instructions,
                        SQLContext sctx, QueryContext qctx,
                        AdvertiserData currentAdvertiserData,
                        Date instructionsDate)
    {
        this.bidder = theBidder;
        this.data = data;
        this.parent = parent;
        this.ident = ident;
        Status status = null;
        boolean lineItemProfileFound = false;
        int i = 0;
        // Note: data is in the form of the reply pattern specified by
        // Bidder.getCampaignDataQuery, namely:
        // (?Advertiser ?LineItem ?LineItemProfile ?Campaign ?CampaignProfile)
        for(Sexpression element: data)
        {
            if(parent != null && element instanceof AdvertiserService)
            {
                advertiser = (AdvertiserService) element;
                if(status == null) {
                	status = initializeCreatingCampaignDataStatus(parent);
                }
                status.elaborate("advertiser " + advertiser.getId(), false);
                parent.service = advertiser;
                advertiserId = advertiser.getId();
            }
            else if(element instanceof LineItemService)
            {
                lineItem = (LineItemService) element;
                if(status == null) {
                	status = initializeCreatingCampaignDataStatus(parent);
                }
                status.elaborate("line item " + lineItem.getId(), false);
                lineItemId = lineItem.getId();
            }
            else if(element instanceof CampaignService)
            {
                campaign = (CampaignService) element;
                if(status == null) {
                	status = initializeCreatingCampaignDataStatus(parent);
                }
                status.elaborate("campaign " + campaign.getId(), false);
                campaignId = campaign.getId();
                stats = campaign.getStats();
                String tz = campaign.getTimezone();
                if(advertiserId == null)
                {
                    advertiserId = campaign.getAdvertiser_id();
                    advertiserName = advertiserNameFromId
                            (advertiserId, bidder, sctx, qctx);
                }
                Map<Date, HistoricalDataRow> hist = getHistoricalData(qctx);
                Aggregate ag =
                        new Aggregate
                                (hist, bidder().getCurrentTime(),
                                 TimeZone.getTimeZone(tz));
                lifetimeImpressionsServed = ag.lifetimeImpressionsServed;
                dailyImpressionsServed = ag.dailyImpressionsServed;
                yesterdayImpressionsServed = ag.yesterdayImpressionsServed;
                dayBeforeYesterdayImpressionsServed =
                        ag.dayBeforeYesterdayImpressionsServed;
            }
            else if(element == Syms.Empty)
            {
                switch(i)
                {
                    case 0: throw Utils.barf
                                    ("Advertiser is missing", this, data);
                    case 1: throw Utils.barf
                                    ("Line Item is missing", this, data);
                    case 2: lineItemProfileFound = true;
                            lineItemProfile = null;
                            break;
                    case 3: throw Utils.barf
                                    ("Campaign is missing", this, data);
                    case 4: campaignProfile = null;
                            break;
                    default: throw Utils.barf
                            ("Unknown arg count.", this, i, data);
                }
            }
            else if(element instanceof ProfileService)
            {
                if(lineItemProfileFound)
                {
                    campaignProfile = (ProfileService) element;
                    if(status == null) {
                    	status = initializeCreatingCampaignDataStatus(parent);
                    }
                    status.elaborate("campaignProfile " +
                                     campaignProfile.getId(), false);
                    campaignProfileId = campaignProfile.getId();
                }
                else
                {
                    lineItemProfile = (ProfileService) element;
                    if(status == null) {
                    	status = initializeCreatingCampaignDataStatus(parent);
                    }
                    status.elaborate("lineItemProfile " +
                                     lineItemProfile.getId(), false);
                    lineItemProfileId = lineItemProfile.getId();
                    lineItemProfileFound = true;
                }
            }
            else {} // Not a known/handled type.
            i = i + 1;
        }
        // Just in case the spreadsheet is empty.
        String key = (instructions != null  ? getKey() : null);
        BidderInstruction bi =
                (instructions != null ? instructions.get(key) : null);
        if(bi != null)
        {
            bi.init(BidderInstruction.bidderInstructionSchema );
            setBiddingPolicy(bi.biddingPolicy);
            dailyImpressionsTarget = bi.dailyImpressionsTarget;
        }
        else if(Bidder.getNewCampaignBidImpositionPolicy() ==
                NewCampaignBidImpositionPolicy.DO_NOTHING)
        {
            if(instructions != null && instructions.size() > 0)
                // Pick an arbitrary one to get the defaults.
                bi = arbitraryElement(instructions);
            if(bi != null)
            {
                Object bp = bi.getDefaultSlotValue
                                (BidderInstruction.BIDDING_POLICY,
                                 BidderInstruction.bidderInstructionSchema);
                if(bp instanceof String)
                    setBiddingPolicy((String) bp);
            }
            if(biddingPolicy == null)
                setBiddingPolicy
                    (NoOptimizationBidStrategy.STRATEGY.getPrimaryName());
            dailyImpressionsTarget = campaign.getDaily_budget_imps();
        }
        else
        {
            if(instructions != null && instructions.size() > 0)
                // Pick an arbitrary one to get the defaults.
                bi = arbitraryElement(instructions);
            Sexpression currentBidPolSexp = getCampaignSettings(sctx, qctx);
            String currentPolicy =
                    (currentBidPolSexp == Null.nil
                            ? null
                            : currentBidPolSexp.third().unboxString());
            setBiddingPolicy(currentPolicy);
            if(bi != null)
            {
                Object bp = bi.getDefaultSlotValue
                                (BidderInstruction.BIDDING_POLICY,
                                 BidderInstruction.bidderInstructionSchema);
                if(Bidder.getNewCampaignBidImpositionPolicy() ==
                   NewCampaignBidImpositionPolicy.IMMEDIATE &&
                   bp instanceof String && currentPolicy == null)
                {
                    // Then we've never seen this before.
                    setBiddingPolicy((String) bp);
                    String noteString =
                            "A new campaign, " + prettyString() +
                            ", has been found and assigned the" +
                            " \"" + bp + "\" bidding strategy.";
                    addNotification(noteString);
                    Utils.logIt(Level.WARN, noteString);
                }
            }
            if(biddingPolicy == null)
            {
                setBiddingPolicy
                    (NoOptimizationBidStrategy.STRATEGY.getPrimaryName());
                if(instructions != null)
                {
                    String noteString =
                            "Campaign " + prettyString() +
                            " has no bidding strategy, so it is being" +
                            " assigned the strategy \"" +
                            getBiddingPolicy() + "\".";
                    addNotification(noteString);
                    Utils.logIt(Level.WARN, noteString);
                }
            }
            dailyImpressionsTarget = campaign.getDaily_budget_imps();
        }
        
        Double recordedMaxBid = getRecordedMaxBid(sctx, qctx);
        /*
         * Youtrack issue - OPT-92
         * To record the max bid for new campaigns with ECP as bid policy
         */
		if (recordedMaxBid == null
				&& campaign.getCpm_bid_type().equals(BidSpec.ECP_BID_MODE)) {
			setMaxBid(campaign.getMax_bid());
		} 
		/*
		 * Youtrack issue - OPT-92
		 * If the campaign is running on ECP and the max bid amount
		 * is different from what was record last time, update the max bid.
		 * Else, use the max bid from the recordedMaxBid (campaignsettings table)
		 * when it was recorded last time (The else conditions includes campaigns 
		 * running on other bid strategies such as Adjustable Daily Impressions too)
		 */
		else if (recordedMaxBid != null) {
			if (campaign.getCpm_bid_type().equals(BidSpec.ECP_BID_MODE)
					&& campaign.getMax_bid() != null
					&& !campaign.getMax_bid().equals(recordedMaxBid)) {
				setMaxBid(campaign.getMax_bid());
			} else {
				setMaxBid(recordedMaxBid);
			}
		}
		else if (bi != null && bi.maxBid != null && bi.maxBid > 0.0d) {
			/*
			 * Youtrack issue - OPT-92
			 * If the campaign is running on ECP and the max bid amount
			 * is different from what was record last time, update the max bid.
			 * Else, use the max bid from the appnexus.xls when it was recorded
			 * last time (The else conditions includes campaigns running on other
			 * bid strategies such as Adjustable Daily Impressions too)
			 */
			if (campaign.getCpm_bid_type().equals(BidSpec.ECP_BID_MODE)
					&& campaign.getMax_bid() != null
					&& !campaign.getMax_bid().equals(bi.maxBid)) {
				setMaxBid(campaign.getMax_bid());
			} else {
				setMaxBid(bi.maxBid);
			}
		} 
		else {
			setMaxBid(getCurrentBid());
		}
        
		/*
		 * Youtrack issue - OPT-92
		 * This code has been commented to fix the issue mentioned in the
		 * Youtrack ticket above
		 */
        /*if(bi != null && bi.maxBid != null && bi.maxBid > 0.0d)
            setMaxBid(bi.maxBid);
        else if(recordedMaxBid != null)
            setMaxBid(recordedMaxBid);
        else
            setMaxBid(getCurrentBid());*/
        
		if (parent != null) {
			saveBiddingPolicyAndMaxBid(ident, sctx, qctx);
			System.out.println("Testing jenkins automatic build trigger with git commit and push");
		}
        if(currentAdvertiserData != null)
        {
            CampaignData currentCampaignData =
                    currentAdvertiserData.getCampaignData(campaignId);
            if(currentCampaignData != null
               && currentCampaignData.getLastWriteDate().getTime() >
                  instructionsDate.getTime())
                currentCampaignData.executeUpdates(this);
        }
        setLastWriteDate(new Date());
    }
    
    private Status initializeCreatingCampaignDataStatus(AdvertiserData parent) {
    	Status status = new Status("Creating campaign data");
    	if(parent != null) {
    		setStatus(status);
    	}
    	return status;
    }
    

    public Bidder bidder()
    {
        if(bidder == null)
            bidder = parent.bidder;
        return bidder;
    }

    Identity ident()
    {
        if(ident == null)
            ident = parent.getIdent();
        return ident;
    }

    void updateBidHistory(SQLContext sctx, QueryContext qctx,
                          String bidStrategy, Double bid,
                          Long dailyImpressionBudget,
                          Long dailyImpressionTarget)
    {
        Sexpression updateQuery =
                Sexpression.readFromString
                        ("(tell (CBO_DB.BidHistory",
                                advertiserId, lineItemId,
                                campaignId, qt, bidStrategy, qt, bid,
                                dailyImpressionBudget,
                                dailyImpressionTarget, "null))");
        sctx.request(updateQuery, qctx);
    }

    boolean shouldImposeBidStrategy(Map<Long, Set<Long>> readyToForceStrategy)
    {
        Set<Long> camps = readyToForceStrategy.get(advertiserId);
        /*
         * Youtrack issue - OPT-93. Temporary fix to exclude campaign names starting with
         * "**" from being optimized. Included the condition !campaign.getName().startsWith("**")
         * as part of the fix.
         */
        Date startDate = getStartDate();
		Date now = bidder.getCurrentTime();
		/*
		 * Youtrack issue - OPT-88. Campaigns running on ECP with a future start date should not
		 * be changed to Adjustable Daily Impressions until the campaign actually starts.
		 */
		boolean campaignHasFutureStartDate = (startDate != null && startDate
				.getTime() > now.getTime());
		return (camps != null && camps.contains(campaignId)
				&& !campaignHasFutureStartDate && !campaign.getName()
				.startsWith("**"));
    }

    void effectuateBid(SQLContext sctx, QueryContext qctx, Date lastReportTime,
                       Map<Long, Set<Long>> readyToForceStrategy)
    {
        Status status = createStatus(sctx, qctx, advertiserId, campaignId, "Bid",
                          "Effectuating campaign bid for " + campaign.getId());
        setStatus(status);
        BidStrategy bs = BidderInstruction.getStrategy(biddingPolicy);
        boolean shouldImpose = shouldImposeBidStrategy(readyToForceStrategy);
        if(bs == null)
            throw Utils.barf("Unhandled bidding policy: " + biddingPolicy,
                             this);
        else if(Bidder.getNewCampaignBidImpositionPolicy() ==
                NewCampaignBidImpositionPolicy.AFTER_DELAY &&
                parent != null &&
                shouldImpose &&
                Bidder.defaultBidStrategiesSet.contains(bs))
        {
            bs = imposeDefaultBidStrategy();
            saveBiddingPolicyAndMaxBid(ident, sctx, qctx);
        }
        else {}
        if(bs instanceof NoOpBidStrategy)
            status.elaborate("Campaign not being optimised");
        else
        {
            // Run the bid.
            BidSuggestion suggestion = suggestedBid(qctx, lastReportTime);
            if(suggestion == null)
            {
                status.elaborate("Bid suggestion returned a null.");
                AppNexusInterface.debugPrint
                        ("Bid suggestion returned a null bid for campaign \""
                         + campaignId + "\".");
            }
            else
            {
            	status.elaborate("Suggestion is: " + suggestion);
                Double bid = suggestion.getSuggestedBid();
                Long dailyImpressionBudget =
                        suggestion.getNewDailyImpressionLimit();
                Long dailyImpressionTarget = dailyImpressionsTarget;
                AppNexusInterface.debugPrint
                      ("Run bid for " + campaignId + " to AppNexus -> " + bid);
                if(!bidder().getUpdateAppNexus())
                {
                    status.elaborate("Appnexus updates disabled!");
                    AppNexusInterface.debugPrint
                            ("Not performing AppNexus update to "
                             + "effectuateBid for campaign \"" + campaignId
                             + "\".");
                    // Special case: Record the bid as -1 if the bid is null.
                    // We could argue that we should just make the bid column
                    // be nullable, but I don't want to change the schema right
                    // now, and this only happens when we're in debugging mode
                    // and not actually updating AppNexus.
                    updateBidHistory(sctx, qctx, bs.getPrimaryName(),
                                     (bid == null ? -1 : bid),
                                     dailyImpressionBudget,
                                     dailyImpressionTarget);
                    setImpressionBudgets
                            (sctx, qctx, dailyImpressionBudget,
                             campaign.getLifetime_budget_imps(), true);
                }
                else
                {
                    if((bid != null) && 
                       (!bid.equals(getCurrentBid()) || bidder().getForceBidUpdating()))
                    {
                        // Update AppNexus.  Set base_bid to the suggested bid,
                        // then update the campaign structure locally to
                        // reflect the change.
                        Sexpression appUpdateQuery =
                                Cons.list(Syms.Tell,
                                    Cons.list(Sexpression.readFromString
                                                ("AppNexus.Campaign.Base_bid"),
                                              advertiser, campaign,
                                              new NumberAtom(bid)));
                        Utils.interpretACL(Integrator.INTEGRATOR,
                                Cons.list(Syms.Request, appUpdateQuery,
                                        Null.nil),
                                qctx);
                        campaign.setBase_bid(bid);
                        updateBidHistory(sctx, qctx, bs.getPrimaryName(), bid,
                                         dailyImpressionBudget,
                                         dailyImpressionTarget);
                        Bidder.logChange(advertiserId, campaignId,
                                campaignProfileId, "Set bid",
                                bid, null, null, null,
                                campaign, campaignProfile, qctx);
                        status.elaborate("Bid updated.");
                    }
                    else  {			// No need to do anything, since bid hasn't changed.
                    	status.elaborate("Bid has not changed.");
                    }
                         
                    // Update daily impression limit if needed
                    if((dailyImpressionBudget != null) &&
                       (!dailyImpressionBudget.equals(getDailyImpressionsTarget()) || bidder().getForceBidUpdating()))
                    {
                        // Update AppNexus.  
                    	// Set daily_budget_imps to the new daily impression limit,
                        // then update the campaign structure locally to
                        // reflect the change.

                        setImpressionBudgets
                            (sctx, qctx, dailyImpressionBudget,
                             campaign.getLifetime_budget_imps(), false);
                        /*
                        Sexpression appUpdateQuery =
                                Cons.list(Syms.Tell,
                                    Cons.list(Sexpression.readFromString
                                                ("AppNexus.Campaign.Daily_budget_imps"),
                                              advertiser, campaign,
                                              new NumberAtom(dailyImpressionLimit)));
                        Utils.interpretACL(Integrator.INTEGRATOR,
                                Cons.list(Syms.Request, appUpdateQuery,
                                        Null.nil),
                                qctx);
                        setDailyImpressionsLimit(dailyImpressionLimit);
                                */
                        Bidder.logChange(advertiserId, campaignId,
                                campaignProfileId, "Set daily impression limit",
                                dailyImpressionBudget,
                                null, null, null, campaign, campaignProfile,
                                qctx);
                        status.elaborate("Daily impression limit updated.");
                    }
                    else {			// Impression limit hasn't changed.
                         status.elaborate("Daily impression limit has not changed.");
                    }
                }
            }
        }
    }

    public void setImpressionBudgets
            (SQLContext sctx, QueryContext qctx, Long dailyImpressionsTarget,
             Long lifetimeImpressionsTarget, boolean justRecord)
    {
        Sexpression appUpdateQueryDaily =
                Cons.list(Syms.Tell,
                        Cons.list(Sexpression.readFromString
                                ("AppNexus.Campaign.Daily_Budget_Imps"),
                                advertiser, campaign,
                                (dailyImpressionsTarget == null
                                  ? Null.nil
                                  : new NumberAtom(dailyImpressionsTarget))));
        // Lifetime update code commented out because at present we only futz
        // with the daily limits.  Safer not to monkey with the lifetime limits
        // unless/until we do that.  Still, we should be logging both the
        // values in DailyImpressionBudgetHistory.
        /*
        Sexpression appUpdateQueryLifetime =
                Cons.list(Syms.Tell,
                        Cons.list(Sexpression.readFromString
                                ("AppNexus.Campaign.Lifetime_Budget_Imps"),
                                advertiser, campaign,
                                new NumberAtom(lifetimeImpressionsTarget)));
                                */
        
        /*
         * Youtrack issue OPT-125: Fix to not update the daily_budget_imps of campaigns
         * that are running on lifetime_pacing (value set to true)
         */
		boolean isLifeTimePacing = campaign.getLifetime_pacing() != null ? campaign
				.getLifetime_pacing() : false;
        
        if(!justRecord && !isLifeTimePacing)
        {
            Utils.interpretACL(Integrator.INTEGRATOR,
                    Cons.list(Syms.Request, appUpdateQueryDaily,
                            Null.nil),
                    qctx);
            setDailyImpressionsLimit(dailyImpressionsTarget);
            /*
            Utils.interpretACL(Integrator.INTEGRATOR,
                    Cons.list(Syms.Request, appUpdateQueryLifetime,
                            Null.nil),
                    qctx);
            campaign.setLifetime_budget_imps(lifetimeImpressionsTarget);
                    */
            Bidder.logChange(advertiserId, campaignId,
                    campaignProfileId, "Set daily impression limit",
                    dailyImpressionsTarget, lifetimeImpressionsTarget, null,
                    null, campaign, campaignProfile, qctx);
        }
        Sexpression updateQuery =
                Sexpression.readFromString
                        ("(tell (CBO_DB.DailyImpressionBudgetHistory",
                                advertiserId, lineItemId, campaignId,
                                dailyImpressionsTarget,
                                lifetimeImpressionsTarget, "null))");
        sctx.request(updateQuery, qctx);
    }

    BidHistoryRow[] getBidHistory(QueryContext qctx)
    {
        SQLConnector connector = bidder().ensureBidderSQLConnector();
        Sexpression query =
                Sexpression.readFromString
                        ("(ask-all (?event-time ?bidStrategy ?bid ?dailyImpressionBudget)",
                                "(CBO_DB.BidHistory", advertiserId,
                                lineItemId, campaignId,
                                "?bidStrategy ?bid ?dailyImpressionBudget ?dailyImpressionTarget ?event-time))");
        Sexpression res = connector.request(query, Null.nil, qctx);
        BidHistoryRow[] out = new BidHistoryRow[res.length()];
        int i = 0;
        while(res != Null.nil)
        {
            Sexpression row = res.car();
            out[i] = new BidHistoryRow(row.car(), row.second(), row.third(),
                                       row.fourth(), row.fifth());
            res = res.cdr();
            i = i + 1;
        }
        return out;
    }

    DailyImpressionBudgetHistoryRow[] getDailyImpressionBudgetHistory
            (QueryContext qctx)
    {
        SQLConnector connector = bidder().ensureBidderSQLConnector();
        Sexpression query =
                Sexpression.readFromString
                        ("(ask-all (?event-time ?imps)",
                                "(CBO_DB.DailyImpressionBudgetHistory",
                                advertiserId, lineItemId, campaignId,
                                "?imps ?event-time))");
        Sexpression res = connector.request(query, Null.nil, qctx);
        DailyImpressionBudgetHistoryRow[] out =
                new DailyImpressionBudgetHistoryRow[res.length()];
        int i = 0;
        while(res != Null.nil)
        {
            Sexpression row = res.car();
            out[i] = new DailyImpressionBudgetHistoryRow(row.car(), row.second());
            res = res.cdr();
            i = i + 1;
        }
        return out;
    }

    Double fallBackBaseBidTransitionValue()
    {
        if(stats == null || stats.get("cost_ecpm") == null)
            return Bidder.defaultDefaultBid;
        else
        {
            String ecpm = (String)stats.get("cost_ecpm");
            if(ecpm != null)
            {
                try
                {
                    return Double.parseDouble(ecpm) *
                           Bidder.eCPMToBaseBidTransitionFraction;
                }
                catch (NumberFormatException e)
                {
                    return Bidder.defaultDefaultBid;
                }
            }
            else return Bidder.defaultDefaultBid;
        }
    }

    Long statsClicks()
    {
        Object val = statsValue("clicks");
        if(val instanceof Long) return (Long) val;
        else return null;
    }

    Long statsImps()
    {
        Object val = statsValue("imps");
        if(val instanceof Long) return (Long) val;
        else return null;
    }

    Double statsEcpm()
    {
        Object val = statsValue("cost_ecpm");
        if(val instanceof Double) return (Double) val;
        else return null;
    }

    Double statsMediaCost()
    {
        Object val = statsValue("media_cost");
        if(val instanceof Double) return (Double) val;
        else return null;
    }

    Object statsValue(String key)
    {
        if(stats == null || stats.get(key) == null)
            return null;
        else
        {
            String val = (String)stats.get(key);
            if(val != null)
            {
                try
                {
                    return Long.parseLong(val);
                }
                catch (NumberFormatException e1)
                {
                    try
                    {
                        return Double.parseDouble(val);
                    }
                    catch (NumberFormatException e2)
                    {
                        return null;
                    }
                }
            }
            else return null;
        }
    }

    void handleBidPolicyChange
            (Identity ident, String from, String to, SQLContext sctx,
             QueryContext qctx)
    {
        Status status = new Status("Handling bid policy change for " + campaign.getId());
        setStatus(status);
        List<AbstractAppNexusServiceWithId> changed = null;
        BidStrategy fromBs = BidderInstruction.getStrategy(from);
        BidStrategy toBs = BidderInstruction.getStrategy(to);
        Long dailyImpressionBudget = campaign.getDaily_budget_imps();
        if(dailyImpressionBudget == null)
          dailyImpressionBudget = Bidder.defaultDefaultDailyImpressionLimit;
        if(!bidder().getUpdateAppNexus())
            AppNexusInterface.debugPrint
                    ("Not performing AppNexus update to "
                            + "handleBidPolicyChange from \"" + from
                            + "\" to \"" + to
                            + "\".");
        else if((!isFixedPriceStrategy(fromBs)) && isFixedPriceStrategy(toBs))
        {
            // Then we should ensure that we move from ECP bid mode to
            // fixed price mode.
            Double bidValue = campaign.getMax_bid();
            if(bidValue == null) bidValue = fallBackBaseBidTransitionValue();
            status.elaborate("Moving from ECP to fixed price bid");
            BidSpec bid = new FixedBaseBidSpec
                                (bidValue, dailyImpressionsTarget, null);
            AppNexusCampaignSplitter.ensureHasBid
                    (ident, advertiserId, campaign, bid, changed);
            updateBidHistory(sctx, qctx, toBs.getPrimaryName(), bidValue,
                             dailyImpressionBudget, dailyImpressionsTarget);
        }
        else if(isFixedPriceStrategy(fromBs) && (!isFixedPriceStrategy(toBs)))
        {
            // Then we should ensure that we move from fixed price mode
            // to ECP bid mode.
            status.elaborate("Moving from fixed price bid to ECP");
            Double recordedMaxBid = getRecordedMaxBid(sctx, qctx);
            if(recordedMaxBid != null)
                // Get the max bid from  our DB if possible
                setMaxBid(recordedMaxBid);
            else if(campaign.getBase_bid() != null)
              setMaxBid(campaign.getBase_bid() * Bidder.fixedToECPBidFraction);
            else if(campaign.getMax_bid() != null)
              setMaxBid(campaign.getMax_bid() * Bidder.fixedToECPBidFraction);
            else setMaxBid(fallBackBaseBidTransitionValue());
            BidSpec bid =
                    new ECPBidSpec(getMaxBid(), dailyImpressionsTarget, null);
            AppNexusCampaignSplitter.ensureHasBid
                    (ident, advertiserId, campaign, bid, changed);
            updateBidHistory(sctx, qctx, toBs.getPrimaryName(), getMaxBid(),
                             dailyImpressionBudget, dailyImpressionsTarget);
            /*
             * Youtrack issue - OPT-93
             * Switch campaigns with names prefixed with ** and are currently
             * running on Adjustable Daily Impressions back to ECP. The following
             * lines of code will update the appnexus.xls
             */
            setBiddingPolicy(to);
            String noteString =
                    "The campaign, " + prettyString() +
                    ", has been switched from " + fromBs.getPrimaryName() + " to " +
                    " \"" + toBs + "\" bidding strategy.";
            addNotification(noteString);
            Utils.logIt(Level.WARN, noteString);
        }
        else status.elaborate("No change in bid policy");
    }

    public static Sexpression getCampaignSettings
            (SQLContext sctx, QueryContext qctx,
             Long advertiserId, Long campaignId)
    {
        Sexpression query =
                Sexpression.readFromString
                        ("(ask-one (?maxBid ?dailyBudget ?biddingPolicy)",
                              "(CBO_DB.CampaignSettings",
                                advertiserId, "?lineItemId", campaignId,
                                "?maxBid ?dailyBudget ?biddingPolicy))");
        return sctx.request(query, qctx, true);
    }

    //### 88% of analysis done at :40 past the hour.
    // Select hour, imps, cost from historicaldata where advertiser_id = xxx and line_item_id = yyy
    // and campaign_id = zzz order by hour ASC.  Sometimes 2 ms to 15 mS.  Getting 100 - 1000 results.
    // Most of the time is proportional to the result count.
    // Called for all campaigns.
    
    // This appears to be select maxBid, dailyBudget, biddingPolicy
    // from CampaignSettings where advertiser_id = a and line_item_id = l and campaign_id = c
    // 
    Sexpression getCampaignSettings(SQLContext sctx, QueryContext qctx)
    {
        Sexpression query =
                Sexpression.readFromString
                        ("(ask-one (?maxBid ?dailyBudget ?biddingPolicy)",
                              "(CBO_DB.CampaignSettings",
                                advertiserId, lineItemId, campaignId,
                                "?maxBid ?dailyBudget ?biddingPolicy))");
        return sctx.request(query, qctx, true);
    }

    Double getRecordedMaxBid(SQLContext sctx, QueryContext qctx)
    {
        Sexpression bid = getCampaignSettings(sctx, qctx).car();
        if(bid instanceof NumberAtom) return ((NumberAtom)bid).doubleValueOf();
        else return null;
    }

    void saveBiddingPolicyAndMaxBid
            (Identity ident, SQLContext sctx, QueryContext qctx)
    {
    	Status status = new Status("Saving bid policy for " + campaign.getId());
        setStatus(status);
        String tableName =
                "CBO_DB." + Bidder.campaignSettingsTable.getName();
        Sexpression currentBidPolSexp = getCampaignSettings(sctx, qctx);
        Double currentMaxBid =
                (currentBidPolSexp == Null.nil
                        ? null
                        : currentBidPolSexp.car().unboxDouble());
        Long currentDailyBudgetImps =
                (currentBidPolSexp == Null.nil
                        ? null
                        : currentBidPolSexp.second().unboxLong());
        String currentPolicy =
                (currentBidPolSexp == Null.nil
                        ? null
                        : currentBidPolSexp.third().unboxString());
        if(currentMaxBid == null && maxBid == null)
            setMaxBid(0.0d); // defaultify.
        if(dailyImpressionsTarget == null)
            dailyImpressionsTarget =
                (currentDailyBudgetImps == null ? 0L : currentDailyBudgetImps);
        boolean changed = false;
        if(currentPolicy != null &&
                (!currentPolicy.equals(biddingPolicy)
                        // I think that the following code is the right thing,
                        // but I'm leaving it commented out for now.
                        // The principle is that if we think that we're in
                        // ECP mode, but the current bid policy suggests that
                        // we should be in any bid policy other than
                        // non-optimisation, then we should also recognise
                        // a bid policy change.
                 /*
                 ||
                 (BidSpec.ECP_BID_MODE.equals(campaign.getCpm_bid_type()) &&
                  !currentPolicy.equals
                       (NoOptimizationBidStrategy.NO_OPTIMIZATION) &&
                  !currentPolicy.equals
                       (NoOptimizationBidStrategy.NO_OPTIMIZATION_SECONDARY))
                       */
                ))
        {
            handleBidPolicyChange(ident, currentPolicy, biddingPolicy,
                                  sctx, qctx);
            changed = true;
        }
        /*
         * Youtrack issue - OPT-93
         * Switch campaigns with names prefixed with ** and are currently
         * running on Adjustable Daily Impressions back to ECP
         */
        else if (currentPolicy != null
				&& campaign.getCpm_bid_type().equals(BidSpec.BASE_BID_MODE)
				&& campaign.getName().startsWith("**")) {
			handleBidPolicyChange(ident, currentPolicy,
					NoOptimizationBidStrategy.STRATEGY.getPrimaryName(), sctx,
					qctx);
			changed = true;
		}
        else if(currentPolicy == null &&
                !biddingPolicy.equals(NotSelectedBidStrategy.NOT_SELECTED))
        {
            handleBidPolicyChange(ident, NotSelectedBidStrategy.NOT_SELECTED,
                                  biddingPolicy, sctx, qctx);
            /*
             * Youtrack issue OPT-88. Insert a record into bidhistory table for new campaigns
             * running on ECP. This will allow the campaigns to be optimized after a day's run.
             */
            Long dailyImpressionBudget = campaign.getDaily_budget_imps();
            updateBidHistory(sctx, qctx, biddingPolicy, getMaxBid(),
            		dailyImpressionBudget, dailyImpressionsTarget);
            changed = true;
        }
        /*
         * Youtrack issue OPT-88. Insert a record into bidhistory table for campaigns
         * already running on ECP but not yet optimized. Check if bidhistory table doesn't have
         * any records for the campaign before insertion.
         * This will allow the campaigns to be optimized after a day's run.
         */
        else if (currentPolicy != null
				&& campaign.getCpm_bid_type().equals(BidSpec.ECP_BID_MODE)) {
        	BidHistoryRow[] bidHistory = getBidHistory(qctx);
			if (bidHistory == null || bidHistory.length == 0) {
				Long dailyImpressionBudget = campaign.getDaily_budget_imps();
				updateBidHistory(sctx, qctx, biddingPolicy, getMaxBid(),
						dailyImpressionBudget, dailyImpressionsTarget);
			}
			changed = true;
		}

		if (!maxBid.equals(currentMaxBid)) {
			changed = true;
			Utils.logIt(Level.INFO, "New Max Bid amount for campaign id "
					+ campaign.getId() + " = " + getMaxBid());
		}
        if(dailyImpressionsTarget == null ||
           !dailyImpressionsTarget.equals(currentDailyBudgetImps))
            changed = true;
        if(changed)
        {
            Sexpression updateQuery =
                    Sexpression.readFromString
                            // Note:  We need the uses of "qt".
                            ("(tell (CBO_DB.CampaignSettings", advertiserId,
                                    lineItemId, campaignId, maxBid,
                                    dailyImpressionsTarget, qt,
                                    biddingPolicy, qt, "))");
            sctx.request(updateQuery, qctx);
            Bidder.logChange(advertiserId, campaignId,
                             campaignProfileId, "Campaign settings changed",
                             maxBid, dailyImpressionsTarget, biddingPolicy,
                             null, campaign, campaignProfile, qctx);
        }
    }

    static void warn(String s)
    {
        System.out.println(s);
        Utils.logThisPoint(Level.WARN, s);
    }

    public static Object[] splitCampaign
            (Bidder bidder, Identity ident, CampaignService campaign,
             int numberOfKids, long primaryPercentage, long ecpPercentage,
             long totalDailyBudget, double primaryBid, double maxBid,
             double bidOffsetPercentage, boolean verbose)
    {
        // Split up a campaign into k experimental chunks
        // plus the original, and one ECP control if the ecpPercentage > 0.
        int totalKids = numberOfKids + 1 + (ecpPercentage > 0 ? 1 : 0);
        BidSpec[] bids = new BidSpec[totalKids];
        Long[] ranges = new Long[totalKids];
        Double[] bidValues = new Double[totalKids];
        if(primaryPercentage < 0L || primaryPercentage + ecpPercentage > 100L)
            throw Utils.barf("primaryPercentage/ecpPercentage out of range!",
                             null);
        if(primaryPercentage == 100 && (numberOfKids > 0 || ecpPercentage > 0))
            throw Utils.barf("Cannot allocate shares to child campaigns.",
                             null);
        Long remainingPercentage = 100 - (primaryPercentage + ecpPercentage);
        Long childShare =
                (numberOfKids == 0 ? 0 : remainingPercentage / numberOfKids);
        if(numberOfKids > 0 && childShare <= 0)
            throw Utils.barf("Cannot allocate shares to child campaigns.",
                             null);
        // Return any share left over by rounding errors.
        primaryPercentage =
                primaryPercentage +
                        (remainingPercentage - (childShare * numberOfKids));
        Long totalPercent =
               primaryPercentage + ecpPercentage + (childShare * numberOfKids);
        if(totalPercent !=100)
            throw Utils.barf
                  ("Cannot make shares add up to 100% (" + totalPercent + ")",
                   null);
        ranges[0] = primaryPercentage;
        if(ecpPercentage > 0)
            ranges[1] = ecpPercentage;
        for(int i = 1 + (ecpPercentage > 0 ? 1 : 0); i < totalKids; i++)
        {
            ranges[i] = childShare;
        }
        FixedBaseBidSpec mainBid =
                new FixedBaseBidSpec
                     (primaryBid,  totalDailyBudget * primaryPercentage / 100,
                      null);
        bids[0] = mainBid;
        bidValues[0] = primaryBid;
        if(ecpPercentage > 0)
        {
            bids[1] = new ECPBidSpec(primaryBid,
                                      totalDailyBudget * ecpPercentage / 100,
                                      null);
            bidValues[1] = primaryBid;
        }
        double sign = 1.0D;
        int multiplier = 2;
        int i = 1 + (ecpPercentage > 0 ? 1 : 0);
        long surplusBudget = 0;
        while(i < totalKids)
        {
            double thisBid =
                    primaryBid + ((sign * (multiplier/2) * primaryBid *
                                   bidOffsetPercentage) / 100);
            long thisBudget = totalDailyBudget * childShare / 100;
            if(thisBid <= maxBid)
            {
                bids[i] = new FixedBaseBidSpec(thisBid, thisBudget, null);
                bidValues[i] = thisBid;
                i = i + 1;
            }
            else if(thisBid <= 0)
            {
                surplusBudget = surplusBudget + thisBudget;
                if(verbose)
                    warn("** Bid of " + thisBid + " <= 0 was rejected");
                break;
            }
            else
            {
                surplusBudget = surplusBudget + thisBudget;
                if(verbose)
                    warn("** Bid of " + thisBid + " >= maxBid was rejected");
            }
            sign = - sign;
            multiplier = multiplier + 1;
        }
        if(surplusBudget > 0 && verbose)
            warn("Budget returned: " + surplusBudget);
        mainBid.addBudget(surplusBudget, null);
        if(verbose)
        {
            warn("Ranges: " + AppNexusUtils.commaSeparate(ranges) + "\n" +
                 "Bid values: "  + AppNexusUtils.commaSeparate(bidValues) + "\n" +
                 "Bids: "  + AppNexusUtils.commaSeparate(bids));
        }
        Boolean includeCookielessUsers = true;
        List<AbstractAppNexusServiceWithId> changed = null;
        if(ident != null && campaign != null)
        {
            SQLConnector connector = bidder.ensureBidderSQLConnector();
            QueryContext qctx =
                    new BasicQueryContext(null, bidder.getAppNexusTheory());
            Map<Long, ProfileService> campaignProfileMap =
                    new HashMap<Long, ProfileService>();
            Map<Long, CampaignService> campaignMap =
                    new HashMap<Long, CampaignService>();
            changed = AppNexusCampaignSplitter.ensureCampaignSplit
                            (ident, campaign, ranges, bids,
                             includeCookielessUsers, campaignProfileMap, campaignMap);
            for(AbstractAppNexusService a: changed)
            {
                if(a instanceof CampaignService)
                {
                    CampaignService c = (CampaignService) a;
                    Long campaignProfileId = c.getProfile_id();
                    ProfileService campaignProfile =
                        (campaignProfileId == null ? null : campaignProfileMap.get(campaignProfileId));
                    Bidder.logChange(campaign.getAdvertiser_id(), c.getId(),
                                     campaignProfileId, (c == campaign
                                                    ? "Split campaign"
                                                    : "Campaign split child"),
                                     null, null, null, null, c, campaignProfile, qctx);
                }
            }
        }
        return new Object[] { bids, ranges, changed };
    }

    static Long ensureNonNullL(Long l)
    {
        return (l == null ? -1L : l);
    }

    static Double ensureNonNullD(Double d)
    {
        return (d == null ? -1.0D : d);
    }

    static Date BEGINNING_OF_TIME = new Date(24 * 3600 * 1000L);

    static Date ensureNonNullDate(Date d)
    {
        return (d == null ? BEGINNING_OF_TIME : d);
    }

    static String ensureNonNullS(String s)
    {
        return (s == null ? "" : s);
    }

    static long[] nullUserGroup = new long[] { -1, -1 };

    static long[] getUserGroup(ProfileService profile)
    {
        if(profile == null) return nullUserGroup;
        {
            ListOrMap existing = profile.getUser_group_targets();
            if(existing == null) return nullUserGroup;
            else
            {
                JSONArray groups = (JSONArray)existing.get("groups");
                if(groups == null) return nullUserGroup;
                else
                {
                    Long low  = (Long)((JSONObject)groups.get(0)).get("low");
                    Long high = (Long)((JSONObject)groups.get(0)).get("high");
                    return new long[]{ low, high };
                }
            }
        }
    }

    static final int DAY_TYPE_WEEKDAY = 0;
    static final int DAY_TYPE_WEEKEND = 1;
    static final int DAY_TYPE_HOLIDAY = 2;

    static int getDayType(Date date)
    {
        int dow = getDoW(date);
        // Handle holidays here, if we ever figure out how.
        if(dow == Calendar.SUNDAY || dow == Calendar.SATURDAY)
            return DAY_TYPE_WEEKEND;
        else return DAY_TYPE_WEEKDAY;
    }

    static int getDoW(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    /*
    // We need to resolve all of the shared attributes that are writable.
    // We think that these are as follows:
    broker_fees
    start_date
    end_date
    state
    enable_pacing
    daily_budget
    daily_budget_imps
    lifetime_budget
    lifetime_budget_imps
    // These have yet to be handled.
    // Rumour has it that timezone may not be writable.
    require_cookie_for_tracking
    timezone

     */
    CampaignService combineJson(LineItemService lineItemLevel,
                                CampaignService campaignLevel)
    {
        if(lineItemLevel == null) return campaignLevel;
        else if(campaignLevel == null)
            return campaignLevel;
        else
        {
            CampaignService res = new CampaignService();
            JSONObject jo = lineItemLevel.serviceToJSONUnwrapped();
            res.initFromJSON(jo);
            res.updateFrom(campaignLevel);
            //---------------------------
            String liSD = lineItemLevel.getStart_date();
            String caSD = campaignLevel.getStart_date();
            res.setStart_date(AppNexusUtils.dateMax(liSD, caSD));
            //---------------------------
            String liED = lineItemLevel.getEnd_date();
            String caED = campaignLevel.getEnd_date();
            res.setEnd_date(AppNexusUtils.dateMin(liED, caED));
            //---------------------------
            if(AppNexusUtils.IN_ACTIVE.equals(lineItemLevel.getState()))
                res.setState(AppNexusUtils.IN_ACTIVE);
            else res.setState(campaignLevel.getState());
            //---------------------------
            // I think this is right.  Any campaign-level setting should trump
            // the line-item, otherwise use the line-item setting.
            if(campaignLevel.getEnable_pacing() != null)
                res.setEnable_pacing(campaignLevel.getEnable_pacing());
            else res.setEnable_pacing(lineItemLevel.getEnable_pacing());
            //---------------------------
            // I think this is right.  Any line-item-level setting should trump
            // the campaign, otherwise use the campaign setting.
            // This is based on a symmetry argument wrt what they say in their
            // docs concerning insertion orders.
            // https://wiki.appnexus.com/display/api/Line+Item+Service
            //         #LineItemService-BrokerFeesArray
            // "Broker fees at the line item level override broker fees
            //  at the insertion order level."
            ListOrMap liBF = lineItemLevel.getBroker_fees();
            if(liBF == null)
                res.setBroker_fees(campaignLevel.getBroker_fees());
            else res.setBroker_fees(liBF);
            //---------------------------
            // The line-item budget supposed to be enough to cover all of the
            // campaigns.  If it isn't, then it trumps the campaign budget.
            // This should be true for all four budget types.
            Double liDB = lineItemLevel.getDaily_budget();
            Double caDB = campaignLevel.getDaily_budget();
            if(liDB == null) res.setDaily_budget(caDB);
            else if(caDB == null) res.setDaily_budget(liDB);
            else if(liDB < caDB) res.setDaily_budget(liDB);
            else res.setDaily_budget(caDB);
            //---------------------------
            Long liDBI = lineItemLevel.getDaily_budget_imps();
            Long caDBI = campaignLevel.getDaily_budget_imps();
            if(liDBI == null) res.setDaily_budget_imps(caDBI);
            else if(caDBI == null) res.setDaily_budget_imps(liDBI);
            else if(liDBI < caDBI) res.setDaily_budget_imps(liDBI);
            else res.setDaily_budget_imps(caDBI);
            //---------------------------
            Double liLB = lineItemLevel.getLifetime_budget();
            Double caLB = campaignLevel.getLifetime_budget();
            if(liLB == null) res.setLifetime_budget(caLB);
            else if(caLB == null) res.setLifetime_budget(liLB);
            else if(liLB < caLB) res.setLifetime_budget(liLB);
            else res.setLifetime_budget(caLB);
            //---------------------------
            Long liLBI = lineItemLevel.getLifetime_budget_imps();
            Long caLBI = campaignLevel.getLifetime_budget_imps();
            if(liLBI == null) res.setLifetime_budget_imps(caLBI);
            else if(caLBI == null) res.setLifetime_budget_imps(liLBI);
            else if(liLBI < caLBI) res.setLifetime_budget_imps(liLBI);
            else res.setLifetime_budget_imps(caLBI);
            //---------------------------
            return res;
        }
    }

    ProfileService combineProfileJson(ProfileService lineItemLevel,
                                      ProfileService campaignLevel)
    {
        if(lineItemLevel == null) return campaignLevel;
        else
        {
            ProfileService res = (ProfileService)lineItemLevel.copySelf();
            res.updateFrom(campaignLevel);
            // For the most part, targeting data is only set at the
            // campaign-profile level because the UI doesn't expose targeting
            // decisions at the line-item level.  However, some stuff is still
            // stashed at the line-item-profile level, conspicuously anything
            // to do with frequency caps.
            //---------------------------
            Long liMSI = lineItemLevel.getMax_session_imps();
            Long caMSI = campaignLevel.getMax_session_imps();
            if(liMSI == null) res.setMax_session_imps(caMSI);
            else if(caMSI == null) res.setMax_session_imps(liMSI);
            else res.setMax_session_imps(Math.min(liMSI, caMSI));
            //---------------------------
            Long liMDI = lineItemLevel.getMax_day_imps();
            Long caMDI = campaignLevel.getMax_day_imps();
            if(liMDI == null) res.setMax_day_imps(caMDI);
            else if(caMDI == null) res.setMax_day_imps(liMDI);
            else res.setMax_day_imps(Math.min(liMDI, caMDI));
            //---------------------------
            Long liMMPI = lineItemLevel.getMin_minutes_per_imp();
            Long caMMPI = campaignLevel.getMin_minutes_per_imp();
            if(liMMPI == null) res.setMin_minutes_per_imp(caMMPI);
            else if(caMMPI == null) res.setMin_minutes_per_imp(liMMPI);
            else res.setMin_minutes_per_imp(Math.max(liMMPI, caMMPI));
            //---------------------------
            return res;
        }
    }

    void runUpdates(SQLConnector connector, QueryContext qctx,
                    Date observation_time)
    {
        Date last_modified = ensureNonNullDate(getLastModified());
        String active = campaign.getState();
        String timezone = campaign.getTimezone();
        Long stats_clicks = statsClicks();
        Double stats_ecpm = statsEcpm();
        Long stats_imps = statsImps();
        Double stats_media_cost = statsMediaCost();
        String control_bid_strategy = getBiddingPolicy();
        Double control_max_bid = maxBid;
        Long control_daily_impression_budget = dailyImpressionsLimit;
        Long control_daily_impression_target = dailyImpressionsTarget;
        String updateQuery =
                "UPDATE observedData\n" +
                "SET active = '" + activeSQLValue(active) +"',\n" +
                "    timezone = '" + timezone + "',\n" +
                (stats_clicks     == null ? "" : "    stats_clicks = '" + stats_clicks + "',\n") +
                (stats_ecpm       == null ? "" : "    stats_ecpm = '" + stats_ecpm + "',\n") +
                (stats_imps       == null ? "" : "    stats_imps = '" + stats_imps + "',\n") +
                (stats_media_cost == null ? "" : "    stats_media_cost = '" + stats_media_cost + "',\n") +
                "    control_bid_strategy = '" + control_bid_strategy + "',\n" +
                "    control_max_bid = '" + ensureNonNullD(control_max_bid) + "',\n" +
                "    control_daily_impression_budget = '" + ensureNonNullL(control_daily_impression_budget) + "',\n" +
                "    control_daily_impression_target = '" + ensureNonNullL(control_daily_impression_target) + "'\n" +
                "WHERE advertiser_id = " + advertiserId + "\n" +
                "AND   campaign_id = " + campaignId + "\n" +
                "AND   observation_time = '" + connector.dateToSQL(observation_time) + "';";
        connector.runSQLUpdate(updateQuery, qctx);
    }

    Object activeSQLValue(String active)
    {
        return (active == null ? Syms.NULL
                               : (AppNexusUtils.ACTIVE.equals(active)
                                    ? 1
                                    : 0));
    }

    long recordObservedData
            (SQLContext sctx, QueryContext qctx, Date now,
             Map<Long, String> lineItemNameMap,
             Map<Long, String> campaignNameMap, Long highWaterMark)
    {
        String lineItemJson = Bidder.serviceToJson(lineItem);
        String lineItemProfileJson  = Bidder.serviceToJson(lineItemProfile);
        String campaignJson = Bidder.serviceToJson(campaign);
        String campaignProfileJson  = Bidder.serviceToJson(campaignProfile);
        CampaignService combinedJson = combineJson(lineItem, campaign);
        ProfileService combinedProfileJson =
                combineProfileJson(lineItemProfile, campaignProfile);
        long[] userGroup = getUserGroup(campaignProfile);
        lineItemNameMap.put(lineItem.getId(), lineItem.getName());
        campaignNameMap.put(campaign.getId(), campaign.getName());
        Date last_modified = ensureNonNullDate(getLastModified());
        String active = campaign.getState();
        String timezone = campaign.getTimezone();
        Long stats_clicks = statsClicks();
        Double stats_ecpm = statsEcpm();
        Long stats_imps = statsImps();
        Double stats_media_cost = statsMediaCost();
        String control_bid_strategy = getBiddingPolicy();
        Double control_max_bid = maxBid;
        Long control_daily_impression_budget = dailyImpressionsLimit;
        Long control_daily_impression_target = dailyImpressionsTarget;
        highWaterMark = highWaterMark + 1;
        Sexpression updateQuery =
                Cons.list(Syms.Tell,
                          Sexpression.boxList
                            (Syms.intern("CBO_DB.OBSERVEDDATA"),
                             advertiserId,
                             lineItemId,
                             (lineItemProfileId == null
                                     ? -1
                                     : lineItemProfileId),
                             campaignId,
                             (campaignProfileId == null
                                     ? -1
                                     : campaignProfileId),
                             now,
                             AppNexusUtils.dayFloor(now),
                             ensureNonNullD(campaign.getBase_bid()),
                             ensureNonNullD(campaign.getMax_bid()),
                             ensureNonNullL(campaign.getDaily_budget_imps()),
                             ensureNonNullL(campaign.getLifetime_budget_imps()),
                             ensureNonNullDate(getStartDate()),
                             ensureNonNullDate(getEndDate()),
                             BEGINNING_OF_TIME,
                             userGroup[0],
                             userGroup[1],
                             (AppNexusCampaignSplitter.isChildCampaign(campaign) ? 1 : 0),
                             getDoW(now),
                             getDayType(now),
                             ensureNonNullS(campaign.getCpm_bid_type()),
                             lineItemJson,
                             lineItemProfileJson,
                             campaignJson,
                             campaignProfileJson,
                             Bidder.serviceToJson(combinedJson),
                             Bidder.serviceToJson(combinedProfileJson),
                             highWaterMark, // Syms.NULL,  // sequence_number
                             Syms.NULL,  	// materially_different (null)
                             0,      		// has_material_differences (false)
                             Syms.NULL,  	// material_differences
                             Syms.NULL,
                             Syms.NULL,
                             Syms.NULL,
                             Syms.NULL,
                             Syms.NULL,
                             Syms.NULL,
                             Syms.NULL,
                             Syms.NULL,
                             Syms.NULL,
                             Syms.NULL,
                             //-------------------------
                             last_modified,
                             activeSQLValue(active),
                             timezone,
                             (stats_clicks == null ? 0 : stats_clicks),
                             (stats_ecpm == null ? 0.0 : stats_ecpm),
                             (stats_imps == null ? 0 : stats_imps),
                             (stats_media_cost == null ? 0.0 : stats_media_cost),
                             control_bid_strategy,
                             control_max_bid,
                             control_daily_impression_budget,
                             control_daily_impression_target
                            ));
        Utils.interpretACL(Integrator.INTEGRATOR,
                           Cons.list(Syms.Request, updateQuery, Null.nil),
                           qctx);
        return highWaterMark;
    }

    @SuppressWarnings("unchecked")
    static Class[] actionClasses =
            {
                    SplitCampaignFromFile.class,
                    CampaignSplitter.class,
                    null
            };

    public static void main(String[] args)
    {
        // Tests.testClasses(actionClasses, args);
        String res;
        int x = 10;
        int y = 20;
        int start = 0;
        String[] formulae =
                {
                        "=1000*{(- x 1),y}/{(- x 2),y}",
                        "={(- x 4),y}*{(- x 1),y}/1000",
                        "={(- x 1),y}*{(- x 4),y}"
                };
        for(String formula: formulae)
        {
            System.out.print(formula + " -> ");
            res = groundOutFormula(x, y, formula, start);
            System.out.println(res);
        }
    }

    
    /** Determines if the bid strategy passed in is a fixed price bid strategy.
     * Returns false if the bid strategy is null.
     * @param bs The bid strategy.
     * @return True if the bid strategy is fixed price or false if not.
     */
    private boolean isFixedPriceStrategy(BidStrategy bs) {
    	return (bs != null) && bs.isFixedPriceStrategy();
    }
}

class Aggregate {
    long dailyImpressionsServed = 0;
    long yesterdayImpressionsServed = 0;
    long dayBeforeYesterdayImpressionsServed = 0;
    double dailyCost = 0.0d;
    long lifetimeImpressionsServed = 0;
    double lifetimeCost = 0.0d;

    Aggregate(Map<Date, HistoricalDataRow> map, Date now, TimeZone tz)
    {
        Date startDate = AppNexusUtils.dayFloor(now, tz, 0);
        long dayStart = startDate.getTime();
        long yesterdayStart = dayStart - (3600 * 24 * 1000);
        long dayBeforeYesterdayStart = yesterdayStart - (3600 * 24 * 1000);
        for(Date key: map.keySet())
        {
            HistoricalDataRow val = map.get(key);
            if(key.getTime() >= yesterdayStart && key.getTime() < dayStart)
            {
                yesterdayImpressionsServed =
                        yesterdayImpressionsServed + val.impressions;
            }
            else if(key.getTime() >= dayBeforeYesterdayStart &&
                    key.getTime() < yesterdayStart)
            {
                dayBeforeYesterdayImpressionsServed =
                        dayBeforeYesterdayImpressionsServed + val.impressions;
            }
            else {}
            if(key.getTime() >= dayStart)
            {
                dailyImpressionsServed =
                        dailyImpressionsServed + val.impressions;
                dailyCost = dailyCost + val.cost;
            }
            lifetimeImpressionsServed =
                    lifetimeImpressionsServed + val.impressions;
            lifetimeCost = lifetimeCost + val.cost;
        }
    }
}

class CampaignComparator implements Comparator<CampaignData> {
    public int compare(CampaignData a, CampaignData b)
    {
        CampaignService aService = a.campaign;
        CampaignService bService = b.campaign;
        if(aService == null)
            return (bService == null ? 0 : -1);
        else if(bService == null) return 1;
        else
        {
            String aName = a.campaign.getName();
            String bName = b.campaign.getName();
            return (aName == null
                    ? (bName == null ? 0 : -1)
                    : (bName == null ? 1 : aName.compareTo(bName)));
        }
    }
}

class PacingData {
    Long advertiserId;
    Long campaignId;
    Long hours;
    Date since;
    Date endHour;
    Long impressions;

    public PacingData(Sexpression row)
    {
        advertiserId = row.car().unboxLong();
        campaignId = row.second().unboxLong();
        hours = row.third().unboxLong();
        since = row.fourth().unboxDate();
        endHour = row.fifth().unboxDate();
        impressions = row.sixth().unboxLong();
    }

    static final long MILLISECONDS_PER_HOUR = 3600000;
    static final long MILLISECONDS_PER_DAY  = 24 * MILLISECONDS_PER_HOUR;

    public Long getHours()
    {
        return hours;
    }

    public Long getLifetimePacing(CampaignData cd)
    {
        CampaignService campaign = cd.campaign;
        long hoursDifference = (endHour.getTime() - since.getTime())/
                               MILLISECONDS_PER_HOUR;
        if(hoursDifference == 0) return null;
        {
            // John's rule for this is that if we do NOT have at least a
            // complete day of results (presumably relative to the
            // campaign's TZ), AND we're in either ECP or DailyImpressions
            // bid mode, then we project using the current daily impression
            // target.  Otherwise, we use the average computed from the
            // real data.
            Date endDayStart = 
                    AppNexusUtils.dayFloor(endHour, cd.getTimeZone(), 0);
            BidStrategy bs = cd.getBiddingPolicyObj();
            boolean projectUsingRealData =
                ((endDayStart.getTime() - since.getTime()) >=
                        MILLISECONDS_PER_DAY) ||
                !(bs ==      NotSelectedBidStrategy.STRATEGY ||
                  bs ==   NoOptimizationBidStrategy.STRATEGY ||
                  bs == DailyImpressionsBidStrategy.STRATEGY);
            double averageImpsPerHour;
            if(projectUsingRealData)
                averageImpsPerHour = (1.0d * impressions) / hoursDifference;
            else if(cd.dailyImpressionsTarget == null) return null;
            else averageImpsPerHour = cd.dailyImpressionsTarget / 24;
            if(endHour == null) return null;
            else
            {
                Date trueEndDate = AppNexusUtils.asDate(campaign.getEnd_date(),
                                                       campaign.getTimezone());
                if(trueEndDate == null || cd.lifetimeImpressionsServed == null)
                    return null;
                else
                {
                    long campaignHoursRemaining =
                            (trueEndDate.getTime() - endHour.getTime())
                                    / MILLISECONDS_PER_HOUR;
                    Double projectedImpressions;
                    projectedImpressions = cd.lifetimeImpressionsServed +
                            (campaignHoursRemaining * averageImpsPerHour);
                    return projectedImpressions.longValue();
                }
            }
        }
    }
}

class StashedUpdate {
    MethodMapper methodMapper;
    String slotName;
    QueryContext qctx;
    Object value;

    public StashedUpdate(MethodMapper methodMapper, String slotName,
                         QueryContext qctx, Object value)
    {
        this.methodMapper = methodMapper;
        this.slotName = slotName;
        this.qctx = qctx;
        this.value = value;
    }

    public void execute(CampaignData campaignData)
    {
        methodMapper.setValue(campaignData, slotName, qctx, value);
    }
}

