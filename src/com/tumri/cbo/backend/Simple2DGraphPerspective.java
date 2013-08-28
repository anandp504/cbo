package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Simple2DGraphPerspective extends GraphPerspective {

    public static Simple2DGraphPerspective PERSPECTIVE =
	                    new Simple2DGraphPerspective();

    private Simple2DGraphPerspective()
    {
        super("2D line graph", 7);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return admin &&
               (Graph.isOneDGraph(o) || Graph.isTwoDGraphP(o) ||
                Graph.isCollectionOfCurvesP(o));
    }


    public static void emit2DGraph(Writer stream, String title,
                                   StringBuffer xPoints, StringBuffer yPoints,
                                   String measureName)
            throws IOException
    {
        emit2DGraphsGeneric(stream, title, xPoints, yPoints, measureName,
                            "Simple2DGraph");
    }

    public static void emit2DGraphs(Writer stream, String title,
                                    StringBuffer xPoints,
                                    List<StringBuffer> yPointsList,
                                    List<String> measureNames)
            throws IOException
    {
        emit2DGraphsGeneric(stream, title, xPoints, yPointsList, measureNames,
                            "Simple2DGraph");
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
            emit2DGraph(stream, "", xPoints, yPoints, Graph.getMeasureName(x));
        }
        else if(Graph.isTwoDGraphP(x))
        {
            double[] xcurve = Graph.toDoubleArraySlice(x, 0);
            double[] ycurve = Graph.toDoubleArraySlice(x, 1);
            StringBuffer xPoints = Graph.collectCurvePoints(xcurve);
            StringBuffer yPoints = Graph.collectCurvePoints(ycurve);
            emit2DGraph(stream, "", xPoints, yPoints,
                        Graph.getMeasureName(x, 1));
        }
        else if(Graph.isCollectionOfCurvesP(x))
        {
            List<Object> curves = Graph.toListOfCurves(x);
            double[] xcurve = Graph.toDoubleArray(curves.get(0));
            StringBuffer xPoints = Graph.collectCurvePoints(xcurve);
            List<StringBuffer> yPointsList = new Vector<StringBuffer>();
            List<String> labels = new Vector<String>();
            for(int i = 1; i < curves.size(); i++)
            {
                double[] ycurve = Graph.toDoubleArray(curves.get(i));
                StringBuffer yPoints = Graph.collectCurvePoints(ycurve);
                yPointsList.add(yPoints);
                labels.add(Graph.getMeasureName(x, i - 1));
            }
            emit2DGraphs(stream, "", xPoints, yPointsList, labels);
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
                   (stream, Bidder.int2DListCurveB, Manager.MANAGER,
                    Manager.MANAGER.sufficientQueryContextFor(),
                    true, true, true, true, true, true, 1000, 1000,
                    1000, "../INSPECT/", null, null, null);
            stream.append("\n<HR>\n");
            /*
            PERSPECTIVE.htmlify
                   (stream, Bidder.intListCurve, Manager.MANAGER,
                    Manager.MANAGER.sufficientQueryContextFor(),
                    true, true, true, true, true, true, 1000, 1000,
                    1000, "../INSPECT/", null, null);
            stream.append("\n<HR>\n");
            PERSPECTIVE.htmlify
                   (stream, Bidder.int2DListCurve, Manager.MANAGER,
                    Manager.MANAGER.sufficientQueryContextFor(),
                    true, true, true, true, true, true, 1000, 1000,
                    1000, "../INSPECT/", null, null);
            stream.append("\n<HR>\n");
            PERSPECTIVE.htmlify
                   (stream, Bidder.intCurve, Manager.MANAGER,
                    Manager.MANAGER.sufficientQueryContextFor(),
                    true, true, true, true, true, true, 1000, 1000,
                    1000, "../INSPECT/", null, null);
            stream.append("\n<HR>\n");
            PERSPECTIVE.htmlify
                   (stream, Bidder.int2DCurve, Manager.MANAGER,
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
