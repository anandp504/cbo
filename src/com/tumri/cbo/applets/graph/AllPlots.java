package com.tumri.cbo.applets.graph;

@SuppressWarnings("unused")
public class AllPlots {

    static Class[] classes =
            new Class[]
                    {
			                CampaignPerformancePlot.class,
                            Simple2DBarPlot.class,
                            Simple2DGraph.class,
                            Simple2DHistogramPlot.class,
                            Simple2DScatterPlot.class,
                            Simple2DStaircasePlot.class,
                            Simple3DBarPlot.class,
                            Simple3DGraph.class,
                            Simple3DGrid.class,
                            Simple3DScatterPlot.class
                    };

    public static void main(String[] args)
    {
        System.out.println(classes.toString());
    }


}

