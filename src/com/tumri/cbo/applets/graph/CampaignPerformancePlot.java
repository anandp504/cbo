package com.tumri.cbo.applets.graph;

import java.applet.AppletContext;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLEncoder;

import javax.swing.*;

import org.jfree.chart.*;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.RangeType;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.StandardGradientPaintTransformer;
import org.jfree.ui.Layer;
import org.jfree.ui.TextAnchor;

public class CampaignPerformancePlot extends JApplet {

    private static final long serialVersionUID = 7526472295622776147L;

    static String[] suffices =
            { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
    static SimpleDateFormat    hourlyFormat() { return new SimpleDateFormat("yyyy-MM-dd HH"); }
    static SimpleDateFormat     dailyFormat() { return new SimpleDateFormat("yyyy-MM-dd"); }
    static SimpleDateFormat    weeklyFormat() { return new SimpleDateFormat("yyyy-ww"); }
    static SimpleDateFormat   monthlyFormat() { return new SimpleDateFormat("yyyy-MM"); }
    static SimpleDateFormat    yearlyFormat() { return new SimpleDateFormat("yyyy"); }
    static SimpleDateFormat  hourlyFormatTZ() { return new SimpleDateFormat("yyyy-MM-dd HH z"); }
    static SimpleDateFormat   dailyFormatTZ() { return new SimpleDateFormat("yyyy-MM-dd z"); }
    static SimpleDateFormat  weeklyFormatTZ() { return new SimpleDateFormat("yyyy-ww z"); }
    static SimpleDateFormat monthlyFormatTZ() { return new SimpleDateFormat("yyyy-MM z"); }
    static SimpleDateFormat  yearlyFormatTZ() { return new SimpleDateFormat("yyyy z"); }
    static SimpleDateFormat  transferFormat() { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); }

    static final String HOURLYTZ = "HOURLYTZ";
    static final String DAILYTZ = "DAILYTZ";
    static final String WEEKLYTZ = "WEEKLYTZ";
    static final String MONTHLYTZ = "MONTHLYTZ";
    static final String YEARLYTZ = "YEARLYTZ";

    static Map<String, SimpleDateFormat> initFormatMap
            (Calendar tzCal, Calendar localCal)
    {
        Map<String, SimpleDateFormat> map =
                new HashMap<String, SimpleDateFormat>();
        map.put("HOURLY", hourlyFormat());
        map.put("DAILY", dailyFormat());
        map.put("WEEKLY", weeklyFormat());
        map.put("MONTHLY", monthlyFormat());
        map.put("YEARLY", yearlyFormat());
        map.put(HOURLYTZ, hourlyFormatTZ());
        map.put(DAILYTZ, dailyFormatTZ());
        map.put(WEEKLYTZ, weeklyFormatTZ());
        map.put(MONTHLYTZ, monthlyFormatTZ());
        map.put(YEARLYTZ, yearlyFormatTZ());
        for(String k: map.keySet())
        {
            SimpleDateFormat f = map.get(k);
            if("HOURLY".equals(k))
                f.setCalendar(tzCal);
            else f.setCalendar(localCal);
        }
        return map;
    }

    static SimpleDateFormat getFormat
            (String name, Map<String, SimpleDateFormat> formatMap)
    {
        return formatMap.get(name);
    }

