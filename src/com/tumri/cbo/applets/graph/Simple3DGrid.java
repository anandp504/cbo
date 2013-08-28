package com.tumri.cbo.applets.graph;
import org.math.plot.Plot3DPanel;

public class Simple3DGrid extends AbstractGraph {

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
            String xAxisPointsS  = getParameter("xPoints"   + suffix);
            String yAxisPointsS  = getParameter("yPoints"   + suffix);
            String zPointsS  = getParameter("zPoints"   + suffix);
            if(name == null) name = "";
            if(xAxisPointsS == null || yAxisPointsS == null ||
               zPointsS == null)
                break;
            double[] xAxisPoints = toDoubleArray(xAxisPointsS);
            double[] yAxisPoints = toDoubleArray(yAxisPointsS);
            double[] zPoints = toDoubleArray(zPointsS);
            int targetPoints = xAxisPoints.length * yAxisPoints.length;
            if(zPoints.length == targetPoints)
            {
                int zIndex = 0;
                double[][] zGrid =
                        new double[yAxisPoints.length][xAxisPoints.length];
                for (int j = 0; j < yAxisPoints.length; j++)
                {
                    for (int i = 0; i < xAxisPoints.length; i++)
                    {
                        zGrid[j][i] = zPoints[zIndex];
                        zIndex = zIndex + 1;
                    }
                }
                plot.addGridPlot(name, xAxisPoints, yAxisPoints, zGrid);
            }
            else throw new Error
                    ("There should be " + targetPoints + " Z points in grid " +
                     (name.equals("") ? "Unnamed" : name) + ", not " +
                     zPoints.length);
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

