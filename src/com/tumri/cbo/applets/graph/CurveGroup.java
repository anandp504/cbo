package com.tumri.cbo.applets.graph;

import org.jfree.data.xy.XYDataset;

import java.util.List;
import java.util.Set;
import java.util.Vector;

public class CurveGroup {
    private String yAxisName;
    private List<String> curveNames;
    private List<double[]> curves;
    private List<XYDataset> dataSets = new Vector<XYDataset>();
    private List<Set<CurveStyle>> curveStylesList;
    private List<List<String>> annotationsList = new Vector<List<String>>();
    private List<Long> curveAdvertisers;
    private List<Long> curveCampaigns;

    public CurveGroup(String yAxisName, List<String> curveNames,
                      List<double[]> curves,
                      List<Set<CurveStyle>> curveStylesList,
                      List<Long> curveAdvertisers, List<Long> curveCampaigns)
    {
        this.yAxisName = yAxisName;
        this.curveNames = curveNames;
        this.curves = curves;
        this.curveStylesList = curveStylesList;
        this.curveAdvertisers = curveAdvertisers;
        this.curveCampaigns = curveCampaigns;
    }

    public String getYAxisName()
    {
        return yAxisName;
    }

    public int size()
    {
        return (dataSets == null ? 0 : dataSets.size());
    }

    public int curvesSize()
    {
        return curves.size();
    }

    public String getName(int curveIndex)
    {
        return curveNames.get(curveIndex);
    }

    public XYDataset getDataSet(int curveIndex)
    {
        return dataSets.get(curveIndex);
    }

    public List<XYDataset> getDataSets()
    {
        return dataSets;
    }

    public double[] getCurve(int curveIndex)
    {
        return curves.get(curveIndex);
    }

    public List<String> getAnnotations(int curveIndex)
    {
        return annotationsList.get(curveIndex);
    }

    public Set<CurveStyle> getStyles(int curveIndex)
    {
        return curveStylesList.get(curveIndex);
    }

    public Long getAdvertiserId(int curveIndex)
    {
        return (curveIndex < curveAdvertisers.size()
                    ? curveAdvertisers.get(curveIndex)
                    : null);
    }

    public Long getCampaignId(int curveIndex)
    {
        return (curveIndex < curveCampaigns.size()
                    ? curveCampaigns.get(curveIndex)
                    : null);
    }

    public void addDataSet(XYDataset dataSet)
    {
        dataSets.add(dataSet);
    }

    public void addCurveName(String name)
    {
        curveNames.add(name);
    }

    public void addCurveAdvertiser(Long id)
    {
        curveAdvertisers.add(id);
    }

    public void addCurveCampaign(Long id)
    {
        curveCampaigns.add(id);
    }

    public void addCurveStyles(Set<CurveStyle> styles)
    {
        curveStylesList.add(styles);
    }

    public void addAnnotations(List<String> annotations)
    {
        annotationsList.add(annotations);
    }

    public void addCurve(double[] yPoints)
    {
        if(curves == null) curves = new Vector<double[]>();
        curves.add(yPoints);
    }

}