    public static RegularTimePeriod toTimePeriod
            (String s, String timeScale, TimeZone tz,
             Map<String, SimpleDateFormat> formatMap, Locale locale)
    {
        Date d;
        Hour h;
        Month m;
        Day day;
        Date mdate;
        RegularTimePeriod res;
        try
        {
            if("HOURLY".equals(timeScale))
            {
                try
                {
                    d = getFormat(HOURLYTZ, formatMap).parse(s);
                    h = new Hour(d, tz, locale);
                    res = h;
                }
                catch (ParseException e)
                {
                    d = formatMap.get(timeScale).parse(s);
                    h = new Hour(d);
                    res = h;
                }
            }
            else if("DAILY".equals(timeScale))
            {
                try
                {
                    Date date = getFormat(DAILYTZ, formatMap).parse(s);
                    day = new Day(date);
                    res = day;
                }
                catch (ParseException e)
                {
                    Date date = formatMap.get(timeScale).parse(s);
                    day = new Day(date);
                    res = day;
                }
            }
            else if("WEEKLY".equals(timeScale))
            {
                try { res = new Week(getFormat(WEEKLYTZ, formatMap).parse(s)); }
                catch (ParseException e)
                { res = new Week(formatMap.get(timeScale).parse(s)); }
            }
            else if("MONTHLY".equals(timeScale))
            {
                try 
                {
                    m = new Month(getFormat(MONTHLYTZ, formatMap).parse(s));
                    res = m;
                }
                catch (ParseException e)
                { res = new Month(formatMap.get(timeScale).parse(s)); }
            }
            else if("QUARTERLY".equals(timeScale))
            {
                Pattern tzp = Pattern.compile("(\\d{4})-Q(\\d) (.*)");
                Matcher tzm = tzp.matcher(s);
                Pattern p = Pattern.compile("(\\d{4})-Q(\\d)");
                Matcher mat = p.matcher(s);
                if(tzm.find())
                {
                    String yearString = tzm.group(1);
                    String quarterString = tzm.group(2);
                    String tzString = tzm.group(3);
                    if(yearString != null && quarterString != null &&
                       tzString != null)
                    {
                        int year = Integer.parseInt(yearString);
                        int quarter = Integer.parseInt(quarterString);
                        int month = ((quarter - 1) * 3) + 1;
                        s = year + "-" + month + " "+ tzString;
                        mdate = getFormat(MONTHLYTZ, formatMap).parse(s);
                        res = new Quarter(mdate, tz, Locale.getDefault());
                    }
                    else throw new Error("Mal-formatted QUARTERLY value" +
                                         s + ", " + tz);
                }
                else if(mat.find())
                {
                    String yearString = mat.group(1);
                    String quarterString = mat.group(2);
                    if(yearString != null && quarterString != null)
                    {
                        int year = Integer.parseInt(yearString);
                        int quarter = Integer.parseInt(quarterString);
                        int month = ((quarter - 1) * 3) + 1;
                        s = year + "-" + month + " "+ tz.getID();
                        mdate = getFormat(MONTHLYTZ, formatMap).parse(s);
                        res = new Quarter(mdate, tz, Locale.getDefault());
                    }
                    else throw new Error("Mal-formatted QUARTERLY value" +
                                         s + ", " + tz);
                }
                else throw new Error("Mal-formatted QUARTERLY value" +
                                     s + ", " + tz);
            }
            else if("YEARLY".equals(timeScale))
            {
                try { res = new Year(getFormat(YEARLYTZ, formatMap).parse(s));}
                catch (ParseException e)
                { res = new Year(formatMap.get(timeScale).parse(s)); }
            }
            else throw new Error("Unhandled timeScale: " + timeScale);
        }
        catch (ParseException e)
        {
            throw new Error("Parse error parsing: " + s + " as " + timeScale);
        }
        return res;
    }

    public static Map<String, SimpleDateFormat> getFormatMap(TimeZone tz)
    {
        Calendar tzCalendar = Calendar.getInstance();
        tzCalendar.setTimeZone(tz);
        Calendar localCalendar = Calendar.getInstance();
        return initFormatMap(tzCalendar, localCalendar);
    }

    static void prepareCurveGroup(String timeScale, TimeZone tz,
                                  String[] xPoints, CurveGroup cg,
                                  Map<String, SimpleDateFormat> formatMap,
                                  Locale locale)
    {
        for(int curveIndex = 0; curveIndex < cg.curvesSize(); curveIndex++)
        {
            String name = cg.getName(curveIndex);
            double[] yPoints = cg.getCurve(curveIndex);
            TimeSeries ts = new TimeSeries(name);
            Set<CurveStyle> curveStyles = cg.getStyles(curveIndex);
            boolean excludeNonPositive =
                    curveStyles.contains(CurveStyle.EXCLUDE_NON_POSITIVE);
            int i = 0;
            for(double y: yPoints)
            {
                if(!excludeNonPositive || y > 0.0d)
                {
                    String x = xPoints[i];
                    RegularTimePeriod xr =
                            toTimePeriod(x, timeScale, tz, formatMap, locale);
                    ts.add(xr, y);
                }
                i = i + 1;
            }
            Long advertiserId = cg.getAdvertiserId(curveIndex);
            Long campaignId = cg.getCampaignId(curveIndex);
            TimeSeriesCollection dataset =
                    new CampaignTimeSeriesCollection
                            (name, advertiserId, campaignId);
            dataset.addSeries(ts);
            cg.addDataSet(dataset);
        }
    }

