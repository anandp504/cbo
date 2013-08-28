package com.tumri.cbo.backend;

import com.tumri.af.context.TimeScale;
import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.zini.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;

public class BidderGrapherHTTPHandler extends HTTPHandler {

    static final String START_HOUR = "StartHour";
    static final String END_HOUR = "EndHour";
    static final String START_HOUR_TITLE = "Start";
    static final String END_HOUR_TITLE = "End";
    static final String IMPRESSIONS_GRAPH_TYPE = "Impressions";
    static final String CLICK_GRAPH_TYPE = "Clicks";
    static final String COST_GRAPH_TYPE = "Cost";
    static final int INDEX_OF_HOUR = 0;
    static final int INDEX_OF_EVENT_TIME = 0;
    static final String BID_HISTORY_GRAPH_TYPE = "Bid History";
    public static final double MS_PER_DAY = 86400000.0d;

    static String[] graphTypes =
           { IMPRESSIONS_GRAPH_TYPE, CLICK_GRAPH_TYPE, COST_GRAPH_TYPE,
             BID_HISTORY_GRAPH_TYPE };

    Bidder bidder = null;

    public void setBidder(Bidder b)
    {
        bidder = b;
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
        HTTPListener.registerHandler(getUrlName(), this);
    }

    static String urlName = "BIDDERGRAPHER";
    static String prettyName = "Bidder Grapher"; 

