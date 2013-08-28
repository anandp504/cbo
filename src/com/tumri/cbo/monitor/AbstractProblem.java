package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.appnexus.AppNexusUtils;
import com.tumri.mediabuying.zini.HTTPHandler;
import com.tumri.mediabuying.zini.Utils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Comparator;

public abstract class AbstractProblem {

    AbstractMonitor monitor;
    String str = null;
    Messages msgs = null;
    Object[] args;
    CampaignData cd;
    public static final SeverityComparator SEVERITY_COMPARATOR =
            new SeverityComparator();

    abstract public int severity();
    abstract public AbstractProblem prototype();
    abstract public String summaryHeading(boolean htmlify);

    public AbstractProblem() {}

    public AbstractProblem(AbstractMonitor monitor, String str,
                           CampaignData cd, Object... args)
    {
        this.monitor = monitor;
        this.str = str;
        this.msgs = null;
        this.args = args;
        this.cd = cd;
    }

    public AbstractProblem(AbstractMonitor monitor, Messages msgs,
                           CampaignData cd, Object... args)
    {
        this.monitor = monitor;
        this.str = null;
        this.msgs = msgs;
        this.args = args;
        this.cd = cd;
    }

    public CampaignData getCampaignData()
    {
        return cd;
    }

    public Long getAdvertiserId()
    {
        return (cd == null ? null : cd.getAdvertiserId());
    }

    public String getAdvertiserName()
    {
        return (cd == null ? null : cd.getAdvertiserName());
    }

    public String getCampaignName()
    {
        return (cd == null ? null : monitor.briefName(cd));
    }

    public AbstractMonitor getMonitor()
    {
        return monitor;
    }

    public String toString()
    {
        StringBuffer sb =
                new StringBuffer("#<" + AppNexusUtils.afterDot
                                              (this.getClass().toString())
                                      + ": ");
        String s =
              (msgs != null && args != null
                      ? msgs.getText()
                      : str);
        if(s != null && args != null)
        {
            int i = 0;
            for(Object obj: args)
            {
                if(i > 0) sb.append(", ");
                sb.append(obj.toString());
            }
        }
        else sb.append("Prototype");
        sb.append(">");
        return sb.toString();
    }

    public static void nl(Writer w, boolean htmlify)
            throws IOException
    {
        nl(w, 0, htmlify);
    }

    public static void nl(Writer w, int spaces, boolean htmlify)
            throws IOException
    {
        if(htmlify) w.append("<BR>\n");
        else w.append("\n");
        sp(w, spaces, htmlify);
    }

    public static void sp(Writer w, int spaces, boolean htmlify)
            throws IOException
    {
        for(int i = 0; i < spaces; i++)
        {
            if(htmlify) w.append("&nbsp;");
            else w.append(" ");
        }
    }

    public String summarise(boolean htmlify)
    {
        return summarise(htmlify, false);
    }

    public static String _html(String s, boolean htmlify)
    {
        if(htmlify) return HTTPHandler.htmlify(s);
        else return s;
    }

    public String summarise(boolean htmlify, boolean terse)
    {
        CampaignData camp = cd;
        Long adv = null;
        try
        {
            StringWriter sb = new StringWriter();
            for(Object arg: args)
            {
                if(arg instanceof CampaignData)
                    camp = (CampaignData) arg;
                else if(arg instanceof Long)
                    adv = (Long) arg;
                else {}
            }
            if(!terse && camp != null)
            {
                sb.append(_html(monitor.briefName(camp), htmlify));
            }
            else if(!terse && adv != null)
            {
                sb.append("Advertiser ");
                sb.append(Long.toString(adv));
            }
            else if(!terse)
            {
                sb.append("???");
            }
            else {}
            String explanation = explain(htmlify);
            if(terse)
                sb.append(_html(monitor.shortHeading(), htmlify));
            if(explanation != null && !"".equals(explanation))
            {
                sp(sb, 2, htmlify);
                sb.append(explanation);
            }
            return sb.toString();
        }
        catch(IOException e)
        {
            throw Utils.barf(e, null);
        }
    }

    public String explain(boolean htmlify)
    {
        if(str != null)
            return _html(str, htmlify);
        else if(msgs != null)
            return (htmlify ? msgs.getHTML()
                            : _html(msgs.getText(), true));
        else throw Utils.barf("Not initialised properly", this);
    }
}

class SeverityComparator implements Comparator<AbstractProblem> {

    public int compare(AbstractProblem x, AbstractProblem y)
    {
        int xs = x.severity();
        int ys = y.severity();
        if(xs < ys) return 1;
        else if(ys < xs) return -1;
        else return 0;
    }

    public boolean equals(Object o)
    {
        return o instanceof SeverityComparator && o == this;
    }
}