    public static JFreeChart prepareChart
            (String title, String xName, String timeScale, TimeZone tz,
             String[] xPoints, CurveGroup left, CurveGroup leftLeft,
             CurveGroup right, CurveGroup rightRight, Locale locale)
    {
        Map<String, SimpleDateFormat> formatMap = getFormatMap(tz);
        prepareCurveGroup(timeScale, tz, xPoints, left, formatMap, locale);
        prepareCurveGroup(timeScale, tz, xPoints, leftLeft, formatMap, locale);
        prepareCurveGroup(timeScale, tz, xPoints, right, formatMap, locale);
        prepareCurveGroup(timeScale, tz, xPoints, rightRight,formatMap,locale);
        return createChart(title, xName, left, leftLeft, right, rightRight,
                           timeScale, tz);
    }

    public ChartPanel prepareChartPanel
            (String title, String xName, String timeScale, TimeZone tz,
             String[] xPoints, CurveGroup left, CurveGroup leftLeft,
             CurveGroup right, CurveGroup rightRight, Locale locale)
    {
        JFreeChart chart = prepareChart(title, xName, timeScale, tz, xPoints,
                                        left, leftLeft, right, rightRight,
                                        locale);
        ChartPanel panel =
                new ChartPanel
                       (chart,
                        ChartPanel.DEFAULT_WIDTH,
                        ChartPanel.DEFAULT_HEIGHT,
                        ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH,
                        ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT,
                        ChartPanel.DEFAULT_MAXIMUM_DRAW_WIDTH,
                        ChartPanel.DEFAULT_MAXIMUM_DRAW_HEIGHT,
                        ChartPanel.DEFAULT_BUFFER_USED,
                        true,  // properties
                        false, // save  -- because Applets can't save
                        false, // print -- because Applets can't print
                        true,  // zoom
                        true); // tooltips
        panel.setMouseWheelEnabled(true);
        panel.addChartMouseListener(new MouseListener(this));
        return panel;
    }

    @SuppressWarnings("EmptyCatchBlock")
    void recordParams(String curveNameParam, String curveStylesParam,
                      String curveAdvertiserParam, String curveCampaignParam,
                      String yPointsParam, String annotationsParam,
                      CurveGroup cg)
    {
        for(String suffix: suffices)
        {
            String name = getParameter(curveNameParam + suffix);
            String advertiser = getParameter(curveAdvertiserParam + suffix);
            String styles = getParameter(curveStylesParam + suffix);
            String campaign = getParameter(curveCampaignParam + suffix);
            String yPointsS  = getParameter(yPointsParam + suffix);
            String annotationsS  = getParameter(annotationsParam + suffix);
            if(name != null && yPointsS != null)
            {
                cg.addCurveName(name);
                Set<CurveStyle> curveStyles = new HashSet<CurveStyle>();
                if(styles != null)
                {
                    String[] split = styles.split(",");
                    for(String s: split)
                    {
                        try
                        {
                            CurveStyle cs = CurveStyle.valueOf(s.trim());
                            curveStyles.add(cs);
                        }
                        catch(IllegalArgumentException e)
                        {
                            System.out.println("Missing style enum: " + s);
                        }
                    }
                }
                List<String> annotations = new Vector<String>();
                if(annotationsS != null)
                {
                    String[] split = annotationsS.split(",");
                    for(String s: split)
                    {
                        try
                        {
                            String decoded =
                                    URLDecoder.decode(s.trim(), "UTF-8");
                            annotations.add(decoded);
                        }
                        catch(UnsupportedEncodingException e)
                        {
                            throw new Error(e);
                        }
                    }
                }
                if(advertiser != null)
                    cg.addCurveAdvertiser(Long.parseLong(advertiser));
                if(campaign != null)
                    cg.addCurveCampaign(Long.parseLong(campaign));
                cg.addCurveStyles(curveStyles);
                cg.addAnnotations(annotations);
                double[] yPoints = AbstractGraph.toDoubleArray(yPointsS);
                cg.addCurve(yPoints);
            }
        }
    }

