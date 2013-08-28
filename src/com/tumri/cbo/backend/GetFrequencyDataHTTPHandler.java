package com.tumri.cbo.backend;

import com.tumri.mediabuying.appnexus.AppNexusInterface;
import com.tumri.mediabuying.appnexus.ReportInterval;
import com.tumri.mediabuying.appnexus.services.ReportNetworkAdvertiserFrequencyRecencyService;
import com.tumri.mediabuying.zini.*;
import java.io.*;
import java.util.*;

public class GetFrequencyDataHTTPHandler extends HTTPHandler {

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(getUrlName(), this);
    }

    Bidder bidder = null;

    public void setBidder(Bidder b)
    {
        bidder = b;
    }

    static String urlName = "GETFREQUENCYDATA";
    static String prettyName = "Get Frequency Data";

    @SuppressWarnings("unused")
    public GetFrequencyDataHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    @SuppressWarnings("unused")
    public GetFrequencyDataHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, urlName, prettyName);
        setBidder(bidder);
    }

    public static final String REPORT_INTERVAL_PARAM = "reportInterval";

    static ReportInterval outputReportIntervalMenu
            (Writer stream, ReportInterval[] reportIntervals,
             Map<String, String> httpParams)
            throws IOException
    {
        return outputReportIntervalMenu
                    (stream, reportIntervals, httpParams, "Report interval: ",
                     REPORT_INTERVAL_PARAM, true);
    }

    static ReportInterval defaultReportInterval = ReportInterval.last_7_days;

    static ReportInterval outputReportIntervalMenu
            (Writer stream, ReportInterval[] reportIntervals,
             Map<String, String> httpParams, String title, String param,
             boolean springLoaded) throws IOException
    {
        stream.append("\n<H3>");
        stream.append(title);
        stream.append("<SELECT NAME=\"");
        stream.append(param);
        stream.append("\"");
        if(springLoaded) stream.append(" onChange=\"{ form.submit(); }\">");
        stream.append(">");
        ReportInterval currentReportInterval = reportIntervals[0];
        String currentReportIntervalString = httpParams.get(param);
        for(ReportInterval reportInterval: reportIntervals)
        {
            String htmlified = htmlify(reportInterval.toString());
            stream.append("\n<OPTION VALUE=\"");
            stream.append(htmlified);
            stream.append("\"");
            if(htmlified.equals(currentReportIntervalString) ||
               (currentReportIntervalString == null &&
                htmlified.equals(defaultReportInterval.toString())))
            {
                currentReportInterval = reportInterval;
                stream.append(" SELECTED");
            }
            stream.append(">");
            stream.append(htmlified);
            // stream.append(" (");stream.append(htmlified);stream.append(")");
        }
        stream.append("</SELECT></H3>");
        return currentReportInterval;
    }

    public boolean isAdminUserCommand() { return true; }

    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream, OutputStream os,
             Map<String, String> httpParams, boolean returnHeaders)
            throws IOException
    {
        String stylesheetUrl = null;
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        QueryContext qctx = connector.sufficientQueryContextFor();
        Map<String, String> headers =
                outputHeaderStuff(stream, stylesheetUrl,
                                  returnHeaders, httpParams);
        Sexpression advertisers =
                BidderGrapherHTTPHandler.getAdvertisers(connector, qctx);
        Sexpression currentAdvertiser =
                BidderGrapherHTTPHandler.outputAdvertiserMenu
                        (stream, advertisers, httpParams);
        ReportInterval[] reportIntervals =
            AppNexusInterface.legalReportingIntervals
                    (ReportNetworkAdvertiserFrequencyRecencyService.class);
        ReportInterval currentReportInterval =
                outputReportIntervalMenu
                        (stream, reportIntervals, httpParams);
        stream.append("\n<H3><INPUT TYPE=\"SUBMIT\" NAME=\"" + DOIT_PARAM +
                                "\" VALUE=\"Do It\"></H3>");
        boolean doitP = httpParams.get(DOIT_PARAM) != null;
        //=================
        if(currentAdvertiser != Null.nil)
        {
            Long advertiserId = currentAdvertiser.car().unboxLong();
            if(doitP)
            {
                int rowCount = bidder.fetchAndSaveAdvertiserFrequencyReport
                                    (advertiserId, currentReportInterval);
                stream.append("Rows loaded: ");
                stream.append(Integer.toString(rowCount));
            }
        }
        else stream.append("<i>No known advertisers!</i>");
        return headers;
    }

    Map<String, String> outputHeaderStuff
            (Writer stream, String stylesheetUrl, boolean returnHeaders,
             Map<String, String> httpParams)
            throws IOException
    {
        String title = prettyName;
        return handlerPageSetup
                (stream, title, urlName, stylesheetUrl, returnHeaders,
                 httpParams);
    }

    public static void register()
    {
        HTTPListener.registerHandlerClass(GetFrequencyDataHTTPHandler.class);
    }
    static
    { register(); }
}

