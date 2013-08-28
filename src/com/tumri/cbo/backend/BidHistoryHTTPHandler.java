package com.tumri.cbo.backend;

import com.tumri.af.context.TimeScale;
import com.tumri.cbo.applets.graph.CurveStyle;
import com.tumri.mediabuying.appnexus.*;
import com.tumri.mediabuying.appnexus.services.AdvertiserService;
import com.tumri.mediabuying.appnexus.services.CampaignService;
import com.tumri.mediabuying.appnexus.services.LineItemService;
import com.tumri.mediabuying.zini.*;
import com.tumri.cbo.applets.graph.CampaignPerformancePlot;
import com.tumri.cbo.applets.graph.CurveGroup;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class BidHistoryHTTPHandler extends BidderGrapherHTTPHandler {

    public void registerHandler(HTTPListener l)
    {
        setListener(l);
        setListener(l);
        HTTPListener.registerHandler(getUrlName(), this);
    }

    static String urlName = "BIDHISTORY";
    static String prettyName = "Campaign Performance Charts";
    static final String TIMESCALE = "TimeScale";
    static final String TIMESCALE_TITLE= "Timescale";
    static final String USE_CAMPAIGN_TZ = "useCampaignTimeZone";
    static final String MULTIPLE_CAMPAIGNS = "multipleCampaigns";
    static ImageFormat downloadImageType = ImageFormat.PNG;
    static String timeAxisName = "Time";
    static String impressionsAxisName = "Impressions";
    static String leftLeftAxisName = "Leftleft";
    static String cpmAxisName = "Price ($)";
    static String valueAxisName = "Value";
    public static int APPLET_WIDTH = 1200;
    public static int APPLET_HEIGHT = 600;
    static CurveSpec[] leftCurveSpecs =
      new CurveSpec[]
       { new CurveSpec("Impressions Served", "getNormalizedImpressionsServed"),
         new CurveSpec("Imp Budget (normalized)", "getNormalizedImpressionBudget"),
         new CurveSpec("Imp Target (normalized)", "getNormalizedImpressionTarget")
       };
    static CurveSpec[] leftLeftCurveSpecs =
      new CurveSpec[] { };
    static CurveSpec[] rightCurveSpecs =
      new CurveSpec[]
       { new CurveSpec("CPM", "getNormalizedAverageCPMPaid"),
         new CurveSpec("Bid", "getNormalizedBidPrice") };
    static CurveSpec[] rightRightCurveSpecs =
      new CurveSpec[]
       { new CurveSpec("Entropy", "getNormalizedEntropy",
                       new CurveStyle[] { CurveStyle.EXCLUDE_NON_POSITIVE },
                       null),
         new CurveSpec("Changes", "getNormalizedChangeCount",
                       new CurveStyle[] { CurveStyle.EXCLUDE_NON_POSITIVE,
                                          CurveStyle.SCATTER ,
                                          CurveStyle.ANNOTATE },
                       new ChangesAnnotator())};
    static CurveSpec[] allCurveSpecs =
            append(0,  leftCurveSpecs,   leftLeftCurveSpecs,
                      rightCurveSpecs, rightRightCurveSpecs);
    static double defaultMeasureValue = 0.0d;
    static boolean showAnnotations = true;

    @SuppressWarnings("unused")
    public BidHistoryHTTPHandler
            (HTTPListener listener, String[] commandLineArgs)
    {
        super(listener, commandLineArgs, urlName, prettyName);
    }

    public BidHistoryHTTPHandler
            (HTTPListener listener, String[] commandLineArgs, Bidder bidder)
    {
        super(listener, commandLineArgs, urlName, prettyName);
        setBidder(bidder);
    }

    static String[][] staticUrlParams =
            {
                    { USE_CAMPAIGN_TZ, "Yes" },
                    { TIMESCALE, TimeScale.HOURLY.toString() },
                    { MULTIPLE_CAMPAIGNS, "No" },
                    { "measure_Impressions Served", "on" },
                    { "measure_CPM", "on" },
                    { "measure_Bid", "on" },
                    { "ShowGraph", "Show Graph" }
            };

    public static String makeURL(CampaignData cd)
    {
        TimeZone tz = cd.getTimeZone();
        Date now = new Date();
        Calendar c = Calendar.getInstance();
        c.setTimeZone(tz);
        c.setTime(now);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DATE, -1);
        c.set(Calendar.HOUR, 23);
        Date currentEndHour = c.getTime();
        c.add(Calendar.DATE, -7);
        c.set(Calendar.HOUR, 0);
        Date currentStartHour = c.getTime();
        StringBuffer b = new StringBuffer();
        b.append("../");
        b.append(urlName);
        b.append("/");
        b.append(urlName);
        b.append("?");
        b.append("&advertiserId=");
        b.append(Long.toString(cd.getAdvertiserId()));
        b.append("&campaignId=");
        b.append(Long.toString(cd.getCampaignId()));
        b.append("&");
        b.append(START_HOUR);
        b.append("=");
        b.append(Utils.urlEscape(DateAtom.princDate(currentStartHour)));
        b.append("&");
        b.append(END_HOUR);
        b.append("=");
        b.append(Utils.urlEscape(DateAtom.princDate(currentEndHour)));
        /*
        b.append("&");
        b.append(SQLHTTPHandler.TIMEZONE);
        b.append("=");
        b.append(escapeHtml(selectedTimeZone));
        */
        for(String[] pair: staticUrlParams)
        {
            b.append("&");
            b.append(pair[0]);
            b.append("=");
            b.append(pair[1]);
        }
        return b.toString();
    }

    static CurveSpec[] append(int start, CurveSpec[]... curves)
    {
        if(start >= curves.length)
            return new CurveSpec[0];
        else if(start == curves.length - 1)
            return curves[start];
        else
        {
            CurveSpec[] a = curves[start];
            CurveSpec[] b = append(start + 1, curves);
            CurveSpec[] res = new CurveSpec[a.length + b.length];
            int j = 0;
            for(CurveSpec c: a)
            {
                res[j] = c;
                j = j + 1;
            }
            for(CurveSpec c: b)
            {
                res[j] = c;
                j = j + 1;
            }
            return res;
        }
    }

    static final String measureParamCookie = "measure_";

    public static List<String> outputMeasureCheckBoxes
            (Writer stream, String title, Map<String, String> httpParams,
             boolean springLoadedP)
            throws IOException
    {
        List<String> res = new Vector<String>();
        stream.append("\n<H3>");
        stream.append(title);
        boolean checksFound = false;
        for(CurveSpec c: allCurveSpecs)
        {
            String param = measureParamCookie + htmlify(c.name);
            if(httpParams.get(param) != null) checksFound = true;
        }
        for(CurveSpec c: allCurveSpecs)
        {
            String param = measureParamCookie + htmlify(c.name);
            stream.append("<br>\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            stream.append("<INPUT TYPE=\"checkbox\" NAME=\"");
            stream.append(param);
            stream.append("\"");
            if(!checksFound || httpParams.get(param) != null)
            {
                stream.append(" CHECKED");
                res.add(c.name);
            }
            if(springLoadedP)
                stream.append("\" onChange=\"{ form.submit(); }\">");
            stream.append(">");
            stream.append(htmlify(c.name));
        }
        stream.append("</H3>");
        return res;
    }

    public static TimeScale[] iterableTimeScales
            (Map<String, String> httpParams)
    {
        boolean admin = HTTPHandler.isAdministratorUser(httpParams);
        if(admin)
            return TimeScale.values();
        else return new TimeScale[]{ TimeScale.HOURLY, TimeScale.DAILY };
    }

    public static TimeScale outputTimeScaleMenu
            (Writer stream, String paramName, String title,
             Map<String, String> httpParams, boolean defaultToEnd,
             boolean springLoadedP)
            throws IOException
    {
        TimeScale[] timeScales = TimeScale.class.getEnumConstants();
        stream.append("\n<H3>");
        stream.append(title);
        stream.append(": <SELECT NAME=\"");
        stream.append(paramName);
        if(springLoadedP)
            stream.append("\" onChange=\"{ form.submit(); }\">");
        else stream.append("\">");
        TimeScale currentTimeScale =
            (defaultToEnd ? timeScales[timeScales.length - 1] : timeScales[0]);
        String currentTimeScaleString = httpParams.get(paramName);
        boolean selectionMade = false;
        int i = 0;
        for(TimeScale ts: iterableTimeScales(httpParams))
        {
            String htmlified = htmlify(ts.toString());
            stream.append("\n<OPTION VALUE=\"");
            stream.append(htmlified);
            stream.append("\"");
            if(htmlified.equals(currentTimeScaleString) ||
                 (defaultToEnd && !selectionMade &&
                  i == timeScales.length - 1) ||
                 (currentTimeScaleString == null &&
                     defaultToEnd && i == timeScales.length - 1))
            {
                currentTimeScale = ts;
                stream.append(" SELECTED");
                selectionMade = true;
            }
            stream.append(">");
            stream.append(htmlified);
            i = i + 1;
        }
        stream.append("</SELECT></H3>");
        return currentTimeScale;
    }

    static String imageFileExtension(ImageFormat format)
    {
        switch(format)
        {
            case PNG : return "png";
            case JPG : return "jpg";
            default : throw Utils.barf("Unhandled image format", null, format);
        }
    }

    static String imageFileContentType(ImageFormat format)
    {
        switch(format)
        {
            case PNG : return HTTPListener.image_png;
            case JPG : return HTTPListener.image_jpg;
            default : throw Utils.barf("Unhandled image format", null, format);
        }
    }

    static String campaignIdsToString(Sexpression campaigns)
    {
        return campaignIdsToString(campaigns, HTTPListener.ARG_SEPARATOR);
    }

    static String campaignIdsToString(Sexpression campaigns, String separator)
    {
        StringBuffer out = new StringBuffer();
        boolean first = true;
        while(campaigns != Null.nil)
        {
            if(first) first = false;
            else out.append(separator);
            out.append(campaigns.car().car().unboxLong().toString());
            campaigns = campaigns.cdr();
        }
        return out.toString();
    }

    static String campaignNamesToString(Sexpression campaigns)
    {
        StringBuffer out = new StringBuffer();
        boolean first = true;
        while(campaigns != Null.nil)
        {
            if(first) first = false;
            else out.append(", ");
            out.append(campaigns.car().second().unboxString());
            campaigns = campaigns.cdr();
        }
        return out.toString();
    }

    static String measureURLComponent(Map<String, String> httpParams)
    {
        StringBuffer mb = new StringBuffer();
        boolean checksFound = false;
        for(CurveSpec c: allCurveSpecs)
        {
            String param = measureParamCookie + htmlify(c.name);
            if(httpParams.get(param) != null) checksFound = true;
        }
        for(CurveSpec c: allCurveSpecs)
        {
            String param = measureParamCookie + htmlify(c.name);
            if(!checksFound || httpParams.get(param) != null)
            {
                mb.append("&");
                mb.append(param);
                mb.append("=Yes");
            }
        }
        return mb.toString();
    }

    static String downloadURL (Sexpression currentAdvertiser,
                               Sexpression currentCampaigns,
                               Sexpression currentStartHour,
                               Sexpression currentEndHour, TimeScale ts,
                               String selectedTimeZone,
                               TimeZone localTimeZone,
                               TimeZone wrtTimezone, DownloadType downloadType,
                               boolean multipleCampaigns,
                               Map<String, String> httpParams)
    {
        String fileName =
                Utils.sanitiseFileName
                        (currentAdvertiser.car().pprinc() + "-" +
                         campaignIdsToString(currentCampaigns) + "-from-" +
                         currentStartHour.pprinc() + "-to-" +
                         currentEndHour.pprinc() + "-" + ts.toString() +
                         "-wrt-" + selectedTimeZone);
        String startHtmlified =
                    escapeHtml((localTimeZone != null && wrtTimezone != null
                                ? SQLHTTPHandler.dateWRTTimeZone
                                        (currentStartHour, localTimeZone,
                                         wrtTimezone)
                                : currentStartHour.pprinc()));
        String endHtmlified =
                    escapeHtml((localTimeZone != null && wrtTimezone != null
                                ? SQLHTTPHandler.dateWRTTimeZone
                                        (currentEndHour, localTimeZone,
                                         wrtTimezone)
                                : currentEndHour.pprinc()));
        StringBuffer mb = new StringBuffer();
        boolean checksFound = false;
        for(CurveSpec c: allCurveSpecs)
        {
            String param = measureParamCookie + htmlify(c.name);
            if(httpParams.get(param) != null) checksFound = true;
        }
        for(CurveSpec c: allCurveSpecs)
        {
            String param = measureParamCookie + htmlify(c.name);
            if(!checksFound || httpParams.get(param) != null)
            {
                mb.append("&");
                mb.append(param);
                mb.append("=Yes");
            }
        }
        return
          "../" + urlName + "/" +
          escapeHtml(fileName.replace(':', '_').replace(' ', '_')) +
          (downloadType == DownloadType.XLS
                  ? ".xls?DownloadFile=true&"
                  : (downloadType == DownloadType.IMAGE
                        ? "." + imageFileExtension(downloadImageType) +
                          "?DownloadImageFile=true&"
                        : "ERROR")) +
          ADVERTISER_ID_PARAM + "=" + currentAdvertiser.car().pprinc() + "&" +
          CAMPAIGN_ID_PARAM + "=" + campaignIdsToString(currentCampaigns) + "&" +
          TIMESCALE + "=" + escapeHtml(ts.toString()) + "&" +
          START_HOUR + "=" + startHtmlified + "&" +
          END_HOUR   + "=" + endHtmlified + "&" +
          MULTIPLE_CAMPAIGNS + "=" +
                  (multipleCampaigns
                          ? RadioBooleanWidget.YES_PARAM
                          : RadioBooleanWidget.NO_PARAM) + "&" +
          SQLHTTPHandler.TIMEZONE + "=" + escapeHtml(selectedTimeZone) +
          measureURLComponent(httpParams);
    }

    static StringBuffer collectTimePointStrings
            (List<PerformanceHistoryRow> curve)
    {
        StringBuffer xPoints = new StringBuffer();
        boolean firstP;
        firstP = true;
        for(PerformanceHistoryRow point: curve)
        {
            String timeString = point.getTimeString();
            if(firstP) firstP = false;
            else xPoints.append(",");
            xPoints.append(timeString);
        }
        return xPoints;
    }

    static double measurePoint
            (PerformanceHistoryRow point, Method accessor,
             TimeScale timeScale, double defaultValue)
    {
        Double value = defaultValue;
        try
        {
            Object obj = accessor.invoke(point, timeScale);
            if(obj == null) {}
            else if(obj instanceof Byte)
                value = ((Byte)obj).doubleValue();
            else if(obj instanceof Integer)
                value = ((Integer)obj).doubleValue();
            else if(obj instanceof Long)
                value = ((Long)obj).doubleValue();
            else if(obj instanceof Float)
                value = ((Float)obj).doubleValue();
            else if(obj instanceof Double)
                value = (Double)obj;
            else throw Utils.barf("Unhandled type", point, obj);
        }
        catch (IllegalAccessException e)
        {
            throw Utils.barf(e, point, accessor);
        }
        catch (InvocationTargetException e)
        {
            throw Utils.barf(e, point, accessor);
        }
        return value;
    }

    static StringBuffer collectMeasurePoints
            (List<PerformanceHistoryRow> curve, Method accessor,
             TimeScale timeScale, double defaultValue)
    {
        StringBuffer points = new StringBuffer();
        boolean firstP;
        firstP = true;
        for(PerformanceHistoryRow point: curve)
        {
            Double value = measurePoint
                    (point, accessor, timeScale, defaultValue);
            if(firstP) firstP = false;
            else points.append(",");
            points.append(value.toString());
        }
        return points;
    }

    static StringBuffer collectAnnotations
            (CurveSpec curveSpec, List<PerformanceHistoryRow> curve)
    {
        if(showAnnotations &&
           curveSpec.curveStyles.contains(CurveStyle.ANNOTATE))
        {
            Annotator annotator = curveSpec.annotator;
            if(annotator != null)
            {
                StringBuffer ann = new StringBuffer();
                boolean firstP;
                firstP = true;
                try
                {
                    for(int i = 0; i < curve.size(); i++)
                    {
                        String summary = annotator.computeAnnotation(i, curve);
                        if(firstP) firstP = false;
                        else ann.append(",");
                        ann.append((summary == null
                                ? ""
                                : URLEncoder.encode(summary, "UTF-8")));
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    throw Utils.barf(e, null);
                }
                return ann;
            }
            else return null;
        }
        else return null;
    }

    static Method getCurveAccessor(String name)
    {
        Class<?> c = PerformanceHistoryRow.class;
        Method m;
        try
        {
            m = c.getMethod(name, TimeScale.class);
        }
        catch (NoSuchMethodException e)
        {
            throw Utils.barf(e, c, name);
        }
        return m;
    }

    static String curveTag(int curveIndex, List<PerformanceCurve> curves)
    {
        if(curves.size() > 1)
        {
            return "[" + Integer.toString(curveIndex) + "] ";
        }
        else return "";
    }

    static void outputCurveParam
            (String paramName, String paramValue, int totalCurves,
             Writer stream, String paramNamePart, int i)
            throws IOException
    {
        stream.append("\n        <PARAM NAME=\"");
        stream.append(paramNamePart);
        stream.append(paramName);
        stream.append(Integer.toString(totalCurves + i));
        stream.append("\" VALUE=\"");
        stream.append(paramValue);
        stream.append("\">");
    }

    static int emitCurve(int totalCurves, int curveIndex, Writer stream,
                         TimeScale timeScale, List<PerformanceCurve> curves,
                         Long advertiserId, List<PerformanceHistoryRow> points,
                         List<String> selectedMeasures, CurveSpec[] curveSpecs,
                         String paramNamePart)
            throws IOException
    {
        for(int i = 0; i < curveSpecs.length; i++)
        {
            if(selectedMeasures.contains(curveSpecs[i].name))
            {
                Method accessor =
                        getCurveAccessor(curveSpecs[i].accessor);
                StringBuffer yPoints =
                        collectMeasurePoints(points, accessor, timeScale,
                                defaultMeasureValue);
                StringBuffer annotations =
                        collectAnnotations(curveSpecs[i], points);
                outputCurveParam
                        ("CurveName", curveTag(curveIndex, curves) +
                                      curveSpecs[i].name,
                         totalCurves, stream, paramNamePart, i);
                PerformanceCurve curve = curves.get(curveIndex);
                outputCurveParam
                        ("CurveAdvertiser", advertiserId.toString(),
                         totalCurves, stream, paramNamePart, i);
                outputCurveParam
                        ("CurveCampaign", curve.campaignId.toString(),
                         totalCurves, stream, paramNamePart, i);
                if(curveSpecs[i].curveStyles != null)
                    outputCurveParam
                            ("CurveStyles",
                             AppNexusUtils.commaSeparate
                                     (curveSpecs[i].curveStyles),
                             totalCurves, stream, paramNamePart, i);
                stream.append("\n        <PARAM NAME=\"");
                stream.append(paramNamePart);
                stream.append("YPoints");
                stream.append(Integer.toString(totalCurves + i));
                stream.append("\" VALUE=\"");
                stream.append(yPoints.toString());
                stream.append("\">");
                if(annotations != null)
                {
                    stream.append("\n        <PARAM NAME=\"");
                    stream.append(paramNamePart);
                    stream.append("Annotations");
                    stream.append(Integer.toString(totalCurves + i));
                    stream.append("\" VALUE=\"");
                    stream.append(annotations);
                    stream.append("\">");
                }
            }
        }
        totalCurves = totalCurves + curveSpecs.length;
        return totalCurves;
    }

    static void emitGraphForCurve
            (Writer stream, Long advertiserId, String advertiserName,
             List<PerformanceCurve> curves, TimeScale timeScale,
             TimeZone wrtTimeZone, List<String> selectedMeasures)
            throws IOException
    {
        StringBuffer buff = new StringBuffer();
        buff.append(advertiserName);
        buff.append("(");
        buff.append(advertiserId);
        buff.append(") \n/ ");
        boolean first = true;
        int curveIndex = 0;
        for(PerformanceCurve curve: curves)
        {
            if(first) first = false;
            else buff.append(", ");
            buff.append(curveTag(curveIndex, curves));
            buff.append(curve.campaignName);
            curveIndex = curveIndex + 1;
        }
        first = true;
        buff.append("(");
        for(PerformanceCurve curve: curves)
        {
            if(first) first = false;
            else buff.append(", ");
            buff.append(curve.campaignId);
        }
        buff.append(")");
        String title = buff.toString();
        stream.append(GraphPerspective.appletTagStart
                            (APPLET_WIDTH, APPLET_HEIGHT));
        stream.append
             ("\n            code=\"com.tumri.cbo.applets.graph.CampaignPerformancePlot.class\">");
        //-------------------
        stream.append("\n        <PARAM NAME=\"title\" VALUE=\"");
        stream.append(title);
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"timeScale\" VALUE=\"");
        stream.append(timeScale.toString());
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"timeZone\" VALUE=\"");
        // stream.append(wrtTimeZone.getDisplayName());
        stream.append(wrtTimeZone.getID());
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"xAxisName\" VALUE=\"");
        stream.append(timeAxisName);
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"leftYAxisName\" VALUE=\"");
        stream.append(impressionsAxisName);
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"leftLeftYAxisName\" VALUE=\"");
        stream.append(leftLeftAxisName);
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"rightYAxisName\" VALUE=\"");
        stream.append(cpmAxisName);
        stream.append("\">");
        stream.append("\n        <PARAM NAME=\"rightRightYAxisName\" VALUE=\"");
        stream.append(valueAxisName);
        stream.append("\">");
        //---------------------
        int totalLeftCurves = 0;
        int totalLeftLeftCurves = 0;
        int totalRightCurves = 0;
        int totalRightRightCurves = 0;
        curveIndex = 0;
        for(PerformanceCurve curve: curves)
        {
            List<PerformanceHistoryRow> points = curve.points;
            StringBuffer xPoints = collectTimePointStrings(points);
            stream.append("\n        <PARAM NAME=\"xPoints\" VALUE=\"");
            stream.append(xPoints.toString());
            stream.append("\">");
            totalLeftCurves = emitCurve
                    (totalLeftCurves, curveIndex, stream, timeScale,
                     curves, advertiserId, points, selectedMeasures,
                     leftCurveSpecs, "left");
            totalLeftLeftCurves = emitCurve
                    (totalLeftLeftCurves, curveIndex, stream, timeScale,
                     curves, advertiserId, points, selectedMeasures,
                     leftLeftCurveSpecs, "leftLeft");
            totalRightCurves = emitCurve
                    (totalRightCurves, curveIndex, stream, timeScale,
                     curves, advertiserId, points, selectedMeasures,
                     rightCurveSpecs, "right");
            totalRightRightCurves = emitCurve
                    (totalRightRightCurves, curveIndex, stream, timeScale,
                     curves, advertiserId, points, selectedMeasures,
                     rightRightCurveSpecs, "rightRight");
            curveIndex = curveIndex + 1;
        }
        //-------------------
        stream.append("\n    A graph should show here!");
        stream.append("\n    </APPLET>");
    }

    static String computeAnnotation(CurveSpec curveSpec, int i,
                                    List<PerformanceHistoryRow> curve)
    {
        if(curveSpec.annotator == null) return "";
        else return curveSpec.annotator.computeAnnotation(i, curve);
    }

    public static CurveGroup prepareDataSets
            (TimeScale timeScale, TimeZone tz,
             List<PerformanceHistoryRow> curve, CurveSpec[] curveSpecs,
             CurveGroup cg)
    {
        Locale locale = Locale.getDefault();
        String timeScaleString = timeScale.toString();
        Map<String, SimpleDateFormat> formatMap =
                CampaignPerformancePlot.getFormatMap(tz);
        for(CurveSpec curveSpec: curveSpecs)
        {
            String name = curveSpec.name;
            Method accessor = getCurveAccessor(curveSpec.accessor);
            TimeSeries ts = new TimeSeries(name);
            Set<CurveStyle> curveStyles = curveSpec.curveStyles;
            boolean excludeNonPositive =
                    curveStyles.contains(CurveStyle.EXCLUDE_NON_POSITIVE);
            boolean annotate =
                    curveStyles.contains(CurveStyle.ANNOTATE);
            int i = 0;
            for(PerformanceHistoryRow point: curve)
            {
                String x = point.getTimeString();
                Double y = measurePoint(point, accessor, timeScale,
                                        defaultMeasureValue);
                if(!excludeNonPositive || y > 0)
                {
                    RegularTimePeriod xr = CampaignPerformancePlot.toTimePeriod
                            (x, timeScaleString, tz, formatMap, locale);
                    ts.add(xr, y);
                }
                i = i + 1;
            }
            double[] yPoints = new double[ts.getItemCount()];
            List<String> annotations = new Vector<String>();
            if(annotate)
            {
                for(i = 0; i < yPoints.length; i++)
                {
                    annotations.add(computeAnnotation(curveSpec, i, curve));
                }
            }
            for(i = 0; i < yPoints.length; i++)
            {
                yPoints[i] = (Double)ts.getDataItem(i).getValue();
            }
            TimeSeriesCollection dataset = new TimeSeriesCollection();
            dataset.addSeries(ts);
            cg.addCurveStyles(curveStyles);
            cg.addDataSet(dataset);
            cg.addCurve(yPoints);
            cg.addAnnotations(annotations);
        }
        return cg;
    }

    static CurveSpec[] filterCurveSpecs
            (CurveSpec[] curveSpecs, List<String>selectedMeasures)
    {
        CurveSpec[] resArray;
        List<CurveSpec> res = new Vector<CurveSpec>();
        for(CurveSpec c: curveSpecs)
        {
            if(selectedMeasures.contains(c.name))
                res.add(c);
        }
        resArray = res.toArray(new CurveSpec[res.size()]);
        return resArray;
    }

    Map<String, String> handleDoIt
            (Writer stream, StringWriter sw, OutputStream os,
             boolean returnHeaders, PerformanceHistoryDAO impl,
             Sexpression currentAdvertiser, Sexpression currentCampaigns,
             Sexpression currentStartHour, Sexpression currentEndHour,
             boolean downloadP, boolean downloadImageP, boolean downloadFileP,
             boolean downloadImageFileP, Map<String, String> headers,
             TimeScale ts, String savePath, String selectedTimeZone,
             TimeZone wrtTimeZone, boolean multipleCampaigns,
             Map<String, String> httpParams, List<String> selectedMeasures)
            throws IOException
    {
        List<PerformanceCurve> curves = new Vector<PerformanceCurve>();
        try
        {
            Long advertiserId = currentAdvertiser.car().unboxLong();
            String advertiserName = currentAdvertiser.second().unboxString();
            String title = advertiserName + "(" + advertiserId + ") \n/ " +
                           campaignNamesToString(currentCampaigns) + "(" +
                           campaignIdsToString(currentCampaigns, ", ")+ ")";
            Sexpression l = currentCampaigns;
            List<Long> campaignIds = new Vector<Long>();
            while(l != Null.nil)
            {
                Long campaignId = l.car().car().unboxLong();
                String campaignName = l.car().second().unboxString();
                campaignIds.add(campaignId);
                List<PerformanceHistoryRow> points =
                        impl.getCampaignPerformanceHistory
                            (advertiserId, campaignId, ts,
                             currentStartHour.unboxDate(),
                             TimeScaleIterator.timeCeiling
                                 (currentEndHour.unboxDate(), ts, wrtTimeZone),
                             wrtTimeZone);
                PerformanceCurve curve =
                        new PerformanceCurve(points, campaignId, campaignName);
                if(points == null || points.size() == 0) {}
                else curves.add(curve);
                l = l.cdr();
            }
            if(curves.size() == 0)
                warn(sw, "No points found");
            else
            {
                if(downloadFileP)
                {
                    ByteArrayOutputStream baos
                            = new ByteArrayOutputStream();
                    impl.dumpPerformanceHistoryToExcel(curves, baos);
                    baos.flush();
                    baos.close();
                    byte[] ba = baos.toByteArray();
                    headers =
                            HTTPListener.emitStandardHeaders
                                (stream, 200, HTTPListener.application_xls,
                                 (long) ba.length, null, false, returnHeaders);
                    stream.flush();
                    os.write(ba);
                    os.flush();
                }
                else if(downloadImageFileP)
                {
                    ByteArrayOutputStream baos
                            = new ByteArrayOutputStream();
                    String xAxisName= timeAxisName;
                    String leftYAxisName = impressionsAxisName;
                    String leftLeftYAxisName = leftLeftAxisName;
                    String rightYAxisName = cpmAxisName;
                    String rightRightYAxisName = valueAxisName;
                    CurveGroup left =
                            new CurveGroup
                                    (leftYAxisName,
                                     CurveSpec.curveNames(leftCurveSpecs),
                                     null,
                                     CurveSpec.styles(leftCurveSpecs),
                                     CurveSpec.idList(advertiserId,
                                                      leftCurveSpecs),
                                     campaignIds);
                    CurveGroup leftLeft =
                            new CurveGroup
                                    (leftLeftYAxisName,
                                     CurveSpec.curveNames(leftLeftCurveSpecs),
                                     null,
                                     CurveSpec.styles(leftLeftCurveSpecs),
                                     CurveSpec.idList(advertiserId,
                                                      leftLeftCurveSpecs),
                                     campaignIds);
                    CurveGroup right =
                            new CurveGroup
                                    (rightYAxisName,
                                     CurveSpec.curveNames(rightCurveSpecs),
                                     null,
                                     CurveSpec.styles(rightCurveSpecs),
                                     CurveSpec.idList(advertiserId,
                                                      rightCurveSpecs),
                                     campaignIds);
                    CurveGroup rightRight =
                            new CurveGroup
                                    (rightRightYAxisName,
                                     CurveSpec.curveNames(rightRightCurveSpecs),
                                     null,
                                     CurveSpec.styles(rightRightCurveSpecs),
                                     CurveSpec.idList(advertiserId,
                                                      rightRightCurveSpecs),
                                     campaignIds);
                    for(PerformanceCurve curve: curves)
                    {
                        prepareDataSets
                              (ts, wrtTimeZone, curve.points,
                               filterCurveSpecs
                                       (leftCurveSpecs, selectedMeasures),
                               left);
                    }
                    for(PerformanceCurve curve: curves)
                    {
                        prepareDataSets
                              (ts, wrtTimeZone, curve.points,
                               filterCurveSpecs
                                       (leftLeftCurveSpecs, selectedMeasures),
                               leftLeft);
                    }
                    for(PerformanceCurve curve: curves)
                    {
                        prepareDataSets
                              (ts, wrtTimeZone, curve.points,
                               filterCurveSpecs
                                      (rightCurveSpecs, selectedMeasures),
                               right);
                    }
                    for(PerformanceCurve curve: curves)
                    {
                        prepareDataSets
                              (ts, wrtTimeZone, curve.points,
                               filterCurveSpecs
                                      (rightRightCurveSpecs, selectedMeasures),
                               rightRight);
                    }
                    JFreeChart chart = CampaignPerformancePlot.createChart
                            (title, xAxisName, left, leftLeft,
                             right, rightRight, ts.name(), wrtTimeZone);
                    ChartUtilities.writeChartAsPNG
                            (baos, chart, APPLET_WIDTH, APPLET_HEIGHT);
                    baos.flush();
                    baos.close();
                    byte[] ba = baos.toByteArray();
                    headers =
                       HTTPListener.emitStandardHeaders
                         (stream, 200, imageFileContentType(downloadImageType),
                          (long) ba.length, null, false, returnHeaders);
                    stream.flush();
                    os.write(ba);
                    os.flush();
                }
                else if(downloadP || downloadImageP)
                {
                    String imgTag = "\n<IMG SRC=\"../" +
                            urlName + "/" + urlName +
                            (downloadP
                                ? "?DownloadFile=true&"
                                : (downloadImageP
                                    ? "?DownloadFile=true&"
                                    : "ERROR")) +
                            ADVERTISER_ID_PARAM + "=" +
                            currentAdvertiser.car().pprinc() + "&" +
                            CAMPAIGN_ID_PARAM + "=" +
                            campaignIdsToString(currentCampaigns) + "&" +
                            TIMESCALE + "=" +
                            escapeHtml(ts.toString()) + "&" +
                            START_HOUR + "=" +
                            escapeHtml(currentStartHour.pprinc()) + "&" +
                            END_HOUR + "=" +
                            escapeHtml(currentEndHour.pprinc()) + "&" +
                            MULTIPLE_CAMPAIGNS + "=" +
                                    (multipleCampaigns
                                            ? RadioBooleanWidget.YES_PARAM
                                            : RadioBooleanWidget.NO_PARAM) + "&" +
                            SQLHTTPHandler.TIMEZONE + "=" + escapeHtml(selectedTimeZone) +
                            measureURLComponent(httpParams) +
                            "\"" +
                            " WIDTH=\"51\" HEIGHT=\"51\">\n";
                    sw.append(imgTag);
                }
                else impl.dumpPerformanceHistoryToExcel(curves, savePath);
                int size = 0;
                for(PerformanceCurve curve: curves)
                {
                    size = size + curve.points.size();
                }
                warn(sw,"Spreadsheet exported.  Rows: " + size);
            }
        }
        catch (Exception e)
        {
            throw Utils.barf(e, this);
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    static <T extends AbstractAppNexusService> T getSingleService
            (AppNexusReturnValue v, Class cl)
    {
        if(v.isGeneralisedServiceList())
            return (T)v.getService(0, cl);
        else return null;
    }

    static TimeZone getCampaignTimeZone
            (Bidder bidder, QueryContext qctx,
             Long advertiserId, Long campaignId)
    {
        Identity ident = bidder.getAppNexusIdentity();
        Observation obs =
             (campaignId == null
                     ? null : bidder.getJSONFor(campaignId, qctx, true));
        String tzName = null;
        CampaignService campaign =
             (campaignId != null
                ? (CampaignService)bidder.serviceForJSON(obs.campaignJSON, true, false)
                : null);
        if(campaign == null)
            campaign = getSingleService
                  (CampaignService.viewSpecific
                          (ident, campaignId, advertiserId,
                           AppNexusInterface.INTERVAL_VALUE_UNSPECIFIED),
                   CampaignService.class);
        if(campaign != null)
        {
            tzName = campaign.getTimezone();
            if(tzName == null)
            {
                LineItemService lineitem =
                        getSingleService
                      (LineItemService.viewSpecific
                              (ident, campaign.getLine_item_id(), advertiserId,
                               AppNexusInterface.INTERVAL_VALUE_UNSPECIFIED),
                       LineItemService.class);
                tzName = lineitem.getTimezone();
            }
            if(tzName == null)
            {
                LineItemService lineitem =
                        getSingleService
                      (LineItemService.viewSpecific
                              (ident, campaign.getLine_item_id(), advertiserId,
                               AppNexusInterface.INTERVAL_VALUE_UNSPECIFIED),
                       LineItemService.class);
                tzName = lineitem.getTimezone();
            }
            if(tzName == null)
            {
                AdvertiserService advertiser =
                        getSingleService
                      (AdvertiserService.viewSpecific
                              (ident, advertiserId,
                               AppNexusInterface.INTERVAL_VALUE_UNSPECIFIED),
                       AdvertiserService.class);
                tzName = advertiser.getTimezone();
            }
            if(tzName != null) return TimeZone.getTimeZone(tzName);
            else return null;
        }
        else return TimeZone.getTimeZone(tzName);
    }

    public static TimeZoneData outputTimeZoneWidgets
            (Writer sw, Bidder bidder, Long advertiserId, Long campaignId,
             Map<String, String> httpParams, QueryContext qctx)
            throws IOException
    {
        TimeZone campaignTimeZone =
                (advertiserId == null || campaignId == null
                        ? null
                        : getCampaignTimeZone
                                (bidder, qctx, advertiserId, campaignId));
        sw.append("\n<H3>");
        String selectedTimeZoneName;
        String radioVal = httpParams.get(USE_CAMPAIGN_TZ);
        boolean useCampTZP =
                campaignTimeZone != null &&
                (radioVal == null ||
                 RadioBooleanWidget.YES_PARAM.equals(radioVal));
        if(campaignTimeZone != null)
        {
            RadioBooleanWidget.radioButton
                    (sw, USE_CAMPAIGN_TZ,
                     RadioBooleanWidget.YES_PARAM, useCampTZP,
                     "Use&nbsp;campaign&nbsp;timezone&nbsp;(" +
                     campaignTimeZone.getID() + "/" +
                     campaignTimeZone.getDisplayName
                             (campaignTimeZone.inDaylightTime(new Date()),
                              TimeZone.LONG)
                     + ")", true);
            sw.append("<BR>\n");
            RadioBooleanWidget.radioButton
                    (sw, USE_CAMPAIGN_TZ,
                     RadioBooleanWidget.NO_PARAM, !useCampTZP,
                     "Explicit&nbsp;timezone&nbsp;", true);
            selectedTimeZoneName =
                    SQLHTTPHandler.outputTimeZoneMenu
                        (sw, "",
                         SQLHTTPHandler.TIMEZONE,
                         httpParams, true);
        }
        else selectedTimeZoneName =
                SQLHTTPHandler.outputTimeZoneMenu
                    (sw, "Timezone:&nbsp;",
                     SQLHTTPHandler.TIMEZONE,
                     httpParams, true);
        sw.append("</H3>");
        TimeZone wrtTimeZone =
                (useCampTZP
                    ? campaignTimeZone
                    : TimeZone.getTimeZone(selectedTimeZoneName));
        return new TimeZoneData(selectedTimeZoneName, campaignTimeZone,
                                wrtTimeZone);
    }

    static Sexpression filterHoursForTimeScale
            (Sexpression hours, TimeScale ts, TimeZone wrtTimezone)
    {
        int qmonth;
        switch(ts)
        {
            case HOURLY: return hours;
            case DAILY:
            case WEEKLY:
            case MONTHLY:
            case QUARTERLY:
            case YEARLY:
                SexpLoc loc = new SexpLoc();
                Calendar cal = Calendar.getInstance();
                cal.setTimeZone(wrtTimezone);
                Sexpression firstFloor = null;
                while(hours != Null.nil)
                {
                    Date hour = hours.car().unboxDate();
                    if(firstFloor == null)
                    {
                        Date firstDate =
                            TimeScaleIterator.timeFloor(hour, ts, wrtTimezone);
                        firstFloor = new DateAtom(firstDate);
                        loc.collect(firstFloor);
                    }
                    cal.setTime(hour);
                    if(cal.get(Calendar.HOUR_OF_DAY) == 0 &&
                       (ts == TimeScale.DAILY
                        || (ts == TimeScale.WEEKLY &&
                            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                        || (ts == TimeScale.MONTHLY &&
                            cal.get(Calendar.DAY_OF_MONTH) == 1)
                        || (ts == TimeScale.QUARTERLY &&
                            cal.get(Calendar.DAY_OF_MONTH) == 1 &&
                            ((qmonth = cal.get(Calendar.MONTH)) == 0 ||
                             qmonth == 4 || qmonth == 7 || qmonth == 10)
                           )
                        || (ts == TimeScale.YEARLY &&
                            cal.get(Calendar.DAY_OF_YEAR) == 1)
                       )
                       && !hours.car().equals(firstFloor)
                      )
                        loc.collect(hours.car());
                    hours = hours.cdr();
                }
                return loc.getSexp();
            default: throw Utils.barf("Unhandled timescale", null, ts);
        }
    }

    public boolean isAdminUserCommand() { return false; }

    public Map<String, String> handle
            (List<String> parsed, String command, String inputLine,
             BufferedReader in, Writer stream,
             OutputStream os, Map<String, String> httpParams,
             boolean returnHeaders)
            throws IOException
    {
        boolean admin = HTTPHandler.isAdministratorUser(httpParams);
        boolean hoursSpringLoaded = false;
        boolean timeScaleSpringLoaded = true;
        Bidder bidder = ensureBidder();
        PerformanceHistoryDAO impl = bidder.performanceHistoryDAOImpl;
        SQLConnector connector = bidder.ensureBidderSQLConnector();
        QueryContext qctx = connector.sufficientQueryContextFor();
        String stylesheetUrl = null;
        String savePath = httpParams.get(SAVE_TO_FILE_PARAM);
        boolean doitP = httpParams.get(DOIT_PARAM) != null;
        boolean downloadP = httpParams.get(DOWNLOAD_PARAM) != null;
        boolean downloadImageP = httpParams.get(DOWNLOAD_IMAGE_PARAM) != null;
        boolean showGraphP = httpParams.get(SHOW_GRAPH_PARAM) != null;
        boolean showGraphImageP = httpParams.get(SHOW_GRAPH_IMAGE_PARAM)!=null;
        boolean downloadFileP = httpParams.get(DOWNLOAD_FILE_PARAM) != null;
        boolean downloadImageFileP =
                httpParams.get(DOWNLOAD_IMAGE_FILE_PARAM) != null;
        boolean multipleCampaigns =
                RadioBooleanWidget.YES_PARAM.equals
                        (httpParams.get(MULTIPLE_CAMPAIGNS));
        StringWriter sw = new StringWriter();
        Map<String, String> headers =
                outputHeaderStuff(sw, stylesheetUrl, returnHeaders, httpParams);
        Sexpression advertisers = getAdvertisers(connector, qctx);
        Sexpression currentAdvertiser =
                outputAdvertiserMenu(sw, advertisers, httpParams);
        //=================
        if(currentAdvertiser != Null.nil)
        {
            Sexpression campaigns =
                    getCampaigns(currentAdvertiser.car(), qctx);
            if(campaigns != Null.nil)
            {
                //--------------------------------
                sw.append("\n<H3>Multiple&nbsp;campaigns?&nbsp;");
                RadioBooleanWidget.radioButton
                        (sw, MULTIPLE_CAMPAIGNS,
                         RadioBooleanWidget.YES_PARAM, multipleCampaigns,
                         "Yes&nbsp;", true);
                sw.append("&nbsp;");
                RadioBooleanWidget.radioButton
                        (sw, MULTIPLE_CAMPAIGNS,
                         RadioBooleanWidget.NO_PARAM, !multipleCampaigns,
                         "No&nbsp;", true);
                sw.append("</H3>");
                //--------------------------------
                Sexpression currentCampaigns =
                       outputParentCampaignMenu
                               (sw, campaigns, httpParams, multipleCampaigns);
                if(!multipleCampaigns && currentCampaigns != Null.nil)
                    currentCampaigns = Cons.list(currentCampaigns);
                // System.out.println("CurrentCampaign: " + currentCampaign);
                if(currentCampaigns != Null.nil)
                {
                    Sexpression hours =
                            getHours(currentAdvertiser.car(),
                                     Cons.mapNth(0, currentCampaigns), qctx);
                    hours = Cons.sort(hours, Cons.Lessp, Cons.identityKey);
                    TimeZoneData tzd =
                            outputTimeZoneWidgets
                                    (sw, bidder,
                                     currentAdvertiser.car().unboxLong(),
                                     currentCampaigns.car().car().unboxLong(),
                                     httpParams, qctx);
                    System.out.println("Exporting using timezone: " +
                                       tzd.getWrtTimeZone().getDisplayName());
                    TimeScale ts =
                         outputTimeScaleMenu
                                 (sw, TIMESCALE, TIMESCALE_TITLE,
                                  httpParams, false, timeScaleSpringLoaded);
                    Sexpression filteredHours =
                      filterHoursForTimeScale(hours, ts, tzd.getWrtTimeZone());
                    Sexpression currentStartHour =
                         outputHourMenu(sw, filteredHours, START_HOUR,
                                        START_HOUR_TITLE, httpParams, false,
                                        tzd.getLocalTimeZone(),
                                        tzd.getWrtTimeZone(),
                                        hoursSpringLoaded, ts);
                    Sexpression currentEndHour =
                         outputHourMenu(sw, filteredHours, END_HOUR,
                                        END_HOUR_TITLE, httpParams, true,
                                        tzd.getLocalTimeZone(),
                                        tzd.getWrtTimeZone(),
                                        hoursSpringLoaded, ts);
                    List<String> selectedMeasures =
                            outputMeasureCheckBoxes
                                (sw, "Measures:", httpParams, false);
                    if(!admin)
                    {
                        sw.append("\n<H3><INPUT TYPE=\"SUBMIT\" NAME=\"" +
                                DOIT_PARAM + "\" " +
                                "VALUE=\"Save to server\">: " +
                                "<INPUT TYPE=\"text\" " +
                                "SIZE=\"60\" " +
                                "NAME=\"" + SAVE_TO_FILE_PARAM +
                                "\" VALUE=\"");
                        if(savePath != null)
                            sw.append(escapeHtml(savePath));
                        sw.append("\">");
                        sw.append("</H3>");
                    }
                    sw.append("\n<H3>");
                    sw.append("\n    <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                                SHOW_GRAPH_PARAM + "\" VALUE=\"Show Graph\">");
                    sw.append("\n    <INPUT TYPE=\"SUBMIT\" NAME=\"" +
                                SHOW_GRAPH_IMAGE_PARAM +
                                "\" VALUE=\"Show Graph As Image\">");
                    sw.append("\n    <INPUT TYPE=\"BUTTON\" NAME=\"" +
                                            DOWNLOAD_PARAM + "\" "
                                + "VALUE=\"Download Spreadsheet\" onClick=\"");
                    sw.append("window.location.href='");
                    sw.append(downloadURL
                                  (currentAdvertiser, currentCampaigns,
                                   currentStartHour, currentEndHour, ts,
                                   tzd.getWrtTimeZone().getDisplayName(),
                                   tzd.getLocalTimeZone(),
                                   tzd.getWrtTimeZone(), DownloadType.XLS,
                                   multipleCampaigns, httpParams));
                    sw.append("'\">");
                    /*
                    sw.append("\n    <INPUT TYPE=\"BUTTON\" "
                                + "NAME=\"" + DOWNLOAD_IMAGE_PARAM + "\" "
                                + "VALUE=\"Download Graph\"  onClick=\"");
                    sw.append("window.location.href='");
                    sw.append(downloadURL
                                  (currentAdvertiser, currentCampaigns,
                                   currentStartHour, currentEndHour, ts,
                                   tzd.getWrtTimeZone().getDisplayName(),
                                   tzd.getLocalTimeZone(),
                                   tzd.getWrtTimeZone(), DownloadType.IMAGE,
                                   multipleCampaigns, httpParams));
                    sw.append("'\">");
                    */
                    sw.append("</H3>");
                    if(currentStartHour != Null.nil &&
                       currentEndHour != Null.nil
                       // && !currentStartHour.equals(currentEndHour)
                            )
                    {
                        if(!Sexpression.Lessp.test
                                (currentStartHour, currentEndHour))
                        {
                            Sexpression temp = currentStartHour;
                            currentStartHour = currentEndHour;
                            currentEndHour = temp;
                        }
                        Sexpression advertiserName =
                                currentAdvertiser.second();
                        String advertiserNameS = advertiserName.unboxString();
                        sw.append("\n<HR>\n");
                        if(doitP && (savePath == null || "".equals(savePath)))
                            warn(sw, "No path specified");
                        else if(doitP || downloadP || downloadFileP ||
                                downloadImageFileP)
                        {
                            headers = handleDoIt(stream, sw, os, returnHeaders,
                                     impl, currentAdvertiser, currentCampaigns,
                                     currentStartHour, currentEndHour,
                                     downloadP, downloadImageP, downloadFileP,
                                     downloadImageFileP, headers,
                                     ts, savePath,
                                     tzd.getWrtTimeZone().getDisplayName(),
                                     tzd.getWrtTimeZone(),
                                     multipleCampaigns, httpParams,
                                     selectedMeasures);
                        }
                        else if(showGraphImageP)
                        {
                            sw.append("\n    <IMG SRC='");
                            sw.append(downloadURL
                                     (currentAdvertiser, currentCampaigns,
                                      currentStartHour, currentEndHour, ts,
                                      tzd.getWrtTimeZone().getDisplayName(),
                                      tzd.getLocalTimeZone(),
                                      tzd.getWrtTimeZone(), DownloadType.IMAGE,
                                      multipleCampaigns, httpParams));
                            sw.append("'\">");
                        }
                        else if(showGraphP)
                        {
                            List<PerformanceCurve> curves =
                                    new Vector<PerformanceCurve>();
                            Long advertiserId =
                                    currentAdvertiser.car().unboxLong();
                            TimeZone wrtTimeZone = tzd.getWrtTimeZone();
                            try
                            {
                                Sexpression l = currentCampaigns;
                                while(l != Null.nil)
                                {
                                    Long campaignId =
                                            l.car().car().unboxLong();
                                    String campaignName =
                                            l.car().second().unboxString();
                                    List<PerformanceHistoryRow> points =
                                        impl.getCampaignPerformanceHistory
                                        (advertiserId, campaignId, ts,
                                         currentStartHour.unboxDate(),
                                         TimeScaleIterator.timeCeiling
                                             (currentEndHour.unboxDate(),
                                              ts, wrtTimeZone),
                                         wrtTimeZone);
                                    if(points == null || points.size() == 0) {}
                                    else curves.add
                                            (new PerformanceCurve
                                                    (points, campaignId,
                                                     campaignName));
                                    l = l.cdr();
                                }
                                if(curves.size() == 0) {}
                                else emitGraphForCurve
                                            (sw, advertiserId, advertiserNameS,
                                             curves, ts, wrtTimeZone,
                                             selectedMeasures);
                            }
                            catch (Exception e)
                            {
                                throw Utils.barf(e, this);
                            }
                        }
                        else {}
                    }
                    else warn(sw, "No valid dates found for this campaign!");
                }
                else warn(sw, "No campaign found for this advertiser!");
            }
            else warn(sw, "No campaigns for this advertiser!");
        }
        else warn(sw, "No advertiser found!");
        HTMLifier.finishHTMLPage(sw);
        if(!downloadFileP && !downloadImageFileP) stream.append(sw.toString());
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
        HTTPListener.registerHandlerClass(BidHistoryHTTPHandler.class);
    }
    static
    { register(); }
}

enum ImageFormat { PNG, JPG }

enum DownloadType { XLS, IMAGE }

class CurveSpec {
    String name;
    String accessor;
    Set<CurveStyle> curveStyles;
    Annotator annotator;

    public CurveSpec(String name, String accessor)
    {
        this.name = name;
        this.accessor = accessor;
        this.curveStyles = new HashSet<CurveStyle>();
        this.annotator = null;
    }

    public CurveSpec(String name, String accessor, CurveStyle[] curveStyles,
                     Annotator annotator)
    {
        this.name = name;
        this.accessor = accessor;
        this.curveStyles = new HashSet<CurveStyle>();
        this.curveStyles.addAll(Arrays.asList(curveStyles));
        this.annotator = annotator;
    }

    public static List<String> curveNames(CurveSpec[] specs)
    {
        List<String> res = new Vector<String>();
        for(CurveSpec spec: specs)
        {
            res.add(spec.name);
        }
        return res;
    }

    @SuppressWarnings("unused")
    public static List<Long> idList(Long id, CurveSpec[] specs)
    {
        List<Long> res = new Vector<Long>();
        for(CurveSpec spec: specs)
        {
            res.add(id);
        }
        return res;
    }

    public static List<Set<CurveStyle>> styles(CurveSpec[] specs)
    {
        List<Set<CurveStyle>> res = new Vector<Set<CurveStyle>>();
        for(CurveSpec spec: specs)
        {
            res.add(spec.curveStyles);
        }
        return res;
    }
}

abstract class Annotator {

    public abstract String computeAnnotation
            (int i, List<PerformanceHistoryRow> curve);

}

class ChangesAnnotator extends Annotator {

    public String computeAnnotation
            (int i, List<PerformanceHistoryRow> curve)
    {
        if(BidHistoryHTTPHandler.showAnnotations)
        {
            String summary;
            PerformanceHistoryRow row = curve.get(i);
            summary = row.getChangesSummary();
            return summary;
        }
        else return "";
    }

}