package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;

public abstract class WarningMonitor extends AbstractMonitor {

    public AbstractProblem result
            (String message, CampaignData cd, Object... arguments)
    {
        return recordProblem(new Warning(this, message, cd, arguments));
    }

    public AbstractProblem result
            (Messages messages, CampaignData cd, Object... arguments)
    {
        return recordProblem(new Warning(this, messages, cd, arguments));
    }

    public WarningMonitor(MessageReport[] applicableReports,
                          ReportApplicabilityColumn... includedColumns)
    {
        super(applicableReports, includedColumns);
    }
}