    public void init()
    {
        String title = getParameter("title");
        String xPointsS  = getParameter("xPoints");
        String[] xPoints = xPointsS.split(",");
        String xName = getParameter("xAxisName");
        String leftYAxisName = getParameter("leftYAxisName");
        String leftLeftYAxisName = getParameter("leftLeftYAxisName");
        String rightYAxisName = getParameter("rightYAxisName");
        String rightRightYAxisName = getParameter("rightRightYAxisName");
        String timeScale = getParameter("timeScale");
        String timeZoneS = getParameter("timeZone");
        TimeZone tz = TimeZone.getTimeZone(timeZoneS);
        Locale locale = Locale.getDefault();
        List<String> leftCurveNames = new Vector<String>();
        List<String> leftLeftCurveNames = new Vector<String>();
        List<Set<CurveStyle>> leftCurveStylesList
                = new Vector<Set<CurveStyle>>();
        List<Set<CurveStyle>> leftLeftCurveStylesList
                = new Vector<Set<CurveStyle>>();
        List<Long> leftCurveAdvertisers = new Vector<Long>();
        List<Long> leftLeftCurveAdvertisers = new Vector<Long>();
        List<Long> leftCurveCampaigns = new Vector<Long>();
        List<Long> leftLeftCurveCampaigns = new Vector<Long>();
        List<double[]> leftCurves = new Vector<double[]>();
        List<double[]> leftLeftCurves = new Vector<double[]>();
        List<String> rightCurveNames = new Vector<String>();
        List<String> rightRightCurveNames = new Vector<String>();
        List<Set<CurveStyle>> rightCurveStylesList
                = new Vector<Set<CurveStyle>>();
        List<Set<CurveStyle>> rightRightCurveStylesList
                = new Vector<Set<CurveStyle>>();
        List<Long> rightCurveAdvertisers = new Vector<Long>();
        List<Long> rightRightCurveAdvertisers = new Vector<Long>();
        List<Long> rightCurveCampaigns = new Vector<Long>();
        List<Long> rightRightCurveCampaigns = new Vector<Long>();
        List<double[]> rightCurves = new Vector<double[]>();
        List<double[]> rightRightCurves = new Vector<double[]>();
        CurveGroup left =
                new CurveGroup(leftYAxisName, leftCurveNames, leftCurves,
                               leftCurveStylesList, leftCurveAdvertisers,
                               leftCurveCampaigns);
        CurveGroup leftLeft =
                new CurveGroup(leftLeftYAxisName, leftLeftCurveNames,
                               leftLeftCurves, leftLeftCurveStylesList,
                               leftLeftCurveAdvertisers,
                               leftLeftCurveCampaigns);
        CurveGroup right =
                new CurveGroup(rightYAxisName, rightCurveNames, rightCurves,
                               rightCurveStylesList, rightCurveAdvertisers,
                               rightCurveCampaigns);
        CurveGroup rightRight =
                new CurveGroup(rightRightYAxisName, rightRightCurveNames,
                               rightRightCurves, rightRightCurveStylesList,
                               rightRightCurveAdvertisers,
                               rightRightCurveCampaigns);
        recordParams("leftCurveName", "leftCurveStyles",
                     "leftCurveAdvertiser", "leftCurveCampaign",
                     "leftYPoints", "leftAnnotations", left);
        recordParams("leftLeftCurveName", "leftLeftCurveStyles",
                     "leftLeftCurveAdvertiser", "leftLeftCurveCampaign",
                     "leftLeftYPoints", "leftLeftAnnotations", leftLeft);
        recordParams("rightCurveName", "rightCurveStyles",
                     "rightCurveAdvertiser", "rightCurveCampaign",
                     "rightYPoints", "rightAnnotations", right);
        recordParams("rightRightCurveName", "rightRightCurveStyles",
                     "rightRightCurveAdvertiser", "rightRightCurveCampaign",
                     "rightRightYPoints", "rightRightAnnotations", rightRight);
        ChartPanel panel = prepareChartPanel
                (title, xName, timeScale, tz, xPoints, left, leftLeft, right,
                 rightRight, locale);
        setContentPane(panel);
    }

    public void start()
    {
    }

    public void stop()
    {
    }

    static final Color veryLightGray = new Color(220, 220, 220);

    static int toolTipDelay = 50;

