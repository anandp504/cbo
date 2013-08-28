package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;
import org.apache.poi.ss.usermodel.Row;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("unused")
public class BidderInstruction extends ExcelInstruction {
     public String advertiserName;
     public Long advertiserId;
     public String lineItemName;
     public Long lineItemId;
     public String campaignName;
     public Long campaignId;
     public Long dailyImpressionsLimit;
     public Long dailyImpressionsTarget;
     public Long lifetimeImpressionsTarget;
     public Long lifetimeImpressionsServed;
     public Long dailyImpressionsServed;
     public Long yesterdayImpressionsServed;
     public Double maxBid;
     public String biddingPolicy;
     public Date startDate;
     public Date endDate;
     public Double suggestedBid;
     public Double currentBid;
     public String bidReason;
     public String pacing;
     public String dailyPacing;
     public String targetingSpec;

     public static final String DAILY_IMPRESSIONS_TARGET = "dailyImpressionsTarget";
     public static final String DAILY_IMPRESSIONS_LIMIT = "dailyImpressionsLimit";
     public static final String ADVERTISER_NAME = "advertiserName";
     public static final String CAMPAIGN_NAME = "campaignName";
     public static final String BIDDING_POLICY = "biddingPolicy";
     public static final String MAX_BID = "maxBid";
     public static final String CURRENT_OR_MAX_BID = "currentOrMaxBid";
     private static Map<String, Field> fieldMap = new HashMap<String, Field>();
     private static boolean fieldMapInitialised = false;
     static Map<String, Method> legalValuesMethodMap =
             new HashMap<String, Method>();
     static boolean legalValuesMethodMapInitialised = false;
     static boolean menuValuesMethodMapInitialised = false;
     static Map<String, Method> menuValuesMethodMap =
             new HashMap<String, Method>();

     // This is where we define the schema of the input/output file.
     // Each row in the following describes a column in the spreadsheet.
     // To add a column to the spreadsheet, you need to define a row of metadata
     // here, and also a get method for the slot for the class CampaignData.
     // For example, if you are defining a slot called mySlot, then you need to
     // define a PUBLIC member field on the BidderInstruction class called
     // mySlot, and a getMySlot method on the CampaignData class.
     // The CellStyle can either be a string naming a style in the cellStyleMap,
     // or the name of a method on CampaignData that, when called delivers the
     // name of a style in the style map (or null).
     public static ExcelFileSchema bidderInstructionSchemaWithoutTargeting =
             new ExcelFileSchema("BidderInstruction Schema Without Targeting",
                     new String[][]
                     {
                             // Pretty Name             Slot Name                     Constraint    Legal Values Method            Menu Values Method         CellStyle
                             { "Advertiser Name",       ADVERTISER_NAME,              READ_ONLY,    null,                          null,                      null},
                             { "Advertiser Id",         "advertiserId",               READ_ONLY,    null,                          null,                      null},
                             { "Line Item Name",        "lineItemName",               READ_ONLY,    null,                          null,                      null},
                             { "Line Item Id",          "lineItemId",                 READ_ONLY,    null,                          null,                      null},
                             { "Campaign Name",         CAMPAIGN_NAME,                READ_ONLY,    null,                          null,                      null},
                             { "Campaign Id",           "campaignId",                 READ_ONLY,    null,                          null,                      null},
                             { "Starts",                "startDate",                  READ_ONLY,    null,                          null,                      "dateOnly"},
                             { "Ends",                  "endDate",                    READ_ONLY,    null,                          null,                      "dateOnly"},
                             { "Lifetime Imp Target",   "lifetimeImpressionsTarget",  READ_ONLY,    null,                          null,                      "numberWithCommas"},
                             { "Imps served lifetime",  "lifetimeImpressionsServed",  READ_ONLY,    null,                          null,                      "numberWithCommas"},
                             { "Lifetime Pacing",       "pacing",                     READ_ONLY,    null,                          null,                      "getPacingCellStyle"},
                             { "Daily Imp Limit",       DAILY_IMPRESSIONS_LIMIT,      NON_NEGATIVE, null,                          null,                      "numberWithCommas"},
                             { "Daily Imp Target",      DAILY_IMPRESSIONS_TARGET,     READ_ONLY,    null,                          null,                      "numberWithCommas"},
                             { "Imps served yesterday", "yesterdayImpressionsServed", READ_ONLY,    null,                          null,                      "numberWithCommas"},
                             { "Daily Pacing",          "dailyPacing",                READ_ONLY,    null,                          null,                      "getDailyPacingCellStyle"},
                             { "Imps served today",     "dailyImpressionsServed",     READ_ONLY,    null,                          null,                      "numberWithCommas"},
                             { "Max Bid",               MAX_BID,                      NON_NEGATIVE, null,                          null,                      "dollarCurrency"},
                             { "Current Bid",           "currentBid",                 READ_ONLY,    null,                          null,                      "dollarCurrency"},
                             // { "Suggested Bid",         "suggestedBid",               READ_ONLY,    null,                          null,                      "dollarCurrency"},
                             { "Bid Reason",            "bidReason",                  READ_ONLY,    null,                          null,                      "getBidReasonCellStyle"},
                             { "Bidding Policy",        BIDDING_POLICY,               LEGAL_VALUES, "getBiddingPolicyLegalValues", "biddingPolicyMenuValues", null}
                     },
                     "com.tumri.cbo.backend.CampaignData",
                     BidderInstruction.class);

