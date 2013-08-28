package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;

public class Warning extends AbstractProblem {

    private Warning() {}

    public Warning(AbstractMonitor monitor, String str,
                   CampaignData cd, Object... args)
    {
        super(monitor, str, cd, args);
    }

    public Warning(AbstractMonitor monitor, Messages msgs,
                   CampaignData cd, Object... args)
    {
        super(monitor, msgs, cd, args);
    }

    public int severity() { return 1; }

    static final Warning prototype = new Warning();

    public AbstractProblem prototype() { return prototype; }

    public String summaryHeading(boolean htmlify)
    {
        if(htmlify)
            return "<H3><U>Warnings:</U></H3>";
        else return "Warnings:\n=========\n";
    }
}
