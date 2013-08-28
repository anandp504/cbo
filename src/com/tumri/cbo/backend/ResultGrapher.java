package com.tumri.cbo.backend;

import org.math.plot.Plot2DPanel;
import org.math.plot.Plot3DPanel;
import com.tumri.mediabuying.zini.QueryContext;

import javax.swing.*;
import java.util.*;


@SuppressWarnings("unused")
public class ResultGrapher {

    @SuppressWarnings("unused")
    public static void graphCampaigns
            (Map<Long, AdvertiserData> advertiserMap, QueryContext qctx)
    {
        for(Long id: advertiserMap.keySet())
        {
            AdvertiserData ad = advertiserMap.get(id);
            graphCampaigns(ad, qctx);
        }
    }

    public static void graphCampaigns(AdvertiserData ad, QueryContext qctx)
    {
        // Loop through each *parent* campaign.
        // If you have children, then make a 3D graph of them, otherwise 2D.
        int i;
        Map<String, List<CampaignData>> map =
                new HashMap<String, List<CampaignData>>();
        for(CampaignData cd: ad.campaignData)
        {
            String canonicalName = cd.getCanonicalName();
            List<CampaignData> entry = map.get(canonicalName);
            if(entry == null)
            {
                entry = new Vector<CampaignData>();
                map.put(canonicalName, entry);
            }
            entry.add(cd);
        }
        for(String name: map.keySet())
        {
            List<CampaignData> list = map.get(name);
            if(list.size() > 1)
            {
                double curveIndex = 0.0d;
                String title = list.get(0).campaign.getName();
                Plot3DPanel plot = new Plot3DPanel(title);
                for(CampaignData cd: list)
                {
                    Map<Date, HistoricalDataRow> hist =
                            cd.getHistoricalData(qctx);
                    List<HistoricalDataRow> hlist =
                            new Vector<HistoricalDataRow>();
                    double[] xPoints = new double[hist.size()];
                    double[] yPoints = new double[hist.size()];
                    double[] zPoints = new double[hist.size()];
                    if(hist.size() > 1)
                    {
                        for(Date d: hist.keySet())
                        {
                            HistoricalDataRow row = hist.get(d);
                            hlist.add(row);
                        }
                        Collections.sort(hlist, HistDataComparator.comp);
                        i = 0;
                        for(HistoricalDataRow row: hlist)
                        {
                            xPoints[i] = new Long
                                    (row.dateTime.getTime()).doubleValue();
                            yPoints[i] = curveIndex;
                            zPoints[i] = new Long(row.impressions).doubleValue();
                            i = i + 1;
                        }
                        String curveTitle = cd.campaign.getName();
                        plot.addLinePlot(curveTitle, xPoints, yPoints, zPoints);
                    }
                    curveIndex = curveIndex + 1.0;
                }
                JFrame frame = new JFrame(title);
                frame.setSize(600, 600);
                frame.setContentPane(plot);
                frame.setVisible(true);
            }
            else
            {
                CampaignData cd = list.get(0);
                Map<Date, HistoricalDataRow> hist = cd.getHistoricalData(qctx);
                List<HistoricalDataRow> hlist =new Vector<HistoricalDataRow>();
                double[] xPoints = new double[hist.size()];
                double[] yPoints = new double[hist.size()];
                if(hist.size() > 1)
                {
                    for(Date d: hist.keySet())
                    {
                        HistoricalDataRow row = hist.get(d);
                        hlist.add(row);
                    }
                    Collections.sort(hlist, HistDataComparator.comp);
                    i = 0;
                    for(HistoricalDataRow row: hlist)
                    {
                        xPoints[i] = new Long
                                (row.dateTime.getTime()).doubleValue();
                        yPoints[i] = new Long(row.impressions).doubleValue();
                        i = i + 1;
                    }
                    String title = cd.campaign.getName();
                    Plot2DPanel plot = new Plot2DPanel(title);
                    plot.addLinePlot(title, xPoints, yPoints);
                    JFrame frame = new JFrame(title);
                    frame.setSize(600, 600);
                    frame.setContentPane(plot);
                    frame.setVisible(true);
                }
            }
        }
    }
}

class HistDataComparator implements Comparator<HistoricalDataRow> {
    public static HistDataComparator comp = new HistDataComparator();

    public int compare(HistoricalDataRow a, HistoricalDataRow b)
    {
        return a.dateTime.compareTo(b.dateTime);
    }
}