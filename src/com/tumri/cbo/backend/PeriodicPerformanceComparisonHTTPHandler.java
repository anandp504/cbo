package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.*;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;

import java.io.*;
import java.util.*;
import java.util.List;

public class PeriodicPerformanceComparisonHTTPHandler extends HTTPHandler {

    Bidder bidder = null;
    DBValueExtractor extractor = null;

    void setBidder(Bidder bidder)
    {
        this.bidder = bidder;
    }

    Bidder ensureBidder()
    {
        if(bidder == null)
            bidder = Bidder.getInstance();
        return bidder;
    }

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        setListener(l);
        HTTPListener.registerHandler(getUrlName(), this);
    }

    static String urlName = "PERIODICPERFORMANCECOMPARISON";
    static String prettyName = "Periodic Performance Comparison";

    @SuppressWarnings("unused")
    public PeriodicPerformanceComparisonHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public PeriodicPerformanceComparisonHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, urlName, prettyName);
        setBidder(bidder);
        extractor = new DBValueExtractor(bidder);
    }

    public boolean isAdminUserCommand() { return false; }

    static final String TIME_PERIOD_PARAM = "TimePeriod";
    static final String LAST_7_DAYS = "Last 7 days";
    static final String LAST_WHOLE_WEEK = "Last whole week";
    static final String LAST_30_DAYS = "Last 30 days";
    static final String LAST_WHOLE_MONTH = "Last whole month";
    static final String WHOLE_CAMPAIGN = "Whole campaign";

    static final String[] TIME_PERIOD_MENU_ITEMS =
            {
                    LAST_7_DAYS,
                    LAST_WHOLE_WEEK,
                    LAST_30_DAYS,
                    LAST_WHOLE_MONTH,
                    WHOLE_CAMPAIGN
            };

    public void downloadSpreadsheetForSchema
            (OutputStream os, List<PeriodicPerformanceData> perfData,
             QueryContext qctx, ExcelFileSchema schema)
    {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet worksheet = workbook.createSheet();
        Map<CellStyleName, CellStyle> styleMap =
                ExcelColSchema.setupStyleMap(workbook);
        // Always write the header row.
        int rowIndex = CampaignData.emitColumnHeaders
                                (worksheet, 0, styleMap, schema);
        MethodMapper methodMapper = schema.getMethodMapper();
        ProgressNoter noter =
                new ProgressNoter("Preparing spreadsheet", 1, false);
        for(PeriodicPerformanceData row: perfData)
        {
            CampaignData.dumpCampaignData
                    (worksheet, rowIndex, styleMap, qctx, schema,
                     methodMapper, row);
            noter.event();
            rowIndex = rowIndex + 1;
        }
        noter.finish();
        BidderInstruction.assertConstraints(worksheet, schema);
        ExcelColSchema.dumpSpreadsheet(workbook, os);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        String timePeriod = httpParams.get(TIME_PERIOD_PARAM);
        if(timePeriod == null) timePeriod = TIME_PERIOD_MENU_ITEMS[0];
        Date now = new Date();
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.setTime(now);
        endCal.setTime(now);
        ExcelFileSchema schema = PeriodicPerformanceComparisonReport.
                                     periodicPerformanceComparisonReportSchema;
        QueryContext qctx = new BasicQueryContext(null, bidder.appNexusTheory);
        bidder.ensureLastReportTime(qctx);
        String stylesheetUrl = null;
        Map<String, String> headers;
        List<String> extraHeaders = new Vector<String>();
        String advertiserIdString =
                httpParams.get(BidderGrapherHTTPHandler.ADVERTISER_ID_PARAM);
        Long advertiserId = null;
        if(advertiserIdString != null)
            advertiserId = Long.parseLong(advertiserIdString);
        String campaignIdString =
                httpParams.get(BidderGrapherHTTPHandler.CAMPAIGN_ID_PARAM);
        Long campaignId = null;
        if(campaignIdString != null)
            campaignId = Long.parseLong(campaignIdString);
        extraHeaders.add("\n<SCRIPT TYPE=\"text/javascript\" " +
                         "SRC=\"/cbo/assets/form-verification.js\"></SCRIPT>");
        Bidder bidder = ensureBidder();
        boolean downloadP = httpParams.get(DOWNLOAD_PARAM) != null;
        Map<Long, AdvertiserData> advertiserDataMap =
                bidder.getAdvertiserMap();
        if(downloadP && advertiserDataMap != null)
        {
            Date startTime;
            Date endTime;
            if(LAST_7_DAYS.equals(timePeriod))
            {
                endCal.set(Calendar.HOUR, 23);
                endCal.set(Calendar.MINUTE, 59);
                endCal.set(Calendar.SECOND, 59);
                endCal.set(Calendar.MILLISECOND, 999);
                endCal.add(Calendar.DATE, -1);
                endTime = endCal.getTime();
                startCal.setTime(endTime);
                startCal.set(Calendar.HOUR, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                startCal.set(Calendar.MILLISECOND, 0);
                startCal.add(Calendar.DATE, -7);
                startTime = startCal.getTime();
            }
            else if(LAST_WHOLE_WEEK.equals(timePeriod))
            {
                endTime = AppNexusUtils.weekFloor(now);
                endCal.setTime(endTime);
                endCal.add(Calendar.MILLISECOND, -1);
                endTime = endCal.getTime();
                startTime = AppNexusUtils.weekFloor(endTime);
            }
            else if(LAST_30_DAYS.equals(timePeriod))
            {
                endCal.set(Calendar.HOUR, 23);
                endCal.set(Calendar.MINUTE, 59);
                endCal.set(Calendar.SECOND, 59);
                endCal.set(Calendar.MILLISECOND, 999);
                endCal.add(Calendar.DATE, -1);
                endTime = endCal.getTime();
                startCal.setTime(endTime);
                startCal.set(Calendar.HOUR, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                startCal.set(Calendar.MILLISECOND, 0);
                startCal.add(Calendar.DATE, -30);
                startTime = startCal.getTime();
            }
            else if(LAST_WHOLE_MONTH.equals(timePeriod))
            {
                endTime = AppNexusUtils.monthFloor(now);
                endCal.setTime(endTime);
                endCal.add(Calendar.MILLISECOND, -1);
                endTime = endCal.getTime();
                startTime = AppNexusUtils.monthFloor(endTime);
            }
            else if(WHOLE_CAMPAIGN.equals(timePeriod))
            {
                endTime = null;
                startTime = null;
            }
            else throw Utils.barf("Unknown time period.", this, timePeriod);
            List<PeriodicPerformanceData> perfData  =
                    PeriodicPerformanceData.getPeriodicPerformanceData
                        (startTime, endTime, advertiserId, campaignId,
                         bidder, qctx);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            downloadSpreadsheetForSchema(baos, perfData, qctx, schema);
            baos.flush();
            baos.close();
            byte[] ba = baos.toByteArray();
            headers = HTTPListener.emitStandardHeaders
                            (stream, 200, HTTPListener.application_xls,
                             (long) ba.length, null, false, returnHeaders);
            stream.flush();
            os.write(ba);
            os.flush();
        }
        else if(advertiserDataMap != null)
        {
            headers = handlerPageSetup
                    (stream, "Performance Comparison", null, stylesheetUrl,
                     extraHeaders, returnHeaders, prettyName, httpParams);
            List<AdvertiserData> advertisers =
                    new ArrayList(advertiserDataMap.values());
            stream.append("\n<FORM METHOD=\"POST\" NAME=\"" + DEFAULT_FORM_NAME
                          + "\" ACTION=\"");
            stream.append(urlName);
            stream.append("?\">");
            stream.append("\n<INPUT TYPE=\"HIDDEN\" NAME=\"Format\" VALUE=\"");
            stream.append(Integer.toString
                            (HTTPHandler.formatIndex(HTTPHandler.XLS)));
            stream.append("\">");
            stream.append("\n<SELECT NAME=\"" + TIME_PERIOD_PARAM + "\">");
            for(String item: TIME_PERIOD_MENU_ITEMS)
            {
                stream.append("\n  <OPTION>");
                stream.append(item);
            }
            stream.append("</SELECT>");
            outputDownloadButton(stream);
            /*
            stream.append("<INPUT TYPE=\"SUBMIT\" NAME=\"");
            stream.append(DOWNLOAD_PARAM);
            stream.append("\" VALUE=\"Download Spreadsheet\">");
            */
            if(advertisers.size() == 0)
                stream.append
                    ("\n<H3>No campaigns match the filter settings</H3>");
            stream.append("\n</FORM>");
            HTMLifier.finishHTMLPage(stream);
        }
        else
        {
            headers = handlerPageSetup
                    (stream, "Dashboard", null, stylesheetUrl,
                     extraHeaders, returnHeaders, prettyName, httpParams);
            stream.append
                ("<i>The Bidder has not run, so the advertisers/campaigns " +
                 "have not been loaded.  Please try again later.</i>");
            HTMLifier.finishHTMLPage(stream);
        }
        return headers;
    }

    public static void register()
    {
        HTTPListener.registerHandlerClass
                (PeriodicPerformanceComparisonHTTPHandler.class);
    }
    static
    { register(); }
}

