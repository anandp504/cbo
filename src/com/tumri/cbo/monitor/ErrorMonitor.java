package com.tumri.cbo.monitor;

import com.tumri.cbo.backend.CampaignData;

@SuppressWarnings("unused")
public abstract class ErrorMonitor extends AbstractMonitor {

    @SuppressWarnings("unused")
    public ErrorMonitor(MessageReport[] applicableReports,
                        ReportApplicabilityColumn... includedColumns)
    {
        super(applicableReports, includedColumns);
    }

    public AbstractProblem result
            (Messages messages, CampaignData cd, Object... arguments)
    {
        return recordProblem(new MonitorError(this, messages, cd, arguments));
    }

    public AbstractProblem result
            (String message, CampaignData cd, Object... arguments)
    {
        return recordProblem(new MonitorError(this, message, cd, arguments));
    }
}
