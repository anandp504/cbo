package com.tumri.cbo.backend;

import com.tumri.mediabuying.zini.Perspective;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Vector;

public abstract class GraphPerspective extends Perspective {

    public GraphPerspective(String name, int priority)
    {
        super(name, priority);
    }

    public static int APPLET_WIDTH = 800;
    public static int APPLET_HEIGHT = 600;

    public static String appletTagStart()
    {
        return appletTagStart(APPLET_WIDTH, APPLET_HEIGHT);
    }
    
    public static String appletTagStart(int width, int height)
    {
        return "\n    <APPLET archive=\"jmatharray.jar," +
        "jmathplot.jar,jcommon-1.0.17.jar,jfreechart-1.0.14.jar,cbo_applet.jar\"" +
        "\n            width=\"" + width + "\" height=\"" +
        height + "\"" +
        "\n            codebase=\"/cbo/\"";
    }

    public static void emit2DGraphsGeneric(Writer stream, String title,
                                           StringBuffer xPoints,
                                           StringBuffer yPoints,
                                           String measureName,
                                           String className)
            throws IOException
    {
        List<StringBuffer> yPointsList = new Vector<StringBuffer>();
        yPointsList.add(yPoints);
        List<String> measureNames = new Vector<String>();
        measureNames.add(measureName);
        emit2DGraphsGeneric(stream, title, xPoints, yPointsList, measureNames,
                            className);
    }

    public static void emit2DGraphsGeneric(Writer stream, String title,
                                           StringBuffer xPoints,
                                           List<StringBuffer> yPointsList,
                                           List<String> measureNames,
                                           String className)
            throws IOException
    {
        stream.append(appletTagStart());
        stream.append("\n            code=\"com.tumri.cbo.applets.graph.");
        stream.append(className);
        stream.append(".class\">");
        stream.append("\n        <PARAM NAME=\"title\" VALUE=\"");
        stream.append(title);
        stream.append("\">");
        int i = 0;
        for(StringBuffer yPoints: yPointsList)
        {
            stream.append("\n        <PARAM NAME=\"xPoints");
            if(i > 0) stream.append(Integer.toString(i - 1));
            stream.append("\" VALUE=\"");
            stream.append(xPoints.toString());
            stream.append("\">");
            stream.append("\n        <PARAM NAME=\"yPoints");
            if(i > 0) stream.append(Integer.toString(i - 1));
            stream.append("\" VALUE=\"");
            stream.append(yPoints.toString());
            stream.append("\">");
            String measureName = measureNames.get(i);
            stream.append("\n        <PARAM NAME=\"name");
            if(i > 0) stream.append(Integer.toString(i - 1));
            stream.append("\" VALUE=\"");
            stream.append(measureName);
            stream.append("\">");
            i = i + 1;
        }
        stream.append("\n    A graph should show here!");
        stream.append("\n    </APPLET>");
    }

    public static void emit3DGraphsGeneric
            (Writer stream, String title, StringBuffer xPoints,
             StringBuffer yPoints, StringBuffer zPoints,
             String measureName, String className)
            throws IOException
    {
        List<StringBuffer> zPointsList = new Vector<StringBuffer>();
        zPointsList.add(zPoints);
        List<String> measureNames = new Vector<String>();
        measureNames.add(measureName);
        emit3DGraphsGeneric(stream, title, xPoints, yPoints, zPointsList,
                            measureNames, className);
    }

    public static void emit3DGraphsGeneric
            (Writer stream, String title, StringBuffer xPoints,
             StringBuffer yPoints, List<StringBuffer> zPointsList,
             List<String> measureNames, String className)
            throws IOException
    {
        stream.append(appletTagStart());
        stream.append("\n            code=\"com.tumri.cbo.applets.graph.");
        stream.append(className);
        stream.append(".class\">");
        stream.append("\n        <PARAM NAME=\"title\" VALUE=\"");
        stream.append(title);
        stream.append("\">");
        int i = 0;
        for(StringBuffer zPoints: zPointsList)
        {
            stream.append("\n        <PARAM NAME=\"xPoints");
            if(i > 0) stream.append(Integer.toString(i - 1));
            stream.append("\" VALUE=\"");
            stream.append(xPoints.toString());
            stream.append("\">");
            stream.append("\n        <PARAM NAME=\"yPoints");
            if(i > 0) stream.append(Integer.toString(i - 1));
            stream.append("\" VALUE=\"");
            stream.append(yPoints.toString());
            stream.append("\">");
            stream.append("\n        <PARAM NAME=\"zPoints");
            if(i > 0) stream.append(Integer.toString(i - 1));
            stream.append("\" VALUE=\"");
            stream.append(zPoints.toString());
            stream.append("\">");
            String measureName = measureNames.get(i);
            stream.append("\n        <PARAM NAME=\"name");
            if(i > 0) stream.append(Integer.toString(i - 1));
            stream.append("\" VALUE=\"");
            stream.append(measureName);
            stream.append("\">");
            i = i + 1;
        }
        stream.append("\n    A graph should show here!");
        stream.append("\n    </APPLET>");
    }



}