    public static Paint[] createDefaultPaintArray() {

        return new Paint[] {
            ChartColor.DARK_RED,
            ChartColor.DARK_GREEN,
            ChartColor.DARK_BLUE,
            ChartColor.DARK_YELLOW,
            ChartColor.DARK_MAGENTA,
            ChartColor.DARK_CYAN,
            Color.darkGray,
            new Color(0xFF, 0x55, 0x55),
            new Color(0x55, 0x55, 0xFF),
            new Color(0x55, 0xFF, 0x55),
            new Color(0xFF, 0xFF, 0x55),
            new Color(0xFF, 0x55, 0xFF),
            new Color(0x55, 0xFF, 0xFF),
            Color.pink,
            Color.gray,
            ChartColor.LIGHT_RED,
            ChartColor.LIGHT_BLUE,
            ChartColor.LIGHT_GREEN,
            ChartColor.LIGHT_YELLOW,
            ChartColor.LIGHT_MAGENTA,
            ChartColor.LIGHT_CYAN,
            Color.lightGray,
            ChartColor.VERY_DARK_RED,
            ChartColor.VERY_DARK_BLUE,
            ChartColor.VERY_DARK_GREEN,
            ChartColor.VERY_DARK_YELLOW,
            ChartColor.VERY_DARK_MAGENTA,
            ChartColor.VERY_DARK_CYAN,
            ChartColor.VERY_LIGHT_RED,
            ChartColor.VERY_LIGHT_BLUE,
            ChartColor.VERY_LIGHT_GREEN,
            ChartColor.VERY_LIGHT_YELLOW,
            ChartColor.VERY_LIGHT_MAGENTA,
            ChartColor.VERY_LIGHT_CYAN
        };
    }

    static XYLineAndShapeRenderer selectRenderer(Set<CurveStyle> curveStyles)
    {
        if(curveStyles.contains(CurveStyle.SCATTER))
        {
            XYLineAndShapeRenderer renderer =
                    new XYLineAndShapeRenderer(false, true);
            renderer.setBaseShapesVisible(true);
            renderer.setBaseToolTipGenerator
                    (StandardXYToolTipGenerator.getTimeSeriesInstance());
            return renderer;
        }
        else
        {
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setBaseShapesVisible(true);
            renderer.setBaseToolTipGenerator
                    (StandardXYToolTipGenerator.getTimeSeriesInstance());
            return renderer;
        }
    }

    static int addCurves(XYPlot plot, DrawingSupplier ds, CurveGroup cg,
                         int dataSetIndex, int yAxisIndex, int startIndex)
    {
        int i = 0;
        XYTextAnnotation annotation;
        Font font = new Font("SansSerif", Font.PLAIN, 9);
        ValueAxis domainAxis = plot.getDomainAxis(0);
        ValueAxis rangeAxis = plot.getRangeAxis(yAxisIndex);
        for(XYDataset dataSet: cg.getDataSets())
        {
            if(i >= startIndex)
            {
                int series = 0;
                Set<CurveStyle> curveStyles = cg.getStyles(i);
                List<String> annotations = cg.getAnnotations(i);
                plot.setDataset(dataSetIndex, dataSet);
                plot.mapDatasetToRangeAxis(dataSetIndex, yAxisIndex);
                XYLineAndShapeRenderer renderer = selectRenderer(curveStyles);
                Paint paint = ds.getNextPaint();
                // System.out.println("dataSetIndex: " + dataSetIndex + ", " + paint);
                renderer.setSeriesPaint(dataSetIndex, paint);
                plot.setRenderer(dataSetIndex, renderer);
                dataSetIndex = dataSetIndex + 1;
                if(curveStyles.contains(CurveStyle.ANNOTATE))
                {
                    for(int item = 0; item < dataSet.getItemCount(series);
                        item++)
                    {
                        double x = dataSet.getXValue(series, item);
                        double y = dataSet.getYValue(series, item);
                        String annotationString =
                                (annotations.size() > item
                                    ? annotations.get(item)
                                    : null);
                        if(annotationString != null &&
                                !annotationString.equals(""))
                        {
                            annotation = new ExplicitAxisXYTextAnnotation
                                    (domainAxis, rangeAxis,
                                     "  " + annotationString, x, y);
                            annotation.setFont(font);
                            annotation.setTextAnchor(TextAnchor.TOP_LEFT);
                            plot.addAnnotation(annotation);
                        }
                    }
                }
            }
            i = i + 1;
        }
        return dataSetIndex;
    }

    static final String HOURLY = "HOURLY";
    static final String DAILY  = "DAILY";

