package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.appnexus.services.*;
import com.tumri.mediabuying.zini.ExcelColSchema;
import com.tumri.mediabuying.zini.QueryContext;
import com.tumri.mediabuying.zini.SQLContext;
import com.tumri.mediabuying.zini.Utils;
import org.apache.log4j.Level;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.*;
import java.util.*;

public class SplitCampaignFromFile {

    static final String PERCENTAGE_COLUMN = "Percentage";
    static final String BID_TYPE_COLUMN = "BidType";
    static final String BID_COLUMN = "Bid";
    static final String DAILY_IMPRESSIONS_BUDGET_COLUMN =
            "DailyImpressionBudget";
    static final String LIFETIME_IMPRESSIONS_BUDGET_COLUMN =
            "LifeTimeImpressionBudget";

    static void info(String s)
    {
        System.out.println(s);
        Utils.logThisPoint(Level.INFO, s);
    }

    static void splitCampaignFromFile(String[] args, Long advertiserId,
                                      Long campaignId, String sheetName,
                                      String inputPath, SQLContext sctx,
                                      QueryContext qctx, CampaignData cd)
    {
        Boolean includeCookielessUsers = true;
        Identity ident = AppNexusUtils.identityFromCommandLine(args);
        if(ident != null)
        {
            CampaignService campaign = AppNexusInterface.simpleFetchCampaign
                            (ident, advertiserId, campaignId,
                             AppNexusInterface.INTERVAL_VALUE_LIFETIME, null);
            HSSFWorkbook workbook = ExcelColSchema.readInWorkbook(inputPath);
            HSSFSheet worksheet =
                    (sheetName == null
                            ? workbook.getSheetAt(0)
                            : workbook.getSheet(sheetName));
            Integer columnIndex = 0;
            Iterator<Row> rowIterator = worksheet.rowIterator();
            Map<String, Integer> columnMap = new HashMap<String, Integer>();
            if(!rowIterator.hasNext())
                throw Utils.barf("Spreadsheet " + inputPath + " is empty",
                                 args, advertiserId, campaignId, sheetName,
                                 inputPath, sctx, qctx, cd);
            Row headers = rowIterator.next();
            Iterator<Cell> cellIterator = headers.cellIterator();
            while(cellIterator.hasNext())
            {
                Cell c = cellIterator.next();
                columnMap.put(c.getStringCellValue(), columnIndex);
                columnMap.put(c.getStringCellValue().toUpperCase(), columnIndex);
                columnIndex = columnIndex + 1;
            }
            Integer percentageIndex = columnMap.get(PERCENTAGE_COLUMN);
            Integer bidTypeIndex = columnMap.get(BID_TYPE_COLUMN);
            Integer bidIndex = columnMap.get(BID_COLUMN);
            Integer dailyImpressionsBudgetIndex =
                    columnMap.get(DAILY_IMPRESSIONS_BUDGET_COLUMN);
            Integer lifetimeImpressionsBudgetIndex =
                    columnMap.get(LIFETIME_IMPRESSIONS_BUDGET_COLUMN);
            if(percentageIndex == null || bidTypeIndex == null ||
                bidIndex == null || dailyImpressionsBudgetIndex == null ||
                lifetimeImpressionsBudgetIndex == null)
                throw Utils.barf("Missing column(s)", args, advertiserId,
                                 campaignId, sheetName, inputPath, sctx,
                                 qctx, cd);
            List<Long> percentages = new Vector<Long>();
            List<BidSpec> bidSpecs = new Vector<BidSpec>();
            while(rowIterator.hasNext())
            {
                Row row = rowIterator.next();
                percentages.add
                  (new Double(row.getCell(percentageIndex).
                                getNumericCellValue()).longValue());
                Double bid = row.getCell(bidIndex).getNumericCellValue();
                Long dailyBudget =
                        new Double(row.getCell(dailyImpressionsBudgetIndex).
                                getNumericCellValue()).longValue();
                Long lifetimeBudget =
                        new Double(row.getCell(lifetimeImpressionsBudgetIndex).
                                getNumericCellValue()).longValue();
                String bidType =
                        row.getCell(bidTypeIndex).getStringCellValue();
                if("ECP".equals(bidType))
                    bidSpecs.add(new ECPBidSpec(bid, dailyBudget, 
                                                lifetimeBudget));
                else bidSpecs.add(new FixedBaseBidSpec(bid, dailyBudget,
                                                       lifetimeBudget));
            }
            List<AbstractAppNexusServiceWithId> changed =
                    AppNexusCampaignSplitter.ensureCampaignSplit
                            (ident, campaign, percentages, bidSpecs,
                                    includeCookielessUsers, null, null);
            for(AbstractAppNexusService s: changed)
            {
                if(cd != null && s instanceof CampaignService)
                {
                    cd.setImpressionBudgets
                        (sctx, qctx,
                         ((CampaignService)s).getDaily_budget_imps(),
                         ((CampaignService)s).getLifetime_budget_imps(),
                         true);
                }
            }
            info("Changed from " + percentages.toString() + ": " + changed);
        }
    }

    public static void main(String[] args)
    {
        String advertiserIdS = 
                AppNexusUtils.commandLineGet("-advertiser", args);
        Long advertiserId;
        if(advertiserIdS == null)
            advertiserId = new Long(AppNexusTestHelper.readLineFromSystemIn
                                        ("Advertiser ID: "));
        else advertiserId = new Long(advertiserIdS);
        String campaignIdS =
                AppNexusUtils.commandLineGet("-campaign", args);
        Long campaignId;
        if(campaignIdS == null)
            campaignId = new Long(AppNexusTestHelper.readLineFromSystemIn
                                        ("Campaign ID: "));
        else campaignId = new Long(campaignIdS);
        String inputPath = AppNexusUtils.commandLineGet("-inputfile", args);
        if(inputPath == null)
           inputPath = AppNexusTestHelper.readLineFromSystemIn("Input file: ");
        String sheetName = null;
        splitCampaignFromFile
                (args, advertiserId, campaignId, sheetName, inputPath,
                 null, null, null);
    }


}
