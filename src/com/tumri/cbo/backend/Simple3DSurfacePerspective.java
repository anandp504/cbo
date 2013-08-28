package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

public class Simple3DSurfacePerspective extends GraphPerspective {

    public static Simple3DSurfacePerspective PERSPECTIVE =
	                    new Simple3DSurfacePerspective();

    private Simple3DSurfacePerspective()
    {
        super("3D surface", 7);
    }

    public boolean applicableTo(Object o, boolean admin)
    {
        return admin && Graph.isSurfaceP(o);
    }

    public static void emit3DSurface(Writer stream, String title,
                                     StringBuffer xPoints,
                                     StringBuffer yPoints,
                                     StringBuffer zPoints,
                                     String measureName)
            throws IOException
    {
        emit3DGraphsGeneric(stream, title, xPoints, yPoints, zPoints,
                            measureName, "Simple3DGrid");
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
        if(Graph.isSurfaceP(x))
        {
            double[][] surface = Graph.toDoubleSurface(x);
            StringBuffer xAxisPoints = Graph.commaSeparateI(surface[0].length);
            StringBuffer yAxisPoints = Graph.commaSeparateI(surface.length);
            emit3DSurface(stream, "", xAxisPoints, yAxisPoints,
                          Graph.collectCurvePoints(surface),
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
                   (stream, Bidder.intSurface, Manager.MANAGER,
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