    public static JFreeChart createChart
            (String title, String xAxisName,
             CurveGroup left,
             CurveGroup leftLeft,
             CurveGroup right,
             CurveGroup rightRight,
             String timeScale,
             TimeZone tz) {
        ToolTipManager.sharedInstance().setInitialDelay(toolTipDelay);
        int leftYAxisIndex = 0;
        int rightYAxisIndex = 1;
        int leftLeftYAxisIndex = 2;
        int rightRightYAxisIndex = 3;
        int dataSetIndex = 0;
        if(left.size() == 0 && leftLeft.size() != 0)
        {
            left = leftLeft;
            leftLeft = null;
        }
        if(right.size() == 0 && rightRight.size() != 0)
        {
            right = rightRight;
            rightRight = null;
        }
        if(left.size() == 0)
        {
            left = right;
            leftLeft = rightRight;
            right = null;
            rightRight = null;
        }
        TimeSeriesCollection dataset0 =
                (TimeSeriesCollection)left.getDataSet(dataSetIndex);
        dataSetIndex = dataSetIndex + 1;
        JFreeChart chart =
                AnnotationHackingChartFactory.createTimeSeriesChart(
            title,
            xAxisName,
            left.getYAxisName(),
            dataset0,
            true,     // legend?
            true,      // tooltips?
            false      // URLs?
        );

        chart.setBackgroundPaint(Color.white);
        XYPlot plot = (XYPlot) chart.getPlot();
        // Colour setup is handled in the DrawingSupplier.
        DrawingSupplier ds; //  = plot.getDrawingSupplier();
        ds = new DefaultDrawingSupplier
                (createDefaultPaintArray(), // DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
                 DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
                 DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                 DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                 DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                 DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(veryLightGray);
        plot.setRangeGridlinePaint(Color.white);

        // The primary left Y axis
        NumberAxis axis0 = (NumberAxis)plot.getRangeAxis(0);
        axis0.setRangeType(RangeType.POSITIVE);
        axis0.setAutoRangeIncludesZero(true);

        // The leftLeft Y axis
        if(leftLeft != null && leftLeft.size() != 0)
        {
            NumberAxis axis2 = new NumberAxis(leftLeft.getYAxisName());
            axis2.setRangeType(RangeType.POSITIVE);
            axis2.setAutoRangeIncludesZero(true);
            plot.setRangeAxis(leftLeftYAxisIndex, axis2);
            plot.setRangeAxisLocation
                    (leftLeftYAxisIndex, AxisLocation.BOTTOM_OR_LEFT);
        }

        // The primary right Y axis
        if(right != null && right.size() != 0)
        {
            NumberAxis axis1 = new NumberAxis(right.getYAxisName());
            axis1.setRangeType(RangeType.POSITIVE);
            axis1.setAutoRangeIncludesZero(true);
            plot.setRangeAxis(rightYAxisIndex, axis1);
        }

        // The rightRight Y axis
        if(rightRight != null && rightRight.size() != 0)
        {
            NumberAxis axis3 = new NumberAxis(rightRight.getYAxisName());
            axis3.setRangeType(RangeType.POSITIVE);
            axis3.setAutoRangeIncludesZero(true);
            plot.setRangeAxis(rightRightYAxisIndex, axis3);
        }
        //-------------
        // Generic setup.
        XYItemRenderer renderer = plot.getRenderer();
        renderer.setBaseToolTipGenerator(
                StandardXYToolTipGenerator.getTimeSeriesInstance());
        if (renderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer rr = (XYLineAndShapeRenderer) renderer;
            rr.setBaseShapesVisible(true);
            rr.setBaseShapesFilled(true);
        }
        // Do all of the left curves.
        //------------------------
        dataSetIndex = addCurves(plot, ds,  left, dataSetIndex,
                                 leftYAxisIndex, 1);
        if(leftLeft != null && leftLeft.size() != 0)
            dataSetIndex =
                addCurves(plot, ds, leftLeft, dataSetIndex,
                          leftLeftYAxisIndex, 0);
        // Now do all of the right curves.
        //------------------------
        if(right != null && right.size() != 0)
            dataSetIndex =
                addCurves(plot, ds, right, dataSetIndex,
                          rightYAxisIndex, 0);
        if(rightRight != null && rightRight.size() != 0)
            // dataSetIndex =
                addCurves(plot, ds, rightRight, dataSetIndex,
                          rightRightYAxisIndex, 0);
        // Handle weekends, ....
        if(HOURLY.equals(timeScale) || DAILY.equals(timeScale))
        {
            GradientPaint c = new GradientPaint
                           //(0.0f, 0.0f, Color.red, 1.0f, 1.0f, Color.orange);
                             (0.0f, 0.0f, Color.gray, 1.0f, 1.0f, Color.lightGray);
            Calendar calStart = Calendar.getInstance();
            calStart.setTimeZone(tz);
            Calendar calEnd = Calendar.getInstance();
            calEnd.setTimeZone(tz);
            int seriesIndex = 0;
            TimeSeries series = dataset0.getSeries(seriesIndex);
            Integer lastDow = null;
            long lastEnd = 0;
            Long weekEndStart = null;
            Long weekEndEnd = null;
            List<long[]> weekends = new Vector<long[]>();
            for(int item = 0; item < series.getItemCount(); item++)
            {
                TimeSeriesDataItem x = series.getDataItem(item);
                RegularTimePeriod p = x.getPeriod();
                long start = p.getFirstMillisecond();
                long end   = p.getLastMillisecond();
                calStart.setTimeInMillis(start);
                int dow = calStart.get(Calendar.DAY_OF_WEEK);
                if(weekEndStart == null &&
                   (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) &&
                   (lastDow == null || lastDow == Calendar.FRIDAY))
                {
                    weekEndStart = start;
                }
                if(weekEndStart != null && weekEndEnd == null &&
                   lastDow != null && dow == Calendar.MONDAY)
                {
                    weekEndEnd = lastEnd;
                }
                lastDow = dow;
                lastEnd = end;
                if(weekEndStart != null && weekEndEnd != null)
                {
                    long[] newWe = new long[]{ weekEndStart, weekEndEnd };
                    weekends.add(newWe);
                    weekEndStart = null;
                    weekEndEnd = null;
                }
            }
            if(weekEndStart != null)
            {
                long[] newWe = new long[]{ weekEndStart, lastEnd };
                weekends.add(newWe);
            }
            for(long[] weekend: weekends)
            {
                IntervalMarker mrkr =
                        new IntervalMarker
                                  (weekend[0], weekend[1],
                                   c, new BasicStroke(2.0f), null, null, 1.0f);
                mrkr.setGradientPaintTransformer
                        (new StandardGradientPaintTransformer(
                         GradientPaintTransformType.HORIZONTAL));
                plot.addDomainMarker(mrkr, Layer.BACKGROUND);
            }
        }
        return chart;
    }

    public static void main(String[] args) {
        CampaignPerformancePlot demo = new CampaignPerformancePlot();
        // demo.pack();
        // RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);
    }

}

class MouseListener implements ChartMouseListener {
    JApplet parent;

