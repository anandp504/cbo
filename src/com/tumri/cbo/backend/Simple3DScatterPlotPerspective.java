package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

public class Simple3DScatterPlotPerspective extends GraphPerspective {

    public static Simple3DScatterPlotPerspective PERSPECTIVE =
	                    new Simple3DScatterPlotPerspective();

    private Simple3DScatterPlotPerspective()
    {
        super("3D scatter plot", 5);
    }

    public static boolean isThreeDGraphP(Object o)
    {
        return Graph.isGraphOfLengthP(o, 3);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return admin && isThreeDGraphP(o);
    }

    public static void emit3DScatterPlot
            (Writer stream, String title, StringBuffer xPoints,
             StringBuffer yPoints, StringBuffer zPoints, String measureName)
            throws IOException
    {
        emit3DGraphsGeneric(stream, title, xPoints, yPoints, zPoints,
                            measureName, "Simple3DScatterPlot");
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
        if(isThreeDGraphP(x))
        {
            double[] xcurve = Graph.toDoubleArraySlice(x, 0);
            double[] ycurve = Graph.toDoubleArraySlice(x, 1);
            double[] zcurve = Graph.toDoubleArraySlice(x, 2);
            StringBuffer xPoints = Graph.collectCurvePoints(xcurve);
            StringBuffer yPoints = Graph.collectCurvePoints(ycurve);
            StringBuffer zPoints = Graph.collectCurvePoints(zcurve);
            emit3DScatterPlot(stream, "", xPoints, yPoints, zPoints,
                              Graph.getMeasureName(x, 2));
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
                   (stream, Bidder.int3DListCurveB, Manager.MANAGER,
                    Manager.MANAGER.sufficientQueryContextFor(),
                    true, true, true, true, true, true, 1000, 1000,
                    1000, "../INSPECT/", null, null, null);
            /*
            stream.append("\n<HR>\n");
            PERSPECTIVE.htmlify
                   (stream, Bidder.int3DListCurve, Manager.MANAGER,
                    Manager.MANAGER.sufficientQueryContextFor(),
                    true, true, true, true, true, true, 1000, 1000,
                    1000, "../INSPECT/", null, null);
            stream.append("\n<HR>\n");
            PERSPECTIVE.htmlify
                   (stream, Bidder.int3DCurve, Manager.MANAGER,
                    Manager.MANAGER.sufficientQueryContextFor(),
                    true, true, true, true, true, true, 1000, 1000,
                    1000, "../INSPECT/", null, null);
                    */
            stream.append("\n</BODY>\n");
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
