package com.tumri.cbo.monitor;

import java.io.IOException;
import java.io.Writer;


public abstract class AbstractReporter implements ProblemReporter {

    public static final String URL_PRELUDE = "../";

    public static void nl(Writer w, boolean htmlify)
            throws IOException
    {
        AbstractProblem.nl(w, htmlify);
    }

    public static void nl(Writer w, int spaces, boolean htmlify)
            throws IOException
    {
        AbstractProblem.nl(w, spaces, htmlify);
    }

    @SuppressWarnings("unused")
    public static void sp(Writer w, int spaces, boolean htmlify)
            throws IOException
    {
        AbstractProblem.sp(w, spaces, htmlify);
    }

    @SuppressWarnings("unused")
    public static String _html(String s, boolean htmlify)
    {
        return AbstractProblem._html(s, htmlify);
    }

    public static void underline(Writer writer, String s, boolean htmlify)
            throws IOException
    {
        if(s != null && s.length() > 0)
        {
            nl(writer, htmlify);
            if(htmlify) writer.append("<U>");
            writer.append(_html(s, htmlify));
            if(htmlify) writer.append("</U>");
            else
            {
                nl(writer, htmlify);
                for(int i = 0; i < s.length(); i++)
                {
                    writer.append("=");
                }
            }
        }
    }
}