    public MouseListener(JApplet parent)
    {
        this.parent = parent;
    }

    @SuppressWarnings("unused")
    static String computePerspectiveForClick(String name)
    {
        // Check name to get the right perspective.
        // "Combined change history" is the default.
        return "Combined change history";
    }

    // Low-rent escape to save us including another jar.
    static String escape(String s)
    {
        return s.replace(" ", "+").replace(":", "%3A");
    }

    public void chartMouseClicked(ChartMouseEvent event) {
        ChartEntity entity = event.getEntity();
        if (entity instanceof XYItemEntity)
        {
            XYItemEntity xyEnt = (XYItemEntity) entity;
            XYDataset d = xyEnt.getDataset();
            if(d instanceof CampaignTimeSeriesCollection)
            {
                CampaignTimeSeriesCollection dataset =
                        (CampaignTimeSeriesCollection)d;
                String name = dataset.name;
                int item = xyEnt.getItem();
                int series = xyEnt.getSeriesIndex();
                TimeSeries ts = dataset.getSeries(series);
                TimeSeriesDataItem dataItem = ts.getDataItem(item);
                TimePeriod period = dataItem.getPeriod();
                Date startDate = period.getStart();
                String hash =
                        escape(CampaignPerformancePlot.transferFormat().format
                                        (startDate));
                Number value = dataItem.getValue();
                Long advertiserId = dataset.advertiserId;
                Long campaignId   = dataset.campaignId;
                System.out.println("" + advertiserId + "/" +
                        campaignId + ", Mouse clicked on data point: "
                        + xyEnt.toString());
                try
                {
                    String perspective = computePerspectiveForClick(name);
                    String uString = "INSPECT/INSPECT?INSPECT=" +
                            campaignId +
                            "&advertiserId=" + advertiserId +
                            "&campaignId=" + campaignId +
                            "&curve=" + URLEncoder.encode(name, "UTF-8") +
                            "&period=" +
                            URLEncoder.encode(period.toString(), "UTF-8") +
                            "&value="  +
                            URLEncoder.encode( value.toString(), "UTF-8") +
                            "&perspective=" +
                            URLEncoder.encode(      perspective, "UTF-8") +
                            "#" + hash;
                    AppletContext a = parent.getAppletContext();
                    URL db = parent.getDocumentBase();
                    String path = db.getPath();
                    int index1 = path.lastIndexOf("/");
                    int index2 = path.substring(0, index1).lastIndexOf("/");
                    URL u = new URL(db.getProtocol(),
                            db.getHost(),
                            db.getPort(),
                            path.substring(0, index2 + 1) + uString);
                    a.showDocument(u, "_blank");
                }
                catch (MalformedURLException e)
                {
                    throw new Error(e);
                }
                catch (UnsupportedEncodingException e)
                {
                    throw new Error(e);
                }
            }
        }
    }

