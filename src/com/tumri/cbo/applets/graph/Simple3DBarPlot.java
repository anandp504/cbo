package com.tumri.cbo.applets.graph;
import org.math.plot.Plot3DPanel;

public class Simple3DBarPlot extends AbstractGraph {

    private static final long serialVersionUID = 7526472295622776147L;

    public void init()
    {
        String title = getParameter("title");
        if(title == null) title = "Unnamed";
        Plot3DPanel plot = new Plot3DPanel(title);
        maybeAddLegend(plot);
        for(String suffix: suffices)
        {
            String name = getParameter("name" + suffix);
            String xPointsS  = getParameter("xPoints"   + suffix);
            String yPointsS  = getParameter("yPoints"   + suffix);
            String zPointsS  = getParameter("zPoints"   + suffix);
            if(name == null) name = "";
            if(xPointsS == null || yPointsS == null) break;
            double[] xPoints = toDoubleArray(xPointsS);
            double[] yPoints = toDoubleArray(yPointsS);
            double[] zPoints = toDoubleArray(zPointsS);
            plot.addBarPlot(name, xPoints, yPoints, zPoints);
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

