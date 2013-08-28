package com.tumri.cbo.applets.graph;
import org.math.plot.PlotPanel;

import javax.swing.*;

public class AbstractGraph extends JApplet {

    private static final long serialVersionUID = 7526472295622776147L;

    public void maybeAddLegend(PlotPanel plot)
    {
        for(String suffix: suffices)
        {
            String name = getParameter("name" + suffix);
            if(name == null) name = "";
            if(!"".equals(name))
            {
                plot.addLegend(PlotPanel.EAST);
                break;
            }
        }        
    }

    static double[] toDoubleArray(String s)
    {
        String[] split = s.split(",");
        double[] res = new double[split.length];
        for(int i=0; i < res.length; i++)
        {
            try
            {
                res[i] = Double.parseDouble(split[i]);
            }
            catch (NumberFormatException e)
            {
                res[i] = 0.0d;
            }
        }
        return res;
    }

    @SuppressWarnings("unused")
    static double[][] toDoubleArrayOfPairs(String x, String y)
    {
        String[] splitx = x.split(",");
        String[] splity = y.split(",");
        if(splitx.length == splity.length)
        {
            double[][] res = new double[splitx.length][2];
            for(int i=0; i < res.length; i++)
            {
                try
                {
                    res[i][0] = Double.parseDouble(splitx[i]);
                    res[i][1] = Double.parseDouble(splity[i]);
                }
                catch (NumberFormatException e)
                {
                    res[i][0] = 0.0d;
                    res[i][1] = 0.0d;
                }
            }
            return res;
        }
        else throw new Error("Lengths of point sets don't match");
    }

    static String[] suffices =
            { "", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };

}