    public void chartMouseMoved(ChartMouseEvent event) {
        // From demos/MouseListenerDemo2.java.
        // We don't want to track mose move, just clicks.
        /*
        int x = event.getTrigger().getX();
        int y = event.getTrigger().getY();
        ChartEntity entity = event.getEntity();
        if (entity != null) {
            System.out.println("Mouse moved: " + x + ", " + y + ": "
                    + entity.toString());
        }
        else {
            System.out.println("Mouse moved: " + x + ", " + y
                    + ": null entity.");
        }
        */
    }

}

class CampaignTimeSeriesCollection extends TimeSeriesCollection {

    private static final long serialVersionUID = 7526472295622776147L;

    String name;
    Long advertiserId;
    Long campaignId;

    public CampaignTimeSeriesCollection
            (String name, Long advertiserId, Long campaignId)
    {
        this.name = name;
        this.advertiserId = advertiserId;
        this.campaignId = campaignId;
    }
}

class AnnotationHackingChartFactory extends ChartFactory {

    public static JFreeChart createTimeSeriesChart(String title,
                                                   String timeAxisLabel,
                                                   String valueAxisLabel,
                                                   XYDataset dataset,
                                                   boolean legend,
                                                   boolean tooltips,
                                                   boolean urls) {

        ValueAxis timeAxis = new DateAxis(timeAxisLabel);
        timeAxis.setLowerMargin(0.02);  // reduce the default margins
        timeAxis.setUpperMargin(0.02);
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        valueAxis.setAutoRangeIncludesZero(false);  // override default
        XYPlot plot = new AnnotationHackingXYPlot
                (dataset, timeAxis, valueAxis, null);

        XYToolTipGenerator toolTipGenerator = null;
        if (tooltips) {
            toolTipGenerator
                = StandardXYToolTipGenerator.getTimeSeriesInstance();
        }

        XYURLGenerator urlGenerator = null;
        if (urls) {
            urlGenerator = new StandardXYURLGenerator();
        }

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true,
                false);
        renderer.setBaseToolTipGenerator(toolTipGenerator);
        renderer.setURLGenerator(urlGenerator);
        plot.setRenderer(renderer);

        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT,
                plot, legend);
        getChartTheme().apply(chart);
        return chart;

    }

}

class ExplicitAxisXYTextAnnotation extends XYTextAnnotation {
    private static final long serialVersionUID = 7526472295622776147L;

    ValueAxis domainAxis;
    ValueAxis rangeAxis;

    ExplicitAxisXYTextAnnotation(ValueAxis domainAxis, ValueAxis rangeAxis,
                                 String text, double x, double y)
    {
        super(text, x, y);
        this.domainAxis = domainAxis;
        this.rangeAxis = rangeAxis;
    }

}

class AnnotationHackingXYPlot extends XYPlot {

    private static final long serialVersionUID = 7526472295622776147L;

    public AnnotationHackingXYPlot(XYDataset dataset, ValueAxis domainAxis,
                                   ValueAxis rangeAxis,
                                   XYItemRenderer renderer) {
        super(dataset, domainAxis, rangeAxis, renderer);
    }

    public void drawAnnotations(Graphics2D g2,
                                Rectangle2D dataArea,
                                PlotRenderingInfo info) {

        for(Object annotationObj: this.getAnnotations())
        {
            XYAnnotation annotation = (XYAnnotation) annotationObj;
            ValueAxis xAxis = getDomainAxis();
            ValueAxis yAxis = getRangeAxis();
            if(annotation instanceof ExplicitAxisXYTextAnnotation)
            {
                ExplicitAxisXYTextAnnotation a =
                        (ExplicitAxisXYTextAnnotation) annotation;
                xAxis = a.domainAxis;
                yAxis = a.rangeAxis;
            }
            annotation.draw(g2, this, dataArea, xAxis, yAxis, 0, info);
        }

    }


}
