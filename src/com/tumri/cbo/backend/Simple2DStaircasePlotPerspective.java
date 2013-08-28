package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

public class Simple2DStaircasePlotPerspective extends GraphPerspective {

    public static Simple2DStaircasePlotPerspective PERSPECTIVE =
	                    new Simple2DStaircasePlotPerspective();

    private Simple2DStaircasePlotPerspective()
    {
        super("2D staircase", 3);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return admin && Graph.isTwoDGraphP(o);
    }

    public static void emit2DStaircasePlot
            (Writer stream, String title, StringBuffer xPoints,
             StringBuffer yPoints, String measureName)
            throws IOException
    {
        Simple2DGraphPerspective.emit2DGraphsGeneric
                (stream, title, xPoints, yPoints, measureName,
                 "Simple2DStaircasePlot");
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
        if(Graph.isOneDGraph(x))
        {
            double[] curve = Graph.toDoubleArray(x);
            StringBuffer xPoints = Graph.commaSeparateI(curve.length);
            StringBuffer yPoints = Graph.collectCurvePoints(curve);
            emit2DStaircasePlot(stream, "", xPoints, yPoints,
                                Graph.getMeasureName(x));
        }
        else if(Graph.isTwoDGraphP(x))
        {
            double[] xcurve = Graph.toDoubleArraySlice(x, 0);
            double[] ycurve = Graph.toDoubleArraySlice(x, 1);
            StringBuffer xPoints = Graph.collectCurvePoints(xcurve);
            StringBuffer yPoints = Graph.collectCurvePoints(ycurve);
            emit2DStaircasePlot(stream, "", xPoints, yPoints,
                                Graph.getMeasureName(x, 1));
        }
        else throw Utils.barf("Unhandled object type: " + x,
                              this, x, agent, qctx);
    }

    public static void main(String[] args)
    {
        String path = "c:/doc/graph-test.html";

        try
        {
            PrintWriter stream = new PrintWriter(path);
            stream.append("<HTML>\n");
            stream.append("<HEAD>\n");
            stream.append("<TITLE>Graphs</TITLE>\n");
            stream.append("</HEAD>\n");
            stream.append("<BODY>\n");
            stream.append("<H2>Graphs</H2>\n");
            PERSPECTIVE.htmlify
                   (stream, Bidder.intCurve, Manager.MANAGER,
                    Manager.MANAGER.sufficientQueryContextFor(),
                    true, true, true, true, true, true, 1000, 1000,
                    1000, "../INSPECT/", null, null, null);
            stream.append("\n<HR>\n");
            PERSPECTIVE.htmlify
                   (stream, Bidder.int2DCurve, Manager.MANAGER,
                    Manager.MANAGER.sufficientQueryContextFor(),
                    true, true, true, true, true, true, 1000, 1000,
                    1000, "../INSPECT/", null, null, null);
            stream.append("</BODY>\n");
            stream.append("<HTML>\n");
            stream.flush();
            stream.close();
        }
        catch (IOException e)
        {
            throw Utils.barf(e, null);
        }
    }
}
