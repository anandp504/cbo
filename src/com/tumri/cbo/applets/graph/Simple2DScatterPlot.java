package com.tumri.cbo.applets.graph;
import org.math.plot.Plot2DPanel;

public class Simple2DScatterPlot extends AbstractGraph {

    private static final long serialVersionUID = 7526472295622776147L;

    public void init()
    {
        String title = getParameter("title");
        if(title == null) title = "Unnamed";
        Plot2DPanel plot = new Plot2DPanel(title);
        maybeAddLegend(plot);
        for(String suffix: suffices)
        {
            String name = getParameter("name" + suffix);
            String xPointsS  = getParameter("xPoints"   + suffix);
            String yPointsS  = getParameter("yPoints"   + suffix);
            if(name == null) name = "";
            if(xPointsS == null || yPointsS == null) break;
            double[] xPoints = toDoubleArray(xPointsS);
            double[] yPoints = toDoubleArray(yPointsS);
            plot.addScatterPlot(name, xPoints, yPoints);
        }
        setContentPane(plot);
    }

    public void start()
    {
    }

    public void stop()
    {
    }

}