     public static ExcelFileSchema bidderInstructionSchema =
             new ExcelFileSchema("Bidder Instruction Schema",
                     bidderInstructionSchemaWithoutTargeting,
                     new String[][]
                     {
                             // Pretty Name             Slot Name                     Constraint    Legal Values Method            Menu Values Method         CellStyle
                             // Disable this column for now.  As I recall,
                             // Pradeep wanted it, but nobody has ever used it,
                             //  and it's huge, thereby making the spreadsheet
                             // harder to read.
                             // { "Targeting",             "targetingSpec",              READ_ONLY,    null,                          null,                      null}
                     },
                     "com.tumri.cbo.backend.CampaignData",
                     BidderInstruction.class);

     static BidStrategy[] knownBidStrategies =
            new BidStrategy[]
                    {
                            DailyImpressionsBidStrategyWithCapChange.STRATEGY,
                            NotSelectedBidStrategy.STRATEGY,
                            NoOptimizationBidStrategy.STRATEGY,
                            // ImpressionTargetBidStrategy.STRATEGY,
                            DailyImpressionsBidStrategy.STRATEGY
                    };

    static Map<String, BidStrategy> ensureBidStrategyMap()
    {
        Map<String, BidStrategy> map = new HashMap<String, BidStrategy>();
        for(BidStrategy bs: knownBidStrategies)
        {
            if(bs == null) {}
            else
            {
                String[] names = bs.getNames();
                for(String name: names)
                {
                    if(name != null) map.put(name, bs);
                    else throw Utils.barf
                            ("Not a legal bid strategy reference.", null, bs);
                }
            }
        }
        return map;
    }

     public static final Map<String, BidStrategy> bidStrategyMap =
             ensureBidStrategyMap();

     public static String[] getBiddingPolicyLegalValues()
     {
         return getBiddingPolicyLegalValues(false, false);
     }

     public static String[] getBiddingPolicyLegalValues
             (boolean exclude, boolean primaryOnly)
     {
         int len = 0;
         for(BidStrategy bs: knownBidStrategies)
         {
             String[] strs = bs.getNames();
             len = len + (primaryOnly ? 1 : strs.length);
         }
         String[] res =
                new String[len - (exclude ? 1 : 0)];
         int i = 0;
         if(primaryOnly)
         {
             for(BidStrategy bs: knownBidStrategies)
             {
                 String str = bs.getPrimaryName();
                 if(!exclude ||
                    !NotSelectedBidStrategy.NOT_SELECTED.equals(str))
                 {
                     res[i] = str;
                     i = i + 1;
                 }
             }
         }
         else
         {
             for(BidStrategy bs: knownBidStrategies)
             {
                 String[] strs = bs.getNames();
                 for(String str: strs)
                 {
                     if(!exclude || !NotSelectedBidStrategy.NOT_SELECTED.equals(str))
                     {
                         res[i] = str;
                         i = i + 1;
                     }
                 }
             }
         }
         return res;
     }

     public static String[] biddingPolicyMenuValues()
     {
         return getBiddingPolicyLegalValues(true, true);
     }

     public static BidStrategy getStrategy(String name)
     {
         BidStrategy res = bidStrategyMap.get(name);
         if(res == null)
             throw Utils.barf("Cannot find a bid strategy for: " + name, null,
                              name);
         else return res;
     }

     public static String policyValuesType()
     {
         int res = 0;
         for(String s: getBiddingPolicyLegalValues())
         {
             res = Math.max(res, s.length());
         }
         return "varchar(" + res + ")";
     }

     public String toString()
     {
         return "["+ AppNexusUtils.afterDot(this.getClass().getName()) + ": "
                 + (advertiserId == null ? "Uninitialised" : advertiserId)
                 + "/"
                 + (lineItemId == null ? "Uninitialised" : lineItemId)
                 + "/"
                 + (campaignId == null ? "Uninitialised" : campaignId)
                 + " -> "
                 + (maxBid == null ? "???" : "$" + maxBid)
                 + "]";
     }

     public String getKey()
     {
         return advertiserId.toString() + "_" + lineItemId.toString() + "_" +
                 campaignId.toString();
     }

     public BidderInstruction() // So that we can call newInstance.
     {
         super();
     }

     public BidderInstruction(Map<String, Integer> columnMap, Row row,
                              ExcelFileSchema schema)
     {
         super(columnMap, row, schema);
     }
 }
