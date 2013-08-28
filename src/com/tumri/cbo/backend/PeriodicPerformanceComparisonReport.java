package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import org.apache.poi.ss.usermodel.Row;

import java.util.*;

public class PeriodicPerformanceComparisonReport extends ExcelInstruction {
    /*
    public String advertiserName;
    public Long   advertiserId;
    public String campaignName;
    public Long   campaignId;
    public Long   lifetimeImpressionsServed;
     */

    public static final String ADVERTISER_NAME = BidderInstruction.ADVERTISER_NAME;
    public static final String CAMPAIGN_NAME = BidderInstruction.CAMPAIGN_NAME;
    public static final String ADVERTISER_ID =
            BidderGrapherHTTPHandler.ADVERTISER_ID_PARAM;
    public static final String CAMPAIGN_ID =
            BidderGrapherHTTPHandler.CAMPAIGN_ID_PARAM;
    static final String UNCONSTRAINED =
            ExcelConstraintType.UNCONSTRAINED.toString();


    public static ExcelFileSchema periodicPerformanceComparisonReportSchema =
            new ExcelFileSchema
                    ("PeriodicPerformanceComparisonReport Schema",
                            new String[][]
                                    {
                                            // Pretty Name       Slot Name                    Constraint     Legal Values Method Menu Values Method CellStyle
                                            { "Advertiser Name", ADVERTISER_NAME,             UNCONSTRAINED, null,               null,              null},
                                            { "Advertiser Id",   ADVERTISER_ID,               UNCONSTRAINED, null,               null,              null},
                                            { "Campaign Name",   CAMPAIGN_NAME,               UNCONSTRAINED, null,               null,              null},
                                            { "Campaign Id",     CAMPAIGN_ID,                 UNCONSTRAINED, null,               null,              null},
                                            { "Imps served",     "impressions",               UNCONSTRAINED, null,               null,              "numberWithCommas"},
                                            { "Media cost",      "mediaCost",                 UNCONSTRAINED, null,               null,              "dollarCurrency"},
                                            { "Opt CPM",         "optCPMFormula",             UNCONSTRAINED, null,               null,              "dollarCurrency"},
                                            { "Unopt CPM",       "unoptCPM",                  UNCONSTRAINED, null,               null,              "dollarCurrency"},
                                            { "Est Unopt Cost",  "estUnoptCostFormula",       UNCONSTRAINED, null,               null,              "dollarCurrency"},
                                            { "Est Savings",     "estSavingsFormula",         UNCONSTRAINED, null,               null,              "dollarCurrency"},
                                            { "Est % Savings",   "estPctSavingsFormula",      UNCONSTRAINED, null,               null,              "percentage"}
                                    },
                            "com.tumri.cbo.backend.PeriodicPerformanceData",
                            PeriodicPerformanceComparisonReport.class);

    @SuppressWarnings("unused")
    public PeriodicPerformanceComparisonReport()
    // So that we can call newInstance.
    {
        super();
    }

    @SuppressWarnings("unused")
    public PeriodicPerformanceComparisonReport
            (Map<String, Integer> columnMap, Row row, ExcelFileSchema schema)
    {
        super(columnMap, row, schema);
    }
}
