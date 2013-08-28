package com.tumri.cbo.backend;

import com.tumri.af.context.TimeScale;
import com.tumri.mediabuying.zini.*;
import java.io.*;
import java.util.*;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class CampaignTabulationHTTPHandler extends BidderGrapherHTTPHandler {

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        HTTPListener.registerHandler(getUrlName(), this);
    }

    static String urlName = "CAMPAIGNTABULATOR";
    static String prettyName = "Campaign Data Tabulation";
    @SuppressWarnings("unused")
    static final String TIMEZONE_TITLE = "Timezone: ";
    @SuppressWarnings("unused")
    static final String TIMEZONE = "timeZone";

    @SuppressWarnings("unused")
    public CampaignTabulationHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public CampaignTabulationHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, urlName, prettyName);
        setBidder(bidder);
    }

    public boolean isAdminUserCommand() { return true; }

    @SuppressWarnings("unchecked")
    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        boolean hoursSpringLoaded = false;
        Bidder bidder = ensureBidder();
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        QueryContext qctx = connector.sufficientQueryContextFor();
        String stylesheetUrl = null;
        Map<String, String> headers =
                outputHeaderStuff(stream, stylesheetUrl, returnHeaders,
                                  httpParams);
        Sexpression advertisers = getAdvertisers(connector, qctx);
        Sexpression currentAdvertiser =
                outputAdvertiserMenu(stream, advertisers, httpParams);
        TimeScale ts = TimeScale.HOURLY;
        //=================
        if(currentAdvertiser != Null.nil)
        {
            Sexpression campaigns =
                    getCampaigns(currentAdvertiser.car(), qctx);
            if(campaigns != Null.nil)
            {
                Map<String, List<Sexpression>> kidsMap
                        = new HashMap<String, List<Sexpression>>();
                Sexpression currentCampaign =
                       outputParentCampaignMenu(stream, campaigns, httpParams,
                                                kidsMap, false);
                // System.out.println("CurrentCampaign: " + currentCampaign);
                if(currentCampaign != Null.nil)
                {
                    Sexpression hours = getHours(currentAdvertiser.car(),
                                                 currentCampaign.car(), qctx);
                    hours = Cons.sort(hours, Cons.Lessp, Cons.identityKey);
                    TimeZoneData tzd =
                            BidHistoryHTTPHandler.outputTimeZoneWidgets
                                    (stream, bidder,
                                     currentAdvertiser.car().unboxLong(),
                                     currentCampaign.car().unboxLong(),
                                     httpParams, qctx);
                    Sexpression currentStartHour =
                         outputHourMenu(stream, hours, START_HOUR,
                                        START_HOUR_TITLE, httpParams, false,
                                        hoursSpringLoaded, ts);
                    Sexpression currentEndHour =
                         outputHourMenu(stream, hours, END_HOUR,
                                        END_HOUR_TITLE, httpParams, true,
                                        hoursSpringLoaded, ts);
                    stream.append("<H3>");
                    stream.append("</H3>");
                    // System.out.println("Start Hour: " + currentStartHour);
                    // System.out.println("End Hour: " + currentEndHour);
                    // System.out.println("Hours: " + hours);
                    if(currentStartHour != Null.nil &&
                       currentEndHour != Null.nil &&
                       !currentStartHour.equals(currentEndHour))
                    {
                        if(!Sexpression.Lessp.test
                                (currentStartHour, currentEndHour))
                        {
                            Sexpression temp = currentStartHour;
                            currentStartHour = currentEndHour;
                            currentEndHour = temp;
                        }
                        Sexpression campaignName = currentCampaign.second();
                        String[] labels = histDataQueryResultColumnNames;
                        String campaignNameS = campaignName.unboxString();
                        List<Sexpression> kidsV = kidsMap.get(campaignNameS);
                        stream.append("\n<HR>\n");
                        Object data =
                            (kidsV == null
                                ? null
                                : getHistoricalDataTable
                                    (currentAdvertiser, currentCampaign,
                                     currentStartHour, currentEndHour, kidsV,
                                     qctx));
                        TimeZone localTimeZone = tzd.getLocalTimeZone();
                        TimeZone wrtTimeZone = tzd.getWrtTimeZone();
                        if(data instanceof Sexpression)
                            tabulate(stream, (Sexpression) data, localTimeZone,
                                     wrtTimeZone, qctx, labels);
                        else if(data instanceof List)
                        {
                            List<Sexpression> list = (List<Sexpression>) data;
                            int i = 0;
                            for(Sexpression s: list)
                            {
                                if(i > 0) stream.append("\n<HR>\n");
                                tabulate(stream, s, localTimeZone,
                                         wrtTimeZone, qctx, labels);
                                i = i + 1;
                            }
                        }
                    }
                    else warn(stream,
                              "No valid dates found for this campaign!");
                }
                else warn(stream, "No campaign found for this advertiser!");
            }
            else warn(stream, "No campaigns for this advertiser!");
        }
        else warn(stream, "No advertiser found!");
        HTMLifier.finishHTMLPage(stream);
        return headers;
    }

    static void tabulate(Writer stream, Sexpression s,
                         TimeZone localTImeZone, TimeZone wrtTimezone,
                         QueryContext qctx, String[] labels)
            throws IOException
    {
        if(s instanceof Atom){} // Nothing to do
        else
        {
            stream.append("\n<TABLE BORDER=\"1\">");
            if(labels != null)
            {
                stream.append("\n  <TR>");
                for(String label: labels)
                {
                    stream.append("\n    <TH>");
                    stream.append(escapeHtml(label));
                    stream.append("\n    </TH>");
                }
                stream.append("\n  </TR>");
            }
            while(s != Null.nil)
            {
                Sexpression row = s.car();
                stream.append("\n  <TR>");
                while(row != Null.nil)
                {
                    Sexpression cell = row.car();
                    stream.append("\n    <TD>");
                    Object mapped = SQLHTTPHandler.mappedObjects(cell, qctx);
                    stream.append(SQLHTTPHandler.pprintCell
                                        (mapped, cell, localTImeZone,
                                         wrtTimezone, null));
                    stream.append("\n    </TD>");
                    row = row.cdr();
                }
                stream.append("\n  </TR>");
                s = s.cdr();
            }
            stream.append("\n</TABLE>");
        }
    }

    static Object getHistoricalDataTable
            (Sexpression currentAdvertiser,
             Sexpression currentCampaign, Sexpression currentStartHour,
             Sexpression currentEndHour, List<Sexpression> kidsV,
             QueryContext qctx)
            throws IOException
    {
        switch (kidsV.size())
        {
            case 0: throw Utils.barf("No campaigns found.", null,
                                     currentAdvertiser, currentCampaign);
            case 1: return getHistoricalData
                                (currentAdvertiser, currentCampaign,
                                 currentStartHour, currentEndHour, qctx);
            default: List<Sexpression> histDataV = new Vector<Sexpression>();
                     for(Sexpression campaign: kidsV)
                     {
                         histDataV.add(getHistoricalData
                                        (currentAdvertiser, campaign,
                                         currentStartHour, currentEndHour,
                                         qctx));
                     }
                     return totaliseCurvesByHour(histDataV, INDEX_OF_HOUR);
        }
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
        HTTPListener.registerHandlerClass(CampaignTabulationHTTPHandler.class);
    }
    static
    { register(); }
}

