package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;

public class Notification extends AbstractProblem {

    private Notification() {}

    public Notification(AbstractMonitor monitor, String str,
                        CampaignData cd, Object... args)
    {
        super(monitor, str, cd, args);
    }

    public Notification(AbstractMonitor monitor, Messages msgs,
                        CampaignData cd, Object... args)
    {
        super(monitor, msgs, cd, args);
    }

    public int severity() { return 0; }

    static final Notification prototype = new Notification();

    public AbstractProblem prototype() { return prototype; }

    public String summaryHeading(boolean htmlify)
    {
        if(htmlify)
            return "<H3><U>Notifications:</U></H3>";
        else return "Notifications:\n==============\n";
    }
}
