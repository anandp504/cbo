package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;
import com.tumri.mediabuying.zini.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class LoggedCampaignNotification extends UrgentNotifyMonitor {

    public LoggedCampaignNotification
            (ReportApplicabilityColumn... includedColumns)
    {
        super(null, includedColumns);
    }

    public AbstractProblem checkCampaign(Long advertiserId, CampaignData cd,
                                 SQLConnector connector, QueryContext qctx,
                                 SQLContext sctx, Date now, TimeZone localTz)
    {
        List<String> notifications = cd.getNotifications();
        int count = notifications.size();
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
                    for(String notification: notifications)
                    {
                        AbstractProblem.nl(sb, 10, htmlify);
                        sb.append(AbstractProblem._html(notification, htmlify));
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
            cd.resetNotifications();
            return result(messages, null);
        }
        else return null;
    }

    public String shortHeading()
    {
        return "Had notifications(s) logged.";
    }

    public String heading()
    {
        return "The following notifications were logged";
    }
}
