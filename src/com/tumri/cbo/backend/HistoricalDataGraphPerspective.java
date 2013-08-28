package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class HistoricalDataGraphPerspective extends Perspective {

    public static HistoricalDataGraphPerspective PERSPECTIVE =
	                    new HistoricalDataGraphPerspective();

    private HistoricalDataGraphPerspective()
    {
        super("Graph historical data", 3);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        if(admin && o instanceof HashMap)
        {
            HashMap hm = (HashMap) o;
            if(hm.size() >= 0)
            {
                for(Object key: hm.keySet())
                {
                    Object val = hm.get(key);
                    if(key instanceof Date && val instanceof HistoricalDataRow)
                    {}
                    else return false;
                }
                return true;
            }
            else return false;
        }
        else return false;
    }

    List<Date> sortedHours(HashMap<Date, HistoricalDataRow> histData)
    {
        List<Date> hours = new Vector<Date>();
        for(Date hour: histData.keySet())
        {
            hours.add(hour);
        }
        Collections.sort(hours);
        return hours;
    }

    StringBuffer collectHourPoints(List<Date> hours)
    {
        StringBuffer xPoints = new StringBuffer();
        Date firstHour = null;
        boolean firstP;
        //-------------------
        firstP = true;
        for(Date hour: hours)
        {
            if(firstHour == null) firstHour = hour;
            if(firstP) firstP = false;
            else xPoints.append(",");
            xPoints.append((hour.getTime() - firstHour.getTime()) /
                              BidderGrapherHTTPHandler.MS_PER_DAY);
        }
        return xPoints;
    }

    StringBuffer collectMeasurePoints
            (List<Date> hours, HashMap<Date, HistoricalDataRow> histData,
             boolean impsP)
    {
        StringBuffer measurePoints = new StringBuffer();
        boolean firstP;
        firstP = true;
        for(Date key: hours)
        {
            HistoricalDataRow row = histData.get(key);
            Double val = (impsP ? row.getImpressionCount() : row.getCost());
            if(firstP) firstP = false;
            else measurePoints.append(",");
            measurePoints.append(val.toString());
        }
        return measurePoints;
    }

    @SuppressWarnings("unchecked")
    public void htmlify
            (Writer stream, Object x, Agent agent, QueryContext qctx,
             boolean ziniStructureToo, boolean javaStructureToo,
             boolean showNulls, boolean showStaticFields, boolean anchorKids,
             boolean useFrameHandles, Integer maxFields, Integer maxLen,
             Integer maxPrintLen, String urlPrefix, Agenda<Anchorable> agenda,
             Perspective p, Map<String, String> httpParams)
	throws IOException
    {
        HashMap<Date, HistoricalDataRow> histData =
                (HashMap<Date, HistoricalDataRow>) x;
        List<Date> hours = sortedHours(histData);
        StringBuffer xPoints = collectHourPoints(hours);
        StringBuffer impsYPoints = collectMeasurePoints(hours, histData, true);
        StringBuffer costYPoints = collectMeasurePoints(hours, histData,false);
        Simple2DGraphPerspective.emit2DGraph
                (stream, "Impressions", xPoints, impsYPoints,"Impressions");
        stream.append("\n<HR>\n");
        Simple2DGraphPerspective.emit2DGraph
                (stream, "Cost", xPoints, costYPoints,"Cost");
    }
}
