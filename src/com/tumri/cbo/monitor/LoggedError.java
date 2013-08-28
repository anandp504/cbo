package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.zini.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class LoggedError extends NonUrgentErrorMonitor {

    public LoggedError(ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign
            (Long advertiserId, CampaignData cd, SQLConnector connector,
             QueryContext qctx, SQLContext sctx, Date now, TimeZone localTz)
    {
        return null; // Don't look at campaigns.
    }

    public AbstractProblem checkGlobal
            (SQLConnector connector, QueryContext qctx, SQLContext sctx,
             Date now, TimeZone localTz)
    {
        List<String> errors = Utils.getErrors();
        int count = errors.size();
        if(count > 0)
        {
            Messages messages;
            String text = null;
            String html = null;
            try
            {
                for(int i = 0; i < AbstractMonitor.MESSAGES_SIZE; i++)
                {
                    boolean htmlify = i > AbstractMonitor.TEXT_INDEX;
                    StringWriter sb = new StringWriter();
                    sb.append("Logged since the last report: ");
                    sb.append(Integer.toString(count));
                    sb.append(".");
                    for(String error: errors)
                    {
                        AbstractProblem.nl(sb, 10, htmlify);
                        sb.append(AbstractProblem._html(error, htmlify));
                    }
                    if(htmlify) html = sb.toString();
                    else text = sb.toString();
                }
            }
            catch(IOException e)
            {
                throw Utils.barf(e, this);
            }
            messages = new Messages(text, html);
            Utils.resetErrors();
            return result(messages, null);
        }
        else return null;
    }

    public String shortHeading()
    {
        return "Had error(s) logged.";
    }

    public String heading()
    {
        return "The following errors were logged";
    }
}