    @SuppressWarnings("unused")
    public BidderGrapherHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public BidderGrapherHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, urlName, prettyName);
        setBidder(bidder);
    }

    public BidderGrapherHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, String urlName,
             String prettyName)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public static Sexpression getAdvertisers
            (SQLConnector connector, QueryContext qctx)
    {
        // This is philosophically the right query, but MySQl is too slow, so
        // optimise it manually.
        /*
        Sexpression query =
                Sexpression.readFromString
                  ("(ask-all (?id ?name) (and (CBO_DB.HistoricalData ?id) " +
                                             "(CBO_DB.AdvertiserNames ?id ?name)))");
        return Utils.interpretACL(Integrator.INTEGRATOR, query, qctx);
        */
        String query = 
              "SELECT advID AS Id,\n" +
              "       (SELECT advertisernames.name\n" +
              "        FROM advertisernames\n" +
              "        WHERE advertisernames.id=advID) AS Name\n" +
              "FROM (SELECT DISTINCT historicaldata.advertiser_id AS advID\n" +
              "      FROM historicaldata) T1\n" +
              "ORDER BY Name;";
        Sexpression res = connector.runSQLQuery(query, qctx);
        SexpLoc loc = new SexpLoc();
        while(res != Null.nil)
        {
            Sexpression row = res.car();
            if(row.second() != Null.nil)
                loc.collect(row);
            res = res.cdr();
        }
        return loc.getSexp();
    }

    static SentenceTemplate getCampaignsQuery =
            new SentenceTemplate(
                    "(ask-all (?id ?name)\n" +
                    "    (and (CBO_DB.HistoricalData ?AdvertiserId ?li ?id)\n"+
                    "         (CBO_DB.CampaignNames ?id ?name)))");

    Sexpression getCampaigns(Sexpression advertiserId, QueryContext qctx)
    {
        BindingList bl = BindingList.truth();
        bl.bind("?AdvertiserId", advertiserId);
        List<Sexpression> instantiated = getCampaignsQuery.instantiate(bl);
        Sexpression query = instantiated.get(0);
        return Utils.interpretACL(Integrator.INTEGRATOR, query, qctx);
    }

    static SentenceTemplate getAdvertisersAndCampaignsQuery =
            new SentenceTemplate(
                 "(ask-all (?AdvertiserId ?CampaignId)\n" +
                 "    (CBO_DB.HistoricalData ?AdvertiserId ?li ?CampaignId))");

    static Sexpression getAdvertisersAndCampaigns(QueryContext qctx)
    {
        BindingList bl = BindingList.truth();
        List<Sexpression> instantiated =
                getAdvertisersAndCampaignsQuery.instantiate(bl);
        Sexpression query = instantiated.get(0);
        return Utils.interpretACL(Integrator.INTEGRATOR, query, qctx);
    }

    static SentenceTemplate getHoursQuery =
            new SentenceTemplate(
                   "(ask-all ?hour (CBO_DB.HistoricalData ?AdvertiserId ?li " +
                                                     "?CampaignId ?hour)))");

    static SentenceTemplate oneofGetHoursQuery =
            new SentenceTemplate(
                   "(ask-all ?hour (and (oneof ?CampaignId @CampaignIds)" +
                                      " (CBO_DB.HistoricalData ?AdvertiserId ?li " +
                                                     "?CampaignId ?hour))))");

    static Sexpression getHours(Sexpression advertiserId, Sexpression campaignId,
                                QueryContext qctx)
    {
        BindingList bl = BindingList.truth();
        bl.bind("?AdvertiserId", advertiserId);
        if(campaignId instanceof Cons)
            bl.bind("@CampaignIds", campaignId);
        else bl.bind("?CampaignId", campaignId);
        List<Sexpression> instantiated =
             (campaignId instanceof Cons ? oneofGetHoursQuery : getHoursQuery).
                     instantiate(bl);
        Sexpression query = instantiated.get(0);
        return Utils.interpretACL(Integrator.INTEGRATOR, query, qctx);
    }

    public boolean isAdminUserCommand() { return true; }

    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        boolean hoursSpringLoaded = false;
        boolean timeScaleSpringLoaded = false;
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
                       outputParentCampaignMenu
                               (stream, campaigns, httpParams, kidsMap, false);
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
                    TimeScale ts =
                         BidHistoryHTTPHandler.outputTimeScaleMenu
                                 (stream, BidHistoryHTTPHandler.TIMESCALE,
                                  BidHistoryHTTPHandler.TIMESCALE_TITLE,
                                  httpParams, false, timeScaleSpringLoaded);
                    Sexpression currentStartHour =
                         outputHourMenu(stream, hours, START_HOUR,
                                        START_HOUR_TITLE, httpParams, false,
                                        tzd.getLocalTimeZone(),
                                        tzd.getWrtTimeZone(),
                                        hoursSpringLoaded, ts);
                    Sexpression currentEndHour =
                         outputHourMenu(stream, hours, END_HOUR,
                                        END_HOUR_TITLE, httpParams, true,
                                        tzd.getLocalTimeZone(),
                                        tzd.getWrtTimeZone(),
                                        hoursSpringLoaded, ts);
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
                        //System.out.println("Start Hour: " + currentStartHour);
                        // System.out.println("End Hour: " + currentEndHour);
                        Sexpression campaignName = currentCampaign.second();
                        String campaignNameS = campaignName.unboxString();
                        List<Sexpression> kidsV = kidsMap.get(campaignNameS);
                        String graphType =
                                outputGraphTypeMenu(stream, httpParams);
                                /*
                                (kidsV.size() == 1
                                    ? null
                                    : outputGraphTypeMenu(stream, httpParams));
                                    */
                        stream.append("\n<HR>\n");
                        if(kidsV == null) {}
                        else if(graphType == null) // kidsV.size() == 1, too
                            outputCampaignPerformanceGraph
                                    (stream, currentAdvertiser,
                                     currentCampaign, currentStartHour,
                                     currentEndHour, kidsV, qctx, ts,
                                     tzd.getWrtTimeZone());
                        else if(graphType.equals(IMPRESSIONS_GRAPH_TYPE))
                            outputImpressionsGraph
                                    (stream, currentAdvertiser,
                                     currentCampaign, currentStartHour,
                                     currentEndHour, kidsV, qctx);
                        else if(graphType.equals(CLICK_GRAPH_TYPE))
                            outputClickGraph
                                    (stream, currentAdvertiser,
                                     currentCampaign, currentStartHour,
                                     currentEndHour, kidsV, qctx);
                        else if(graphType.equals(COST_GRAPH_TYPE))
                            outputCostGraph
                                    (stream, currentAdvertiser,
                                     currentCampaign, currentStartHour,
                                     currentEndHour, kidsV, qctx);
                        else if(graphType.equals(BID_HISTORY_GRAPH_TYPE))
                            outputBidHistoryGraph
                                    (stream, currentAdvertiser,
                                     currentCampaign, currentStartHour,
                                     currentEndHour, kidsV, qctx);
                        else {}
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

    void warn(Writer stream, String str)
            throws IOException
    {
        stream.append("\n<H3>");
        stream.append(str);
        stream.append("</H3>");
    }

    static SentenceTemplate getHistDataQuery =
            new SentenceTemplate(
                   "(ask-all (?hour ?imps ?clicks ?cost)" +
                     " (and (CBO_DB.HistoricalData" +
                             " ?AdvertiserId ?li ?CampaignId ?hour" +
                             " ?imps ?clicks ?cost)" +
                           "(>= ?hour ?StartHour) (=< ?hour ?EndHour)))");

    public static String[] histDataQueryResultColumnNames = 
            { "Hour", "Impressions", "Clicks", "Cost" };

    static Sexpression getHistoricalData
            (Sexpression currentAdvertiser, Sexpression currentCampaign,
             Sexpression currentStartHour, Sexpression currentEndHour,
             QueryContext qctx)
    {
        BindingList bl = BindingList.truth();
        bl.bind("?AdvertiserId", currentAdvertiser.car());
        bl.bind("?CampaignId", currentCampaign.car());
        bl.bind("?StartHour", currentStartHour);
        bl.bind("?EndHour", currentEndHour);
        List<Sexpression> instantiated = getHistDataQuery.instantiate(bl);
        Sexpression query = instantiated.get(0);
        return Utils.interpretACL(Integrator.INTEGRATOR, query, qctx);
    }

    static SentenceTemplate getBidHistoryDataQuery =
            new SentenceTemplate(
                   "(ask-all (?eventtime ?bid ?dailyImpressionLimit)" +
                     " (and (CBO_DB.BidHistory" +
                             " ?AdvertiserId ?li ?CampaignId ?bid ?dailyImpressionLimit ?eventtime)" +
                           "(>= ?eventtime ?StartHour) (=< ?eventtime ?EndHour)))");

    static Sexpression getBidHistoryData
            (Sexpression currentAdvertiser, Sexpression currentCampaign,
             Sexpression currentStartHour, Sexpression currentEndHour,
             QueryContext qctx)
    {
        BindingList bl = BindingList.truth();
        bl.bind("?AdvertiserId", currentAdvertiser.car());
        bl.bind("?CampaignId", currentCampaign.car());
        bl.bind("?StartHour", currentStartHour);
        bl.bind("?EndHour", currentEndHour);
        List<Sexpression> instantiated =getBidHistoryDataQuery.instantiate(bl);
        Sexpression query = instantiated.get(0);
        return Utils.interpretACL(Integrator.INTEGRATOR, query, qctx);
    }

    static StringBuffer collectHourPoints(Sexpression histData)
    {
        StringBuffer xPoints = new StringBuffer();
        Date firstHour = null;
        boolean firstP;
        Sexpression l;
        //-------------------
        firstP = true;
        l = histData;
        while(l != Null.nil)
        {
            Sexpression row = l.car();
            Date hour = row.car().unboxDate();
            if(firstHour == null) firstHour = hour;
            if(firstP) firstP = false;
            else xPoints.append(",");
            xPoints.append
                    ((hour.getTime() - firstHour.getTime()) / MS_PER_DAY);
            l = l.cdr();
        }
        return xPoints;
    }

    static StringBuffer collectTimePointStrings
            (Sexpression histData, TimeScale timeScale)
    {
        StringBuffer xPoints = new StringBuffer();
        boolean firstP;
        Sexpression l;
        //-------------------
        firstP = true;
        l = histData;
        SimpleDateFormat format = 
                TimeScaleIterator.getFormats().get(timeScale);
        while(l != Null.nil)
        {
            Sexpression row = l.car();
            Date hour = row.car().unboxDate();
            if(firstP) firstP = false;
            else xPoints.append(",");
            xPoints.append(format.format(hour));
            l = l.cdr();
        }
        return xPoints;
    }

    static StringBuffer collectMeasurePoints
            (Sexpression histData, int measureIndex)
    {
        StringBuffer measurePoints = new StringBuffer();
        return collectMeasurePoints(histData, measureIndex, measurePoints);
    }

    static StringBuffer collectMeasurePoints
           (Sexpression histData, int measureIndex, StringBuffer measurePoints)
    {
        boolean firstP;
        firstP = true;
        Sexpression l = histData;
        while(l != Null.nil)
        {
            Sexpression row = l.car();
            Double val = Sexpression.nth(measureIndex + 1, row).unboxDouble();
            if(firstP) firstP = false;
            else measurePoints.append(",");
            measurePoints.append(val.toString());
            l = l.cdr();
        }
        return measurePoints;
    }

    static String[] suffices =
            { "", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };

    static String simplifyMeasureName(String mn)
    {
        if(AppNexusCampaignSplitter.isChildCampaign(mn))
           return Integer.toString(AppNexusCampaignSplitter.getSplitIndex(mn));
        else return mn;
    }

    static void emitCampaignPerformanceGraph
            (Writer stream, Sexpression currentAdvertiser,
             Sexpression currentCampaign, Sexpression histData,
             int[] measureIndices, String[] measureNames,
             TimeScale timeScale, TimeZone tz)
            throws IOException
    {
        String title = currentAdvertiser.second().unboxString() + "/" +
                         currentCampaign.second().unboxString();
        stream.append(GraphPerspective.appletTagStart());
        stream.append
             ("\n            code=\"com.tumri.cbo.applets.graph.CampaignPerformancePlot.class\">");
        StringBuffer xPoints = collectTimePointStrings(histData, timeScale);
        //-------------------
        stream.append("\n        <PARAM NAME=\"title\" VALUE=\"");
        stream.append(title);
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"timeScale\" VALUE=\"");
        stream.append(timeScale.toString());
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"timeZone\" VALUE=\"");
        stream.append(tz.getDisplayName());
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"xAxisName\" VALUE=\"");
        stream.append("Time");
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"xPoints\" VALUE=\"");
        stream.append(xPoints.toString());
        stream.append("\">");
        for(int i = 0; i < measureIndices.length; i++)
        {
            StringBuffer yPoints =
                    collectMeasurePoints(histData, measureIndices[i]);
            stream.append("\n        <PARAM NAME=\"yAxisName");
            stream.append(Integer.toString(i));
            stream.append("\" VALUE=\"");
            stream.append(measureNames[i]);
            stream.append("\">");
            stream.append("\n        <PARAM NAME=\"yPoints");
            stream.append(Integer.toString(i));
            stream.append("\" VALUE=\"");
            stream.append(yPoints.toString());
            stream.append("\">");
        }
        stream.append("\n    A graph should show here!");
        stream.append("\n    </APPLET>");
    }

    static void emit2DGraph(Writer stream, Sexpression currentAdvertiser,
                            Sexpression currentCampaign, Sexpression histData,
                            int measureIndex, String measureName)
            throws IOException
    {
        String title = currentAdvertiser.second().unboxString() + "/" +
                         currentCampaign.second().unboxString() + "/" +
                         measureName;
        stream.append(GraphPerspective.appletTagStart());
        stream.append
             ("\n            code=\"com.tumri.cbo.applets.graph.Simple2DGraph.class\">");
        StringBuffer xPoints = collectHourPoints(histData);
        StringBuffer yPoints = collectMeasurePoints(histData, measureIndex);
        //-------------------
        stream.append("\n        <PARAM NAME=\"title\" VALUE=\"");
        stream.append(title);
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"name\" VALUE=\"");
        stream.append(measureName);
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"xPoints\" VALUE=\"");
        stream.append(xPoints.toString());
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"yPoints\" VALUE=\"");
        stream.append(yPoints.toString());
        stream.append("\">");
        stream.append("\n    A graph should show here!");
        stream.append("\n    </APPLET>");
    }

    static void emit3DGraph(Writer stream, Sexpression currentAdvertiser,
                            List<Sexpression> campaigns,
                            List<Sexpression> histDataV,
                            int measureIndex, String measureName)
            throws IOException
    {
        Sexpression currentCampaign = campaigns.get(0);
        String title = currentAdvertiser.second().unboxString() + "/" +
                         currentCampaign.second().unboxString() + "/" +
                         measureName;
        stream.append(GraphPerspective.appletTagStart());
        stream.append
             ("\n            code=\"com.tumri.cbo.applets.graph.Simple3DGraph.class\">");
        stream.append("\n        <PARAM NAME=\"title\" VALUE=\"");
        stream.append(title);
        stream.append("\">");
        int i = 0;
        for(Sexpression campaign: campaigns)
        {
            Sexpression histData = histDataV.get(i);
            String suffix = suffices[i];
            StringBuffer xPoints = collectHourPoints(histData);
            StringBuffer yPoints = Graph.commaSeparateLength
                                        (i, histData.length());
            StringBuffer zPoints = collectMeasurePoints(histData,measureIndex);
            stream.append("\n        <PARAM NAME=\"name");
            stream.append(suffix);
            stream.append("\" VALUE=\"");
            stream.append(simplifyMeasureName(campaign.second().unboxString()));
            stream.append("/");
            stream.append(measureName);
            stream.append("\">");
            stream.append("\n        <PARAM NAME=\"xPoints");
            stream.append(suffix);
            stream.append("\" VALUE=\"");
            stream.append(xPoints.toString());
            stream.append("\">");
            stream.append("\n        <PARAM NAME=\"yPoints");
            stream.append(suffix);
            stream.append("\" VALUE=\"");
            stream.append(yPoints.toString());
            stream.append("\">");
            stream.append("\n        <PARAM NAME=\"zPoints");
            stream.append(suffix);
            stream.append("\" VALUE=\"");
            stream.append(zPoints.toString());
            stream.append("\">");
            i = i + 1;
        }
        stream.append("\n    A graph should show here!");
        stream.append("\n    </APPLET>");
    }

    @SuppressWarnings("unused")
    static void emit3DGrid(Writer stream, Sexpression currentAdvertiser,
                           List<Sexpression> campaigns,
                           List<Sexpression> histDataV,
                           int measureIndex, String measureName)
            throws IOException
    {
        Sexpression currentCampaign = campaigns.get(0);
        String title = currentAdvertiser.second().unboxString() + "/" +
                         currentCampaign.second().unboxString() + "/" +
                         measureName;
        stream.append(GraphPerspective.appletTagStart());
        stream.append
             ("\n            code=\"com.tumri.cbo.applets.graph.Simple3DGrid.class\">");
        stream.append("\n        <PARAM NAME=\"title\" VALUE=\"");
        stream.append(title);
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"name\" VALUE=\"");
        stream.append(title);
        stream.append("/");
        stream.append(measureName);
        stream.append("\">");
        // Hist data must have been totalised.
        Sexpression histData = histDataV.get(0);
        StringBuffer xPoints = collectHourPoints(histData);
        StringBuffer yPoints = Graph.commaSeparateI(campaigns.size());
        stream.append("\n        <PARAM NAME=\"xPoints\" VALUE=\"");
        stream.append(xPoints.toString());
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"yPoints\" VALUE=\"");
        stream.append(yPoints.toString());
        stream.append("\">");
        StringBuffer zBuffer = new StringBuffer();
        for(int i = 0; i < campaigns.size(); i++)
        {
            if(i > 0) zBuffer.append(",");
            histData = histDataV.get(i);
            collectMeasurePoints(histData, measureIndex, zBuffer);
        }
        stream.append("\n        <PARAM NAME=\"zPoints\" VALUE=\"");
        stream.append(zBuffer.toString());
        stream.append("\">");
        stream.append("\n    A graph should show here!");
        stream.append("\n    </APPLET>");
    }

    static List<Sexpression> totaliseCurvesByHour
                                (List<Sexpression> curves, int wrtIndex)
    {
        Date minDate = null;
        Date maxDate = null;
        for(Sexpression curve: curves)
        {
            while(curve != Null.nil)
            {
                Date d = Sexpression.nth(wrtIndex, curve.car()).unboxDate();
                if(minDate == null) minDate = d;
                else if(d.compareTo(minDate) < 0) minDate = d;
                else {}
                if(maxDate == null) maxDate = d;
                else if(d.compareTo(maxDate) > 0) maxDate = d;
                else {}
                curve = curve.cdr();
            }
        }
        if(minDate == null)
            throw Utils.barf("No dates found!", null, curves, wrtIndex);
        List<Sexpression> allDates = new Vector<Sexpression>();
        Date currentDate = minDate;
        for(long i= minDate.getTime(); i <= maxDate.getTime(); i = i + 3600000)
        {
            allDates.add(new DateAtom(currentDate));
            if(currentDate.getTime() >= maxDate.getTime()) break;
            currentDate = new Date(currentDate.getTime() + 3600000);
        }
        List<Sexpression> res = new Vector<Sexpression>();
        for(Sexpression curve: curves)
        {
            Sexpression resCurve = Null.nil;
            Sexpression tail = Null.nil;
            for(Sexpression date: allDates)
            {
                Sexpression val = Cons.assoc(date, curve);
                if(val == Null.nil)
                    val = Cons.list(date, NumberAtom.Zero, NumberAtom.Zero,
                                    NumberAtom.Zero);
                Sexpression newTail = Cons.list(val);
                if(resCurve == Null.nil)
                {
                    resCurve = newTail;
                    tail = newTail;
                }
                else
                {
                    tail.setCdr(newTail);
                    tail = newTail;
                }
            }
            res.add(resCurve);
        }
        return res;
    }

    static void outputHistoricalDataGraph
            (Writer stream, Sexpression currentAdvertiser,
             Sexpression currentCampaign, Sexpression currentStartHour,
             Sexpression currentEndHour, List<Sexpression> kidsV,
             QueryContext qctx, String title, int measureIndex)
            throws IOException
    {
        if(kidsV == null)
            throw Utils.barf("No campaigns found.", null,
                                     currentAdvertiser, currentCampaign);
        {
            switch (kidsV.size())
            {
                case 0: throw Utils.barf("No campaigns found.", null,
                        currentAdvertiser, currentCampaign);
                case 1: Sexpression histData =
                        getHistoricalData
                               (currentAdvertiser, currentCampaign,
                                currentStartHour, currentEndHour,
                                qctx);
                    // System.out.println(histData);
                    emit2DGraph(stream, currentAdvertiser, currentCampaign,
                                histData, measureIndex, title);
                    break;
                default: List<Sexpression> histDataV =
                        new Vector<Sexpression>();
                    for(Sexpression campaign: kidsV)
                    {
                        histDataV.add(getHistoricalData
                                (currentAdvertiser, campaign,
                                 currentStartHour, currentEndHour, qctx));
                    }
                    List<Sexpression> totalHist =
                            totaliseCurvesByHour(histDataV, INDEX_OF_HOUR);
                    emit3DGraph(stream, currentAdvertiser, kidsV, totalHist,
                                measureIndex, title);
                    //emit3DGrid(stream, currentAdvertiser, kidsV, totalHist,
                    //           measureIndex, title);
                    break;
            }
        }
    }

    static void outputCampaignPerformanceGraph
            (Writer stream, Sexpression currentAdvertiser,
             Sexpression currentCampaign, Sexpression currentStartHour,
             Sexpression currentEndHour, List<Sexpression> kidsV,
             QueryContext qctx, TimeScale timeScale, TimeZone tz)
            throws IOException
    {
        switch (kidsV.size())
        {
            case 1: Sexpression histData =
                        getHistoricalData(currentAdvertiser, currentCampaign,
                                          currentStartHour, currentEndHour,
                                          qctx);
                    int[] measureIndices = { 0, 1, 2 };
                    String[] measureNames = { "Impressions", "Clicks", "Cost"};
                    emitCampaignPerformanceGraph
                        (stream, currentAdvertiser, currentCampaign, histData,
                         measureIndices, measureNames, timeScale, tz);
                    break;
            default: throw Utils.barf("Wrong number of campaigns found.", null,
                                      currentAdvertiser, currentCampaign);
        }
    }

    static void outputImpressionsGraph
            (Writer stream, Sexpression currentAdvertiser,
             Sexpression currentCampaign, Sexpression currentStartHour,
             Sexpression currentEndHour, List<Sexpression> kidsV,
             QueryContext qctx)
            throws IOException
    {
        outputHistoricalDataGraph
            (stream, currentAdvertiser, currentCampaign, currentStartHour,
             currentEndHour, kidsV, qctx, "Impressions", 0);
    }

    static void outputClickGraph
            (Writer stream, Sexpression currentAdvertiser,
             Sexpression currentCampaign, Sexpression currentStartHour,
             Sexpression currentEndHour, List<Sexpression> kidsV,
             QueryContext qctx)
            throws IOException
    {
        outputHistoricalDataGraph
            (stream, currentAdvertiser, currentCampaign, currentStartHour,
             currentEndHour, kidsV, qctx, "Clicks", 1);
    }

    static void outputCostGraph
            (Writer stream, Sexpression currentAdvertiser,
             Sexpression currentCampaign, Sexpression currentStartHour,
             Sexpression currentEndHour, List<Sexpression> kidsV,
             QueryContext qctx)
            throws IOException
    {
        outputHistoricalDataGraph
            (stream, currentAdvertiser, currentCampaign, currentStartHour,
             currentEndHour, kidsV, qctx, "Cost", 2);
    }

    static List<Sexpression> totaliseCurves
            (List<Sexpression> curves, int wrtIndex)
    {
        Date minDate = null;
        Date maxDate = null;
        Set<Sexpression> allDatesSet = new LinkedHashSet<Sexpression>();
        for(Sexpression curve: curves)
        {
            while(curve != Null.nil)
            {
                Sexpression ds= Sexpression.nth(wrtIndex, curve.car());
                Date d = ds.unboxDate();
                allDatesSet.add(ds);
                if(minDate == null) minDate = d;
                else if(d.compareTo(minDate) < 0) minDate = d;
                else {}
                if(maxDate == null) maxDate = d;
                else if(d.compareTo(maxDate) > 0) maxDate = d;
                else {}
                curve = curve.cdr();
            }
        }
        if(minDate == null)
            throw Utils.barf("No dates found!", null, curves, wrtIndex);
        List<Sexpression> allDates = new Vector<Sexpression>(allDatesSet);
        Collections.sort(allDates, Sexpression.comparator);
        List<Sexpression> res = new Vector<Sexpression>();
        for(Sexpression curve: curves)
        {
            Sexpression resCurve = Null.nil;
            Sexpression tail = Null.nil;
            Sexpression lastValue = NumberAtom.Zero;
            for(Sexpression date: allDates)
            {
                Sexpression val = Cons.assoc(date, curve);
                if(val == Null.nil)
                {
                    val = Cons.copyList(lastValue);
                    Cons.setNth(wrtIndex, val, date);
                }
                else lastValue = val;
                Sexpression newTail = Cons.list(val);
                if(resCurve == Null.nil)
                {
                    resCurve = newTail;
                    tail = newTail;
                }
                else
                {
                    tail.setCdr(newTail);
                    tail = newTail;
                }
            }
            res.add(resCurve);
        }
        return res;
    }

    public static void outputBidHistoryGraph
            (Writer stream, Sexpression currentAdvertiser,
             Sexpression currentCampaign, Sexpression currentStartHour,
             Sexpression currentEndHour, List<Sexpression> kidsV,
             QueryContext qctx)
            throws IOException
    {
        int measureIndex = 0;
        String title = "Bid history";
        switch (kidsV.size())
        {
            case 0: throw Utils.barf("No campaigns found.", null);
            case 1: Sexpression histData =
                        getBidHistoryData(currentAdvertiser, currentCampaign,
                                          currentStartHour, currentEndHour,
                                          qctx);
                    // System.out.println(histData);
                    emit2DGraph(stream, currentAdvertiser, currentCampaign,
                                histData, measureIndex, title);
                    break;
            default: List<Sexpression> histDataV = new Vector<Sexpression>();
                     for(Sexpression campaign: kidsV)
                     {
                         histDataV.add(getBidHistoryData
                                        (currentAdvertiser, campaign,
                                         currentStartHour, currentEndHour,
                                         qctx));
                     }
                     List<Sexpression> totalHist =
                             totaliseCurves(histDataV, INDEX_OF_EVENT_TIME);
                     emit3DGraph(stream, currentAdvertiser, kidsV, totalHist,
                                 measureIndex, title);
                     // System.out.println(histDataV);
                     break;
        }
    }

    String outputGraphTypeMenu
            (Writer stream, Map<String, String> httpParams)
            throws IOException
    {
        stream.append("\n<H3>Graph type: <SELECT NAME=\"graphType\" ");
        stream.append("onChange=\"{ form.submit(); }\">");
        String currentGraphTypeString = httpParams.get("graphType");
        String currentGraphType = graphTypes[0];
        for(String graphType: graphTypes)
        {
            String htmlified = htmlify(graphType);
            stream.append("\n<OPTION VALUE=\"");
            stream.append(htmlified);
            stream.append("\"");
            if(htmlified.equals(currentGraphTypeString))
            {
                currentGraphType = graphType;
                stream.append(" SELECTED");
            }
            stream.append(">");
            stream.append(htmlified);
        }
        stream.append("</SELECT></H3>");
        return currentGraphType;
    }

    public static final String ADVERTISER_ID_PARAM = "advertiserId";

    static Sexpression outputAdvertiserMenu
            (Writer stream, Sexpression advertisers,
             Map<String, String> httpParams)
            throws IOException
    {
        return outputAdvertiserMenu
                    (stream, advertisers, httpParams, "Advertiser: ",
                     ADVERTISER_ID_PARAM, true);
    }

    static Sexpression outputAdvertiserMenu
            (Writer stream, Sexpression advertisers,
             Map<String, String> httpParams, String title, String param,
             boolean springLoaded) throws IOException
    {
        return outputAdvertiserMenu(stream, advertisers, httpParams, title,
                                    param, springLoaded, true, false);
    }

    static Sexpression outputAdvertiserMenu
            (Writer stream, Sexpression advertisers,
             Map<String, String> httpParams, String title, String param,
             boolean springLoaded, boolean headingP, boolean nullItem)
            throws IOException
    {
        return outputMenu(stream, advertisers, httpParams, title, param,
                          springLoaded, headingP, nullItem, "Any",
                          false, null);
    }

    @SuppressWarnings("unused")
    static Sexpression outputAdvertiserMenu
            (Writer stream, Sexpression advertisers,
             Map<String, String> httpParams, String title, String param,
             boolean springLoaded, boolean headingP, boolean nullItem,
             boolean allItem)
            throws IOException
    {
        return outputMenu(stream, advertisers, httpParams, title, param,
                          springLoaded, headingP, nullItem, "None",
                          allItem, "All");
    }

    public static final String CAMPAIGN_ID_PARAM = "campaignId";
    public static boolean filterOutChildCampaigns = false;

    Sexpression outputParentCampaignMenu
         (Writer stream, Sexpression campaigns, Map<String, String> httpParams,
          boolean multiple)
         throws IOException
    {
        Map<String, List<Sexpression>> kidsMap
                = new HashMap<String, List<Sexpression>>();
        return outputParentCampaignMenu
                (stream, campaigns, httpParams, kidsMap, multiple);
    }

    Sexpression outputParentCampaignMenu
            (Writer stream, Sexpression campaigns,
             Map<String, String> httpParams,
             Map<String, List<Sexpression>> kidsMap, boolean multiple)
            throws IOException
    {
        campaigns = Cons.sort(campaigns, Cons.Lessp, Cons.cadrKey);
        Sexpression parentCampaigns = Null.nil;
        Sexpression l = campaigns;
        while(l != Null.nil)
        {
            Sexpression pair = l.car();
            Sexpression name = pair.second();
            String nameS = name.unboxString();
            String canonicalName =
                    AppNexusCampaignSplitter.getCanonicalName(nameS);
            if(!filterOutChildCampaigns ||
               !AppNexusCampaignSplitter.isChildCampaign(nameS))
                parentCampaigns = new Cons(pair, parentCampaigns);
            List<Sexpression> list = kidsMap.get(canonicalName);
            if(list == null)
            {
                list = new Vector<Sexpression>();
                kidsMap.put(canonicalName, list);
            }
            list.add(pair);
            l = l.cdr();
        }
        parentCampaigns = Cons.reverse(parentCampaigns);
        return outputCampaignMenu
                    (stream, parentCampaigns, httpParams, multiple);
    }

    Sexpression outputCampaignMenu
            (Writer stream, Sexpression parentCampaigns,
             Map<String, String> httpParams, boolean multiple)
            throws IOException
    {
        return outputCampaignMenu
                    (stream, parentCampaigns, httpParams, "Campaign: ",
                     CAMPAIGN_ID_PARAM, true, multiple);
    }

    static int maxMultipleMenuSize = 5;

    Sexpression outputCampaignMenu
            (Writer stream, Sexpression parentCampaigns,
             Map<String, String> httpParams, String title, String param,
             boolean springLoaded, boolean multiple)
            throws IOException
    {
        stream.append("\n<H3>");
        stream.append(title);
        stream.append("<SELECT NAME=\"");
        stream.append(param);
        stream.append("\"");
        if(multiple)
        {
            stream.append(" MULTIPLE");
            int maxl = Math.max(1, Math.min(maxMultipleMenuSize,
                                            parentCampaigns.length()));
            stream.append(" SIZE=");
            stream.append(Integer.toString(maxl));
        }
        if(springLoaded) stream.append(" onChange=\"{ form.submit(); }\"");
        stream.append(">");
        Sexpression currentCampaigns = Null.nil;
        String currentCampaignString = httpParams.get(param);
        List<String> currentCampaignStrings = new Vector<String>();
        if(currentCampaignString != null)
        {
            String[] vs = currentCampaignString.split
                    (HTTPListener.ARG_SEPARATOR);
            currentCampaignStrings.addAll(Arrays.asList(vs));
        }
        Sexpression l = parentCampaigns;
        while(l != Null.nil)
        {
            Sexpression campaign = l.car();
            String htmlifiedId = htmlify(campaign.car().pprint
                    (false, 0, true, Sexpression.PR_TS_LEVEL,
                            Sexpression.PR_TS_LENGTH));
            String htmlifiedName =
                    htmlify(campaign.second().pprint
                            (false, 0, false, Sexpression.PR_TS_LEVEL,
                                    Sexpression.PR_TS_LENGTH));
            stream.append("\n<OPTION VALUE=\"");
            stream.append(htmlifiedId);
            stream.append("\"");
            if(currentCampaignStrings.contains(htmlifiedId))
            {
                if(!Cons.member(campaign, currentCampaigns))
                    currentCampaigns =
                        Cons.append(currentCampaigns, Cons.list(campaign));
                stream.append(" SELECTED");
            }
            stream.append(">");
            stream.append(htmlifiedName);
            stream.append(" (");
            stream.append(htmlifiedId);
            stream.append(")");
            l = l.cdr();
        }
        if(currentCampaigns == Null.nil && parentCampaigns != Null.nil)
            currentCampaigns = Cons.list(parentCampaigns.car());
        stream.append("</SELECT></H3>");
        return (multiple ? currentCampaigns : currentCampaigns.firstIfList());
    }

    @SuppressWarnings("unused")
    Sexpression outputHourMenu
            (Writer stream, Sexpression hours, String paramName,
             String title, Map<String, String> httpParams,
             boolean defaultToEnd, boolean springLoaded,
             TimeScale ts) throws IOException
    {
        return outputHourMenu(stream, hours, paramName, title, httpParams,
                              defaultToEnd, null, null, springLoaded, ts);
    }

    static final String HOUR_PARAM_PART = "HOUR_";
    static final Object[][] HOURS_OF_DAY =
            {
                    {  0, "12 AM" },
                    {  1, "01 AM" },
                    {  2, "02 AM" },
                    {  3, "03 AM" },
                    {  4, "04 AM" },
                    {  5, "05 AM" },
                    {  6, "06 AM" },
                    {  7, "07 AM" },
                    {  8, "08 AM" },
                    {  9, "09 AM" },
                    { 10, "10 AM" },
                    { 11, "11 AM" },
                    { 12, "12 PM" },
                    { 13, "01 PM" },
                    { 14, "02 PM" },
                    { 15, "03 PM" },
                    { 16, "04 PM" },
                    { 17, "05 PM" },
                    { 18, "06 PM" },
                    { 19, "07 PM" },
                    { 20, "08 PM" },
                    { 21, "09 PM" },
                    { 22, "10 PM" },
                    { 23, "11 PM" }
            };

    static Sexpression dayFloors(Sexpression hours, TimeZone tz)
    {
        SexpLoc loc = new SexpLoc();
        HashSet<Date> set = new HashSet<Date>();
        while(hours != Null.nil)
        {
            Sexpression thisHour = hours.car();
            Date hour = AppNexusUtils.dayFloor(thisHour.unboxDate(), tz, 0);
            if(set.add(hour))
                loc.collect(new DateAtom(hour));
            hours = hours.cdr();
        }
        return loc.getSexp();
    }

    static String htmlifiedSansHour
            (String htmlified, boolean hourlyMode)
    {
        if(htmlified != null && hourlyMode)
        {
            int spaceIndex = htmlified.indexOf(" ");
            htmlified = htmlified.substring(0, spaceIndex);
        }
        return htmlified;
    }

    Sexpression outputHourMenu
            (Writer stream, Sexpression hours, String paramName,
             String title, Map<String, String> httpParams,
             boolean defaultToEnd, TimeZone localTimeZone,
             TimeZone wrtTimezone, boolean springLoaded,
             TimeScale ts) throws IOException
    {
        boolean hourlyMode = ts == TimeScale.HOURLY;
        stream.append("\n<H3>");
        stream.append(title);
        stream.append(":&nbsp;");
        //-------------------------------
        stream.append("<SELECT NAME=\"");
        stream.append(paramName);
        if(springLoaded)
            stream.append("\" onChange=\"{ form.submit(); }\">");
        else stream.append("\">");
        Sexpression currentHour =
                (defaultToEnd ? hours.lastElement() : hours.car());
        String currentHourString = httpParams.get(paramName);
        String currentHourSans =
                htmlifiedSansHour(currentHourString, hourlyMode);
        Sexpression l = dayFloors(hours, wrtTimezone);
        boolean selectionMade = false;
        while(l != Null.nil)
        {
            Sexpression hour = l.car();
            String htmlified =
                    htmlify((localTimeZone != null && wrtTimezone != null
                                ? SQLHTTPHandler.dateWRTTimeZone
                                        (hour, localTimeZone, wrtTimezone)
                                : hour.pprinc()));
            String sansHour = htmlifiedSansHour(htmlified, hourlyMode);
            stream.append("\n<OPTION VALUE=\"");
            stream.append(htmlified);
            stream.append("\"");
            if(htmlified.equals(currentHourString) ||
                 (defaultToEnd && !selectionMade && l.cdr() == Null.nil) ||
                 (currentHourString == null &&
                     defaultToEnd && l.cdr() == Null.nil) ||
                 (hourlyMode && sansHour.equals(currentHourSans)))
            {
                currentHour = hour;
                stream.append(" SELECTED");
                selectionMade = true;
            }
            stream.append(">");
            if(hourlyMode)
            {
                int spaceIndex = htmlified.indexOf(" ");
                htmlified = htmlified.substring(0, spaceIndex);
            }
            stream.append(htmlified);
            l = l.cdr();
        }
        stream.append("</SELECT>");
        //---------------------------------
        if(hourlyMode)
        {
            stream.append("<SELECT NAME=\"");
            stream.append(HOUR_PARAM_PART);
            stream.append(paramName);
            if(springLoaded)
                stream.append("\" onChange=\"{ form.submit(); }\">");
            else stream.append("\">");
            Integer currentHourOfDay =
                    (defaultToEnd
                          ? (Integer)HOURS_OF_DAY[HOURS_OF_DAY.length - 1][0]
                          : (Integer)HOURS_OF_DAY[0][0]);
            String currentHourOfDayString =
                    httpParams.get(HOUR_PARAM_PART + paramName);
            selectionMade = false;
            for(Object[] hourOfDay: HOURS_OF_DAY)
            {
                Integer hourOfDayNum = (Integer)hourOfDay[0];
                String htmlified = (String)hourOfDay[1];
                stream.append("\n<OPTION VALUE=\"");
                stream.append(htmlified);
                stream.append("\"");
                if(htmlified.equals(currentHourOfDayString) ||
                     (defaultToEnd && !selectionMade && hourOfDayNum == 23) ||
                     (currentHourString == null &&
                         defaultToEnd && hourOfDayNum == 23))
                {
                    currentHourOfDay = hourOfDayNum;
                    stream.append(" SELECTED");
                    selectionMade = true;
                }
                stream.append(">");
                stream.append(htmlified);
            }
            currentHour = new DateAtom(currentHour.unboxDate().getTime() +
                                       (currentHourOfDay * 3600000));
            stream.append("</SELECT>");
        }
        //---------------------------------
        stream.append("</H3>");
        return currentHour;
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
        HTTPListener.registerHandlerClass(BidderGrapherHTTPHandler.class);
    }
    static
    { register(); }
}

