package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;

public class MonitorError extends AbstractProblem {

    private MonitorError() {}

    public MonitorError(AbstractMonitor monitor, String str,
                        CampaignData cd, Object... args)
    {
        super(monitor, str, cd, args);
    }

    public MonitorError(AbstractMonitor monitor, Messages msgs,
                        CampaignData cd, Object... args)
    {
        super(monitor, msgs, cd, args);
    }

    public int severity() { return 2; }

    static final MonitorError prototype = new MonitorError();

    public AbstractProblem prototype() { return prototype; }

    public String summaryHeading(boolean htmlify)
    {
        if(htmlify)
            return "<U>Errors:</U><BR>";
        else return "Errors:\n=======\n";
    }
}
